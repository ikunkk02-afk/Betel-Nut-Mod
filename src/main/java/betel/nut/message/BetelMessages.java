package betel.nut.message;

import betel.nut.BetelNutConfig;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

public final class BetelMessages {
	public static final String WITHDRAWAL_SUPPRESSED = "\u4e0d\u9002\u611f\u6682\u65f6\u6d88\u9000\u4e86\u2026\u2026";
	public static final String MILK_RELIEF = "\u725b\u5976\u53ea\u80fd\u6682\u65f6\u7f13\u89e3\u4e0d\u9002\uff0c\u65e0\u6cd5\u771f\u6b63\u964d\u4f4e\u4f9d\u8d56\u3002";
	public static final String GOLDEN_APPLE_RECOVERY = "\u4f60\u7684\u4f9d\u8d56\u611f\u51cf\u8f7b\u4e86\u4e00\u4e9b\u3002";
	public static final String ENCHANTED_GOLDEN_APPLE_RECOVERY = "\u4f60\u7684\u8eab\u4f53\u4f3c\u4e4e\u88ab\u5f3a\u884c\u51c0\u5316\u4e86\u3002";
	public static final String WITHDRAWAL_CONTINUES_AFTER_DEATH = "\u6b7b\u4ea1\u5e76\u6ca1\u6709\u6446\u8131\u4f60\u7684\u4f9d\u8d56\u3002";
	public static final String WITHDRAWAL_BODY_RECOVERING = "\u4f60\u7684\u8eab\u4f53\u72b6\u6001\u5f00\u59cb\u6062\u590d\u3002";
	public static final String EATING_RESTRICTION_STAGE2 = "\u4f60\u7684\u5634\u90e8\u50f5\u786c\uff0c\u5df2\u7ecf\u5f88\u96be\u5403\u4e0b\u666e\u901a\u98df\u7269\u3002";
	public static final String EATING_RESTRICTION_STAGE3 = "\u4e25\u91cd\u6212\u65ad\u8ba9\u4f60\u51e0\u4e4e\u65e0\u6cd5\u8fdb\u98df\uff0c\u53ea\u6709\u9644\u9b54\u91d1\u82f9\u679c\u8fd8\u80fd\u8d77\u6548\u3002";
	public static final String EATING_RESTRICTION_STAGE4 = "\u4f60\u7684\u8eab\u4f53\u5df2\u7ecf\u65e0\u6cd5\u63a5\u53d7\u666e\u901a\u98df\u7269\u3002";
	public static final String HECHENG_TIANXIA_COMA_STARTED = "\u5408\u6210\u5929\u4e0b\u7684\u5927\u679c\u540e\u52b2\u88ad\u6765\uff0c\u4f60\u660f\u8ff7\u4e86\u3002";
	public static final String HECHENG_TIANXIA_COMA_ENDED = "\u4f60\u4ece\u660f\u8ff7\u4e2d\u9192\u6765\uff0c\u4f46\u8eab\u4f53\u72b6\u6001\u53d8\u5f97\u6781\u5dee\u3002";
	public static final String HECHENG_TIANXIA_REPEAT_BLOCKED = "\u4f60\u7684\u8eab\u4f53\u8fd8\u627f\u53d7\u4e0d\u4e86\u7b2c\u4e8c\u9897\u5408\u6210\u5929\u4e0b\u7684\u5927\u679c\u3002";
	public static final String HECHENG_TIANXIA_COMA_BLOCKED = "\u4f60\u4ecd\u5728\u660f\u8ff7\u4e2d\uff0c\u65e0\u6cd5\u884c\u52a8\u3002";

	public static boolean send(ServerPlayer player, String message) {
		BetelNutConfig config = BetelNutConfig.get();

		if (config.showActionbarMessages) {
			player.displayClientMessage(Component.literal(message), true);
			return true;
		}

		if (config.showChatMessages) {
			player.displayClientMessage(Component.literal(message), false);
			return true;
		}

		return false;
	}

	public static String betelNutEatenMessage(int previousAddiction, int currentAddiction, int maxAddiction) {
		if (previousAddiction <= 0) {
			return "\u4f60\u611f\u89c9\u7cbe\u795e\u8d77\u6765\u4e86\u3002";
		}

		if (currentAddiction >= threshold(maxAddiction, 75)) {
			return "\u4f60\u5df2\u7ecf\u5f88\u96be\u6446\u8131\u8fd9\u79cd\u4f9d\u8d56\u4e86\u3002";
		}

		if (currentAddiction >= threshold(maxAddiction, 50)) {
			return "\u4f60\u5f00\u59cb\u5bf9\u69df\u6994\u4ea7\u751f\u4f9d\u8d56\u3002";
		}

		return "\u8fd9\u79cd\u611f\u89c9\u4f3c\u4e4e\u6709\u70b9\u8ba9\u4eba\u7559\u604b\u3002";
	}

	public static String withdrawalStageMessage(int stage) {
		return switch (stage) {
			case 5 -> "\u6781\u5ea6\u6212\u65ad\u8ba9\u4f60\u7684\u8eab\u4f53\u51e0\u4e4e\u65e0\u6cd5\u627f\u53d7\u3002";
			case 4 -> "\u4e25\u91cd\u6212\u65ad\u8ba9\u4f60\u51e0\u4e4e\u65e0\u6cd5\u6b63\u5e38\u884c\u52a8\u3002";
			case 3 -> "\u4f60\u7684\u8eab\u4f53\u72b6\u6001\u660e\u663e\u6076\u5316\uff0c\u6700\u5927\u751f\u547d\u503c\u4e0b\u964d\u4e86\u3002";
			case 2 -> "\u6212\u65ad\u53cd\u5e94\u6b63\u5728\u4fb5\u8680\u4f60\u7684\u8eab\u4f53\u3002";
			case 1 -> "\u4f60\u5f00\u59cb\u611f\u5230\u70e6\u8e81\u3002";
			default -> "";
		};
	}

	private static int threshold(int maxValue, int percent) {
		return Math.max(1, maxValue * percent / 100);
	}

	private BetelMessages() {
	}
}
