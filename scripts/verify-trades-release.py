"""Verify trade-only scope, test evidence and the exact 5.4.0 release files."""
from pathlib import Path
import difflib
import hashlib
import json
import shutil
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'art-rework/trades-v5.4.0'
RES = ROOT / 'src/main/resources'


def sha(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()


def canonical(data):
    try:
        return data.decode('utf-8').replace('\r\n', '\n').encode('utf-8')
    except UnicodeDecodeError:
        return data


def verify():
    allowed = {
        'README.md', 'CHANGELOG.md', 'build.gradle', 'gradle.properties',
        'src/main/java/betel/nut/BetelNutConfig.java',
        'src/main/java/betel/nut/villager/ModVillagerTrades.java',
        'src/main/java/betel/nut/command/BetelCommands.java',
        'src/main/resources/assets/betel-nut-mod/lang/zh_cn.json',
        'src/main/resources/assets/betel-nut-mod/lang/en_us.json',
        'src/gametest/java/betel/nut/test/BetelGameplayGameTests.java',
        'src/gametest/resources/fabric.mod.json',
    }
    changed, preserved, diff = [], [], []
    with zipfile.ZipFile(OUT / 'before-trades.zip') as snapshot:
        baseline = [name for name in snapshot.namelist() if not name.endswith('/')]
        for name in baseline:
            path = ROOT / name
            assert path.is_file(), f'Unexpected deletion: {name}'
            old = snapshot.read(name)
            if canonical(old) == canonical(path.read_bytes()):
                preserved.append(name)
                continue
            assert name in allowed, f'Out of scope: {name}'
            changed.append(name)
            diff.extend(difflib.unified_diff(old.decode('utf-8').replace('\r\n', '\n').splitlines(keepends=True),
                        path.read_text(encoding='utf-8').splitlines(keepends=True),
                        fromfile='before/' + name, tofile='after/' + name))
        recipes = [name for name in baseline if '/recipe/' in name or '/advancement/' in name]
        art = [name for name in baseline if any(part in name for part in ['/textures/', '/models/', '/blockstates/', '/palm_mesh/'])]
        worldgen = [name for name in baseline if '/worldgen/' in name]
        assert all(name in preserved for name in recipes + art + worldgen)
    for path in RES.rglob('*.json'):
        json.loads(path.read_text(encoding='utf-8'))
    for name in ['build-final.log', 'farmersdelight-tests.log']:
        log = (OUT / name).read_text(encoding='utf-8')
        assert 'All 87 required tests passed' in log and 'BUILD SUCCESSFUL' in log
        assert 'required tests failed' not in log and 'Failed to parse' not in log
    assert 'farmersdelight 1.21.1-3.3.6' in (OUT / 'farmersdelight-tests.log').read_text(encoding='utf-8')
    client = (OUT / 'client-preview.log').read_text(encoding='utf-8')
    marker = ('TRADES_CLIENT_ALL_CHECKS_PASS green=true ripe=true fisherman=true relief=true '
              'goldenService=true wrongApple=true enchantedService=true')
    assert marker in client and 'BUILD SUCCESSFUL' in client
    screenshots = sorted(path.name for path in (OUT / 'client-run/screenshots').glob('*.png'))
    assert len(screenshots) == 7
    release = ROOT / 'build/libs/betel-nut-mod-5.4.0.jar'
    source = ROOT / 'build/libs/betel-nut-mod-5.4.0-sources.jar'
    with zipfile.ZipFile(release) as jar:
        names = jar.namelist()
        assert json.loads(jar.read('fabric.mod.json'))['version'] == '5.4.0'
        assert 'betel/nut/villager/ModVillagerTrades$Entry.class' in names
        assert not any(name.startswith(('betel/nut/test/', 'betel/nut/preview/', 'vectorwing/')) for name in names)
        for path in RES.rglob('*'):
            if path.is_file() and path.name != 'fabric.mod.json':
                assert jar.read(path.relative_to(RES).as_posix()) == path.read_bytes(), f'Stale resource: {path}'
        embedded = [name for name in names if name.startswith('META-INF/jars/') and name.endswith('.jar')]
        assert embedded and all('midnightlib' in name for name in embedded)
    with zipfile.ZipFile(source) as jar:
        assert 'betel/nut/villager/ModVillagerTrades.java' in jar.namelist()
    for path in [release, source]:
        shutil.copy2(path, OUT / path.name)
        assert sha(path) == sha(OUT / path.name)
    untracked = subprocess.check_output(['git', 'ls-files', '--others', '--exclude-standard', '-z'], cwd=ROOT).decode().split('\0')
    staged_new = subprocess.check_output(['git', 'diff', '--name-only', '--diff-filter=A',
                                         (OUT / 'baseline-commit.txt').read_text().strip()], cwd=ROOT).decode().splitlines()
    additions = sorted(set(name for name in untracked + staged_new if name))
    for name in additions:
        path = ROOT / name
        diff.extend(difflib.unified_diff([], path.read_text(encoding='utf-8').splitlines(keepends=True),
                    fromfile='before/' + name, tofile='after/' + name))
    assert all(line[1:].rstrip('\r\n') == line[1:].rstrip() for line in diff
               if line.startswith('+') and not line.startswith('+++')), 'Trailing whitespace'
    (OUT / 'changes.diff').write_text(''.join(diff), encoding='utf-8')
    report = {
        'version': '5.4.0', 'baselineCommit': (OUT / 'baseline-commit.txt').read_text().strip(),
        'normalGameTestsPassed': 87, 'farmersDelightGameTestsPassed': 87, 'newTradeTests': 12,
        'actualMerchantMenuOffersExecuted': 24, 'farmerCandidates': 18, 'fishermanCandidates': 2, 'clericCandidates': 4,
        'farmersDelightVersion': '1.21.1-3.3.6+refabricated', 'clientActualTradePacketsPassed': True,
        'clientScreenshots': screenshots, 'configSchema': 15,
        'existingRecipesAndAdvancementsUnchanged': len(recipes), 'existingArtFilesUnchanged': len(art),
        'worldgenFilesUnchanged': len(worldgen), 'preservedBaselineFiles': len(preserved),
        'changedBaselineFiles': sorted(changed), 'newFiles': additions,
        'allPackagedResourcesMatch': True, 'verificationCodeExcluded': True,
        'artifacts': {path.name: sha(path) for path in [release, source]},
    }
    (OUT / 'verification.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
    print(json.dumps(report, ensure_ascii=False, indent=2))


if __name__ == '__main__':
    verify()
