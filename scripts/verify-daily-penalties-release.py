"""Validate the day-based negative-effect release and preserve its evidence."""
from pathlib import Path
from io import BytesIO
import hashlib
import json
import re
import shutil
import zipfile
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'art-rework/daily-penalties-v5.0.2'
RES = ROOT / 'src/main/resources'
VERSION = '5.0.2'
JAR = ROOT / f'build/libs/betel-nut-mod-{VERSION}.jar'

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

build = (OUT / 'build.log').read_text(encoding='utf-8', errors='replace')
client = (OUT / 'client-preview.log').read_text(encoding='utf-8', errors='replace')
assert 'BUILD SUCCESSFUL' in build and 'All 53 required tests passed' in build
assert 'BUILD SUCCESSFUL' in client and 'DAILY_PENALTIES_CLIENT_ALL_CHECKS_PASS' in client
for day in range(1, 8):
    assert f'DAILY_PENALTIES_MEAL_PASS day={day}' in client
assert not re.search(r'\bERROR\b|Unable to load model|Failed to load|Missing texture', client)

tables = [json.loads((RES / f'assets/betel-nut-mod/lang/{lang}.json').read_text(encoding='utf-8')) for lang in ['zh_cn', 'en_us']]
assert set(tables[0]) == set(tables[1])
for table in tables:
    assert 'effect.betel-nut-mod.betel_dependence' in table
    for stage in range(1, 6):
        assert f'hud.betel-nut-mod.symptoms.{stage}' in table

art = json.loads((OUT / 'imagegen/integration.json').read_text(encoding='utf-8'))
original = ROOT / art['original']; texture = ROOT / art['texture']
assert digest(original) == art['originalSha256'] and digest(texture) == art['textureSha256']
with Image.open(original) as source, Image.open(texture) as sprite:
    source.load(); sprite.load()
    assert source.mode == sprite.mode == 'RGBA' and sprite.size == (18, 18)
    assert sprite.getextrema()[3][0] == 0 and sprite.getextrema()[3][1] == 255
    assert source.resize((18, 18), Image.Resampling.NEAREST).tobytes() == sprite.tobytes()

resources = list(RES.rglob('*.json'))
with zipfile.ZipFile(JAR) as jar:
    assert jar.testzip() is None
    assert json.loads(jar.read('fabric.mod.json'))['version'] == VERSION
    assert not any('/preview/' in name or '/test/' in name or 'gametest' in name.lower() for name in jar.namelist())
    for path in resources:
        json.loads(path.read_text(encoding='utf-8'))
        if path.name != 'fabric.mod.json':
            assert jar.read(path.relative_to(RES).as_posix()) == path.read_bytes(), path
    assert jar.read(texture.relative_to(RES).as_posix()) == texture.read_bytes()
    for asset in json.loads((ROOT / 'art-rework/features-v5/imagegen/integration.json').read_text(encoding='utf-8')):
        old = ROOT / asset['texture']
        assert digest(old) == asset['sha256'] and jar.read(old.relative_to(RES).as_posix()) == old.read_bytes()

sources = ROOT / f'build/libs/betel-nut-mod-{VERSION}-sources.jar'
with zipfile.ZipFile(sources) as published, zipfile.ZipFile(ROOT / f'build/devlibs/betel-nut-mod-{VERSION}-sources.jar') as named:
    assert published.testzip() is None
    assert not any('/preview/' in name or '/test/' in name for name in published.namelist())
    for path in list((ROOT / 'src/main/java').rglob('*.java')) + list((ROOT / 'src/client/java').rglob('*.java')):
        relative = path.as_posix().split('/java/', 1)[1]
        assert relative in published.namelist()
        assert named.read(relative).replace(b'\r\n', b'\n') == path.read_bytes().replace(b'\r\n', b'\n'), relative

changed = []
with zipfile.ZipFile(OUT / 'before-daily-penalties.zip') as before:
    for name in before.namelist():
        if name.endswith('/'): continue
        path = ROOT / name.replace('\\', '/')
        assert path.is_file(), name
        if path.read_bytes().replace(b'\r\n', b'\n') != before.read(name).replace(b'\r\n', b'\n'):
            changed.append(name)

shots = sorted((OUT / 'client-run/screenshots').glob('*.png'))
assert len(shots) == 13, len(shots)
for shot in shots:
    with Image.open(shot) as image:
        image.load(); assert image.size == (1600, 1000)
for path in [JAR, sources]:
    shutil.copy2(path, OUT / path.name)
report = dict(version=VERSION, gameTestsPassed=53, actualClientMealsVerified=7,
    clientMarker='DAILY_PENALTIES_CLIENT_ALL_CHECKS_PASS', jarSha256=digest(JAR),
    sourcesSha256=digest(sources), jsonResourcesParsed=len(resources),
    bilingualKeyParity=True, namedSourcesMatch=True, devClassesExcluded=True,
    imagegenSpriteAlphaAndJarVerified=True, existingImagegenTexturesUnchanged=15,
    changedFiles=changed, screenshots=[path.name for path in shots],
    limitation='Client checks use an integrated server in a copied test save, not two independent clients.')
(OUT / 'verification.json').write_text(json.dumps(report, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k not in {'changedFiles','screenshots'}}, ensure_ascii=False, indent=2))
