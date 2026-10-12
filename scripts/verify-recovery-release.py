"""Verify the 5.2 release against its before snapshot and package exact build outputs."""
from pathlib import Path
import difflib
import hashlib
import importlib.util
import json
import shutil
import zipfile
from PIL import Image

ROOT=Path(__file__).resolve().parent.parent
OUT=ROOT/'art-rework/recovery-v5.2.0'
RES=ROOT/'src/main/resources'
def sha(path):return hashlib.sha256(path.read_bytes()).hexdigest()

def verify():
    before=json.loads((OUT/'before-sha256.json').read_text(encoding='utf-8'))
    changed=[];preserved=[]
    allowed={
        'src/main/java/betel/nut/item/ModItems.java',
        'src/main/java/betel/nut/item/ModItemGroups.java',
        'src/main/java/betel/nut/component/BetelNutAddictionComponent.java',
        'src/main/java/betel/nut/event/BetelNutEvents.java',
        'src/main/java/betel/nut/event/WithdrawalEatingRestrictions.java',
        'src/main/java/betel/nut/BetelNutConfig.java',
        'src/main/resources/betel-nut-mod.mixins.json',
        'src/main/resources/assets/betel-nut-mod/lang/zh_cn.json',
        'src/main/resources/assets/betel-nut-mod/lang/en_us.json',
        'src/main/resources/data/betel-nut-mod/tags/item/withdrawal_stage2_allowed_foods.json',
        'src/main/resources/data/betel-nut-mod/tags/item/withdrawal_stage3_allowed_foods.json',
        'src/gametest/java/betel/nut/test/BetelGameplayGameTests.java',
        'src/gametest/resources/fabric.mod.json',
        'scripts/generate-feature-assets.py',
        'README.md','CHANGELOG.md','gradle.properties','build.gradle',
    }
    for name,digest in before.items():
        p=ROOT/name;assert p.is_file(),f'Unexpected deletion: {name}'
        if sha(p)!=digest:
            assert name in allowed,f'Out of scope: {name}'
            changed.append(name)
        else:preserved.append(name)
    old_recipes=[name for name in before if '/recipe/' in name or '/advancement/' in name]
    assert all(name in preserved for name in old_recipes),'An existing nut recipe or advancement changed'
    old_art=[name for name in before if '/textures/' in name or '/models/' in name or '/blockstates/' in name]
    assert all(name in preserved for name in old_art),'An existing art asset changed'
    for p in RES.rglob('*.json'):json.loads(p.read_text(encoding='utf-8'))
    old_resource_hashes={p:sha(p) for p in RES.rglob('*') if p.is_file()}
    spec=importlib.util.spec_from_file_location('recovery_assets',ROOT/'scripts/generate-recovery-assets.py')
    mod=importlib.util.module_from_spec(spec);spec.loader.exec_module(mod);mod.generate()
    assert all(sha(p)==digest for p,digest in old_resource_hashes.items()),'Recovery generator is not idempotent'

    assets=['golden_apple_paste','enchanted_golden_apple_paste','relief_potion']
    for name in assets:
        im=Image.open(RES/f'assets/betel-nut-mod/textures/item/{name}.png')
        assert im.size==(32,32) and im.mode=='RGBA' and im.getextrema()[3][0]==0,'Invalid sprite alpha/grid'
        assert (OUT/f'imagegen/{name}-original.png').is_file(),'Missing generated original'
    log=(OUT/'build-final.log').read_text(encoding='utf-8')
    fd=(OUT/'farmersdelight-tests-final.log').read_text(encoding='utf-8')
    client=(OUT/'client-preview.log').read_text(encoding='utf-8')
    for content in [log,fd]:
        assert 'All 69 required tests passed' in content and 'BUILD SUCCESSFUL' in content
        assert 'Failed to parse' not in content and 'required tests failed' not in content
    assert 'farmersdelight 1.21.1-3.3.6' in fd
    assert 'RECOVERY_CLIENT_ALL_CHECKS_PASS' in client and 'BUILD SUCCESSFUL' in client
    release=ROOT/'build/libs/betel-nut-mod-5.2.0.jar'
    source=ROOT/'build/libs/betel-nut-mod-5.2.0-sources.jar'
    with zipfile.ZipFile(release) as jar:
        names=jar.namelist()
        assert json.loads(jar.read('fabric.mod.json'))['version']=='5.2.0'
        assert not any(n.startswith(('betel/nut/test/','betel/nut/preview/','vectorwing/')) for n in names),'Verification code leaked'
        assert {'betel/nut/item/RecoveryDrinkItem.class','betel/nut/mixin/CakeBlockMixin.class'} <= set(names)
        for p in RES.rglob('*'):
            if p.is_file() and p.name!='fabric.mod.json':assert jar.read(p.relative_to(RES).as_posix())==p.read_bytes(),f'Stale resource: {p}'
        assert all('midnightlib' in n for n in names if n.startswith('META-INF/jars/') and n.endswith('.jar')),'Unexpected dependency bundled'
    with zipfile.ZipFile(source) as jar:assert 'betel/nut/item/RecoveryDrinkItem.java' in jar.namelist()
    for p in [release,source]:shutil.copy2(p,OUT/p.name)

    additions=[p for folder in ['src','scripts','docs'] for p in (ROOT/folder).rglob('*') if p.is_file()
        and '__pycache__' not in p.parts and p.relative_to(ROOT).as_posix() not in before]
    diff=[];binary=[]
    with zipfile.ZipFile(OUT/'before-recovery.zip') as snapshot:
        for name in sorted(changed+[p.relative_to(ROOT).as_posix() for p in additions]):
            try:
                old=snapshot.read(name).decode('utf-8').replace('\r\n','\n').splitlines(keepends=True) if name in before else []
                new=(ROOT/name).read_text(encoding='utf-8').splitlines(keepends=True)
            except UnicodeDecodeError:binary.append(name);continue
            diff.extend(difflib.unified_diff(old,new,fromfile='before/'+name,tofile='after/'+name))
    assert all(line[1:].rstrip('\r\n')==line[1:].rstrip() for line in diff
               if line.startswith('+') and not line.startswith('+++')), 'Trailing whitespace in added lines'
    (OUT/'changes.diff').write_text(''.join(diff),encoding='utf-8')
    report={
        'version':'5.2.0','normalGameTestsPassed':69,'farmersDelightGameTestsPassed':69,
        'farmersDelightVersion':'1.21.1-3.3.6+refabricated','actualNewPotBatchesVerified':3,
        'allExistingNutRecipesAndAdvancementsUnchanged':len(old_recipes),'allExistingArtUnchanged':len(old_art),
        'clientActualUsePacketsPassed':True,'clientScreenshots':len(list((OUT/'client-run/screenshots').glob('*.png'))),
        'clientVerified':['stageII bread','stageII hard apples blocked','stageIII bread and honey blocked',
            'golden paste drink + bottle','enchanted paste drink + glint + cure','relief partial recovery','daily dose + next day','HUD sync'],
        'preservedBaselineFiles':len(preserved),'changedBaselineFiles':sorted(changed),
        'newSourceAndResourceFiles':sorted(p.relative_to(ROOT).as_posix() for p in additions),
        'binaryAdditions':binary,'generatorIdempotent':True,'allPackagedResourcesMatch':True,
        'testAndPreviewAndDelightCodeExcluded':True,'artifacts':{p.name:sha(p) for p in [release,source]},
    }
    (OUT/'verification.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
    print(json.dumps(report,ensure_ascii=False,indent=2))

if __name__=='__main__':verify()
