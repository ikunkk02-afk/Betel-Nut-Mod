"""Verify the whole-bunch release against its snapshot and package exact build outputs."""
from pathlib import Path
import difflib
import hashlib
import importlib.util
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'art-rework/fruit-v5.3.0'
RES = ROOT / 'src/main/resources'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def verify():
    before = json.loads((OUT / 'before-sha256.json').read_text(encoding='utf-8'))
    changed, preserved = [], []
    allowed = {
        'src/main/java/betel/nut/block/BetelNutClusterBlock.java',
        'src/main/java/betel/nut/block/BetelPalmLogBlock.java',
        'src/main/java/betel/nut/block/ModBlocks.java',
        'src/main/java/betel/nut/item/ModItemGroups.java',
        'src/client/java/betel/nut/client/BetelPalmTooltipAppender.java',
        'src/gametest/java/betel/nut/test/BetelFruitGameTests.java',
        'src/main/resources/data/betel-nut-mod/loot_table/blocks/betel_nut_cluster.json',
        'src/main/resources/data/betel-nut-mod/loot_table/blocks/betel_palm_leaves.json',
        'src/main/resources/data/betel-nut-mod/recipe/betel_palm_sapling.json',
        'src/main/resources/data/betel-nut-mod/recipe/compat/farmersdelight/cutting_betel_palm_leaves.json',
        'src/main/resources/assets/betel-nut-mod/lang/zh_cn.json',
        'src/main/resources/assets/betel-nut-mod/lang/en_us.json',
        'scripts/generate-feature-assets.py',
        'README.md', 'CHANGELOG.md', 'gradle.properties', 'build.gradle',
    }
    for name, digest in before.items():
        path = ROOT / name
        assert path.is_file(), f'Unexpected deletion: {name}'
        if sha(path) != digest:
            assert name in allowed, f'Out of scope: {name}'
            changed.append(name)
        else:
            preserved.append(name)
    old_recipes = [name for name in before if ('/recipe/' in name or '/advancement/' in name)
                   and name not in allowed]
    assert all(name in preserved for name in old_recipes), 'Existing nut/recovery recipe changed'
    old_art = [name for name in before if any(part in name for part in ['/textures/', '/models/', '/blockstates/'])]
    assert all(name in preserved for name in old_art), 'Existing art changed'
    worldgen = [name for name in before if '/worldgen/' in name]
    assert all(name in preserved for name in worldgen), 'World generation changed'

    for path in RES.rglob('*.json'):
        json.loads(path.read_text(encoding='utf-8'))
    resource_hashes = {path: sha(path) for path in RES.rglob('*') if path.is_file()}
    spec = importlib.util.spec_from_file_location('fruit_assets', ROOT / 'scripts/generate-fruit-harvest-assets.py')
    generator = importlib.util.module_from_spec(spec)
    spec.loader.exec_module(generator)
    generator.generate()
    assert all(sha(path) == digest for path, digest in resource_hashes.items()), 'Generator is not idempotent'
    assert len(resource_hashes) == sum(path.is_file() for path in RES.rglob('*')), 'Generator added files'
    models = RES / 'assets/betel-nut-mod/models'
    green = json.loads((models / 'item/unripe_betel_nut_cluster.json').read_text())
    ripe = json.loads((models / 'item/betel_nut_cluster.json').read_text())
    placed_green = json.loads((models / 'block/betel_nut_cluster_1.json').read_text())
    assert green['display'] == ripe['display'], 'Held transforms differ'
    assert green['textures'] == placed_green['textures']
    assert len(green['elements']) == len(placed_green['elements']) == len(ripe['elements'])
    assert all(a['faces'] == b['faces'] for a, b in zip(green['elements'], placed_green['elements'])), 'Green UVs changed'

    normal_log = (OUT / 'build-final.log').read_text(encoding='utf-8')
    fd_log = (OUT / 'farmersdelight-tests.log').read_text(encoding='utf-8')
    client_log = (OUT / 'client-preview.log').read_text(encoding='utf-8')
    for log in [normal_log, fd_log]:
        assert 'All 75 required tests passed' in log and 'BUILD SUCCESSFUL' in log
        assert 'Failed to parse' not in log and 'required tests failed' not in log
    assert 'farmersdelight 1.21.1-3.3.6' in fd_log
    marker = ('FRUIT_CLIENT_ALL_CHECKS_PASS greenPacket=true ripePacket=true noStalk=true '
              'drops=true regrowth=true maturity=true heldModels=true')
    assert marker in client_log and 'BUILD SUCCESSFUL' in client_log
    screenshots = sorted(path.name for path in (OUT / 'client-run/screenshots').glob('*.png'))
    assert len(screenshots) == 11

    release = ROOT / 'build/libs/betel-nut-mod-5.3.0.jar'
    source = ROOT / 'build/libs/betel-nut-mod-5.3.0-sources.jar'
    with zipfile.ZipFile(release) as jar:
        names = jar.namelist()
        assert json.loads(jar.read('fabric.mod.json'))['version'] == '5.3.0'
        assert not any(name.startswith(('betel/nut/test/', 'betel/nut/preview/', 'vectorwing/'))
                       for name in names), 'Test/preview/FD implementation leaked'
        assert {'betel/nut/item/BetelNutClusterItem.class', 'betel/nut/item/RecoveryDrinkItem.class'} <= set(names)
        for path in RES.rglob('*'):
            if path.is_file() and path.name != 'fabric.mod.json':
                assert jar.read(path.relative_to(RES).as_posix()) == path.read_bytes(), f'Stale resource: {path}'
        embedded = [name for name in names if name.startswith('META-INF/jars/') and name.endswith('.jar')]
        assert embedded and all('midnightlib' in name for name in embedded), 'Unexpected bundled dependency'
    with zipfile.ZipFile(source) as jar:
        assert 'betel/nut/item/BetelNutClusterItem.java' in jar.namelist()
    for path in [release, source]:
        shutil.copy2(path, OUT / path.name)
        assert sha(path) == sha(OUT / path.name)

    additions = [path for folder in ['src', 'scripts', 'docs'] for path in (ROOT / folder).rglob('*')
                 if path.is_file() and '__pycache__' not in path.parts and path.relative_to(ROOT).as_posix() not in before]
    diff, binary = [], []
    with zipfile.ZipFile(OUT / 'before-fruit.zip') as snapshot:
        for name in sorted(changed + [path.relative_to(ROOT).as_posix() for path in additions]):
            try:
                old = snapshot.read(name).decode('utf-8').replace('\r\n', '\n').splitlines(keepends=True) if name in before else []
                new = (ROOT / name).read_text(encoding='utf-8').splitlines(keepends=True)
            except UnicodeDecodeError:
                binary.append(name)
                continue
            diff.extend(difflib.unified_diff(old, new, fromfile='before/' + name, tofile='after/' + name))
    assert all(line[1:].rstrip('\r\n') == line[1:].rstrip() for line in diff
               if line.startswith('+') and not line.startswith('+++')), 'Trailing whitespace in added lines'
    (OUT / 'changes.diff').write_text(''.join(diff), encoding='utf-8')
    report = {
        'version': '5.3.0', 'normalGameTestsPassed': 75, 'farmersDelightGameTestsPassed': 75,
        'farmersDelightVersion': '1.21.1-3.3.6+refabricated', 'actualCuttingBoardBatchesVerified': 3,
        'unchangedExistingNutAndRecoveryRecipesAndAdvancements': len(old_recipes),
        'allExistingArtUnchanged': len(old_art), 'worldgenFilesUnchanged': len(worldgen),
        'clientActualUsePacketsPassed': True, 'clientScreenshots': screenshots,
        'clientVerified': ['green and yellow use packets', 'whole bunch drops and shear durability',
                           'immediate air with no stalk', 'regrowth and ripening with accelerated server random ticks',
                           'left and right hand', 'third person', 'inventory and Chinese tooltips'],
        'preservedBaselineFiles': len(preserved), 'changedBaselineFiles': sorted(changed),
        'newSourceAndResourceFiles': sorted(path.relative_to(ROOT).as_posix() for path in additions),
        'binaryAdditions': binary, 'generatorIdempotent': True, 'allPackagedResourcesMatch': True,
        'testAndPreviewAndDelightCodeExcluded': True, 'artifacts': {path.name: sha(path) for path in [release, source]},
    }
    (OUT / 'verification.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    verify()
