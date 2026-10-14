"""Generate the Create-only production routes without changing manual or FD recipes."""
from pathlib import Path
import json

ROOT = Path(__file__).resolve().parents[1]
RECIPES = ROOT / 'src/main/resources/data/betel_nut_mod/recipe'
OUTPUT = RECIPES / 'compat/create'
MOD = 'betel_nut_mod:'
CONDITIONS = [{'type': 'neoforge:mod_loaded', 'modid': 'create'}]
generated = {}


def item(name):
    return {'item': name if ':' in name else MOD + name}


def fluid(name, amount):
    return {'type': 'neoforge:tag', 'tag': 'c:honey', 'amount': amount} if name == 'honey' else {
        'type': 'neoforge:single', 'fluid': name, 'amount': amount}


def output(name, count=1):
    return {'id': name if ':' in name else MOD + name, 'count': count}


def add(name, data):
    generated[name] = {**data, 'neoforge:conditions': CONDITIONS}


def process(name, kind, ingredients, result, count=1, heat=None, time=None):
    recipe = {'type': 'create:' + kind, 'ingredients': ingredients, 'results': [output(result, count)]}
    if heat:
        recipe['heat_requirement'] = heat
    if time is not None:
        recipe['processing_time'] = time
    add(name, recipe)


def mix(name, base, extras, *, heat=None, water=0, count=2):
    ingredients = [item(base) for _ in range(count)] + [item(e) for e in extras]
    if water:
        ingredients.append(fluid('minecraft:water', water))
    process('mixing_' + name, 'mixing', ingredients, name.split('_from_')[0], count, heat)


def mechanical(name, pattern, keys, result=None, count=1):
    add('mechanical_' + name, {
        'type': 'create:mechanical_crafting', 'category': 'misc', 'accept_mirrored': False,
        'pattern': pattern, 'key': {k: item(v) for k, v in keys.items()},
        'result': output(result or name, count), 'show_notification': False})


for name, material, result, count, time in [
    ('cutting_unripe_cluster', 'unripe_betel_nut_cluster', 'raw_betel_nut', 3, 80),
    ('cutting_ripe_cluster', 'betel_nut_cluster', 'raw_betel_nut', 5, 80),
    ('cutting_palm_leaves', 'betel_palm_leaves', 'betel_leaf', 2, 40),
    ('cutting_palm_log', 'betel_palm_log', 'betel_palm_planks', 6, 100),
]:
    process(name, 'cutting', [item(material)], result, count, time=time)
process('milling_shell', 'milling', [item('shell')], 'shell_powder', 4, time=100)
process('compacting_residue', 'compacting', [item('betel_nut_residue')] * 9, 'betel_nut_residue_block')
add('smoking_raw_betel', {'type': 'minecraft:smoking', 'ingredient': item('raw_betel_nut'),
                        'result': output('roasted_betel_nut'), 'experience': 0.35, 'cookingtime': 600})

# A normal raw nut carries Create's progress component between two deployers.
# Neither a new item ID nor a newly registered intermediate is needed.
add('assembling_wrapped_raw_betel', {
    'type': 'create:sequenced_assembly', 'ingredient': item('raw_betel_nut'), 'loops': 1,
    'transitional_item': output('raw_betel_nut'), 'results': [output('wrapped_raw_betel')],
    'sequence': [{'type': 'create:deploying', 'ingredients': [item('raw_betel_nut'), item(material)],
                  'results': [output('raw_betel_nut')]} for material in ('shell_powder', 'betel_leaf')]})

# New leaves enter only with plain roasted nuts. Flavored inputs already carry their leaf cost.
mix('sweet_betel_nut', 'roasted_betel_nut', ['betel_leaf', 'minecraft:sugar'], heat='heated', water=250)
mix('spicy_betel_nut', 'roasted_betel_nut', ['betel_leaf', 'minecraft:blaze_powder', 'minecraft:sweet_berries'], heat='heated', water=250)
for mineral in ('prismarine_shard', 'prismarine_crystals'):
    mix('refreshing_betel_nut_from_' + mineral, 'roasted_betel_nut',
        ['betel_leaf', 'minecraft:' + mineral, 'minecraft:kelp'], water=250)
process('filling_honey_betel_nut', 'filling', [item('sweet_betel_nut'), fluid('honey', 125)], 'honey_betel_nut')
mix('energizing_betel_nut', 'sweet_betel_nut', ['minecraft:redstone', 'minecraft:cocoa_beans'], heat='heated', water=250)
mix('night_betel_nut', 'refreshing_betel_nut', ['minecraft:golden_carrot'], heat='heated', water=250)
mix('glow_betel_nut', 'night_betel_nut', ['minecraft:glow_berries', 'minecraft:glowstone_dust'])
mix('phantom_betel_nut', 'night_betel_nut', ['minecraft:phantom_membrane', 'minecraft:sugar'])
mix('quartz_betel_nut', 'sweet_betel_nut', ['minecraft:quartz'], heat='heated', water=250)
mix('lapis_betel_nut', 'sweet_betel_nut', ['minecraft:lapis_lazuli', 'minecraft:rabbit_foot'], heat='heated', water=250)
mix('amethyst_betel_nut', 'honey_betel_nut', ['minecraft:amethyst_shard'], heat='heated', water=250)
mix('magma_betel_nut', 'spicy_betel_nut', ['minecraft:magma_cream', 'minecraft:blaze_powder'], heat='heated')

mechanical('ender_betel_nut', ['NPN', ' C '], {'N': 'night_betel_nut', 'P': 'minecraft:ender_pearl', 'C': 'minecraft:chorus_fruit'}, count=2)
mechanical('echo_betel', ['WSW', ' E '], {'W': 'wrapped_raw_betel', 'S': 'ancient_shell', 'E': 'minecraft:echo_shard'}, count=2)
mechanical('synthetic_world_betel', ['ABC', ' D '], {'A': 'energizing_betel_nut', 'B': 'quartz_betel_nut', 'C': 'amethyst_betel_nut', 'D': 'minecraft:golden_apple'}, count=3)
mechanical('rich_world_betel', ['ABC', 'D E'], {'A': 'sweet_betel_nut', 'B': 'night_betel_nut', 'C': 'amethyst_betel_nut', 'D': 'minecraft:emerald', 'E': 'minecraft:glow_berries'}, count=3)
mechanical('underground_betel', ['ABC', ' D '], {'A': 'night_betel_nut', 'B': 'quartz_betel_nut', 'C': 'amethyst_betel_nut', 'D': 'minecraft:echo_shard'}, count=3)

mechanical('empty_pack', ['PP', 'S '], {'P': 'minecraft:paper', 'S': 'minecraft:string'}, count=2)
for name in ('kouweiwang_pack', 'hecheng_tianxia_pack', 'xiangtan_puzi_pack'):
    manual = json.loads((RECIPES / (name + '.json')).read_text(encoding='utf-8'))
    keys = dict(zip('ABCDEFPS', (entry['item'] for entry in manual['ingredients'])))
    mechanical(name, ['ABC', 'DEF', ' PS'], keys)

for name, materials in [
    ('relief_potion', ['minecraft:dandelion', 'minecraft:sugar']),
    ('golden_apple_paste', ['minecraft:golden_apple']),
    ('enchanted_golden_apple_paste', ['minecraft:enchanted_golden_apple']),
]:
    process('mixing_' + name, 'mixing', [*[item(m) for m in materials], item('minecraft:glass_bottle'), fluid('honey', 250)], name, heat='heated')

add('washing_dirt', {'type': 'create:splashing', 'ingredients': [item('minecraft:dirt')],
    'results': [{'id': MOD + 'shell', 'count': 1, 'chance': 0.25}]})
gravel = {'type': 'create:splashing', 'ingredients': [item('minecraft:gravel')],
    'results': [{'id': 'minecraft:flint', 'chance': 0.25}, {'id': 'minecraft:iron_nugget', 'chance': 0.125},
                {'id': MOD + 'shell', 'count': 1, 'chance': 0.25}], 'neoforge:conditions': CONDITIONS}
gravel_path = ROOT / 'src/main/resources/data/create/recipe/splashing/gravel.json'
gravel_path.parent.mkdir(parents=True, exist_ok=True)
gravel_path.write_text(json.dumps(gravel, indent=2) + '\n', encoding='utf-8')
assert len(generated) == 34
OUTPUT.mkdir(parents=True, exist_ok=True)
for name, data in sorted(generated.items()):
    (OUTPUT / (name + '.json')).write_text(json.dumps(data, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')
for old in OUTPUT.glob('*.json'):
    if old.stem not in generated:
        old.unlink()

# Keep original recipe IDs and manual/FD behavior. Only Create's automatic conversion is filtered.
protected = set()
for data in generated.values():
    results = data.get('results', [data.get('result')])
    for result in results:
        if result and result['id'].startswith(MOD):
            path = result['id'][len(MOD):]
            if (RECIPES / (path + '.json')).exists() and path not in {
                'raw_betel_nut', 'betel_leaf', 'betel_palm_planks', 'shell_powder', 'betel_nut_residue_block', 'roasted_betel_nut'}:
                protected.add(path)
protected.discard('refreshing_betel_nut')
protected.update(('refreshing_betel_nut_from_prismarine_shard', 'refreshing_betel_nut_from_prismarine_crystals'))
source = '''package betel.nut.compat;

import betel.nut.BetelNutMod;
import java.util.Set;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;

/** Prevents Create's generic crafting adapters from bypassing the explicit production routes. */
public final class CreateRecipePolicy {
    private static final Set<String> MANUAL_RECIPES = Set.of(
%s);

    public static boolean requiresDedicatedRoute(RecipeHolder<?> recipe) {
        return recipe.value().getType() == RecipeType.CRAFTING
                && recipe.id().getNamespace().equals(BetelNutMod.MOD_ID)
                && MANUAL_RECIPES.contains(recipe.id().getPath());
    }
    private CreateRecipePolicy() {}
}
''' % ',\n'.join('            "' + name + '"' for name in sorted(protected))
java = ROOT / 'src/main/java/betel/nut/compat/CreateRecipePolicy.java'
java.parent.mkdir(parents=True, exist_ok=True)
java.write_text(source, encoding='utf-8')
print(f'Generated {len(generated)} Create routes; preserved and protected {len(protected)} manual recipe IDs')
