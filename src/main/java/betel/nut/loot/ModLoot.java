package betel.nut.loot;

import betel.nut.item.ModItems;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.LootTableLoadEvent;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class ModLoot {
    public static void register() {
        NeoForge.EVENT_BUS.addListener((LootTableLoadEvent event) -> {
            var key = event.getName(); var builder = event.getTable();
            if (!key.getNamespace().equals("minecraft")) return;
            switch (key.getPath()) {
                case "chests/ancient_city" -> builder.addPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.15F))
                        .add(LootItem.lootTableItem(ModItems.ANCIENT_SHELL)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))).build());
                case "gameplay/fishing/junk" -> builder.addPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.35F))
                        .add(LootItem.lootTableItem(ModItems.SHELL)).build());
                default -> { }
            }
        });
    }
    private ModLoot() { }
}
