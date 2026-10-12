package betel.nut.block;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.ItemInteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.DirectionProperty;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;

/** A renewable fruit bunch attached to a palm stem, below the green crownshaft. */
public class BetelNutClusterBlock extends Block {
	public static final DirectionProperty FACING = BlockStateProperties.HORIZONTAL_FACING;
	public static final IntegerProperty AGE = BlockStateProperties.AGE_2;
	// The bunch hugs the supporting trunk and extends below its anchor block.
	private static final VoxelShape NORTH_SHAPE = Block.box(3, -7.5, 14.5, 13, 15, 21.5);
	private static final VoxelShape SOUTH_SHAPE = Block.box(3, -7.5, -5.5, 13, 15, 1.5);
	private static final VoxelShape EAST_SHAPE = Block.box(-5.5, -7.5, 3, 1.5, 15, 13);
	private static final VoxelShape WEST_SHAPE = Block.box(14.5, -7.5, 3, 21.5, 15, 13);

	public BetelNutClusterBlock(Properties properties) {
		super(properties);
		registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH).setValue(AGE, 1));
	}

	@Override
	protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
		builder.add(FACING, AGE);
	}

	@Override
	public BlockState getStateForPlacement(BlockPlaceContext context) {
		Direction face = context.getClickedFace();
		if (!face.getAxis().isHorizontal()) return null;
		BlockState state = defaultBlockState().setValue(FACING, face);
		return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
	}

	@Override
	protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
		return level.getBlockState(pos.relative(state.getValue(FACING).getOpposite())).is(ModBlocks.BETEL_PALM_LOG);
	}

	@Override
	protected BlockState updateShape(BlockState state, Direction direction, BlockState neighbor,
			LevelAccessor level, BlockPos pos, BlockPos neighborPos) {
		return state.canSurvive(level, pos) ? state : Blocks.AIR.defaultBlockState();
	}

	@Override
	protected BlockState rotate(BlockState state, Rotation rotation) {
		return state.setValue(FACING, rotation.rotate(state.getValue(FACING)));
	}

	@Override
	protected BlockState mirror(BlockState state, Mirror mirror) {
		return rotate(state, mirror.getRotation(state.getValue(FACING)));
	}

	@Override
	protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return switch (state.getValue(FACING)) {
			case NORTH -> NORTH_SHAPE;
			case SOUTH -> SOUTH_SHAPE;
			case EAST -> EAST_SHAPE;
			case WEST -> WEST_SHAPE;
			default -> NORTH_SHAPE;
		};
	}

	@Override
	protected VoxelShape getCollisionShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
		return Shapes.empty();
	}

	@Override
	protected boolean isRandomlyTicking(BlockState state) {
		return state.getValue(AGE) < 2;
	}

	@Override
	protected void randomTick(BlockState state, ServerLevel level, BlockPos pos, RandomSource random) {
		BlockState current = level.getBlockState(pos);
		if (!current.is(this)) return;
		// Age zero is an old save's spent stalk. Remove it instead of leaving a visible remnant.
		if (current.getValue(AGE) == 0) {
			level.removeBlock(pos, false);
		} else if (current.getValue(AGE) == 1 && current.canSurvive(level, pos)
				&& level.getMaxLocalRawBrightness(pos) >= 9 && random.nextInt(5) == 0) {
			level.setBlock(pos, current.setValue(AGE, 2), Block.UPDATE_ALL);
		}
	}

	public static ItemStack harvestedBunch(BlockState state) {
		return new ItemStack(state.getValue(AGE) == 2
				? ModBlocks.BETEL_NUT_CLUSTER_ITEM : ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM);
	}

	@Override
	public ItemStack getCloneItemStack(LevelReader level, BlockPos pos, BlockState state) {
		return state.getValue(AGE) == 0 ? ItemStack.EMPTY : harvestedBunch(state);
	}

	@Override
	protected ItemInteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos,
			Player player, InteractionHand hand, BlockHitResult hit) {
		// Re-read the authoritative state so a second interaction cannot harvest stale fruit.
		BlockState current = level.getBlockState(pos);
		if (!stack.is(Items.SHEARS) || !player.mayBuild() || !current.is(this)
				|| current.getValue(AGE) == 0 || !current.canSurvive(level, pos)) {
			return ItemInteractionResult.PASS_TO_DEFAULT_BLOCK_INTERACTION;
		}
		if (!level.isClientSide) {
			// Remove first, then drop once: stale packets cannot duplicate a whole bunch.
			if (!level.removeBlock(pos, false)) return ItemInteractionResult.FAIL;
			popResource(level, pos, harvestedBunch(current));
			level.playSound(null, pos, SoundEvents.SHEEP_SHEAR, SoundSource.BLOCKS, 1.0F, 0.9F);
			level.gameEvent(player, GameEvent.SHEAR, pos);
			player.awardStat(Stats.ITEM_USED.get(stack.getItem()));
			stack.hurtAndBreak(1, player, hand == InteractionHand.MAIN_HAND ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND);
		}
		return ItemInteractionResult.sidedSuccess(level.isClientSide);
	}
}
