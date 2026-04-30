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
							.then(literal("generate_one_block").executes(BetelCommands::generateOneBlockSkyblock))
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
					.then(literal("generate_one_block").executes(BetelCommands::generateOneBlockSkyblock))
					.then(literal("reset").executes(BetelCommands::resetSkyblock))
					.then(literal("reset_one_block").executes(BetelCommands::resetOneBlockSkyblock))
					.then(literal("tp").executes(BetelCommands::teleportSkyblock))
					.then(literal("tp_one_block").executes(BetelCommands::teleportOneBlockSkyblock))
					.then(literal("generate_end_portal").executes(BetelCommands::generateEndPortal))
					.then(literal("end_portal").executes(BetelCommands::generateEndPortal)));
		});

		BetelNutMod.LOGGER.info("Betel nut debug commands registered successfully");
	}

	private static int getAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		BetelNutConfig config = BetelNutConfig.get();
		long gameTime = player.level().getGameTime();
		long lastEatTime = addiction.getLastEatTime();
		long timeSinceLastEat = lastEatTime <= 0 ? 0 : Math.max(0, gameTime - lastEatTime);
		String lastEatText = lastEatTime <= 0
				? "\u65e0"
				: lastEatTime + "\uff08\u8ddd\u4eca " + timeSinceLastEat + " tick\uff09";
		long cleanRemainingTicks = Math.max(0, addiction.getCleanTime() - gameTime);
		String withdrawalStartRemaining = withdrawalStartRemainingText(player, addiction, config);
		double maxHealthPenalty = addiction.getCurrentMaxHealthPenalty(player);
		boolean hasMaxHealthPenalty = addiction.hasWithdrawalMaxHealthPenalty(player);
		ItemStack mainHandStack = player.getMainHandItem();
		EatingRestrictionCheck eatingCheck = WithdrawalEatingRestrictions.evaluate(player, addiction, mainHandStack);
		boolean eatingRestrictionEnabled = WithdrawalEatingRestrictions.isFeatureEnabled(config);
		String mainHandName = mainHandStack.isEmpty() ? "empty" : mainHandStack.getHoverName().getString();

		context.getSource().sendSuccess(() -> Component.literal(
				"\u69df\u6994\u6210\u763e\u72b6\u6001\uff1a\u6210\u763e\u503c " + addiction.getAddictionValue()
						+ "\uff0c\u6210\u763e\u9636\u6bb5 " + addiction.getAddictionStage()
						+ "\uff0c\u6212\u65ad\u503c " + addiction.getWithdrawalValue()
						+ "\uff0c\u5f53\u524d\u6212\u65ad\u60e9\u7f5a\u5f3a\u5ea6 " + addiction.getWithdrawalSeverity()
						+ "\uff0c\u5df2\u63d0\u793a\u6212\u65ad\u9636\u6bb5 " + addiction.getNotifiedWithdrawalStage()
						+ "\uff0c\u751f\u547d\u4e0a\u9650\u60e9\u7f5a " + maxHealthPenalty
						+ "\uff0c\u6b63\u5728\u53d7\u751f\u547d\u4e0a\u9650\u60e9\u7f5a " + hasMaxHealthPenalty
						+ "\uff0c\u4e0a\u6b21\u98df\u7528\u65f6\u95f4 " + lastEatText
						+ "\uff0ctimeSinceLastEat " + timeSinceLastEat + " tick"
						+ "\uff0cwithdrawalStartRemaining " + withdrawalStartRemaining
						+ "\uff0c\u6e05\u9192\u4fdd\u62a4\u5269\u4f59 " + cleanRemainingTicks + " tick"
						+ "\uff0c\u8fdb\u98df\u9650\u5236\u542f\u7528 " + eatingRestrictionEnabled
						+ "\uff0c\u5f53\u524d\u8fdb\u98df\u9650\u5236\u7b49\u7ea7 "
						+ eatingCheck.restrictionLevel().label()
						+ "\uff0c\u4e3b\u624b\u7269\u54c1 " + mainHandName
						+ "\uff0c\u662f\u5426\u98df\u7269 " + eatingCheck.food()
						+ "\uff0c\u662f\u5426\u53ef\u98df\u7528 " + eatingCheck.allowed()
						+ "\uff0c\u547d\u4e2d\u5141\u8bb8\u6807\u7b7e " + eatingCheck.matchedAllowedTags()
						+ "\uff0c\u5224\u5b9a\u539f\u56e0 " + eatingCheck.reason() + "\u3002"),
				false);
		return Command.SINGLE_SUCCESS;
	}

	private static int setAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.setAddictionValue(value);
		addiction.refreshWithdrawalEffects(player);

		context.getSource().sendSuccess(() -> Component.literal(
				"\u5df2\u5c06\u4f60\u7684\u69df\u6994\u6210\u763e\u503c\u8bbe\u7f6e\u4e3a "
						+ addiction.getAddictionValue() + "\uff0c\u6210\u763e\u9636\u6bb5 "
						+ addiction.getAddictionStage() + "\u3002"),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int setAddictionAndResetTimer(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.setAddictionValueAndResetLastEatTime(player, value);

		context.getSource().sendSuccess(() -> Component.literal(
				"Set betel addiction to " + addiction.getAddictionValue()
						+ " (stage " + addiction.getAddictionStage() + ")"
						+ " and reset lastEatTime to the current game time."),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int addAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.addAddictionValue(value);
		addiction.refreshWithdrawalEffects(player);

		context.getSource().sendSuccess(() -> Component.literal(
				"\u5df2\u589e\u52a0\u69df\u6994\u6210\u763e\u503c " + value
						+ "\uff0c\u5f53\u524d\u6210\u763e\u503c\u4e3a " + addiction.getAddictionValue()
						+ "\uff0c\u6210\u763e\u9636\u6bb5 " + addiction.getAddictionStage() + "\u3002"),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int clearAddiction(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutEntityComponents.ADDICTION.get(player).clearAddiction(player);

		context.getSource().sendSuccess(() -> Component.literal(
				"\u5df2\u6e05\u7a7a\u4f60\u7684\u69df\u6994\u6210\u763e\u503c\u548c\u6212\u65ad\u503c\u3002"),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int setWithdrawal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		int value = IntegerArgumentType.getInteger(context, "value");
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.setWithdrawalValue(player, value);

		context.getSource().sendSuccess(() -> Component.literal(
				"\u5df2\u5c06\u4f60\u7684\u69df\u6994\u6212\u65ad\u503c\u8bbe\u7f6e\u4e3a "
						+ addiction.getWithdrawalValue() + "\uff0c\u60e9\u7f5a\u5f3a\u5ea6 "
						+ addiction.getWithdrawalSeverity() + "\u3002"),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int triggerWithdrawal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		addiction.triggerWithdrawalTest(player);

		context.getSource().sendSuccess(() -> Component.literal(
				"Triggered betel withdrawal test. addiction=" + addiction.getAddictionValue()
						+ ", stage=" + addiction.getAddictionStage()
						+ ", withdrawal=" + addiction.getWithdrawalValue()
						+ ", severity=" + addiction.getWithdrawalSeverity() + "."),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int teleportEnderBetel(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		boolean teleported = EnderBetelTeleportHandler.tryTeleport(player, true);

		if (teleported) {
			context.getSource().sendSuccess(() -> Component.literal(
					"\u5df2\u6d4b\u8bd5\u672b\u5f71\u69df\u6994\u968f\u673a\u77ac\u79fb\u3002"), true);
			return Command.SINGLE_SUCCESS;
		}

		context.getSource().sendFailure(Component.literal(
				"\u672b\u5f71\u69df\u6994\u77ac\u79fb\u6d4b\u8bd5\u5931\u8d25\uff1a\u6ca1\u6709\u627e\u5230\u5b89\u5168\u843d\u70b9\u3002"));
		return 0;
	}

	private static String withdrawalStartRemainingText(ServerPlayer player, BetelNutAddictionComponent addiction,
			BetelNutConfig config) {
		if (!config.enableAddictionSystem) {
			return "disabled";
		}
		if (addiction.getAddictionValue() <= 0) {
			return "no addiction";
		}
		if (addiction.getAddictionValue() < config.minimumAddictionForWithdrawal) {
			return "not eligible, needs addiction >= " + config.minimumAddictionForWithdrawal;
		}
		if (addiction.getLastEatTime() <= 0) {
			return "timer not started";
		}

		int remaining = addiction.getNextWithdrawalTicks(player);
		if (remaining < 0) {
			return "no scheduled withdrawal";
		}
		return remaining + " tick";
	}

	private static int reloadConfig(CommandContext<CommandSourceStack> context) {
		boolean loaded = BetelNutConfig.reload();
		BetelNutMidnightConfig.reload();

		if (loaded) {
			context.getSource().sendSuccess(() -> Component.literal(
					"\u69df\u6994\u914d\u7f6e\u548c MidnightLib \u914d\u7f6e\u5df2\u91cd\u65b0\u52a0\u8f7d\u3002"), true);
		} else {
			context.getSource().sendFailure(Component.literal(
					"\u69df\u6994\u914d\u7f6e\u8bfb\u53d6\u5931\u8d25\uff0c\u5df2\u56de\u9000\u5230\u9ed8\u8ba4\u914d\u7f6e\u3002\u8bf7\u67e5\u770b\u65e5\u5fd7\u3002"));
		}

		return Command.SINGLE_SUCCESS;
	}

	private static int showTradeInfo(CommandContext<CommandSourceStack> context) {
		BetelNutConfig config = BetelNutConfig.get();
		String status = config.enableFarmerTrades ? "enabled" : "disabled";

		context.getSource().sendSuccess(() -> Component.literal(
				"Farmer betel trades are " + status + ". "
						+ "Novice: 1 emerald -> " + config.farmerTradeRawBetelBuyCount
						+ " raw betel nut; " + config.farmerTradeRawBetelSellCount + " raw betel nut -> 1 emerald. "
						+ "Apprentice: 1 emerald -> " + config.farmerTradeLeafBuyCount + " betel leaf. "
						+ "Journeyman: 2 emeralds -> " + config.farmerTradeRoastedBetelBuyCount
						+ " roasted betel nut. "
						+ "Expert: " + config.farmerTradeFlavorEmeraldCost
						+ " emeralds -> 1 flavored betel nut. "
						+ "Master: " + config.farmerTradeSyntheticWorldEmeraldCost
						+ " emeralds + 1 roasted betel nut -> 1 synthetic world betel."),
				false);
		return Command.SINGLE_SUCCESS;
	}

	private static int skyblockStatus(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			context.getSource().sendFailure(Component.literal("Overworld is not loaded."));
			return 0;
		}

		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		context.getSource().sendSuccess(() -> Component.literal(
				"Betel Skyblock: enabled=" + BetelSkyblockManager.isEnabled(overworld)
						+ ", isBetelSkyblockPreset=" + BetelSkyblockManager.isBetelSkyblockPreset(overworld)
						+ ", isBetelOneBlockSkyblockPreset="
						+ BetelSkyblockManager.isBetelOneBlockSkyblockPreset(overworld)
						+ ", classicConfigEnabled=" + BetelNutMidnightConfig.enableClassicSkyblock
						+ ", oneBlockConfigEnabled=" + BetelNutMidnightConfig.enableOneBlockSkyblock
						+ ", legacyConfigFallback=" + BetelSkyblockManager.isConfigEnabled()
						+ ", hasGeneratedBetelSkyIsland=" + skyblock.hasGeneratedBetelSkyIsland()
						+ ", islandCenter=" + skyblock.getIslandCenter()
						+ ", fixedIslandCenter=" + BetelSkyblockManager.getIslandCenter()
						+ ", skyblockSpawn=" + BetelSkyblockManager.getSkyblockSpawn(overworld)
						+ ", hasSetSkyblockSpawn=" + skyblock.hasSetSkyblockSpawn()
						+ ", hasGeneratedBetelOneBlockIsland="
						+ skyblock.hasGeneratedBetelOneBlockIsland()
						+ ", oneBlockCenter=" + skyblock.getOneBlockCenter()
						+ ", fixedOneBlockCenter=" + BetelSkyblockManager.getOneBlockCenter()
						+ ", oneBlockSpawn=" + BetelSkyblockManager.getOneBlockSpawn(overworld)
						+ ", hasSetBetelOneBlockSpawn=" + skyblock.hasSetBetelOneBlockSpawn()
						+ "."),
				false);
		return Command.SINGLE_SUCCESS;
	}

	private static int generateSkyblock(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			context.getSource().sendFailure(Component.literal("Overworld is not loaded."));
			return 0;
		}

		boolean generated = BetelSkyblockManager.generateForCommand(overworld);
		if (generated) {
			boolean teleported = teleportSourcePlayerToSkyblockSpawn(context, overworld);
			context.getSource().sendSuccess(() -> Component.literal(
					"Generated Betel Skyblock island at " + BetelSkyblockManager.getIslandCenter()
							+ ", set spawn to " + BetelSkyblockManager.getSkyblockSpawn(overworld)
							+ ", teleportedCommandPlayer=" + teleported + "."),
					true);
			return Command.SINGLE_SUCCESS;
		}

		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		context.getSource().sendFailure(Component.literal(
				"Betel Skyblock island was not generated. Generated="
						+ skyblock.hasGeneratedBetelSkyIsland()
						+ ", center=" + skyblock.getIslandCenter() + "."));
		return 0;
	}

	private static int generateOneBlockSkyblock(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			context.getSource().sendFailure(Component.literal("Overworld is not loaded."));
			return 0;
		}

		boolean generated = BetelSkyblockManager.generateOneBlockForCommand(overworld);
		if (generated) {
			boolean teleported = teleportSourcePlayerToOneBlockSpawn(context, overworld);
			context.getSource().sendSuccess(() -> Component.literal(
					"Generated Betel One Block Skyblock block at " + BetelSkyblockManager.getOneBlockCenter()
							+ ", set spawn to " + BetelSkyblockManager.getOneBlockSpawn(overworld)
							+ ", teleportedCommandPlayer=" + teleported + "."),
					true);
			return Command.SINGLE_SUCCESS;
		}

		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		context.getSource().sendFailure(Component.literal(
				"Betel One Block Skyblock block was not generated. Generated="
						+ skyblock.hasGeneratedBetelOneBlockIsland()
						+ ", center=" + skyblock.getOneBlockCenter() + "."));
		return 0;
	}

	private static int resetSkyblock(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			context.getSource().sendFailure(Component.literal("Overworld is not loaded."));
			return 0;
		}

		BetelSkyblockManager.resetGenerationState(overworld);
		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		context.getSource().sendSuccess(() -> Component.literal(
				"Reset Betel Skyblock generation state. hasGeneratedBetelSkyIsland="
						+ skyblock.hasGeneratedBetelSkyIsland()
						+ ", islandCenter=" + skyblock.getIslandCenter()
						+ ", hasSetSkyblockSpawn=" + skyblock.hasSetSkyblockSpawn() + "."),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int resetOneBlockSkyblock(CommandContext<CommandSourceStack> context) {
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			context.getSource().sendFailure(Component.literal("Overworld is not loaded."));
			return 0;
		}

		BetelSkyblockManager.resetOneBlockGenerationState(overworld);
		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		context.getSource().sendSuccess(() -> Component.literal(
				"Reset Betel One Block Skyblock generation state. hasGeneratedBetelOneBlockIsland="
						+ skyblock.hasGeneratedBetelOneBlockIsland()
						+ ", oneBlockCenter=" + skyblock.getOneBlockCenter()
						+ ", hasSetBetelOneBlockSpawn=" + skyblock.hasSetBetelOneBlockSpawn() + "."),
				true);
		return Command.SINGLE_SUCCESS;
	}

	private static int teleportSkyblock(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			context.getSource().sendFailure(Component.literal("Overworld is not loaded."));
			return 0;
		}

		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		if (!skyblock.hasGeneratedBetelSkyIsland()) {
			context.getSource().sendFailure(Component.literal(
					"\u5f53\u524d\u4e16\u754c\u8fd8\u6ca1\u6709\u8bb0\u5f55\u69df\u6994\u7a7a\u5c9b\u5750\u6807\u3002"));
			return 0;
		}

		boolean teleported = BetelSkyblockManager.teleportPlayerToSkyblockSpawn(player, overworld);
		if (teleported) {
			context.getSource().sendSuccess(() -> Component.literal(
					"\u5df2\u4f20\u9001\u5230\u69df\u6994\u7a7a\u5c9b\u51fa\u751f\u70b9\u3002"), true);
			return Command.SINGLE_SUCCESS;
		}

		context.getSource().sendFailure(Component.literal("Failed to teleport to Betel Skyblock spawn."));
		return 0;
	}

	private static int teleportOneBlockSkyblock(CommandContext<CommandSourceStack> context)
			throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		ServerLevel overworld = context.getSource().getServer().getLevel(Level.OVERWORLD);
		if (overworld == null) {
			context.getSource().sendFailure(Component.literal("Overworld is not loaded."));
			return 0;
		}

		BetelSkyblockWorldComponent skyblock = BetelNutWorldComponents.SKYBLOCK_WORLD.get(overworld);
		if (!skyblock.hasGeneratedBetelOneBlockIsland()) {
			context.getSource().sendFailure(Component.literal(
					"\u5f53\u524d\u4e16\u754c\u8fd8\u6ca1\u6709\u8bb0\u5f55\u69df\u6994\u4e00\u65b9\u5757\u7a7a\u5c9b\u5750\u6807\u3002"));
			return 0;
		}

		boolean teleported = BetelSkyblockManager.teleportPlayerToOneBlockSpawn(player, overworld);
		if (teleported) {
			context.getSource().sendSuccess(() -> Component.literal(
					"\u5df2\u4f20\u9001\u5230\u69df\u6994\u4e00\u65b9\u5757\u7a7a\u5c9b\u51fa\u751f\u70b9\u3002"), true);
			return Command.SINGLE_SUCCESS;
		}

		context.getSource().sendFailure(Component.literal("Failed to teleport to Betel One Block Skyblock spawn."));
		return 0;
	}

	private static int generateEndPortal(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		if (!BetelNutMidnightConfig.allowEndPortalCommand) {
			context.getSource().sendFailure(Component.literal(
					"\u672b\u5730\u4f20\u9001\u95e8\u751f\u6210\u6307\u4ee4\u5df2\u88ab\u914d\u7f6e\u5173\u95ed\u3002"));
			return 0;
		}

		ServerPlayer player = context.getSource().getPlayerOrException();
		ServerLevel level = player.serverLevel();
		BlockPos center = player.blockPosition().relative(player.getDirection(), 3).below();

		generateEndPortalStructure(level, center);
		level.playSound(null, center, SoundEvents.END_PORTAL_FRAME_FILL, SoundSource.BLOCKS, 1.0F, 1.0F);

		boolean skyblockPreset = BetelSkyblockManager.isBetelSkyblockPreset(level)
				|| BetelSkyblockManager.isBetelOneBlockSkyblockPreset(level);
		String skyblockHint = skyblockPreset ? ""
				: "\u8be5\u6307\u4ee4\u4e3b\u8981\u7528\u4e8e\u69df\u6994\u7a7a\u5c9b\u4e16\u754c\u3002";
		context.getSource().sendSuccess(() -> Component.literal(
				"\u5df2\u751f\u6210\u5b8c\u6574\u672b\u5730\u4f20\u9001\u95e8\u3002\u4e2d\u5fc3\u5750\u6807\uff1a"
						+ center.getX() + ", " + center.getY() + ", " + center.getZ() + "\u3002"
						+ skyblockHint),
				true);
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
		String result = check.allowed() ? "\u53ef\u4ee5" : "\u4e0d\u80fd";
		String scope = check.checkedItem()
				? "\u4f1a\u8fdb\u5165\u8fdb\u98df\u9650\u5236\u68c0\u67e5"
				: "\u4e0d\u662f\u98df\u7269\u6216\u6cbb\u7597\u996e\u54c1\uff0c\u4e0d\u4f1a\u88ab\u8fdb\u98df\u9650\u5236\u62e6\u622a";

		context.getSource().sendSuccess(() -> Component.literal(
				"\u8fdb\u98df\u6d4b\u8bd5\uff1a\u5f53\u524d\u624b\u6301\u7269\u54c1 " + itemName
						+ "\uff0c\u6210\u763e\u503c " + addiction.getAddictionValue()
						+ "\uff0c\u6210\u763e\u9636\u6bb5 " + addiction.getAddictionStage()
						+ "\uff0c\u6212\u65ad\u503c " + addiction.getWithdrawalValue()
						+ "\uff0c\u6212\u65ad\u60e9\u7f5a\u5f3a\u5ea6 " + addiction.getWithdrawalSeverity()
						+ "\uff0c\u8fdb\u98df\u9650\u5236\u542f\u7528 "
						+ WithdrawalEatingRestrictions.isFeatureEnabled(BetelNutConfig.get())
						+ "\uff0c\u8fdb\u98df\u9650\u5236\u7b49\u7ea7 "
						+ check.restrictionLevel().label()
						+ "\uff0c\u662f\u5426\u98df\u7269 " + check.food()
						+ "\uff0c" + scope
						+ "\uff0c\u547d\u4e2d\u5141\u8bb8\u6807\u7b7e " + check.matchedAllowedTags()
						+ "\uff0c\u5224\u5b9a\u539f\u56e0 " + check.reason()
						+ "\uff0c\u5f53\u524d\u9636\u6bb5" + result + "\u4f7f\u7528\u3002"),
				false);
		return Command.SINGLE_SUCCESS;
	}

	private static int generateTree(CommandContext<CommandSourceStack> context) throws CommandSyntaxException {
		ServerPlayer player = context.getSource().getPlayerOrException();
		BetelNutConfig config = BetelNutConfig.get();
		boolean generated = tryGenerateTree(player.serverLevel(), player.blockPosition(), player.getRandom(),
				config.betelPalmMinHeight, config.betelPalmMaxHeight);

		if (generated) {
			context.getSource().sendSuccess(() -> Component.literal(
					"\u5df2\u751f\u6210\u4e00\u68f5\u69df\u6994\u6811\u3002"), true);
		} else {
			context.getSource().sendFailure(Component.literal(
					"\u5f53\u524d\u4f4d\u7f6e\u4e0d\u9002\u5408\u751f\u6210\u69df\u6994\u6811\u3002"));
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
