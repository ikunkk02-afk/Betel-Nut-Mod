package betel.nut.skyblock;

import java.util.List;
import java.util.Set;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMidnightConfig;
import betel.nut.BetelNutMod;
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
	private static final boolean REPAIR_MISSING_OR_MOVED_ISLAND = true;
	private static final int CENTER_X = 0;
	private static final int CENTER_Z = 0;
	private static final int SPAWN_CLEAR_RADIUS = 2;
	private static final int STARTUP_CHECK_DELAY_TICKS = 1;

	private static boolean startupCheckScheduled;
	private static boolean startupCheckFinished;
	private static int startupCheckDelay;

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
		if (player.level().dimension() != Level.OVERWORLD || !isEnabled(player.serverLevel())) {
			return;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(player.level());
		if (!worldData.hasGeneratedBetelSkyIsland()) {
			return;
		}

		BetelSkyblockPlayerComponent playerData = BetelNutEntityComponents.SKYBLOCK_PLAYER.get(player);
		if (!playerData.hasEatenFirstBetelNutInSkyblock()) {
			playerData.markEatenFirstBetelNutInSkyblock();
			player.sendSystemMessage(Component.literal(
					"\u4f60\u5b8c\u6210\u4e86\u69df\u6994\u7a7a\u5c9b\u7684\u7b2c\u4e00\u6b65\uff1a\u83b7\u5f97\u69df\u6994\u6e23\u3002"));
			player.giveExperiencePoints(1);
			player.playNotifySound(SoundEvents.EXPERIENCE_ORB_PICKUP, SoundSource.PLAYERS, 0.8F, 1.0F);
			BetelNutMod.LOGGER.info("Player {} ate their first betel nut in Betel Skyblock mode.",
					player.getScoreboardName());
		}
	}

	public static boolean generateForCommand(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		generateInitialIsland(level, worldData, true);
		return true;
	}

	public static boolean generateOneBlockForCommand(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		generateOneBlockIsland(level, worldData, true);
		return true;
	}

	public static void resetGenerationState(ServerLevel level) {
		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		worldData.resetBetelSkyIslandGeneration();
		BetelNutMod.LOGGER.info(
				"[Betel Nut Mod] hasGeneratedBetelSkyIsland reset to false; next startup check or /betelskyblock generate can create the island again.");
	}

	public static void resetOneBlockGenerationState(ServerLevel level) {
		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		worldData.resetBetelOneBlockIslandGeneration();
		BetelNutMod.LOGGER.info(
				"[Betel Nut Mod] hasGeneratedBetelOneBlockIsland reset to false; next startup check or /betelskyblock generate_one_block can create the one-block island again.");
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
			setSkyblockSpawn(level, worldData);
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
			setBetelOneBlockSpawn(level, worldData);
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
			setSkyblockSpawn(overworld, worldData);
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.level().dimension() != Level.OVERWORLD || !player.isAlive()) {
				continue;
			}
			handleFirstSkyblockJoin(player, overworld, worldData);
			handleVoidProtection(player, overworld, worldData);
		}
	}

	private static void handleOneBlockSkyblockPlayers(MinecraftServer server, ServerLevel overworld,
			BetelSkyblockWorldComponent worldData) {
		if (!worldData.hasGeneratedBetelOneBlockIsland()) {
			return;
		}

		if (!worldData.hasSetBetelOneBlockSpawn()) {
			setBetelOneBlockSpawn(overworld, worldData);
		}

		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			if (player.level().dimension() != Level.OVERWORLD || !player.isAlive()) {
				continue;
			}
			handleFirstOneBlockSkyblockJoin(player, overworld, worldData);
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
		player.sendSystemMessage(Component.literal(
				"\u4f60\u4ece\u865a\u7a7a\u4e2d\u88ab\u69df\u6994\u7a7a\u5c9b\u62c9\u4e86\u56de\u6765\u3002"));
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
		player.sendSystemMessage(Component.literal(
				"\u4f60\u4ece\u865a\u7a7a\u4e2d\u88ab\u69df\u6994\u4e00\u65b9\u5757\u7a7a\u5c9b\u62c9\u4e86\u56de\u6765\u3002"));
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
		player.sendSystemMessage(Component.literal(
				"\u6b22\u8fce\u6765\u5230\u69df\u6994\u4e00\u65b9\u5757\u7a7a\u5c9b\u3002\n"
						+ "\u4f60\u53ea\u6709\u4e00\u4e2a\u69df\u6994\u6e23\u5757\u3001\u4e00\u68f5\u69df\u6994\u6811\u82d7\u3001\u4e00\u5757\u6ce5\u571f\u548c\u4e00\u4e2a\u69df\u6994\u3002\n"
						+ "\u5403\u4e0b\u69df\u6994\u53ef\u4ee5\u83b7\u5f97\u69df\u6994\u6e23\u3002\n"
						+ "\u63a5\u4e0b\u6765\uff0c\u5c31\u770b\u4f60\u7684\u9020\u5316\u4e86\u3002\n"
						+ "\u672c\u6a21\u7ec4\u5185\u5bb9\u53ea\u662f\u6e38\u620f\u673a\u5236\u8bbe\u8ba1\uff0c"
						+ "\u4e0d\u4ee3\u8868\u73b0\u5b9e\u5065\u5eb7\u5efa\u8bae\uff0c\u4e5f\u4e0d\u9f13\u52b1\u73b0\u5b9e\u4e2d\u98df\u7528\u69df\u6994\u3002"));
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
			BlockPos recordedCenter = worldData.getIslandCenter();
			boolean islandCorePresent = isIslandCorePresent(level, recordedCenter);
			BetelNutMod.LOGGER.info("[Betel Nut Mod] recorded island center = {}", recordedCenter);
			BetelNutMod.LOGGER.info("[Betel Nut Mod] configured island center = {}", configuredIslandCenter);
			BetelNutMod.LOGGER.info("[Betel Nut Mod] island core present at recorded center = {}", islandCorePresent);

			if (islandCorePresent) {
				if (!worldData.hasSetSkyblockSpawn()) {
					setSkyblockSpawn(level, worldData);
				}
				BetelNutMod.LOGGER.info(
						"[Betel Nut Mod] Betel Skyblock already generated; startup generation skipped");
				return;
			}

			if (!REPAIR_MISSING_OR_MOVED_ISLAND) {
				BetelNutMod.LOGGER.warn(
						"[Betel Nut Mod] CCA says the sky island was generated, but the fixed island check failed; use /betelskyblock reset then /betelskyblock generate to repair it.");
				return;
			}

			BetelNutMod.LOGGER.warn(
					"[Betel Nut Mod] CCA says the sky island was generated, but the recorded island is missing; regenerating at x={}, y={}, z={}",
					configuredIslandCenter.getX(), configuredIslandCenter.getY(), configuredIslandCenter.getZ());
		}

		generateInitialIsland(level, worldData, false);
	}

	private static void generateOneBlockIslandIfNeeded(ServerLevel level, BetelSkyblockWorldComponent worldData) {
		BlockPos configuredOneBlockCenter = getConfiguredOneBlockCenter();
		if (worldData.hasGeneratedBetelOneBlockIsland()) {
			BlockPos recordedCenter = worldData.getOneBlockCenter();
			boolean oneBlockCorePresent = isOneBlockCorePresent(level, recordedCenter);
			BetelNutMod.LOGGER.info("[Betel Nut Mod] recorded one-block center = {}", recordedCenter);
			BetelNutMod.LOGGER.info("[Betel Nut Mod] configured one-block center = {}", configuredOneBlockCenter);
			BetelNutMod.LOGGER.info("[Betel Nut Mod] one-block core present at recorded center = {}",
					oneBlockCorePresent);

			if (oneBlockCorePresent) {
				if (!worldData.hasSetBetelOneBlockSpawn()) {
					setBetelOneBlockSpawn(level, worldData);
				}
				BetelNutMod.LOGGER.info(
						"[Betel Nut Mod] Betel One Block Skyblock already generated; startup generation skipped");
				return;
			}

			if (!REPAIR_MISSING_OR_MOVED_ISLAND) {
				BetelNutMod.LOGGER.warn(
						"[Betel Nut Mod] CCA says the one-block island was generated, but the fixed block check failed; use /betelskyblock reset_one_block then /betelskyblock generate_one_block to repair it.");
				return;
			}

			BetelNutMod.LOGGER.warn(
					"[Betel Nut Mod] CCA says the one-block island was generated, but the recorded block is missing; regenerating at x={}, y={}, z={}",
					configuredOneBlockCenter.getX(), configuredOneBlockCenter.getY(), configuredOneBlockCenter.getZ());
		}

		generateOneBlockIsland(level, worldData, false);
	}

	private static void generateInitialIsland(ServerLevel level, BetelSkyblockWorldComponent worldData,
			boolean forced) {
		BlockPos islandCenter = getConfiguredIslandCenter();
		BetelNutMod.LOGGER.info("[Betel Nut Mod] generating island at x={}, y={}, z={}{}",
				islandCenter.getX(), islandCenter.getY(), islandCenter.getZ(), forced ? " (forced)" : "");

		int setBlockCalls = 0;
		setBlockCalls += clearBuildArea(level, islandCenter);
		setBlockCalls += buildVanillaIslandTerrain(level, islandCenter);
		setBlockCalls += placeVanillaStarterContents(level, islandCenter);
		setBlockCalls += placeOptionalBetelContents(level, islandCenter);

		worldData.markGenerated(islandCenter);
		setBlockCalls += setSkyblockSpawn(level, worldData);
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
		setBlockCalls += setBetelOneBlockSpawn(level, worldData);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] one-block island generation finished; setBlockState calls = {}",
				setBlockCalls);
	}

	private static int setSkyblockSpawn(ServerLevel level, BetelSkyblockWorldComponent worldData) {
		BlockPos spawnPos = getSkyblockSpawn(worldData);
		int setBlockCalls = clearSpawnSpace(level, spawnPos);
		level.setDefaultSpawnPos(spawnPos, 0.0F);
		worldData.markSkyblockSpawnSet();
		BetelNutMod.LOGGER.info("[Betel Nut Mod] world spawn set to x={}, y={}, z={}",
				spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
		return setBlockCalls;
	}

	private static int setBetelOneBlockSpawn(ServerLevel level, BetelSkyblockWorldComponent worldData) {
		BlockPos spawnPos = getOneBlockSpawn(worldData);
		int setBlockCalls = clearSpawnSpace(level, spawnPos);
		level.setDefaultSpawnPos(spawnPos, 0.0F);
		worldData.markBetelOneBlockSpawnSet();
		BetelNutMod.LOGGER.info("[Betel Nut Mod] one-block world spawn set to x={}, y={}, z={}",
				spawnPos.getX(), spawnPos.getY(), spawnPos.getZ());
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

	private static int buildVanillaIslandTerrain(ServerLevel level, BlockPos center) {
		int setBlockCalls = 0;
		for (int x = -4; x <= 4; x++) {
			for (int z = -4; z <= 4; z++) {
				if (!isInsideIslandTop(x, z)) {
					continue;
				}

				BlockPos topPos = center.offset(x, 0, z);
				setBlockCalls += setBlockState(level, topPos, Blocks.GRASS_BLOCK.defaultBlockState());
				setBlockCalls += setBlockState(level, topPos.below(), Blocks.DIRT.defaultBlockState());

				int depth = hangingDepth(x, z);
				for (int y = 2; y <= depth; y++) {
					BlockState state = y == depth || Math.abs(x) + Math.abs(z) <= 3
							? Blocks.STONE.defaultBlockState()
							: Blocks.DIRT.defaultBlockState();
					setBlockCalls += setBlockState(level, topPos.below(y), state);
				}
			}
		}
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
			setBlockCalls += setBlockState(level, center.offset(0, 1, -2),
					ModBlocks.BETEL_PALM_SAPLING.defaultBlockState());
			setBlockCalls += setBlockState(level, center.offset(-3, 0, 0),
					ModBlocks.BETEL_NUT_RESIDUE_BLOCK.defaultBlockState());
			setBlockCalls += setBlockState(level, center.offset(3, 0, -1),
					ModBlocks.BETEL_NUT_RESIDUE_BLOCK.defaultBlockState());
			setBlockCalls += setBlockState(level, center.offset(1, 0, 3),
					ModBlocks.BETEL_NUT_RESIDUE_BLOCK.defaultBlockState());
			setBlockCalls += setBlockState(level, center.offset(-1, 0, -3),
					ModBlocks.BETEL_NUT_RESIDUE_BLOCK.defaultBlockState());
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
						guidePage("\u6b22\u8fce\u6765\u5230\u69df\u6994\u7a7a\u5c9b\u3002\n"
								+ "\u8fd9\u91cc\u8d44\u6e90\u6781\u5c11\uff0c\u4f60\u9700\u8981\u4f9d\u9760\u69df\u6994\u5efa\u7acb\u6700\u521d\u7684\u8d44\u6e90\u5faa\u73af\u3002"),
						guidePage("\u5403\u4e0b\u69df\u6994\u540e\uff0c\u4f60\u4f1a\u83b7\u5f97\u69df\u6994\u6e23\u3002\n"
								+ "\u69df\u6994\u6e23\u53ef\u4ee5\u653e\u5165\u5806\u80a5\u6876\uff0c\u7528\u6765\u83b7\u5f97\u9aa8\u7c89\u3002"),
						guidePage("\u9aa8\u7c89\u53ef\u4ee5\u50ac\u719f\u69df\u6994\u6811\u3002\n"
								+ "\u69df\u6994\u6811\u4f1a\u7ee7\u7eed\u4ea7\u51fa\u69df\u6994\u3002\n"
								+ "\u8fd9\u5c31\u662f\u7a7a\u5c9b\u524d\u671f\u6700\u91cd\u8981\u7684\u5faa\u73af\u3002"),
						guidePage("9 \u4e2a\u69df\u6994\u6e23\u53ef\u4ee5\u5408\u6210 1 \u4e2a\u69df\u6994\u6e23\u5757\u3002\n"
								+ "\u69df\u6994\u6e23\u5757\u53ef\u4ee5\u4f5c\u4e3a\u6269\u5c55\u7a7a\u5c9b\u7684\u57fa\u7840\u65b9\u5757\u3002"),
						guidePage("\u6ce8\u610f\uff1a\u672c\u6a21\u7ec4\u5185\u5bb9\u53ea\u662f\u6e38\u620f\u673a\u5236\u8bbe\u8ba1\uff0c"
								+ "\u4e0d\u4ee3\u8868\u73b0\u5b9e\u5065\u5eb7\u5efa\u8bae\uff0c\u4e5f\u4e0d\u9f13\u52b1\u73b0\u5b9e\u4e2d\u98df\u7528\u69df\u6994\u3002")),
				true));
		return book;
	}

	private static Filterable<Component> guidePage(String text) {
		return Filterable.passThrough(Component.literal(text));
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

	private static boolean isIslandCorePresent(ServerLevel level, BlockPos center) {
		BlockState centerState = level.getBlockState(center);
		BlockState belowState = level.getBlockState(center.below());
		BlockState composterState = level.getBlockState(center.offset(-2, 1, 0));
		BlockState campfireState = level.getBlockState(center.offset(0, 1, 2));
		return centerState.is(Blocks.GRASS_BLOCK)
				&& (belowState.is(Blocks.DIRT) || belowState.is(Blocks.STONE))
				&& composterState.is(Blocks.COMPOSTER)
				&& campfireState.is(Blocks.CAMPFIRE);
	}

	private static boolean isOneBlockCorePresent(ServerLevel level, BlockPos center) {
		return level.getBlockState(center).is(ModBlocks.BETEL_NUT_RESIDUE_BLOCK);
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
