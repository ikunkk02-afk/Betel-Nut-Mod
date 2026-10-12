// Native Blockbench cube layout: branching peduncle and small ovoid drupes.
const stems=[],fruit=[];
function cube(list,name,center,size,material,angle=0) {
  const c={name,box_uv:false,from:center.map((v,i)=>v-size[i]/2),to:center.map((v,i)=>v+size[i]/2),material};
  if(angle)c.rotation={axis:'y',angle,origin:center};
  list.push(c);
}
cube(stems,'attachment_peduncle',[8,12.5,17],[1.6,1.5,9],'stem');
for(let i=0;i<5;i++)cube(stems,'descending_rachis_'+i,[8,12-i*1.8,13.5-i*2.2],[1,2.6,3],'stem');
const fruits=[[4,9,10],[7,8.5,9],[10.5,9,10],[12,7,8],[3.5,6,7],[6.5,5.5,6],[9.5,5,6.5],[11,3.5,5],
  [5,3,4],[8,2.5,3.5],[6,10,12],[10,10.5,12.5],[4.5,5,11],[8,4.5,10],[11.5,5.5,11]];
fruits.forEach(([x,unusedY,z],i)=>{
  const attachY=4.4+(z-3.5)*.72;
  const y=Math.max(2,attachY-2.6-(i%3)*.15);
  const w=2.05+(i%3)*.13;
  cube(stems,'fruit_branch_'+i,[(x+8)/2,attachY,z],[Math.abs(x-8)+.5,.5,.55],'stem');
  cube(stems,'fruit_pedicel_'+i,[x,(y+1.5+attachY)/2,z],[.4,Math.max(.4,attachY-y-1.5),.4],'stem');
  cube(fruit,'drupe_'+i+'_body',[x,y,z],[w,2.2,w],'fruit');
  cube(fruit,'drupe_'+i+'_upper',[x,y+1.4,z],[w*.78,.8,w*.78],'fruit');
  cube(fruit,'drupe_'+i+'_tip',[x,y-1.4,z],[w*.65,.8,w*.65],'fruit');
});
const result={stems,fruit};
if(require.main===module)console.log(JSON.stringify(result));
module.exports=result;
