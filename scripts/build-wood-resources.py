"""Build the palm wood family data using Minecraft 1.21.1 door templates."""
import json
from pathlib import Path
from zipfile import ZipFile

ROOT = Path(__file__).resolve().parent.parent
RES = ROOT / "src/main/resources"
ASSETS = RES / "assets/betel-nut-mod"
DATA = RES / "data/betel-nut-mod"
MOD = "betel-nut-mod:"


def write(path, data):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


with ZipFile(Path.home() / ".gradle/caches/fabric-loom/1.21.1/minecraft-client.jar") as vanilla:
    for name in ["blockstates/oak_door.json"] + [
        f"models/block/oak_door_{half}_{hinge}{opened}.json"
        for half in ["bottom", "top"] for hinge in ["left", "right"] for opened in ["", "_open"]
    ]:
        source = vanilla.read("assets/minecraft/" + name).decode()
        # Keep vanilla template parents and rotations; replace only the material references.
        source = source.replace("minecraft:block/oak_door", MOD + "block/betel_palm_door")
        write(ASSETS / name.replace("oak_door", "betel_palm_door"), json.loads(source))
    loot = vanilla.read("data/minecraft/loot_table/blocks/oak_door.json").decode()
    loot = loot.replace("minecraft:oak_door", MOD + "betel_palm_door")
    loot = loot.replace("minecraft:blocks/oak_door", MOD + "blocks/betel_palm_door")
    write(DATA / "loot_table/blocks/betel_palm_door.json", json.loads(loot))

write(ASSETS / "models/item/betel_palm_door.json", {
    "parent": "minecraft:item/generated",
    "textures": {"layer0": MOD + "item/betel_palm_door"},
})
write(DATA / "recipe/betel_palm_door.json", {
    "type": "minecraft:crafting_shaped", "category": "redstone", "group": "wooden_door",
    "key": {"#": {"item": MOD + "betel_palm_planks"}}, "pattern": ["##", "##", "##"],
    "result": {"count": 3, "id": MOD + "betel_palm_door"},
})

# Discover the wood family from its actual recipes to avoid missing recipe-book entries.
for recipe_file in sorted((DATA / "recipe").glob("betel_palm_*.json")):
    recipe = json.loads(recipe_file.read_text(encoding="utf-8"))
    if recipe_file.stem == "betel_palm_sapling":
        continue
    ingredient = "betel_palm_log" if recipe_file.stem == "betel_palm_planks" else "betel_palm_planks"
    recipe_id = MOD + recipe_file.stem
    write(DATA / f"advancement/recipes/{recipe['category']}/{recipe_file.stem}.json", {
        "parent": "minecraft:recipes/root",
        "criteria": {
            "has_material": {"trigger": "minecraft:inventory_changed", "conditions": {
                "items": [{"items": MOD + ingredient}]}},
            "has_the_recipe": {"trigger": "minecraft:recipe_unlocked", "conditions": {"recipe": recipe_id}},
        },
        "requirements": [["has_material", "has_the_recipe"]],
        "rewards": {"recipes": [recipe_id]},
    })

for kind in ["block", "item"]:
    for tag, block in [("wooden_doors", "betel_palm_door"), ("wooden_trapdoors", "betel_palm_trapdoor")]:
        write(RES / f"data/minecraft/tags/{kind}/{tag}.json", {"replace": False, "values": [MOD + block]})
axe_file = RES / "data/minecraft/tags/block/mineable/axe.json"
axe = json.loads(axe_file.read_text(encoding="utf-8"))
for block in ["betel_palm_door", "betel_palm_trapdoor"]:
    if MOD + block not in axe["values"]:
        axe["values"].append(MOD + block)
write(axe_file, axe)

for language, label in [("zh_cn", "槟榔木门"), ("en_us", "Betel Palm Door")]:
    file = ASSETS / f"lang/{language}.json"
    translations = json.loads(file.read_text(encoding="utf-8"))
    translations["block.betel-nut-mod.betel_palm_door"] = label
    write(file, translations)

print("Built door models, loot, recipe, wood recipe unlocks, tags and translations.")
