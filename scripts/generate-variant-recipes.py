"""Generate the 5.1 variant processing chain; ordinary betel recipes are never written."""
from collections import Counter
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parent.parent
DATA = ROOT / 'src/main/resources/data/betel-nut-mod'
MOD = 'betel-nut-mod:'
LEAF = MOD + 'betel_leaf'
ROASTED = MOD + 'roasted_betel_nut'


def nut(name):
    return MOD + name


def pair(base, *seasonings):
    return [base, base, LEAF, *['minecraft:' + item for item in seasonings]]


# A leaf binds a small batch. Every output nut consumes one ordinary/variant nut.
# Special reagents are fictional magical ingredients, matched to the existing effects.
SPECS = [
    ('sweet_betel_nut', 'sweet_betel_nut', '调味', pair(ROASTED, 'sugar'), 2, 100),
    ('honey_betel_nut', 'honey_betel_nut', '调味', pair(ROASTED, 'honey_bottle'), 2, 100),
    ('spicy_betel_nut', 'spicy_betel_nut', '特殊', pair(ROASTED, 'blaze_powder', 'sweet_berries'), 2, 160),
    ('refreshing_betel_nut_from_prismarine_shard', 'refreshing_betel_nut', '特殊', pair(ROASTED, 'prismarine_shard', 'kelp'), 2, 160),
    ('refreshing_betel_nut_from_prismarine_crystals', 'refreshing_betel_nut', '特殊', pair(ROASTED, 'prismarine_crystals', 'kelp'), 2, 160),
    ('night_betel_nut', 'night_betel_nut', '特殊', pair(ROASTED, 'golden_carrot'), 2, 160),
    ('energizing_betel_nut', 'energizing_betel_nut', '进阶', pair(nut('sweet_betel_nut'), 'redstone', 'cocoa_beans'), 2, 200),
    ('glow_betel_nut', 'glow_betel_nut', '特殊', pair(ROASTED, 'glow_berries', 'glowstone_dust'), 2, 160),
    ('phantom_betel_nut', 'phantom_betel_nut', '特殊', pair(ROASTED, 'phantom_membrane', 'sugar'), 2, 200),
    ('ender_betel_nut', 'ender_betel_nut', '特殊', pair(ROASTED, 'ender_pearl', 'chorus_fruit'), 2, 200),
    ('lapis_betel_nut', 'lapis_betel_nut', '特殊', pair(ROASTED, 'lapis_lazuli', 'rabbit_foot'), 2, 200),
    ('quartz_betel_nut', 'quartz_betel_nut', '特殊', pair(ROASTED, 'quartz', 'sugar'), 2, 160),
    ('magma_betel_nut', 'magma_betel_nut', '进阶', pair(nut('spicy_betel_nut'), 'magma_cream', 'blaze_powder'), 2, 240),
    ('amethyst_betel_nut', 'amethyst_betel_nut', '特殊', pair(ROASTED, 'amethyst_shard', 'honey_bottle'), 2, 200),
    ('echo_betel', 'echo_betel', '远古', [nut('wrapped_raw_betel'), nut('wrapped_raw_betel'), nut('ancient_shell'), 'minecraft:echo_shard'], 2, 240),
    ('synthetic_world_betel', 'synthetic_world_betel', '复合', [nut('energizing_betel_nut'), nut('quartz_betel_nut'), nut('amethyst_betel_nut'), LEAF, 'minecraft:golden_apple'], 3, 300),
    ('rich_world_betel', 'rich_world_betel', '复合', [nut('sweet_betel_nut'), nut('night_betel_nut'), nut('amethyst_betel_nut'), LEAF, 'minecraft:emerald', 'minecraft:glow_berries'], 3, 300),
    ('underground_betel', 'underground_betel', '复合', [nut('night_betel_nut'), nut('quartz_betel_nut'), nut('amethyst_betel_nut'), LEAF, 'minecraft:echo_shard'], 3, 300),
]


def condition(present):
    loaded = {'condition': 'fabric:all_mods_loaded', 'values': ['farmersdelight']}
    return [loaded if present else {'condition': 'fabric:not', 'value': loaded}]


def save(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')


def unlock(recipe_id, material, present):
    return {
        'fabric:load_conditions': condition(present),
        'parent': 'minecraft:recipes/root',
        'criteria': {
            'has_material': {'trigger': 'minecraft:inventory_changed', 'conditions': {'items': [{'items': material}]}},
            'has_the_recipe': {'trigger': 'minecraft:recipe_unlocked', 'conditions': {'recipe': MOD + recipe_id}},
        },
        'requirements': [['has_material', 'has_the_recipe']],
        'rewards': {'recipes': [MOD + recipe_id]},
    }


def cooking_id(name):
    # Retain the original shard recipe ID; add a distinct crystals alternative.
    suffix = 'refreshing_betel_nut' if name == 'refreshing_betel_nut_from_prismarine_shard' else name
    return 'compat/farmersdelight/cooking_' + suffix


def generate():
    for name, output, tier, inputs, count, ticks in SPECS:
        ingredients = [{'item': item} for item in inputs]
        result = {'id': MOD + output, 'count': count}
        save(DATA / f'recipe/{name}.json', {
            'fabric:load_conditions': condition(False),
            'type': 'minecraft:crafting_shapeless', 'category': 'misc',
            'ingredients': ingredients, 'result': result,
        })
        pot = cooking_id(name)
        save(DATA / f'recipe/{pot}.json', {
            'fabric:load_conditions': condition(True),
            'type': 'farmersdelight:cooking', 'recipe_book_tab': 'misc',
            'ingredients': ingredients, 'result': result,
            'cookingtime': ticks, 'experience': 0.1 if tier == '调味' else 0.35,
        })
        for recipe_id, present in [(name, False), (pot, True)]:
            save(DATA / f'advancement/recipes/{recipe_id}.json', unlock(recipe_id, inputs[0], present))

    names = json.loads((ROOT / 'src/main/resources/assets/betel-nut-mod/lang/zh_cn.json').read_text(encoding='utf-8'))
    vanilla = {
        'sugar': '糖', 'honey_bottle': '蜂蜜瓶', 'blaze_powder': '烈焰粉', 'sweet_berries': '甜浆果',
        'prismarine_shard': '海晶碎片', 'prismarine_crystals': '海晶砂粒', 'kelp': '海带',
        'golden_carrot': '金胡萝卜', 'redstone': '红石粉', 'cocoa_beans': '可可豆',
        'glow_berries': '发光浆果', 'glowstone_dust': '荧石粉', 'phantom_membrane': '幻翼膜',
        'ender_pearl': '末影珍珠', 'chorus_fruit': '紫颂果', 'lapis_lazuli': '青金石',
        'rabbit_foot': '兔子脚', 'quartz': '下界石英', 'magma_cream': '岩浆膏',
        'amethyst_shard': '紫水晶碎片', 'echo_shard': '回响碎片', 'golden_apple': '金苹果', 'emerald': '绿宝石',
    }

    def label(item):
        namespace, path = item.split(':')
        return names['item.' + namespace + '.' + path] if namespace == 'betel-nut-mod' else vanilla[path]

    lines = [
        '# 变种槟榔加工配方（5.1.0）', '',
        '普通路线保持原样：生槟榔营火烘烤 600 tick 得到 1 个熟槟榔；生槟榔＋槟榔叶＋贝壳粉得到 1 个裹叶生槟榔。贝壳制粉、木材、副产品和品牌包装配方也保持原样。', '',
        '未安装 Farmer\'s Delight Refabricated：下表配方均为工作台无序合成。安装后：相同原料和数量改用放在热源上的烹饪锅，工作台路线隐藏。锅内最多使用六个原料槽，重复的槟榔要分开放进两个槽位；不需要纸或碗作为输出容器。', '',
        '一颗成品消耗一颗槟榔原料。普通批次为两颗输入、两颗输出；复合批次为三颗输入、三颗输出。槟榔叶是小批次的裹叶材料，纸仅用于独立包装。蜂蜜瓶消耗后返还玻璃瓶，烹饪锅会将玻璃瓶弹出。', '',
        '提神从甜味继续加工，岩浆从火辣继续加工；三款复合槟榔整合已有口味能力。远古贝壳加工使用两颗裹叶生槟榔，保留普通生食路线的作用。所有矿物、下界与远古材料均为游戏中的魔法加工材料。', '',
        '| 成品 | 层次 | 每批原料 | 产量 | 锅内时间 |',
        '| --- | --- | --- | --- | --- |',
    ]
    for name, output, tier, inputs, count, ticks in SPECS:
        ingredients = '＋'.join((str(n) + '×' if n > 1 else '') + label(item) for item, n in Counter(inputs).items())
        lines.append(f'| {label(MOD + output)} | {tier} | {ingredients} | {count} | {ticks / 20:g} 秒 |')
    lines += ['', '物品 ID、增益、依赖与口腔暴露规则、树木采收和交易均保持原有行为。已有成品可以正常使用，旧存档重新加载后生效。配方书解锁同时覆盖工作台与烹饪锅，各环境只解锁当前可用路线。', '',
              '生成来源：`scripts/generate-variant-recipes.py`。重新生成命令：`py -3 -X utf8 scripts/generate-variant-recipes.py`。', '']
    (ROOT / 'docs').mkdir(exist_ok=True)
    (ROOT / 'docs/variant-processing-5.1.0.md').write_text('\n'.join(lines), encoding='utf-8')
    print('Generated 18 recipe pairs and their unlocks for 17 variants; ordinary recipes untouched.')


if __name__ == '__main__':
    generate()
