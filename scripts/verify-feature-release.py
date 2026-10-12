"""Check the built artifact against real resources and save bounded verification evidence."""
from pathlib import Path
from io import BytesIO
import hashlib
import json
import re
import shutil
import zipfile
from PIL import Image

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'art-rework/features-v5'
RES = ROOT / 'src/main/resources'
ART = OUT / 'imagegen'
JAR = ROOT / 'build/libs/betel-nut-mod-5.0.0.jar'

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

manifest = json.loads((ART / 'manifest.json').read_text(encoding='utf-8'))
integration = json.loads((ART / 'integration.json').read_text(encoding='utf-8'))
assert len(manifest['assets']) == len(integration) == 15
with zipfile.ZipFile(JAR) as archive:
    assert archive.testzip() is None
    mod = json.loads(archive.read('fabric.mod.json'))
    assert mod['version'] == '5.0.0'
    assert not any('/preview/' in n or '/test/' in n or 'gametest' in n.lower() for n in archive.namelist())
    for a in integration:
        texture = ROOT / a['texture']
        assert digest(texture) == a['sha256']
        assert archive.read(texture.relative_to(RES).as_posix()) == texture.read_bytes()
        image = Image.open(BytesIO(texture.read_bytes()))
        image.load()
        assert image.mode == 'RGBA' and image.size == (a['size'], a['size'])
        alpha_min, alpha_max = image.getextrema()[3]
        assert alpha_min == 0 and alpha_max >= 250
        original = Image.open(ROOT / a['original'])
        original.load()
        assert original.mode == 'RGBA' and original.size == (1254, 1254)
        assert original.resize(image.size, Image.Resampling.NEAREST).tobytes() == image.tobytes()
    recipe_names = ['shell_powder','wrapped_raw_betel','echo_betel','empty_pack',
                    'kouweiwang_pack','hecheng_tianxia_pack','xiangtan_puzi_pack']
    for name in recipe_names:
        for relative in [f'data/betel-nut-mod/recipe/{name}.json',
                         f'data/betel-nut-mod/advancement/recipes/{name}.json']:
            assert archive.read(relative) == (RES / relative).read_bytes()

json_count = 0
for path in RES.rglob('*.json'):
    json.loads(path.read_text(encoding='utf-8')); json_count += 1
languages = {}
for lang in ['zh_cn','en_us']:
    languages[lang] = json.loads((RES / f'assets/betel-nut-mod/lang/{lang}.json').read_text(encoding='utf-8'))
    for a in integration:
        category = 'effect' if a['size'] == 18 else 'item'
        assert f'{category}.betel-nut-mod.{a["name"]}' in languages[lang]
    for stage in range(4): assert f'hud.betel-nut-mod.oral.{stage}' in languages[lang]
    for state in range(3): assert f'hud.betel-nut-mod.mouth.{state}' in languages[lang]
    for prize in range(5): assert f'message.betel-nut-mod.lottery.{prize}' in languages[lang]
assert set(languages['zh_cn']) == set(languages['en_us']), 'Localization key mismatch'

baseline_modified, missing = [], []
with zipfile.ZipFile(OUT / 'before-features.zip') as baseline:
    for name in baseline.namelist():
        if name.endswith('/'): continue
        path = ROOT / name
        if not path.exists(): missing.append(name)
        elif path.read_bytes() != baseline.read(name): baseline_modified.append(name)
assert not missing, missing
build_log = (OUT / 'build.log').read_text(encoding='utf-8',errors='replace')
assert 'BUILD SUCCESSFUL' in build_log and 'All 47 required tests passed' in build_log
client_log = (OUT / 'client-preview.log').read_text(encoding='utf-8',errors='replace')
markers = ['FEATURE_NETWORK_PACK_AND_LOTTERY_PASS','FEATURE_NETWORK_LOCKED_JAW_PASS',
           'FEATURE_CLIENT_RECOVERY_PASS','FEATURE_CLIENT_ALL_CHECKS_PASS']
assert all(m in client_log for m in markers) and 'BUILD SUCCESSFUL' in client_log
assert not re.search(r'(?:\bERROR\b|Missing model|Unable to load model|Failed to load)', client_log)
screenshots = sorted(p.name for p in (OUT / 'client-run/screenshots').glob('*.png'))
assert len(screenshots) >= 8

shutil.copy2(JAR, OUT / JAR.name)
shutil.copy2(ROOT / 'build/libs/betel-nut-mod-5.0.0-sources.jar', OUT / 'betel-nut-mod-5.0.0-sources.jar')
asset_zip = OUT / 'betel-nut-v5-imagegen-assets.zip'
with zipfile.ZipFile(asset_zip, 'w', zipfile.ZIP_DEFLATED) as archive:
    for a in integration:
        original = ROOT / a['original']
        archive.write(original, f'originals/{original.name}')
        texture = ROOT / a['texture']
        archive.write(texture, 'game-textures/' + texture.relative_to(RES).as_posix())
    for path in [ART / 'manifest.json', ART / 'integration.json', OUT / 'item-contact-sheet.png', OUT / 'README.md']:
        archive.write(path, path.name)
    archive.write(ROOT / 'scripts/integrate-feature-imagegen.py', 'integrate-feature-imagegen.py')
with zipfile.ZipFile(asset_zip) as archive: assert archive.testzip() is None

report = {'version': '5.0.0', 'gameTestsPassed': 47, 'clientChecks': markers,
    'jsonResourcesParsed': json_count, 'imagegenTexturesMatchedInJar': len(integration),
    'originalAlphaPreserved': True, 'recipeAndUnlockPairsVerified': len(recipe_names),
    'bilingualKeyParity': True, 'developmentClassesExcludedFromRelease': True,
    'baselineFilesMissing': missing, 'baselineFilesModified': baseline_modified,
    'screenshots': screenshots, 'jarSha256': digest(JAR), 'assetZipSha256': digest(asset_zip),
    'limits': ['No two-independent-client dedicated-server session', 'No installed Farmers Delight integration playtest']}
(OUT / 'verification.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n',encoding='utf-8')
print(json.dumps({k:v for k,v in report.items() if k not in {'baselineFilesModified','screenshots'}}, ensure_ascii=False, indent=2))
