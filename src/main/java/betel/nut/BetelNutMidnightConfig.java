package betel.nut;

import eu.midnightdust.lib.config.MidnightConfig;

public class BetelNutMidnightConfig extends MidnightConfig {
	private static boolean initialized;

	@Entry(category = "classic_skyblock")
	public static boolean enableClassicSkyblock = true;
	@Entry(category = "classic_skyblock", min = 1, max = 300)
	public static int classicIslandY = 128;
	@Entry(category = "classic_skyblock")
	public static boolean classicGenerateStarterChest = true;
	@Entry(category = "classic_skyblock")
	public static boolean classicEnableVoidRescue = true;
	@Entry(category = "classic_skyblock")
	public static boolean classicGiveGuideBook = true;

	@Entry(category = "one_block_skyblock")
	public static boolean enableOneBlockSkyblock = true;
	@Entry(category = "one_block_skyblock", min = 1, max = 300)
	public static int oneBlockY = 128;
	@Entry(category = "one_block_skyblock")
	public static boolean oneBlockGiveStarterItems = true;
	@Entry(category = "one_block_skyblock")
	public static boolean oneBlockEnableVoidRescue = true;

	@Entry(category = "one_block_starter_items", min = 0)
	public static int oneBlockStarterBetelNutCount = 1;
	@Entry(category = "one_block_starter_items", min = 0)
	public static int oneBlockStarterSaplingCount = 1;
	@Entry(category = "one_block_starter_items", min = 0)
	public static int oneBlockStarterDirtCount = 1;

	@Entry(category = "gameplay_values", min = 0.0D, max = 1.0D)
	public static double residueCompostChance = 0.3D;
	@Entry(category = "gameplay_values", min = 0.0D, max = 1.0D)
	public static double residueBlockCompostChance = 1.0D;
	@Entry(category = "gameplay_values")
	public static boolean allowEndPortalCommand = true;
	@Entry(category = "gameplay_values")
	public static boolean enableEndFrameWaterRitual = true;
	@Entry(category = "gameplay_values", min = 1)
	public static int endFrameRitualEnderEyeCost = 16;
	@Entry(category = "gameplay_values", min = 1)
	public static int endFrameRitualHechengBetelCost = 12;
	@Entry(category = "gameplay_values", min = 1)
	public static int endFrameRitualFrameReward = 12;
	@Entry(category = "gameplay_values", min = 5)
	public static int endFrameRitualCheckIntervalTicks = 20;
	@Entry(category = "gameplay_values", min = 1, max = 1800)
	public static int hechengTianxiaPositiveDurationSeconds = 300;
	@Entry(category = "gameplay_values", min = 1, max = 600)
	public static int hechengTianxiaComaDurationSeconds = 120;
	@Entry(category = "gameplay_values", min = 1, max = 3600)
	public static int hechengTianxiaNegativeDurationSeconds = 600;
	@Entry(category = "gameplay_values")
	public static boolean hechengTianxiaAllowRepeatBeforeAftermath = false;

	public static void load() {
		if (!initialized) {
			MidnightConfig.init(BetelNutMod.MOD_ID, BetelNutMidnightConfig.class);
			initialized = true;
		} else {
			MidnightConfig.configInstances.get(BetelNutMod.MOD_ID).loadValuesFromJson();
		}

		clampValues();
		MidnightConfig.write(BetelNutMod.MOD_ID);
		logLoadedValues();
	}

	public static void reload() {
		load();
	}

	private static void clampValues() {
		classicIslandY = clamp(classicIslandY, 1, 300);
		oneBlockY = clamp(oneBlockY, 1, 300);
		oneBlockStarterBetelNutCount = atLeast(oneBlockStarterBetelNutCount, 0);
		oneBlockStarterSaplingCount = atLeast(oneBlockStarterSaplingCount, 0);
		oneBlockStarterDirtCount = atLeast(oneBlockStarterDirtCount, 0);
		residueCompostChance = clamp(residueCompostChance, 0.0D, 1.0D);
		residueBlockCompostChance = clamp(residueBlockCompostChance, 0.0D, 1.0D);
		endFrameRitualEnderEyeCost = atLeast(endFrameRitualEnderEyeCost, 1);
		endFrameRitualHechengBetelCost = atLeast(endFrameRitualHechengBetelCost, 1);
		endFrameRitualFrameReward = atLeast(endFrameRitualFrameReward, 1);
		endFrameRitualCheckIntervalTicks = atLeast(endFrameRitualCheckIntervalTicks, 5);
		hechengTianxiaPositiveDurationSeconds = clamp(hechengTianxiaPositiveDurationSeconds, 1, 1800);
		hechengTianxiaComaDurationSeconds = clamp(hechengTianxiaComaDurationSeconds, 1, 600);
		hechengTianxiaNegativeDurationSeconds = clamp(hechengTianxiaNegativeDurationSeconds, 1, 3600);
	}

	private static void logLoadedValues() {
		BetelNutMod.LOGGER.info("[Betel Nut Mod] MidnightLib config loaded.");
		BetelNutMod.LOGGER.info("[Betel Nut Mod] classicIslandY = {}", classicIslandY);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] oneBlockY = {}", oneBlockY);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] residueCompostChance = {}", residueCompostChance);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] residueBlockCompostChance = {}",
				residueBlockCompostChance);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] enableEndFrameWaterRitual = {}",
				enableEndFrameWaterRitual);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] endFrameRitualEnderEyeCost = {}",
				endFrameRitualEnderEyeCost);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] endFrameRitualHechengBetelCost = {}",
				endFrameRitualHechengBetelCost);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] endFrameRitualFrameReward = {}",
				endFrameRitualFrameReward);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] endFrameRitualCheckIntervalTicks = {}",
				endFrameRitualCheckIntervalTicks);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] hechengTianxiaPositiveDurationSeconds = {}",
				hechengTianxiaPositiveDurationSeconds);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] hechengTianxiaComaDurationSeconds = {}",
				hechengTianxiaComaDurationSeconds);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] hechengTianxiaNegativeDurationSeconds = {}",
				hechengTianxiaNegativeDurationSeconds);
		BetelNutMod.LOGGER.info("[Betel Nut Mod] hechengTianxiaAllowRepeatBeforeAftermath = {}",
				hechengTianxiaAllowRepeatBeforeAftermath);
	}

	private static int atLeast(int value, int min) {
		return Math.max(min, value);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
