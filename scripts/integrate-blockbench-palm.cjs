// Import editable Java component geometry. V3 also supplies folded leaf meshes.
const fs = require('node:fs');
const path = require('node:path');
const root = path.resolve(__dirname, '..');
const assets = path.join(root, 'src/main/resources/assets/betel-nut-mod');
const names = ['betel_palm_leaves', 'betel_palm_frond_1', 'betel_palm_frond_2', 'betel_palm_frond_3',
  'betel_palm_frond_diagonal_1', 'betel_palm_frond_diagonal_2'];
const write = (file, data) => fs.writeFileSync(file, JSON.stringify(data, null, 2) + '\n');
function integrate() {
  const leaves=Array.from({length:4},(_,style)=>names.map(name=>name+(style?'_style_'+style:''))).flat();
  const fruits=[0,1,2].map(age=>'betel_nut_cluster_'+age);
  for (const name of [...leaves,...fruits]) {
    const model = JSON.parse(fs.readFileSync(path.join(root, 'art-rework/blockbench', name + '.json')));
    if (!model.elements?.length) throw new Error('Empty Blockbench export: ' + name);
    write(path.join(assets, 'models/block', name + '.json'), {
      parent: 'minecraft:block/block', credit: 'Areca Palm - Blockbench editable model', ambientocclusion: false,
      textures: {particle:'betel-nut-mod:block/'+(name.startsWith('betel_nut_cluster')?'betel_nut_fruit_skin':'betel_palm_leaf_blade'),
        ...Object.fromEntries(Object.entries(model.textures).map(([id,texture])=>[id,'betel-nut-mod:block/'+texture.split('/').at(-1)]))},
      elements:model.elements, ...(model.display ? {display:model.display} : {})
    });
  }
  const variants = {};
  for (const [facing, y] of [['north',0],['east',90],['south',180],['west',270]])
    for(let style=0;style<4;style++) for (let segment=0;segment<names.length;segment++) variants[`facing=${facing},segment=${segment},style=${style}`] = {
      model:'betel-nut-mod:block/' + names[segment]+(style?'_style_'+style:''), y
    };
  write(path.join(assets, 'blockstates/betel_palm_leaves.json'), {variants});
  const fruitVariants={};
  for(const [facing,y] of [['north',0],['east',90],['south',180],['west',270]])for(let age=0;age<3;age++)fruitVariants[`age=${age},facing=${facing}`]={model:'betel-nut-mod:block/betel_nut_cluster_'+age,y};
  write(path.join(assets,'blockstates/betel_nut_cluster.json'),{variants:fruitVariants});
  // World fruit is offset toward the supporting trunk. Centre a separate item model
  // so hand, dropped-item and inventory transforms never inherit that attachment offset.
  const fruitItem=JSON.parse(fs.readFileSync(path.join(assets,'models/block/betel_nut_cluster_2.json')));
  const bounds=[0,1,2].map(axis=>[
    Math.min(...fruitItem.elements.map(e=>e.from[axis])),Math.max(...fruitItem.elements.map(e=>e.to[axis]))]);
  const offset=bounds.map(([lo,hi])=>8-(lo+hi)/2);
  for(const e of fruitItem.elements){
    for(const key of ['from','to'])e[key]=e[key].map((n,i)=>n+offset[i]);
    if(e.rotation)e.rotation.origin=e.rotation.origin.map((n,i)=>n+offset[i]);
  }
  const pose=(rotation,translation,scale)=>({rotation,translation,scale:[scale,scale,scale]});
  fruitItem.display={
    gui:pose([15,35,0],[0,0,0],.66),ground:pose([0,0,0],[0,3,0],.35),fixed:pose([0,180,0],[0,0,0],.65),
    thirdperson_righthand:pose([75,0,0],[0,1,0],.48),thirdperson_lefthand:pose([75,0,0],[0,1,0],.48),
    firstperson_righthand:pose([0,-25,0],[-1,5,0],.34),firstperson_lefthand:pose([0,25,0],[-1,5,0],.34)
  };
  write(path.join(assets,'models/item/betel_nut_cluster.json'),fruitItem);
  write(path.join(assets,'models/item/betel_palm_leaves.json'),{
    parent:'betel-nut-mod:block/betel_palm_leaves',display:{
      gui:pose([20,35,0],[0,-5,0],.48),ground:pose([0,0,0],[0,2,0],.25),fixed:pose([0,180,0],[0,-4,0],.45),
      thirdperson_righthand:pose([75,0,0],[0,1,0],.30),thirdperson_lefthand:pose([75,0,0],[0,1,0],.30),
      firstperson_righthand:pose([0,-25,0],[-2,3.5,0],.20),firstperson_lefthand:pose([0,25,0],[-2,3.5,0],.20)
    }
  });
  console.log('Integrated 24 Java leaf fallback models, three fruit models, and 108 blockstate variants.');
}
if(require.main===module) integrate();
module.exports=integrate;
