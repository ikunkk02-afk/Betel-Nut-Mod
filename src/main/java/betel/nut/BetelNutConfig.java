package betel.nut;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;

import net.fabricmc.loader.api.FabricLoader;

public final class BetelNutConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CONFIG_FILE_NAME = "betel_nut.json";
	private static final int CONFIG_VERSION = 14;
	private static BetelNutConfig INSTANCE = new BetelNutConfig();

	public static final int ROASTED_ADDICTION = 5;
	public static final int SPICY_ADDICTION = 18;
	public static final int SWEET_ADDICTION = 8;
	public static final int REFRESHING_ADDICTION = 10;
	public static final int NIGHT_ADDICTION = 10;
	public static final int ENERGIZING_ADDICTION = 20;
	public static final int HONEY_ADDICTION = 10;
	public static final int GLOW_ADDICTION = 12;
	public static final int PHANTOM_ADDICTION = 14;
	public static final int ENDER_ADDICTION = 18;
	public static final int LAPIS_ADDICTION = 8;
	public static final int QUARTZ_ADDICTION = 10;
	public static final int MAGMA_ADDICTION = 18;
	public static final int AMETHYST_ADDICTION = 10;
	public static final int SYNTHETIC_WORLD_ADDICTION = 45;
	public static final int RICH_WORLD_ADDICTION = 35;
	public static final int UNDERGROUND_ADDICTION = 30;

	public int configVersion = CONFIG_VERSION;
	public boolean enableAddictionSystem = true;
	public int maxAddictionValue = 7;
	public int legacyMaxAddictionValue = 100;
	public boolean enableOralDisease = true;
	public int oralCancerStage1Days = 3;
	public int oralCancerStage2Days = 5;
	public int oralCancerStage3Days = 7;
	public int maxWithdrawalValue = 100;
	public int withdrawalCheckIntervalTicks = 200;
	public int timeBeforeWithdrawalTicks = 1200;
	public int baseWithdrawalIncrease = 4;
	public int milkReliefDurationTicks = 1200;
	public int goldenAppleAddictionReduction = 1;
	public int enchantedGoldenAppleAddictionReduction = 5;
	public int enchantedGoldenAppleCleanTimeTicks = 6000;
	public boolean showActionbarMessages = true;
	public boolean showChatMessages = false;
	public int honeyBetelAddictionIncrease = HONEY_ADDICTION;
	public int glowingBetelAddictionIncrease = GLOW_ADDICTION;
	public int phantomBetelAddictionIncrease = PHANTOM_ADDICTION;
	public int enderBetelAddictionIncrease = ENDER_ADDICTION;
	public int lapisBetelAddictionIncrease = LAPIS_ADDICTION;
	public int quartzBetelAddictionIncrease = QUARTZ_ADDICTION;
	public int magmaBetelAddictionIncrease = MAGMA_ADDICTION;
	public int amethystBetelAddictionIncrease = AMETHYST_ADDICTION;

	public int minimumAddictionForWithdrawal = 2;
	public int goldenAppleWithdrawalReduction = 30;
	public int withdrawalEffectDurationTicks = 260;
	public int withdrawalNauseaDurationTicks = 100;
	public int feedbackCooldownTicks = 80;
	public boolean keepAddictionAfterDeath = true;
	public boolean keepWithdrawalAfterDeath = true;
	public boolean reapplyWithdrawalAfterRespawn = true;
	public int respawnWithdrawalDelayTicks = 40;
	public boolean enableWithdrawalMaxHealthPenalty = true;
	public double stage2MaxHealthPenalty = 4.0D;
	public double stage3MaxHealthPenalty = 8.0D;
	public double stage4MaxHealthPenalty = 12.0D;
	public double minimumMaxHealthAfterPenalty = 6.0D;
	public int stage1EffectAmplifierOffset = 1;
	public boolean enableStage4BlindnessOrDarkness = false;
	public boolean enableWithdrawalEatingRestriction = true;
	public int stage2EatingRestrictionWithdrawalValue = 50;
	public int stage3EatingRestrictionWithdrawalValue = 75;
	// Legacy field retained for saved-config compatibility; hard apples are always blocked at stage II.
	public boolean allowGoldenAppleInStage2 = false;
	public boolean allowMilkInStage2 = true;
	public boolean showEatingRestrictionMessage = true;
	public int eatingRestrictionMessageCooldownTicks = 60;

	public boolean enableBetelPalmWorldGeneration = true;
	public boolean betelPalmGenerateInJungle = true;
	public boolean betelPalmGenerateInSparseJungle = true;
	public boolean betelPalmGenerateInBambooJungle = true;
	public boolean betelPalmGenerateNearJungleRivers = true;
	public boolean betelPalmGenerateInBeach = false;
	public boolean betelPalmGenerateInMangroveSwamp = false;
	public double betelPalmSpawnChance = 0.08D;
	public int betelPalmTreesPerChunk = 1;
	public int betelPalmMinHeight = 9;
	public int betelPalmMaxHeight = 14;

	public boolean enableEnderBetelTeleport = true;
	public int enderBetelTeleportRadiusMin = 8;
	public int enderBetelTeleportRadiusMax = 24;
	public int enderBetelTeleportMaxAttempts = 32;
	public boolean enderBetelTeleportPlaySound = true;
	public boolean enderBetelTeleportParticles = true;
	public boolean enderBetelTeleportDamage = false;
	public double enderBetelTeleportDamageAmount = 2.0D;

	public boolean enableFarmerTrades = true;
	public int farmerTradeRawBetelBuyCount = 4;
	public int farmerTradeRawBetelSellCount = 16;
	public int farmerTradeLeafBuyCount = 6;
	public int farmerTradeRoastedBetelBuyCount = 4;
	public int farmerTradeFlavorEmeraldCost = 3;
	public int farmerTradeSyntheticWorldEmeraldCost = 16;

	public boolean betelSkyblockEnabled = false;
	public boolean enableBetelSkyblockVoidProtection = true;
	public int betelSkyblockVoidProtectionMinY = 0;
	public boolean betelSkyblockVoidProtectionDamage = false;
	public double betelSkyblockVoidProtectionDamageAmount = 2.0D;
	public int betelSkyblockSpawnProtectionTicks = 60;

	public static BetelNutConfig get() {
		return INSTANCE;
	}

	public static boolean load() {
		Path configPath = getConfigPath();
		BetelNutConfig defaults = new BetelNutConfig();

		if (Files.notExists(configPath)) {
			INSTANCE = defaults.validated();
			save();
			BetelNutMod.LOGGER.info("Betel nut config loaded successfully: {}", configPath);
			return true;
		}

		try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			var document = JsonParser.parseReader(reader);
			if (!document.isJsonObject()) throw new JsonParseException("Config must be an object.");
			var json = document.getAsJsonObject();
			// Files predating versioned configs use the old 100-point score scale.
			if (!json.has("configVersion")) json.addProperty("configVersion", 0);
			BetelNutConfig loaded = GSON.fromJson(json, BetelNutConfig.class);
			if (loaded.configVersion < 12 && !json.has("maxAddictionValue")) loaded.maxAddictionValue = 100;

			INSTANCE = loaded.validated();
			save();
			BetelNutMod.LOGGER.info("Betel nut config loaded successfully: {}", configPath);
			return true;
		} catch (IOException | JsonParseException exception) {
			INSTANCE = defaults.validated();
			BetelNutMod.LOGGER.warn("Failed to load betel nut config from {}, using defaults.", configPath,
					exception);
			return false;
		}
	}

	public static boolean reload() {
		return load();
	}

	private static void save() {
		Path configPath = getConfigPath();

		try {
			Files.createDirectories(configPath.getParent());
			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				GSON.toJson(INSTANCE, writer);
			}
		} catch (IOException exception) {
			BetelNutMod.LOGGER.warn("Failed to save betel nut config to {}.", configPath, exception);
		}
	}

	private static Path getConfigPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
	}

	private BetelNutConfig validated() {
		migrateOldDefaults();

		this.maxAddictionValue = clamp(this.maxAddictionValue, 5, 8);
		this.oralCancerStage1Days = clamp(this.oralCancerStage1Days, 3, 365);
		this.oralCancerStage2Days = clamp(this.oralCancerStage2Days, this.oralCancerStage1Days + 1, 366);
		this.oralCancerStage3Days = clamp(this.oralCancerStage3Days, this.oralCancerStage2Days + 1, 367);
		this.maxWithdrawalValue = atLeast(this.maxWithdrawalValue, 1);
		this.withdrawalCheckIntervalTicks = atLeast(this.withdrawalCheckIntervalTicks, 1);
		this.timeBeforeWithdrawalTicks = atLeast(this.timeBeforeWithdrawalTicks, 0);
		this.baseWithdrawalIncrease = atLeast(this.baseWithdrawalIncrease, 1);
		this.milkReliefDurationTicks = atLeast(this.milkReliefDurationTicks, 0);
		this.goldenAppleAddictionReduction = atLeast(this.goldenAppleAddictionReduction, 0);
		this.enchantedGoldenAppleAddictionReduction = atLeast(this.enchantedGoldenAppleAddictionReduction, 0);
		this.enchantedGoldenAppleCleanTimeTicks = atLeast(this.enchantedGoldenAppleCleanTimeTicks, 0);
		this.honeyBetelAddictionIncrease = atLeast(this.honeyBetelAddictionIncrease, 0);
		this.glowingBetelAddictionIncrease = atLeast(this.glowingBetelAddictionIncrease, 0);
		this.phantomBetelAddictionIncrease = atLeast(this.phantomBetelAddictionIncrease, 0);
		this.enderBetelAddictionIncrease = atLeast(this.enderBetelAddictionIncrease, 0);
		this.lapisBetelAddictionIncrease = atLeast(this.lapisBetelAddictionIncrease, 0);
		this.quartzBetelAddictionIncrease = atLeast(this.quartzBetelAddictionIncrease, 0);
		this.magmaBetelAddictionIncrease = atLeast(this.magmaBetelAddictionIncrease, 0);
		this.amethystBetelAddictionIncrease = atLeast(this.amethystBetelAddictionIncrease, 0);
		this.minimumAddictionForWithdrawal = clamp(this.minimumAddictionForWithdrawal, 0, this.maxAddictionValue);
		this.goldenAppleWithdrawalReduction = atLeast(this.goldenAppleWithdrawalReduction, 0);
		this.withdrawalEffectDurationTicks = atLeast(this.withdrawalEffectDurationTicks, 1);
		this.withdrawalNauseaDurationTicks = atLeast(this.withdrawalNauseaDurationTicks, 1);
		this.feedbackCooldownTicks = atLeast(this.feedbackCooldownTicks, 0);
		this.respawnWithdrawalDelayTicks = atLeast(this.respawnWithdrawalDelayTicks, 0);
		this.stage2MaxHealthPenalty = atLeast(this.stage2MaxHealthPenalty, 0.0D);
		this.stage3MaxHealthPenalty = atLeast(this.stage3MaxHealthPenalty, 0.0D);
		this.stage4MaxHealthPenalty = atLeast(this.stage4MaxHealthPenalty, 0.0D);
		this.minimumMaxHealthAfterPenalty = atLeast(this.minimumMaxHealthAfterPenalty, 1.0D);
		this.stage1EffectAmplifierOffset = atLeast(this.stage1EffectAmplifierOffset, 0);
		if (this.stage2EatingRestrictionWithdrawalValue <= 0) {
			this.stage2EatingRestrictionWithdrawalValue = 50;
		}
		if (this.stage3EatingRestrictionWithdrawalValue <= 0) {
			this.stage3EatingRestrictionWithdrawalValue = 75;
		}
		this.stage2EatingRestrictionWithdrawalValue = clamp(this.stage2EatingRestrictionWithdrawalValue, 1,
				this.maxWithdrawalValue);
		this.stage3EatingRestrictionWithdrawalValue = clamp(this.stage3EatingRestrictionWithdrawalValue,
				this.stage2EatingRestrictionWithdrawalValue, this.maxWithdrawalValue);
		this.eatingRestrictionMessageCooldownTicks = atLeast(this.eatingRestrictionMessageCooldownTicks, 0);
		this.betelPalmSpawnChance = clamp(this.betelPalmSpawnChance, 0.0D, 1.0D);
		this.betelPalmTreesPerChunk = clamp(this.betelPalmTreesPerChunk, 0, 8);
		this.betelPalmMinHeight = clamp(this.betelPalmMinHeight, 5, 32);
		this.betelPalmMaxHeight = clamp(this.betelPalmMaxHeight, this.betelPalmMinHeight, 32);
		this.enderBetelTeleportRadiusMin = atLeast(this.enderBetelTeleportRadiusMin, 1);
		this.enderBetelTeleportRadiusMax = atLeast(this.enderBetelTeleportRadiusMax,
				this.enderBetelTeleportRadiusMin);
		this.enderBetelTeleportMaxAttempts = atLeast(this.enderBetelTeleportMaxAttempts, 1);
		this.enderBetelTeleportDamageAmount = atLeast(this.enderBetelTeleportDamageAmount, 0.0D);
		this.farmerTradeRawBetelBuyCount = clamp(this.farmerTradeRawBetelBuyCount, 1, 64);
		this.farmerTradeRawBetelSellCount = clamp(this.farmerTradeRawBetelSellCount, 1, 64);
		this.farmerTradeLeafBuyCount = clamp(this.farmerTradeLeafBuyCount, 1, 64);
		this.farmerTradeRoastedBetelBuyCount = clamp(this.farmerTradeRoastedBetelBuyCount, 1, 64);
		this.farmerTradeFlavorEmeraldCost = clamp(this.farmerTradeFlavorEmeraldCost, 1, 64);
		this.farmerTradeSyntheticWorldEmeraldCost = clamp(this.farmerTradeSyntheticWorldEmeraldCost, 1, 64);
		this.betelSkyblockSpawnProtectionTicks = atLeast(this.betelSkyblockSpawnProtectionTicks, 0);
		this.betelSkyblockVoidProtectionDamageAmount = atLeast(this.betelSkyblockVoidProtectionDamageAmount, 0.0D);
		this.configVersion = CONFIG_VERSION;
		return this;
	}

	private void migrateOldDefaults() {
		if (this.configVersion >= CONFIG_VERSION) {
			return;
		}

		if (this.configVersion < 9) {
			if (this.minimumAddictionForWithdrawal == 25) {
				this.minimumAddictionForWithdrawal = 1;
			}
			if (this.timeBeforeWithdrawalTicks == 6000) {
				this.timeBeforeWithdrawalTicks = 1200;
			}
			if (this.baseWithdrawalIncrease == 1) {
				this.baseWithdrawalIncrease = 4;
			}
		}
		if (this.configVersion < 5) {
			this.enableWithdrawalEatingRestriction = true;
			this.stage2EatingRestrictionWithdrawalValue = 50;
			this.stage3EatingRestrictionWithdrawalValue = 75;
			this.allowGoldenAppleInStage2 = false;
			this.allowMilkInStage2 = true;
			this.showEatingRestrictionMessage = true;
			this.eatingRestrictionMessageCooldownTicks = 60;
		}
		if (this.configVersion < 6) {
			this.enableEnderBetelTeleport = true;
			this.enderBetelTeleportRadiusMin = 8;
			this.enderBetelTeleportRadiusMax = 32;
			this.enderBetelTeleportMaxAttempts = 32;
			this.enderBetelTeleportPlaySound = true;
			this.enderBetelTeleportParticles = true;
			this.enderBetelTeleportDamage = false;
			this.enderBetelTeleportDamageAmount = 2.0D;
		}
		if (this.configVersion < 7) {
			this.honeyBetelAddictionIncrease = HONEY_ADDICTION;
			this.glowingBetelAddictionIncrease = GLOW_ADDICTION;
			this.phantomBetelAddictionIncrease = PHANTOM_ADDICTION;
			this.enderBetelAddictionIncrease = ENDER_ADDICTION;
			this.lapisBetelAddictionIncrease = LAPIS_ADDICTION;
			this.quartzBetelAddictionIncrease = QUARTZ_ADDICTION;
			this.magmaBetelAddictionIncrease = MAGMA_ADDICTION;
			this.amethystBetelAddictionIncrease = AMETHYST_ADDICTION;
			if (this.enderBetelTeleportRadiusMax == 32) {
				this.enderBetelTeleportRadiusMax = 24;
			}
		}
		if (this.configVersion < 8) {
			this.enableBetelSkyblockVoidProtection = true;
			this.betelSkyblockVoidProtectionMinY = 0;
			this.betelSkyblockVoidProtectionDamage = false;
			this.betelSkyblockVoidProtectionDamageAmount = 2.0D;
			this.betelSkyblockSpawnProtectionTicks = 60;
		}
		if (this.configVersion < 9) {
			this.betelSkyblockEnabled = false;
		}
		if (this.configVersion < 10) {
			// Upgrade the old default silhouette; preserve custom height ranges and biome switches.
			if (this.betelPalmMinHeight == 5 && this.betelPalmMaxHeight == 8) {
				this.betelPalmMinHeight = 9;
				this.betelPalmMaxHeight = 14;
			}
		}
		if (this.configVersion < 12) {
			this.legacyMaxAddictionValue = Math.max(1, this.maxAddictionValue);
			this.maxAddictionValue = 7;
			this.minimumAddictionForWithdrawal = this.minimumAddictionForWithdrawal <= 1 ? 2
					: betel.nut.addiction.AddictionStageUtil.migrateLegacyScore(this.minimumAddictionForWithdrawal, this.legacyMaxAddictionValue);
			this.goldenAppleAddictionReduction = this.goldenAppleAddictionReduction == 10 || this.goldenAppleAddictionReduction == 1 ? 1
					: Math.max(0, Math.round(this.goldenAppleAddictionReduction * 7F / this.legacyMaxAddictionValue));
			this.enchantedGoldenAppleAddictionReduction = this.enchantedGoldenAppleAddictionReduction == 40 || this.enchantedGoldenAppleAddictionReduction == 5 ? 5
					: Math.max(0, Math.round(this.enchantedGoldenAppleAddictionReduction * 7F / this.legacyMaxAddictionValue));
		} else if (this.configVersion < 13) {
			// Version 12 already stores real eating days; only replace its old timing defaults.
			this.maxAddictionValue = this.maxAddictionValue >= 5 && this.maxAddictionValue <= 8
					? this.maxAddictionValue : 7;
			if (this.minimumAddictionForWithdrawal == 3) this.minimumAddictionForWithdrawal = 2;
		}
		if (this.configVersion < 14 && this.oralCancerStage1Days == 12
				&& this.oralCancerStage2Days == 21 && this.oralCancerStage3Days == 30) {
			// Replace the complete old default set; preserve custom disease schedules and switches.
			this.oralCancerStage1Days = 3;
			this.oralCancerStage2Days = 5;
			this.oralCancerStage3Days = 7;
		}
	}

	private static int atLeast(int value, int min) {
		return Math.max(min, value);
	}

	private static double atLeast(double value, double min) {
		return Math.max(min, value);
	}

	private static int clamp(int value, int min, int max) {
		return Math.max(min, Math.min(max, value));
	}

	private static double clamp(double value, double min, double max) {
		return Math.max(min, Math.min(max, value));
	}
}
