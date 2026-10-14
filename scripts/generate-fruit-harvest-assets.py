"""Keep palm assets; generate whole-bunch harvest, processing and seed data."""
from pathlib import Path
from copy import deepcopy
import json

ROOT=Path(__file__).resolve().parent.parent
ASSETS=ROOT/'src/main/resources/assets/betel-nut-mod'
DATA=ROOT/'src/main/resources/data/betel-nut-mod'
MOD='betel-nut-mod:'

def save(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

def read(path):return json.loads(path.read_text(encoding='utf-8'))

def unlock(name,material,conditions=None):
    value={'parent':'minecraft:recipes/root','criteria':{
        'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':MOD+material}]}},
        'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':MOD+name}}},
        'requirements':[['has_material','has_the_recipe']],'rewards':{'recipes':[MOD+name]}}
    if conditions:value['fabric:load_conditions']=conditions
    save(DATA/f'advancement/recipes/{name}.json',value)

def generate():
    leaf=read(DATA/'loot_table/blocks/betel_palm_leaves.json')
    # Keep vanilla shears/Silk Touch whole-leaf collection for building.
    leaf['pools']=leaf['pools'][:1]+[{
        'rolls':1,'conditions':[deepcopy(leaf['pools'][1]['conditions'][0]),{'condition':'minecraft:survives_explosion'},
                              {'condition':'minecraft:random_chance','chance':0.5}],
        'entries':[{'type':'minecraft:item','name':MOD+'betel_leaf'}]}]
    save(DATA/'loot_table/blocks/betel_palm_leaves.json',leaf)
    save(DATA/'loot_table/blocks/betel_nut_cluster.json',{'type':'minecraft:block','pools':[
        {'rolls':1,'conditions':[
            {'condition':'minecraft:block_state_property','block':MOD+'betel_nut_cluster','properties':{'age':str(age)}},
            {'condition':'minecraft:survives_explosion'}],
         'entries':[{'type':'minecraft:item','name':MOD+item}]}
        for age,item in [(1,'unripe_betel_nut_cluster'),(2,'betel_nut_cluster')]]})

    # No fruit or seeds can be obtained by processing foliage.
    leaf_cut=read(DATA/'recipe/compat/farmersdelight/cutting_betel_palm_leaves.json')
    leaf_cut['result']=[{'item':{'id':MOD+'betel_leaf','count':2}}]
    save(DATA/'recipe/compat/farmersdelight/cutting_betel_palm_leaves.json',leaf_cut)
    loaded=[{'condition':'fabric:all_mods_loaded','values':['farmersdelight']}]
    for material,count,suffix in [('unripe_betel_nut_cluster',3,'unripe_cluster'),('betel_nut_cluster',5,'ripe_cluster')]:
        name='raw_betel_nut_from_'+suffix
        save(DATA/f'recipe/{name}.json',{'type':'minecraft:crafting_shapeless','category':'misc',
            'ingredients':[{'item':MOD+material}],'result':{'id':MOD+'raw_betel_nut','count':count}})
        unlock(name,material)
        name='compat/farmersdelight/cutting_'+suffix
        save(DATA/f'recipe/{name}.json',{'fabric:load_conditions':loaded,'type':'farmersdelight:cutting',
            'ingredients':[{'item':MOD+material}],'tool':{'tag':'c:tools/knife'},
            'result':[{'item':{'id':MOD+'raw_betel_nut','count':count}}]})
        unlock(name,material,loaded)
    save(DATA/'recipe/betel_palm_sapling.json',{'type':'minecraft:crafting_shapeless','category':'misc',
        'ingredients':[{'item':MOD+'betel_nut_cluster'},{'item':MOD+'betel_leaf'}],
        'result':{'id':MOD+'betel_palm_sapling','count':1}})
    unlock('betel_palm_sapling','betel_nut_cluster')

    # Preserve the previous held/third-person scale and centering. Reuse the actual green model UVs.
    ripe_item=read(ASSETS/'models/item/betel_nut_cluster.json')
    ripe_block=read(ASSETS/'models/block/betel_nut_cluster_2.json')
    green=read(ASSETS/'models/block/betel_nut_cluster_1.json')
    delta=[ripe_item['elements'][0]['from'][axis]-ripe_block['elements'][0]['from'][axis] for axis in range(3)]
    assert len(ripe_item['elements'])==len(ripe_block['elements'])==len(green['elements'])
    for item_element,block_element in zip(ripe_item['elements'],ripe_block['elements']):
        for key in ['from','to']:
            assert all(abs(item_element[key][axis]-block_element[key][axis]-delta[axis])<0.00001 for axis in range(3)), 'Held fruit transform is not a simple translation'
    for element in green['elements']:
        for key in ['from','to']:element[key]=[round(element[key][axis]+delta[axis],4) for axis in range(3)]
        if 'rotation' in element:element['rotation']['origin']=[round(element['rotation']['origin'][axis]+delta[axis],4) for axis in range(3)]
    green['display']=deepcopy(ripe_item['display'])
    save(ASSETS/'models/item/unripe_betel_nut_cluster.json',green)
    text={
        'block.betel-nut-mod.betel_nut_cluster':('成熟槟榔果串','Ripe Betel Nut Bunch'),
        'item.betel-nut-mod.unripe_betel_nut_cluster':('未成熟槟榔果串','Unripe Betel Nut Bunch'),
        'tooltip.betel-nut-mod.cluster.harvest':('持剪刀右键采收完整青果串或黄果串，采后不留果梗。','Right-click with shears to harvest a whole green or yellow bunch; no stalk remains.'),
        'tooltip.betel-nut-mod.cluster.regrow':('完整树干使用骨粉可补齐最多3串黄果；青果串用骨粉立即成熟。','Bone meal on an intact trunk fills up to 3 ripe bunches; it also ripens green bunches instantly.'),
        'tooltip.betel-nut-mod.cluster.ripe':('黄色：拆成 5 个生槟榔，或配槟榔叶制作 1 棵树苗。','Yellow: unpack into 5 raw betel nuts, or combine with a betel leaf for 1 sapling.'),
        'tooltip.betel-nut-mod.cluster.unripe':('绿色：拆成 3 个生槟榔，用于普通槟榔加工。','Green: unpack into 3 raw betel nuts for normal processing.'),
        'tooltip.betel-nut-mod.sapling.seed':('砍伐绿色冠轴可获得树苗；黄果串 + 槟榔叶也可育苗。','Felling green crownshaft logs yields saplings; a ripe bunch plus a betel leaf also makes one.'),
    }
    for locale,index in [('zh_cn',0),('en_us',1)]:
        path=ASSETS/f'lang/{locale}.json';values=read(path)
        values.update({key:pair[index] for key,pair in text.items()});save(path,values)
    print('Generated whole-bunch loot, leaf-only foliage, green/ripe processing, yellow seeds and green held model.')

if __name__=='__main__':generate()
