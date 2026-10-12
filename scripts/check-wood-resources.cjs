const fs = require('node:fs');
const path = require('node:path');
const sharp = require(path.join(process.env.CODEX_NODE_MODULES || path.join(process.env.USERPROFILE,
  '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules'), 'sharp'));
const root = path.resolve(__dirname, '..');
const assets = path.join(root, 'src/main/resources/assets/betel-nut-mod');
const assert = (value, message) => { if (!value) throw new Error(message); };
async function main() {
  const top = await sharp(path.join(assets,'textures/block/betel_palm_door_top.png')).ensureAlpha().raw().toBuffer();
  const bottom = await sharp(path.join(assets,'textures/block/betel_palm_door_bottom.png')).ensureAlpha().raw().toBuffer();
  assert(top.length===32*32*4 && bottom.length===top.length, 'Door halves must be 32x32');
  const holes = new Set();
  for (let i=0; i<32*32; i++) {
    assert(top[i*4+3]===0 || top[i*4+3]===255, 'Door alpha must be binary');
    assert(bottom[i*4+3]===255, 'Lower half must be solid wood');
    if(!top[i*4+3]) holes.add(i);
  }
  let windows=0;
  const transparentPixels=holes.size;
  while (holes.size) {
    windows++;
    const queue=[holes.values().next().value];holes.delete(queue[0]);
    for(let j=0; j<queue.length; j++) {
      const index=queue[j], x=index%32, y=Math.floor(index/32);
      assert(x>0&&x<31&&y>0&&y<31, 'Window must not cut through the outer frame');
      for(const neighbor of [index-1,index+1,index-32,index+32]) if(holes.delete(neighbor)) queue.push(neighbor);
    }
  }
  assert(windows===4,'Door must have exactly four transparent windows');
  const icon = await sharp(path.join(assets,'textures/item/betel_palm_door.png')).ensureAlpha().raw().toBuffer();
  assert(icon.length===32*32*4,'Generated item sprite must use a square canvas');
  for(let y=0;y<32;y++) for(let x=0;x<32;x++) if(x<8||x>=24)
    assert(icon[(y*32+x)*4+3]===0,'Tall door icon must preserve its aspect ratio');
  const variants=JSON.parse(fs.readFileSync(path.join(assets,'blockstates/betel_palm_door.json'))).variants;
  assert(Object.keys(variants).length===32,'Door model variants must cover facing, half, hinge and open');
  for(const facing of ['north','south','east','west']) for(const half of ['lower','upper'])
    for(const hinge of ['left','right']) for(const open of ['false','true'])
      assert(variants[`facing=${facing},half=${half},hinge=${hinge},open=${open}`], 'Missing door model variant');
  const report={doorWindows:windows,transparentPixels,doorHalves:'32x32',itemSprite:'32x32 with a 16x32 centered door',doorModelVariants:32};
  fs.writeFileSync(path.join(root,'art-rework/wood-family/asset-verification.json'),JSON.stringify(report,null,2)+'\n');
  console.log(JSON.stringify(report));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
