package betel.nut.worldgen;

import java.util.LinkedHashMap;
import java.util.Map;

import betel.nut.block.ModBlocks;
import betel.nut.block.BetelPalmLeavesBlock;
import betel.nut.block.BetelPalmLogBlock;
import betel.nut.block.BetelNutClusterBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.tags.BlockTags;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.state.BlockState;

public final class BetelPalmTreeGenerator {
	public static final int DEFAULT_SAPLING_MIN_HEIGHT = 8;
	public static final int DEFAULT_SAPLING_MAX_HEIGHT = 12;
	public static final int DEFAULT_WORLDGEN_MIN_HEIGHT = 9;
	public static final int DEFAULT_WORLDGEN_MAX_HEIGHT = 14;
	private static final int[][] CARDINAL_STYLES = {{0, 1, 2, 0}, {1, 1, 0, 3}, {2, 0, 2, 3}, {3, 2, 1, 3}};
	private static final int[][] DIAGONAL_STYLES = {{1, 3, 0, 2}, {1, 3, 1, 0}, {2, 2, 3, 0}, {3, 0, 3, 1}};

	public static boolean generate(LevelAccessor level, BlockPos basePos, RandomSource random, int minHeight,
			int maxHeight) {
		return generateVariant(level, basePos, random, minHeight, maxHeight, random.nextInt(4), random.nextInt(4) != 0);
	}

	/** Explicit profile is also useful for controlled previews and generation tests. */
	public static boolean generateVariant(LevelAccessor level, BlockPos basePos, RandomSource random, int minHeight,
			int maxHeight, int profile, boolean bearingFruit) {
		int height = randomHeight(random, minHeight, maxHeight);
		Map<BlockPos, BlockState> leaves = createLeaves(basePos, height, random, Math.floorMod(profile, 4));

		if (!canGenerate(level, basePos, height, leaves)) {
			return false;
		}

		BlockState logState = ModBlocks.BETEL_PALM_LOG.defaultBlockState();
		for (int y = 0; y < height; y++) {
			level.setBlock(basePos.above(y), logState.setValue(BetelPalmLogBlock.CROWNSHAFT, y >= height - 2),
					Block.UPDATE_ALL);
		}

		for (Map.Entry<BlockPos, BlockState> leaf : leaves.entrySet()) {
			level.setBlock(leaf.getKey(), leaf.getValue(), Block.UPDATE_ALL);
		}
		if (bearingFruit) {
			Direction start = Direction.from2DDataValue(random.nextInt(4));
			int count = 1 + random.nextInt(3);
			for (int i = 0; i < count; i++) {
				BlockPos fruitPos = basePos.above(height - 3).relative(start);
				if (canWriteAt(level, fruitPos) && canReplaceTreeBlock(level.getBlockState(fruitPos))) {
					level.setBlock(fruitPos, ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState()
							.setValue(BetelNutClusterBlock.FACING, start)
							.setValue(BetelNutClusterBlock.AGE, 1 + random.nextInt(2)), Block.UPDATE_ALL);
				}
				start = start.getClockWise();
			}
		}

		return true;
	}

	private static int randomHeight(RandomSource random, int minHeight, int maxHeight) {
		int min = Math.max(5, Math.min(32, minHeight));
		int max = Math.max(min, Math.min(32, maxHeight));
		return min + random.nextInt(max - min + 1);
	}

	private static Map<BlockPos, BlockState> createLeaves(BlockPos basePos, int height, RandomSource random, int profile) {
		Map<BlockPos, BlockState> leaves = new LinkedHashMap<>();
		BlockPos center = basePos.above(height);
		BlockState leaf = ModBlocks.BETEL_PALM_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT, false);
		leaves.put(center, leaf.setValue(LeavesBlock.DISTANCE, 1)
				.setValue(BetelPalmLeavesBlock.STYLE, profile)
				.setValue(BetelPalmLeavesBlock.FACING, Direction.from2DDataValue(random.nextInt(4))));
		int rotation = random.nextInt(4);
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			int index = (direction.get2DDataValue() + rotation) % 4;
			for (int segment = 1; segment <= 3; segment++) {
				leaves.put(center.relative(direction, segment), leaf
						.setValue(BetelPalmLeavesBlock.FACING, direction)
						.setValue(BetelPalmLeavesBlock.STYLE, CARDINAL_STYLES[profile][index])
						.setValue(BetelPalmLeavesBlock.SEGMENT, segment)
						.setValue(LeavesBlock.DISTANCE, segment + 1));
			}
			// Both diagonal sections touch a cardinal leaf and each other by a face.
			// This keeps vanilla leaf-distance propagation and natural decay intact.
			Direction side = direction.getClockWise();
			leaves.put(center.relative(direction).relative(side), leaf
					.setValue(BetelPalmLeavesBlock.FACING, direction)
					.setValue(BetelPalmLeavesBlock.STYLE, DIAGONAL_STYLES[profile][index])
					.setValue(BetelPalmLeavesBlock.SEGMENT, 4)
					.setValue(LeavesBlock.DISTANCE, 3));
			leaves.put(center.relative(direction, 2).relative(side), leaf
					.setValue(BetelPalmLeavesBlock.FACING, direction)
					.setValue(BetelPalmLeavesBlock.STYLE, DIAGONAL_STYLES[profile][index])
					.setValue(BetelPalmLeavesBlock.SEGMENT, 5)
					.setValue(LeavesBlock.DISTANCE, 4));
		}
		return leaves;
	}

	private static boolean canGenerate(LevelAccessor level, BlockPos basePos, int height,
			Map<BlockPos, BlockState> leaves) {
		if (!canWriteAt(level, basePos.below()) || !canWriteAt(level, basePos.above(height + 1))) {
			return false;
		}

		if (!isValidGround(level.getBlockState(basePos.below()))) {
			return false;
		}
		// Blockbench fronds overhang their anchor blocks. Reserve the complete crown
		// envelope, including downturned tips and the upright young leaves.
		for (BlockPos pos : BlockPos.betweenClosed(basePos.offset(-3, height - 1, -3),
				basePos.offset(3, height + 1, 3))) {
			if (!canWriteAt(level, pos) || !canReplaceTreeBlock(level.getBlockState(pos))) {
				return false;
			}
		}

		for (int y = 0; y < height; y++) {
			BlockPos trunkPos = basePos.above(y);
			if (!canWriteAt(level, trunkPos) || !canReplaceTreeBlock(level.getBlockState(trunkPos))) {
				return false;
			}
		}

		for (BlockPos leafPos : leaves.keySet()) {
			if (!canWriteAt(level, leafPos) || !canReplaceTreeBlock(level.getBlockState(leafPos))) {
				return false;
			}
		}

		return true;
	}

	static boolean canWriteAt(LevelAccessor level, BlockPos pos) {
		if (level.isOutsideBuildHeight(pos)) {
			return false;
		}

		if (level instanceof WorldGenLevel worldGenLevel) {
			if (level instanceof WorldGenRegion region
					&& (Math.abs((pos.getX() >> 4) - region.getCenter().x) > 1
						|| Math.abs((pos.getZ() >> 4) - region.getCenter().z) > 1)) return false;
			return worldGenLevel.ensureCanWrite(pos)
					&& worldGenLevel.getLevel().getWorldBorder().isWithinBounds(pos);
		}

		if (level instanceof Level worldLevel) {
			return worldLevel.getWorldBorder().isWithinBounds(pos);
		}

		return true;
	}

	public static boolean isValidGround(BlockState state) {
		return (state.is(BlockTags.DIRT) && !state.is(Blocks.MUD) && !state.is(Blocks.MUDDY_MANGROVE_ROOTS))
				|| state.is(Blocks.FARMLAND);
	}

	private static boolean canReplaceTreeBlock(BlockState state) {
		return state.getFluidState().isEmpty() && (state.isAir()
				|| state.canBeReplaced()
				|| state.is(ModBlocks.BETEL_PALM_SAPLING)
				|| state.is(BlockTags.LEAVES)
				|| state.is(Blocks.VINE));
	}

	private BetelPalmTreeGenerator() {
	}
}
