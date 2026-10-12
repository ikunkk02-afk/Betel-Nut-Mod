package betel.nut.test;

import betel.nut.BetelNutConfig;
import betel.nut.block.ModBlocks;
import betel.nut.item.ModItems;
import betel.nut.villager.ModVillagerTrades;
import com.google.gson.Gson;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerTrades;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.ItemCost;
import net.minecraft.world.item.trading.MerchantOffer;
import net.minecraft.world.item.trading.MerchantOffers;

public final class BetelTradeGameTests implements FabricGameTest {
    private static ModVillagerTrades.Entry entry(String id) {
        return ModVillagerTrades.catalog().stream().filter(e -> e.id().equals(id)).findFirst().orElseThrow();
    }

    private static MerchantOffer offer(String id) {
        return entry(id).createOffer(new BetelNutConfig());
    }

    private static Villager villager(GameTestHelper h, VillagerProfession profession, int level) {
        var v = new Villager(EntityType.VILLAGER, h.getLevel());
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, profession, level));
        v.setNoAi(true);
        return v;
    }

    private static List<VillagerTrades.ItemListing> originals(VillagerProfession profession, int level) {
        return Arrays.stream(VillagerTrades.TRADES.get(profession).get(level))
                .filter(e -> !(e instanceof ModVillagerTrades.Entry)).toList();
    }

    private static int inventory(net.minecraft.server.level.ServerPlayer player, Item item) {
        int count = 0;
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) {
            var stack = player.getInventory().getItem(i);
            if (stack.is(item)) count += stack.getCount();
        }
        return count;
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void catalogRegistrationAndRestartsKeepVanillaEntries(GameTestHelper h) {
        Set<String> ids = new HashSet<>();
        var originals = originals(VillagerProfession.FARMER, 1);
        h.assertTrue(!originals.isEmpty(), "Vanilla farmer entries remain available");
        ModVillagerTrades.register();
        ModVillagerTrades.refresh();
        ModVillagerTrades.refresh();
        for (var e : ModVillagerTrades.catalog()) {
            h.assertTrue(ids.add(e.id()) && e.level() >= 1 && e.level() <= 5, "Unique IDs and valid career levels");
            for (var pools : List.of(VillagerTrades.TRADES, VillagerTrades.EXPERIMENTAL_TRADES)) {
                long count = Arrays.stream(pools.get(e.profession()).get(e.level())).filter(candidate -> candidate == e).count();
                h.assertTrue(count == 1, "Exactly one candidate in normal and experimental pools: " + e.id());
            }
            var a = e.createOffer(new BetelNutConfig());
            var b = e.createOffer(new BetelNutConfig());
            a.increaseUses();
            h.assertTrue(a != b && b.getUses() == 0 && a.getMaxUses() > 0 && a.getXp() > 0,
                    "Each villager owns separate stock and valid XP");
        }
        h.assertTrue(originals.equals(originals(VillagerProfession.FARMER, 1)), "Refresh preserves vanilla factories and order");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void switchesRebuildPoolsWithoutEmptyVanillaSlots(GameTestHelper h) {
        var c = BetelNutConfig.get();
        boolean master = c.enableFarmerTrades, fish = c.enableFishermanTrades, cleric = c.enableClericTrades;
        var normal = originals(VillagerProfession.FARMER, 1);
        try {
            c.enableFarmerTrades = false;
            ModVillagerTrades.refresh();
            for (var e : ModVillagerTrades.catalog()) {
                h.assertTrue(e.getOffer(null, h.getLevel().random) == null, "Master switch covers all three professions");
                h.assertTrue(Arrays.stream(VillagerTrades.TRADES.get(e.profession()).get(e.level()))
                        .noneMatch(v -> v instanceof ModVillagerTrades.Entry), "Disabled entries do not occupy vanilla random slots");
            }
            for (int i = 0; i < 24; i++) {
                var v = villager(h, VillagerProfession.FARMER, 1);
                v.getRandom().setSeed(i);
                h.assertTrue(v.getOffers().size() == 2, "Disabled system leaves both vanilla trade slots intact");
            }
            c.enableFarmerTrades = true;
            c.enableFishermanTrades = false;
            c.enableClericTrades = false;
            ModVillagerTrades.refresh();
            h.assertTrue(entry("buy_green_bunch").getOffer(null, h.getLevel().random) != null
                    && entry("shell").getOffer(null, h.getLevel().random) == null
                    && entry("relief").getOffer(null, h.getLevel().random) == null, "Profession switches remain independent");
            c.enableFishermanTrades = true;
            c.enableClericTrades = true;
            ModVillagerTrades.refresh();
            h.assertTrue(entry("shell").getOffer(null, h.getLevel().random) != null
                    && entry("relief").getOffer(null, h.getLevel().random) != null, "A later world/reload can re-enable offers");
            h.assertTrue(normal.equals(originals(VillagerProfession.FARMER, 1)), "Toggle round trips preserve vanilla candidates");
        } finally {
            c.enableFarmerTrades = master;
            c.enableFishermanTrades = fish;
            c.enableClericTrades = cleric;
            ModVillagerTrades.refresh();
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void allTradesExecuteThroughRealMerchantMenu(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        for (var e : ModVillagerTrades.catalog()) {
            player.getInventory().clearContent();
            var trade = e.createOffer(new BetelNutConfig());
            var v = villager(h, e.profession(), e.level());
            var offers = new MerchantOffers();
            offers.add(trade);
            v.setOffers(offers);
            v.setTradingPlayer(player);
            var menu = new MerchantMenu(1, player.getInventory(), v);
            menu.setSelectionHint(0);
            menu.getSlot(0).set(trade.getCostA().copy());
            menu.getSlot(1).set(trade.getCostB().copy());
            h.assertTrue(ItemStack.matches(menu.getSlot(2).getItem(), trade.getResult()), "Server produces the correct result: " + e.id());
            menu.quickMoveStack(player, 2);
            h.assertTrue(inventory(player, trade.getResult().getItem()) == trade.getResult().getCount(), "Shift-click delivers one exact batch: " + e.id());
            h.assertTrue(menu.getSlot(0).getItem().isEmpty() && menu.getSlot(1).getItem().isEmpty()
                    && trade.getUses() == 1 && v.getVillagerXp() == trade.getXp(), "Both costs, stock and career XP update: " + e.id());
            h.assertTrue(menu.quickMoveStack(player, 2).isEmpty(), "No unpaid repeat transaction: " + e.id());
            menu.removed(player);
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void secondaryIngredientsAreMandatoryAndMaturityMatters(GameTestHelper h) {
        for (String id : List.of("relief", "golden_paste_service", "golden_paste", "enchanted_paste_service", "synthetic_world")) {
            var trade = offer(id);
            var payment = trade.getCostA().copy();
            h.assertTrue(!trade.take(payment, ItemStack.EMPTY) && ItemStack.matches(payment, trade.getCostA()), "Missing second ingredient cannot charge emeralds: " + id);
            var wrong = new ItemStack(Items.APPLE);
            h.assertTrue(!trade.take(payment, wrong) && wrong.getCount() == 1, "Incorrect ingredient is not consumed: " + id);
        }
        h.assertTrue(!offer("enchanted_paste_service").satisfiedBy(new ItemStack(Items.EMERALD, 64), new ItemStack(Items.GOLDEN_APPLE)),
                "Ordinary gold apples cannot be upgraded to enchanted paste");
        h.assertTrue(!offer("buy_ripe_bunch").satisfiedBy(new ItemStack(ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM, 64), ItemStack.EMPTY)
                && !offer("buy_green_bunch").satisfiedBy(new ItemStack(ModBlocks.BETEL_NUT_CLUSTER_ITEM, 64), ItemStack.EMPTY),
                "Whole green and ripe bunch payments stay distinct");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void offerFactoriesReadNewPricesWithoutRewritingSavedOffers(GameTestHelper h) {
        var c = BetelNutConfig.get();
        int old = c.clericTradeReliefEmeraldCost;
        try {
            var saved = entry("relief").getOffer(null, h.getLevel().random);
            c.clericTradeReliefEmeraldCost = 11;
            var fresh = entry("relief").getOffer(null, h.getLevel().random);
            h.assertTrue(fresh.getBaseCostA().getCount() == 11 && saved.getBaseCostA().getCount() == old,
                    "Config reload changes new offers and leaves saved prices unchanged");
        } finally { c.clericTradeReliefEmeraldCost = old; }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void outOfStockRefusesFurtherTradesAndRestockRestoresThem(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        player.getInventory().clearContent();
        var v = villager(h, VillagerProfession.CLERIC, 1);
        var trade = offer("relief");
        var offers = new MerchantOffers(); offers.add(trade); v.setOffers(offers); v.setTradingPlayer(player);
        trade.setToOutOfStock();
        var menu = new MerchantMenu(1, player.getInventory(), v);
        menu.getSlot(0).set(trade.getCostA().copy()); menu.getSlot(1).set(new ItemStack(Items.GLASS_BOTTLE));
        h.assertTrue(menu.getSlot(2).getItem().isEmpty() && menu.quickMoveStack(player, 2).isEmpty(), "Exhausted offer cannot issue another dose");
        v.restock();
        menu.getSlot(0).set(trade.getCostA().copy()); menu.setSelectionHint(0);
        h.assertTrue(!trade.isOutOfStock() && !menu.getSlot(2).getItem().isEmpty(), "Vanilla restock restores the offer with current demand");
        menu.quickMoveStack(player, 2);
        h.assertTrue(inventory(player, ModItems.RELIEF_POTION) == 1 && trade.getUses() == 1, "Restocked trade completes normally");
        menu.removed(player); h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void nativeVillagerSaveKeepsOffersStockAndComponents(GameTestHelper h) {
        var v = villager(h, VillagerProfession.CLERIC, 5);
        var offers = new MerchantOffers();
        for (var e : ModVillagerTrades.catalog()) {
            var trade = e.createOffer(new BetelNutConfig()); trade.increaseUses(); offers.add(trade);
        }
        v.setOffers(offers);
        var tag = new CompoundTag(); v.addAdditionalSaveData(tag);
        var loaded = villager(h, VillagerProfession.CLERIC, 1); loaded.readAdditionalSaveData(tag);
        h.assertTrue(loaded.getVillagerData().getLevel() == 5 && loaded.getOffers().size() == offers.size(), "Native save preserves career and all offers");
        for (int i = 0; i < offers.size(); i++) {
            var before = offers.get(i); var after = loaded.getOffers().get(i);
            h.assertTrue(ItemStack.matches(before.getBaseCostA(), after.getBaseCostA())
                    && ItemStack.matches(before.getCostB(), after.getCostB()) && ItemStack.matches(before.getResult(), after.getResult())
                    && after.getUses() == 1 && after.getMaxUses() == before.getMaxUses(), "Save round trip preserves inputs, components, results and stock");
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void oldOffersSurviveCatalogRefresh(GameTestHelper h) {
        var v = villager(h, VillagerProfession.FARMER, 3);
        var legacy = new MerchantOffer(new ItemCost(Items.EMERALD), new ItemStack(ModItems.RAW_BETEL_NUT, 4), 16, 2, 0.05F);
        legacy.increaseUses();
        var offers = new MerchantOffers(); offers.add(legacy); v.setOffers(offers);
        ModVillagerTrades.refresh();
        h.assertTrue(v.getOffers().getFirst() == legacy && legacy.getUses() == 1, "Refresh never clears a villager's established stock or prices");
        var tag = new CompoundTag(); v.addAdditionalSaveData(tag);
        var loaded = villager(h, VillagerProfession.FARMER, 1); loaded.readAdditionalSaveData(tag);
        h.assertTrue(loaded.getOffers().getFirst().getResult().is(ModItems.RAW_BETEL_NUT)
                && loaded.getOffers().getFirst().getUses() == 1, "Legacy offers remain readable across save/load");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void defaultsAndOldConfigCustomPricesValidate(GameTestHelper h) throws Exception {
        var old = new Gson().fromJson("{\"configVersion\":14,\"enableFarmerTrades\":false,\"farmerTradeRawBetelSellCount\":23,\"farmerTradeFlavorEmeraldCost\":9}", BetelNutConfig.class);
        var validate = BetelNutConfig.class.getDeclaredMethod("validated"); validate.setAccessible(true); validate.invoke(old);
        h.assertTrue(old.configVersion == 15 && !old.enableFarmerTrades && old.farmerTradeRawBetelSellCount == 23
                && old.farmerTradeFlavorEmeraldCost == 9 && old.clericTradeReliefEmeraldCost == 2,
                "Old custom prices and disabled state survive while missing new fields receive defaults");
        for (var field : BetelNutConfig.class.getFields()) {
            if (field.getType() != int.class || !field.getName().contains("Trade")) continue;
            field.setInt(old, -10); validate.invoke(old); h.assertTrue(field.getInt(old) == 1, "Trade counts cannot become free or negative");
            field.setInt(old, 1000); validate.invoke(old); h.assertTrue(field.getInt(old) == 64, "Costs and quantities fit vanilla stack limits");
        }
        var defaults = new BetelNutConfig();
        h.assertTrue(offer("buy_green_bunch").getBaseCostA().getCount() == 6 && offer("buy_ripe_bunch").getBaseCostA().getCount() == 4
                && offer("buy_leaves").getBaseCostA().getCount() == 24 && offer("brand_seal").getBaseCostA().getCount() == 2,
                "Balanced harvest and packaging defaults are active");
        h.assertTrue(entry("night").createOffer(defaults).getBaseCostA().getCount() == 4
                && entry("sweet").createOffer(defaults).getResult().getCount() == 2, "Advanced flavors cost more; basic flavors are paired");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void realVillagersDrawNormalOffersFromProfessionPools(GameTestHelper h) {
        for (var profession : List.of(VillagerProfession.FARMER, VillagerProfession.FISHERMAN, VillagerProfession.CLERIC)) {
            boolean foundMod = false, foundVanilla = false;
            for (int i = 0; i < 64; i++) {
                var v = villager(h, profession, 1); v.getRandom().setSeed(i);
                h.assertTrue(v.getOffers().size() == 2, "Vanilla still draws two novice offers");
                for (var trade : v.getOffers()) {
                    boolean mod = trade.getBaseCostA().is(ModItems.RAW_BETEL_NUT) || trade.getBaseCostA().is(ModItems.BETEL_LEAF)
                            || trade.getBaseCostA().is(ModBlocks.BETEL_NUT_CLUSTER_ITEM) || trade.getBaseCostA().is(ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM)
                            || trade.getResult().is(ModItems.SHELL) || trade.getResult().is(ModItems.RELIEF_POTION);
                    foundMod |= mod; foundVanilla |= !mod;
                }
            }
            h.assertTrue(foundMod && foundVanilla, "Actual generated villagers retain vanilla and include their new profession's candidates");
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void harvestCannotBeImmediatelyBoughtBackAtAnyDiscount(GameTestHelper h) {
        Set<Item> purchased = new HashSet<>(); Set<Item> sold = new HashSet<>();
        for (var e : ModVillagerTrades.catalog()) {
            var trade = e.createOffer(new BetelNutConfig());
            if (trade.getResult().is(Items.EMERALD)) purchased.add(trade.getBaseCostA().getItem());
            else {
                trade.setSpecialPriceDiff(-64);
                h.assertTrue(trade.getCostA().getCount() >= 1, "Vanilla discount never makes a sale free");
                sold.add(trade.getResult().getItem());
            }
        }
        h.assertTrue(purchased.stream().noneMatch(sold::contains), "No raw/bunch/leaf direct buyback loop even with curing discounts");
        h.assertTrue(!sold.contains(Items.ENCHANTED_GOLDEN_APPLE) && !sold.contains(ModItems.ANCIENT_SHELL)
                && !sold.contains(ModItems.GOLDEN_SHELL), "Exploration and lottery rarities retain their acquisition routes");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void tradesCommandRunsFromServerAndUsesCurrentCatalog(GameTestHelper h) throws Exception {
        var server = h.getLevel().getServer();
        int result = server.getCommands().getDispatcher().execute("betel trades info", server.createCommandSourceStack().withSuppressedOutput());
        h.assertTrue(result == 1 && !ModVillagerTrades.describe(new BetelNutConfig()).getString().isEmpty(), "Server command exposes the current shared catalog");
        h.succeed();
    }
}
