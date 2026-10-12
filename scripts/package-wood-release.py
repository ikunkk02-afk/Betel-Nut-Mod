"""Package the verified wood-family release and verify resources against its built JAR."""
from pathlib import Path
import hashlib
import json
import re
import shutil
from zipfile import ZipFile

root = Path(__file__).resolve().parent.parent
out = root / "art-rework/wood-family"
version = re.search(r"^mod_version=(.+)$", (root / "gradle.properties").read_text(), re.M)[1].strip()
source = root / f"build/libs/betel-nut-mod-{version}.jar"
build_log = (out / "build.log").read_text(encoding="utf-8-sig")
client_log = (out / "client-preview-final.log").read_text(encoding="utf-8-sig")
assert "BUILD SUCCESSFUL" in build_log and "All 24 required tests passed" in build_log
assert "BUILD SUCCESSFUL" in client_log
markers = ["WOOD_MODELS_VERIFIED doorStates=64 itemModels=2", "WOOD_CLIENT_MANUAL_OPEN_VERIFIED",
           "WOOD_CLIENT_PLATE_ACTIVATION_VERIFIED", "WOOD_CLIENT_PLATE_RELEASE_VERIFIED"]
for marker in markers:
    assert marker in client_log, marker

with ZipFile(source) as jar:
    assert jar.testzip() is None
    assert not any("betel/nut/preview/" in p or "betel/nut/test/" in p for p in jar.namelist())
    assert json.loads(jar.read("fabric.mod.json"))["version"] == version
    checked = 0
    for file in (root / "src/main/resources").rglob("*"):
        if file.is_file() and file.name != "fabric.mod.json":
            name = file.relative_to(root / "src/main/resources").as_posix()
            assert jar.read(name) == file.read_bytes(), name
            checked += 1
    preserved_art = 0
    with ZipFile(root / "art-rework/palm-v3/betel-nut-mod-4.0.1-palm-v3.1-fabric-1.21.1.jar") as previous:
        for name in previous.namelist():
            if name.startswith("assets/betel-nut-mod/") and not name.endswith("/"):
                if "/lang/" in name:
                    old, new = json.loads(previous.read(name)), json.loads(jar.read(name))
                    assert all(new.get(key) == value for key, value in old.items()), name
                else:
                    assert jar.read(name) == previous.read(name), name
                preserved_art += 1

release = out / f"betel-nut-mod-{version}-wood-family-fabric-1.21.1.jar"
shutil.copy2(source, release)
previews = out / "previews"
previews.mkdir(exist_ok=True)
screenshots = sorted((root / "art-rework/client-run/screenshots").glob("wood-*.png"))
assert len(screenshots) == 6
for file in screenshots:
    shutil.copy2(file, previews / file.name)

report = {
    "release": release.name,
    "sha256": hashlib.sha256(release.read_bytes()).hexdigest(),
    "gameTestsPassed": 24,
    "newWoodGameTests": 10,
    "packagedResourceParityCount": checked,
    "preservedPreviousArtResources": preserved_art,
    "previewAndTestCodeExcluded": True,
    "clientVerificationMarkers": markers,
    "screenshots": [p.name for p in screenshots],
    "doorArtwork": {"method": "built-in imagegen", "master": "door-master.png", "prompt": "door-prompt.txt"},
}
(out / "verification.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report))
