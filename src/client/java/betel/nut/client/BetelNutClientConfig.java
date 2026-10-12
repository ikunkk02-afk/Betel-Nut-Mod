package betel.nut.client;

import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import betel.nut.BetelNutMod;
import net.fabricmc.loader.api.FabricLoader;

public final class BetelNutClientConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final String CONFIG_FILE_NAME = "betel_nut_mod_client.json";
	private static final int CONFIG_VERSION = 3;
	private static BetelNutClientConfig INSTANCE = new BetelNutClientConfig();

	public int configVersion = 0;
	public boolean hudEnabled = true;
	public HudAnchor hudAnchor = HudAnchor.BOTTOM_LEFT;
	public int hudOffsetX = 8;
	public int hudOffsetY = 8;
	public int hudX = 8;
	public int hudY = 8;
	public boolean hudUseCustomPosition = false;
	public double hudScale = 1.0D;

	public static BetelNutClientConfig get() {
		return INSTANCE;
	}

	public static boolean load() {
		Path configPath = getConfigPath();
		BetelNutClientConfig defaults = new BetelNutClientConfig();

		if (Files.notExists(configPath)) {
			INSTANCE = defaults.validated();
			save();
			BetelNutMod.LOGGER.info("Betel nut client config loaded successfully: {}", configPath);
			return true;
		}

		try (Reader reader = Files.newBufferedReader(configPath, StandardCharsets.UTF_8)) {
			BetelNutClientConfig loaded = GSON.fromJson(reader, BetelNutClientConfig.class);
			if (loaded == null) {
				throw new JsonParseException("Client config file is empty.");
			}

			INSTANCE = loaded.validated();
			save();
			BetelNutMod.LOGGER.info("Betel nut client config loaded successfully: {}", configPath);
			return true;
		} catch (IOException | JsonParseException | IllegalArgumentException exception) {
			INSTANCE = defaults.validated();
			BetelNutMod.LOGGER.warn("Failed to load betel nut client config from {}, using defaults.", configPath,
					exception);
			return false;
		}
	}

	public static boolean reload() {
		return load();
	}

	public static void save() {
		Path configPath = getConfigPath();

		try {
			Files.createDirectories(configPath.getParent());
			try (Writer writer = Files.newBufferedWriter(configPath, StandardCharsets.UTF_8)) {
				GSON.toJson(INSTANCE.validated(), writer);
			}
		} catch (IOException exception) {
			BetelNutMod.LOGGER.warn("Failed to save betel nut client config to {}.", configPath, exception);
		}
	}

	public void setHudPosition(int x, int y) {
		this.hudX = atLeast(x, 0);
		this.hudY = atLeast(y, 0);
		this.hudUseCustomPosition = true;
	}

	private static Path getConfigPath() {
		return FabricLoader.getInstance().getConfigDir().resolve(CONFIG_FILE_NAME);
	}

	private BetelNutClientConfig validated() {
		migrateOldDefaults();

		if (this.hudAnchor == null) {
			this.hudAnchor = HudAnchor.BOTTOM_LEFT;
		}
		this.hudOffsetX = clamp(this.hudOffsetX, 0, 300);
		this.hudOffsetY = clamp(this.hudOffsetY, 0, 300);
		this.hudX = clamp(this.hudX, 0, 10000);
		this.hudY = clamp(this.hudY, 0, 10000);
		if (!Double.isFinite(this.hudScale)) {
			this.hudScale = 1.0D;
		}
		this.hudScale = clamp(this.hudScale, 0.5D, 2.0D);
		this.configVersion = CONFIG_VERSION;
		return this;
	}

	private void migrateOldDefaults() {
		if (this.configVersion >= CONFIG_VERSION) {
			return;
		}

		if (this.configVersion < 2) {
			if (this.hudX == 0 && this.hudY == 0) {
				this.hudX = this.hudOffsetX;
				this.hudY = this.hudOffsetY;
			}
			this.hudUseCustomPosition = this.hudAnchor == null || this.hudAnchor == HudAnchor.TOP_LEFT;
		}
		if (this.configVersion < 3 && this.hudAnchor == HudAnchor.TOP_LEFT
				&& this.hudX == 10 && this.hudY == 30 && this.hudOffsetX == 10 && this.hudOffsetY == 30) {
			this.hudAnchor = HudAnchor.BOTTOM_LEFT;
			this.hudOffsetX = this.hudOffsetY = 8;
			this.hudUseCustomPosition = false;
		}
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

	public enum HudAnchor {
		TOP_LEFT,
		TOP_RIGHT,
		BOTTOM_LEFT,
		BOTTOM_RIGHT,
		CENTER_TOP,
		CENTER_BOTTOM
	}
}
