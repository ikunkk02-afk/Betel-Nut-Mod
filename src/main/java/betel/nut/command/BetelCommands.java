package betel.nut.command;

import static net.minecraft.commands.Commands.argument;
import static net.minecraft.commands.Commands.literal;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMidnightConfig;
import betel.nut.BetelNutMod;
import betel.nut.component.BetelNutAddictionComponent;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.event.WithdrawalEatingRestrictions;
import betel.nut.event.WithdrawalEatingRestrictions.EatingRestrictionCheck;
import betel.nut.item.EnderBetelTeleportHandler;
import betel.nut.skyblock.BetelSkyblockManager;
import betel.nut.skyblock.BetelSkyblockManager.GenerationResult;
import betel.nut.component.BetelNutWorldComponents;
import betel.nut.component.BetelSkyblockWorldComponent;
import betel.nut.worldgen.BetelPalmTreeGenerator;
import com.mojang.brigadier.Command;
import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.EndPortalFrameBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BetelCommands {
	public static void register() {
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
			dispatcher.register(literal("betel")
					.requires(source -> source.hasPermission(2))
					.then(literal("addiction")
							.then(literal("get").executes(BetelCommands::getAddiction))
							.then(literal("set")
									.then(argument("value", IntegerArgumentType.integer(0))
											.executes(BetelCommands::setAddiction)
											.then(literal("reset_timer")
													.executes(BetelCommands::setAddictionAndResetTimer))))
							.then(literal("clear").executes(BetelCommands::clearAddiction))
							.then(literal("add")
									.then(argument("value", IntegerArgumentType.integer(0))
											.executes(BetelCommands::addAddiction))))
					.then(literal("withdrawal")
							.then(literal("set")
									.then(argument("value", IntegerArgumentType.integer(0))
											.executes(BetelCommands::setWithdrawal)))
							.then(literal("trigger").executes(BetelCommands::triggerWithdrawal)))
					.then(literal("ender")
							.then(literal("teleport").executes(BetelCommands::teleportEnderBetel)))
					.then(literal("tree")
							.then(literal("generate").executes(BetelCommands::generateTree)))
					.then(literal("skyblock")
							.then(literal("status").executes(BetelCommands::skyblockStatus))
							.then(literal("generate").executes(BetelCommands::generateSkyblock))
							.then(literal("force_generate").executes(BetelCommands::forceGenerateSkyblock))
							.then(literal("generate_one_block").executes(BetelCommands::generateOneBlockSkyblock))
							.then(literal("force_generate_one_block")
									.executes(BetelCommands::forceGenerateOneBlockSkyblock))
							.then(literal("reset").executes(BetelCommands::resetSkyblock))
							.then(literal("reset_one_block").executes(BetelCommands::resetOneBlockSkyblock))
							.then(literal("tp").executes(BetelCommands::teleportSkyblock))
							.then(literal("tp_one_block").executes(BetelCommands::teleportOneBlockSkyblock))
							.then(literal("generate_end_portal").executes(BetelCommands::generateEndPortal))
							.then(literal("end_portal").executes(BetelCommands::generateEndPortal)))
					.then(literal("trades")
							.then(literal("info").executes(BetelCommands::showTradeInfo)))
					.then(literal("eatingtest").executes(BetelCommands::eatingTest))
					.then(literal("reload").executes(BetelCommands::reloadConfig)));

			dispatcher.register(literal("betelskyblock")
					.requires(source -> source.hasPermission(2))
					.then(literal("status").executes(BetelCommands::skyblockStatus))
					.then(literal("generate").executes(BetelCommands::generateSkyblock))
					.then(literal("force_generate").executes(BetelCommands::forceGenerateSkyblock))
					.then(literal("generate_one_block").executes(BetelCommands::generateOneBlockSkyblock))
					.then(literal("force_generate_one_block")
							.executes(BetelCommands::forceGenerateOneBlockSkyblock))
					.then(literal("reset").executes(BetelCommands::resetSkyblock))
					.then(literal("reset_one_block").executes(BetelCommands::resetOneBlockSkyblock))
					.then(literal("tp").executes(BetelCommands::teleportSkyblock))
					.then(literal("tp_one_block").executes(BetelCommands::teleportOneBlockSkyblock))
					.then(literal("generate_end_portal").executes(BetelCommands::generateEndPortal))
					.then(literal("end_portal").executes(BetelCommands::generateEndPortal)));
		});

		BetelNutMod.LOGGER.info("Betel nut debug commands registered successfully");
	}

	private static Component t(String translationKey, Object... args) {
		return Component.translatable(translationKey, args);
	}

	private static void sendSuccess(CommandSourceStack source, String translationKey, boolean broadcast,
			Object... args) {
		source.sendSuccess(() -> t(translationKey, args), broadcast);
	}

	private static void sendFailure(CommandSourceStack source, String translationKey, Object... args) {
		source.sendFailure(t(translationKey, args));
	}

	private static void sendOverworldNotLoaded(CommandSourceStack source) {
		sendFailure(source, "command.betel-nut-mod.overworld_not_loaded");
	}

	private static int getAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		BetelNutConfig config = BetelNutConfig.get();
		long gameTime = player.level().getGameTime();
		long lastEatTime = addiction.getLastEatTime();
		long timeSinceLastEat = lastEatTime <= 0 ? 0 : Math.max(0, gameTime - lastEatTime);
		Component lastEatText = lastEatTime <= 0
				? t("command.betel-nut-mod.common.none")
				: t("command.betel-nut-mod.addiction.last_eat", lastEatTime, timeSinceLastEat);
		long cleanRemainingTicks = Math.max(0, addiction.getCleanTime() - gameTime);
		Component withdrawalStartRemaining = withdrawalStartRemainingText(player, addiction, config);
		double maxHealthPenalty = addiction.getCurrentMaxHealthPenalty(player);
		boolean hasMaxHealthPenalty = addiction.hasWithdrawalMaxHealthPenalty(player);
		ItemStack mainHandStack = player.getMainHandItem();
		EatingRestrictionCheck eatingCheck = WithdrawalEatingRestrictions.evaluate(player, addiction, mainHandStack);
		boolean eatingRestrictionEnabled = WithdrawalEatingRestrictions.isFeatureEnabled(config);
		String mainHandName = mainHandStack.isEmpty() ? "empty" : mainHandStack.getHoverName().getString();

		sendSuccess(context.getSource(), "command.betel-nut-mod.addiction.status", false,
				addiction.getAddictionValue(), addiction.getAddictionStage(), addiction.getWithdrawalValue(),
				addiction.getWithdrawalSeverity(), addiction.getNotifiedWithdrawalStage(), maxHealthPenalty,
				hasMaxHealthPenalty, lastEatText, timeSinceLastEat, withdrawalStartRemaining, cleanRemainingTicks,
				eatingRestrictionEnabled, eatingCheck.restrictionLevel().label(), mainHandName, eatingCheck.food(),
				eatingCheck.allowed(), eatingCheck.matchedAllowedTags(), eatingCheck.reason());
		return Command.SINGLE_SUCCESS;
	}

	private static int setAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.setAddictionValue(value);
		addiction.refreshWithdrawalEffects(player);

		sendSuccess(context.getSource(), "command.betel-nut-mod.addiction.set.success", true,
				addiction.getAddictionValue(), addiction.getAddictionStage());
		return Command.SINGLE_SUCCESS;
	}

	private static int setAddictionAndResetTimer(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.setAddictionValueAndResetLastEatTime(player, value);

		sendSuccess(context.getSource(), "command.betel-nut-mod.addiction.set_reset_timer.success", true,
				addiction.getAddictionValue(), addiction.getAddictionStage());
		return Command.SINGLE_SUCCESS;
	}

	private static int addAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.addAddictionValue(value);
		addiction.refreshWithdrawalEffects(player);

		sendSuccess(context.getSource(), "command.betel-nut-mod.addiction.add.success", true, value,
				addiction.getAddictionValue(), addiction.getAddictionStage());
		return Command.SINGLE_SUCCESS;
	}

	private static int clearAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutEntityComponents.ADDICTION.get(player).clearAddiction(player);

		sendSuccess(context.getSource(), "command.betel-nut-mod.addiction.clear.success", true);
		return Command.SINGLE_SUCCESS;
	}

	private static int setWithdrawal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.setWithdrawalValue(player, value);

		sendSuccess(context.getSource(), "command.betel-nut-mod.withdrawal.set.success", true,
				addiction.getWithdrawalValue(), addiction.getWithdrawalSeverity());
		return Command.SINGLE_SUCCESS;
	}

	private static int triggerWithdrawal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.triggerWithdrawalTest(player);

		sendSuccess(context.getSource(), "command.betel-nut-mod.withdrawal.trigger.success", true,
				addiction.getAddictionValue(), addiction.getAddictionStage(), addiction.getWithdrawalValue(),
				addiction.getWithdrawalSeverity());
		return Command.SINGLE_SUCCESS;
	}

	private static int teleportEnderBetel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		boolean teleported = EnderBetelTeleportHandler.tryTeleport(player, true);

		if (teleported) {
			sendSuccess(context.getSource(), "command.betel-nut-mod.ender_teleport.success", true);
			return Command.SINGLE_SUCCESS;
		}

		sendFailure(context.getSource(), "command.betel-nut-mod.ender_teleport.failure");
		return 0;
	}

	private static Component withdrawalStartRemainingText(ServerPlayer player, BetelNutAddictionComponent addiction,
			BetelNutConfig config) {
		if (!config.enableAddictionSystem) {
			return t("command.betel-nut-mod.withdrawal_start.disabled");
		}
		if (addiction.getAddictionValue() <= 0) {
			return t("command.betel-nut-mod.withdrawal_start.no_addiction");
		}
		if (addiction.getAddictionValue() < config.minimumAddictionForWithdrawal) {
			return t("command.betel-nut-mod.withdrawal_start.not_eligible",
					config.minimumAddictionForWithdrawal);
		}
		if (addiction.getLastEatTime() <= 0) {
			return t("command.betel-nut-mod.withdrawal_start.timer_not_started");
		}

		int remaining = addiction.getNextWithdrawalTicks(player);
		if (remaining < 0) {
			return t("command.betel-nut-mod.withdrawal_start.no_scheduled");
		}
		return t("command.betel-nut-mod.withdrawal_start.remaining", remaining);
	}

	private static int reloadConfig(CommandContext<CommandSourceStack> context) {
		boolean loaded = BetelNutConfig.reload();
		BetelNutMidnightConfig.reload();

		if (loaded) {
			sendSuccess(context.getSource(), "command.betel-nut-mod.reload.success", true);
		} else {
			sendFailure(context.getSource(), "command.betel-nut-mod.reload.failure");
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int showTradeInfo(CommandContext<CommandSourceStack> context) {
		BetelNutConfig config = BetelNutConfig.get();
		String status = config.enableFarmerTrades ? "enabled" : "disabled";

		sendSuccess(context.getSource(), "command.betel-nut-mod.trades.info", false, status,
				config.farmerTradeRawBetelBuyCount, config.farmerTradeRawBetelSellCount,
				config.farmerTradeLeafBuyCount, config.farmerTradeRoastedBetelBuyCount,
				config.farmerTradeFlavorEmeraldCost, config.farmerTradeSyntheticWorldEmeraldCost);
		return Command.SINGLE_SUCCESS;
	}

	private static int skyblockStatus(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			sendOverworldNotLoaded(context.getSource());
			return 0;
		}

		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		sendSuccess(context.getSource(), "command.betel-nut-mod.skyblock.status", false,
				BetelSkyblockManager.isEnabled(overworld), BetelSkyblockManager.isBetelSkyblockPreset(overworld),
				BetelSkyblockManager.isBetelOneBlockSkyblockPreset(overworld),
				BetelNutMidnightConfig.enableClassicSkyblock, BetelNutMidnightConfig.enableOneBlockSkyblock,
				BetelSkyblockManager.isConfigEnabled(), skyblock.hasGeneratedBetelSkyIsland(),
				skyblock.isBetelSkyIslandResetPending(), skyblock.getIslandCenter(),
				BetelSkyblockManager.getIslandCenter(), BetelSkyblockManager.getSkyblockSpawn(overworld),
				skyblock.hasSetSkyblockSpawn(), skyblock.hasGeneratedBetelOneBlockIsland(),
				skyblock.isBetelOneBlockIslandResetPending(), skyblock.getOneBlockCenter(),
				BetelSkyblockManager.getOneBlockCenter(), BetelSkyblockManager.getOneBlockSpawn(overworld),
				skyblock.hasSetBetelOneBlockSpawn());
		return Command.SINGLE_SUCCESS;
	}

	private static int generateSkyblock(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			sendOverworldNotLoaded(context.getSource());
			return 0;
		}

		GenerationResult result = BetelSkyblockManager.generateForCommand(overworld);
		if (result == GenerationResult.GENERATED) {
			boolean teleported = teleportSourcePlayerToSkyblockSpawn(context, overworld);
			sendSuccess(context.getSource(), "command.betel-nut-mod.skyblock.generate.success", true,
					BetelSkyblockManager.getIslandCenter(), BetelSkyblockManager.getSkyblockSpawn(overworld),
					teleported);
			return Command.SINGLE_SUCCESS;
		}

		sendClassicGenerationFailure(context, overworld, result);
		return 0;
	}

	private static int forceGenerateSkyblock(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			sendOverworldNotLoaded(context.getSource());
			return 0;
		}

		GenerationResult result = BetelSkyblockManager.forceGenerateForCommand(overworld);
		if (result == GenerationResult.GENERATED) {
			boolean teleported = teleportSourcePlayerToSkyblockSpawn(context, overworld);
			sendSuccess(context.getSource(), "command.betel-nut-mod.skyblock.force_generate.success", true,
					BetelSkyblockManager.getIslandCenter(), teleported);
			return Command.SINGLE_SUCCESS;
		}

		sendClassicGenerationFailure(context, overworld, result);
		return 0;
	}

	private static int generateOneBlockSkyblock(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			sendOverworldNotLoaded(context.getSource());
			return 0;
		}

		GenerationResult result = BetelSkyblockManager.generateOneBlockForCommand(overworld);
		if (result == GenerationResult.GENERATED) {
			boolean teleported = teleportSourcePlayerToOneBlockSpawn(context, overworld);
			sendSuccess(context.getSource(), "command.betel-nut-mod.one_block.generate.success", true,
					BetelSkyblockManager.getOneBlockCenter(), BetelSkyblockManager.getOneBlockSpawn(overworld),
					teleported);
			return Command.SINGLE_SUCCESS;
		}

		sendOneBlockGenerationFailure(context, overworld, result);
		return 0;
	}

	private static int forceGenerateOneBlockSkyblock(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			sendOverworldNotLoaded(context.getSource());
			return 0;
		}

		GenerationResult result = BetelSkyblockManager.forceGenerateOneBlockForCommand(overworld);
		if (result == GenerationResult.GENERATED) {
			boolean teleported = teleportSourcePlayerToOneBlockSpawn(context, overworld);
			sendSuccess(context.getSource(), "command.betel-nut-mod.one_block.force_generate.success", true,
					BetelSkyblockManager.getOneBlockCenter(), teleported);
			return Command.SINGLE_SUCCESS;
		}

		sendOneBlockGenerationFailure(context, overworld, result);
		return 0;
	}

	private static void sendClassicGenerationFailure(CommandContext<CommandSourceStack> context,
			ServerLevel overworld, GenerationResult result) {
		if (result == GenerationResult.ALREADY_GENERATED) {
			sendFailure(context.getSource(), "command.betel-nut-mod.skyblock.generate.already_generated");
			return;
		}
		if (result == GenerationResult.TARGET_OCCUPIED) {
			sendFailure(context.getSource(), "command.betel-nut-mod.skyblock.generate.target_occupied");
			return;
		}
		if (result == GenerationResult.NOT_OVERWORLD) {
			sendFailure(context.getSource(), "command.betel-nut-mod.skyblock.generate.not_overworld");
			return;
		}

		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		sendFailure(context.getSource(), "command.betel-nut-mod.skyblock.generate.failed",
				skyblock.hasGeneratedBetelSkyIsland(), skyblock.getIslandCenter());
	}

	private static void sendOneBlockGenerationFailure(CommandContext<CommandSourceStack> context,
			ServerLevel overworld, GenerationResult result) {
		if (result == GenerationResult.ALREADY_GENERATED) {
			sendFailure(context.getSource(), "command.betel-nut-mod.one_block.generate.already_generated");
			return;
		}
		if (result == GenerationResult.TARGET_OCCUPIED) {
			sendFailure(context.getSource(), "command.betel-nut-mod.one_block.generate.target_occupied");
			return;
		}
		if (result == GenerationResult.NOT_OVERWORLD) {
			sendFailure(context.getSource(), "command.betel-nut-mod.one_block.generate.not_overworld");
			return;
		}

		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		sendFailure(context.getSource(), "command.betel-nut-mod.one_block.generate.failed",
				skyblock.hasGeneratedBetelOneBlockIsland(), skyblock.getOneBlockCenter());
	}

	private static int resetSkyblock(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			sendOverworldNotLoaded(context.getSource());
			return 0;
		}

		BetelSkyblockManager.resetGenerationState(overworld);
		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		sendSuccess(context.getSource(), "command.betel-nut-mod.skyblock.reset.success", true,
				skyblock.hasGeneratedBetelSkyIsland(), skyblock.getIslandCenter(),
				skyblock.hasSetSkyblockSpawn());
		return Command.SINGLE_SUCCESS;
	}

	private static int resetOneBlockSkyblock(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			sendOverworldNotLoaded(context.getSource());
			return 0;
		}

		BetelSkyblockManager.resetOneBlockGenerationState(overworld);
		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		sendSuccess(context.getSource(), "command.betel-nut-mod.one_block.reset.success", true,
				skyblock.hasGeneratedBetelOneBlockIsland(), skyblock.getOneBlockCenter(),
				skyblock.hasSetBetelOneBlockSpawn());
		return Command.SINGLE_SUCCESS;
	}

	private static int teleportSkyblock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			sendOverworldNotLoaded(context.getSource());
			return 0;
		}

		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		if (!skyblock.hasGeneratedBetelSkyIsland()) {
			sendFailure(context.getSource(), "command.betel-nut-mod.skyblock.tp.missing");
			return 0;
		}

		boolean teleported = BetelSkyblockManager.teleportPlayerToSkyblockSpawn(player, overworld);
		if (teleported) {
			sendSuccess(context.getSource(), "command.betel-nut-mod.skyblock.tp.success", true);
			return Command.SINGLE_SUCCESS;
		}

		sendFailure(context.getSource(), "command.betel-nut-mod.skyblock.tp.failed");
		return 0;
	}

	private static int teleportOneBlockSkyblock(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			sendOverworldNotLoaded(context.getSource());
			return 0;
		}

		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		if (!skyblock.hasGeneratedBetelOneBlockIsland()) {
			sendFailure(context.getSource(), "command.betel-nut-mod.one_block.tp.missing");
			return 0;
		}

		boolean teleported = BetelSkyblockManager.teleportPlayerToOneBlockSpawn(player, overworld);
		if (teleported) {
			sendSuccess(context.getSource(), "command.betel-nut-mod.one_block.tp.success", true);
			return Command.SINGLE_SUCCESS;
		}

		sendFailure(context.getSource(), "command.betel-nut-mod.one_block.tp.failed");
		return 0;
	}

	private static int generateEndPortal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		if (!BetelNutMidnightConfig.allowEndPortalCommand) {
			sendFailure(context.getSource(), "command.betel-nut-mod.end_portal.disabled");
			return 0;
		}

		ServerPlayer player = context.getSource().getPlayerOrException();
		ServerLevel level = player.serverLevel();
		BlockPos center = player.blockPosition().relative(player.getDirection(), 3).below();

		generateEndPortalStructure(level, center);
		level.playSound(null, center, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);

		boolean skyblockPreset = BetelSkyblockManager.isBetelSkyblockPreset(level)
				|| BetelSkyblockManager.isBetelOneBlockSkyblockPreset(level);
		String translationKey = skyblockPreset
				? "command.betel-nut-mod.end_portal.success"
				: "command.betel-nut-mod.end_portal.success_with_hint";
		sendSuccess(context.getSource(), translationKey, true, center.getX(), center.getY(), center.getZ());
		BetelNutMod.LOGGER.info(
				"[Betel Nut Mod] Generated complete End Portal for player {} at x={}, y={}, z={} in {}.",
				player.getScoreboardName(), center.getX(), center.getY(), center.getZ(),
				level.dimension().location());
		return Command.SINGLE_SUCCESS;
	}

	private static void generateEndPortalStructure(ServerLevel level, BlockPos center) {
		for (int x = -2; x <= 2; x++) {
			for (int z = -2; z <= 2; z++) {
				for (int y = 0; y <= 2; y++) {
					level.setBlock(center.offset(x, y, z), Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
				}
			}
		}

		for (int x = -1; x <= 1; x++) {
			for (int z = -1; z <= 1; z++) {
				level.setBlock(center.offset(x, 0, z), Blocks.END_PORTAL.defaultBlockState(), Block.UPDATE_ALL);
			}
		}

		for (int x = -1; x <= 1; x++) {
			placeEndPortalFrame(level, center, x, -2, Direction.SOUTH);
			placeEndPortalFrame(level, center, x, 2, Direction.NORTH);
		}
		for (int z = -1; z <= 1; z++) {
			placeEndPortalFrame(level, center, -2, z, Direction.EAST);
			placeEndPortalFrame(level, center, 2, z, Direction.WEST);
		}
	}

	private static void placeEndPortalFrame(ServerLevel level, BlockPos center, int offsetX, int offsetZ,
			Direction facing) {
		BlockState frameState = Blocks.END_PORTAL_FRAME.defaultBlockState()
				.setValue(EndPortalFrameBlock.HAS_EYE, true)
				.setValue(EndPortalFrameBlock.FACING, facing);
		level.setBlock(center.offset(offsetX, 0, offsetZ), frameState, Block.UPDATE_ALL);
	}

	private static boolean teleportSourcePlayerToSkyblockSpawn(CommandContext<CommandSourceStack> context,
			ServerLevel overworld) {
		try {
			ServerPlayer player = context.getSource().getPlayerOrException();
			return BetelSkyblockManager.teleportPlayerToSkyblockSpawn(player, overworld);
		} catch (CommandSyntaxException exception) {
			return false;
		}
	}

	private static boolean teleportSourcePlayerToOneBlockSpawn(CommandContext<CommandSourceStack> context,
			ServerLevel overworld) {
		try {
			ServerPlayer player = context.getSource().getPlayerOrException();
			return BetelSkyblockManager.teleportPlayerToOneBlockSpawn(player, overworld);
		} catch (CommandSyntaxException exception) {
			return false;
		}
	}

	private static int eatingTest(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		ItemStack stack = player.getMainHandItem();
		EatingRestrictionCheck check = WithdrawalEatingRestrictions.evaluate(player, addiction, stack);
		String itemName = stack.isEmpty() ? "empty" : stack.getHoverName().getString();
		Component result = t(check.allowed()
				? "command.betel-nut-mod.eatingtest.result.allowed"
				: "command.betel-nut-mod.eatingtest.result.blocked");
		Component scope = t(check.checkedItem()
				? "command.betel-nut-mod.eatingtest.scope.checked"
				: "command.betel-nut-mod.eatingtest.scope.ignored");

		sendSuccess(context.getSource(), "command.betel-nut-mod.eatingtest.status", false, itemName,
				addiction.getAddictionValue(), addiction.getAddictionStage(), addiction.getWithdrawalValue(),
				addiction.getWithdrawalSeverity(), WithdrawalEatingRestrictions.isFeatureEnabled(BetelNutConfig.get()),
				check.restrictionLevel().label(), check.food(), scope, check.matchedAllowedTags(), check.reason(),
				result);
		return Command.SINGLE_SUCCESS;
	}

	private static int generateTree(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutConfig config = BetelNutConfig.get();
		boolean generated = tryGenerateTree(player.serverLevel(), player.blockPosition(), player.getRandom(),
				config.betelPalmMinHeight, config.betelPalmMaxHeight);

		if (generated) {
			sendSuccess(context.getSource(), "command.betel-nut-mod.tree.generate.success", true);
		} else {
			sendFailure(context.getSource(), "command.betel-nut-mod.tree.generate.failure");
		}

		return Command.SINGLE_SUCCESS;
	}

	private static boolean tryGenerateTree(ServerLevel level, BlockPos origin, RandomSource random, int minHeight,
			int maxHeight) {
		for (int radius = 0; radius <= 3; radius++) {
			for (int x = -radius; x <= radius; x++) {
				for (int z = -radius; z <= radius; z++) {
					if (Math.abs(x) != radius && Math.abs(z) != radius) {
						continue;
					}

					if (BetelPalmTreeGenerator.generate(level, origin.offset(x, 0, z), random, minHeight, maxHeight)) {
						return true;
					}
				}
			}
		}
		return false;
	}

	private BetelCommands() {
	}
}
