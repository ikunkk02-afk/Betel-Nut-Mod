package betel.nut.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BonemealableBlock;
import betel.nut.worldgen.BetelPalmTreeGenerator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;

/** The same harvestable log, with the slim stem and crownshaft of a real areca palm. */
public class BetelPalmLogBlock extends RotatedPillarBlock implements BonemealableBlock {
	public static final BooleanProperty CROWNSHAFT = BooleanProperty.create("crownshaft");
	private static final VoxelShape Y_SHAPE = Block.box(5, 0, 5, 11, 16, 11);
	private static final VoxelShape X_SHAPE = Block.box(0, 5, 5, 16, 11, 11);
	private static final VoxelShape Z_SHAPE = Block.box(5, 5, 0, 11, 11, 16);

	public BetelPalmLogBlock(BlockBehaviour.Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(CROWNSHAFT, false));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		super.createBlockStateDefinition(builder);
		builder.add(CROWNSHAFT);
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(AXIS) == Direction.Axis.Y && !state.getValue(CROWNSHAFT);
	}

	/** Recognize the fruit-bearing node from existing tree geometry, including old saves. */
	public static boolean isFruitingNode(LevelReader level, BlockPos pos) {
		BlockState anchor = level.getBlockState(pos);
		if (!anchor.is(ModBlocks.BETEL_PALM_LOG) || anchor.getValue(AXIS) != Direction.Axis.Y
				|| anchor.getValue(CROWNSHAFT)) return false;
		for (int y = 1; y <= 2; y++) {
			BlockState shaft = level.getBlockState(pos.above(y));
			if (!shaft.is(ModBlocks.BETEL_PALM_LOG) || shaft.getValue(AXIS) != Direction.Axis.Y
					|| !shaft.getValue(CROWNSHAFT)) return false;
		}
		BlockState crown = level.getBlockState(pos.above(3));
		if (!crown.is(ModBlocks.BETEL_PALM_LEAVES) || crown.getValue(BetelPalmLeavesBlock.SEGMENT) != 0)
			return false;
		// A severed/felled trunk or a decorative lone log cannot produce free fruit.
		for (int depth = 1; depth <= 32; depth++) {
			BlockState below = level.getBlockState(pos.below(depth));
			if (!below.is(ModBlocks.BETEL_PALM_LOG))
				return depth >= 3 && BetelPalmTreeGenerator.isValidGround(below);
			if (below.getValue(AXIS) != Direction.Axis.Y) return false;
		}
		return false;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		if (!isFruitingNode(level, pos) || random.nextInt(5) != 0) return;
		int count = 0;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockState fruit = level.getBlockState(pos.relative(direction));
			if (fruit.is(ModBlocks.BETEL_NUT_CLUSTER)
					&& fruit.getValue(BetelNutClusterBlock.FACING) == direction) {
				if (fruit.getValue(BetelNutClusterBlock.AGE) == 0) level.removeBlock(pos.relative(direction), false);
				else count++;
			}
		}
		if (count >= 3) return;
		Direction direction = Direction.from2DDataValue(random.nextInt(4));
		for (int attempt = 0; attempt < 4; attempt++, direction = direction.getClockWise()) {
			BlockPos fruitPos = pos.relative(direction);
			if (level.isLoaded(fruitPos) && level.isEmptyBlock(fruitPos) && level.isEmptyBlock(fruitPos.below())
					&& level.getMaxLocalRawBrightness(fruitPos) >= 9) {
				level.setBlock(fruitPos, ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState()
						.setValue(BetelNutClusterBlock.FACING, direction).setValue(BetelNutClusterBlock.AGE, 1), Block.UPDATE_ALL);
				return;
			}
		}
	}

	/** Allow cultivation from any upright part of the intact trunk. */
	private static BlockPos findFruitingNode(LevelReader level, BlockPos pos) {
		BlockState clicked = level.getBlockState(pos);
		if (!clicked.is(ModBlocks.BETEL_PALM_LOG) || clicked.getValue(AXIS) != Direction.Axis.Y) return null;
		for (int offset = -2; offset <= 32; offset++) {
			BlockPos candidate = pos.above(offset);
			if (!level.hasChunkAt(candidate)) continue;
			if (offset > 0 && !level.getBlockState(candidate).is(ModBlocks.BETEL_PALM_LOG)) break;
			if (isFruitingNode(level, candidate)) return candidate;
		}
		return null;
	}

	private static boolean isAttachedFruit(BlockState state, Direction direction) {
		return state.is(ModBlocks.BETEL_NUT_CLUSTER)
				&& state.getValue(BetelNutClusterBlock.FACING) == direction;
	}

	private static boolean hasFruitSpace(LevelReader level, BlockPos pos) {
		BlockState state = level.getBlockState(pos);
		return (state.isAir() || state.is(ModBlocks.BETEL_NUT_CLUSTER)
				&& state.getValue(BetelNutClusterBlock.AGE) == 0)
				&& level.getBlockState(pos.below()).isAir();
	}

	@Override
	public boolean isValidBonemealTarget(LevelReader level, BlockPos pos, BlockState state) {
		BlockPos anchor = findFruitingNode(level, pos);
		if (anchor == null) return false;
		int count = 0;
		boolean space = false;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos fruitPos = anchor.relative(direction);
			BlockState fruit = level.getBlockState(fruitPos);
			if (isAttachedFruit(fruit, direction) && fruit.getValue(BetelNutClusterBlock.AGE) > 0) {
				if (fruit.getValue(BetelNutClusterBlock.AGE) < 2) return true;
				count++;
			} else if (hasFruitSpace(level, fruitPos)) space = true;
		}
		return count < 3 && space;
	}

	@Override
	public boolean isBonemealSuccess(Level level, RandomSource random, BlockPos pos, BlockState state) {
		return true;
	}

	@Override
	public void performBonemeal(ServerLevel level, RandomSource random, BlockPos pos, BlockState state) {
		BlockPos anchor = findFruitingNode(level, pos);
		if (anchor == null) return;
		int count = 0;
		for (Direction direction : Direction.Plane.HORIZONTAL) {
			BlockPos fruitPos = anchor.relative(direction);
			BlockState fruit = level.getBlockState(fruitPos);
			if (isAttachedFruit(fruit, direction) && fruit.getValue(BetelNutClusterBlock.AGE) > 0) {
				count++;
				level.setBlock(fruitPos, fruit.setValue(BetelNutClusterBlock.AGE, 2), Block.UPDATE_ALL);
			}
		}
		Direction direction = Direction.from2DDataValue(random.nextInt(4));
		for (int attempt = 0; attempt < 4 && count < 3; attempt++, direction = direction.getClockWise()) {
			BlockPos fruitPos = anchor.relative(direction);
			if (hasFruitSpace(level, fruitPos)) {
				level.setBlock(fruitPos, ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState()
						.setValue(BetelNutClusterBlock.FACING, direction).setValue(BetelNutClusterBlock.AGE, 2), Block.UPDATE_ALL);
				count++;
			}
		}
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		Direction.Axis axis = state.getValue(AXIS);
		return axis == Direction.Axis.X ? X_SHAPE : axis == Direction.Axis.Z ? Z_SHAPE : Y_SHAPE;
	}
}
