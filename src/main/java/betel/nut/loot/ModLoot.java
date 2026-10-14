package betel.nut.loot;

import betel.nut.item.ModItems;
import net.fabricmc.fabric.api.loot.v3.LootTableEvents;
import net.minecraft.world.level.storage.loot.LootPool;
import net.minecraft.world.level.storage.loot.entries.LootItem;
import net.minecraft.world.level.storage.loot.functions.SetItemCountFunction;
import net.minecraft.world.level.storage.loot.predicates.LootItemRandomChanceCondition;
import net.minecraft.world.level.storage.loot.predicates.ExplosionCondition;
import net.minecraft.world.level.storage.loot.providers.number.ConstantValue;
import net.minecraft.world.level.storage.loot.providers.number.UniformGenerator;

public final class ModLoot {
    public static void register() {
        LootTableEvents.MODIFY.register((key, builder, source, registries) -> {
            if (!source.isBuiltin() || !key.location().getNamespace().equals("minecraft")) return;
            switch (key.location().getPath()) {
                case "blocks/dirt", "blocks/gravel" -> builder.withPool(LootPool.lootPool()
                        .setRolls(ConstantValue.exactly(1))
                        .when(ExplosionCondition.survivesExplosion())
                        .when(LootItemRandomChanceCondition.randomChance(0.10F))
                        .add(LootItem.lootTableItem(ModItems.SHELL)));
                case "chests/ancient_city" -> builder.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.15F))
                        .add(LootItem.lootTableItem(ModItems.ANCIENT_SHELL)
                                .apply(SetItemCountFunction.setCount(UniformGenerator.between(1, 2)))));
                case "gameplay/fishing/junk" -> builder.withPool(LootPool.lootPool().setRolls(ConstantValue.exactly(1))
                        .when(LootItemRandomChanceCondition.randomChance(0.35F))
                        .add(LootItem.lootTableItem(ModItems.SHELL)));
                default -> { }
            }
        });
    }
    private ModLoot() { }
}
