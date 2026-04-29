package betel.nut.skyblock;

import java.util.List;
import java.util.Set;

import betel.nut.BetelNutConfig;
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
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.phys.Vec3;

public final class BetelSkyblockManager {
	private static final boolean REPAIR_MISSING_OR_MOVED_ISLAND = true;
	private static final BlockPos ISLAND_CENTER = new BlockPos(0, 128, 0);
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
		return isBetelSkyblockPreset(level) || isConfigEnabled();
	}

	public static boolean isBetelSkyblockPreset(ServerLevel level) {
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}

		if (!(level.getChunkSource().getGenerator() instanceof FlatLevelSource flatLevelSource)) {
			return false;
		}

		return flatLevelSource.settings().getLayersInfo().isEmpty()
				&& flatLevelSource.settings().getLayers().isEmpty();
	}

	public static BlockPos getIslandCenter() {
		return ISLAND_CENTER;
	}

	public static BlockPos getSkyblockSpawn() {
		return getSkyblockSpawnForCenter(ISLAND_CENTER);
	}

	public static BlockPos getSkyblockSpawn(ServerLevel level) {
		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		return getSkyblockSpawn(worldData);
	}

	private static BlockPos getSkyblockSpawn(BetelSkyblockWorldComponent worldData) {
		return getSkyblockSpawnForCenter(worldData.getIslandCenter());
	}

	private static BlockPos getSkyblockSpawnForCenter(BlockPos islandCenter) {
		return islandCenter.above(4);
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

	public static void resetGenerationState(ServerLevel level) {
		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		worldData.resetBetelSkyIslandGeneration();
		BetelNutMod.LOGGER.info(
				"[Betel Nut Mod] hasGeneratedBetelSkyIsland reset to false; next startup check or /betelskyblock generate can create the island again.");
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

		if (!isEnabled(overworld)) {
			return;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
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

	private static void handleVoidProtection(ServerPlayer player, ServerLevel overworld,
			BetelSkyblockWorldComponent worldData) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!config.enableBetelSkyblockVoidProtection || player.getY() >= config.betelSkyblockVoidProtectionMinY) {
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

	private static void generateInitialIslandIfNeeded(ServerLevel level) {
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Checking Betel Skyblock world...");
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Current dimension: {}", dimensionLabel(level));

		if (level.dimension() != Level.OVERWORLD) {
			BetelNutMod.LOGGER.info("[Betel Nut Mod] Betel Skyblock check skipped outside overworld");
			return;
		}

		BetelSkyblockWorldComponent worldData = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
		boolean isPreset = isBetelSkyblockPreset(level);
		boolean configEnabled = isConfigEnabled();
		boolean enabled = isPreset || configEnabled;
		BlockPos islandPosition = worldData.hasGeneratedBetelSkyIsland()
				? worldData.getIslandCenter()
				: ISLAND_CENTER;
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Is Betel Skyblock preset: {}", isPreset);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Betel Skyblock config fallback enabled: {}", configEnabled);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Betel Skyblock enabled: {}", enabled);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Has generated island: {}",
				worldData.hasGeneratedBetelSkyIsland());
		BetelNutMod.LOGGER.info("[Betel Nut Mod] Island position: x={}, y={}, z={}",
				islandPosition.getX(), islandPosition.getY(), islandPosition.getZ());

		if (!enabled) {
			BetelNutMod.LOGGER.info(
					"[Betel Nut Mod] Betel Skyblock disabled because current world is not Betel Skyblock preset and config betelSkyblockEnabled is false.");
			return;
		}

		if (worldData.hasGeneratedBetelSkyIsland()) {
			BlockPos recordedCenter = worldData.getIslandCenter();
			boolean centerMatches = ISLAND_CENTER.equals(recordedCenter);
			boolean islandCorePresent = isIslandCorePresent(level, ISLAND_CENTER);
			BetelNutMod.LOGGER.info("[Betel Nut Mod] recorded island center = {}", recordedCenter);
			BetelNutMod.LOGGER.info("[Betel Nut Mod] fixed island center expected = {}", ISLAND_CENTER);
			BetelNutMod.LOGGER.info("[Betel Nut Mod] island core present at fixed center = {}", islandCorePresent);

			if (centerMatches && islandCorePresent) {
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
					"[Betel Nut Mod] CCA says the sky island was generated, but the fixed island is missing or moved; regenerating at x={}, y={}, z={}",
					ISLAND_CENTER.getX(), ISLAND_CENTER.getY(), ISLAND_CENTER.getZ());
		}

		generateInitialIsland(level, worldData, false);
	}

	private static void generateInitialIsland(ServerLevel level, BetelSkyblockWorldComponent worldData,
			boolean forced) {
		BetelNutMod.LOGGER.info("[Betel Nut Mod] generating island at x={}, y={}, z={}{}",
				ISLAND_CENTER.getX(), ISLAND_CENTER.getY(), ISLAND_CENTER.getZ(), forced ? " (forced)" : "");

		int setBlockCalls = 0;
		setBlockCalls += clearBuildArea(level, ISLAND_CENTER);
		setBlockCalls += buildVanillaIslandTerrain(level, ISLAND_CENTER);
		setBlockCalls += placeVanillaStarterContents(level, ISLAND_CENTER);
		setBlockCalls += placeOptionalBetelContents(level, ISLAND_CENTER);

		worldData.markGenerated(ISLAND_CENTER);
		setBlockCalls += setSkyblockSpawn(level, worldData);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] island generation finished; setBlockState calls = {}",
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
		BlockPos chestPos = center.offset(2, 1, 0);

		setBlockCalls += setBlockState(level, composterPos, Blocks.COMPOSTER.defaultBlockState());
		setBlockCalls += setBlockState(level, campfirePos, Blocks.CAMPFIRE.defaultBlockState());
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
			chest.setItem(8, createGuideBook());
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

	private static boolean isIslandCorePresent(ServerLevel level, BlockPos center) {
		BlockState centerState = level.getBlockState(center);
		BlockState belowState = level.getBlockState(center.below());
		BlockState composterState = level.getBlockState(center.offset(-2, 1, 0));
		BlockState campfireState = level.getBlockState(center.offset(0, 1, 2));
		BlockState chestState = level.getBlockState(center.offset(2, 1, 0));
		return centerState.is(Blocks.GRASS_BLOCK)
				&& (belowState.is(Blocks.DIRT) || belowState.is(Blocks.STONE))
				&& composterState.is(Blocks.COMPOSTER)
				&& campfireState.is(Blocks.CAMPFIRE)
				&& chestState.is(Blocks.CHEST);
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
