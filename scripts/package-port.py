"""Create a clean source archive and copy the verified release artifacts."""
from pathlib import Path
from zipfile import ZipFile, ZIP_DEFLATED
import hashlib
import json
import shutil

root = Path(__file__).resolve().parents[1]
report = json.loads((root / 'verification/report.json').read_text(encoding='utf-8'))
assert report['passed']
dist = root / 'dist'
dist.mkdir(exist_ok=True)
artifacts = []
for entry in report['artifacts']:
    file = root / entry['file']
    assert hashlib.sha256(file.read_bytes()).hexdigest() == entry['sha256']
    shutil.copy2(file, dist / file.name)
    artifacts.append({'file': file.name, 'bytes': file.stat().st_size, 'sha256': entry['sha256']})
archive = dist / ('betel-nut-mod-neoforge-' + report['version'] + '-project.zip')
paths = []
for directory in ('src', 'gradle', 'docs', 'scripts', '.github', 'verification'):
    paths.extend(p for p in (root / directory).rglob('*') if p.is_file() and '__pycache__' not in p.parts)
paths.extend(root / name for name in ('build.gradle', 'settings.gradle', 'gradle.properties',
                                     'gradlew', 'gradlew.bat', 'LICENSE', 'README.md', '.gitignore'))
with ZipFile(archive, 'w', ZIP_DEFLATED, compresslevel=9) as zip:
    for path in sorted(paths):
        zip.write(path, path.relative_to(root).as_posix())
with ZipFile(archive) as zip:
    assert zip.testzip() is None
    assert len(zip.namelist()) == len(paths)
    assert zip.read('src/main/java/betel/nut/BetelNutMod.java') == (root / 'src/main/java/betel/nut/BetelNutMod.java').read_bytes()
artifacts.append({'file': archive.name, 'bytes': archive.stat().st_size,
                  'sha256': hashlib.sha256(archive.read_bytes()).hexdigest()})
(dist / 'manifest.json').write_text(json.dumps({'version': report['version'], 'artifacts': artifacts}, indent=2) + '\n', encoding='utf-8')
print(json.dumps(artifacts, indent=2))
