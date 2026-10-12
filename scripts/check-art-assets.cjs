const fs = require('node:fs');
const path = require('node:path');
const crypto = require('node:crypto');
const sharp = require(path.join(process.env.CODEX_NODE_MODULES || path.join(process.env.USERPROFILE,
  '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules'), 'sharp'));
const root = path.resolve(__dirname, '..');
const assetRoot = path.join(root, 'src/main/resources/assets/betel-nut-mod');
const backupRoot = path.join(root, 'art-rework/backup/src/main/resources/assets/betel-nut-mod');
const walk = dir => fs.readdirSync(dir, { withFileTypes: true }).flatMap(e => e.isDirectory() ? walk(path.join(dir, e.name)) : [path.join(dir, e.name)]);
const hash = file => crypto.createHash('sha256').update(fs.readFileSync(file)).digest('hex');
const assert = (ok, message) => { if (!ok) throw new Error(message); };
const reference = (id, kind) => {
  if (!id.startsWith('betel-nut-mod:')) return;
  const target = path.join(assetRoot, kind === 'texture' ? 'textures' : 'models', id.split(':')[1] + (kind === 'texture' ? '.png' : '.json'));
  assert(fs.existsSync(target), `Missing ${kind}: ${id}`);
};
async function main() {
  const originals = walk(backupRoot).filter(f => f.endsWith('.png'));
  for (const original of originals) {
    const target = path.join(assetRoot, path.relative(backupRoot, original));
    assert(fs.existsSync(target), `Original asset disappeared: ${target}`);
    assert(hash(original) !== hash(target), `Original artwork was not replaced: ${target}`);
  }
  const manifest = JSON.parse(fs.readFileSync(path.join(root, 'art-rework/art-manifest.json')));
  for (const entry of manifest) {
    const file = path.join(assetRoot, entry.destination);
    const metadata = await sharp(file).metadata();
    assert(metadata.width === entry.size && metadata.height === entry.size, `Unexpected size: ${entry.name}`);
    const { data } = await sharp(file).ensureAlpha().raw().toBuffer({ resolveWithObject: true });
    let transparent = 0;
    for (let i = 3; i < data.length; i += 4) {
      assert(data[i] === 0 || data[i] === 255, `Unclean alpha: ${entry.name}`);
      if (data[i] === 0) transparent++;
    }
    assert(!entry.alpha || transparent > 0, `Expected transparent pixels: ${entry.name}`);
    assert(entry.alpha || transparent === 0, `Opaque texture contains holes: ${entry.name}`);
  }
  const files = walk(assetRoot).filter(f => f.endsWith('.json'));
  for (const file of files) {
    const data = JSON.parse(fs.readFileSync(file));
    if (file.includes(`${path.sep}models${path.sep}`)) {
      if (data.parent) reference(data.parent, 'model');
      for (const texture of Object.values(data.textures || {})) if (!texture.startsWith('#')) reference(texture, 'texture');
    }
    if (file.includes(`${path.sep}blockstates${path.sep}`)) {
      const scan = value => { if (!value || typeof value !== 'object') return; if (value.model) reference(value.model, 'model'); for (const child of Object.values(value)) scan(child); };
      scan(data);
    }
  }
  const report = { replacedOriginalPngs: originals.length, productionTextures: manifest.length, jsonFilesChecked: files.length, alphaAndResolution: 'passed', localModelAndTextureReferences: 'passed' };
  fs.writeFileSync(path.join(root, 'art-rework/asset-verification.json'), JSON.stringify(report, null, 2) + '\n');
  console.log(JSON.stringify(report));
}
main().catch(error => { console.error(error); process.exitCode = 1; });
