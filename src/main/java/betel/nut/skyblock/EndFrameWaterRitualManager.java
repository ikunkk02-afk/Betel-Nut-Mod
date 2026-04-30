package betel.nut.skyblock;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import betel.nut.BetelNutMidnightConfig;
import betel.nut.BetelNutMod;
import betel.nut.advancement.BetelQuestAdvancements;
import betel.nut.item.ModItems;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LightningBolt;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public final class EndFrameWaterRitualManager {
	private static final double SEARCH_RADIUS = 3.0D;
	private static final double SEARCH_RADIUS_SQUARED = SEARCH_RADIUS * SEARCH_RADIUS;
	private static final double MESSAGE_RADIUS_SQUARED = 48.0D * 48.0D;
	private static final int REWARD_DELAY_TICKS = 20;
	private static final List<PendingReward> PENDING_REWARDS = new ArrayList<>();

	private static boolean registered;

	public static void register() {
		if (registered) {
			return;
		}

		ServerTickEvents.END_WORLD_TICK.register(EndFrameWaterRitualManager::tickWorld);
		ServerLifecycleEvents.SERVER_STOPPED.register(server -> PENDING_REWARDS.clear());
		registered = true;
		BetelNutMod.LOGGER.info("[Betel Nut Mod] End frame water ritual handler registered.");
	}

	private static void tickWorld(ServerLevel level) {
		handlePendingRewards(level);
		if (!shouldScan(level)) {
			return;
		}

		List<ItemEntity> waterItems = collectWaterIngredients(level);
		for (ItemEntity item : waterItems) {
			RitualBatch batch = collectBatch(item, waterItems);
			if (batch.hasEnough()) {
				triggerRitual(level, batch);
				return;
			}
		}
	}

	private static boolean shouldScan(ServerLevel level) {
		if (!BetelNutMidnightConfig.enableEndFrameWaterRitual) {
			return false;
		}
		if (level.dimension() != Level.OVERWORLD) {
			return false;
		}
		if (!BetelSkyblockManager.isEnabled(level) && !BetelSkyblockManager.isOneBlockEnabled(level)) {
			return false;
		}
		int interval = Math.max(5, BetelNutMidnightConfig.endFrameRitualCheckIntervalTicks);
		return level.getGameTime() % interval == 0;
	}

	private static List<ItemEntity> collectWaterIngredients(ServerLevel level) {
		List<ItemEntity> items = new ArrayList<>();
		for (Entity entity : level.getAllEntities()) {
			if (entity instanceof ItemEntity item
					&& item.isAlive()
					&& isIngredient(item.getItem())
					&& isInWater(level, item)) {
				items.add(item);
			}
		}
		return items;
	}

	private static RitualBatch collectBatch(ItemEntity center, List<ItemEntity> waterItems) {
		List<ItemEntity> nearby = new ArrayList<>();
		int enderEyes = 0;
		int hechengBetel = 0;
		double totalX = 0.0D;
		double totalY = 0.0D;
		double totalZ = 0.0D;

		for (ItemEntity item : waterItems) {
			if (distanceSquared(center, item) > SEARCH_RADIUS_SQUARED) {
				continue;
			}
			ItemStack stack = item.getItem();
			nearby.add(item);
			totalX += item.getX();
			totalY += item.getY();
			totalZ += item.getZ();
			if (stack.is(Items.ENDER_EYE)) {
				enderEyes += stack.getCount();
			} else if (stack.is(ModItems.SYNTHETIC_WORLD_BETEL)) {
				hechengBetel += stack.getCount();
			}
		}

		Vec3 centerPos = nearby.isEmpty()
				? center.position()
				: new Vec3(totalX / nearby.size(), totalY / nearby.size(), totalZ / nearby.size());
		return new RitualBatch(nearby, centerPos, enderEyes, hechengBetel);
	}

	private static void triggerRitual(ServerLevel level, RitualBatch batch) {
		int eyeCost = Math.max(1, BetelNutMidnightConfig.endFrameRitualEnderEyeCost);
		int betelCost = Math.max(1, BetelNutMidnightConfig.endFrameRitualHechengBetelCost);
		consume(batch.items(), Items.ENDER_EYE, eyeCost);
		consume(batch.items(), ModItems.SYNTHETIC_WORLD_BETEL, betelCost);

		Vec3 center = batch.center();
		summonLightning(level, center);
		level.sendParticles(ParticleTypes.LARGE_SMOKE, center.x, center.y + 0.2D, center.z,
				24, 0.5D, 0.35D, 0.5D, 0.02D);
		level.sendParticles(ParticleTypes.FLAME, center.x, center.y + 0.2D, center.z,
				12, 0.35D, 0.25D, 0.35D, 0.01D);
		level.sendParticles(ParticleTypes.ENCHANT, center.x, center.y + 0.5D, center.z,
				36, 0.8D, 0.45D, 0.8D, 0.1D);
		notifyNearbyPlayers(level, center);
		PENDING_REWARDS.add(new PendingReward(level.dimension(), center, REWARD_DELAY_TICKS,
				Math.max(1, BetelNutMidnightConfig.endFrameRitualFrameReward)));

		BetelNutMod.LOGGER.info(
				"[Betel Nut Mod] End frame water ritual triggered at x={}, y={}, z={}; consumed enderEyes={}, syntheticWorldBetel={}, pendingFrames={}",
				center.x, center.y, center.z, eyeCost, betelCost,
				Math.max(1, BetelNutMidnightConfig.endFrameRitualFrameReward));
	}

	private static void summonLightning(ServerLevel level, Vec3 center) {
		LightningBolt lightning = EntityType.LIGHTNING_BOLT.create(level);
		if (lightning == null) {
			return;
		}
		lightning.setPos(center.x, center.y, center.z);
		lightning.setVisualOnly(true);
		level.addFreshEntity(lightning);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_THUNDER,
				SoundSource.WEATHER, 1.0F, 1.0F);
		level.playSound(null, center.x, center.y, center.z, SoundEvents.LIGHTNING_BOLT_IMPACT,
				SoundSource.WEATHER, 1.0F, 1.0F);
	}

	private static void handlePendingRewards(ServerLevel level) {
		Iterator<PendingReward> iterator = PENDING_REWARDS.iterator();
		while (iterator.hasNext()) {
			PendingReward reward = iterator.next();
			if (!reward.dimension().equals(level.dimension())) {
				continue;
			}
			reward.tick();
			if (!reward.ready()) {
				continue;
			}
			spawnReward(level, reward.center(), reward.count());
			iterator.remove();
		}
	}

	private static void spawnReward(ServerLevel level, Vec3 center, int count) {
		Vec3 spawnPos = rewardSpawnPosition(level, center);
		int remaining = Math.max(1, count);
		while (remaining > 0) {
			int stackCount = Math.min(64, remaining);
			ItemEntity reward = new ItemEntity(level, spawnPos.x, spawnPos.y, spawnPos.z,
					new ItemStack(Items.END_PORTAL_FRAME, stackCount));
			level.addFreshEntity(reward);
			remaining -= stackCount;
		}
		level.sendParticles(ParticleTypes.ENCHANT, spawnPos.x, spawnPos.y + 0.25D, spawnPos.z,
				48, 0.6D, 0.35D, 0.6D, 0.12D);
		grantRitualAdvancement(level, center);
	}

	private static Vec3 rewardSpawnPosition(ServerLevel level, Vec3 center) {
		BlockPos pos = BlockPos.containing(center);
		for (int i = 0; i < 6 && level.getFluidState(pos).is(FluidTags.WATER); i++) {
			pos = pos.above();
		}
		return Vec3.atCenterOf(pos);
	}

	private static void notifyNearbyPlayers(ServerLevel level, Vec3 center) {
		Component message = Component.translatable("message.betel-nut-mod.end_frame_ritual.success");
		for (ServerPlayer player : level.getPlayers(player -> player.distanceToSqr(center) <= MESSAGE_RADIUS_SQUARED)) {
			player.sendSystemMessage(message);
		}
	}

	private static void grantRitualAdvancement(ServerLevel level, Vec3 center) {
		for (ServerPlayer player : level.getPlayers(player -> player.distanceToSqr(center) <= MESSAGE_RADIUS_SQUARED)) {
			BetelQuestAdvancements.grantEndFrameRitual(player);
		}
	}

	private static void consume(List<ItemEntity> items, Item item, int amount) {
		int remaining = amount;
		for (ItemEntity entity : items) {
			if (remaining <= 0 || !entity.isAlive()) {
				continue;
			}
			ItemStack stack = entity.getItem();
			if (!stack.is(item)) {
				continue;
			}
			int consumed = Math.min(remaining, stack.getCount());
			stack.shrink(consumed);
			remaining -= consumed;
			if (stack.isEmpty()) {
				entity.discard();
			} else {
				entity.setItem(stack);
			}
		}
	}

	private static boolean isIngredient(ItemStack stack) {
		return !stack.isEmpty()
				&& (stack.is(Items.ENDER_EYE) || stack.is(ModItems.SYNTHETIC_WORLD_BETEL));
	}

	private static boolean isInWater(ServerLevel level, ItemEntity item) {
		if (item.isInWaterOrBubble()) {
			return true;
		}
		BlockPos pos = item.blockPosition();
		return level.getFluidState(pos).is(FluidTags.WATER)
				|| level.getFluidState(pos.above()).is(FluidTags.WATER)
				|| level.getFluidState(pos.below()).is(FluidTags.WATER);
	}

	private static double distanceSquared(ItemEntity first, ItemEntity second) {
		double x = first.getX() - second.getX();
		double y = first.getY() - second.getY();
		double z = first.getZ() - second.getZ();
		return x * x + y * y + z * z;
	}

	private record RitualBatch(List<ItemEntity> items, Vec3 center, int enderEyes, int hechengBetel) {
		boolean hasEnough() {
			return this.enderEyes >= Math.max(1, BetelNutMidnightConfig.endFrameRitualEnderEyeCost)
					&& this.hechengBetel >= Math.max(1,
							BetelNutMidnightConfig.endFrameRitualHechengBetelCost);
		}
	}

	private static final class PendingReward {
		private final net.minecraft.resources.ResourceKey<Level> dimension;
		private final Vec3 center;
		private final int count;
		private int ticksRemaining;

		private PendingReward(net.minecraft.resources.ResourceKey<Level> dimension, Vec3 center,
				int ticksRemaining, int count) {
			this.dimension = dimension;
			this.center = center;
			this.ticksRemaining = ticksRemaining;
			this.count = count;
		}

		private net.minecraft.resources.ResourceKey<Level> dimension() {
			return this.dimension;
		}

		private Vec3 center() {
			return this.center;
		}

		private int count() {
			return this.count;
		}

		private void tick() {
			this.ticksRemaining--;
		}

		private boolean ready() {
			return this.ticksRemaining <= 0;
		}
	}

	private EndFrameWaterRitualManager() {
	}
}
