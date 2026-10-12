"""Mechanically resize imagegen originals; never redraw or flatten their alpha."""
from pathlib import Path
import hashlib
import json
import zipfile
from PIL import Image, ImageDraw, ImageFont

ROOT = Path(__file__).resolve().parent.parent
OUT = ROOT / 'art-rework/features-v5'
ART = OUT / 'imagegen'
ASSETS = ROOT / 'src/main/resources/assets/betel-nut-mod'
manifest = json.loads((ART / 'manifest.json').read_text(encoding='utf-8'))
icons = {'oral_cancer', 'mouth_locked', 'echo_sight'}
backup = ART / 'provisional-textures.zip'
if not backup.exists():
    with zipfile.ZipFile(backup, 'w', zipfile.ZIP_DEFLATED) as archive:
        for a in manifest['assets']:
            category = 'mob_effect' if a['name'] in icons else 'item'
            path = ASSETS / f'textures/{category}/{a["name"]}.png'
            if path.exists():
                archive.write(path, path.relative_to(ROOT))

integrated = []
for a in manifest['assets']:
    name = a['name']
    source = ART / f'originals/{name}.png'
    image = Image.open(source)
    assert image.mode == 'RGBA' and image.getextrema()[3] == (0, 255), name
    size = 18 if name in icons else 128 if name in {'kouweiwang_pack', 'hecheng_tianxia_pack', 'xiangtan_puzi_pack'} else 64 if name == 'empty_pack' else 32
    target = ASSETS / f'textures/{"mob_effect" if name in icons else "item"}/{name}.png'
    target.parent.mkdir(parents=True, exist_ok=True)
    image.resize((size, size), Image.Resampling.NEAREST).save(target)
    integrated.append({'name': name, 'original': str(source.relative_to(ROOT)),
        'texture': str(target.relative_to(ROOT)), 'size': size,
        'sha256': hashlib.sha256(target.read_bytes()).hexdigest()})
(ART / 'integration.json').write_text(json.dumps(integrated, indent=2) + '\n', encoding='utf-8')

sheet = Image.new('RGB', (900, 600), '#17262d')
draw = ImageDraw.Draw(sheet)
font = ImageFont.truetype('C:/Windows/Fonts/msyh.ttc', 13)
zh = json.loads((ASSETS / 'lang/zh_cn.json').read_text(encoding='utf-8'))
for i, a in enumerate(integrated):
    x, y = i % 5 * 180, i // 5 * 200
    image = Image.open(ROOT / a['texture']).resize((144, 144), Image.Resampling.NEAREST)
    sheet.paste(image, (x + 18, y + 8), image)
    key = ('effect' if a['name'] in icons else 'item') + '.betel-nut-mod.' + a['name']
    draw.text((x + 8, y + 162), zh[key], font=font, fill='#e8e4d6')
    draw.text((x + 8, y + 181), f'{a["size"]} x {a["size"]} / RGBA', font=font, fill='#8ca6ad')
sheet.save(OUT / 'item-contact-sheet.png')
print(f'Integrated {len(integrated)} imagegen textures; originals and alpha preserved.')
