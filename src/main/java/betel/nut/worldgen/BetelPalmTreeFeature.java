package betel.nut.worldgen;

import com.mojang.serialization.Codec;

import betel.nut.BetelNutConfig;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

public class BetelPalmTreeFeature extends Feature<NoneFeatureConfiguration> {
	public BetelPalmTreeFeature(Codec<NoneFeatureConfiguration> codec) {
		super(codec);
	}

	@Override
	public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
		return !BetelPalmGroveGenerator.generate(context.level(), context.origin(), context.random(),
				BetelNutConfig.get()).isEmpty();
	}
}
