package betel.nut.item;

import java.util.Set;

import betel.nut.BetelNutConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.RelativeMovement;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public final class EnderBetelTeleportHandler {
	private static final String TELEPORT_SUCCESS_MESSAGE = "message.betel-nut-mod.ender_betel.teleport_success";
	private static final String TELEPORT_FAILURE_MESSAGE = "message.betel-nut-mod.ender_betel.teleport_failure";
	private static final int PRIMARY_VERTICAL_SEARCH_RANGE = 8;
	private static final int SECONDARY_VERTICAL_SEARCH_RANGE = 16;

	public static boolean tryTeleport(ServerPlayer player) {
		return tryTeleport(player, false);
	}

	public static boolean tryTeleport(ServerPlayer player, boolean force) {
		BetelNutConfig config = BetelNutConfig.get();
		if (!force && !config.enableEnderBetelTeleport) {
			return false;
		}

		ServerLevel level = player.serverLevel();
		BlockPos targetPos = findSafeTeleportPos(level, player);
		if (targetPos == null) {
			sendActionbar(player, TELEPORT_FAILURE_MESSAGE);
			return false;
		}

		double oldX = player.getX();
		double oldY = player.getY();
		double oldZ = player.getZ();
		double targetX = targetPos.getX() + 0.5D;
		double targetY = targetPos.getY();
		double targetZ = targetPos.getZ() + 0.5D;

		if (config.enderBetelTeleportParticles) {
			spawnTeleportParticles(level, oldX, oldY, oldZ);
		}
		if (config.enderBetelTeleportPlaySound) {
			playTeleportSound(level, oldX, oldY, oldZ);
		}

		boolean teleported = player.teleportTo(level, targetX, targetY, targetZ, Set.<RelativeMovement>of(),
				player.getYRot(), player.getXRot());
		if (!teleported) {
			sendActionbar(player, TELEPORT_FAILURE_MESSAGE);
			return false;
		}

		player.resetFallDistance();
		player.addEffect(new MobEffectInstance(MobEffects.DAMAGE_RESISTANCE, 160, 0));
		if (config.enderBetelTeleportParticles) {
			spawnTeleportParticles(level, targetX, targetY, targetZ);
		}
		if (config.enderBetelTeleportPlaySound) {
			playTeleportSound(level, targetX, targetY, targetZ);
		}
		if (config.enderBetelTeleportDamage && config.enderBetelTeleportDamageAmount > 0.0D) {
			player.hurt(player.damageSources().magic(), (float) config.enderBetelTeleportDamageAmount);
		}

		sendActionbar(player, TELEPORT_SUCCESS_MESSAGE);
		return true;
	}

	public static BlockPos findSafeTeleportPos(ServerLevel level, ServerPlayer player) {
		BetelNutConfig config = BetelNutConfig.get();
		RandomSource random = player.getRandom();
		BlockPos origin = player.blockPosition();
		int minRadius = config.enderBetelTeleportRadiusMin;
		int maxRadius = Math.max(minRadius, config.enderBetelTeleportRadiusMax);
		int radiusRange = maxRadius - minRadius + 1;

		for (int attempt = 0; attempt < config.enderBetelTeleportMaxAttempts; attempt++) {
			int radius = minRadius + random.nextInt(radiusRange);
			double angle = random.nextDouble() * Math.PI * 2.0D;
			int offsetX = (int) Math.round(Math.cos(angle) * radius);
			int offsetZ = (int) Math.round(Math.sin(angle) * radius);
			if (offsetX == 0 && offsetZ == 0) {
				offsetX = radius;
			}

			BlockPos candidate = findSafeInColumn(level, player, origin.getX() + offsetX, origin.getZ() + offsetZ,
					origin.getY(), PRIMARY_VERTICAL_SEARCH_RANGE);
			if (candidate != null) {
				return candidate;
			}

			candidate = findSafeInColumn(level, player, origin.getX() + offsetX, origin.getZ() + offsetZ,
					origin.getY(), SECONDARY_VERTICAL_SEARCH_RANGE);
			if (candidate != null) {
				return candidate;
			}
		}

		return null;
	}

	public static boolean isSafeTeleportPos(ServerLevel level, BlockPos pos) {
		if (!canCheckAt(level, pos) || !canCheckAt(level, pos.above()) || !canCheckAt(level, pos.below())) {
			return false;
		}

		if (!level.getWorldBorder().isWithinBounds(pos)) {
			return false;
		}

		BlockState supportState = level.getBlockState(pos.below());
		BlockState footState = level.getBlockState(pos);
		BlockState headState = level.getBlockState(pos.above());

		return isSafeSupport(level, pos.below(), supportState)
				&& isSafeOccupantSpace(footState)
				&& isSafeOccupantSpace(headState);
	}

	public static boolean isSafeTeleportPos(ServerLevel level, ServerPlayer player, BlockPos pos) {
		if (!isSafeTeleportPos(level, pos)) {
			return false;
		}

		double targetX = pos.getX() + 0.5D;
		double targetY = pos.getY();
		double targetZ = pos.getZ() + 0.5D;
		AABB targetBox = player.getBoundingBox().move(targetX - player.getX(), targetY - player.getY(),
				targetZ - player.getZ());
		return level.getWorldBorder().isWithinBounds(targetBox) && level.noCollision(player, targetBox);
	}

	private static BlockPos findSafeInColumn(ServerLevel level, ServerPlayer player, int x, int z, int centerY,
			int verticalRange) {
		int maxY = Math.min(level.getMaxBuildHeight() - 2, centerY + verticalRange);
		int minY = Math.max(level.getMinBuildHeight() + 1, centerY - verticalRange);

		for (int y = maxY; y >= minY; y--) {
			BlockPos pos = new BlockPos(x, y, z);
			if (isSafeTeleportPos(level, player, pos)) {
				return pos;
			}
		}
		return null;
	}

	private static boolean canCheckAt(ServerLevel level, BlockPos pos) {
		return !level.isOutsideBuildHeight(pos) && level.isLoaded(pos) && level.getWorldBorder().isWithinBounds(pos);
	}

	private static boolean isSafeSupport(ServerLevel level, BlockPos pos, BlockState state) {
		return !isDangerous(state) && state.isFaceSturdy(level, pos, Direction.UP);
	}

	private static boolean isSafeOccupantSpace(BlockState state) {
		return !isDangerous(state) && (state.isAir() || (state.getFluidState().isEmpty() && state.canBeReplaced()));
	}

	private static boolean isDangerous(BlockState state) {
		return !state.getFluidState().isEmpty()
				|| state.is(Blocks.FIRE)
				|| state.is(Blocks.SOUL_FIRE)
				|| state.is(Blocks.CAMPFIRE)
				|| state.is(Blocks.SOUL_CAMPFIRE)
				|| state.is(Blocks.MAGMA_BLOCK)
				|| state.is(Blocks.CACTUS)
				|| state.is(Blocks.SWEET_BERRY_BUSH)
				|| state.is(Blocks.WITHER_ROSE)
				|| state.is(Blocks.POWDER_SNOW);
	}

	private static void spawnTeleportParticles(ServerLevel level, double x, double y, double z) {
		level.sendParticles(ParticleTypes.PORTAL, x, y + 1.0D, z, 32, 0.45D, 0.8D, 0.45D, 0.08D);
	}

	private static void playTeleportSound(ServerLevel level, double x, double y, double z) {
		level.playSound(null, x, y, z, SoundEvents.ENDERMAN_TELEPORT, SoundSource.PLAYERS, 0.9F, 1.0F);
	}

	private static void sendActionbar(ServerPlayer player, String message) {
		player.displayClientMessage(Component.translatable(message), true);
	}

	private EnderBetelTeleportHandler() {
	}
}
