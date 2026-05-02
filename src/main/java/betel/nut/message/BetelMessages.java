package betel.nut.message;

import betel.nut.BetelNutConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BetelMessages {
	public static final String BETEL_SUPPRESSES_WITHDRAWAL = "message.betel-nut-mod.betel_suppresses_withdrawal";
	public static final String WITHDRAWAL_SUPPRESSED = "message.betel-nut-mod.withdrawal_suppressed";
	public static final String MILK_RELIEF = "message.betel-nut-mod.milk_relief";
	public static final String GOLDEN_APPLE_RECOVERY = "message.betel-nut-mod.golden_apple_recovery";
	public static final String ENCHANTED_GOLDEN_APPLE_RECOVERY =
			"message.betel-nut-mod.enchanted_golden_apple_recovery";
	public static final String WITHDRAWAL_CONTINUES_AFTER_DEATH =
			"message.betel-nut-mod.withdrawal_continues_after_death";
	public static final String WITHDRAWAL_BODY_RECOVERING = "message.betel-nut-mod.withdrawal_body_recovering";
	public static final String EATING_RESTRICTION_STAGE2 = "message.betel-nut-mod.eating_restriction_stage2";
	public static final String EATING_RESTRICTION_STAGE3 = "message.betel-nut-mod.eating_restriction_stage3";
	public static final String EATING_RESTRICTION_STAGE4 = "message.betel-nut-mod.eating_restriction_stage4";
	public static final String HECHENG_TIANXIA_COMA_STARTED =
			"message.betel-nut-mod.hecheng_tianxia_coma_started";
	public static final String HECHENG_TIANXIA_COMA_ENDED = "message.betel-nut-mod.hecheng_tianxia_coma_ended";
	public static final String HECHENG_TIANXIA_REPEAT_BLOCKED =
			"message.betel-nut-mod.hecheng_tianxia_repeat_blocked";
	public static final String HECHENG_TIANXIA_COMA_BLOCKED =
			"message.betel-nut-mod.hecheng_tianxia_coma_blocked";

	public static boolean send(ServerPlayer player, String translationKey) {
		return sendTranslatable(player, translationKey);
	}

	public static boolean sendTranslatable(ServerPlayer player, String translationKey) {
		if (translationKey == null || translationKey.isEmpty()) {
			return false;
		}

		BetelNutConfig config = BetelNutConfig.get();
		Component message = Component.translatable(translationKey);

		if (config.showActionbarMessages) {
			player.displayClientMessage(message, true);
			return true;
		}

		if (config.showChatMessages) {
			player.displayClientMessage(message, false);
			return true;
		}

		return false;
	}

	public static String betelNutEatenMessage(int previousAddiction, int currentAddiction, int maxAddiction) {
		if (previousAddiction <= 0) {
			return "message.betel-nut-mod.betel_eaten.first";
		}

		if (currentAddiction >= threshold(maxAddiction, 75)) {
			return "message.betel-nut-mod.betel_eaten.heavy";
		}

		if (currentAddiction >= threshold(maxAddiction, 50)) {
			return "message.betel-nut-mod.betel_eaten.medium";
		}

		return "message.betel-nut-mod.betel_eaten.light";
	}

	public static String withdrawalStageMessage(int stage) {
		return switch (stage) {
			case 5 -> "message.betel-nut-mod.withdrawal_stage.5";
			case 4 -> "message.betel-nut-mod.withdrawal_stage.4";
			case 3 -> "message.betel-nut-mod.withdrawal_stage.3";
			case 2 -> "message.betel-nut-mod.withdrawal_stage.2";
			case 1 -> "message.betel-nut-mod.withdrawal_stage.1";
			default -> "";
		};
	}

	private static int threshold(int maxValue, int percent) {
		return Math.max(1, maxValue * percent / 100);
	}

	private BetelMessages() {
	}
}
