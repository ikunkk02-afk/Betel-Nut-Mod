package betel.nut.event;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.advancement.BetelQuestAdvancements;
import betel.nut.item.ModItems;
import betel.nut.component.BetelNutAddictionComponent;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.network.AddictionSyncPayload;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerEntityEvents;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

public final class BetelNutEvents {
	public static void register() {
		ServerTickEvents.END_SERVER_TICK.register(server -> {
			BetelNutConfig config = BetelNutConfig.get();
			if (!config.enableAddictionSystem) {
				for (ServerPlayer player : server.getPlayerList().getPlayers()) {
					if (player.isAlive()) {
						BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
						addiction.serverTick(player);
						addiction.tickHechengTianxiaAftereffects(player);
						addiction.tickBetelWithdrawalSuppressions();
						if (server.getTickCount() % 20 == 0) {
							AddictionSyncPayload.send(player);
						}
					}
				}
				return;
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isAlive()) {
					BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
					if (!config.enableWithdrawalMaxHealthPenalty) {
						addiction.clearWithdrawalMaxHealthPenalty(player);
					}
					addiction.handleRespawnWithdrawalCheck(player);
					addiction.serverTick(player);
					addiction.tickHechengTianxiaAftereffects(player);
					addiction.tickBetelWithdrawalSuppressions();
				}
			}

			if (server.getTickCount() % config.withdrawalCheckIntervalTicks == 0) {
				BetelNutMod.LOGGER.debug("AddictionTickHandler tick: serverTick={}, interval={}, players={}",
						server.getTickCount(), config.withdrawalCheckIntervalTicks,
						server.getPlayerList().getPlayerCount());
			}

			for (ServerPlayer player : server.getPlayerList().getPlayers()) {
				if (player.isAlive() && server.getTickCount() % 20 == 0) {
					AddictionSyncPayload.send(player);
				}
			}
		});

		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> {
			BetelNutEntityComponents.ADDICTION.get(handler.player).beginCalendarSession(handler.player);
			BetelNutEntityComponents.ADDICTION.get(handler.player).maintainDependenceEffects(handler.player);
			BetelNutEntityComponents.ADDICTION.get(handler.player).maintainOralEffects(handler.player);
			AddictionSyncPayload.send(handler.player);
		});

		ServerEntityEvents.ENTITY_LOAD.register((entity, world) -> {
			if (entity instanceof ServerPlayer player) {
				AddictionSyncPayload.send(player);
			}
		});

		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) -> {
			if (!alive) {
				BetelNutEntityComponents.ADDICTION.get(newPlayer).scheduleRespawnWithdrawalCheck(newPlayer);
			}
			BetelNutEntityComponents.ADDICTION.get(newPlayer).maintainDependenceEffects(newPlayer);
			BetelNutEntityComponents.ADDICTION.get(newPlayer).maintainOralEffects(newPlayer);
			AddictionSyncPayload.send(newPlayer);
		});

		AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
			if (!world.isClientSide() && player instanceof ServerPlayer serverPlayer) {
				BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(serverPlayer);
				if (addiction.blockHechengTianxiaActionIfComatose(serverPlayer, true)) {
					return InteractionResult.FAIL;
				}
			}
			return InteractionResult.PASS;
		});

		BetelNutMod.LOGGER.info("Betel nut addiction system initialized successfully");
	}

	public static void handleFinishedUsingItem(ServerPlayer player, ItemStack stack, long gameTime) {
		if (!BetelNutConfig.get().enableAddictionSystem) {
			return;
		}

		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		int oldDependence = addiction.getAddictionValue();
		int oldWithdrawal = addiction.getWithdrawalValue();
		int oldExposure = addiction.getOralExposureDays();
		boolean recovery = !stack.is(Items.MILK_BUCKET);

		if (stack.is(Items.MILK_BUCKET)) {
			addiction.applyMilkRelief(player, gameTime);
		} else if (stack.is(Items.ENCHANTED_GOLDEN_APPLE) || stack.is(ModItems.ENCHANTED_GOLDEN_APPLE_PASTE)) {
			addiction.applyEnchantedGoldenAppleRecovery(player, gameTime);
		} else if (stack.is(Items.GOLDEN_APPLE) || stack.is(ModItems.GOLDEN_APPLE_PASTE)) {
			addiction.applyGoldenAppleRecovery(player, gameTime);
        } else if (stack.is(ModItems.RELIEF_POTION)) {
            addiction.applyReliefPotion(player, gameTime);
		}
		if (recovery && (addiction.getAddictionValue() < oldDependence
				|| addiction.getWithdrawalValue() < oldWithdrawal || addiction.getOralExposureDays() < oldExposure)) {
			BetelQuestAdvancements.grantRecovery(player, addiction.getAddictionValue() == 0
					&& addiction.getWithdrawalValue() == 0 && addiction.getOralExposureDays() == 0);
		}
	}

	private BetelNutEvents() {
	}
}
