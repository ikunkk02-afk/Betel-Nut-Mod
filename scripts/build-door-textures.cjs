// Production sizing only; the artwork master was generated with the built-in imagegen tool.
const fs = require('node:fs');
const path = require('node:path');
const sharp = require(path.join(process.env.CODEX_NODE_MODULES || path.join(process.env.USERPROFILE,
  '.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules'), 'sharp'));
const root = path.resolve(__dirname, '..');
const out = path.join(root, 'art-rework/wood-family');
const textures = path.join(root, 'src/main/resources/assets/betel-nut-mod/textures');
async function main() {
  const {data} = await sharp(path.join(out, 'door-master.png')).resize(32, 64, {kernel:'nearest',fit:'fill'})
    .ensureAlpha().raw().toBuffer({resolveWithObject:true});
  for(let i=0; i<data.length; i+=4) {
    data[i+3] = data[i+3]<128 ? 0 : 255;
    if(!data[i+3]) data[i]=data[i+1]=data[i+2]=0;
  }
  const bitmap = sharp(data,{raw:{width:32,height:64,channels:4}});
  fs.mkdirSync(path.join(textures,'item'),{recursive:true});
  // Generated item models map the complete sprite to a square, so center a tall icon on a square canvas.
  const icon = await bitmap.clone().resize(16,32,{kernel:'nearest'}).png().toBuffer();
  await sharp({create:{width:32,height:32,channels:4,background:{r:0,g:0,b:0,alpha:0}}})
    .composite([{input:icon,left:8,top:0}]).png().toFile(path.join(textures,'item/betel_palm_door.png'));
  for(const [name,top] of [['top',0],['bottom',32]]) {
    await bitmap.clone().extract({left:0,top,width:32,height:32}).png()
      .toFile(path.join(textures,`block/betel_palm_door_${name}.png`));
  }
  await bitmap.clone().resize(320,640,{kernel:'nearest'}).png().toFile(path.join(out,'door-texture-preview.png'));
  console.log('Installed 32x32 door halves and the correctly proportioned square item sprite.');
}
main().catch(e=>{console.error(e);process.exitCode=1;});
