package betel.nut.test;

import betel.nut.block.BetelNutClusterBlock;
import betel.nut.block.BetelPalmLogBlock;
import betel.nut.block.ModBlocks;
import betel.nut.item.ModItems;
import betel.nut.worldgen.BetelPalmTreeGenerator;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BoneMealItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DispenserBlock;
import net.minecraft.world.level.block.entity.DispenserBlockEntity;

@GameTestHolder("betel_nut_mod")
@PrefixGameTestTemplate(false)
public class BetelProductionGameTests {
    private static BlockPos tree(GameTestHelper h) {
        var base = h.absolutePos(new BlockPos(4, 2, 4));
        for (var pos : BlockPos.betweenClosed(base.offset(-3, 0, -3), base.offset(3, 18, 3)))
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        h.getLevel().setBlock(base.below(), Blocks.DIRT.defaultBlockState(), 3);
        h.assertTrue(BetelPalmTreeGenerator.generateVariant(h.getLevel(), base, RandomSource.create(2), 9, 9, 0, false), "Tree generated");
        return base;
    }

    private static void assertRipe(GameTestHelper h, BlockPos base) {
        int count = 0;
        for (var direction : Direction.Plane.HORIZONTAL) {
            var state = h.getLevel().getBlockState(base.above(6).relative(direction));
            if (state.is(ModBlocks.BETEL_NUT_CLUSTER)) {
                count++;
                h.assertTrue(state.getValue(BetelNutClusterBlock.AGE) == 2, "Every bunch is immediately ripe");
            }
        }
        h.assertTrue(count == 3, "Exactly three ripe bunches per intact tree");
    }

    @GameTest(template = "empty")
    public void trunkBonemealRenewsThreeRipeBunchesAndConsumesOnlyOnChange(GameTestHelper h) {
        var base = tree(h);
        var meal = new ItemStack(Items.BONE_MEAL, 5);
        for (int cycle = 0; cycle < 3; cycle++) {
            h.assertTrue(BoneMealItem.growCrop(meal, h.getLevel(), base), "Bone meal works from the bottom of the intact trunk");
            assertRipe(h, base);
            h.assertTrue(meal.getCount() == 4 - cycle, "One bone meal per whole-tree harvest cycle");
            h.assertTrue(!BoneMealItem.growCrop(meal, h.getLevel(), base.above(8)), "Full ripe tree rejects another dose from crownshaft");
            for (var direction : Direction.Plane.HORIZONTAL) h.getLevel().removeBlock(base.above(6).relative(direction), false);
        }
        h.assertTrue(meal.getCount() == 2, "Rejected doses consume nothing");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void greenBonemealRipensOnceAndRejectsOldStalk(GameTestHelper h) {
        var base = tree(h);
        var pos = base.above(6).north();
        h.getLevel().setBlock(pos, ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState(), 3);
        var meal = new ItemStack(Items.BONE_MEAL, 3);
        h.assertTrue(BoneMealItem.growCrop(meal, h.getLevel(), pos), "Green bunch accepts bone meal");
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(BetelNutClusterBlock.AGE) == 2 && meal.getCount() == 2, "One dose ripens a green bunch");
        h.assertTrue(!BoneMealItem.growCrop(meal, h.getLevel(), pos) && meal.getCount() == 2, "Ripe bunch consumes nothing");
        h.getLevel().setBlock(pos, ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState().setValue(BetelNutClusterBlock.AGE, 0), 3);
        h.assertTrue(!BoneMealItem.growCrop(meal, h.getLevel(), pos), "Legacy stalk is not a fruit to fertilize");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void bonemealRespectsSeveredTreesAndOccupiedSpaces(GameTestHelper h) {
        var base = tree(h);
        var meal = new ItemStack(Items.BONE_MEAL, 4);
        h.getLevel().removeBlock(base.above(2), false);
        h.assertTrue(!BoneMealItem.growCrop(meal, h.getLevel(), base.above(6)), "A severed tree cannot produce fruit");
        h.getLevel().setBlock(base.above(2), ModBlocks.BETEL_PALM_LOG.defaultBlockState(), 3);
        for (var d : Direction.Plane.HORIZONTAL) h.getLevel().setBlock(base.above(6).relative(d).below(), Blocks.STONE.defaultBlockState(), 3);
        h.assertTrue(!BoneMealItem.growCrop(meal, h.getLevel(), base), "Fruit needs room below its anchor");
        h.assertTrue(meal.getCount() == 4, "Invalid targets consume nothing");
        h.getLevel().removeBlock(base.above(5).north(), false);
        h.assertTrue(BoneMealItem.growCrop(meal, h.getLevel(), base), "One clear space is usable");
        h.assertTrue(h.getLevel().getBlockState(base.above(6).north()).getValue(BetelNutClusterBlock.AGE) == 2, "Available space gets one ripe bunch");
        for (var d : Direction.Plane.HORIZONTAL) if (d != Direction.NORTH)
            h.assertTrue(h.getLevel().getBlockState(base.above(5).relative(d)).is(Blocks.STONE), "Player blocks remain intact");
        h.succeed();
    }

    @GameTest(template = "empty")
    public void dispenserActuallyFertilizesTrunk(GameTestHelper h) {
        var base = tree(h);
        var pos = base.south();
        h.getLevel().setBlock(pos, Blocks.DISPENSER.defaultBlockState().setValue(DispenserBlock.FACING, Direction.NORTH), 3);
        var dispenser = (DispenserBlockEntity) h.getLevel().getBlockEntity(pos);
        dispenser.setItem(0, new ItemStack(Items.BONE_MEAL, 2));
        h.getLevel().setBlock(pos.south(), Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
        h.runAfterDelay(8, () -> {
            assertRipe(h, base);
            h.assertTrue(dispenser.getItem(0).getCount() == 1, "Real dispenser uses one dose");
            h.succeed();
        });
    }

    @GameTest(template = "empty")
    public void dirtAndGravelKeepVanillaLootAndSometimesDropOrdinaryShells(GameTestHelper h) {
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        for (var block : new Block[]{Blocks.DIRT, Blocks.GRAVEL}) {
            int shells = 0;
            for (int i = 0; i < 1024; i++) {
                boolean vanilla = false;
                for (var stack : Block.getDrops(block.defaultBlockState(), h.getLevel(), pos, null, null, new ItemStack(Items.IRON_SHOVEL))) {
                    if (stack.is(ModItems.SHELL)) { shells += stack.getCount(); h.assertTrue(stack.getCount() == 1, "At most one extra shell"); }
                    else vanilla |= stack.is(block.asItem()) || block == Blocks.GRAVEL && stack.is(Items.FLINT);
                    h.assertTrue(!stack.is(ModItems.ANCIENT_SHELL), "Digging never grants ancient shells");
                }
                h.assertTrue(vanilla, "Dirt/gravel retain their vanilla material drop on every roll");
            }
            h.assertTrue(shells > 40 && shells < 180, "Both successes and misses near the 10% drop rate");
        }
        h.succeed();
    }

    @GameTest(template = "empty")
    public void fellingRecoversAttachedBunchesAndCrownshaftSaplings(GameTestHelper h) {
        var base = tree(h);
        var anchor = base.above(6);
        var meal = new ItemStack(Items.BONE_MEAL);
        BoneMealItem.growCrop(meal, h.getLevel(), base);
        h.getLevel().destroyBlock(anchor, true);
        var area = new net.minecraft.world.phys.AABB(anchor).inflate(3);
        int bunches = h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class, area).stream()
                .filter(e -> e.getItem().is(ModBlocks.BETEL_NUT_CLUSTER_ITEM)).mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(bunches == 3, "Removing the supporting trunk drops every whole ripe bunch exactly once");
        var shaft = ModBlocks.BETEL_PALM_LOG.defaultBlockState().setValue(BetelPalmLogBlock.CROWNSHAFT, true);
        var loot = Block.getDrops(shaft, h.getLevel(), base.above(7), null, null, new ItemStack(Items.IRON_AXE));
        h.assertTrue(loot.stream().filter(s -> s.is(ModBlocks.BETEL_PALM_SAPLING_ITEM)).mapToInt(ItemStack::getCount).sum() == 1, "Crownshaft yields one sapling");
        h.assertTrue(loot.stream().filter(s -> s.is(ModBlocks.BETEL_PALM_LOG_ITEM)).mapToInt(ItemStack::getCount).sum() == 1, "Crownshaft retains its log drop");
        h.succeed();
    }
}
