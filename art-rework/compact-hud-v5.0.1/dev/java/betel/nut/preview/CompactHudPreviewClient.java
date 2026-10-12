package betel.nut.preview;

import betel.nut.BetelNutMod;
import betel.nut.client.BetelNutClientConfig;
import betel.nut.client.addiction.AddictionHudRenderer;
import betel.nut.client.addiction.ClientAddictionData;
import betel.nut.client.gui.HudEditScreen;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.item.ModItems;
import betel.nut.network.AddictionSyncPayload;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/** Runs only in an isolated copy of the development world, never in a player's save. */
public final class CompactHudPreviewClient implements ClientModInitializer {
    private int tick;
    private volatile RuntimeException failure;
    private int stages;
    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (failure != null) throw failure;
            if (c.player == null || c.level == null || c.getSingleplayerServer() == null) return;
            tick++; c.getToasts().clear(); c.gui.clear();
            if (tick == 1) {
                c.options.pauseOnLostFocus = false; c.options.hideGui = false;
                c.options.guiScale().set(3); c.resizeDisplay(); c.setScreen(null);
                verifyClientConfigMigration();
                var cfg = BetelNutClientConfig.get();
                require(cfg.configVersion == 3 && !cfg.hudUseCustomPosition && cfg.hudAnchor == BetelNutClientConfig.HudAnchor.BOTTOM_LEFT,
                        "v2 default HUD position migrates to an anchored bottom-left strip");
                server(c, p -> {
                    var w = p.serverLevel(); w.setDayTime(240006000L);
                    w.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, p.server);
                    w.getGameRules().getRule(GameRules.RULE_WEATHER_CYCLE).set(false, p.server);
                    w.setWeatherParameters(0, 12000, false, false);
                    p.setGameMode(GameType.SURVIVAL); p.getInventory().clearContent(); p.removeAllEffects();
                    p.teleportTo(w, 3.5, 80, -14, Set.of(), 135, 12);
                    var s = BetelNutEntityComponents.ADDICTION.get(p); s.clearAddiction(p); s.beginCalendarSession(p);
                    new ItemStack(ModItems.WRAPPED_RAW_BETEL).finishUsingItem(w, p);
                    new ItemStack(ModItems.SWEET_BETEL_NUT).finishUsingItem(w, p);
                    require(s.getAddictionValue() == 1 && s.getOralExposureDays() == 1, "same-day foods count once");
                    p.removeAllEffects(); p.setHealth(p.getMaxHealth()); p.getFoodData().setFoodLevel(20);
                    p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModItems.ANCIENT_SHELL));
                    p.inventoryMenu.broadcastChanges(); AddictionSyncPayload.send(p);
                });
            }
            if (tick == 80) {
                require(ClientAddictionData.getAddictionValue() == 1 && ClientAddictionData.getMaxAddictionValue() == 7
                        && ClientAddictionData.hasEatenToday(), "actual daily state and migrated max reach client");
                require(AddictionHudRenderer.getHudWidth() == 124 && AddictionHudRenderer.getHudHeight() == 26,
                        "normal HUD has the intended compact footprint");
                checkBounds(c, true); capture(c, "hud-day-1.png");
            }
            if (tick == 90) server(c, p -> {
                var s = BetelNutEntityComponents.ADDICTION.get(p);
                for (int day = 2; day <= 7; day++) {
                    p.server.overworld().setDayTime(240006000L + (day - 1) * 24000L);
                    new ItemStack(ModItems.WRAPPED_RAW_BETEL).finishUsingItem(p.serverLevel(), p);
                }
                require(s.getAddictionValue() == 7 && s.getAddictionStage() == 5, "seven actual eating dates fully addict");
                require(s.getOralExposureDays() == 7 && s.getOralCancerStage() == 0, "disease timeline remains separate");
                p.removeAllEffects(); AddictionSyncPayload.send(p);
            });
            if (tick == 125) {
                require(ClientAddictionData.getAddictionStage() == 5 && ClientAddictionData.getStageProgress() == 1F, "full stage sync and progress");
                capture(c, "hud-day-7.png");
            }
            if (tick == 135) server(c, p -> setCancer(p, 21, false));
            if (tick == 165) { require(ClientAddictionData.getMouthRestrictionLevel() == 1, "soft food alert sync"); capture(c, "hud-cancer-II.png"); }
            if (tick == 175) server(c, p -> setCancer(p, 30, true));
            if (tick == 205) {
                require(ClientAddictionData.getOralCancerStage() == 3 && ClientAddictionData.getMouthRestrictionLevel() == 2
                        && ClientAddictionData.getWithdrawalSeverity() > 0, "all abnormal alerts sync");
                require(AddictionHudRenderer.getHudHeight() == 40, "abnormal state uses only one extra row");
                checkBounds(c, true); capture(c, "hud-cancer-III.png");
            }
            if (tick == 215) c.setScreen(new InventoryScreen(c.player));
            if (tick == 235) capture(c, "inventory-without-hud.png");
            if (tick == 245) {
                c.setScreen(null); c.options.guiScale().set(5); c.resizeDisplay();
                checkBounds(c, true);
            }
            if (tick == 270) capture(c, "hud-large-gui.png");
            if (tick == 272) { BetelNutClientConfig.get().hudScale = 2; checkBounds(c, true); }
            if (tick == 278) capture(c, "hud-user-double-scale.png");
            if (tick == 280) {
                BetelNutClientConfig.get().hudScale = 1; c.options.guiScale().set(3); c.resizeDisplay();
                c.setScreen(new HoverPreviewScreen());
            }
            if (tick == 305) capture(c, "hud-editor-details.png");
            if (tick == 315) {
                c.screen.onClose(); require(!BetelNutClientConfig.get().hudUseCustomPosition, "viewing details preserves responsive anchoring");
                c.options.guiScale().set(5); c.resizeDisplay(); c.setScreen(new HudEditScreen());
                var pos = AddictionHudRenderer.getConfiguredPosition(c, false);
                c.screen.mouseClicked(pos.x()+5, pos.y()+5, 0);
                c.screen.mouseDragged(10000, 10000, 0, 9000, 9000); c.screen.mouseReleased(10000,10000,0);
                require(BetelNutClientConfig.get().hudUseCustomPosition, "drag saves a custom position"); checkBounds(c, false);
            }
            if (tick == 335) capture(c, "hud-editor-drag-clamped.png");
            if (tick == 345) {
                c.screen.onClose(); BetelNutClientConfig.get().hudUseCustomPosition = false;
                c.options.guiScale().set(3); c.resizeDisplay();
                c.getLanguageManager().setSelected("en_us"); c.options.languageCode = "en_us"; c.reloadResourcePacks();
            }
            if (tick == 420) { checkBounds(c, true); capture(c, "hud-english.png"); }
            if (tick == 430) server(c, p -> { BetelNutEntityComponents.ADDICTION.get(p).clearAddiction(p); p.removeAllEffects(); AddictionSyncPayload.send(p); });
            if (tick == 460) { require(!ClientAddictionData.shouldRenderHud(), "cleared player hides the HUD"); capture(c, "hud-healthy-hidden.png"); }
            if (tick == 470) { BetelNutMod.LOGGER.info("COMPACT_HUD_CLIENT_ALL_CHECKS_PASS screenshots={}", stages); c.stop(); }
        });
    }
    private static void verifyClientConfigMigration() {
        try {
            var v = BetelNutClientConfig.class.getDeclaredMethod("validated"); v.setAccessible(true);
            var custom = new BetelNutClientConfig(); custom.configVersion = 2; custom.setHudPosition(74, 82);
            custom.hudEnabled = false; custom.hudScale = 1.5; v.invoke(custom);
            require(custom.hudX == 74 && custom.hudY == 82 && custom.hudUseCustomPosition && !custom.hudEnabled && custom.hudScale == 1.5,
                    "custom position, scale and visibility survive upgrade");
        } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
    }
    private static void checkBounds(Minecraft c, boolean hotbar) {
        var p = AddictionHudRenderer.getConfiguredPosition(c, false);
        int w = AddictionHudRenderer.getScaledHudWidth(), h = AddictionHudRenderer.getScaledHudHeight();
        int sw = c.getWindow().getGuiScaledWidth(), sh = c.getWindow().getGuiScaledHeight();
        require(p.x() >= 0 && p.y() >= 0 && p.x()+w <= sw && p.y()+h <= sh, "HUD fits screen");
        if (hotbar && p.x()+w > sw/2-100 && p.x() < sw/2+100) require(p.y()+h <= sh-72, "anchored HUD stays clear of vanilla gameplay bars");
    }
    private static void setCancer(ServerPlayer p, int days, boolean withdrawal) {
        var s = BetelNutEntityComponents.ADDICTION.get(p); s.setOralExposureDays(days);
        if (withdrawal) s.setWithdrawalValue(p, 100);
        s.maintainOralEffects(p); p.setHealth(p.getMaxHealth()); AddictionSyncPayload.send(p);
    }
    private static void require(boolean good, String message) { if (!good) throw new IllegalStateException(message); }
    private void server(Minecraft c, java.util.function.Consumer<ServerPlayer> work) {
        c.getSingleplayerServer().execute(() -> { try { work.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst()); }
            catch (Exception e) { failure = new RuntimeException("Compact HUD verification failed", e); } });
    }
    private void capture(Minecraft c, String name) {
        stages++; Screenshot.grab(c.gameDirectory, name, c.getMainRenderTarget(), msg -> BetelNutMod.LOGGER.info("COMPACT_HUD_SCREENSHOT {}", name));
    }
    /** Supply a deterministic hover coordinate to the real editor renderer for screenshot QA. */
    private static final class HoverPreviewScreen extends Screen {
        private final HudEditScreen editor = new HudEditScreen();
        HoverPreviewScreen() { super(Component.empty()); }
        @Override protected void init() { editor.init(minecraft, width, height); }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void onClose() { editor.onClose(); }
        @Override public void render(GuiGraphics graphics, int mouseX, int mouseY, float delta) {
            var p = AddictionHudRenderer.getConfiguredPosition(minecraft, false);
            editor.render(graphics, p.x()+8, p.y()+8, delta);
        }
    }
}
