package betel.nut.effect;

import betel.nut.BetelNutMidnightConfig;
import betel.nut.BetelNutConfig;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.item.BetelEffectProfile;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;

public final class HechengTianxiaEffects {
	private static final int TICKS_PER_SECOND = 20;
	private static final int COMA_REFRESH_TICKS = 20;
	private static final int AFTERMATH_REFRESH_TICKS = 40;
	private static final int SHORT_AFTERMATH_DURATION_TICKS = 8 * TICKS_PER_SECOND;
	private static final int HUNGER_AFTERMATH_DURATION_TICKS = 20 * TICKS_PER_SECOND;

	public static int positiveDurationTicks() {
		return secondsToTicks(BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds);
	}

	public static int comaDurationTicks() {
		return secondsToTicks(BetelNutMidnightConfig.hechengTianxiaComaDurationSeconds);
	}

	public static int negativeDurationTicks() {
		return secondsToTicks(BetelNutMidnightConfig.hechengTianxiaNegativeDurationSeconds);
	}

	public static int applyPositiveBurst(ServerPlayer player) {
		int stage = BetelNutConfig.get().enableAddictionSystem
				? BetelNutEntityComponents.ADDICTION.get(player).getAddictionStage() : 0;
		int longest = 1;
		for (var effect : BetelEffectProfile.SYNTHETIC.scaledEffects(stage)) {
			add(player, effect.effect(), effect.durationTicks(), effect.amplifier());
			longest = Math.max(longest, effect.durationTicks());
		}
		return longest;
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
		addHidden(player, MobEffects.MOVEMENT_SLOWDOWN, longDuration, 0);
		addHidden(player, MobEffects.DIG_SLOWDOWN, longDuration, 0);
		addHidden(player, MobEffects.WEAKNESS, longDuration, 0);

		int elapsedTicks = Math.max(0, negativeDurationTicks() - remainingTicks);
		if (elapsedTicks < HUNGER_AFTERMATH_DURATION_TICKS) {
			addHidden(player, MobEffects.HUNGER, Math.min(remainingTicks,
					HUNGER_AFTERMATH_DURATION_TICKS - elapsedTicks), 0);
		}
		if (elapsedTicks < SHORT_AFTERMATH_DURATION_TICKS) {
			int shortRemaining = SHORT_AFTERMATH_DURATION_TICKS - elapsedTicks;
			int shortDuration = Math.max(1, Math.min(shortRemaining, remainingTicks));
			addHidden(player, MobEffects.CONFUSION, shortDuration, 0);
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
