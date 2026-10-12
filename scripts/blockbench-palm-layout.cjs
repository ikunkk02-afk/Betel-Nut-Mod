// Parametric cube placement for the Blockbench MCP authoring session.
// This describes native editable cubes, not a substitute Java-model exporter.
let parts;
let style=0;
const profiles=[
  {name:'balanced',rise:1.0,curve:.024,length:48,width:10.2,pairs:16,sway:1.2,young:[22.5,157.5,292.5]},
  {name:'upright',rise:1.30,curve:.024,length:43,width:8.6,pairs:16,sway:-1.6,young:[0,112.5,225,292.5]},
  {name:'drooping',rise:.62,curve:.019,length:48,width:11.2,pairs:17,sway:2.2,young:[67.5,247.5]},
  {name:'open',rise:1.08,curve:.025,length:45,width:7.5,pairs:12,sway:-2.0,young:[22.5,135,270]}
];
const variants={};
const round = n => Math.round(n * 10000) / 10000;
const greens = [[26,10],[34,16],[28,20],[29,24],[36,27],[45,30],[38,33],[36,36],[24,39],[20,42],[22,45],[39,48]];
function box(part, name, center, size, yaw=0, material='leaf', shade=0) {
  // Java permits a single rotation axis and angles up to 45 degrees.
  const quarter = Math.round(yaw / 90), angle = round(yaw - quarter * 90);
  if (Math.abs(quarter % 2) === 1) size = [size[2],size[1],size[0]];
  const cube = {name, box_uv:false, from:center.map((n,i)=>round(n-size[i]/2)), to:center.map((n,i)=>round(n+size[i]/2)), material, shade};
  if (angle) cube.rotation = {axis:'y', angle, origin:center.map(round)};
  parts[part].push(cube);
}
function frond({name,yaw,length,young=false,diagonal=false}) {
  const a=yaw*Math.PI/180, dir=[Math.sin(a),-Math.cos(a)], side=[Math.cos(a),Math.sin(a)];
  const profile=profiles[style];
  const height = young ? s=>5+1.4*s : s=>4+profile.rise*s-profile.curve*s*s;
  const partAt = s => young ? 0 : diagonal ? (s<25?4:5) : (s<24?1:s<40?2:3);
  const point = (s,sideways=0,drop=0) => {
    const bend=sideways+(young?0:profile.sway*Math.sin(Math.PI*s/length));
    return [8+dir[0]*s+side[0]*bend,height(s)-drop,8+dir[1]*s+side[1]*bend];
  };
  const offset = part => part===0?[0,0,0]:part===4?[16,0,-16]:part===5?[16,0,-32]:[0,0,-16*part];
  const put = (part,label,center,size,angle,material,shade) => box(part,name+'_'+label,center.map((v,i)=>v-offset(part)[i]),size,angle,material,shade);
  const step=young?2:3;
  for(let s=0;s<length;s+=step) {
    const end=Math.min(length,s+step), p=point((s+end)/2), thick=young?.85:1.15-.45*s/length;
    put(partAt((s+end)/2),'rachis_'+s,p,[thick,Math.abs(height(end)-height(s))+thick,end-s+.45],-yaw,'stem',0);
  }
  const count=young?6:profile.pairs;
  for(let i=0;i<count;i++) {
    const s=(young?2.3:5)+(length-(young?3.5:6))*i/(count-1);
    const widthBase=(young?4.1:profile.width)*Math.pow(Math.sin(Math.PI*(s+1)/(length+4)),.75);
    for(const sign of [-1,1]) {
      const width=widthBase*(sign>0?1.03:.90)*(1+.07*Math.sin(i*1.7+style));
      const sweep=22.5*Math.PI/180, pinDir=[side[0]*sign*Math.cos(sweep)+dir[0]*Math.sin(sweep),side[1]*sign*Math.cos(sweep)+dir[1]*Math.sin(sweep)];
      const segments=young?1:2;
      for(let j=0;j<segments;j++) {
        const start=j===0?0:width*.63, end=j===0?(segments===1?width:width*.7):width;
        const p=point(s), mid=(start+end)/2;
        p[0]+=pinDir[0]*mid; p[2]+=pinDir[1]*mid; p[1]-=j*.5+(sign<0?.15:0);
        const angle=-Math.atan2(pinDir[1],pinDir[0])*180/Math.PI;
        put(partAt(s),`pinna_${i}_${sign>0?'R':'L'}_${j}`,p,[end-start+.35,young?.7:j===0?.85:.65,young?1.45:j===0?3.2:1.8],round(angle),'leaf',(i+(sign>0?2:0)+(j?3:0))%greens.length);
      }
    }
  }
}
// An opaque blade material keeps the modeled leaflets solid at every mip level.
function uv(cube) {
  if(cube.material==='stem') return [0,8,2,24];
  const x=(cube.shade*3)%24, y=(cube.shade%3)*6;
  return [x,y,x+3,y+10];
}
for(style=0;style<profiles.length;style++) {
  parts=Object.fromEntries([0,1,2,3,4,5].map(n=>[n,[]]));
  frond({name:'mature_north',yaw:0,length:profiles[style].length});
  frond({name:'mature_northeast',yaw:45,length:profiles[style].length-2,diagonal:true});
  box(0,'crownshaft_core',[8,3.5,8],[6,7,6],0,'stem');
  for(const yaw of profiles[style].young)frond({name:'young_'+yaw,yaw,length:16-(yaw%3),young:true});
  for(const cubes of Object.values(parts))for(const cube of cubes)cube.uv=uv(cube);
  variants[style]=parts;
}
const result={parts:variants[0],variants,profiles,greens};
if(require.main===module) console.log(JSON.stringify(result));
module.exports=result;
