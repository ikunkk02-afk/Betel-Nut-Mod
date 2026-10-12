const fs=require('node:fs'), path=require('node:path'), assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'), assets=path.join(root,'src/main/resources/assets/betel-nut-mod');
const names=['betel_palm_leaves','betel_palm_frond_1','betel_palm_frond_2','betel_palm_frond_3','betel_palm_frond_diagonal_1','betel_palm_frond_diagonal_2'];
const read=p=>JSON.parse(fs.readFileSync(p));
const variants=read(path.join(assets,'blockstates/betel_palm_leaves.json')).variants;
assert.equal(Object.keys(variants).length,96);
const offsets=[[0,0,0],[0,0,-16],[0,0,-32],[0,0,-48],[16,0,-16],[16,0,-32]];
const report={nativeBlockbenchModels:27,leafBlockstateVariants:96,fruitBlockstateVariants:12,crownProfiles:4,crownBlocks:21,geometry:'passed',exportParity:'passed'};
for(let style=0;style<4;style++)for(let part=0;part<names.length;part++) {
  const name=names[part]+(style?'_style_'+style:''),model=read(path.join(assets,'models/block',name+'.json'));
  const source=read(path.join(root,'art-rework/blockbench',name+'.json'));
  assert.deepEqual(model.elements,source.elements,'Runtime geometry/UVs must match the native Blockbench export');
  for(const element of model.elements) {
    for(const coords of [element.from,element.to]) for(const n of coords) assert(n>=-16 && n<=32,'Java element coordinates out of bounds');
    const r=element.rotation;
    if(r) {assert.equal(r.axis,'y');assert([-45,-22.5,0,22.5,45].includes(r.angle));}
    for(const face of Object.values(element.faces)) {
      assert(!face.cullface,'Overhanging leaves must render across anchor block boundaries');
      assert(model.textures[face.texture.slice(1)].endsWith(':block/betel_palm_leaf_blade'));
      assert(face.uv.every(v=>v>=0 && v<=16),'UV outside blade material');
    }
    // Check rotated vertices against the volume reserved before tree generation.
    for(const x of [element.from[0],element.to[0]]) for(const y of [element.from[1],element.to[1]]) for(const z of [element.from[2],element.to[2]]) {
      let p=[x,y,z];
      if(r){const a=r.angle*Math.PI/180,dx=x-r.origin[0],dz=z-r.origin[2];p=[r.origin[0]+Math.cos(a)*dx+Math.sin(a)*dz,y,r.origin[2]-Math.sin(a)*dx+Math.cos(a)*dz];}
      p=p.map((v,i)=>v+offsets[part][i]);
      assert(p[0]>=-48 && p[0]<=64 && p[2]>=-48 && p[2]<=64 && p[1]>=-16 && p[1]<=32,'Model exceeds reserved crown envelope');
    }
  }
}
for(let age=0;age<3;age++) {
  const name='betel_nut_cluster_'+age;
  const model=read(path.join(assets,'models/block',name+'.json'));
  assert.deepEqual(model.elements,read(path.join(root,'art-rework/blockbench',name+'.json')).elements);
  for(const e of model.elements)for(const p of [e.from,e.to])assert(p.every(n=>n>=-16&&n<=32));
}
assert.equal(Object.keys(read(path.join(assets,'blockstates/betel_nut_cluster.json')).variants).length,12);
fs.writeFileSync(path.join(root,'art-rework/blockbench/model-verification.json'),JSON.stringify(report,null,2)+'\n');
console.log(JSON.stringify(report));
