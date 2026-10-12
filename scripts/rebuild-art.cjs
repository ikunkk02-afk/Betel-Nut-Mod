// Production sizing/alpha normalization and model integration; artwork is generated with imagegen.
const fs = require('node:fs');
const path = require('node:path');
const sharp = require(path.join(process.env.CODEX_NODE_MODULES || path.join(process.env.USERPROFILE,
  '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules'), 'sharp'));
const root = path.resolve(__dirname, '..');
const assets = path.join(root, 'src/main/resources/assets/betel-nut-mod');
const review = path.join(root, 'art-rework');
const json = (file, data) => { fs.mkdirSync(path.dirname(file), { recursive: true }); fs.writeFileSync(file, JSON.stringify(data, null, 2) + '\n'); };

function makeModels() {
  const texture = (name) => `betel-nut-mod:block/${name}`;
  const faces = (side, end) => Object.fromEntries(['north', 'south', 'east', 'west', 'up', 'down'].map(face =>
    [face, { uv: face === 'up' || face === 'down' ? [5, 5, 11, 11] : [5, 0, 11, 16], texture: face === 'up' || face === 'down' ? end : side }]));
  for (const crown of [false, true]) {
    json(path.join(assets, 'models/block', crown ? 'betel_palm_crownshaft.json' : 'betel_palm_log.json'), {
      parent: 'minecraft:block/block', ambientocclusion: false,
      textures: { particle: texture(crown ? 'betel_palm_crownshaft' : 'betel_palm_log'), side: texture(crown ? 'betel_palm_crownshaft' : 'betel_palm_log'), end: texture('betel_palm_log_top') },
      elements: [{ from: [5, 0, 5], to: [11, 16, 11], faces: faces('#side', '#end') }]
    });
  }
  const logs = {};
  for (const crown of [false, true]) for (const axis of ['x', 'y', 'z']) {
    const model = { model: `betel-nut-mod:block/${crown ? 'betel_palm_crownshaft' : 'betel_palm_log'}` };
    if (axis !== 'y') model.x = 90;
    if (axis === 'x') model.y = 90;
    logs[`axis=${axis},crownshaft=${crown}`] = model;
  }
  json(path.join(assets, 'blockstates/betel_palm_log.json'), { variants: logs });

  require('./integrate-blockbench-palm.cjs')();
}

async function main() {
  makeModels();
  const manifest = JSON.parse(fs.readFileSync(path.join(review, 'art-manifest.json'), 'utf8'));
  fs.mkdirSync(path.join(review, 'masters'), { recursive: true });
  const report = [];
  for (const entry of manifest) {
    const source = path.join(review, 'masters', `${entry.name}.png`);
    if (!fs.existsSync(source)) fs.copyFileSync(entry.source, source);
    const { data, info } = await sharp(source).resize(entry.size, entry.size, { kernel: 'nearest', fit: 'fill' }).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
    let transparent = 0;
    for (let i = 0; i < data.length; i += 4) {
      data[i + 3] = entry.alpha && data[i + 3] < 128 ? 0 : 255;
      if (!data[i + 3]) { transparent++; data[i] = data[i + 1] = data[i + 2] = 0; }
    }
    if (entry.alpha && transparent < entry.size * entry.size * .02) throw new Error(`${entry.name}: expected real transparency`);
    const dest = path.join(assets, entry.destination);
    fs.mkdirSync(path.dirname(dest), { recursive: true });
    await sharp(data, { raw: info }).png({ palette: true, colours: entry.name === 'icon' ? 64 : 32, dither: 0 }).toFile(dest);
    report.push({ name: entry.name, path: entry.destination, size: entry.size, transparentPixels: transparent });
  }
  json(path.join(review, 'asset-report.json'), report);
  // Review sheets enlarge actual production textures with nearest-neighbor filtering.
  const columns = 6, cellW = 180, cellH = 184;
  const rows = Math.ceil(manifest.length / columns);
  const tiles = [];
  for (let i = 0; i < manifest.length; i++) {
    const entry = manifest[i];
    const x = (i % columns) * cellW, y = Math.floor(i / columns) * cellH;
    tiles.push({ input: await sharp(path.join(assets, entry.destination)).resize(128, 128, { kernel: 'nearest' }).png().toBuffer(), left: x + 26, top: y + 10 });
    const label = `<svg width="180" height="42"><text x="90" y="20" fill="#eee9df" text-anchor="middle" font-family="Arial" font-size="11">${entry.name.replaceAll('_', ' ')}</text><text x="90" y="36" fill="#aaa799" text-anchor="middle" font-family="Arial" font-size="10">${entry.size} × ${entry.size}</text></svg>`;
    tiles.push({ input: Buffer.from(label), left: x, top: y + 140 });
  }
  await sharp({ create: { width: columns * cellW, height: rows * cellH, channels: 4, background: '#232b27' } }).composite(tiles).png().toFile(path.join(review, 'textures-preview.png'));
  console.log(`Integrated ${report.length} textures and rebuilt log/frond models.`);
}
main().catch(error => { console.error(error); process.exitCode = 1; });
