"""Check recipe consistency, scope preservation and the exact release JAR."""
from pathlib import Path
import difflib
import hashlib
import importlib.util
import json
import shutil
import zipfile

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'art-rework/recipes-v5.1.0'
module_spec = importlib.util.spec_from_file_location('variant_recipes', ROOT / 'scripts/generate-variant-recipes.py')
recipes = importlib.util.module_from_spec(module_spec)
module_spec.loader.exec_module(recipes)


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def loaded(conditions, delight):
    def check(c):
        if c['condition'] == 'fabric:all_mods_loaded':
            return delight
        assert c['condition'] == 'fabric:not'
        return not check(c['value'])
    return all(check(c) for c in conditions)


def verify():
    before = json.loads((OUT / 'before-sha256.json').read_text(encoding='utf-8'))
    generated = []
    for name, output, tier, inputs, count, ticks in recipes.SPECS:
        ids = [name, recipes.cooking_id(name)]
        pair = [json.loads((recipes.DATA / f'recipe/{i}.json').read_text(encoding='utf-8')) for i in ids]
        assert pair[0]['ingredients'] == pair[1]['ingredients']
        assert pair[0]['result'] == pair[1]['result'] == {'id': recipes.MOD + output, 'count': count}
        assert len(inputs) <= 6 and len(inputs) == len(pair[0]['ingredients'])
        assert pair[1]['cookingtime'] == ticks and 'container' not in pair[1]
        for delight in [False, True]:
            assert sum(loaded(r['fabric:load_conditions'], delight) for r in pair) == 1
        for i, present in zip(ids, [False, True]):
            for kind in ['recipe', 'advancement/recipes']:
                generated.append(recipes.DATA / f'{kind}/{i}.json')
            book = json.loads((recipes.DATA / f'advancement/recipes/{i}.json').read_text(encoding='utf-8'))
            assert book['fabric:load_conditions'] == recipes.condition(present)
            assert book['rewards']['recipes'] == [recipes.MOD + i]

    allowed = {p.relative_to(ROOT).as_posix() for p in generated} | {
        'README.md', 'CHANGELOG.md', 'gradle.properties', 'build.gradle', 'scripts/generate-feature-assets.py',
        'src/gametest/resources/fabric.mod.json', 'src/gametest/java/betel/nut/test/BetelGameplayGameTests.java',
    }
    changed = []
    preserved = []
    for name, digest in before.items():
        path = ROOT / name
        assert path.is_file(), f'Unexpected deletion: {name}'
        if sha(path) != digest:
            assert name in allowed, f'Out-of-scope change: {name}'
            changed.append(name)
        else:
            preserved.append(name)
    ordinary = [
        'roasted_betel_nut_from_campfire', 'wrapped_raw_betel', 'shell_powder',
        'empty_pack', 'kouweiwang_pack', 'hecheng_tianxia_pack', 'xiangtan_puzi_pack',
    ]
    for name in ordinary:
        relative = f'src/main/resources/data/betel-nut-mod/recipe/{name}.json'
        assert relative in preserved, f'Ordinary/packaging recipe changed: {name}'

    # Regeneration must not alter the data, documentation or any protected resource.
    generated.append(ROOT / 'docs/variant-processing-5.1.0.md')
    digests = {p: sha(p) for p in generated}
    recipes.generate()
    assert all(sha(p) == digest for p, digest in digests.items()), 'Generator is not deterministic'
    for path in (ROOT / 'src/main/resources').rglob('*.json'):
        json.loads(path.read_text(encoding='utf-8'))

    release = ROOT / 'build/libs/betel-nut-mod-5.1.0.jar'
    source = ROOT / 'build/libs/betel-nut-mod-5.1.0-sources.jar'
    with zipfile.ZipFile(release) as jar:
        entries = set(jar.namelist())
        assert json.loads(jar.read('fabric.mod.json'))['version'] == '5.1.0'
        assert not any(n.startswith(('betel/nut/test/', 'vectorwing/')) for n in entries), 'Verification code/mod leaked into release'
        for path in (ROOT / 'src/main/resources').rglob('*'):
            if not path.is_file() or path.name == 'fabric.mod.json':
                continue
            entry = path.relative_to(ROOT / 'src/main/resources').as_posix()
            assert jar.read(entry) == path.read_bytes(), f'Packaged resource is stale: {entry}'
        bundled = [n for n in entries if n.startswith('META-INF/jars/') and n.endswith('.jar')]
        assert all('midnightlib' in n for n in bundled), 'Unexpected bundled dependency'

    normal_log = (OUT / 'build-vanilla.log').read_text(encoding='utf-8')
    delight_log = (OUT / 'test-farmersdelight.log').read_text(encoding='utf-8')
    assert 'All 61 required tests passed' in normal_log and 'BUILD SUCCESSFUL' in normal_log
    assert 'All 61 required tests passed' in delight_log and 'BUILD SUCCESSFUL' in delight_log
    assert 'farmersdelight 1.21.1-3.3.6' in delight_log
    for path in [release, source]:
        shutil.copy2(path, OUT / path.name)

    additions = [p for p in (ROOT / 'src').rglob('*') if p.is_file() and p.relative_to(ROOT).as_posix() not in before]
    additions += [ROOT / 'scripts/generate-variant-recipes.py', ROOT / 'scripts/verify-variant-recipes.py', ROOT / 'docs/variant-processing-5.1.0.md',
                  OUT / 'compat-tests.gradle', OUT / 'compat-test/java/betel/nut/test/FarmerRecipeChecks.java']
    diff = []
    with zipfile.ZipFile(OUT / 'before-recipes.zip') as snapshot:
        for name in sorted(changed + [p.relative_to(ROOT).as_posix() for p in additions]):
            old = snapshot.read(name).decode('utf-8').replace('\r\n', '\n').splitlines(keepends=True) if name in before else []
            new = (ROOT / name).read_text(encoding='utf-8').splitlines(keepends=True)
            diff.extend(difflib.unified_diff(old, new, fromfile='before/' + name, tofile='after/' + name))
    (OUT / 'changes.diff').write_text(''.join(diff), encoding='utf-8')
    report = {
        'version': '5.1.0', 'variantCount': 17, 'recipePairs': 18,
        'vanillaRequiredGameTestsPassed': 61, 'delightRequiredGameTestsPassed': 61,
        'delightVersion': '1.21.1-3.3.6+refabricated', 'actualHeatedPotRecipeBatchesVerified': 18,
        'ordinaryAndPackagingRecipeSha256': {n: before[f'src/main/resources/data/betel-nut-mod/recipe/{n}.json'] for n in ordinary},
        'preservedBaselineFiles': len(preserved), 'changedBaselineFiles': sorted(changed),
        'generatorIdempotent': True, 'allPackagedResourcesMatch': True, 'testCodeAndDelightExcludedFromRelease': True,
        'artifacts': {p.name: sha(p) for p in [release, source]},
    }
    (OUT / 'verification.json').write_text(json.dumps(report, indent=2, ensure_ascii=False) + '\n', encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    verify()
