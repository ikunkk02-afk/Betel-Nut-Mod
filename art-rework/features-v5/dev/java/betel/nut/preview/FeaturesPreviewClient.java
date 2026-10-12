package betel.nut.preview;

import betel.nut.BetelNutMod;
import betel.nut.client.BetelNutClientConfig;
import betel.nut.client.addiction.AddictionHudRenderer;
import betel.nut.client.addiction.ClientAddictionData;
import betel.nut.client.gui.HudEditScreen;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.effect.ModEffects;
import betel.nut.event.WithdrawalEatingRestrictions;
import betel.nut.item.ModItems;
import betel.nut.network.AddictionSyncPayload;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;

/** Actual client rendering plus server-validated use packets, in a copied test save. */
public final class FeaturesPreviewClient implements ClientModInitializer {
    private int tick;
    private volatile RuntimeException failure;
    private static final Item[] ART = {ModItems.SHELL, ModItems.SHELL_POWDER, ModItems.ANCIENT_SHELL,
        ModItems.GOLDEN_SHELL, ModItems.WRAPPED_RAW_BETEL, ModItems.ECHO_BETEL, ModItems.EMPTY_PACK,
        ModItems.BRAND_SEAL, ModItems.KOUWEIWANG_PACK, ModItems.HECHENG_TIANXIA_PACK,
        ModItems.XIANGTAN_PUZI_PACK, ModItems.LOTTERY_TICKET};
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (failure != null) throw failure;
            if (c.player == null || c.level == null || c.getSingleplayerServer() == null) return;
            tick++; c.getToasts().clear();
            if (tick == 1) {
                c.options.pauseOnLostFocus = false; c.options.hideGui = false;
                c.options.guiScale().set(3); c.resizeDisplay(); c.setScreen(null);
                BetelNutClientConfig.get().setHudPosition(10, 10);
                server(c, p -> {
                    var world = p.serverLevel();
                    world.setDayTime(240006000L);
                    world.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, p.server);
                    world.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, p.server);
                    world.setWeatherParameters(0, 12000, false, false);
                    p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent();
                    p.teleportTo(world, 3.5, 80, -14, Set.of(), 135, 12);
                    var s = BetelNutEntityComponents.ADDICTION.get(p); s.clearAddiction(p); s.beginCalendarSession(p);
                    s.setAddictionValue(6);
                    new ItemStack(ModItems.WRAPPED_RAW_BETEL).finishUsingItem(world, p);
                    new ItemStack(ModItems.SWEET_BETEL_NUT).finishUsingItem(world, p);
                    require(s.getAddictionValue() == 7 && s.getOralExposureDays() == 1, "same-day foods count once");
                    p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ANCIENT_SHELL));
                    p.inventoryMenu.broadcastChanges(); AddictionSyncPayload.send(p);
                });
            }
            if (tick == 90) {
                require(ClientAddictionData.getAddictionValue() == 7 && ClientAddictionData.hasEatenToday(), "daily state reaches client");
                capture(c, "hud-daily.png");
            }
            if (tick == 100) server(c, p -> {
                p.getInventory().clearContent(); p.getInventory().selected = 0;
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.KOUWEIWANG_PACK)); p.inventoryMenu.broadcastChanges();
            });
            if (tick == 130) c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND);
            if (tick == 160) server(c, p -> {
                require(count(p, ModItems.KOUWEIWANG_PACK) == 0 && count(p, ModItems.LOTTERY_TICKET) == 1, "network last pack consumes once and issues ticket");
                for (Item nut : new Item[]{ModItems.ROASTED_BETEL_NUT, ModItems.SPICY_BETEL_NUT, ModItems.SWEET_BETEL_NUT,
                    ModItems.REFRESHING_BETEL_NUT, ModItems.NIGHT_BETEL_NUT, ModItems.ENERGIZING_BETEL_NUT})
                    require(count(p, nut) == 1, "network pack has each distinct flavor");
                ItemStack ticket = p.getInventory().items.stream().filter(i -> i.is(ModItems.LOTTERY_TICKET)).findFirst().orElseThrow().copy();
                p.getInventory().clearContent(); p.setItemInHand(InteractionHand.MAIN_HAND, ticket); p.inventoryMenu.broadcastChanges();
            });
            if (tick == 190) c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND);
            if (tick == 220) server(c, p -> {
                require(count(p, ModItems.LOTTERY_TICKET) == 0, "network ticket is consumed once");
                BetelNutMod.LOGGER.info("FEATURE_NETWORK_PACK_AND_LOTTERY_PASS");
                p.getInventory().clearContent(); for (int i=0; i<ART.length; i++) p.getInventory().setItem(i, new ItemStack(ART[i]));
                p.inventoryMenu.broadcastChanges();
            });
            if (tick == 240) c.setScreen(new ArtScreen());
            if (tick == 270) capture(c, "imagegen-items-in-game.png");
            if (tick == 280) c.setScreen(null);
            if (tick == 290) server(c, p -> setCancer(p, 12));
            if (tick == 330) { require(ClientAddictionData.getOralCancerStage() == 1, "cancer I sync"); capture(c, "hud-cancer-I.png"); }
            if (tick == 340) server(c, p -> setCancer(p, 21));
            if (tick == 380) { require(ClientAddictionData.getMouthRestrictionLevel() == 1, "soft food sync"); capture(c, "hud-cancer-II.png"); }
            if (tick == 390) server(c, p -> {
                setCancer(p, 30); p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.WRAPPED_RAW_BETEL));
                p.getFoodData().setFoodLevel(6); p.inventoryMenu.broadcastChanges();
            });
            if (tick == 420) c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND);
            if (tick == 460) server(c, p -> {
                require(p.getMainHandItem().getCount() == 1 && !p.isUsingItem(), "locked jaw blocks real use packet");
                require(WithdrawalEatingRestrictions.canUseAtCurrentRestriction(p, BetelNutEntityComponents.ADDICTION.get(p), new ItemStack(Items.MILK_BUCKET)), "liquids stay available");
                BetelNutMod.LOGGER.info("FEATURE_NETWORK_LOCKED_JAW_PASS");
            });
            if (tick == 470) { require(ClientAddictionData.getMouthRestrictionLevel() == 2, "locked jaw sync"); capture(c, "hud-cancer-III-locked.png"); }
            if (tick == 480) c.setScreen(new InventoryScreen(c.player));
            if (tick == 510) capture(c, "effects-inventory.png");
            if (tick == 520) { c.setScreen(null); server(c, p -> {
                var s = BetelNutEntityComponents.ADDICTION.get(p);
                new ItemStack(Items.ENCHANTED_GOLDEN_APPLE).finishUsingItem(p.serverLevel(), p);
                require(s.getOralCancerStage() == 1 && !p.hasEffect(ModEffects.MOUTH_LOCKED), "magic food repairs exposure and jaw");
                require(s.getAddictionValue() == 2, "magic recovery reduces five dependence days");
                BetelNutMod.LOGGER.info("FEATURE_CLIENT_RECOVERY_PASS");
            }); }
            if (tick == 550) { require(ClientAddictionData.getMouthRestrictionLevel() == 0, "recovery reaches client"); capture(c, "hud-after-recovery.png"); }
            if (tick == 560) {
                c.options.guiScale().set(5); c.resizeDisplay(); c.setScreen(new HudEditScreen());
                c.screen.mouseClicked(15, 15, 0); c.screen.mouseDragged(10000, 10000, 0, 9985, 9985); c.screen.mouseReleased(10000,10000,0);
                var pos = AddictionHudRenderer.getConfiguredPosition(c, false);
                require(pos.x() >= 0 && pos.x() + AddictionHudRenderer.getScaledHudWidth() <= c.getWindow().getGuiScaledWidth(), "narrow GUI drag stays on screen");
            }
            if (tick == 590) capture(c, "hud-editor-small-gui.png");
            if (tick == 600) { c.setScreen(null); BetelNutMod.LOGGER.info("FEATURE_CLIENT_ALL_CHECKS_PASS"); c.stop(); }
        });
    }
    private static void setCancer(ServerPlayer p, int days) {
        var s = BetelNutEntityComponents.ADDICTION.get(p); s.setOralExposureDays(days); s.maintainOralEffects(p);
        p.setHealth(p.getMaxHealth()); AddictionSyncPayload.send(p);
    }
    private static int count(ServerPlayer p, Item item) { return p.getInventory().items.stream().filter(s -> s.is(item)).mapToInt(ItemStack::getCount).sum(); }
    private static void require(boolean good, String message) { if (!good) throw new IllegalStateException(message); }
    private void server(Minecraft c, java.util.function.Consumer<ServerPlayer> work) {
        c.getSingleplayerServer().execute(() -> { try { work.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst()); }
            catch (Exception e) { failure = new RuntimeException("Feature verification failed", e); } });
    }
    private static void capture(Minecraft c, String name) {
        Screenshot.grab(c.gameDirectory, name, c.getMainRenderTarget(), msg -> BetelNutMod.LOGGER.info("FEATURE_SCREENSHOT {}", name));
    }
    private static final class ArtScreen extends Screen {
        ArtScreen() { super(Component.literal("imagegen")); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(GuiGraphics g, int mx, int my, float delta) {
            g.fill(0, 0, width, height, 0xFF14232B);
            g.drawCenteredString(font, Component.literal("IMAGEGEN / 5.0.0"), width/2, 12, 0xFFEAE5D5);
            int cell = Math.min(116, (width-16)/4), left=(width-cell*4)/2;
            for(int i=0; i<ART.length; i++) {
                int x=left+(i%4)*cell, y=34+(i/4)*92;
                g.fill(x+2,y, x+cell-2,y+86,0xFF20353C);
                g.pose().pushPose(); g.pose().translate(x+cell/2-24,y+3,0);g.pose().scale(3,3,1);
                g.renderItem(new ItemStack(ART[i]),0,0);g.pose().popPose();
                g.drawCenteredString(font, new ItemStack(ART[i]).getHoverName(),x+cell/2,y+60,0xFFE7DFCD);
            }
        }
    }
}
