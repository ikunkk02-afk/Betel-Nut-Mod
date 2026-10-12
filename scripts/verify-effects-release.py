"""Verify the effects-only release against the v5.4.0 main-branch baseline."""
import io
import json
from pathlib import Path
import re
import subprocess
import zipfile

ROOT = Path(__file__).resolve().parents[1]
BASELINE = "9077c6fdb4c538a67208a4702ba5a94f416747b0"
VERSION = "5.5.0"
JAR = ROOT / "build/libs" / f"betel-nut-mod-{VERSION}.jar"
ALLOWED = {
    "CHANGELOG.md", "README.md", "build.gradle", "gradle.properties",
    "src/gametest/resources/fabric.mod.json",
    "src/gametest/java/betel/nut/test/BetelEffectGameTests.java",
    "src/main/java/betel/nut/BetelNutMidnightConfig.java",
    "src/main/java/betel/nut/component/BetelNutAddictionComponent.java",
    "src/main/java/betel/nut/effect/HechengTianxiaEffects.java",
    "src/main/java/betel/nut/item/BetelEffectProfile.java",
    "src/main/java/betel/nut/item/BetelNutItem.java",
    "src/main/java/betel/nut/item/EnderBetelTeleportHandler.java",
    "src/main/java/betel/nut/item/ModItems.java",
    "src/main/resources/assets/betel-nut-mod/lang/en_us.json",
    "src/main/resources/assets/betel-nut-mod/lang/zh_cn.json",
    "docs/betel-effects-5.5.0.md", "scripts/verify-effects-release.py",
    "art-rework/effects-v5.5.0/preview.gradle",
    "art-rework/effects-v5.5.0/dev/resources/fabric.mod.json",
    "art-rework/effects-v5.5.0/dev/java/betel/nut/preview/EffectsPreviewClient.java",
}


def git(*args):
    return subprocess.check_output(["git", *args], cwd=ROOT)


def comparable(path, data):
    return data.replace(b"\r\n", b"\n") if Path(path).suffix in {".json", ".java", ".gradle", ".md", ".properties"} else data


def main():
    changed = set(git("diff", "--name-only", BASELINE).decode().splitlines())
    changed.update(git("ls-files", "--others", "--exclude-standard").decode().splitlines())
    assert changed <= ALLOWED, f"Unexpected scope: {sorted(changed - ALLOWED)}"
    count = 0
    with zipfile.ZipFile(io.BytesIO(git("archive", "--format=zip", BASELINE, "src/main/resources"))) as baseline:
        for path in baseline.namelist():
            if path.endswith("/") or path in ALLOWED:
                continue
            assert comparable(path, baseline.read(path)) == comparable(path, (ROOT / path).read_bytes()), path
            count += 1
    profiles = set(re.findall(r"BetelEffectProfile\.([A-Z_]+)", (ROOT / "src/main/java/betel/nut/item/ModItems.java").read_text(encoding="utf8")))
    assert len(profiles) == 19
    for lang in ("en_us", "zh_cn"):
        values = json.loads((ROOT / f"src/main/resources/assets/betel-nut-mod/lang/{lang}.json").read_text(encoding="utf8"))
        assert all(f"tooltip.betel-nut-mod.effects.{profile.lower()}" in values for profile in profiles)
    with zipfile.ZipFile(JAR) as jar:
        names = jar.namelist()
        assert json.loads(jar.read("fabric.mod.json"))["version"] == VERSION
        assert "betel/nut/item/BetelEffectProfile.class" in names
        assert not any("/preview/" in name or "/test/" in name or name.startswith("vectorwing/") for name in names)
        for path in ROOT.glob("src/main/resources/**/*"):
            if path.is_file() and path.name != "fabric.mod.json":
                assert jar.read(path.relative_to(ROOT / "src/main/resources").as_posix()) == path.read_bytes(), path
    print(f"PASS: effects-only scope; {count} existing resources unchanged; 19 profiles translated; release JAR resources and exclusions verified")


if __name__ == "__main__":
    main()
