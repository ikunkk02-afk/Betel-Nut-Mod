"""Seed fresh isolated skyblock test worlds; never touch an existing saved world."""
from pathlib import Path
import shutil

root = Path(__file__).resolve().parent.parent
out = root / 'art-rework/skyblock-v5.6.0/client-run'
source = root / 'art-rework/client-run/saves/ArecaPreview/level.dat'
for name in ['SkyblockClassic', 'SkyblockOneBlock']:
    save = out / 'saves' / name
    if save.exists():
        raise SystemExit(f'Test world already exists; use a fresh test directory: {save}')
    save.mkdir(parents=True)
    shutil.copy2(source, save / 'level.dat')
shutil.copy2(root / 'art-rework/client-run/options.txt', out / 'options.txt')
print(out)
