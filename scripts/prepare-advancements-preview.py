"""Create one isolated client fixture with completed v5.6.0 advancement criteria."""
import json
from pathlib import Path
import shutil
import subprocess

root = Path(__file__).resolve().parents[1]
out = root / "art-rework/advancements-v5.7.0/client-run"
save = out / "saves/AdvancementsPreview"
if save.exists():
    raise SystemExit(f"Refusing to overwrite test world: {save}")
save.mkdir(parents=True)
shutil.copy2(root / "art-rework/client-run/saves/ArecaPreview/level.dat", save / "level.dat")
shutil.copy2(root / "art-rework/client-run/options.txt", out / "options.txt")
old = {}
for name in ("root", "underground_betel", "skyblock/first_compost", "skyblock/enter_end"):
    raw = subprocess.check_output(["git", "show", f"970ac49:src/main/resources/data/betel-nut-mod/advancement/{name}.json"], cwd=root)
    criteria = json.loads(raw)["criteria"]
    old["betel-nut-mod:" + name] = {"criteria": {key: "2026-10-12 10:00:00 +0800" for key in criteria}, "done": True}
old["DataVersion"] = 3955
(save / "advancements").mkdir()
(save / "advancements/853b046a-d7b5-4c8d-b071-abc9a0be6251.json").write_text(json.dumps(old, indent=2) + "\n", encoding="utf-8")
print(out)
