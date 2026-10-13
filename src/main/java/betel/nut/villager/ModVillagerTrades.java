package betel.nut.villager;

import betel.nut.platform.BetelPlatform;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.ToIntFunction;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.block.ModBlocks;
import betel.nut.item.ModItems;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.level.ItemLike;

public final class ModVillagerTrades {
    private static final float PRICE_MULTIPLIER = 0.05F;
    private static boolean registered;

    /** Factories read the current server config only when an offer is generated. */
    public record Entry(String id, VillagerProfession profession, int level,
                        Function<BetelNutConfig, MerchantOffer> factory) implements VillagerTrades.ItemListing {
        public MerchantOffer createOffer(BetelNutConfig config) {
            return enabled(profession, config) ? factory.apply(config) : null;
        }

        @Override
        public MerchantOffer getOffer(Entity trader, RandomSource random) {
            return createOffer(BetelNutConfig.get());
        }
    }

    private static final List<Entry> CATALOG = List.of(
        // Farmers buy harvests, but do not sell the same raw materials back to players.
        buy("buy_raw", 1, ModItems.RAW_BETEL_NUT, c -> c.farmerTradeRawBetelSellCount, 16, 2),
        buy("buy_green_bunch", 1, ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM, c -> c.farmerTradeUnripeBunchSellCount, 12, 2),
        buy("buy_ripe_bunch", 1, ModBlocks.BETEL_NUT_CLUSTER_ITEM, c -> c.farmerTradeRipeBunchSellCount, 12, 2),
        buy("buy_leaves", 1, ModItems.BETEL_LEAF, c -> c.farmerTradeLeafSellCount, 16, 2),
        sell("roasted", VillagerProfession.FARMER, 2, ModItems.ROASTED_BETEL_NUT, c -> 2,
                c -> c.farmerTradeRoastedBetelBuyCount, 12, 5),
        sell("sapling", VillagerProfession.FARMER, 2, ModBlocks.BETEL_PALM_SAPLING_ITEM,
                c -> c.farmerTradeSaplingEmeraldCost, c -> 1, 8, 5),
        sell("empty_pack", VillagerProfession.FARMER, 2, ModItems.EMPTY_PACK, c -> 1,
                c -> c.farmerTradeEmptyPackBuyCount, 12, 5),
        sell("brand_seal", VillagerProfession.FARMER, 2, ModItems.BRAND_SEAL,
                c -> c.farmerTradeBrandSealEmeraldCost, c -> 1, 8, 5),
        flavor("sweet", ModItems.SWEET_BETEL_NUT, 3, false),
        flavor("spicy", ModItems.SPICY_BETEL_NUT, 3, false),
        flavor("honey", ModItems.HONEY_BETEL_NUT, 3, false),
        flavor("refreshing", ModItems.REFRESHING_BETEL_NUT, 3, false),
        sell("kouweiwang_pack", VillagerProfession.FARMER, 3, ModItems.KOUWEIWANG_PACK,
                c -> c.farmerTradeKouweiwangPackEmeraldCost, c -> 1, 4, 10),
        flavor("night", ModItems.NIGHT_BETEL_NUT, 4, true),
        flavor("energizing", ModItems.ENERGIZING_BETEL_NUT, 4, true),
        sell("xiangtan_pack", VillagerProfession.FARMER, 4, ModItems.XIANGTAN_PUZI_PACK,
                c -> c.farmerTradeXiangtanPackEmeraldCost, c -> 1, 4, 15),
        sell("hecheng_pack", VillagerProfession.FARMER, 5, ModItems.HECHENG_TIANXIA_PACK,
                c -> c.farmerTradeHechengPackEmeraldCost, c -> 1, 3, 30),
        exchange("synthetic_world", VillagerProfession.FARMER, 5, ModItems.SYNTHETIC_WORLD_BETEL,
                c -> c.farmerTradeSyntheticWorldEmeraldCost, ModItems.ROASTED_BETEL_NUT, 3, 30),

        sell("shell", VillagerProfession.FISHERMAN, 1, ModItems.SHELL, c -> 1,
                c -> c.fishermanTradeShellBuyCount, 16, 2),
        sell("shell_powder", VillagerProfession.FISHERMAN, 2, ModItems.SHELL_POWDER, c -> 1,
                c -> c.fishermanTradeShellPowderBuyCount, 12, 5),

        exchange("relief", VillagerProfession.CLERIC, 1, ModItems.RELIEF_POTION,
                c -> c.clericTradeReliefEmeraldCost, Items.GLASS_BOTTLE, 8, 2),
        exchange("golden_paste_service", VillagerProfession.CLERIC, 2, ModItems.GOLDEN_APPLE_PASTE,
                c -> c.clericTradeGoldenPasteServiceEmeraldCost, Items.GOLDEN_APPLE, 6, 5),
        exchange("golden_paste", VillagerProfession.CLERIC, 3, ModItems.GOLDEN_APPLE_PASTE,
                c -> c.clericTradeGoldenPasteEmeraldCost, Items.GLASS_BOTTLE, 4, 10),
        exchange("enchanted_paste_service", VillagerProfession.CLERIC, 5, ModItems.ENCHANTED_GOLDEN_APPLE_PASTE,
                c -> c.clericTradeEnchantedPasteServiceEmeraldCost, Items.ENCHANTED_GOLDEN_APPLE, 2, 30)
    );

    public static List<Entry> catalog() {
        return CATALOG;
    }

    public static boolean enabled(VillagerProfession profession, BetelNutConfig config) {
        // Keep the old switch as the master switch, including its previous effect on fishermen.
        return config.enableFarmerTrades && (profession == VillagerProfession.FARMER
                || profession == VillagerProfession.FISHERMAN && config.enableFishermanTrades
                || profession == VillagerProfession.CLERIC && config.enableClericTrades);
    }

    public static void register() {
        if (registered) return;
        registered = true;
        BetelPlatform.onServerStarting(server -> refresh());
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.addListener((net.neoforged.neoforge.event.village.VillagerTradesEvent event) -> {
            for (Entry entry : CATALOG) if (entry.profession() == event.getType() && enabled(entry.profession(), BetelNutConfig.get()))
                event.getTrades().get(entry.level()).add(entry);
        });
    }

    /** Rebuild only our candidates; existing villagers' saved offers remain untouched. */
    public static synchronized void refresh() {
        for (var pools : List.of(VillagerTrades.TRADES, VillagerTrades.EXPERIMENTAL_TRADES)) {
            for (var levels : pools.values()) {
                levels.replaceAll((level, entries) -> Arrays.stream(entries)
                        .filter(entry -> !(entry instanceof Entry)).toArray(VillagerTrades.ItemListing[]::new));
            }
        }
        for (Entry entry : CATALOG) {
            if (!enabled(entry.profession(), BetelNutConfig.get())) continue;
            var normal = VillagerTrades.TRADES.get(entry.profession());
            var experimental = VillagerTrades.EXPERIMENTAL_TRADES.get(entry.profession());
            append(normal, entry);
            if (experimental != normal) append(experimental, entry);
        }
        BetelNutMod.LOGGER.info("Refreshed betel trade pools: master={}, fisherman={}, cleric={}",
                BetelNutConfig.get().enableFarmerTrades, BetelNutConfig.get().enableFishermanTrades,
                BetelNutConfig.get().enableClericTrades);
    }

    /** The command and trade factories use the same catalog and current configuration. */
    private static void append(it.unimi.dsi.fastutil.ints.Int2ObjectMap<VillagerTrades.ItemListing[]> levels, Entry entry) {
        if (levels == null) return;
        var old = levels.get(entry.level());
        if (old == null) old = new VillagerTrades.ItemListing[0];
        var next = Arrays.copyOf(old, old.length + 1); next[old.length] = entry; levels.put(entry.level(), next);
    }
    public static Component describe(BetelNutConfig config) {
        var text = Component.translatable("command.betel_nut_mod.trades.info", status(config.enableFarmerTrades));
        for (var profession : List.of(VillagerProfession.FARMER, VillagerProfession.FISHERMAN, VillagerProfession.CLERIC)) {
            var name = Component.translatable("entity.minecraft.villager."
                    + BuiltInRegistries.VILLAGER_PROFESSION.getKey(profession).getPath());
            text.append("\n").append(Component.translatable("command.betel_nut_mod.trades.profession",
                    name, status(enabled(profession, config))));
            for (Entry entry : CATALOG) {
                if (entry.profession() != profession) continue;
                var offer = entry.createOffer(config);
                if (offer == null) continue;
                var a = offer.getBaseCostA();
                var b = offer.getCostB();
                var result = offer.getResult();
                Component extra = b.isEmpty() ? Component.empty() : Component.translatable(
                        "command.betel_nut_mod.trades.extra", b.getHoverName(), b.getCount());
                text.append("\n").append(Component.translatable("command.betel_nut_mod.trades.entry",
                        entry.level(), a.getHoverName(), a.getCount(), extra,
                        result.getHoverName(), result.getCount(), offer.getMaxUses()));
            }
        }
        return text.append("\n").append(Component.translatable("command.betel_nut_mod.trades.saved_note"));
    }

    private static Component status(boolean enabled) {
        return Component.translatable("command.betel_nut_mod.trades." + (enabled ? "enabled" : "disabled"));
    }

    private static Entry buy(String id, int level, ItemLike item, ToIntFunction<BetelNutConfig> count,
                             int maxUses, int experience) {
        return new Entry(id, VillagerProfession.FARMER, level, c -> new MerchantOffer(
                new ItemCost(item, count.applyAsInt(c)), new ItemStack(Items.EMERALD), maxUses, experience, PRICE_MULTIPLIER));
    }

    private static Entry sell(String id, VillagerProfession profession, int level, ItemLike item,
                              ToIntFunction<BetelNutConfig> cost, ToIntFunction<BetelNutConfig> count,
                              int maxUses, int experience) {
        return new Entry(id, profession, level, c -> new MerchantOffer(
                new ItemCost(Items.EMERALD, cost.applyAsInt(c)), new ItemStack(item, count.applyAsInt(c)),
                maxUses, experience, PRICE_MULTIPLIER));
    }

    private static Entry exchange(String id, VillagerProfession profession, int level, ItemLike result,
                                  ToIntFunction<BetelNutConfig> cost, ItemLike material, int maxUses, int experience) {
        return new Entry(id, profession, level, c -> new MerchantOffer(new ItemCost(Items.EMERALD, cost.applyAsInt(c)),
                Optional.of(new ItemCost(material)), new ItemStack(result), maxUses, experience, PRICE_MULTIPLIER));
    }

    private static Entry flavor(String id, ItemLike item, int level, boolean advanced) {
        return sell(id, VillagerProfession.FARMER, level, item,
                c -> Math.min(64, c.farmerTradeFlavorEmeraldCost + (advanced ? 1 : 0)),
                c -> 2, advanced ? 6 : 8, advanced ? 15 : 10);
    }

    private ModVillagerTrades() { }
}
