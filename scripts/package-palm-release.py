"""Package the verified V3.1 visuals without including development-only preview code."""
from pathlib import Path
import hashlib
import json
import shutil
import zipfile

root = Path(__file__).resolve().parent.parent
out = root / "art-rework/palm-v3"
assets = root / "src/main/resources/assets/betel-nut-mod"
source_jar = root / "build/libs/betel-nut-mod-4.0.1.jar"
log = (out / "visual-fixes-client-final.log").read_text(encoding="utf-8-sig")
assert "BUILD SUCCESSFUL" in log
assert "ART_ITEM_MESH_VERIFIED quads=746 model=PalmModel" in log

with zipfile.ZipFile(source_jar) as jar:
    assert jar.testzip() is None
    assert not any("betel/nut/preview/" in p or "betel/nut/test/" in p for p in jar.namelist())
    checked = 0
    for folder in ["palm_mesh", "models/item"]:
        for file in (assets / folder).glob("*.json"):
            assert jar.read("assets/betel-nut-mod/" + file.relative_to(assets).as_posix()) == file.read_bytes()
            checked += 1

release = out / "betel-nut-mod-4.0.1-palm-v3.1-fabric-1.21.1.jar"
shutil.copy2(source_jar, release)
previews = out / "previews"
previews.mkdir(exist_ok=True)
screenshots = list((root / "art-rework/client-run/screenshots").glob("fix-*.png"))
assert len(screenshots) == 7
for file in screenshots:
    shutil.copy2(file, previews / file.name)

archive = out / "areca-palm-v3.1-editable-models.zip"
with zipfile.ZipFile(archive, "w", zipfile.ZIP_DEFLATED) as z:
    for pattern in ["*.bbmodel", "betel_*.json", "mesh/*.bbmodel", "mesh/*.json",
                    "assets/**/*.png", "previews/*.png", "README.md", "design.json", "materials-master.png"]:
        for file in sorted(out.glob(pattern)):
            z.write(file, file.relative_to(out).as_posix())
    for name in ["build-palm-v3.cjs", "integrate-blockbench-palm.cjs", "export-palm-mesh.cjs", "check-palm-v3.cjs"]:
        z.write(root / "scripts" / name, "scripts/" + name)
    for name in ["betel_palm_leaves", "betel_nut_cluster"]:
        z.write(assets / "models/item" / (name + ".json"), "item-models/" + name + ".json")
with zipfile.ZipFile(archive) as z:
    assert z.testzip() is None

report = {
    "release": release.name,
    "sha256": hashlib.sha256(release.read_bytes()).hexdigest(),
    "packagedJsonParityCount": checked,
    "previewCodeExcluded": True,
    "clientItemMesh": {"model": "PalmModel", "quads": 746},
    "screenshots": [p.name for p in screenshots],
    "editableArchive": archive.name,
}
(out / "visual-fixes-verification.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report))
