"""Data and models for the v5 gameplay additions. Art is generated with imagegen separately."""
from pathlib import Path
import json

root = Path(__file__).resolve().parent.parent
resources = root / 'src/main/resources'
assets = resources / 'assets/betel-nut-mod'
data = resources / 'data/betel-nut-mod'
def save_json(path, obj):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(obj, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

names = {
    'shell': ('贝壳', 'Shell'), 'shell_powder': ('贝壳粉', 'Shell Powder'),
    'ancient_shell': ('远古贝壳', 'Ancient Shell'), 'golden_shell': ('纪念金贝壳', 'Commemorative Golden Shell'),
    'wrapped_raw_betel': ('裹叶生槟榔', 'Leaf-wrapped Raw Betel Nut'),
    'echo_betel': ('回响槟榔', 'Echo Betel Nut'), 'empty_pack': ('空槟榔包装', 'Empty Betel Bag'),
    'brand_seal': ('品牌封签', 'Brand Seal'), 'kouweiwang_pack': ('口味王混合装', 'Kouweiwang Assortment'),
    'hecheng_tianxia_pack': ('和成天下混合装', 'Hecheng Tianxia Assortment'),
    'xiangtan_puzi_pack': ('湘潭铺子混合装', 'Xiangtan Puzi Assortment'),
    'lottery_ticket': ('槟榔奖券', 'Betel Prize Ticket'),
}
translations = {
 'tag.item.betel-nut-mod.liquid_foods': ('饮品与汤', 'Drinks and Soups'),
 'tag.item.betel-nut-mod.betel_nuts': ('槟榔', 'Betel Nuts'),
 'tag.item.betel-nut-mod.betel_foods': ('槟榔食品', 'Betel Foods'),
 'tag.item.betel-nut-mod.betel_sweets': ('槟榔甜食', 'Betel Sweets'),
 'tag.item.betel-nut-mod.withdrawal_stage2_allowed_foods': ('戒断可用软食', 'Withdrawal Soft Foods'),
 'tag.item.betel-nut-mod.withdrawal_stage3_allowed_foods': ('强效恢复食品', 'Strong Recovery Foods'),
 'effect.betel-nut-mod.oral_cancer': ('口腔癌', 'Oral Cancer'),
 'effect.betel-nut-mod.mouth_locked': ('无法张嘴', 'Locked Jaw'),
 'effect.betel-nut-mod.echo_sight': ('回响感知', 'Echo Sight'),
 'hud.betel-nut-mod.title': ('槟榔 · 身体状态', 'BETEL / HEALTH'),
 'hud.betel-nut-mod.days': ('依赖 %s / %s 天', 'Dependence: %s / %s days'),
 'hud.betel-nut-mod.today.used': ('今日已计入', 'Counted today'),
 'hud.betel-nut-mod.today.unused': ('今日未食用', 'Unused today'),
 'hud.betel-nut-mod.sober': ('连续停用 %s 天', 'Abstinent: %s days'),
 'hud.betel-nut-mod.next': ('下阶段还需 %s 天', 'Next stage: %s days'),
 'hud.betel-nut-mod.withdrawal': ('戒断强度 %s', 'Withdrawal: %s'),
 'hud.betel-nut-mod.oral.0': ('口腔状态：正常', 'Oral health: clear'),
 'hud.betel-nut-mod.oral.1': ('口腔癌 I · 生命上限降低', 'Oral cancer I / reduced max health'),
 'hud.betel-nut-mod.oral.2': ('口腔癌 II · 仅可食用软食', 'Oral cancer II / soft food only'),
 'hud.betel-nut-mod.oral.3': ('口腔癌 III · 无法张嘴', 'Oral cancer III / locked jaw'),
 'hud.betel-nut-mod.mouth.0': ('进食：正常', 'Eating: unrestricted'),
 'hud.betel-nut-mod.mouth.1': ('进食：仅软食、饮品与槟榔', 'Eating: soft food, drinks and betel'),
 'hud.betel-nut-mod.mouth.2': ('进食：饮品、汤与附魔金苹果', 'Eating: liquids and enchanted apples'),
 'message.betel-nut-mod.mouth_locked': ('你无法张嘴咀嚼。可以喝饮品、汤，或使用附魔金苹果。', 'Your jaw is locked. Liquids, soup and enchanted golden apples can still be used.'),
 'message.betel-nut-mod.pack_opened': ('已拆开包装：6 种槟榔与 1 张奖券。', 'Opened: six different betel nuts and one prize ticket.'),
 'message.betel-nut-mod.lottery.0': ('奖券：谢谢惠顾。', 'Prize ticket: better luck next time.'),
 'message.betel-nut-mod.lottery.1': ('奖券中奖：3 片槟榔叶！', 'Prize: three betel leaves!'),
 'message.betel-nut-mod.lottery.2': ('奖券中奖：1 颗绿宝石！', 'Prize: one emerald!'),
 'message.betel-nut-mod.lottery.3': ('奖券中奖：1 颗蜂蜜槟榔！', 'Prize: one honey betel nut!'),
 'message.betel-nut-mod.lottery.4': ('收藏大奖：纪念金贝壳！', 'Collector jackpot: a commemorative golden shell!'),
 'tooltip.betel-nut-mod.pack': ('右键拆封 · 6 种口味各 1 颗 + 1 张奖券', 'Open: six flavors, one of each, plus one ticket'),
 'tooltip.betel-nut-mod.pack_seal': ('封签与包装拆开后不返还。', 'Opening consumes the bag and its brand seal.'),
 'tooltip.betel-nut-mod.ticket': ('右键刮奖 · 结果在获得奖券时确定', 'Use to scratch; the prize is fixed when issued'),
 'tooltip.betel-nut-mod.ticket_odds': ('未中奖60% · 叶25% · 绿宝石10% · 蜂蜜4% · 金贝壳1%', 'No prize 60% / leaves 25% / emerald 10% / honey 4% / shell 1%'),
 'tooltip.betel-nut-mod.counted_today': ('今日已计入：再次食用不会增加依赖天数。', 'Already counted today; another nut adds no dependence day.'),
 'tooltip.betel-nut-mod.counts_today': ('今日首次食用会增加 1 天依赖。', 'The first nut today adds one dependence day.'),
 'command.betel-nut-mod.oral.set': ('口腔暴露天数设为 %s，疾病等级 %s。', 'Oral exposure set to %s days; disease stage %s.'),
 'betel_nut_mod.addiction.stage.none': ('尝试期 / 无依赖', 'Trial / no dependence'),
 'betel_nut_mod.tooltip.addiction.risk': ('长期食用会形成依赖并影响口腔状态。', 'Long-term use causes dependence and oral status effects.'),
 'betel_nut_mod.tooltip.addiction.increase': ('今日食用将进入下一个依赖阶段。', 'Using today will reach the next dependence stage.'),
 'message.betel-nut-mod.enchanted_golden_apple_recovery': ('魔法减轻了依赖，并修复了部分口腔损伤。', 'Magic eased your dependence and repaired some oral damage.'),
 'screen.betel_nut_mod.hud_edit.title': ('身体状态 HUD 编辑', 'Health HUD Editor'),
 'screen.betel_nut_mod.hud_edit.hint': ('拖动调整位置 · 悬停查看详情 · Esc 保存', 'Drag to move / Hover for details / Esc to save'),
 'hud.betel-nut-mod.compact.days': ('%s/%s 天', '%s/%s d'),
 'hud.betel-nut-mod.compact.stage.0': ('尝试期', 'Trial'),
 'hud.betel-nut-mod.compact.stage.1': ('轻度依赖', 'Mild'),
 'hud.betel-nut-mod.compact.stage.2': ('中度依赖', 'Moderate'),
 'hud.betel-nut-mod.compact.stage.3': ('重度依赖', 'Heavy'),
 'hud.betel-nut-mod.compact.stage.4': ('严重依赖', 'Severe'),
 'hud.betel-nut-mod.compact.stage.5': ('完全成瘾', 'Fully addicted'),
 'hud.betel-nut-mod.compact.oral': ('口腔癌 %s', 'Cancer %s'),
 'hud.betel-nut-mod.compact.mouth.1': ('限制进食', 'Soft food'),
 'hud.betel-nut-mod.compact.mouth.2': ('无法张嘴', 'Jaw locked'),
 'hud.betel-nut-mod.compact.withdrawal': ('戒断 %s', 'Withdrawal %s'),
 'hud.betel-nut-mod.compact.legend': ('绿点：今日已计入 · 每格为一个食用日', 'Green dot: counted today / One segment per eating day'),
 'effect.betel-nut-mod.betel_dependence': ('槟榔依赖', 'Betel Dependence'),
 'hud.betel-nut-mod.compact.symptoms': ('症状 %s', 'Symptoms %s'),
 'hud.betel-nut-mod.symptoms.1': ('依赖症状 I：移速 -5% · 挖掘 -10% · 饥饿加快', 'Dependence I: speed -5%, mining -10%, faster hunger'),
 'hud.betel-nut-mod.symptoms.2': ('依赖症状 II：移速 -10% · 挖掘 -20% · 攻击 -0.5 · 饥饿加快', 'Dependence II: speed -10%, mining -20%, attack -0.5, faster hunger'),
 'hud.betel-nut-mod.symptoms.3': ('依赖症状 III：移速 -15% · 挖掘 -30% · 攻击 -1 · 饥饿加快', 'Dependence III: speed -15%, mining -30%, attack -1, faster hunger'),
 'hud.betel-nut-mod.symptoms.4': ('依赖症状 IV：移速 -20% · 挖掘 -40% · 攻击 -1.5 · 饥饿加快', 'Dependence IV: speed -20%, mining -40%, attack -1.5, faster hunger'),
 'hud.betel-nut-mod.symptoms.5': ('依赖症状 V：移速 -25% · 挖掘 -50% · 攻击 -2 · 饥饿加快', 'Dependence V: speed -25%, mining -50%, attack -2, faster hunger'),
}
for lang, ix in [('zh_cn', 0), ('en_us', 1)]:
    path = assets / f'lang/{lang}.json'
    obj = json.loads(path.read_text(encoding='utf-8'))
    obj.update({f'item.betel-nut-mod.{k}': v[ix] for k, v in names.items()})
    obj.update({k: v[ix] for k, v in translations.items()})
    for key, value in list(obj.items()):
        if key.startswith('command.') and isinstance(value, str):
            if ix == 0: obj[key] = value.replace('成瘾值', '依赖天数')
            else: obj[key] = value.replace('addiction value', 'dependence days').replace('addictionValue', 'dependenceDays')
    save_json(path, obj)

def recipe(name, inputs, output, count=1):
    save_json(data / f'recipe/{name}.json', {'type':'minecraft:crafting_shapeless', 'category':'misc',
        'ingredients':[{'item':i if ':' in i else f'betel-nut-mod:{i}'} for i in inputs],
        'result':{'id':f'betel-nut-mod:{output}', 'count':count}})
    first = inputs[0] if ':' in inputs[0] else f'betel-nut-mod:{inputs[0]}'
    save_json(data / f'advancement/recipes/{name}.json', {
        'parent':'minecraft:recipes/root',
        'criteria':{'has_material':{'trigger':'minecraft:inventory_changed',
            'conditions':{'items':[{'items':first}]}},
            'has_the_recipe':{'trigger':'minecraft:recipe_unlocked',
                'conditions':{'recipe':f'betel-nut-mod:{name}'}}},
        'requirements':[['has_material','has_the_recipe']],
        'rewards':{'recipes':[f'betel-nut-mod:{name}']}})
recipe('shell_powder', ['shell'], 'shell_powder', 4)
recipe('wrapped_raw_betel', ['raw_betel_nut','betel_leaf','shell_powder'], 'wrapped_raw_betel')
# Variant recipes (including echo betel) are owned by generate-variant-recipes.py.
recipe('empty_pack', ['minecraft:paper','minecraft:paper','minecraft:string'], 'empty_pack', 2)
packs = {
 'kouweiwang_pack':['roasted_betel_nut','spicy_betel_nut','sweet_betel_nut','refreshing_betel_nut','night_betel_nut','energizing_betel_nut'],
 'hecheng_tianxia_pack':['roasted_betel_nut','honey_betel_nut','quartz_betel_nut','amethyst_betel_nut','lapis_betel_nut','phantom_betel_nut'],
 'xiangtan_puzi_pack':['roasted_betel_nut','honey_betel_nut','sweet_betel_nut','refreshing_betel_nut','night_betel_nut','spicy_betel_nut']}
for name, nuts in packs.items(): recipe(name, nuts+['empty_pack','brand_seal'], name)
for tag in ['betel_nuts', 'betel_foods']:
    path=data/f'tags/item/{tag}.json';obj=json.loads(path.read_text())
    for name in ['wrapped_raw_betel','echo_betel']:
        item=f'betel-nut-mod:{name}'
        if item not in obj['values']: obj['values'].append(item)
    save_json(path,obj)
save_json(data/'tags/item/liquid_foods.json',{'replace':False,'values':['minecraft:milk_bucket','minecraft:honey_bottle',
    'minecraft:mushroom_stew','minecraft:rabbit_stew','minecraft:beetroot_soup','minecraft:suspicious_stew']})

for name in names:
    save_json(assets / f'models/item/{name}.json', {'parent':'minecraft:item/generated', 'textures':{'layer0':f'betel-nut-mod:item/{name}'}})
print('Updated data and models; imagegen textures are preserved.')
import runpy
runpy.run_path(str(root / 'scripts/generate-variant-recipes.py'), run_name='__main__')
runpy.run_path(str(root / 'scripts/generate-recovery-assets.py'), run_name='__main__')
runpy.run_path(str(root / 'scripts/generate-fruit-harvest-assets.py'), run_name='__main__')
