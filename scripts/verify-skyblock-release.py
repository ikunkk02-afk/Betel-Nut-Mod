"""Verify the skyblock release against the v5.5.0 main-branch baseline."""
import hashlib
import io
import json
from pathlib import Path
import re
import struct
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BASELINE = "da9fa2595a26280dc4cc494066d0ae5023ae8417"
VERSION = "5.6.0"
ARTIFACTS = ROOT / "art-rework/skyblock-v5.6.0"
ALLOWED = {
    "CHANGELOG.md", "README.md", "build.gradle", "gradle.properties",
    "src/gametest/resources/fabric.mod.json",
    "src/gametest/java/betel/nut/test/BetelSkyblockGameTests.java",
    "src/main/java/betel/nut/BetelNutMidnightConfig.java",
    "src/main/java/betel/nut/component/BetelSkyblockWorldComponent.java",
    "src/main/java/betel/nut/skyblock/BetelSkyblockManager.java",
    "src/main/java/betel/nut/skyblock/SkyblockComposting.java",
    "src/main/java/betel/nut/skyblock/SkyblockSpawnSafety.java",
    "src/main/java/betel/nut/skyblock/SkyblockStarterKit.java",
    "src/main/java/betel/nut/mixin/NaturalSpawnerMixin.java",
    "src/main/resources/betel-nut-mod.mixins.json",
    "src/main/resources/assets/betel-nut-mod/lang/en_us.json",
    "src/main/resources/assets/betel-nut-mod/lang/zh_cn.json",
    "docs/skyblock-5.6.0.md", "scripts/prepare-skyblock-preview.py",
    "scripts/verify-skyblock-release.py",
    "art-rework/skyblock-v5.6.0/preview.gradle",
    "art-rework/skyblock-v5.6.0/dev/resources/fabric.mod.json",
    "art-rework/skyblock-v5.6.0/dev/java/betel/nut/preview/SkyblockPreviewClient.java",
}
CLIENT_CHECKS = [
    "SKYBLOCK_CLASSIC_START_PASS", "SKYBLOCK_CLASSIC_RESCUE_PASS",
    "SKYBLOCK_CLASSIC_RELOAD_PASS", "SKYBLOCK_ONE_BLOCK_START_PASS",
    "SKYBLOCK_TREE_RENEWAL_HARVEST_PASS", "SKYBLOCK_COMPOST_PACKET_PASS",
    "SKYBLOCK_EMERGENCY_RESCUE_PASS", "SKYBLOCK_CLIENT_ALL_CHECKS_PASS",
]


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT)


def comparable(data):
    return data.replace(b"\r\n", b"\n")


def main():
    changed = set(git("diff", "--name-only", BASELINE).decode().splitlines())
    changed.update(git("ls-files", "--others", "--exclude-standard").decode().splitlines())
    assert changed <= ALLOWED, f"Unexpected scope: {sorted(changed - ALLOWED)}"
    unchanged = 0
    with zipfile.ZipFile(io.BytesIO(git("archive", "--format=zip", BASELINE, "src/main/resources"))) as baseline:
        for name in baseline.namelist():
            if name.endswith("/") or name in ALLOWED:
                continue
            assert comparable(baseline.read(name)) == comparable((ROOT / name).read_bytes()), name
            unchanged += 1

    for language in ("en_us", "zh_cn"):
        path = f"src/main/resources/assets/betel-nut-mod/lang/{language}.json"
        before = json.loads(git("show", f"{BASELINE}:{path}"))
        after = json.loads((ROOT / path).read_text(encoding="utf-8"))
        permitted = {"message.betel-nut-mod.one_block_welcome"}
        permitted.update(f"book.betel-nut-mod.skyblock_guide.page{page}" for page in range(1, 10))
        assert all(after.get(key) == value for key, value in before.items() if key not in permitted)
        assert all(after[f"book.betel-nut-mod.skyblock_guide.page{page}"] for page in range(1, 10))

    jars = [ROOT / "build/libs" / f"betel-nut-mod-{VERSION}{suffix}.jar" for suffix in ("", "-sources")]
    with zipfile.ZipFile(jars[0]) as jar:
        assert jar.testzip() is None
        metadata = json.loads(jar.read("fabric.mod.json"))
        assert metadata["version"] == VERSION and metadata["id"] == "betel-nut-mod"
        for cls in ("SkyblockComposting", "SkyblockSpawnSafety", "SkyblockStarterKit"):
            assert f"betel/nut/skyblock/{cls}.class" in jar.namelist()
        assert "betel/nut/mixin/NaturalSpawnerMixin.class" in jar.namelist()
        assert not any("/preview/" in name or "/test/" in name or "/gametest/" in name
                       or name.startswith("vectorwing/") for name in jar.namelist())
        for path in (ROOT / "src/main/resources").rglob("*"):
            if path.is_file() and path.name != "fabric.mod.json":
                assert jar.read(path.relative_to(ROOT / "src/main/resources").as_posix()) == path.read_bytes(), path
    # Loom remaps the published sources to intermediary names. Compare the named
    # input archive with the workspace, and check the remapped archive's entries.
    named_sources = ROOT / "build/devlibs" / f"betel-nut-mod-{VERSION}-sources.jar"
    with zipfile.ZipFile(named_sources) as jar, zipfile.ZipFile(jars[1]) as remapped:
        assert jar.testzip() is None
        assert remapped.testzip() is None and set(jar.namelist()) == set(remapped.namelist())
        for source_dir in ("src/main/java", "src/client/java"):
            for path in (ROOT / source_dir).rglob("*.java"):
                assert comparable(jar.read(path.relative_to(ROOT / source_dir).as_posix())) == comparable(path.read_bytes()), path

    builds = {}
    for name in ("build.log", "farmers-delight-build.log"):
        log = (ARTIFACTS / name).read_text(encoding="utf-8")
        assert "BUILD SUCCESSFUL" in log and "All 107 required tests passed" in log, name
        builds[name] = 107
    client_log = (ARTIFACTS / "client-smoke.log").read_text(encoding="utf-8")
    assert "BUILD SUCCESSFUL" in client_log
    assert all(marker in client_log for marker in CLIENT_CHECKS)
    mob_log = (ARTIFACTS / "mob-smoke.log").read_text(encoding="utf-8")
    assert "BUILD SUCCESSFUL" in mob_log and "SKYBLOCK_MOBS_ALL_CHECKS_PASS" in mob_log
    assert all(f"SKYBLOCK_NATURAL_MOBS_PASS mode={mode}" in mob_log for mode in ("classic", "one_block"))
    guide_log = (ARTIFACTS / "guide-smoke.log").read_text(encoding="utf-8")
    assert "SKYBLOCK_GUIDE_ALL_CHECKS_PASS" in guide_log and "BUILD SUCCESSFUL" in guide_log
    guide_layout = [{"language": language, "page": int(page), "lines": int(lines), "height": int(height)}
                    for language, page, lines, height in re.findall(
                        r"SKYBLOCK_GUIDE_LAYOUT language=(\w+) page=(\d+) lines=(\d+) height=(\d+)", guide_log)]
    assert len(guide_layout) == 18 and all(page["height"] <= 128 for page in guide_layout)
    for page in guide_layout:
        path = ARTIFACTS / "client-run/screenshots" / f"guide-{page['language']}-page{page['page']}.png"
        data = path.read_bytes()
        assert data[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(">II", data[16:24]) == (1600, 1000), path

    report = {
        "version": VERSION, "baseline": BASELINE, "scope": "passed",
        "unchangedResources": unchanged, "jarResourcesMatchSource": True,
        "namedSourceJarMatchesSource": True, "remappedSourceJarEntriesMatch": True,
        "previewAndTestCodeExcluded": True,
        "serverGameTests": builds, "clientChecks": CLIENT_CHECKS,
        "vanillaNaturalSpawnerCreatesMonstersInBothPresets": True,
        "guideTextArea": {"width": 114, "height": 128}, "guideLayout": guide_layout,
        "artifacts": [{"name": path.name, "bytes": path.stat().st_size,
                       "sha256": hashlib.sha256(path.read_bytes()).hexdigest()} for path in jars],
    }
    (ARTIFACTS / "verification.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"PASS: skyblock-only scope; {unchanged} resources unchanged; 107 tests in both environments; "
          "client checks and all 18 guide pages passed; release and source JARs verified")


if __name__ == "__main__":
    main()
