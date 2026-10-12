// V3 editable Java-block geometry. Coordinates retain the existing 21-block crown layout.
const fs = require('node:fs'), path = require('node:path'), crypto = require('node:crypto');
const root = path.resolve(__dirname, '..');
const out = path.join(root, 'art-rework/palm-v3');
const assets = path.join(root, 'src/main/resources/assets/betel-nut-mod');
const names = ['betel_palm_leaves','betel_palm_frond_1','betel_palm_frond_2','betel_palm_frond_3','betel_palm_frond_diagonal_1','betel_palm_frond_diagonal_2'];
const textures = ['betel_palm_leaf_blade','betel_palm_crownshaft','betel_palm_log','betel_nut_fruit_skin'];
const offsets = [[0,0,0],[0,0,-16],[0,0,-32],[0,0,-48],[16,0,-16],[16,0,-32]];
const profiles = [
 {name:'balanced',length:46,rise:2.05,curve:.051,width:14.6,pairs:17,sway:1.4,young:[22.5,157.5,270]},
 {name:'upright',length:43,rise:2.22,curve:.050,width:12.7,pairs:16,sway:-1.5,young:[0,135,247.5]},
 {name:'drooping',length:48,rise:1.53,curve:.037,width:16,pairs:18,sway:1.8,young:[45,202.5]},
 {name:'open',length:44,rise:1.90,curve:.047,width:14.3,pairs:15,sway:-2,young:[67.5,180,292.5]}
];
const round=n=>Math.round(n*10000)/10000;
const uid = label => {const h=crypto.createHash('sha256').update('palm-v3:'+label).digest('hex');return `${h.slice(0,8)}-${h.slice(8,12)}-4${h.slice(13,16)}-a${h.slice(17,20)}-${h.slice(20,32)}`;};
const write=(file,data)=>{fs.mkdirSync(path.dirname(file),{recursive:true});fs.writeFileSync(file,JSON.stringify(data,null,2)+'\n');};
function box(name,center,size,yaw=0,material=0,uv=[1,2,4,14]) {
 const q=Math.round(yaw/90),angle=round(yaw-q*90);
 if(Math.abs(q%2)===1)size=[size[2],size[1],size[0]];
 return {name,from:center.map((v,i)=>round(v-size[i]/2)),to:center.map((v,i)=>round(v+size[i]/2)),
  ...(angle?{rotation:{axis:'y',angle,origin:center.map(round)}}:{}),
  faces:Object.fromEntries(['north','east','south','west','up','down'].map(face=>[face,{uv,texture:'#'+material,...(['up','down'].includes(face)?{rotation:90}:{})}]))};
}
function shifted(e,offset) {
 const result=structuredClone(e);
 for(const k of ['from','to'])result[k]=result[k].map((n,i)=>round(n+offset[i]));
 if(result.rotation)result.rotation.origin=result.rotation.origin.map((n,i)=>round(n+offset[i]));
 return result;
}
function frond(parts,profile,{yaw,length,young=false,diagonal=false}) {
 const a=yaw*Math.PI/180,dir=[Math.sin(a),-Math.cos(a)],side=[Math.cos(a),Math.sin(a)];
 const h=s=>young?4+1.63*s-.018*s*s:4+profile.rise*s-profile.curve*s*s;
 const partAt=s=>young?0:diagonal?(s<25?4:5):(s<24?1:s<39?2:3);
 const point=(s,lateral=0)=>[8+dir[0]*s+side[0]*(lateral+profile.sway*Math.sin(s/length*Math.PI)),h(s),8+dir[1]*s+side[1]*(lateral+profile.sway*Math.sin(s/length*Math.PI))];
 const put=(s,e)=>{const p=partAt(s);parts[p].push(shifted(e,offsets[p].map(v=>-v)));};
 const stemStep=1.65;
 for(let s=0;s<length;s+=stemStep) {
  const end=Math.min(length,s+stemStep),m=(s+end)/2,t=m/length;
  put(m,box(`frond_${yaw}_rachis_${round(s)}`,point(m),[.8-.42*t,Math.abs(h(end)-h(s))+.45,end-s+.22],-yaw,0,[3,1,4,14]));
 }
 const pairs=young?10:profile.pairs;
 for(let i=0;i<pairs;i++)for(const sign of [-1,1]) {
  const s=(young?2.8:4.7)+(length-(young?4:6.2))*i/(pairs-1)+(sign===1?.40:0),t=Math.min(.999,s/length);
  const blade=(young?6.7:profile.width)*Math.pow(Math.sin(Math.PI*t),.77)*(1+.10*Math.sin(i*2.2+sign));
  // Swept, overlapping lanceolate pinnae. Narrow tips and falling edges give volume.
  const sweep=Math.PI/4, v=[side[0]*sign*Math.cos(sweep)+dir[0]*Math.sin(sweep),side[1]*sign*Math.cos(sweep)+dir[1]*Math.sin(sweep)];
  const heading=-Math.atan2(v[1],v[0])*180/Math.PI;
  const cuts=[0,.45,.78,1], widths=[young?1.8:3.15,young?1.05:2.0,.64];
  for(let j=0;j<3;j++) {
   const lo=cuts[j]*blade,hi=cuts[j+1]*blade,mid=(lo+hi)/2,p=point(s);
   p[0]+=v[0]*mid;p[2]+=v[1]*mid;
   p[1]=h(Math.min(length,s+mid*.40))-(young?.13:.24)*mid-.014*mid*mid+(sign===1?.28:0);
   const x=(i*3+ (sign===1?5:0))%22/2;
   put(s,box(`frond_${yaw}_pinna_${i}_${sign}_${j}`,p,[hi-lo+.23,.5-j*.12,widths[j]*(.6+.4*Math.sin(Math.PI*t))],heading,0,[x,1,x+1.3,13]));
  }
 }
}
function makeCrown(style) {
 const p=profiles[style],parts=offsets.map(()=>[]);
 frond(parts,p,{yaw:0,length:p.length});
 frond(parts,p,{yaw:45,length:p.length-2,diagonal:true});
 parts[0].push(box('crown_heart',[8,3,8],[5.8,6,5.8],0,0,[3,1,5,15]));
 for(let i=0;i<p.young.length;i++)frond(parts,p,{yaw:p.young[i],length:19-i*.9,young:true});
 return parts;
}
function makeFruit(age) {
 const e=[box('short_peduncle',[8,14.3,20.55],[1.1,1.0,1.9],0,1),box('descending_spadix',[8,4.3,19.6],[.75,20.4,.75],0,1)];
 for(let row=0;row<7;row++) {
  const count=[4,5,6,6,5,4,3][row],radius=[2.2,3.2,3.7,3.5,3.0,2.2,1.0][row];
  for(let j=0;j<count;j++) {
   const a=(j/count*Math.PI*2)+row*.58,x=8+Math.cos(a)*radius,z=17.7+Math.sin(a)*radius*.44;
   const y=11.6-row*2.8+.35*Math.sin(j*2.3),size=2.1+(j%3)*.14;
   const branchLength=Math.hypot(x-8,z-19.6),branchYaw=Math.round(-Math.atan2(z-19.6,x-8)*180/Math.PI/22.5)*22.5;
   e.push(box(`branch_${row}_${j}`,[(8+x)/2,y+1.8,(19.6+z)/2],[branchLength+.25,.35,.32],branchYaw,1,[4,1,5,14]));
   if(age===0)continue;
   const mat=age===1?0:3,u=(row*3+j*2)%10;
   e.push(box(`fruit_${row}_${j}_body`,[x,y,z],[size,2.25,size],0,mat,[u,2,u+2,8]));
   e.push(box(`fruit_${row}_${j}_shoulder`,[x,y+1.42,z],[size*.76,.66,size*.76],0,mat,[u,2,u+2,5]));
   e.push(box(`fruit_${row}_${j}_tip`,[x,y-1.42,z],[size*.62,.66,size*.62],0,mat,[u,5,u+2,8]));
  }
 }
 return e;
}
function javaModel(elements) {return {credit:'Areca Palm V3 - editable cube model',ambientocclusion:false,textures:Object.fromEntries(textures.map((s,i)=>[i,'betel-nut-mod:block/'+s])),elements};}
function bbmodel(name,sections) {
 const model={meta:{format_version:'5.0',model_format:'java_block',box_uv:false},name,java_block_version:'1.21.1',ambientocclusion:false,resolution:{width:32,height:32},elements:[],groups:[],outliner:[],textures:[]};
 for(const [section,elements] of Object.entries(sections)) {
  const gid=uid(name+'/'+section),children=[];
  model.groups.push({name:section,origin:[8,0,8],uuid:gid,export:true,isOpen:false,visibility:true});
  for(let i=0;i<elements.length;i++) {
   const e=elements[i],id=uid(name+'/'+section+'/'+i),rot=[0,0,0];
   if(e.rotation)rot['xyz'.indexOf(e.rotation.axis)]=e.rotation.angle;
   model.elements.push({name:e.name,type:'cube',uuid:id,box_uv:false,from:e.from,to:e.to,origin:e.rotation?.origin||[8,0,8],rotation:rot,rescale:false,autouv:0,color:2,
    faces:Object.fromEntries(Object.entries(e.faces).map(([k,v])=>[k,{...v,uv:v.uv.map(n=>n*2),texture:Number(v.texture.slice(1))}]))});
   children.push(id);
  }
  model.outliner.push({uuid:gid,isOpen:false,children});
 }
 for(let i=0;i<textures.length;i++) {
  const file=path.join(out,'assets/betel-nut-mod/textures/block',textures[i]+'.png');
  model.textures.push({name:textures[i]+'.png',id:String(i),uuid:uid(textures[i]),relative_path:'assets/betel-nut-mod/textures/block/'+textures[i]+'.png',width:32,height:32,uv_width:32,uv_height:32,internal:true,saved:true,render_mode:'default',render_sides:'auto',source:'data:image/png;base64,'+fs.readFileSync(file).toString('base64')});
 }
 return model;
}
// Continuous folded mesh leaves are the production crown; Java cubes remain a fallback.
function meshFrond(parts,p,{yaw,length,young=false,diagonal=false}) {
 const a=yaw*Math.PI/180,dir=[Math.sin(a),-Math.cos(a)],side=[Math.cos(a),Math.sin(a)];
 const h=s=>young?4+1.63*s-.018*s*s:4+p.rise*s-p.curve*s*s;
 const point=s=>[8+dir[0]*s+side[0]*p.sway*Math.sin(s/length*Math.PI),h(s),8+dir[1]*s+side[1]*p.sway*Math.sin(s/length*Math.PI)];
 const partAt=s=>young?0:diagonal?(s<25?4:5):(s<24?1:s<39?2:3);
 function add(name,s,quads) {const part=partAt(s);parts[part].push({name,quads:quads.map(q=>({...q,vertices:q.vertices.map(v=>v.map((n,i)=>round(n-offsets[part][i])))}))});}
 const uv=[[0,0],[1,0],[1,1],[0,1]];
 for(let s=0;s<length;s+=2.5){
  const end=Math.min(length,s+2.5),from=point(s),to=point(end),radius=.42-.23*s/length,quads=[];
  const ring=(v,r)=>[[v[0]+side[0]*r,v[1],v[2]+side[1]*r],[v[0],v[1]+r,v[2]],[v[0]-side[0]*r,v[1],v[2]-side[1]*r],[v[0],v[1]-r,v[2]]];
  const r0=ring(from,radius),r1=ring(to,radius*.93);
  for(let j=0;j<4;j++)quads.push({vertices:[r0[j],r1[j],r1[(j+1)%4],r0[(j+1)%4]],uv,material:0,doubleSided:false});
  add(`Rachis ${yaw} ${s}`,Math.min(length-.01,(s+end)/2),quads);
 }
 const count=young?9:p.pairs;
 for(let i=0;i<count;i++)for(const sign of [-1,1]){
  const s=(young?2.6:4.1)+(length-(young?4.2:5.1))*i/(count-1)+(sign===1?.40:0),t=Math.min(.998,s/length);
  const blade=(young?7.4:p.width)*Math.pow(Math.sin(Math.PI*t),.68)*(1+.08*Math.sin(i*2.2+sign));
  const sweep=(44+4*Math.sin(i*1.72))*Math.PI/180;
  const v=[side[0]*sign*Math.cos(sweep)+dir[0]*Math.sin(sweep),side[1]*sign*Math.cos(sweep)+dir[1]*Math.sin(sweep)],across=[-v[1],v[0]];
  const base=point(s),rows=[],cuts=[0,.28,.64,1],widths=[.12,1,.70,0];
  for(let j=0;j<cuts.length;j++){
   const u=cuts[j],dist=blade*u,w=widths[j]*(young?1.12:1.85)*(.66+.34*Math.sin(Math.PI*t));
   const center=[base[0]+v[0]*dist+side[0]*sign*.9*u*u, h(Math.min(length,s+dist*.35))-(young?.18:.29)*dist-.014*dist*dist,base[2]+v[1]*dist+side[1]*sign*.9*u*u];
   rows.push([-1,0,1].map(k=>[center[0]+across[0]*w*k,center[1]-(k===0?0:w*.36),center[2]+across[1]*w*k]));
  }
  const quads=[],x=(i%6)*.075;
  for(let j=0;j<3;j++)for(let k=0;k<2;k++)quads.push({vertices:[rows[j][k],rows[j][k+1],rows[j+1][k+1],rows[j+1][k]],uv:[[x+k*.07,cuts[j]],[x+(k+1)*.07,cuts[j]],[x+(k+1)*.07,cuts[j+1]],[x+k*.07,cuts[j+1]]],material:0,doubleSided:true});
  add(`Leaf ${yaw} ${i} ${sign}`,s,quads);
 }
}
function meshCrown(style){
 const p=profiles[style],parts=offsets.map(()=>[]);
 const ring=[[5,5],[11,5],[11,11],[5,11]],uv=[[0,0],[.2,0],[.2,1],[0,1]],quads=[];
 for(let i=0;i<4;i++){const a=ring[i],b=ring[(i+1)%4];quads.push({vertices:[[b[0],0,b[1]],[a[0],0,a[1]],[a[0],5,a[1]],[b[0],5,b[1]]],uv,material:1,doubleSided:false});}
 quads.push({vertices:[[5,5,5],[5,5,11],[11,5,11],[11,5,5]],uv,material:1,doubleSided:false});
 quads.push({vertices:[[5,0,5],[11,0,5],[11,0,11],[5,0,11]],uv,material:1,doubleSided:false});
 parts[0].push({name:'Crown heart',quads});
 meshFrond(parts,p,{yaw:0,length:p.length});meshFrond(parts,p,{yaw:45,length:p.length-2,diagonal:true});
 for(let i=0;i<p.young.length;i++)meshFrond(parts,p,{yaw:p.young[i],length:19-i*.9,young:true});
 return parts;
}
function meshBB(model,meshes){
 model.meta.model_format='free';
 // Remove fallback crown cubes; the true folded leaves are independently editable meshes.
 const oldIds=new Set(model.outliner[0].children);model.elements=model.elements.filter(e=>!oldIds.has(e.uuid));model.outliner[0].children=[];
 for(let i=0;i<meshes.length;i++){
  const m=meshes[i],id=uid(model.name+'/mesh/'+i),vertices={},faces={};
  m.quads.forEach((q,j)=>{
   const ids=q.vertices.map((v,k)=>{const key='v'+j+'_'+k;vertices[key]=v;return key;});
   // Collapse the repeated end point of a pointed leaf into a proper triangle.
   const unique=ids.filter((key,k)=>!ids.slice(0,k).some(other=>vertices[other].every((n,c)=>n===vertices[key][c])));
   if(unique.length<3)return;
   faces['f'+j]={vertices:unique,uv:Object.fromEntries(unique.map(key=>[key,q.uv[ids.indexOf(key)].map(n=>n*32)])),texture:q.material};
   if(q.doubleSided)faces['back'+j]={vertices:[...unique].reverse(),uv:Object.fromEntries(unique.map(key=>[key,q.uv[ids.indexOf(key)].map(n=>n*32)])),texture:q.material};
  });
  model.elements.push({name:m.name,type:'mesh',uuid:id,origin:[0,0,0],rotation:[0,0,0],vertices,faces,color:2});model.outliner[0].children.push(id);
 }
 return model;
}
function quarter(e,q) {
 const r=structuredClone(e),rot=p=>{let [x,y,z]=p;for(let j=0;j<q;j++)[x,z]=[16-z,x];return [round(x),y,round(z)];};
 const f=rot(r.from),t=rot(r.to);r.from=f.map((v,i)=>Math.min(v,t[i]));r.to=f.map((v,i)=>Math.max(v,t[i]));
 if(r.rotation)r.rotation.origin=rot(r.rotation.origin);
 return r;
}
function previewScale(e,height) {
 const r=structuredClone(e),p=v=>[round(8+(v[0]-8)/8),round((v[1]+height*16)/8),round(8+(v[2]-8)/8)];
 r.from=p(r.from);r.to=p(r.to);if(r.rotation)r.rotation.origin=p(r.rotation.origin);return r;
}
async function main() {
 fs.mkdirSync(out,{recursive:true});
 const sharp=require(process.env.CODEX_NODE_MODULES?path.join(process.env.CODEX_NODE_MODULES,'sharp'):path.join(process.env.USERPROFILE,'.cache/codex-runtimes/codex-primary-runtime/dependencies/node/node_modules/sharp'));
 const atlas=path.join(out,'materials-master.png');
 if(!fs.existsSync(atlas))fs.copyFileSync('C:/Users/shouy/.codex/generated_images/01a12abe-6d3f-7f63-b07a-60cfde6edf3f/exec-2ebbc7f2-9558-458e-a4b8-8758114d41cf.png',atlas);
 const metadata=await sharp(atlas).metadata(),half=Math.floor(metadata.width/2);
 for(let i=0;i<4;i++) {
  const dir=path.join(out,'assets/betel-nut-mod/textures/block');fs.mkdirSync(dir,{recursive:true});
  await sharp(atlas).extract({left:i%2*half,top:Math.floor(i/2)*half,width:half,height:half}).resize(32,32,{kernel:'nearest'}).removeAlpha().png({palette:true,colours:16,dither:0}).toFile(path.join(dir,textures[i]+'.png'));
 }
 const crowns=profiles.map((p,i)=>makeCrown(i)),meshCrowns=profiles.map((p,i)=>meshCrown(i));
 const card=[[0,1,2,0],[1,1,0,3],[2,0,2,3],[3,2,1,3]],diag=[[1,3,0,2],[1,3,1,0],[2,2,3,0],[3,0,3,1]];
 for(let s=0;s<4;s++)for(let part=0;part<6;part++) {
  const name=names[part]+(s?'_style_'+s:'');write(path.join(out,name+'.json'),javaModel(crowns[s][part]));write(path.join(out,name+'.bbmodel'),bbmodel(name,{[part===0?'Crown heart and new leaves':'Arching frond']:crowns[s][part]}));
  write(path.join(out,'mesh',name+'.json'),{format:1,textures:textures.map(t=>'betel-nut-mod:block/'+t),quads:meshCrowns[s][part].flatMap(m=>m.quads)});
  const meshSource=meshBB(bbmodel(name,{Crown:crowns[s][part]}),meshCrowns[s][part]);
  for(const texture of meshSource.textures)texture.relative_path='../assets/betel-nut-mod/textures/block/'+texture.name;
  write(path.join(out,'mesh',name+'.bbmodel'),meshSource);
 }
 for(let age=0;age<3;age++){const name='betel_nut_cluster_'+age;write(path.join(out,name+'.json'),javaModel(makeFruit(age)));write(path.join(out,name+'.bbmodel'),bbmodel(name,{'Pendulous fruit bunch':makeFruit(age)}));}
 for(let s=0;s<4;s++) {
  const height=[10,12,11,9][s],crown=[...crowns[s][0]];
  for(let q=0;q<4;q++)for(let part=1;part<6;part++)for(const e of crowns[(part<4?card:diag)[s][q]][part])crown.push(quarter(shifted(e,offsets[part]),q));
  const trunk=[];
  for(let y=0;y<height;y++)trunk.push(box('trunk_'+y,[8,-height*16+y*16+8,8],[6,16,6],0,y>=height-2?1:2,[5,0,11,16]));
  const fruit=[];
  for(let q=0;q<2;q++)for(const e of makeFruit(q===0?2:1))fruit.push(quarter(shifted(e,[0,-48,-16]),q*2));
  const sections={'Crown - swept tapered leaves':crown.map(e=>previewScale(e,height)),'Trunk - ring scars':trunk.map(e=>previewScale(e,height)),'Fruit - hanging clusters':fruit.map(e=>previewScale(e,height))};
  const assembled=structuredClone(meshCrowns[s][0]);
  for(let q=0;q<4;q++)for(let part=1;part<6;part++)for(const mesh of meshCrowns[(part<4?card:diag)[s][q]][part]){
   const m=structuredClone(mesh);m.quads.forEach(face=>face.vertices=face.vertices.map(v=>{let [x,y,z]=v.map((n,i)=>n+offsets[part][i]);for(let j=0;j<q;j++)[x,z]=[16-z,x];return [x,y,z];}));assembled.push(m);
  }
  for(const m of assembled)for(const q of m.quads)q.vertices=q.vertices.map(v=>[round(8+(v[0]-8)/8),round((v[1]+height*16)/8),round(8+(v[2]-8)/8)]);
  write(path.join(out,'areca-palm-v3-'+profiles[s].name+'.bbmodel'),meshBB(bbmodel('Areca Palm V3 - '+profiles[s].name+' (1 to 8)',sections),assembled));
  write(path.join(out,'areca-palm-v3-'+profiles[s].name+'.json'),javaModel(Object.values(sections).flat()));
 }
 write(path.join(out,'design.json'),{version:3,profiles,source:'scripts/build-palm-v3.cjs',reference:'https://www.nparks.gov.sg/florafaunaweb/flora/2/5/2587',materials:'Built-in imagegen: 2x2 atlas, smooth palm materials, 32px per tile; master retained.',coordinateSystem:'16 units per block; full-tree assembly at 1:8; component files at 1:1'});
 const primary=JSON.parse(fs.readFileSync(path.join(out,'areca-palm-v3-balanced.bbmodel')));
 primary.name='Areca Palm V3 - close hanging fruit';write(path.join(out,'areca-palm-v3-final.bbmodel'),primary);
 if(process.argv.includes('--install')) {
  for(const f of fs.readdirSync(out))if(/^betel_.*\.(json|bbmodel)$/.test(f)){fs.copyFileSync(path.join(out,f),path.join(root,'art-rework/blockbench',f));}
  for(const t of textures)for(const dir of [path.join(assets,'textures/block'),path.join(root,'art-rework/blockbench/assets/betel-nut-mod/textures/block')])fs.copyFileSync(path.join(out,'assets/betel-nut-mod/textures/block',t+'.png'),path.join(dir,t+'.png'));
  fs.mkdirSync(path.join(assets,'palm_mesh'),{recursive:true});
  for(const f of fs.readdirSync(path.join(out,'mesh')))if(f.endsWith('.json'))fs.copyFileSync(path.join(out,'mesh',f),path.join(assets,'palm_mesh',f));
  require('./integrate-blockbench-palm.cjs')();
 }
 console.log(JSON.stringify({output:out,crownParts:crowns.map(p=>p.map(a=>a.length)),installed:process.argv.includes('--install')}));
}
main().catch(e=>{console.error(e);process.exitCode=1;});
