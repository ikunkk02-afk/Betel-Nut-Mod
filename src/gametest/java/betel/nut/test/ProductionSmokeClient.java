package betel.nut.test;

import betel.nut.BetelNutMod;
import betel.nut.block.BetelNutClusterBlock;
import betel.nut.block.ModBlocks;
import betel.nut.worldgen.BetelPalmTreeGenerator;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.util.RandomSource;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.LevelSettings;
import net.minecraft.world.level.WorldDataConfiguration;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.levelgen.WorldOptions;
import net.minecraft.world.level.levelgen.presets.WorldPresets;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

/** Exercises client item-use packets and the resulting synchronized fruit states. */
@EventBusSubscriber(modid = BetelNutMod.MOD_ID, value = Dist.CLIENT)
public final class ProductionSmokeClient {
    private static final BlockPos BASE = new BlockPos(0, 100, 0);
    private static boolean launched;
    private static int ticks;
    private static volatile RuntimeException failure;

    @SubscribeEvent
    public static void tick(ClientTickEvent.Post event) {
        if (!Boolean.getBoolean("betel.productionSmoke")) return;
        if (failure != null) throw failure;
        var c = Minecraft.getInstance();
        if (!launched && c.screen != null && (c.screen instanceof TitleScreen
                || c.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen")) && c.getOverlay() == null) {
            launched = true;
            c.createWorldOpenFlows().createFreshLevel("Production580",
                    new LevelSettings("Production580", GameType.SURVIVAL, false, Difficulty.PEACEFUL, true,
                            new GameRules(), WorldDataConfiguration.DEFAULT), new WorldOptions(580L, false, false),
                    r -> r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(WorldPresets.FLAT).value().createWorldDimensions(),
                    new TitleScreen());
            return;
        }
        if (c.player == null || c.level == null || c.getSingleplayerServer() == null || c.getOverlay() != null) return;
        c.options.pauseOnLostFocus = false;
        ticks++;
        if (ticks == 1) {
            c.setScreen(null);
            c.options.renderDistance().set(6);
            c.options.guiScale().set(2);
            c.resizeDisplay();
            server(c, () -> {
                var level = c.getSingleplayerServer().overworld();
                var player = c.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                level.setDayTime(6000);
                level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, level.getServer());
                for (var pos : BlockPos.betweenClosed(-6, 99, -6, 6, 99, 6)) level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
                level.setBlock(BASE.below(), Blocks.DIRT.defaultBlockState(), 3);
                require(BetelPalmTreeGenerator.generateVariant(level, BASE, RandomSource.create(2), 9, 9, 0, false), "Tree generated");
                player.setInvulnerable(true);
                player.setGameMode(GameType.SURVIVAL);
                player.teleportTo(level, .5, 100, 3.5, Set.of(), 180, 0);
                player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.BONE_MEAL, 3));
                player.inventoryMenu.broadcastChanges();
            });
        }
        if (ticks == 40) {
            require(c.player.getMainHandItem().is(Items.BONE_MEAL), "Client received bone meal");
            c.gameMode.useItemOn(c.player, InteractionHand.MAIN_HAND,
                    new BlockHitResult(Vec3.atCenterOf(BASE), Direction.SOUTH, BASE, false));
        }
        if (ticks == 65) {
            int ripe = 0;
            for (var d : Direction.Plane.HORIZONTAL) {
                var state = c.level.getBlockState(BASE.above(6).relative(d));
                if (state.is(ModBlocks.BETEL_NUT_CLUSTER) && state.getValue(BetelNutClusterBlock.AGE) == 2) ripe++;
            }
            require(ripe == 3 && c.player.getMainHandItem().getCount() == 2, "Real client packet creates 3 synchronized ripe bunches and consumes 1 dose");
            BetelNutMod.LOGGER.info("PRODUCTION_CLIENT_BONEMEAL_PACKET_PASS");
            server(c, () -> {
                var p = c.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                p.setGameMode(GameType.CREATIVE);
                p.getAbilities().flying = true;
                p.onUpdateAbilities();
                p.teleportTo(p.serverLevel(), 8, 106, 12, Set.of(), 146, -8);
            });
        }
        if (ticks == 100) Screenshot.grab(c.gameDirectory, "production-580-ripe-tree.png", c.getMainRenderTarget(),
                message -> BetelNutMod.LOGGER.info("PRODUCTION_SCREENSHOT {}", message.getString()));
        if (ticks == 110) {
            BetelNutMod.LOGGER.info("PRODUCTION_CLIENT_ALL_CHECKS_PASS");
            c.stop();
        }
    }

    private static void server(Minecraft c, Runnable task) {
        c.getSingleplayerServer().execute(() -> {
            try { task.run(); } catch (Throwable error) { failure = new RuntimeException("Production client check failed", error); }
        });
    }
    private static void require(boolean pass, String message) {
        if (!pass) throw new IllegalStateException(message);
    }
}
