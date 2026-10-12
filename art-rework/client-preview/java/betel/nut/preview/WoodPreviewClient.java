package betel.nut.preview;

import betel.nut.BetelNutMod;
import betel.nut.block.ModBlocks;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

/** Development-only client/server interaction and model smoke test. */
public class WoodPreviewClient implements ClientModInitializer {
    private static final BlockPos DOOR = new BlockPos(0, 80, 45);
    private static final BlockPos PLATE = DOOR.south();
    private int ticks;
    private boolean prepared;
    private ItemEntity trigger;
    private volatile RuntimeException failure;

    @Override public void onInitializeClient() {
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            if (failure != null) throw failure;
            if (client.player == null || client.level == null || client.getSingleplayerServer() == null) return;
            if (!prepared) {
                prepared = true;
                client.options.pauseOnLostFocus = false;
                client.options.hideGui = false;
                client.options.guiScale().set(3);
                client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
                client.resizeDisplay();
                client.setScreen(null);
                server(client, () -> prepare(client));
            }
            ticks++;
            client.getToasts().clear();
            if (client.screen != null) client.setScreen(null);
            if (ticks == 60) {
                var missing = client.getModelManager().getMissingModel();
                for (var state : ModBlocks.BETEL_PALM_DOOR.getStateDefinition().getPossibleStates()) {
                    var model = client.getBlockRenderer().getBlockModel(state);
                    if (model == missing) throw new IllegalStateException("Missing door model: " + state);
                    int quads = model.getQuads(state, null, RandomSource.create(0)).size();
                    for (var side : Direction.values()) quads += model.getQuads(state, side, RandomSource.create(0)).size();
                    if (quads == 0) throw new IllegalStateException("Empty door model: " + state);
                }
                for (var block : new net.minecraft.world.level.block.Block[]{ModBlocks.BETEL_PALM_DOOR, ModBlocks.BETEL_PALM_PRESSURE_PLATE}) {
                    if (client.getItemRenderer().getModel(new ItemStack(block), client.level, client.player, 0) == missing)
                        throw new IllegalStateException("Missing item model: " + block);
                }
                BetelNutMod.LOGGER.info("WOOD_MODELS_VERIFIED doorStates=64 itemModels=2");
                capture(client, "wood-door-closed.png");
            }
            if (ticks == 70) server(client, () -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                player.inventoryMenu.broadcastChanges();
            });
            if (ticks == 85) clickDoor(client, DOOR.above());
            if (ticks == 110) {
                check(client, true, false);
                capture(client, "wood-door-manual-open.png");
                BetelNutMod.LOGGER.info("WOOD_CLIENT_MANUAL_OPEN_VERIFIED upper-half interaction synchronized");
            }
            if (ticks == 120) clickDoor(client, DOOR);
            if (ticks == 145) {
                check(client, false, false);
                server(client, () -> {
                    var level = client.getSingleplayerServer().overworld();
                    trigger = new ItemEntity(level, PLATE.getX() + .5, PLATE.getY() + .06, PLATE.getZ() + .5, new ItemStack(Items.STICK));
                    trigger.setNoGravity(true);
                    trigger.setDeltaMovement(Vec3.ZERO);
                    level.addFreshEntity(trigger);
                });
            }
            if (ticks == 180) {
                check(client, true, true);
                if (!client.level.getBlockState(PLATE).getValue(PressurePlateBlock.POWERED))
                    throw new IllegalStateException("Client did not receive plate activation");
                capture(client, "wood-pressure-plate-powered.png");
                BetelNutMod.LOGGER.info("WOOD_CLIENT_PLATE_ACTIVATION_VERIFIED natural item collision opened both door halves");
            }
            if (ticks == 190) server(client, () -> trigger.discard());
            if (ticks == 225) {
                check(client, false, false);
                if (client.level.getBlockState(PLATE).getValue(PressurePlateBlock.POWERED))
                    throw new IllegalStateException("Pressure plate remained powered after trigger removal");
                capture(client, "wood-pressure-plate-released.png");
                BetelNutMod.LOGGER.info("WOOD_CLIENT_PLATE_RELEASE_VERIFIED plate reset and both door halves closed");
            }
            if (ticks == 235) server(client, () -> {
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                player.teleportTo(player.serverLevel(), -3, 80, 48.5, Set.of(), 180, 8);
            });
            if (ticks == 260) capture(client, "wood-double-doors.png");
            if (ticks == 270) server(client, () -> {
                var level = client.getSingleplayerServer().overworld();
                var player = client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
                for (var pos : new BlockPos[]{DOOR.west(4), DOOR.west(3)}) {
                    ((DoorBlock) ModBlocks.BETEL_PALM_DOOR).setOpen(player, level, level.getBlockState(pos), pos, true);
                }
            });
            if (ticks == 295) capture(client, "wood-double-doors-open.png");
            if (ticks == 310) client.stop();
        });
    }

    private static void clickDoor(Minecraft client, BlockPos pos) {
        client.gameMode.useItemOn(client.player, InteractionHand.MAIN_HAND,
                new BlockHitResult(Vec3.atCenterOf(pos), Direction.SOUTH, pos, false));
    }

    private static void check(Minecraft client, boolean open, boolean powered) {
        for (var pos : new BlockPos[]{DOOR, DOOR.above()}) {
            var state = client.level.getBlockState(pos);
            if (!state.is(ModBlocks.BETEL_PALM_DOOR) || state.getValue(DoorBlock.OPEN) != open || state.getValue(DoorBlock.POWERED) != powered)
                throw new IllegalStateException("Unexpected client door state: " + state);
        }
    }

    private void server(Minecraft client, Runnable task) {
        client.getSingleplayerServer().execute(() -> {
            try { task.run(); } catch (Exception e) { failure = new RuntimeException("Wood preview verification failed", e); }
        });
    }

    private static void prepare(Minecraft client) {
        var server = client.getSingleplayerServer();
        var level = server.overworld();
        level.setDayTime(6000);
        level.setWeatherParameters(0, 12000, false, false);
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        for (var pos : BlockPos.betweenClosed(new BlockPos(-7, 80, 40), new BlockPos(7, 87, 54)))
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        for (var pos : BlockPos.betweenClosed(new BlockPos(-7, 79, 40), new BlockPos(7, 79, 54)))
            level.setBlock(pos, Blocks.SMOOTH_STONE.defaultBlockState(), 2);
        for (var pos : BlockPos.betweenClosed(new BlockPos(-5, 80, 43), new BlockPos(1, 82, 43)))
            level.setBlock(pos, Blocks.MAGENTA_CONCRETE.defaultBlockState(), 2);
        var player = server.getPlayerList().getPlayers().getFirst();
        player.setGameMode(GameType.CREATIVE);
        player.getAbilities().flying = false;
        player.onUpdateAbilities();
        player.getInventory().clearContent();
        player.getInventory().selected = 0;
        player.setYRot(180);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.BETEL_PALM_DOOR_ITEM));
        for (var pos : new BlockPos[]{DOOR, DOOR.west(4), DOOR.west(3)}) {
            var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false);
            player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        }
        level.setBlock(PLATE, ModBlocks.BETEL_PALM_PRESSURE_PLATE.defaultBlockState(), 3);
        player.getInventory().setItem(1, new ItemStack(ModBlocks.BETEL_PALM_PRESSURE_PLATE_ITEM));
        player.getInventory().setItem(2, new ItemStack(ModBlocks.BETEL_PALM_PLANKS_ITEM));
        player.getInventory().setItem(3, new ItemStack(ModBlocks.BETEL_PALM_TRAPDOOR_ITEM));
        player.inventoryMenu.broadcastChanges();
        player.teleportTo(level, .5, 80, 49, Set.of(), 180, 12);
    }

    private static void capture(Minecraft client, String name) {
        Screenshot.grab(client.gameDirectory, name, client.getMainRenderTarget(), message -> BetelNutMod.LOGGER.info("WOOD_PREVIEW {} {}", name, message.getString()));
    }
}
