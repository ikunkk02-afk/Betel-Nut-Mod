"""Check progression scope, save compatibility and verified release artifacts."""
import hashlib
import io
import json
from pathlib import Path
import re
import struct
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BASELINE = "970ac49e822aa4f5bc85ca6df3e5f1d751b16687"
VERSION = "5.7.0"
OUT = ROOT / "art-rework/advancements-v5.7.0"
ADV = "src/main/resources/data/betel-nut-mod/advancement/"
ALLOWED = {
    "CHANGELOG.md", "README.md", "gradle.properties", "build.gradle",
    "src/gametest/resources/fabric.mod.json",
    "src/gametest/java/betel/nut/test/BetelAdvancementGameTests.java",
    "src/gametest/java/betel/nut/test/BetelSkyblockGameTests.java",
    "src/main/java/betel/nut/BetelNutMod.java",
    "src/main/java/betel/nut/advancement/BetelQuestAdvancements.java",
    "src/main/java/betel/nut/block/BetelNutClusterBlock.java",
    "src/main/java/betel/nut/item/BetelPackItem.java",
    "src/main/java/betel/nut/event/BetelNutEvents.java",
    "src/main/java/betel/nut/mixin/ComposterBlockMixin.java",
    "src/main/resources/betel-nut-mod.mixins.json",
    "src/main/resources/assets/betel-nut-mod/lang/en_us.json",
    "src/main/resources/assets/betel-nut-mod/lang/zh_cn.json",
    "docs/advancements-5.7.0.md", "scripts/generate-advancements.py",
    "scripts/prepare-advancements-preview.py", "scripts/verify-advancements-release.py",
    "art-rework/advancements-v5.7.0/preview.gradle",
    "art-rework/advancements-v5.7.0/dev/resources/fabric.mod.json",
    "art-rework/advancements-v5.7.0/dev/java/betel/nut/preview/AdvancementsPreviewClient.java",
}


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT)


def comparable(data):
    return data.replace(b"\r\n", b"\n")


def main():
    changed = set(git("diff", "--name-only", BASELINE).decode().splitlines())
    changed.update(git("ls-files", "--others", "--exclude-standard").decode().splitlines())
    assert all(name in ALLOWED or name.startswith(ADV) and not name.startswith(ADV + "recipes/") for name in changed), changed
    nodes = {"betel-nut-mod:" + path.relative_to(ROOT / ADV).with_suffix("").as_posix(): json.loads(path.read_text(encoding="utf-8"))
             for path in (ROOT / ADV).rglob("*.json") if "recipes" not in path.relative_to(ROOT / ADV).parts}
    visible = {name: data for name, data in nodes.items() if "display" in data}
    assert len(nodes) == 33 and len(visible) == 28
    assert sum("parent" not in data for data in visible.values()) == 2
    for name, data in visible.items():
        seen = set()
        while "parent" in data:
            assert name not in seen, name
            seen.add(name)
            name = data["parent"]
            data = nodes[name]
    for name, data in nodes.items():
        assert all(criterion in data["criteria"] for group in data["requirements"] for criterion in group), name
    assert len(nodes["betel-nut-mod:processing/all_flavors"]["criteria"]) == 17
    assert nodes["betel-nut-mod:skyblock/first_compost"]["criteria"]["has_bone_meal"]["trigger"] == "minecraft:impossible"

    unchanged = 0
    with zipfile.ZipFile(io.BytesIO(git("archive", "--format=zip", BASELINE, "src/main/resources"))) as baseline:
        for name in baseline.namelist():
            if name.endswith("/"):
                continue
            if name.startswith(ADV) and not name.startswith(ADV + "recipes/"):
                old = json.loads(baseline.read(name))
                new = json.loads((ROOT / name).read_text(encoding="utf-8"))
                assert set(old["criteria"]) == set(new["criteria"]), name
                if "display" not in old and name.endswith(("eat_first_betel_nut.json", "get_first_residue.json", "addiction_started.json", "withdrawal_started.json", "eat_hecheng_tianxia.json")):
                    assert old == new, name
            if name not in changed:
                assert comparable(baseline.read(name)) == comparable((ROOT / name).read_bytes()), name
                unchanged += 1
    for language in ("zh_cn", "en_us"):
        path = f"src/main/resources/assets/betel-nut-mod/lang/{language}.json"
        old = json.loads(git("show", f"{BASELINE}:{path}"))
        new = json.loads((ROOT / path).read_text(encoding="utf-8"))
        assert all(new.get(key) == value for key, value in old.items() if not key.startswith("advancement.betel-nut-mod."))
        for data in visible.values():
            for field in ("title", "description"):
                assert new[data["display"][field]["translate"]], (language, field)

    jars = [ROOT / "build/libs" / f"betel-nut-mod-{VERSION}{suffix}.jar" for suffix in ("", "-sources")]
    with zipfile.ZipFile(jars[0]) as jar:
        assert jar.testzip() is None
        metadata = json.loads(jar.read("fabric.mod.json"))
        assert metadata["version"] == VERSION and metadata["id"] == "betel-nut-mod"
        assert "betel/nut/mixin/ComposterBlockMixin.class" in jar.namelist()
        assert not any("/preview/" in name or "/test/" in name or "/gametest/" in name or name.startswith("vectorwing/") for name in jar.namelist())
        for path in (ROOT / "src/main/resources").rglob("*"):
            if path.is_file() and path.name != "fabric.mod.json":
                assert jar.read(path.relative_to(ROOT / "src/main/resources").as_posix()) == path.read_bytes(), path
    with zipfile.ZipFile(ROOT / "build/devlibs" / f"betel-nut-mod-{VERSION}-sources.jar") as named, zipfile.ZipFile(jars[1]) as remapped:
        assert named.testzip() is None and remapped.testzip() is None
        assert set(named.namelist()) == set(remapped.namelist())
        for source in ("src/main/java", "src/client/java"):
            for path in (ROOT / source).rglob("*.java"):
                assert comparable(named.read(path.relative_to(ROOT / source).as_posix())) == comparable(path.read_bytes()), path
    for log in ("advancements-build.log", "advancements-fd-build.log"):
        content = (ROOT / ".git" / log).read_text(encoding="utf-8")
        assert "All 116 required tests passed" in content and "BUILD SUCCESSFUL" in content, log
    client = (ROOT / ".git/advancements-client.log").read_text(encoding="utf-8")
    markers = ["ADVANCEMENT_LEGACY_COMPLETION_PASS", "ADVANCEMENT_REAL_CLIENT_EVENTS_PASS",
               "ADVANCEMENT_RELOAD_AND_PARTIAL_PROGRESS_PASS", "ADVANCEMENT_CLIENT_ALL_CHECKS_PASS"]
    assert all(marker in client for marker in markers) and "BUILD SUCCESSFUL" in client
    layouts = re.findall(r"ADVANCEMENT_CLIENT_LAYOUT language=(\w+) root=([^ ]+) nodes=(\d+) overlap=false", client)
    assert {(lang, root, count) for lang, root, count in layouts} == {
        (lang, root, count) for lang in ("en_us", "zh_cn")
        for root, count in (("betel-nut-mod:root", "24"), ("betel-nut-mod:skyblock/enter_betel_skyblock", "4"))}
    for language in ("en_us", "zh_cn"):
        for name in [f"main-{language}-{view}.png" for view in ("center", "top", "bottom")] + [f"skyblock-{language}.png"]:
            data = (OUT / "client-run/screenshots" / name).read_bytes()
            assert data[:8] == b"\x89PNG\r\n\x1a\n" and struct.unpack(">II", data[16:24]) == (1600, 1000), name
    report = {"version": VERSION, "baseline": BASELINE, "visibleAdvancements": 28, "tabs": 2,
              "legacyCriterionNamesPreserved": True, "hiddenQuestHelpersUnchanged": 5,
              "unchangedResources": unchanged, "vanillaServerTests": 116, "farmersDelightServerTests": 116,
              "clientChecks": markers, "localizedLayouts": layouts,
              "jarResourcesMatchSource": True, "namedSourcesMatchWorkspace": True,
              "artifacts": [{"name": path.name, "bytes": path.stat().st_size, "sha256": hashlib.sha256(path.read_bytes()).hexdigest()} for path in jars]}
    OUT.mkdir(parents=True, exist_ok=True)
    (OUT / "verification.json").write_text(json.dumps(report, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
    print(f"PASS: 28 visible advancements, two tabs, five unchanged helpers, {unchanged} unchanged resources; 116 tests in both environments; client persistence/layout and JAR/source checks passed")


if __name__ == "__main__":
    main()
