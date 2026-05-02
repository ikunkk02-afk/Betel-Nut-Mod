package betel.nut.addiction;

import betel.nut.BetelNutConfig;
import net.minecraft.ChatFormatting;

public final class AddictionStageUtil {
	private static final int STAGE_COUNT = 5;
	private static final int TICKS_PER_SECOND = 20;

	public static int getStage(int addictionValue) {
		return getStage(addictionValue, BetelNutConfig.get().maxAddictionValue);
	}

	public static int getStage(int addictionValue, int maxAddictionValue) {
		int maxValue = Math.max(1, maxAddictionValue);
		int value = clamp(addictionValue, 0, maxValue);
		if (value <= 0) {
			return 0;
		}

		for (int stage = STAGE_COUNT; stage >= 1; stage--) {
			if (value >= getStageStartThreshold(stage, maxValue)) {
				return stage;
			}
		}
		return 0;
	}

	public static int getAddictionStage(int addictionValue, int maxAddictionValue) {
		return getStage(addictionValue, maxAddictionValue);
	}

	public static int getStageStartThreshold(int stage, int maxAddictionValue) {
		int maxValue = Math.max(1, maxAddictionValue);
		int clampedStage = clamp(stage, 0, STAGE_COUNT);
		if (clampedStage <= 0) {
			return 0;
		}
		if (clampedStage == 1) {
			return 1;
		}
		return (int) Math.ceil(maxValue * ((clampedStage - 1) / (double) STAGE_COUNT));
	}

	public static int getNextStageThreshold(int addictionValue, int maxAddictionValue) {
		int stage = getStage(addictionValue, maxAddictionValue);
		if (stage >= STAGE_COUNT) {
			return Math.max(1, maxAddictionValue);
		}
		return getStageStartThreshold(stage + 1, maxAddictionValue);
	}

	public static float getStageProgress(int addictionValue) {
		return getStageProgress(addictionValue, BetelNutConfig.get().maxAddictionValue);
	}

	public static float getStageProgress(int addictionValue, int maxAddictionValue) {
		int maxValue = Math.max(1, maxAddictionValue);
		int value = clamp(addictionValue, 0, maxValue);
		int stage = getStage(value, maxValue);

		int start = getStageStartThreshold(stage, maxValue);
		int next = stage >= STAGE_COUNT ? maxValue : getStageStartThreshold(stage + 1, maxValue);
		int span = Math.max(1, next - start);
		return clamp((value - start) / (float) span, 0.0F, 1.0F);
	}

	public static boolean wouldReachNextStage(int addictionValue, int addictionIncrease, int maxAddictionValue) {
		if (getStage(addictionValue, maxAddictionValue) >= STAGE_COUNT) {
			return false;
		}
		int nextThreshold = getNextStageThreshold(addictionValue, maxAddictionValue);
		if (nextThreshold <= addictionValue) {
			return false;
		}
		return addictionValue + Math.max(0, addictionIncrease) >= nextThreshold;
	}

	public static int getNextWithdrawalTicks(long nextWithdrawalTime, long gameTime) {
		if (nextWithdrawalTime < 0) {
			return -1;
		}

		return clampRemainingTicks(nextWithdrawalTime - gameTime);
	}

	public static String formatTicksAsTime(int ticks) {
		if (ticks < 0) {
			return "--:--";
		}

		int totalSeconds = Math.max(0, (ticks + TICKS_PER_SECOND - 1) / TICKS_PER_SECOND);
		int minutes = totalSeconds / 60;
		int seconds = totalSeconds % 60;
		return String.format("%02d:%02d", minutes, seconds);
	}

	public static String getStageTranslationKey(int stage) {
		return switch (clamp(stage, 0, STAGE_COUNT)) {
			case 1 -> "betel_nut_mod.addiction.stage.light";
			case 2 -> "betel_nut_mod.addiction.stage.medium";
			case 3 -> "betel_nut_mod.addiction.stage.heavy";
			case 4 -> "betel_nut_mod.addiction.stage.severe";
			case 5 -> "betel_nut_mod.addiction.stage.extreme";
			default -> "betel_nut_mod.addiction.stage.none";
		};
	}

	public static int getWithdrawalSeverity(int stage) {
		return clamp(stage, 0, STAGE_COUNT);
	}

	public static float getBetelRewardDurationMultiplier(int stage) {
		return switch (clamp(stage, 0, STAGE_COUNT)) {
			case 1 -> 0.80F;
			case 2 -> 0.65F;
			case 3 -> 0.50F;
			case 4 -> 0.35F;
			case 5 -> 0.25F;
			default -> 1.00F;
		};
	}

	public static int getBetelRewardAmplifierPenalty(int stage) {
		return switch (clamp(stage, 0, STAGE_COUNT)) {
			case 1, 2 -> 1;
			case 3, 4 -> 2;
			case 5 -> 3;
			default -> 0;
		};
	}

	public static String getWarningTitleKey(int stage) {
		return switch (clamp(stage, 1, STAGE_COUNT)) {
			case 1 -> "betel_nut_mod.warning.light.title";
			case 2 -> "betel_nut_mod.warning.medium.title";
			case 3 -> "betel_nut_mod.warning.heavy.title";
			case 4 -> "betel_nut_mod.warning.severe.title";
			default -> "betel_nut_mod.warning.extreme.title";
		};
	}

	public static String getWarningSubtitleKey(int stage) {
		return switch (clamp(stage, 1, STAGE_COUNT)) {
			case 1 -> "betel_nut_mod.warning.light.subtitle";
			case 2 -> "betel_nut_mod.warning.medium.subtitle";
			case 3 -> "betel_nut_mod.warning.heavy.subtitle";
			case 4 -> "betel_nut_mod.warning.severe.subtitle";
			default -> "betel_nut_mod.warning.extreme.subtitle";
		};
	}

	public static int getStageColor(int stage) {
		return switch (clamp(stage, 0, STAGE_COUNT)) {
			case 1 -> 0xFF43D6A4;
			case 2 -> 0xFFFFD34D;
			case 3 -> 0xFFFF8A2A;
			case 4 -> 0xFFFF4E45;
			case 5 -> 0xFF9B224B;
			default -> 0xFF9A9A9A;
		};
	}

	public static ChatFormatting getStageFormatting(int stage) {
		return switch (clamp(stage, 0, STAGE_COUNT)) {
			case 1 -> ChatFormatting.GREEN;
			case 2 -> ChatFormatting.YELLOW;
			case 3 -> ChatFormatting.GOLD;
			case 4 -> ChatFormatting.RED;
			case 5 -> ChatFormatting.DARK_RED;
			default -> ChatFormatting.GRAY;
		};
	}

	private static int clampRemainingTicks(long ticks) {
		return ticks > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) Math.max(0L, ticks);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
	}

	private AddictionStageUtil() {
	}
}
