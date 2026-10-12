"""Seed only the isolated HUD test run with old-version configuration fixtures."""
from pathlib import Path
import json
import shutil

root = Path(__file__).resolve().parent.parent
out = root / 'art-rework/compact-hud-v5.0.1/client-run'
save = out / 'saves/CompactHudPreview'
if not save.exists():
    shutil.copytree(root / 'art-rework/client-run/saves/ArecaPreview', save)
out.mkdir(parents=True, exist_ok=True)
shutil.copy2(root / 'art-rework/client-run/options.txt', out / 'options.txt')
cfg = out / 'config'
cfg.mkdir(exist_ok=True)
(cfg / 'betel_nut_mod_client.json').write_text(json.dumps(dict(configVersion=2, hudEnabled=True,
    hudAnchor='TOP_LEFT', hudOffsetX=10, hudOffsetY=30, hudX=10, hudY=30,
    hudUseCustomPosition=True, hudScale=1)), encoding='utf-8')
(cfg / 'betel_nut.json').write_text(json.dumps(dict(configVersion=12, maxAddictionValue=30,
    minimumAddictionForWithdrawal=3, legacyMaxAddictionValue=100)), encoding='utf-8')
print('Prepared isolated preview configuration:', out)
