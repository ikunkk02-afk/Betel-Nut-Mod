"""Verify delivered source, resources, runtime evidence and the distributable JAR."""
from pathlib import Path
from zipfile import ZipFile
import hashlib
import io
import json
import re

ROOT = Path(__file__).resolve().parents[1]
SOURCE = ROOT.parent / 'Betel-Nut-Mod-main'
VERSION = re.search(r'^mod_version=(.+)$', (ROOT / 'gradle.properties').read_text(encoding='utf-8'), re.M).group(1).strip()
JAR = ROOT / f'build/libs/betel-nut-mod-neoforge-{VERSION}.jar'
SOURCES = JAR.with_name(JAR.stem + '-sources.jar')


def sha(data):
    return hashlib.sha256(data).hexdigest()


def condition(value):
    if value['condition'] == 'fabric:all_mods_loaded':
        children = [{'type': 'neoforge:mod_loaded', 'modid': name} for name in value['values']]
        return children[0] if len(children) == 1 else {'type': 'neoforge:and', 'values': children}
    if value['condition'] == 'fabric:not':
        return {'type': 'neoforge:not', 'value': condition(value['value'])}
    raise AssertionError(value)


def evidence(name, marker):
    path = ROOT / 'verification/create-5.7.1' / name
    if not path.exists():
        path = ROOT / name
    data = path.read_text(encoding='utf-8', errors='replace')
    assert marker in data, f'Missing completion marker in {name}'
    assert 'Parsing error loading recipe' not in data, name
    return {'log': str(path.relative_to(ROOT)), 'sha256': sha(path.read_bytes()), 'marker': marker}


report = {'version': VERSION, 'minecraft': '1.21.1', 'neoforge': '21.1.259',
          'verificationDate': '2026-10-13', 'fabricSourceCommit': '45a37b8',
          'testedOptionalMods': {'farmersdelight': '1.3.4', 'create': '6.0.10'}}
report['evidence'] = [
    evidence('core-tests.log', 'All 126 required tests passed'),
    evidence('create-only-tests.log', 'All 126 required tests passed'),
    evidence('compat-tests.log', 'All 126 required tests passed'),
    evidence('client-smoke.log', 'PORT_CLIENT_ALL_CHECKS_PASS'),
    evidence('build.log', 'BUILD SUCCESSFUL'),
]
report['baselineValidation'] = {'version': '5.7.0', 'naturalWorldReport': 'verification/natural-world-report.json',
                                'note': 'Existing models, world generation and base gameplay are retained; recipe revision has new tests above.'}
resources = ROOT / 'src/main/resources'
count = 0
with ZipFile(JAR) as jar, ZipFile(SOURCES) as sources:
    assert jar.testzip() is None and sources.testzip() is None
    assert 'fabric.mod.json' not in jar.namelist()
    assert not any(name.startswith(('betel/nut/test/', 'betel/nut/probe/', 'net/fabricmc/', 'org/ladysnake/')) for name in jar.namelist())
    assert 'betel/nut/BetelNutMod.class' in jar.namelist()
    metadata = jar.read('META-INF/neoforge.mods.toml').decode()
    assert 'modId="betel_nut_mod"' in metadata and f'version="{VERSION}"' in metadata
    assert 'license="MIT"' in metadata
    assert jar.read('LICENSE') == (ROOT / 'LICENSE').read_bytes()
    embedded = json.loads(jar.read('META-INF/jarjar/metadata.json'))['jars']
    assert len(embedded) == 1 and embedded[0]['identifier']['artifact'] == 'midnightlib'
    with ZipFile(io.BytesIO(jar.read(embedded[0]['path']))) as library:
        assert library.testzip() is None and 'META-INF/neoforge.mods.toml' in library.namelist()
        assert 'midnightlib' in library.read('META-INF/neoforge.mods.toml').decode()
    for file in resources.rglob('*'):
        if file.is_file():
            rel = file.relative_to(resources).as_posix()
            assert jar.read(rel) == file.read_bytes(), f'Packaged resource differs: {rel}'
            if file.suffix == '.json':
                json.loads(file.read_text(encoding='utf-8'))
                assert 'fabric:load_conditions' not in file.read_text(encoding='utf-8')
            count += 1
    java = list((ROOT / 'src/main/java').rglob('*.java'))
    for file in java:
        data = file.read_text(encoding='utf-8')
        assert 'net.fabricmc' not in data and 'org.ladysnake' not in data
        rel = file.relative_to(ROOT / 'src/main/java').as_posix()
        assert sources.read(rel).decode().replace('\r\n', '\n') == data.replace('\r\n', '\n'), rel
    assert not any('PortSmokeClient' in name or 'NaturalWorldgenProbe' in name for name in sources.namelist())
report['packagedResources'] = count
report['productionJavaFiles'] = len(java)

# Compare every original gameplay resource, allowing only namespace/loader adapters.
compared = 0
if (SOURCE / 'src/main/resources').exists():
    for old in (SOURCE / 'src/main/resources').rglob('*'):
        if not old.is_file() or old.name == 'fabric.mod.json':
            continue
        rel = old.relative_to(SOURCE / 'src/main/resources').as_posix().replace('betel-nut-mod', 'betel_nut_mod')
        new = resources / rel
        assert new.is_file(), f'Missing original resource: {rel}'
        if old.suffix in ('.json', '.mcmeta'):
            expected = json.loads(old.read_text(encoding='utf-8').replace('betel-nut-mod', 'betel_nut_mod'))
            actual = json.loads(new.read_text(encoding='utf-8'))
            if 'fabric:load_conditions' in expected:
                expected['neoforge:conditions'] = [condition(c) for c in expected.pop('fabric:load_conditions')]
            if rel == 'betel_nut_mod.mixins.json':
                expected['mixins'].append('CreateAutomationRecipesMixin')
            if 'betel_mesh' in actual:
                assert actual['loader'] == 'betel_nut_mod:palm_mesh'
                mesh = json.loads((resources / 'assets/betel_nut_mod/palm_mesh' / new.name).read_text(encoding='utf-8'))
                assert actual['betel_mesh'] == mesh
                for key in ('loader', 'betel_mesh', 'render_type'):
                    actual.pop(key)
                actual['textures'] = {k: v for k, v in actual['textures'].items() if not k.startswith('mesh')}
            assert actual == expected, f'Unexpected gameplay resource change: {rel}'
        else:
            assert new.read_bytes() == old.read_bytes(), rel
        compared += 1
report['originalResourcesPreservedOrAdapted'] = compared
report['visibleAdvancements'] = sum('display' in json.loads(p.read_text(encoding='utf-8'))
                                    for p in (resources / 'data/betel_nut_mod/advancement').rglob('*.json'))
assert report['visibleAdvancements'] == 28
report['createRecipes'] = len(list((resources / 'data/betel_nut_mod/recipe/compat/create').glob('*.json')))
assert report['createRecipes'] == 33
natural = json.loads((ROOT / 'verification/natural-world-report.json').read_text(encoding='utf-8'))
assert len(natural['samples']) == 4 and all(sample['featureInjected'] for sample in natural['samples'])
roots = {tuple(root) for sample in natural['samples'] for root in sample['roots']}
assert roots
report['naturalWorldDistinctRoots'] = len(roots)
report['naturalWorldSampleRootBiomes'] = {s['biome']: s['actualRootBiomes'] for s in natural['samples']}
report['artifacts'] = [{'file': str(p.relative_to(ROOT)), 'bytes': p.stat().st_size,
                        'sha256': sha(p.read_bytes())} for p in (JAR, SOURCES)]
report['passed'] = True
(ROOT / 'verification/report.json').write_text(json.dumps(report, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
print(json.dumps(report, ensure_ascii=False, indent=2))
