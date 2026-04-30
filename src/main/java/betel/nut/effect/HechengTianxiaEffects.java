package betel.nut.effect;

import betel.nut.BetelNutMidnightConfig;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class HechengTianxiaEffects {
	private static final int TICKS_PER_SECOND = 20;
	private static final int COMA_REFRESH_TICKS = 20;
	private static final int AFTERMATH_REFRESH_TICKS = 40;
	private static final int REGENERATION_DURATION_TICKS = 120 * TICKS_PER_SECOND;
	private static final int SHORT_AFTERMATH_DURATION_TICKS = 60 * TICKS_PER_SECOND;

	public static int positiveDurationTicks() {
		return secondsToTicks(BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds);
	}

	public static int comaDurationTicks() {
		return secondsToTicks(BetelNutMidnightConfig.hechengTianxiaComaDurationSeconds);
	}

	public static int negativeDurationTicks() {
		return secondsToTicks(BetelNutMidnightConfig.hechengTianxiaNegativeDurationSeconds);
	}

	public static void applyPositiveBurst(ServerPlayer player) {
		int duration = positiveDurationTicks();
		add(player, MobEffects.MOVEMENT_SPEED, duration, 2);
		add(player, MobEffects.DIG_SPEED, duration, 2);
		add(player, MobEffects.DAMAGE_BOOST, duration, 2);
		add(player, MobEffects.JUMP, duration, 1);
		add(player, MobEffects.REGENERATION, Math.min(duration, REGENERATION_DURATION_TICKS), 1);
		add(player, MobEffects.DAMAGE_RESISTANCE, duration, 1);
		add(player, MobEffects.FIRE_RESISTANCE, duration, 0);
		add(player, MobEffects.WATER_BREATHING, duration, 0);
		add(player, MobEffects.NIGHT_VISION, duration, 0);
		add(player, MobEffects.SLOW_FALLING, duration, 0);
		add(player, MobEffects.ABSORPTION, duration, 1);
		add(player, MobEffects.LUCK, duration, 1);
	}

	public static void maintainComa(ServerPlayer player, int remainingTicks) {
		int duration = Math.max(1, Math.min(remainingTicks, COMA_REFRESH_TICKS));
		addHidden(player, MobEffects.BLINDNESS, duration, 0);
		addHidden(player, MobEffects.MOVEMENT_SLOWDOWN, duration, 255);
		addHidden(player, MobEffects.DIG_SLOWDOWN, duration, 255);
		addHidden(player, MobEffects.WEAKNESS, duration, 4);
		addHidden(player, MobEffects.DAMAGE_RESISTANCE, duration, 1);
		player.stopUsingItem();
		player.setSprinting(false);
		player.setDeltaMovement(0.0D, 0.0D, 0.0D);
	}

	public static void applyNegativeAftermath(ServerPlayer player, int remainingTicks) {
		maintainNegativeAftermath(player, remainingTicks);
	}

	public static void maintainNegativeAftermath(ServerPlayer player, int remainingTicks) {
		int longDuration = Math.max(1, Math.min(remainingTicks, AFTERMATH_REFRESH_TICKS));
		addHidden(player, MobEffects.MOVEMENT_SLOWDOWN, longDuration, 2);
		addHidden(player, MobEffects.DIG_SLOWDOWN, longDuration, 2);
		addHidden(player, MobEffects.WEAKNESS, longDuration, 2);
		addHidden(player, MobEffects.HUNGER, longDuration, 2);
		addHidden(player, MobEffects.UNLUCK, longDuration, 0);

		int elapsedTicks = Math.max(0, negativeDurationTicks() - remainingTicks);
		if (elapsedTicks < SHORT_AFTERMATH_DURATION_TICKS) {
			int shortRemaining = SHORT_AFTERMATH_DURATION_TICKS - elapsedTicks;
			int shortDuration = Math.max(1, Math.min(shortRemaining, AFTERMATH_REFRESH_TICKS));
			addHidden(player, MobEffects.CONFUSION, shortDuration, 2);
			addHidden(player, MobEffects.BLINDNESS, shortDuration, 0);
			addHidden(player, MobEffects.DARKNESS, shortDuration, 0);
		}
	}

	private static void add(ServerPlayer player,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
			int durationTicks,
			int amplifier) {
		player.addEffect(new MobEffectInstance(effect, Math.max(1, durationTicks), amplifier));
	}

	private static void addHidden(ServerPlayer player,
			net.minecraft.core.Holder<net.minecraft.world.effect.MobEffect> effect,
			int durationTicks,
			int amplifier) {
		player.addEffect(new MobEffectInstance(effect, Math.max(1, durationTicks), amplifier,
				false, false, true));
	}

	private static int secondsToTicks(int seconds) {
		return Math.max(1, seconds) * TICKS_PER_SECOND;
	}

	private HechengTianxiaEffects() {
	}
}
