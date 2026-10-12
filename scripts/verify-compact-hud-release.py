"""Verify the 5.0.1 artifact and retain evidence for this gameplay/HUD change."""
from pathlib import Path
from io import BytesIO
import hashlib
import json
import re
import shutil
import zipfile
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'art-rework/compact-hud-v5.0.1'
RES = ROOT / 'src/main/resources'
JAR = ROOT / 'build/libs/betel-nut-mod-5.0.1.jar'

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

log = (OUT / 'final-build-and-client.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in log and 'All 50 required tests passed' in log
assert 'COMPACT_HUD_CLIENT_ALL_CHECKS_PASS screenshots=11' in log
assert not re.search(r'\bERROR\b|Missing model|Unable to load model|Failed to load', log)
client_log = (OUT / 'final-client.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in client_log and 'COMPACT_HUD_CLIENT_ALL_CHECKS_PASS screenshots=11' in client_log
assert not re.search(r'\bERROR\b|Missing model|Unable to load model|Failed to load', client_log)

languages = {lang: json.loads((RES / f'assets/betel-nut-mod/lang/{lang}.json').read_text(encoding='utf-8'))
             for lang in ['zh_cn', 'en_us']}
assert set(languages['zh_cn']) == set(languages['en_us'])
for table in languages.values():
    for stage in range(6):
        assert f'hud.betel-nut-mod.compact.stage.{stage}' in table
    for name in ['days', 'oral', 'mouth.1', 'mouth.2', 'withdrawal', 'legend']:
        assert f'hud.betel-nut-mod.compact.{name}' in table

resources = list(RES.rglob('*.json'))
for path in resources:
    json.loads(path.read_text(encoding='utf-8'))
integration = json.loads((ROOT / 'art-rework/features-v5/imagegen/integration.json').read_text(encoding='utf-8'))
with zipfile.ZipFile(JAR) as jar:
    assert jar.testzip() is None
    assert json.loads(jar.read('fabric.mod.json'))['version'] == '5.0.1'
    assert not any('/preview/' in name or '/test/' in name or 'gametest' in name.lower() for name in jar.namelist())
    for path in resources:
        relative = path.relative_to(RES).as_posix()
        if relative != 'fabric.mod.json':
            assert jar.read(relative) == path.read_bytes(), relative
    for asset in integration:
        texture = ROOT / asset['texture']
        assert digest(texture) == asset['sha256']
        assert jar.read(texture.relative_to(RES).as_posix()) == texture.read_bytes()
        with Image.open(BytesIO(texture.read_bytes())) as im:
            im.load()
            assert im.mode == 'RGBA' and im.size == (asset['size'], asset['size'])

sources = ROOT / 'build/libs/betel-nut-mod-5.0.1-sources.jar'
with zipfile.ZipFile(sources) as jar, zipfile.ZipFile(ROOT / 'build/devlibs/betel-nut-mod-5.0.1-sources.jar') as named:
    assert jar.testzip() is None
    assert not any('/preview/' in name or '/test/' in name for name in jar.namelist())
    for path in list((ROOT / 'src/main/java').rglob('*.java')) + list((ROOT / 'src/client/java').rglob('*.java')):
        relative = path.as_posix().split('/java/', 1)[1]
        assert relative in jar.namelist(), relative
        # Loom deliberately remaps Minecraft names in the published source JAR.
        assert named.read(relative).replace(b'\r\n', b'\n') == path.read_bytes().replace(b'\r\n', b'\n'), relative

changed = []
with zipfile.ZipFile(OUT / 'before-compact-hud.zip') as before:
    for name in before.namelist():
        if name.endswith('/'): continue
        path = ROOT / name.replace('\\', '/')
        assert path.is_file(), name
        if path.read_bytes().replace(b'\r\n', b'\n') != before.read(name).replace(b'\r\n', b'\n'):
            changed.append(name)
            if path.suffix in {'.java', '.json', '.md', '.gradle', '.properties'}:
                text = path.read_text(encoding='utf-8')
                assert not any(re.match(r'^(<<<<<<<|=======|>>>>>>>)', line) for line in text.splitlines())

screenshots = sorted((OUT / 'client-run/screenshots').glob('*.png'))
assert len(screenshots) == 11
for path in screenshots:
    with Image.open(path) as im:
        im.load()
        assert im.size == (1600, 1000)

for path in [JAR, sources]:
    shutil.copy2(path, OUT / path.name)
report = {
    'version': '5.0.1', 'defaultFullDependenceEatingDays': 7, 'supportedRange': [5, 8],
    'defaultStageThresholds': [2, 3, 4, 5, 7], 'gameTestsPassed': 50,
    'clientChecks': 'COMPACT_HUD_CLIENT_ALL_CHECKS_PASS', 'jsonResourcesParsed': len(resources),
    'unchangedImagegenTexturesVerified': len(integration), 'bilingualKeyParity': True,
    'developmentClassesExcludedFromBothJars': True, 'namedSourceJarMatchesWorkingSource': True,
    'originalSnapshotFilesMissing': [], 'changedFiles': changed,
    'screenshots': [path.name for path in screenshots], 'jarSha256': digest(JAR),
    'sourcesJarSha256': digest(sources),
    'limits': ['Client checks use a copied development world with an integrated server, not two independent clients.',
               'Double-scale screenshot intentionally exercises a user-selected HUD scale of 2; default scale is 1.']
}
(OUT / 'verification.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps({k: v for k, v in report.items() if k not in {'screenshots', 'changedFiles'}}, ensure_ascii=False, indent=2))
