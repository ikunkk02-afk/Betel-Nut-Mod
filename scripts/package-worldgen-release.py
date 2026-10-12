"""Package the jungle-groves release after server, client, and resource checks."""
from pathlib import Path
import hashlib
import json
import re
import shutil
from zipfile import ZipFile

root = Path(__file__).resolve().parent.parent
out = root / "art-rework/worldgen"
version = re.search(r"^mod_version=(.+)$", (root / "gradle.properties").read_text(), re.M)[1].strip()
source = root / f"build/libs/betel-nut-mod-{version}.jar"
build_log = (out / "build.log").read_text(encoding="utf-8-sig")
probe_log = (out / "natural-probe.log").read_text(encoding="utf-8-sig")
client_log = (out / "client-preview.log").read_text(encoding="utf-8-sig")
assert "BUILD SUCCESSFUL" in build_log and "All 33 required tests passed" in build_log
assert "BUILD SUCCESSFUL" in probe_log and "NATURAL_WORLDGEN_PROBE_PASSED" in probe_log
assert "BUILD SUCCESSFUL" in client_log and "GROVE_CLIENT_NATURAL_WORLD_VERIFIED" in client_log
for log in (probe_log, client_log):
    for failure in ("NATURAL_WORLDGEN_PROBE_FAILED", "Natural grove preview failed", "Detected setBlock in a far chunk", "Exception generating new chunk"):
        assert failure not in log, failure
natural = json.loads((out / "natural-world-report.json").read_text())
assert len(natural["samples"]) == 4 and all(s["featureInjected"] for s in natural["samples"])
assert natural["spawnChance"] == .08 and natural["groveAttemptsPerChunk"] == 1
unique_roots = {tuple(pos) for sample in natural["samples"] for pos in sample["roots"]}
assert unique_roots

with ZipFile(source) as jar:
    assert jar.testzip() is None
    assert not any(any(prefix in name for prefix in ("betel/nut/preview/", "betel/nut/probe/", "betel/nut/test/")) for name in jar.namelist())
    assert "betel/nut/worldgen/BetelPalmGroveGenerator.class" in jar.namelist()
    assert json.loads(jar.read("fabric.mod.json"))["version"] == version
    checked = 0
    for file in (root / "src/main/resources").rglob("*"):
        if file.is_file() and file.name != "fabric.mod.json":
            name = file.relative_to(root / "src/main/resources").as_posix()
            assert jar.read(name) == file.read_bytes(), name
            checked += 1
    preserved_art = preserved_data = 0
    with ZipFile(root / "art-rework/wood-family/betel-nut-mod-4.0.2-wood-family-fabric-1.21.1.jar") as previous:
        for name in previous.namelist():
            if name.endswith("/"):
                continue
            if name.startswith("assets/"):
                assert jar.read(name) == previous.read(name), name
                preserved_art += 1
            if name.startswith("data/") and name != "data/betel-nut-mod/worldgen/placed_feature/betel_palm_tree.json":
                assert jar.read(name) == previous.read(name), name
                preserved_data += 1

release = out / f"betel-nut-mod-{version}-jungle-groves-fabric-1.21.1.jar"
shutil.copy2(source, release)
previews = out / "previews"
previews.mkdir(exist_ok=True)
screenshots = sorted((root / "art-rework/client-run/screenshots").glob("grove-natural-*.png"))
assert len(screenshots) == 2
for file in screenshots:
    shutil.copy2(file, previews / file.name)

report = {
    "release": release.name,
    "sha256": hashlib.sha256(release.read_bytes()).hexdigest(),
    "gameTestsPassed": 33,
    "newGroveGameTests": 9,
    "packagedResourceParityCount": checked,
    "preservedPreviousArtResources": preserved_art,
    "preservedPreviousDataResources": preserved_data,
    "previewProbeAndTestCodeExcluded": True,
    "naturalWorldSeed": natural["seed"],
    "naturalWorldUniqueTreesObserved": len(unique_roots),
    "naturalWorldSamples": [{k: v for k, v in sample.items() if k != "roots"} for sample in natural["samples"]],
    "clientVerification": "GROVE_CLIENT_NATURAL_WORLD_VERIFIED",
    "screenshots": [p.name for p in screenshots],
}
(out / "verification.json").write_text(json.dumps(report, indent=2) + "\n", encoding="utf-8")
print(json.dumps(report))
