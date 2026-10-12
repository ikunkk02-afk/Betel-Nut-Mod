package betel.nut.test;

import betel.nut.BetelNutConfig;
import betel.nut.block.BetelPalmLeavesBlock;
import betel.nut.block.BetelPalmLogBlock;
import betel.nut.block.BetelPalmSaplingBlock;
import betel.nut.block.ModBlocks;
import betel.nut.worldgen.BetelPalmTreeGenerator;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.phys.shapes.CollisionContext;

public class BetelPalmGameTests implements FabricGameTest {
	private static BlockPos site(GameTestHelper helper) {
		BlockPos base = helper.absolutePos(new BlockPos(4, 2, 4));
		// The stock empty fixture is only one block high; clear the full tall-palm envelope.
		for (BlockPos pos : BlockPos.betweenClosed(base.offset(-3, 0, -3), base.offset(3, 18, 3))) {
			helper.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
		}
		helper.getLevel().setBlock(base.below(), Blocks.DIRT.defaultBlockState(), 3);
		return base;
	}

	@GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 100)
	public void frondsStayAttachedAndDecayAfterFelling(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos base = site(helper);
		helper.assertTrue(BetelPalmTreeGenerator.generate(level, base, RandomSource.create(12), 9, 9), "Palm must grow");
		for (int y = 0; y < 9; y++) {
			var state = level.getBlockState(base.above(y));
			helper.assertTrue(state.is(ModBlocks.BETEL_PALM_LOG), "Continuous unbranched trunk");
			helper.assertTrue(state.getValue(BetelPalmLogBlock.CROWNSHAFT) == (y >= 7), "Only upper two logs form crownshaft");
		}
		helper.runAfterDelay(15, () -> {
			checkFronds(helper, base.above(9), false);
			for (int y = 0; y < 9; y++) level.setBlock(base.above(y), Blocks.AIR.defaultBlockState(), 3);
		});
		helper.runAfterDelay(35, () -> {
			checkFronds(helper, base.above(9), true);
			for (BlockPos pos : BlockPos.betweenClosed(base.offset(-3, 9, -3), base.offset(3, 9, 3))) {
				var state = level.getBlockState(pos);
				if (state.is(ModBlocks.BETEL_PALM_LEAVES)) state.randomTick(level, pos, RandomSource.create(1));
			}
			for (BlockPos pos : BlockPos.betweenClosed(base.offset(-3, 9, -3), base.offset(3, 9, 3)))
				helper.assertTrue(!level.getBlockState(pos).is(ModBlocks.BETEL_PALM_LEAVES), "Felled foliage must decay");
			helper.succeed();
		});
	}

	private static void checkFronds(GameTestHelper helper, BlockPos crown, boolean detached) {
		var level = helper.getLevel();
		helper.assertTrue(level.getBlockState(crown).getValue(LeavesBlock.DISTANCE) == (detached ? 7 : 1), "Crown leaf distance");
		for (Direction direction : Direction.Plane.HORIZONTAL) for (int segment = 1; segment <= 3; segment++) {
			var state = level.getBlockState(crown.relative(direction, segment));
			helper.assertTrue(state.is(ModBlocks.BETEL_PALM_LEAVES), "All twelve frond sections remain present");
			helper.assertTrue(!state.getValue(LeavesBlock.PERSISTENT), "Generated foliage must decay normally");
			helper.assertTrue(state.getValue(LeavesBlock.DISTANCE) == (detached ? 7 : segment + 1), "Frond connected to stem");
			helper.assertTrue(state.getValue(BetelPalmLeavesBlock.FACING) == direction, "Outward frond orientation");
		}
		for (Direction direction : Direction.Plane.HORIZONTAL) for (int segment = 4; segment <= 5; segment++) {
			var pos = crown.relative(direction, segment - 3).relative(direction.getClockWise());
			var state = level.getBlockState(pos);
			helper.assertTrue(state.is(ModBlocks.BETEL_PALM_LEAVES), "Diagonal foliage stays attached");
			helper.assertTrue(state.getValue(BetelPalmLeavesBlock.SEGMENT) == segment, "Correct diagonal model section");
			helper.assertTrue(state.getValue(BetelPalmLeavesBlock.FACING) == direction, "Diagonal model rotates with its anchor");
			helper.assertTrue(state.getValue(LeavesBlock.DISTANCE) == (detached ? 7 : segment - 1), "Diagonal leaf distance propagates by face adjacency");
		}
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void overhangingFrondsReserveNeighboringSpace(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos base = site(helper);
		level.setBlock(base, ModBlocks.BETEL_PALM_SAPLING.defaultBlockState(), 3);
		// Below the outer frond anchor: the new drooping model extends here.
		BlockPos obstacle = base.offset(0, 8, -3);
		level.setBlock(obstacle, Blocks.STONE.defaultBlockState(), 3);
		helper.assertTrue(!BetelPalmTreeGenerator.generate(level, base, RandomSource.create(1), 9, 9), "Drooping tips must not clip through stone");
		helper.assertTrue(level.getBlockState(base).is(ModBlocks.BETEL_PALM_SAPLING), "Rejected tree preserves sapling");
		helper.assertTrue(level.getBlockState(obstacle).is(Blocks.STONE), "Rejected tree preserves obstacle");
		helper.assertTrue(level.getBlockState(base.above()).isAir(), "Preflight prevents partial trunk");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void obstructedCrownLeavesSaplingAndObstacleIntact(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos base = site(helper);
		level.setBlock(base, ModBlocks.BETEL_PALM_SAPLING.defaultBlockState(), 3);
		BlockPos obstacle = base.above(9).north(3);
		level.setBlock(obstacle, Blocks.STONE.defaultBlockState(), 3);
		helper.assertTrue(!BetelPalmTreeGenerator.generate(level, base, RandomSource.create(1), 9, 9), "Blocked crown must reject entire tree");
		helper.assertTrue(level.getBlockState(base).is(ModBlocks.BETEL_PALM_SAPLING), "Rejected growth preserves sapling");
		helper.assertTrue(level.getBlockState(obstacle).is(Blocks.STONE), "Rejected growth preserves obstacle");
		helper.assertTrue(level.getBlockState(base.above()).isAir(), "Rejected growth leaves no partial trunk");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void sandMudAndStandingWaterRejectGrowth(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos base = site(helper);
		for (var soil : new net.minecraft.world.level.block.Block[] { Blocks.SAND, Blocks.RED_SAND, Blocks.MUD }) {
			level.setBlock(base.below(), soil.defaultBlockState(), 3);
			helper.assertTrue(!BetelPalmTreeGenerator.generate(level, base, RandomSource.create(1), 9, 9), "Unsuitable soil must reject tree");
		}
		level.setBlock(base.below(), Blocks.DIRT.defaultBlockState(), 3);
		level.setBlock(base, Blocks.WATER.defaultBlockState(), 3);
		helper.assertTrue(!BetelPalmTreeGenerator.generate(level, base, RandomSource.create(1), 9, 9), "Waterlogged site must reject tree");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void cultivatedSaplingGrowsOnSingleDirtBlock(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos base = site(helper);
		var sapling = (BetelPalmSaplingBlock) ModBlocks.BETEL_PALM_SAPLING;
		level.setBlock(base, sapling.defaultBlockState(), 3);
		sapling.advanceTree(level, base, level.getBlockState(base), RandomSource.create(7));
		helper.assertTrue(level.getBlockState(base).getValue(BetelPalmSaplingBlock.STAGE) == 1, "First growth advances sapling stage");
		sapling.advanceTree(level, base, level.getBlockState(base), RandomSource.create(7));
		helper.assertTrue(level.getBlockState(base).is(ModBlocks.BETEL_PALM_LOG), "Artificial cultivation needs no extra soil or water");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void uprightFrondHeadroomIsReserved(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos base = site(helper);
		level.setBlock(base.above(10), Blocks.STONE.defaultBlockState(), 3);
		helper.assertTrue(!BetelPalmTreeGenerator.generate(level, base, RandomSource.create(1), 9, 9), "Upright fronds must not clip through solid ceiling");
		helper.assertTrue(level.getBlockState(base).isAir(), "Rejected ceiling leaves no trunk");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void treeMigrationPreservesExistingGameplaySettings(GameTestHelper helper) throws ReflectiveOperationException {
		var config = new BetelNutConfig();
		config.configVersion = 9;
		config.betelPalmMinHeight = 5;
		config.betelPalmMaxHeight = 8;
		config.maxAddictionValue = 100;
        config.minimumAddictionForWithdrawal = 25;
		config.timeBeforeWithdrawalTicks = 6000;
		config.baseWithdrawalIncrease = 1;
		config.betelPalmGenerateInBeach = true;
		var validate = BetelNutConfig.class.getDeclaredMethod("validated");
		validate.setAccessible(true);
		validate.invoke(config);
		helper.assertTrue(config.betelPalmMinHeight == 9 && config.betelPalmMaxHeight == 14, "Old default tree heights migrate");
		helper.assertTrue(config.minimumAddictionForWithdrawal == 3 && config.maxAddictionValue == 7 && config.legacyMaxAddictionValue == 100 && config.timeBeforeWithdrawalTicks == 6000
				&& config.baseWithdrawalIncrease == 1, "Tree settings survive the intentional score-to-days migration");
		helper.assertTrue(config.betelPalmGenerateInBeach, "Existing biome switches remain intact");
		config.configVersion = 9;
		config.betelPalmMinHeight = 6;
		config.betelPalmMaxHeight = 11;
		validate.invoke(config);
		helper.assertTrue(config.betelPalmMinHeight == 6 && config.betelPalmMaxHeight == 11, "Custom tree heights remain intact");
		helper.succeed();
	}

	@GameTest(template = EMPTY_STRUCTURE)
	public void trunkCollisionFollowsAllThreeAxes(GameTestHelper helper) {
		var level = helper.getLevel();
		BlockPos base = site(helper);
		for (Direction.Axis axis : Direction.Axis.values()) {
			var state = ModBlocks.BETEL_PALM_LOG.defaultBlockState().setValue(RotatedPillarBlock.AXIS, axis);
			var bounds = state.getCollisionShape(level, base, CollisionContext.empty()).bounds();
			for (Direction.Axis check : Direction.Axis.values()) {
				double extent = check == Direction.Axis.X ? bounds.getXsize() : check == Direction.Axis.Y ? bounds.getYsize() : bounds.getZsize();
				helper.assertTrue(Math.abs(extent - (axis == check ? 1 : 0.375)) < .0001, "Log collision matches slender rendered axis");
			}
		}
		helper.succeed();
	}
}
