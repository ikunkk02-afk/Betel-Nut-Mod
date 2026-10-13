package betel.nut.worldgen;

import betel.nut.BetelNutMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public final class ModConfiguredFeatures {
	public static final Feature<NoneFeatureConfiguration> BETEL_PALM_TREE = new BetelPalmTreeFeature(
			NoneFeatureConfiguration.CODEC);

	public static void register() {
		betel.nut.platform.BetelRegistries.add(BuiltInRegistries.FEATURE, BetelNutMod.id("betel_palm_tree"), BETEL_PALM_TREE);
		BetelNutMod.LOGGER.info("Registered betel palm configured feature type");
	}

	private ModConfiguredFeatures() {
	}
}
