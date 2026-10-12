// Export one 1:1 Blockbench mesh component after editing vertices/UVs.
const fs=require('node:fs'),path=require('node:path');
const [source,destination]=process.argv.slice(2);
if(!source||!destination)throw new Error('Usage: node scripts/export-palm-mesh.cjs <component.bbmodel> <palm_mesh.json>');
const model=JSON.parse(fs.readFileSync(source));
if(model.meta?.model_format!=='free')throw new Error('Expected a Blockbench generic mesh component');
if(!/^betel_palm_(leaves|frond_)/.test(model.name))throw new Error('Use an original-scale leaf component, not the 1:8 assembled tree');
const rotated=o=>o.rotation?.some(n=>Math.abs(n)>1e-8);
if((model.groups||[]).some(rotated))throw new Error('Bake group rotations into mesh vertices before export');
const textures=model.textures.map(t=>'betel-nut-mod:block/'+path.basename(t.name,'.png'));
const quads=[];
for(const mesh of model.elements){
 if(mesh.type!=='mesh'||mesh.export===false)continue;
 if(rotated(mesh))throw new Error('Bake mesh rotations into vertices before export: '+mesh.name);
 const origin=mesh.origin||[0,0,0];
 for(const face of Object.values(mesh.faces)){
  if(face.texture===null)continue;
  const ids=[...face.vertices];if(ids.length!==3&&ids.length!==4)throw new Error('Triangulate n-gons before exporting');
  if(ids.length===3)ids.push(ids[2]);
  const material=typeof face.texture==='number'?face.texture:model.textures.findIndex(t=>t.uuid===face.texture||t.id===face.texture);
  if(!textures[material])throw new Error('Missing face texture');
  const texture=model.textures[material];
  const vertices=ids.map(id=>mesh.vertices[id].map((n,i)=>n+origin[i]));
  if(!vertices.every(v=>v.every(Number.isFinite)))throw new Error('Invalid mesh coordinate');
  const uv=ids.map(id=>[face.uv[id][0]/(texture.uv_width||model.resolution.width),face.uv[id][1]/(texture.uv_height||model.resolution.height)]);
  quads.push({vertices,uv,material,doubleSided:false});
 }
}
if(!quads.length)throw new Error('The component contains no mesh faces');
fs.mkdirSync(path.dirname(path.resolve(destination)),{recursive:true});
fs.writeFileSync(destination,JSON.stringify({format:1,textures,quads},null,2)+'\n');
console.log(JSON.stringify({source,destination,quads:quads.length}));
