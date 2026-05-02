package betel.nut.skyblock;

import java.util.List;
import java.util.Set;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMidnightConfig;
import betel.nut.BetelNutMod;
import betel.nut.advancement.BetelQuestAdvancements;
import betel.nut.block.ModBlocks;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.component.BetelNutWorldComponents;
import betel.nut.component.BetelSkyblockPlayerComponent;
import betel.nut.component.BetelSkyblockWorldComponent;
import betel.nut.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerWorldEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.Filterable;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.WrittenBookContent;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.ChestBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.phys.Vec3;

public final class BetelSkyblockManager {
	private static final int CENTER_X = 0;
	private static final int CENTER_Z = 0;
	private static final int SPAWN_CLEAR_RADIUS = 2;
	private static final int STARTUP_CHECK_DELAY_TICKS = 1;
	private static final int CLASSIC_PROTECTION_XZ_RADIUS = 8;
	private static final int CLASSIC_PROTECTION_MIN_Y_OFFSET = -4;
	private static final int CLASSIC_PROTECTION_MAX_Y_OFFSET = 8;
	private static final int ONE_BLOCK_PROTECTION_XZ_RADIUS = 2;
	private static final int ONE_BLOCK_PROTECTION_MIN_Y_OFFSET = -1;
	private static final int ONE_BLOCK_PROTECTION_MAX_Y_OFFSET = 4;

	private static boolean startupCheckScheduled;
	private static boolean startupCheckFinished;
	private static int startupCheckDelay;

	public enum GenerationResult {
		GENERATED,
		NOT_OVERWORLD,
		ALREADY_GENERATED,
		TARGET_OCCUPIED
	}

	public static void register() {
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Betel Skyblock event handlers registered");

		ServerLifecycleEvents.SERVER_STARTED.register(server -> scheduleStartupCheck("server started"));
		ServerWorldEvents.LOAD.register((server, level) -> {
			if (level.dimension() == Level.OVERWORLD) {
				BetelNutMod.LOGGER.info("[Betel Nut Mod] Betel Skyblock overworld load event received");
				scheduleStartupCheck("overworld load");
			}
		});
		ServerTickEvents.START_SERVER_TICK.register(BetelSkyblockManager::runScheduledStartupCheck);
		ServerTickEvents.END_SERVER_TICK.register(BetelSkyblockManager::handleSkyblockPlayers);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> resetStartupCheckState());
		EndFrameWaterRitualManager.register();
	}

	public static boolean isConfigEnabled() {
		return BetelNutConfig.get().betelSkyblockEnabled;
	}

	public static boolean isEnabled(ServerLevel level) {
		return isBetelSkyblockPreset(level) && BetelNutMidnightConfig.enableClassicSkyblock;
	}

	public static boolean isOneBlockEnabled(ServerLevel level) {
		return isBetelOneBlockSkyblockPreset(level) && BetelNutMidnightConfig.enableOneBlockSkyblock;
	}

	public static boolean isBetelSkyblockPreset(ServerLevel level) {
		FlatLevelSource flatLevelSource = getOverworldFlatLevelSource(level);
		return flatLevelSource != null
				&& usesTheVoidBiome(flatLevelSource)
				&& hasNoFlatLayers(flatLevelSource);
	}

	public static boolean isBetelOneBlockSkyblockPreset(ServerLevel level) {
		FlatLevelSource flatLevelSource = getOverworldFlatLevelSource(level);
		return flatLevelSource != null
				&& usesTheVoidBiome(flatLevelSource)
				&& hasOneAirMarkerLayer(flatLevelSource);
	}

	public static BlockPos getIslandCenter() {
		return getConfiguredIslandCenter();
	}

	public static BlockPos getSkyblockSpawn() {
		return getSkyblockSpawnForCenter(getConfiguredIslandCenter());
	}

	public static BlockPos getSkyblockSpawn(ServerLevel level) {
		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		return getSkyblockSpawn(worldData);
	}

	public static BlockPos getOneBlockCenter() {
		return getConfiguredOneBlockCenter();
	}

	public static BlockPos getOneBlockSpawn() {
		return getOneBlockSpawnForCenter(getConfiguredOneBlockCenter());
	}

	public static BlockPos getOneBlockSpawn(ServerLevel level) {
		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		return getOneBlockSpawn(worldData);
	}

	private static BlockPos getSkyblockSpawn(BetelSkyblockWorldComponent worldData) {
		return getSkyblockSpawnForCenter(worldData.getIslandCenter());
	}

	private static BlockPos getSkyblockSpawnForCenter(BlockPos islandCenter) {
		return islandCenter.above(4);
	}

	private static BlockPos getOneBlockSpawn(BetelSkyblockWorldComponent worldData) {
		return getOneBlockSpawnForCenter(worldData.getOneBlockCenter());
	}

	private static BlockPos getOneBlockSpawnForCenter(BlockPos oneBlockCenter) {
		return oneBlockCenter.above(2);
	}

	private static BlockPos getConfiguredIslandCenter() {
		return new BlockPos(CENTER_X, BetelNutMidnightConfig.classicIslandY, CENTER_Z);
	}

	private static BlockPos getConfiguredOneBlockCenter() {
		return new BlockPos(CENTER_X, BetelNutMidnightConfig.oneBlockY, CENTER_Z);
	}

	public static void onBetelNutEaten(ServerPlayer player) {
		if (player.level().dimension() != Level.OVERWORLD) {
			return;
		}

		ServerLevel level = player.serverLevel();
		boolean normalSkyblockMode = isEnabled(level);
		boolean oneBlockMode = isOneBlockEnabled(level);
		if (!normalSkyblockMode && !oneBlockMode) {
			return;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		if (normalSkyblockMode && !worldData.hasGeneratedBetelSkyIsland()) {
			return;
		}
		if (oneBlockMode && !worldData.hasGeneratedBetelOneBlockIsland()) {
			return;
		}

		BetelSkyblockPlayerComponent playerData = BetelNutEntityComponents.SKYBLOCK_PLAYER.get(player);
		if (!playerData.hasEatenFirstBetelNutInSkyblock()) {
			playerData.markEatenFirstBetelNutInSkyblock();
			player.sendSystemMessage(Component.translatable("message.betel-nut-mod.skyblock_first_residue"));
			player.giveExperiencePoints(1);
			player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F);
			BetelNutMod.LOGGER.info("Player {} ate their first betel nut in Betel Skyblock mode.",
					player.getScoreboardName());
		}
		BetelQuestAdvancements.grantEatFirstBetelNut(player);
	}

	public static GenerationResult generateForCommand(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return GenerationResult.NOT_OVERWORLD;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		if (worldData.hasGeneratedBetelSkyIsland()) {
			return GenerationResult.ALREADY_GENERATED;
		}
		if (isClassicTargetAreaOccupied(level, getConfiguredIslandCenter())) {
			BetelNutMod.LOGGER.warn(
					"[Betel Nut Mod] Classic Betel Skyblock target area already has blocks; safe generation cancelled to avoid overwriting player builds.");
			return GenerationResult.TARGET_OCCUPIED;
		}

		generateInitialIsland(level, worldData, false);
		return GenerationResult.GENERATED;
	}

	public static GenerationResult forceGenerateForCommand(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return GenerationResult.NOT_OVERWORLD;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		generateInitialIsland(level, worldData, true);
		return GenerationResult.GENERATED;
	}

	public static GenerationResult generateOneBlockForCommand(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return GenerationResult.NOT_OVERWORLD;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		if (worldData.hasGeneratedBetelOneBlockIsland()) {
			return GenerationResult.ALREADY_GENERATED;
		}
		if (isOneBlockTargetAreaOccupied(level, getConfiguredOneBlockCenter())) {
			BetelNutMod.LOGGER.warn(
					"[Betel Nut Mod] Betel One Block Skyblock target area already has blocks; safe generation cancelled to avoid overwriting player builds.");
			return GenerationResult.TARGET_OCCUPIED;
		}

		generateOneBlockIsland(level, worldData, false);
		return GenerationResult.GENERATED;
	}

	public static GenerationResult forceGenerateOneBlockForCommand(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return GenerationResult.NOT_OVERWORLD;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		generateOneBlockIsland(level, worldData, true);
		return GenerationResult.GENERATED;
	}

	public static void resetGenerationState(ServerLevel level) {
		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		worldData.resetBetelSkyIslandGeneration();
		BetelNutMod.LOGGER.info(
				"[Betel Nut Mod] hasGeneratedBetelSkyIsland reset to false; automatic generation is paused until /betelskyblock generate is run.");
	}

	public static void resetOneBlockGenerationState(ServerLevel level) {
		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		worldData.resetBetelOneBlockIslandGeneration();
		BetelNutMod.LOGGER.info(
				"[Betel Nut Mod] hasGeneratedBetelOneBlockIsland reset to false; automatic generation is paused until /betelskyblock generate_one_block is run.");
	}

	public static boolean teleportPlayerToSkyblockSpawn(ServerPlayer player, ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		if (!worldData.hasGeneratedBetelSkyIsland()) {
			return false;
		}
		if (!worldData.hasSetSkyblockSpawn()) {
			setSkyblockSpawn(level, worldData, false);
		}
		return teleportPlayerToSkyblockSpawn(player, level, getSkyblockSpawn(worldData));
	}

	public static boolean teleportPlayerToOneBlockSpawn(ServerPlayer player, ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		if (!worldData.hasGeneratedBetelOneBlockIsland()) {
			return false;
		}
		if (!worldData.hasSetBetelOneBlockSpawn()) {
			setBetelOneBlockSpawn(level, worldData, false);
		}
		return teleportPlayerToSkyblockSpawn(player, level, getOneBlockSpawn(worldData));
	}

	private static void scheduleStartupCheck(String reason) {
		if (startupCheckFinished) {
			return;
		}

		startupCheckScheduled = true;
		startupCheckDelay = STARTUP_CHECK_DELAY_TICKS;
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Betel Skyblock startup check scheduled: {}", reason);
	}

	private static void runScheduledStartupCheck(MinecraftServer server) {
		if (!startupCheckScheduled || startupCheckFinished) {
			return;
		}

		if (startupCheckDelay > 0) {
			startupCheckDelay--;
			return;
		}

		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) {
			BetelNutMod.LOGGER.warn("[Betel Nut Mod] Betel Skyblock check delayed because overworld is not loaded");
			startupCheckDelay = 20;
			return;
		}

		try {
			generateInitialIslandIfNeeded(overworld);
			startupCheckScheduled = false;
			startupCheckFinished = true;
		} catch (RuntimeException exception) {
			BetelNutMod.LOGGER.error(
					"[Betel Nut Mod] Betel Skyblock check failed during server tick; retrying in 20 ticks.",
					exception);
			startupCheckScheduled = true;
			startupCheckFinished = false;
			startupCheckDelay = 20;
		}
	}

	private static void resetStartupCheckState() {
		startupCheckScheduled = false;
		startupCheckFinished = false;
		startupCheckDelay = 0;
	}

	private static void handleSkyblockPlayers(MinecraftServer server) {
		ServerLevel overworld = server.getLevel(Level.OVERWORLD);
		if (overworld == null) {
			return;
		}

		boolean oneBlockMode = isOneBlockEnabled(overworld);
		boolean normalSkyblockMode = isEnabled(overworld);
		if (!oneBlockMode && !normalSkyblockMode) {
			return;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		if (oneBlockMode) {
			handleOneBlockSkyblockPlayers(server, overworld, worldData);
			return;
		}

		if (!worldData.hasGeneratedBetelSkyIsland()) {
			return;
		}

		if (!worldData.hasSetSkyblockSpawn()) {
			setSkyblockSpawn(overworld, worldData, false);
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.level().dimension() != Level.OVERWORLD || !player.isAlive()) {
				continue;
			}
			handleFirstSkyblockJoin(player, overworld, worldData);
			BetelQuestAdvancements.grantEnterBetelSkyblock(player);
			handleVoidProtection(player, overworld, worldData);
		}
	}

	private static void handleOneBlockSkyblockPlayers(MinecraftServer server, ServerLevel overworld,
			BetelSkyblockWorldComponent worldData) {
		if (!worldData.hasGeneratedBetelOneBlockIsland()) {
			return;
		}

		if (!worldData.hasSetBetelOneBlockSpawn()) {
			setBetelOneBlockSpawn(overworld, worldData, false);
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.level().dimension() != Level.OVERWORLD || !player.isAlive()) {
				continue;
			}
			handleFirstOneBlockSkyblockJoin(player, overworld, worldData);
			BetelQuestAdvancements.grantEnterBetelSkyblock(player);
			handleOneBlockVoidProtection(player, overworld, worldData);
		}
	}

	private static void handleFirstSkyblockJoin(ServerPlayer player, ServerLevel overworld,
			BetelSkyblockWorldComponent worldData) {
		BetelSkyblockPlayerComponent playerData = BetelNutEntityComponents.SKYBLOCK_PLAYER.get(player);
		if (playerData.hasJoinedBetelSkyblockBefore()) {
			return;
		}

		boolean teleported = teleportPlayerToSkyblockSpawn(player, overworld, getSkyblockSpawn(worldData));
		if (!teleported) {
			BetelNutMod.LOGGER.warn(
					"[Betel Nut Mod] Failed to move first-time Betel Skyblock player {} to the sky island spawn.",
					player.getScoreboardName());
			return;
		}

		applySpawnProtection(player);
		playerData.markJoinedBetelSkyblockBefore();
		BetelNutMod.LOGGER.info("Player {} joined Betel Skyblock for the first time and was moved to spawn.",
				player.getScoreboardName());
	}

	private static void handleFirstOneBlockSkyblockJoin(ServerPlayer player, ServerLevel overworld,
			BetelSkyblockWorldComponent worldData) {
		BetelSkyblockPlayerComponent playerData = BetelNutEntityComponents.SKYBLOCK_PLAYER.get(player);
		if (playerData.hasReceivedBetelOneBlockStarterItems()) {
			return;
		}

		boolean teleported = teleportPlayerToSkyblockSpawn(player, overworld, getOneBlockSpawn(worldData));
		if (!teleported) {
			BetelNutMod.LOGGER.warn(
					"[Betel Nut Mod] Failed to move first-time Betel One Block Skyblock player {} to spawn.",
					player.getScoreboardName());
			return;
		}

		applySpawnProtection(player);
		if (BetelNutMidnightConfig.oneBlockGiveStarterItems) {
			giveOneBlockStarterItems(player);
		}
		sendOneBlockWelcomeMessage(player);
		playerData.markReceivedBetelOneBlockStarterItems();
		BetelNutMod.LOGGER.info(
				"Player {} joined Betel One Block Skyblock for the first time and received starter items.",
				player.getScoreboardName());
	}

	private static void handleVoidProtection(ServerPlayer player, ServerLevel overworld,
			BetelSkyblockWorldComponent worldData) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!BetelNutMidnightConfig.classicEnableVoidRescue
				|| player.getY() >= config.betelSkyblockVoidProtectionMinY) {
			return;
		}

		boolean teleported = teleportPlayerToSkyblockSpawn(player, overworld, getSkyblockSpawn(worldData));
		if (!teleported) {
			return;
		}

		if (config.betelSkyblockVoidProtectionDamage
				&& config.betelSkyblockVoidProtectionDamageAmount > 0.0D) {
			player.hurt(player.damageSources().generic(),
					(float) config.betelSkyblockVoidProtectionDamageAmount);
		}
		applySpawnProtection(player);
		player.sendSystemMessage(Component.translatable("message.betel-nut-mod.void_rescue.classic"));
	}

	private static void handleOneBlockVoidProtection(ServerPlayer player, ServerLevel overworld,
			BetelSkyblockWorldComponent worldData) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!BetelNutMidnightConfig.oneBlockEnableVoidRescue
				|| player.getY() >= config.betelSkyblockVoidProtectionMinY) {
			return;
		}

		boolean teleported = teleportPlayerToSkyblockSpawn(player, overworld, getOneBlockSpawn(worldData));
		if (!teleported) {
			return;
		}

		if (config.betelSkyblockVoidProtectionDamage
				&& config.betelSkyblockVoidProtectionDamageAmount > 0.0D) {
			player.hurt(player.damageSources().generic(),
					(float) config.betelSkyblockVoidProtectionDamageAmount);
		}
		applySpawnProtection(player);
		player.sendSystemMessage(Component.translatable("message.betel-nut-mod.void_rescue.one_block"));
	}

	private static boolean teleportPlayerToSkyblockSpawn(ServerPlayer player, ServerLevel level, BlockPos spawnPos) {
		boolean teleported = player.teleportTo(level, spawnPos.getX() + 0.5D, spawnPos.getY(),
				spawnPos.getZ() + 0.5D, Set.<RelativeMovement>of(), player.getYRot(), player.getXRot());
		if (teleported) {
			player.setDeltaMovement(Vec3.ZERO);
			player.resetFallDistance();
		}
		return teleported;
	}

	private static void applySpawnProtection(ServerPlayer player) {
		int duration = BetelNutConfig.get().betelSkyblockSpawnProtectionTicks;
		if (duration <= 0) {
			return;
		}
		player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, duration, 0, false, false, true));
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, duration, 0, false, false, true));
	}

	private static void giveOneBlockStarterItems(ServerPlayer player) {
		giveOrDrop(player, new ItemStack(ModBlocks.BETEL_PALM_SAPLING_ITEM,
				BetelNutMidnightConfig.oneBlockStarterSaplingCount));
		giveOrDrop(player, new ItemStack(Items.DIRT, BetelNutMidnightConfig.oneBlockStarterDirtCount));
		giveOrDrop(player, new ItemStack(ModItems.ROASTED_BETEL_NUT,
				BetelNutMidnightConfig.oneBlockStarterBetelNutCount));
	}

	private static void giveOrDrop(ServerPlayer player, ItemStack stack) {
		if (stack.isEmpty() || stack.getCount() <= 0) {
			return;
		}
		if (!player.getInventory().add(stack)) {
			player.drop(stack, false);
		}
	}

	private static void sendOneBlockWelcomeMessage(ServerPlayer player) {
		player.sendSystemMessage(Component.translatable("message.betel-nut-mod.one_block_welcome"));
	}

	private static void generateInitialIslandIfNeeded(ServerLevel level) {
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Checking Betel Skyblock world...");
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Current dimension: {}", dimensionLabel(level));

		if (level.dimension() != Level.OVERWORLD) {
			BetelNutMod.LOGGER.info("[Betel Nut Mod] Betel Skyblock check skipped outside overworld");
			return;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		boolean isPreset = isBetelSkyblockPreset(level);
		boolean isOneBlockPreset = isBetelOneBlockSkyblockPreset(level);
		boolean classicEnabled = isPreset && BetelNutMidnightConfig.enableClassicSkyblock;
		boolean oneBlockEnabled = isOneBlockPreset && BetelNutMidnightConfig.enableOneBlockSkyblock;
		BlockPos configuredIslandCenter = getConfiguredIslandCenter();
		BlockPos configuredOneBlockCenter = getConfiguredOneBlockCenter();
		BlockPos islandPosition = worldData.hasGeneratedBetelSkyIsland()
				? worldData.getIslandCenter()
				: configuredIslandCenter;
		BlockPos oneBlockPosition = worldData.hasGeneratedBetelOneBlockIsland()
				? worldData.getOneBlockCenter()
				: configuredOneBlockCenter;
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Is Betel Skyblock preset: {}", isPreset);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Is Betel One Block Skyblock preset: {}", isOneBlockPreset);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Classic Betel Skyblock enabled: {}", classicEnabled);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Betel One Block Skyblock enabled: {}", oneBlockEnabled);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Has generated island: {}",
				worldData.hasGeneratedBetelSkyIsland());
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Island position: x={}, y={}, z={}",
				islandPosition.getX(), islandPosition.getY(), islandPosition.getZ());
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Has generated one-block island: {}",
				worldData.hasGeneratedBetelOneBlockIsland());
		BetelNutMod.LOGGER.info("[Betel Nut Mod] One-block island position: x={}, y={}, z={}",
				oneBlockPosition.getX(), oneBlockPosition.getY(), oneBlockPosition.getZ());

		if (isOneBlockPreset) {
			if (!BetelNutMidnightConfig.enableOneBlockSkyblock) {
				BetelNutMod.LOGGER.info(
						"[Betel Nut Mod] One block skyblock preset detected, but one block skyblock generation is disabled by config.");
				return;
			}
			generateOneBlockIslandIfNeeded(level, worldData);
			return;
		}

		if (!isPreset) {
			BetelNutMod.LOGGER.info("[Betel Nut Mod] Betel Skyblock disabled because current world is not a skyblock preset.");
			return;
		}

		if (!BetelNutMidnightConfig.enableClassicSkyblock) {
			BetelNutMod.LOGGER.info(
					"[Betel Nut Mod] Classic skyblock preset detected, but classic skyblock generation is disabled by config.");
			return;
		}

		if (worldData.hasGeneratedBetelSkyIsland()) {
			if (!worldData.hasSetSkyblockSpawn()) {
				setSkyblockSpawn(level, worldData, false);
			}
			BetelNutMod.LOGGER.info(
					"[Betel Nut Mod] Classic Betel Skyblock already generated, skipping generation.");
			return;
		}

		if (worldData.isBetelSkyIslandResetPending()) {
			BetelNutMod.LOGGER.info(
					"[Betel Nut Mod] Classic Betel Skyblock generation was reset; waiting for /betelskyblock generate instead of auto-generating.");
			return;
		}

		if (isClassicTargetAreaOccupied(level, configuredIslandCenter)) {
			BetelNutMod.LOGGER.warn(
					"[Betel Nut Mod] Classic Betel Skyblock target area already has blocks; startup generation cancelled to avoid overwriting player builds.");
			return;
		}

		generateInitialIsland(level, worldData, false);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Classic Betel Skyblock generated for the first time.");
	}

	private static void generateOneBlockIslandIfNeeded(ServerLevel level, BetelSkyblockWorldComponent worldData) {
		BlockPos configuredOneBlockCenter = getConfiguredOneBlockCenter();
		if (worldData.hasGeneratedBetelOneBlockIsland()) {
			if (!worldData.hasSetBetelOneBlockSpawn()) {
				setBetelOneBlockSpawn(level, worldData, false);
			}
			BetelNutMod.LOGGER.info(
					"[Betel Nut Mod] Betel One Block Skyblock already generated, skipping generation.");
			return;
		}

		if (worldData.isBetelOneBlockIslandResetPending()) {
			BetelNutMod.LOGGER.info(
					"[Betel Nut Mod] Betel One Block Skyblock generation was reset; waiting for /betelskyblock generate_one_block instead of auto-generating.");
			return;
		}

		if (isOneBlockTargetAreaOccupied(level, configuredOneBlockCenter)) {
			BetelNutMod.LOGGER.warn(
					"[Betel Nut Mod] Betel One Block Skyblock target area already has blocks; startup generation cancelled to avoid overwriting player builds.");
			return;
		}

		generateOneBlockIsland(level, worldData, false);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Betel One Block Skyblock generated for the first time.");
	}

	private static void generateInitialIsland(ServerLevel level, BetelSkyblockWorldComponent worldData,
			boolean forced) {
		BlockPos islandCenter = getConfiguredIslandCenter();
		BetelNutMod.LOGGER.info("[Betel Nut Mod] generating island at x={}, y={}, z={}{}",
				islandCenter.getX(), islandCenter.getY(), islandCenter.getZ(), forced ? " (forced)" : "");

		int setBlockCalls = 0;
		setBlockCalls += clearBuildArea(level, islandCenter);
		setBlockCalls += buildResidueIslandTerrain(level, islandCenter);
		setBlockCalls += placeVanillaStarterContents(level, islandCenter);
		setBlockCalls += placeOptionalBetelContents(level, islandCenter);

		worldData.markGenerated(islandCenter);
		setBlockCalls += setSkyblockSpawn(level, worldData, true);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] island generation finished; setBlockState calls = {}",
				setBlockCalls);
	}

	private static void generateOneBlockIsland(ServerLevel level, BetelSkyblockWorldComponent worldData,
			boolean forced) {
		BlockPos oneBlockCenter = getConfiguredOneBlockCenter();
		BetelNutMod.LOGGER.info("[Betel Nut Mod] generating one-block island at x={}, y={}, z={}{}",
				oneBlockCenter.getX(), oneBlockCenter.getY(), oneBlockCenter.getZ(),
				forced ? " (forced)" : "");

		int setBlockCalls = 0;
		setBlockCalls += setBlockState(level, oneBlockCenter,
				ModBlocks.BETEL_NUT_RESIDUE_BLOCK.defaultBlockState());

		worldData.markOneBlockGenerated(oneBlockCenter);
		setBlockCalls += setBetelOneBlockSpawn(level, worldData, true);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] one-block island generation finished; setBlockState calls = {}",
				setBlockCalls);
	}

	private static int setSkyblockSpawn(ServerLevel level, BetelSkyblockWorldComponent worldData,
			boolean clearSpawnBlocks) {
		BlockPos spawnPos = getSkyblockSpawn(worldData);
		int setBlockCalls = clearSpawnBlocks ? clearSpawnSpace(level, spawnPos) : 0;
		level.setDefaultSpawnPos(spawnPos, 0.0F);
		worldData.markSkyblockSpawnSet();
		BetelNutMod.LOGGER.info("[Betel Nut Mod] world spawn set to x={}, y={}, z={}{}",
				spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(),
				clearSpawnBlocks ? "" : " without clearing blocks");
		return setBlockCalls;
	}

	private static int setBetelOneBlockSpawn(ServerLevel level, BetelSkyblockWorldComponent worldData,
			boolean clearSpawnBlocks) {
		BlockPos spawnPos = getOneBlockSpawn(worldData);
		int setBlockCalls = clearSpawnBlocks ? clearSpawnSpace(level, spawnPos) : 0;
		level.setDefaultSpawnPos(spawnPos, 0.0F);
		worldData.markBetelOneBlockSpawnSet();
		BetelNutMod.LOGGER.info("[Betel Nut Mod] one-block world spawn set to x={}, y={}, z={}{}",
				spawnPos.getX(), spawnPos.getY(), spawnPos.getZ(),
				clearSpawnBlocks ? "" : " without clearing blocks");
		return setBlockCalls;
	}

	private static int clearBuildArea(ServerLevel level, BlockPos center) {
		int setBlockCalls = 0;
		for (int x = -5; x <= 5; x++) {
			for (int y = -3; y <= 10; y++) {
				for (int z = -5; z <= 5; z++) {
					BlockPos pos = center.offset(x, y, z);
					setBlockCalls += setBlockState(level, pos, Blocks.AIR.defaultBlockState());
				}
			}
		}
		return setBlockCalls;
	}

	private static int buildResidueIslandTerrain(ServerLevel level, BlockPos center) {
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Generating classic Betel Skyblock with residue block terrain.");
		int setBlockCalls = 0;
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				if (!isInsideIslandTop(x, z)) {
					continue;
				}

				BlockPos topPos = center.offset(x, 0, z);
				setBlockCalls += setBlockState(level, topPos, residueTerrainState());
				setBlockCalls += setBlockState(level, topPos.below(), residueTerrainState());

				int depth = hangingDepth(x, z);
				for (int y = 2; y <= depth; y++) {
					setBlockCalls += setBlockState(level, topPos.below(y), residueTerrainState());
				}
			}
		}

		BlockPos grassPos = classicGrassBlockPos(center);
		BlockPos dirtPos = classicDirtBlockPos(center);
		setBlockCalls += setBlockState(level, grassPos, Blocks.GRASS_BLOCK.defaultBlockState());
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Placed one grass block at x={}, y={}, z={}",
				grassPos.getX(), grassPos.getY(), grassPos.getZ());
		setBlockCalls += setBlockState(level, dirtPos, Blocks.DIRT.defaultBlockState());
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Placed one dirt block at x={}, y={}, z={}",
				dirtPos.getX(), dirtPos.getY(), dirtPos.getZ());
		return setBlockCalls;
	}

	private static int placeVanillaStarterContents(ServerLevel level, BlockPos center) {
		int setBlockCalls = 0;
		BlockPos composterPos = center.offset(-2, 1, 0);
		BlockPos campfirePos = center.offset(0, 1, 2);

		setBlockCalls += setBlockState(level, composterPos, Blocks.COMPOSTER.defaultBlockState());
		setBlockCalls += setBlockState(level, campfirePos, Blocks.CAMPFIRE.defaultBlockState());
		if (!BetelNutMidnightConfig.classicGenerateStarterChest) {
			return setBlockCalls;
		}

		BlockPos chestPos = center.offset(2, 1, 0);
		setBlockCalls += setBlockState(level, chestPos,
				Blocks.CHEST.defaultBlockState().setValue(ChestBlock.FACING, Direction.WEST));
		fillStarterChest(level, chestPos);
		return setBlockCalls;
	}

	private static int placeOptionalBetelContents(ServerLevel level, BlockPos center) {
		int setBlockCalls = 0;
		try {
			setBlockCalls += setBlockState(level, classicGrassBlockPos(center).above(),
					ModBlocks.BETEL_PALM_SAPLING.defaultBlockState());
			setBlockCalls += setBlockState(level, center.offset(-3, 0, 0),
					residueTerrainState());
			setBlockCalls += setBlockState(level, center.offset(3, 0, -1),
					residueTerrainState());
			setBlockCalls += setBlockState(level, center.offset(1, 0, 3),
					residueTerrainState());
			setBlockCalls += setBlockState(level, center.offset(-1, 0, -3),
					residueTerrainState());
		} catch (RuntimeException exception) {
			BetelNutMod.LOGGER.warn(
					"[Betel Nut Mod] Optional betel blocks failed to place; vanilla sky island generation remains intact.",
					exception);
		}
		return setBlockCalls;
	}

	private static void fillStarterChest(ServerLevel level, BlockPos chestPos) {
		try {
			if (!(level.getBlockEntity(chestPos) instanceof ChestBlockEntity chest)) {
				BetelNutMod.LOGGER.warn("[Betel Nut Mod] Betel Skyblock starter chest block entity missing at {}.",
						chestPos);
				return;
			}

			for (int slot = 0; slot < chest.getContainerSize(); slot++) {
				chest.setItem(slot, ItemStack.EMPTY);
			}

			try {
				chest.setItem(0, new ItemStack(ModItems.RAW_BETEL_NUT, 3));
				chest.setItem(1, new ItemStack(ModItems.ROASTED_BETEL_NUT, 1));
				chest.setItem(2, new ItemStack(ModBlocks.BETEL_PALM_SAPLING_ITEM, 1));
				chest.setItem(3, new ItemStack(ModItems.BETEL_NUT_RESIDUE, 2));
			} catch (RuntimeException exception) {
				BetelNutMod.LOGGER.warn(
						"[Betel Nut Mod] Optional betel starter chest items failed to fill; vanilla chest items remain.",
						exception);
			}

			chest.setItem(4, new ItemStack(Items.BONE_MEAL, 1));
			chest.setItem(5, new ItemStack(Items.STICK, 4));
			chest.setItem(6, new ItemStack(Items.ICE, 1));
			chest.setItem(7, new ItemStack(Items.LAVA_BUCKET, 1));
			if (BetelNutMidnightConfig.classicGiveGuideBook) {
				chest.setItem(8, createGuideBook());
			}
		} catch (RuntimeException exception) {
			BetelNutMod.LOGGER.warn(
					"[Betel Nut Mod] Starter chest fill failed; island terrain generation remains intact.",
					exception);
		}
	}

	private static ItemStack createGuideBook() {
		ItemStack book = new ItemStack(Items.WRITTEN_BOOK);
		book.set(DataComponents.WRITTEN_BOOK_CONTENT, new WrittenBookContent(
				Filterable.passThrough("\u69df\u6994\u7a7a\u5c9b\u6307\u5357"),
				"Betel Nut Mod",
				0,
				List.of(
						guidePage("book.betel-nut-mod.skyblock_guide.page1"),
						guidePage("book.betel-nut-mod.skyblock_guide.page2"),
						guidePage("book.betel-nut-mod.skyblock_guide.page3"),
						guidePage("book.betel-nut-mod.skyblock_guide.page4"),
						guidePage("book.betel-nut-mod.skyblock_guide.page5")),
				true));
		return book;
	}

	private static Filterable<Component> guidePage(String translationKey) {
		return Filterable.passThrough(Component.translatable(translationKey));
	}

	private static int clearSpawnSpace(ServerLevel level, BlockPos spawnPos) {
		int setBlockCalls = 0;
		for (int x = -SPAWN_CLEAR_RADIUS; x <= SPAWN_CLEAR_RADIUS; x++) {
			for (int z = -SPAWN_CLEAR_RADIUS; z <= SPAWN_CLEAR_RADIUS; z++) {
				for (int y = 0; y <= 2; y++) {
					setBlockCalls += setBlockState(level, spawnPos.offset(x, y, z), Blocks.AIR.defaultBlockState());
				}
			}
		}
		return setBlockCalls;
	}

	private static boolean isInsideIslandTop(int x, int z) {
		double ellipse = (x * x) / 16.0D + (z * z) / 12.25D;
		if (ellipse <= 1.0D) {
			return true;
		}
		return (x == -3 && z == 2) || (x == 3 && z == -1) || (x == 2 && z == 3);
	}

	private static int hangingDepth(int x, int z) {
		int distance = Math.abs(x) + Math.abs(z);
		if (distance <= 1) {
			return 5;
		}
		if (distance <= 4) {
			return 3 + Math.floorMod(x * 31 + z * 17, 2);
		}
		return 2 + Math.floorMod(x * 13 + z * 7, 2);
	}

	private static BlockPos classicGrassBlockPos(BlockPos center) {
		return center;
	}

	private static BlockPos classicDirtBlockPos(BlockPos center) {
		return center.east();
	}

	private static BlockState residueTerrainState() {
		return ModBlocks.BETEL_NUT_RESIDUE_BLOCK.defaultBlockState();
	}

	private static boolean isClassicTargetAreaOccupied(ServerLevel level, BlockPos center) {
		return hasNonAirBlockInArea(level, center,
				CLASSIC_PROTECTION_XZ_RADIUS,
				CLASSIC_PROTECTION_MIN_Y_OFFSET,
				CLASSIC_PROTECTION_MAX_Y_OFFSET);
	}

	private static boolean isOneBlockTargetAreaOccupied(ServerLevel level, BlockPos center) {
		return hasNonAirBlockInArea(level, center,
				ONE_BLOCK_PROTECTION_XZ_RADIUS,
				ONE_BLOCK_PROTECTION_MIN_Y_OFFSET,
				ONE_BLOCK_PROTECTION_MAX_Y_OFFSET);
	}

	private static boolean hasNonAirBlockInArea(ServerLevel level, BlockPos center, int xzRadius,
			int minYOffset, int maxYOffset) {
		for (int x = -xzRadius; x <= xzRadius; x++) {
			for (int y = minYOffset; y <= maxYOffset; y++) {
				for (int z = -xzRadius; z <= xzRadius; z++) {
					BlockPos pos = center.offset(x, y, z);
					if (!canWriteAt(level, pos)) {
						continue;
					}
					if (!level.getBlockState(pos).isAir()) {
						BetelNutMod.LOGGER.warn(
								"[Betel Nut Mod] Skyblock target area occupied at x={}, y={}, z={}",
								pos.getX(), pos.getY(), pos.getZ());
						return true;
					}
				}
			}
		}
		return false;
	}

	private static int setBlockState(ServerLevel level, BlockPos pos, BlockState state) {
		if (!canWriteAt(level, pos)) {
			BetelNutMod.LOGGER.warn("[Betel Nut Mod] Skipped skyblock block outside writable area: {}", pos);
			return 0;
		}

		level.setBlock(pos, state, Block.UPDATE_ALL);
		return 1;
	}

	private static boolean canWriteAt(ServerLevel level, BlockPos pos) {
		return !level.isOutsideBuildHeight(pos) && level.getWorldBorder().isWithinBounds(pos);
	}

	private static FlatLevelSource getOverworldFlatLevelSource(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return null;
		}
		if (level.getChunkSource().getGenerator() instanceof FlatLevelSource flatLevelSource) {
			return flatLevelSource;
		}
		return null;
	}

	private static boolean usesTheVoidBiome(FlatLevelSource flatLevelSource) {
		return flatLevelSource.settings().getBiome().is(Biomes.THE_VOID);
	}

	private static boolean hasNoFlatLayers(FlatLevelSource flatLevelSource) {
		return flatLevelSource.settings().getLayersInfo().isEmpty()
				&& flatLevelSource.settings().getLayers().isEmpty();
	}

	private static boolean hasOneAirMarkerLayer(FlatLevelSource flatLevelSource) {
		List<FlatLayerInfo> layers = flatLevelSource.settings().getLayersInfo();
		return layers.size() == 1
				&& layers.get(0).getHeight() == 1
				&& layers.get(0).getBlockState().is(Blocks.AIR);
	}

	private static String dimensionLabel(ServerLevel level) {
		if (level.dimension() == Level.OVERWORLD) {
			return "overworld";
		}
		return level.dimension().location().toString();
	}

	private BetelSkyblockManager() {
	}
}
