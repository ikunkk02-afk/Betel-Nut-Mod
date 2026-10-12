package betel.nut.preview;

import betel.nut.BetelNutMod;
import betel.nut.block.ModBlocks;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.item.ModItems;
import betel.nut.villager.ModVillagerTrades;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ServerboundSelectTradePacket;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.npc.Villager;
import net.minecraft.world.entity.npc.VillagerData;
import net.minecraft.world.entity.npc.VillagerProfession;
import net.minecraft.world.entity.npc.VillagerType;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.inventory.MerchantMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.trading.MerchantOffers;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public final class TradesPreviewClient implements ClientModInitializer {
    private int tick;
    private volatile RuntimeException failure;
    private static volatile int farmerId, fishermanId, clericId;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (failure != null) throw failure;
            if (c.player == null || c.level == null || c.getSingleplayerServer() == null) return;
            tick++; c.getToasts().clear(); c.gui.clear(); c.gui.getChat().clearMessages(false);
            if (tick == 1) {
                c.options.pauseOnLostFocus = false; c.options.guiScale().set(3); c.resizeDisplay(); c.setScreen(null);
                server(c, this::setup);
            }
            if (tick == 30) open(c, farmerId);
            if (tick == 45) select(c, 1);
            if (tick == 55) capture(c, "farmer-green-bunch.png");
            if (tick == 60) trade(c);
            if (tick == 75) server(c, p -> require(count(p, Items.EMERALD) == 1 && count(p, ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM) == 0, "Green bunch packet consumes six and pays one emerald"));
            if (tick == 80) c.player.closeContainer();
            if (tick == 85) server(c, p -> inventory(p, new ItemStack(ModBlocks.BETEL_NUT_CLUSTER_ITEM, 4)));
            if (tick == 95) open(c, farmerId);
            if (tick == 110) select(c, 2);
            if (tick == 120) capture(c, "farmer-ripe-bunch.png");
            if (tick == 125) trade(c);
            if (tick == 140) server(c, p -> require(count(p, Items.EMERALD) == 1 && count(p, ModBlocks.BETEL_NUT_CLUSTER_ITEM) == 0, "Ripe bunch packet consumes four and pays one emerald"));
            if (tick == 145) c.player.closeContainer();
            if (tick == 150) server(c, p -> inventory(p, new ItemStack(Items.EMERALD)));
            if (tick == 160) open(c, fishermanId);
            if (tick == 175) select(c, 1);
            if (tick == 185) capture(c, "fisherman-shell-powder.png");
            if (tick == 190) trade(c);
            if (tick == 205) server(c, p -> require(count(p, ModItems.SHELL_POWDER) == 8 && count(p, Items.EMERALD) == 0, "Fisherman packet sells eight powder"));
            if (tick == 210) c.player.closeContainer();
            if (tick == 215) server(c, p -> inventory(p, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.GLASS_BOTTLE)));
            if (tick == 225) open(c, clericId);
            if (tick == 240) select(c, 0);
            if (tick == 250) capture(c, "cleric-affordable-relief.png");
            if (tick == 255) trade(c);
            if (tick == 270) server(c, p -> require(count(p, ModItems.RELIEF_POTION) == 1 && count(p, Items.EMERALD) == 0 && count(p, Items.GLASS_BOTTLE) == 0, "Relief packet charges emeralds and bottle exactly once"));
            if (tick == 275) server(c, p -> inventory(p, new ItemStack(Items.EMERALD, 2), new ItemStack(Items.GOLDEN_APPLE)));
            if (tick == 290) select(c, 1);
            if (tick == 300) capture(c, "cleric-golden-paste-service.png");
            if (tick == 305) trade(c);
            if (tick == 320) server(c, p -> require(count(p, ModItems.GOLDEN_APPLE_PASTE) == 1 && count(p, Items.GOLDEN_APPLE) == 0 && count(p, Items.EMERALD) == 0, "Gold apple service consumes material and fee"));
            if (tick == 325) server(c, p -> inventory(p, new ItemStack(Items.EMERALD, 8), new ItemStack(Items.GOLDEN_APPLE)));
            if (tick == 340) select(c, 3);
            if (tick == 350) capture(c, "cleric-wrong-apple-rejected.png");
            if (tick == 355) server(c, p -> {
                var menu = menu(p);
                require(menu.getSlot(2).getItem().isEmpty(), "Wrong apple produces no enchanted paste");
                require(count(p, Items.GOLDEN_APPLE) == 1 && count(p, Items.EMERALD) + menu.getSlot(0).getItem().getCount() == 8, "Rejected trade consumes no payment");
            });
            if (tick == 360) server(c, p -> {
                menu(p).getSlot(0).set(ItemStack.EMPTY); menu(p).getSlot(1).set(ItemStack.EMPTY);
                inventory(p, new ItemStack(Items.EMERALD, 8), new ItemStack(Items.ENCHANTED_GOLDEN_APPLE));
            });
            if (tick == 375) select(c, 3);
            if (tick == 385) capture(c, "cleric-enchanted-paste-service.png");
            if (tick == 390) trade(c);
            if (tick == 405) server(c, p -> require(count(p, ModItems.ENCHANTED_GOLDEN_APPLE_PASTE) == 1 && count(p, Items.ENCHANTED_GOLDEN_APPLE) == 0 && count(p, Items.EMERALD) == 0, "Enchanted service requires and consumes enchanted apple"));
            if (tick == 410) {
                BetelNutMod.LOGGER.info("TRADES_CLIENT_ALL_CHECKS_PASS green=true ripe=true fisherman=true relief=true goldenService=true wrongApple=true enchantedService=true");
                c.stop();
            }
        });
    }

    private void setup(ServerPlayer p) {
        var w = p.serverLevel();
        w.setDayTime(6000); w.setWeatherParameters(0, 12000, false, false);
        w.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, p.server);
        w.getGameRules().getRule(GameRules.RULE_DOMOBSPAWNING).set(false, p.server);
        for (var v : w.getEntitiesOfClass(Villager.class, new AABB(new BlockPos(3, 80, 1)).inflate(15))) v.discard();
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-2, 79, -4), new BlockPos(9, 79, 4))) w.setBlock(pos, Blocks.OAK_PLANKS.defaultBlockState(), 3);
        for (BlockPos pos : BlockPos.betweenClosed(new BlockPos(-2, 80, -4), new BlockPos(9, 84, 4))) w.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        farmerId = spawn(p, VillagerProfession.FARMER, 5, 3, "农民 · 收购与加工品");
        fishermanId = spawn(p, VillagerProfession.FISHERMAN, 2, 5, "渔夫 · 贝壳材料");
        clericId = spawn(p, VillagerProfession.CLERIC, 5, 7, "牧师 · 缓解与糨糊加工");
        p.removeAllEffects(); BetelNutEntityComponents.ADDICTION.get(p).clearAddiction(p);
        p.setGameMode(GameType.SURVIVAL); p.setHealth(p.getMaxHealth());
        p.teleportTo(w, 5, 80, -2, Set.of(), 0, 0);
        p.lookAt(EntityAnchorArgument.Anchor.EYES, new Vec3(5, 81.5, 1));
        inventory(p, new ItemStack(ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM, 6));
    }

    private static int spawn(ServerPlayer p, VillagerProfession profession, int level, int x, String name) {
        var v = new Villager(EntityType.VILLAGER, p.serverLevel());
        v.setVillagerData(new VillagerData(VillagerType.PLAINS, profession, level));
        v.setNoAi(true); v.setInvulnerable(true); v.setCustomName(Component.literal(name)); v.setCustomNameVisible(true);
        v.moveTo(x, 80, 1, 180, 0);
        var offers = new MerchantOffers();
        for (var entry : ModVillagerTrades.catalog()) if (entry.profession() == profession) offers.add(entry.getOffer(v, v.getRandom()));
        v.setOffers(offers); p.serverLevel().addFreshEntity(v); return v.getId();
    }

    private static void inventory(ServerPlayer p, ItemStack... stacks) {
        p.getInventory().clearContent();
        for (int i = 0; i < stacks.length; i++) p.getInventory().setItem(i, stacks[i]);
        p.containerMenu.broadcastChanges();
    }
    private static int count(ServerPlayer p, Item item) { return p.getInventory().countItem(item); }
    private static MerchantMenu menu(ServerPlayer p) { return (MerchantMenu) p.containerMenu; }
    private static void open(Minecraft c, int id) {
        var v = c.level.getEntity(id); require(v != null, "Villager entity reaches client");
        c.gameMode.interact(c.player, v, InteractionHand.MAIN_HAND);
    }
    private static void select(Minecraft c, int index) {
        require(c.player.containerMenu instanceof MerchantMenu, "Real villager interaction opens merchant menu");
        ((MerchantMenu)c.player.containerMenu).setSelectionHint(index);
        c.getConnection().send(new ServerboundSelectTradePacket(index));
    }
    private static void trade(Minecraft c) {
        c.gameMode.handleInventoryMouseClick(c.player.containerMenu.containerId, 2, 0, ClickType.QUICK_MOVE, c.player);
    }
    private void server(Minecraft c, java.util.function.Consumer<ServerPlayer> action) {
        c.getSingleplayerServer().execute(() -> {
            try { action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst()); }
            catch (Exception e) { failure = new RuntimeException("Trades client verification failed", e); }
        });
    }
    private static void require(boolean good, String message) { if (!good) throw new IllegalStateException(message); }
    private static void capture(Minecraft c, String name) {
        Screenshot.grab(c.gameDirectory, name, c.getMainRenderTarget(), msg -> BetelNutMod.LOGGER.info("TRADES_SCREENSHOT {}", name));
    }
}
