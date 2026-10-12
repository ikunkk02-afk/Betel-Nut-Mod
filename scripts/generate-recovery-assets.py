"""Generate recovery recipes and text; preserve all existing nut processing recipes."""
from pathlib import Path
import json
import runpy

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / 'src/main/resources/data/betel-nut-mod'
ASSETS = ROOT / 'src/main/resources/assets/betel-nut-mod'
MOD = 'betel-nut-mod:'

def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2)+'\n', encoding='utf-8')

def condition(present):
    loaded = {'condition':'fabric:all_mods_loaded', 'values':['farmersdelight']}
    return [loaded if present else {'condition':'fabric:not','value':loaded}]

SPECS = [
    ('golden_apple_paste', ['golden_apple', 'honey_bottle'], 100),
    ('enchanted_golden_apple_paste', ['enchanted_golden_apple', 'honey_bottle'], 160),
    ('relief_potion', ['honey_bottle', 'dandelion', 'sugar'], 120),
]

def generate():
    for name, materials, ticks in SPECS:
        ingredients = [{'item':'minecraft:'+x} for x in materials]
        result = {'id':MOD+name, 'count':1}
        save(DATA/f'recipe/{name}.json', {
            'fabric:load_conditions':condition(False), 'type':'minecraft:crafting_shapeless', 'category':'misc',
            'ingredients':ingredients+[{'item':'minecraft:glass_bottle'}], 'result':result})
        pot='compat/farmersdelight/cooking_'+name
        save(DATA/f'recipe/{pot}.json', {
            'fabric:load_conditions':condition(True), 'type':'farmersdelight:cooking', 'recipe_book_tab':'drinks',
            'ingredients':ingredients, 'result':result,
            'container':{'id':'minecraft:glass_bottle','count':1}, 'cookingtime':ticks, 'experience':0.1})
        for recipe,present in [(name,False),(pot,True)]:
            save(DATA/f'advancement/recipes/{recipe}.json', {
                'fabric:load_conditions':condition(present), 'parent':'minecraft:recipes/root',
                'criteria':{
                    'has_material':{'trigger':'minecraft:inventory_changed','conditions':{'items':[{'items':'minecraft:'+materials[0]}]}},
                    'has_the_recipe':{'trigger':'minecraft:recipe_unlocked','conditions':{'recipe':MOD+recipe}}},
                'requirements':[['has_material','has_the_recipe']], 'rewards':{'recipes':[MOD+recipe]}})
        save(ASSETS/f'models/item/{name}.json', {'parent':'minecraft:item/generated','textures':{'layer0':MOD+'item/'+name}})

    soft=['bread','baked_potato','pumpkin_pie','cake','rabbit_stew','mushroom_stew','beetroot_soup','suspicious_stew','honey_bottle']
    save(DATA/'tags/item/withdrawal_stage2_allowed_foods.json',{'replace':False,'values':['minecraft:'+x for x in soft]})
    save(DATA/'tags/item/withdrawal_stage3_allowed_foods.json',{'replace':False,'values':[MOD+x[0] for x in SPECS]})
    text={
        'item.betel-nut-mod.golden_apple_paste':('金苹果糨糊','Golden Apple Paste'),
        'item.betel-nut-mod.enchanted_golden_apple_paste':('附魔金苹果糨糊','Enchanted Golden Apple Paste'),
        'item.betel-nut-mod.relief_potion':('缓解药剂','Relief Potion'),
        'tooltip.betel-nut-mod.recovery_drink':('无需咀嚼；无法张嘴时仍可饮用。','No chewing needed; drinkable with a locked jaw.'),
        'tooltip.betel-nut-mod.golden_apple_paste':('保留金苹果的增益与依赖恢复效果；饮用后返还玻璃瓶。','Retains golden apple buffs and dependence recovery; returns a glass bottle.'),
        'tooltip.betel-nut-mod.enchanted_golden_apple_paste':('保留附魔金苹果的增益与恢复效果，可修复口腔损伤。','Retains enchanted apple buffs and recovery, including oral repair.'),
        'tooltip.betel-nut-mod.relief_potion':('每天一次：依赖和口腔暴露各减 1 天，缓解戒断 2 分钟。','Once per world day: -1 dependence day, -1 oral exposure day; 2 min withdrawal relief.'),
        'message.betel-nut-mod.relief_potion':('药剂减轻了 1 天依赖和口腔暴露，暂时缓解了戒断。','The remedy eased dependence and oral exposure by one day and temporarily relieved withdrawal.'),
        'message.betel-nut-mod.relief_already_used':('今天已使用缓解药剂，请等下一个主世界日再用。','Relief potion already used today. Wait for the next Overworld day.'),
        'message.betel-nut-mod.relief_not_needed':('目前没有需要缓解的依赖、戒断或口腔损伤。','There is currently no dependence, withdrawal or oral damage to relieve.'),
        'message.betel-nut-mod.eating_restriction_stage2':('现在只能吃面包、熟马铃薯等软食；金苹果需要先制成糨糊。','Only soft foods such as bread or baked potatoes are edible. Mash golden apples into paste first.'),
        'message.betel-nut-mod.eating_restriction_stage3':('你无法进食。可饮用恢复糨糊、缓解药剂、牛奶或药水。','You cannot eat. Recovery pastes, relief potion, milk and potions can still be drunk.'),
        'message.betel-nut-mod.eating_restriction_stage4':('你无法进食。请使用可饮用的恢复用品。','You cannot eat. Use a drinkable remedy.'),
        'message.betel-nut-mod.mouth_locked':('你无法张嘴进食，软食和汤也不行。可饮用恢复糨糊、缓解药剂、牛奶或药水。','Your jaw is locked: even soft foods and soups are blocked. Drink recovery paste, relief potion, milk or potions.'),
        'hud.betel-nut-mod.mouth.1':('进食：仅软食；金苹果需制成糨糊','Eating: soft food only; mash golden apples first'),
        'hud.betel-nut-mod.mouth.2':('进食：禁止食物，仅可饮用恢复用品','Eating: no food; drinkable remedies only'),
        'tag.item.betel-nut-mod.withdrawal_stage2_allowed_foods':('可用软食','Allowed Soft Foods'),
        'tag.item.betel-nut-mod.withdrawal_stage3_allowed_foods':('可饮用恢复用品','Drinkable Remedies'),
    }
    for locale,index in [('zh_cn',0),('en_us',1)]:
        path=ASSETS/f'lang/{locale}.json'
        values=json.loads(path.read_text(encoding='utf-8'))
        values.update({key:pair[index] for key,pair in text.items()})
        save(path, values)
    print('Generated three recovery routes, recipe unlocks, item models, food tags and bilingual text.')

if __name__=='__main__': generate()
