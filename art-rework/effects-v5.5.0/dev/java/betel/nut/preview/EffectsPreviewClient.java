package betel.nut.preview;

import betel.nut.BetelNutMod;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.item.ModItems;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;

public final class EffectsPreviewClient implements ClientModInitializer {
    private int tick;
    private volatile RuntimeException failure;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(c -> {
            if (failure != null) throw failure;
            if (c.player == null || c.level == null || c.getSingleplayerServer() == null) return;
            tick++; c.getToasts().clear();
            if (tick == 1) {
                c.options.pauseOnLostFocus = false; c.options.guiScale().set(2); c.resizeDisplay(); c.setScreen(null);
                server(c, p -> {
                    p.serverLevel().getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false, p.server);
                    p.serverLevel().setDayTime(6000);
                    for (int x = -2; x <= 2; x++) for (int z = -2; z <= 2; z++)
                        p.serverLevel().setBlock(new BlockPos(x, 79, z), Blocks.STONE.defaultBlockState(), 3);
                    p.teleportTo(p.serverLevel(), .5, 80, .5, Set.of(), 0, 0);
                    p.setGameMode(GameType.SURVIVAL); reset(p);
                    p.setHealth(10); p.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 0));
                    hold(p, ModItems.HONEY_BETEL_NUT, 2);
                });
            }
            if (tick == 30) start(c);
            if (tick == 70) stop(c);
            if (tick == 75) server(c, p -> {
                require(p.getMainHandItem().is(ModItems.HONEY_BETEL_NUT) && p.getMainHandItem().getCount() == 1, "Packet-driven food consumes one nut");
                require(!p.hasEffect(MobEffects.POISON) && p.hasEffect(MobEffects.REGENERATION) && !p.hasEffect(MobEffects.ABSORPTION), "Honey clears poison and grants only regeneration");
                require(p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE) == 1, "Food produces one residue");
            });
            if (tick == 85) c.setScreen(new InventoryScreen(c.player));
            if (tick == 90) capture(c, "honey-consumed.png");
            if (tick == 100) c.setScreen(new EffectTooltipScreen(ModItems.HONEY_BETEL_NUT));
            if (tick == 105) capture(c, "tooltip-honey.png");
            if (tick == 115) c.setScreen(new EffectTooltipScreen(ModItems.SPICY_BETEL_NUT));
            if (tick == 120) capture(c, "tooltip-spicy.png");
            if (tick == 130) c.setScreen(new EffectTooltipScreen(ModItems.SYNTHETIC_WORLD_BETEL));
            if (tick == 135) capture(c, "tooltip-synthetic.png");
            if (tick == 145) c.setScreen(new EffectTooltipScreen(ModItems.UNDERGROUND_BETEL));
            if (tick == 150) capture(c, "tooltip-underground.png");
            if (tick == 160) c.setScreen(new EffectTooltipScreen(ModItems.ENDER_BETEL_NUT));
            if (tick == 165) capture(c, "tooltip-ender.png");
            if (tick == 175) {
                c.setScreen(null);
                server(c, p -> { reset(p); BetelNutEntityComponents.ADDICTION.get(p).setAddictionValue(7); hold(p, ModItems.SYNTHETIC_WORLD_BETEL, 1); });
            }
            if (tick == 200) start(c);
            if (tick == 240) stop(c);
            if (tick == 245) server(c, p -> {
                var speed = p.getEffect(MobEffects.MOVEMENT_SPEED);
                require(speed != null && speed.getAmplifier() == 0 && speed.getDuration() <= 600 && speed.getDuration() > 500, "Synthetic respects stage-five tolerance over a real use packet");
                require(!p.hasEffect(MobEffects.FIRE_RESISTANCE) && !p.hasEffect(MobEffects.WATER_BREATHING) && !p.hasEffect(MobEffects.DAMAGE_BOOST), "Synthetic has no hidden extra burst");
                require(p.getMainHandItem().is(ModItems.BETEL_NUT_RESIDUE), "Last nut is replaced by residue");
            });
            if (tick == 250) c.setScreen(new InventoryScreen(c.player));
            if (tick == 255) capture(c, "synthetic-tolerance.png");
            if (tick == 265) { c.setScreen(null); server(c, p -> hold(p, ModItems.SYNTHETIC_WORLD_BETEL, 1)); }
            if (tick == 285) start(c);
            if (tick == 295) stop(c);
            if (tick == 300) server(c, p -> require(p.getMainHandItem().is(ModItems.SYNTHETIC_WORLD_BETEL) && p.getMainHandItem().getCount() == 1, "Repeat attempt leaves the second nut intact"));
            if (tick == 305) {
                BetelNutMod.LOGGER.info("EFFECTS_CLIENT_ALL_CHECKS_PASS honey=true tolerance=true repeatGuard=true tooltips=true"); c.stop();
            }
        });
    }
    private static void reset(ServerPlayer p) {
        var state = BetelNutEntityComponents.ADDICTION.get(p);
        state.clearAddiction(p);
        // Reused preview saves may retain the previous run's separate synthetic crash cycle.
        var saved = new net.minecraft.nbt.CompoundTag(); state.writeToNbt(saved, p.registryAccess());
        saved.putInt("hechengTianxiaPositiveTicks", 0); saved.putInt("hechengTianxiaComaTicks", 0);
        saved.putInt("hechengTianxiaNegativeTicks", 0); state.readFromNbt(saved, p.registryAccess());
        p.stopUsingItem(); p.removeAllEffects(); p.getInventory().clearContent();
    }
    private static void hold(ServerPlayer p, Item item, int count) {
        p.getFoodData().setFoodLevel(6); p.getInventory().selected = 0;
        p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, count)); p.inventoryMenu.broadcastChanges();
    }
    private static void start(Minecraft c) { c.options.keyUse.setDown(true); c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND); }
    private static void stop(Minecraft c) { c.options.keyUse.setDown(false); }
    private void server(Minecraft c, java.util.function.Consumer<ServerPlayer> action) {
        c.getSingleplayerServer().execute(() -> {
            try { action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst()); }
            catch (Exception e) { failure = new RuntimeException("Effects verification failed", e); }
        });
    }
    private static void require(boolean good, String message) { if (!good) throw new IllegalStateException(message); }
    private static void capture(Minecraft c, String name) {
        Screenshot.grab(c.gameDirectory, name, c.getMainRenderTarget(), msg -> BetelNutMod.LOGGER.info("EFFECTS_SCREENSHOT {}", name));
    }
    private static final class EffectTooltipScreen extends Screen {
        private final Item item;
        EffectTooltipScreen(Item item) { super(Component.empty()); this.item = item; }
        @Override public boolean isPauseScreen() { return false; }
        @Override public void render(GuiGraphics g, int mx, int my, float delta) {
            g.fill(0, 0, width, height, 0xFF15262D);
            g.drawCenteredString(font, Component.literal("Betel Nut 5.5.0"), width / 2, 30, 0xFFE4E8D2);
            var stack = new ItemStack(item);
            var lines = stack.getTooltipLines(Item.TooltipContext.of(minecraft.level), minecraft.player, TooltipFlag.NORMAL);
            for (var line : lines) require(!line.getString().contains("tooltip.betel-nut-mod.effects."), "All new tooltip keys resolve");
            g.renderItem(stack, width / 2 - 130, height / 2 - 70);
            g.renderTooltip(font, stack, width / 2 - 100, height / 2 - 70);
        }
    }
}
