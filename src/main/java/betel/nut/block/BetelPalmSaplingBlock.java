package betel.nut.block;

import java.util.Optional;

import betel.nut.worldgen.BetelPalmTreeGenerator;
import betel.nut.skyblock.BetelSkyblockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.tags.FluidTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SaplingBlock;
import net.minecraft.world.level.block.grower.TreeGrower;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

public class BetelPalmSaplingBlock extends SaplingBlock {
	private static final TreeGrower BETEL_PALM_GROWER = new TreeGrower("betel_palm", Optional.empty(),
			Optional.empty(), Optional.empty());

	public BetelPalmSaplingBlock(BlockBehaviour.Properties properties) {
		super(BETEL_PALM_GROWER, properties);
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (level.getMaxLocalRawBrightness(pos.above()) >= 9 && hasGrowingClimate(level, pos)
				&& random.nextInt(7) == 0) {
			advanceTree(level, pos, state, random);
		}
	}

	private static boolean hasGrowingClimate(ServerLevel level, BlockPos pos) {
		// Retain the original renewable-resource loop in both void-island presets.
		if (BetelSkyblockManager.isBetelSkyblockPreset(level)
				|| BetelSkyblockManager.isBetelOneBlockSkyblockPreset(level)) {
			return true;
		}
		var biome = level.getBiome(pos).value();
		if (biome.getBaseTemperature() < 0.5F) {
			return false;
		}
		if (biome.getBaseTemperature() >= 0.8F && biome.hasPrecipitation()) {
			return true;
		}
		for (BlockPos nearby : BlockPos.betweenClosed(pos.offset(-4, -1, -4), pos.offset(4, 0, 4))) {
			if (level.getFluidState(nearby).is(FluidTags.WATER)) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void advanceTree(ServerLevel level, BlockPos pos, BlockState state, RandomSource random) {
		if (level.getMaxLocalRawBrightness(pos.above()) < 9) {
			return;
		}
		// Bone meal is the artificial-cultivation route, independent of biome climate.
		if (state.getValue(STAGE) == 0) {
			level.setBlock(pos, state.setValue(STAGE, 1), Block.UPDATE_ALL);
			return;
		}
		BetelPalmTreeGenerator.generate(level, pos, random, BetelPalmTreeGenerator.DEFAULT_SAPLING_MIN_HEIGHT,
				BetelPalmTreeGenerator.DEFAULT_SAPLING_MAX_HEIGHT);
	}
}
