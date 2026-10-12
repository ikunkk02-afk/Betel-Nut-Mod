const fs=require('node:fs'),path=require('node:path'),assert=require('node:assert/strict');
const root=path.resolve(__dirname,'..'),out=path.join(root,'art-rework/palm-v3'),assets=path.join(root,'src/main/resources/assets/betel-nut-mod');
const read=p=>JSON.parse(fs.readFileSync(p));
const names=['betel_palm_leaves','betel_palm_frond_1','betel_palm_frond_2','betel_palm_frond_3','betel_palm_frond_diagonal_1','betel_palm_frond_diagonal_2'];
const offsets=[[0,0,0],[0,0,-16],[0,0,-32],[0,0,-48],[16,0,-16],[16,0,-32]];
let quads=0;
const average=points=>[0,1,2].map(i=>points.reduce((sum,p)=>sum+p[i],0)/points.length);
function outward(vertices,center){
 const a=vertices[1].map((v,i)=>v-vertices[0][i]),b=vertices[2].map((v,i)=>v-vertices[0][i]);
 const normal=[a[1]*b[2]-a[2]*b[1],a[2]*b[0]-a[0]*b[2],a[0]*b[1]-a[1]*b[0]],mid=average(vertices);
 return normal.reduce((sum,n,i)=>sum+n*(mid[i]-center[i]),0)>0;
}
for(let style=0;style<4;style++)for(let p=0;p<6;p++){
 const name=names[p]+(style?'_style_'+style:'')+'.json',m=read(path.join(out,'mesh',name));
 assert.deepEqual(m,read(path.join(assets,'palm_mesh',name)),'Source/runtime mesh parity');
 if(p===0){
  const heart=m.quads.filter(q=>q.material===1);
  assert.equal(heart.length,6,'Crown heart must be closed on all six sides');
  for(const q of heart)assert(outward(q.vertices,[8,2.5,8]),'Crown heart has an inward face');
 }
 for(const q of m.quads){
  quads++;
  assert.equal(q.vertices.length,4);assert.equal(q.uv.length,4);
  assert(q.material>=0&&q.material<m.textures.length);
  for(const v of q.vertices){
   assert.equal(v.length,3);assert(v.every(Number.isFinite));
   const world=v.map((n,i)=>n+offsets[p][i]);
   assert(world[0]>=-48&&world[0]<=64&&world[2]>=-48&&world[2]<=64&&world[1]>=-16&&world[1]<=32,'Frond exceeds reserved worldgen crown volume');
  }
  for(const uv of q.uv)assert(uv.length===2&&uv.every(n=>n>=0&&n<=1));
 }
}
const models=[];
for(const style of ['balanced','upright','drooping','open']){
 const m=read(path.join(out,'areca-palm-v3-'+style+'.bbmodel'));
 assert.equal(m.meta.model_format,'free');
 const ids=new Set(m.elements.map(e=>e.uuid));assert.equal(ids.size,m.elements.length);
 for(const group of m.outliner)for(const id of group.children)assert(ids.has(id));
 const meshes=m.elements.filter(e=>e.type==='mesh');assert(meshes.length>100);
 for(const mesh of meshes)for(const f of Object.values(mesh.faces)){
  assert(f.vertices.length>=3&&f.vertices.length<=4);
  for(const id of f.vertices){assert(mesh.vertices[id]);assert(f.uv[id]);}
 }
 for(const mesh of meshes.filter(e=>e.name.startsWith('Rachis '))){
  const center=average(Object.values(mesh.vertices));
  for(const face of Object.values(mesh.faces))assert(outward(face.vertices.map(v=>mesh.vertices[v]),center),'Rachis has an inward face');
 }
 assert(m.textures.every(t=>t.source.startsWith('data:image/png;base64,')));
 models.push({style,meshElements:meshes.length,cubes:m.elements.length-meshes.length});
}
const fruit=read(path.join(out,'betel_nut_cluster_2.json')).elements;
const berries=fruit.filter(e=>e.name.startsWith('fruit_'));
const min=axis=>Math.min(...berries.map(e=>e.from[axis])),max=axis=>Math.max(...berries.map(e=>e.to[axis]));
assert(max(2)<=21.01,'Fruit must remain outside the trunk surface');
assert(21-max(2)<1.0,'Fruit should hug the trunk');
assert(max(1)-min(1)>20,'Bunch should be elongated');
const fruitItem=read(path.join(assets,'models/item/betel_nut_cluster.json'));
for(let axis=0;axis<3;axis++){
 const lo=Math.min(...fruitItem.elements.map(e=>e.from[axis])),hi=Math.max(...fruitItem.elements.map(e=>e.to[axis]));
 assert(Math.abs((lo+hi)/2-8)<1e-6,'Fruit item must be centred independently of its world attachment');
}
for(const item of ['betel_nut_cluster','betel_palm_leaves']){
 const display=read(path.join(assets,'models/item',item+'.json')).display;
 for(const pose of ['firstperson_righthand','firstperson_lefthand','thirdperson_righthand','thirdperson_lefthand','gui','ground','fixed'])assert(display[pose]?.scale.every(n=>n>0&&n<1));
}
const report={meshParts:24,sourceQuads:quads,sourceRuntimeParity:'passed',worldgenEnvelope:'passed',editableModels:models,fruit:{count:berries.length/3,width:max(0)-min(0),length:max(1)-min(1),trunkGap:21-max(2),peduncleLength:fruit[0].to[2]-fruit[0].from[2]}};
fs.writeFileSync(path.join(out,'geometry-verification.json'),JSON.stringify(report,null,2)+'\n');console.log(JSON.stringify(report));
