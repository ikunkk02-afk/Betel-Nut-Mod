"""Maintain the visible progression tree and its English/Chinese descriptions."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DATA = ROOT / "src/main/resources/data/betel-nut-mod/advancement"
LANG = ROOT / "src/main/resources/assets/betel-nut-mod/lang"
MOD = "betel-nut-mod:"
translations = {language: json.loads((LANG / f"{language}.json").read_text(encoding="utf-8"))
                for language in ("zh_cn", "en_us")}


def item(name):
    return name if ":" in name else MOD + name


def inventory(*names):
    return {"obtained": {"trigger": "minecraft:inventory_changed",
                         "conditions": {"items": [{"items": [item(n) for n in names]}]}}}


def collection(*names):
    return {name: {"trigger": "minecraft:inventory_changed",
                   "conditions": {"items": [{"items": item(name)}]}} for name in names}


def event(name="triggered"):
    return {name: {"trigger": "minecraft:impossible"}}


def write(path, parent, icon, zh, en, criteria, *, frame="task", xp=0, root=False, hidden=False):
    key = "advancement.betel-nut-mod." + path.replace("/", ".")
    for language, pair in (("zh_cn", zh), ("en_us", en)):
        translations[language][key + ".title"] = pair[0]
        translations[language][key + ".description"] = pair[1]
    display = {"icon": {"count": 1, "id": item(icon)}, "title": {"translate": key + ".title"},
               "description": {"translate": key + ".description"}, "frame": frame,
               "show_toast": True, "announce_to_chat": frame == "challenge", "hidden": hidden}
    if root:
        display["background"] = "minecraft:textures/gui/advancements/backgrounds/husbandry.png"
    data = {"display": display, "criteria": criteria, "requirements": [[name] for name in criteria]}
    if parent:
        data["parent"] = MOD + parent
    if xp:
        data["rewards"] = {"experience": xp}
    target = DATA / f"{path}.json"
    target.parent.mkdir(parents=True, exist_ok=True)
    target.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")


write("root", None, "raw_betel_nut", ("槟榔工艺", "获得生槟榔、果串、槟榔叶、原木或树苗，开始种植与加工。"),
      ("Betel Craft", "Obtain a raw nut, bunch, leaf, palm log or sapling to begin farming and processing."),
      {"has_raw_betel_nut": inventory("raw_betel_nut", "betel_nut_cluster", "unripe_betel_nut_cluster",
                                    "betel_leaf", "betel_palm_log", "betel_palm_sapling")["obtained"]}, root=True)
write("cultivation/harvest", "root", "unripe_betel_nut_cluster", ("整串采收", "用剪刀右键采下一串青果或黄果。"),
      ("A Whole Bunch", "Right-click a green or ripe bunch with shears to harvest it."), event())
write("cultivation/ripe_harvest", "cultivation/harvest", "betel_nut_cluster", ("等到金黄", "用剪刀采下一串成熟黄果；每串可拆为五颗生槟榔。"),
      ("Worth the Wait", "Shear a ripe yellow bunch; it yields five raw nuts when processed."), event(), frame="goal", xp=10)
write("cultivation/sapling", "cultivation/ripe_harvest", "betel_palm_sapling", ("留作种子", "获得槟榔树苗：砍伐绿色冠轴，或用黄果串与槟榔叶育苗。"),
      ("Save Some for Planting", "Obtain a palm sapling by felling a green crownshaft or combining a ripe bunch with a betel leaf."), inventory("betel_palm_sapling"))
write("cultivation/plant", "cultivation/sapling", "betel_palm_sapling", ("种下新一轮", "亲手将槟榔树苗种在泥土上，开始下一轮收成。"),
      ("The Next Harvest", "Plant a betel palm sapling on soil to begin another harvest cycle."),
      {"planted": {"trigger": "minecraft:placed_block", "conditions": {"location": [
          {"condition": "minecraft:block_state_property", "block": item("betel_palm_sapling")}]}}}, xp=10)
write("cultivation/compost", "root", "minecraft:composter", ("回到泥土", "从成熟的堆肥桶中亲手取出骨粉；槟榔渣也能堆肥。"),
      ("Back to the Soil", "Collect bone meal from a ready composter by hand; betel residue is compostable too."), event(), xp=10)
write("cultivation/wood", "root", "betel_palm_planks", ("物尽其用", "获得槟榔木板，用于建筑、木门和红石部件。"),
      ("Use Every Part", "Obtain palm planks for buildings, doors and redstone components."), inventory("betel_palm_planks"))
write("processing/roast", "root", "roasted_betel_nut", ("营火飘香", "获得熟槟榔：生槟榔可以在营火上烘烤。"),
      ("By the Campfire", "Obtain a roasted betel nut; raw nuts can be roasted on a campfire."), inventory("roasted_betel_nut"))
write("processing/wrap", "processing/roast", "wrapped_raw_betel", ("一叶一果", "获得裹叶生槟榔；需要生槟榔、槟榔叶与贝壳粉。"),
      ("Wrapped in a Leaf", "Obtain wrapped raw betel, made with a raw nut, betel leaf and shell powder."), inventory("wrapped_raw_betel"))
write("processing/basic_flavors", "processing/roast", "honey_betel_nut", ("调味工作室", "分别获得辛辣、甜味与蜂蜜槟榔，无需食用。"),
      ("Flavor Workshop", "Obtain spicy, sweet and honey betel nuts over time; eating them is not required."),
      collection("spicy_betel_nut", "sweet_betel_nut", "honey_betel_nut"), frame="goal", xp=15)
write("processing/mineral_flavors", "processing/basic_flavors", "amethyst_betel_nut", ("矿物调色盘", "分别获得石英、青金石与紫水晶槟榔。"),
      ("A Mineral Palette", "Obtain quartz, lapis and amethyst betel nuts over time."),
      collection("quartz_betel_nut", "lapis_betel_nut", "amethyst_betel_nut"), frame="goal", xp=20)
flavors = ["spicy", "sweet", "refreshing", "night", "energizing", "honey", "glow", "phantom", "ender", "lapis", "quartz", "magma", "amethyst"]
write("processing/all_flavors", "processing/mineral_flavors", "synthetic_world_betel", ("风味收藏家", "分别收集全部十七种变种槟榔，可分次完成，无需食用。"),
      ("Flavor Collector", "Collect all seventeen betel variants over time; eating them is not required."),
      collection(*(f"{name}_betel_nut" for name in flavors), "synthetic_world_betel", "rich_world_betel", "underground_betel", "echo_betel"),
      frame="challenge", xp=100)
# Preserve both IDs and criterion names so existing completions and rewards remain valid.
for name in ("synthetic_world", "rich_world_betel", "underground_betel"):
    path = DATA / f"{name}.json"
    data = json.loads(path.read_text(encoding="utf-8"))
    data["parent"] = MOD + "processing/mineral_flavors"
    data["display"]["hidden"] = False
    path.write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
write("collection/shell", "processing/wrap", "shell", ("河岸来客", "挖掘泥土或沙砾有10%概率获得贝壳，也可向渔夫购买或钓鱼获得。"),
      ("From the Water", "Dirt and gravel drop shells at 10%; fishermen and fishing are also sources."), inventory("shell"))
write("collection/ancient_shell", "collection/shell", "ancient_shell", ("古城遗物", "获得远古贝壳；探索尚未开启的古城宝箱。"),
      ("An Ancient Relic", "Obtain an ancient shell; search unopened ancient city chests."), inventory("ancient_shell"), frame="goal", xp=25)
write("collection/echo", "collection/ancient_shell", "echo_betel", ("回声留存", "获得回响槟榔；以远古贝壳、回响碎片与裹叶生槟榔加工。"),
      ("An Echo Preserved", "Obtain echo betel, processed with an ancient shell, echo shard and wrapped raw betel."), inventory("echo_betel"), frame="goal", xp=25)
write("trade/seal", "root", "brand_seal", ("与农民合作", "完成一次购买品牌封签的村民交易；二级农民可能出售。"),
      ("Working with Farmers", "Buy a brand seal in a villager trade; apprentice farmers may offer one."),
      {"traded": {"trigger": "minecraft:villager_trade", "conditions": {"item": {"items": item("brand_seal")}}}}, xp=10)
write("trade/open_pack", "trade/seal", "kouweiwang_pack", ("拆包时刻", "实际打开任意混合装，获得六种槟榔与一张奖券。"),
      ("Unpack an Assortment", "Open any assortment to receive six betel nuts and a lottery ticket."), event())
write("trade/three_packs", "trade/open_pack", "xiangtan_puzi_pack", ("三款齐备", "分别获得口味王、和成天下与湘潭铺子混合装。"),
      ("All Three Assortments", "Obtain Kouweiwang, Hecheng Tianxia and Xiangtan Puzi packs over time."),
      collection("kouweiwang_pack", "hecheng_tianxia_pack", "xiangtan_puzi_pack"), frame="goal", xp=20)
write("recovery/supplies", "root", "relief_potion", ("备好退路", "获得缓解药剂、金苹果糨糊或附魔金苹果糨糊。"),
      ("Keep a Way Back", "Obtain relief potion, golden apple paste or enchanted golden apple paste."),
      inventory("relief_potion", "golden_apple_paste", "enchanted_golden_apple_paste"))
write("recovery/relief", "recovery/supplies", "golden_apple_paste", ("缓解有方", "使用缓解药剂或金苹果类恢复用品，实际降低依赖、戒断或口腔暴露。"),
      ("A Step Toward Recovery", "Use relief potion or golden apple recovery to reduce dependence, withdrawal or oral exposure."), event(), xp=10)
write("recovery/recovered", "recovery/relief", "enchanted_golden_apple_paste", ("重新出发", "使用恢复用品，将已有的依赖、戒断与口腔暴露全部清零。"),
      ("A Fresh Start", "Use recovery supplies to clear all existing dependence, withdrawal and oral exposure."), event(), frame="goal", xp=25)
write("skyblock/enter_betel_skyblock", None, "betel_nut_residue_block", ("槟榔空岛", "进入经典或一方块槟榔空岛，建立可持续的生存循环。"),
      ("Betel Skyblock", "Enter classic or one-block Betel Skyblock and build a sustainable survival loop."), event(), root=True)
write("skyblock/first_compost", "skyblock/enter_betel_skyblock", "minecraft:bone_meal", ("空岛的第一份堆肥", "在已启用的槟榔空岛里，从成熟堆肥桶亲手取出骨粉。"),
      ("Skyblock's First Compost", "Collect bone meal by hand from a ready composter in enabled Betel Skyblock."), event("has_bone_meal"))
write("skyblock/end_frame_ritual", "skyblock/first_compost", "minecraft:end_portal_frame", ("水中召雷", "在槟榔空岛完成末地框架仪式；也可以走村民和末影之眼路线。"),
      ("Lightning over Water", "Complete the skyblock end-frame ritual; villagers and eyes of ender offer another route."), event(), frame="goal", xp=25)
write("skyblock/enter_end", "skyblock/first_compost", "minecraft:ender_eye", ("越过虚空", "从已启用的槟榔空岛出发进入末地，可使用任意有效路线。"),
      ("Beyond the Void", "Reach the End from enabled Betel Skyblock using any valid route."), event("entered_the_end"), frame="challenge", xp=50)
for language, data in translations.items():
    (LANG / f"{language}.json").write_text(json.dumps(data, ensure_ascii=False, indent=2) + "\n", encoding="utf-8")
print("Updated 28 visible advancements in two tabs; preserved legacy helper IDs and criteria.")
