package betel.nut.test;

import betel.nut.block.BetelNutClusterBlock;
import betel.nut.block.BetelPalmLeavesBlock;
import betel.nut.block.BetelPalmLogBlock;
import betel.nut.block.ModBlocks;
import betel.nut.item.ModItems;
import betel.nut.worldgen.BetelPalmTreeGenerator;
import java.util.HashSet;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import java.util.List;
import java.util.ArrayList;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BetelFruitGameTests implements FabricGameTest {
	private static BlockPos site(GameTestHelper h) {
		BlockPos base=h.absolutePos(new BlockPos(4,2,4));
		for(BlockPos p:BlockPos.betweenClosed(base.offset(-3,0,-3),base.offset(3,18,3))) h.getLevel().setBlock(p,Blocks.AIR.defaultBlockState(),2);
		h.getLevel().setBlock(base.below(),Blocks.DIRT.defaultBlockState(),3);
		return base;
	}
	private static BlockPos bunch(GameTestHelper h,int age) {
		BlockPos base=site(h);
		h.getLevel().setBlock(base,ModBlocks.BETEL_PALM_LOG.defaultBlockState(),3);
		BlockPos p=base.north();
		h.getLevel().setBlock(p,ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState().setValue(BetelNutClusterBlock.FACING,Direction.NORTH).setValue(BetelNutClusterBlock.AGE,age),3);
		return p;
	}
	private static int nuts(GameTestHelper h,BlockPos p) {
		return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(2)).stream().filter(e->e.getItem().is(ModItems.RAW_BETEL_NUT)).mapToInt(e->e.getItem().getCount()).sum();
	}
	private static int drops(GameTestHelper h,BlockPos p,Item item) {
		return h.getLevel().getEntitiesOfClass(ItemEntity.class,new AABB(p).inflate(2)).stream()
				.filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();
	}
	private static BlockHitResult hit(BlockPos p) {return new BlockHitResult(Vec3.atCenterOf(p),Direction.NORTH,p,false);}

	@GameTest(template=EMPTY_STRUCTURE)
	public void ripeShearingCannotRepeat(GameTestHelper h) {
		var level=h.getLevel(); var p=bunch(h,2); var player=h.makeMockServerPlayerInLevel();
		player.setGameMode(GameType.SURVIVAL); var shears=new ItemStack(Items.SHEARS);
		player.setItemInHand(InteractionHand.OFF_HAND,shears); var old=level.getBlockState(p);
		old.useItemOn(shears,level,player,InteractionHand.OFF_HAND,hit(p)); int count=drops(h,p,ModBlocks.BETEL_NUT_CLUSTER_ITEM);
		h.assertTrue(count==1&&nuts(h,p)==0,"Ripe harvest drops exactly one whole yellow bunch, no loose nuts");
		h.assertTrue(level.getBlockState(p).isAir(),"Harvest removes the whole bunch without a stalk");
		h.assertTrue(shears.getDamageValue()==1,"Exactly one offhand shear durability");
		old.useItemOn(shears,level,player,InteractionHand.OFF_HAND,hit(p));
		h.assertTrue(drops(h,p,ModBlocks.BETEL_NUT_CLUSTER_ITEM)==count&&shears.getDamageValue()==1,"Stale second interaction cannot duplicate drops or durability cost");
		h.assertTrue(level.getBlockState(p.south()).is(ModBlocks.BETEL_PALM_LOG),"Harvest preserves tree"); h.succeed();
	}
	@GameTest(template=EMPTY_STRUCTURE)
	public void wrongToolAndGreenHarvest(GameTestHelper h) {
		var level=h.getLevel();var p=bunch(h,1);var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);
		level.getBlockState(p).useItemOn(new ItemStack(Items.STICK),level,player,InteractionHand.MAIN_HAND,hit(p));
		h.assertTrue(nuts(h,p)==0&&level.getBlockState(p).getValue(BetelNutClusterBlock.AGE)==1,"Wrong tool does nothing");
		var shears=new ItemStack(Items.SHEARS);player.setItemInHand(InteractionHand.MAIN_HAND,shears);
		level.getBlockState(p).useItemOn(shears,level,player,InteractionHand.MAIN_HAND,hit(p));
		h.assertTrue(drops(h,p,ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM)==1&&nuts(h,p)==0&&shears.getDamageValue()==1,
				"Green bunch stays whole and damages mainhand shears once");
		h.assertTrue(level.getBlockState(p).isAir(),"No green stalk remains after harvest");h.succeed();
	}
	@GameTest(template=EMPTY_STRUCTURE)
	public void spentBunchRegrows(GameTestHelper h) {
		var level=h.getLevel();var base=site(h);
		h.assertTrue(BetelPalmTreeGenerator.generateVariant(level,base,RandomSource.create(2),9,9,0,false),"An initially fruitless tree grows");
		var anchor=base.above(6);var p=anchor.north();
		level.setBlock(p,ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState().setValue(BetelNutClusterBlock.FACING,Direction.NORTH).setValue(BetelNutClusterBlock.AGE,0),3);
		level.getBlockState(p).randomTick(level,p,RandomSource.create(17));
		h.assertTrue(level.getBlockState(p).isAir(),"Legacy empty stalk is removed on update");
		var random=RandomSource.create(17);
		for(int i=0;i<500;i++)level.getBlockState(anchor).randomTick(level,anchor,random);
		int count=0;
		for(Direction d:Direction.Plane.HORIZONTAL){var fruit=anchor.relative(d);var state=level.getBlockState(fruit);
			if(state.is(ModBlocks.BETEL_NUT_CLUSTER)){
				count++;h.assertTrue(state.getValue(BetelNutClusterBlock.AGE)==1,"Tree regrows a visible green bunch from empty space");
				for(int i=0;i<500;i++)level.getBlockState(fruit).randomTick(level,fruit,random);
				h.assertTrue(level.getBlockState(fruit).getValue(BetelNutClusterBlock.AGE)==2&&!level.getBlockState(fruit).isRandomlyTicking(),"Green fruit ripens and stops ticking");}}
		h.assertTrue(count==3,"A tree renews up to three bunches, without any retained fruit block");h.succeed();
	}
	@GameTest(template=EMPTY_STRUCTURE)
	public void fruitNeedsSupportInAllDirections(GameTestHelper h) {
		var level=h.getLevel();var base=site(h);level.setBlock(base,ModBlocks.BETEL_PALM_LOG.defaultBlockState(),3);
		for(Direction d:Direction.Plane.HORIZONTAL){var p=base.relative(d);var state=ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState().setValue(BetelNutClusterBlock.FACING,d).setValue(BetelNutClusterBlock.AGE,2);level.setBlock(p,state,3);h.assertTrue(state.canSurvive(level,p),"Fruit attaches in all directions");}
		level.setBlock(base,Blocks.AIR.defaultBlockState(),3);
		for(Direction d:Direction.Plane.HORIZONTAL)h.assertTrue(level.getBlockState(base.relative(d)).isAir(),"No floating fruit after support removal");h.succeed();
	}
	@GameTest(template=EMPTY_STRUCTURE)
	public void breakingFruitUsesLootTable(GameTestHelper h) {
		var p=bunch(h,2);h.getLevel().destroyBlock(p,true);
		h.assertTrue(drops(h,p,ModBlocks.BETEL_NUT_CLUSTER_ITEM)==1&&nuts(h,p)==0,"Ripe block break drops a whole ripe bunch");
		p=bunch(h,1);h.getLevel().destroyBlock(p,true);
		h.assertTrue(drops(h,p,ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM)==1&&nuts(h,p)==0,"Green block break drops a whole green bunch");h.succeed();
	}

	@GameTest(template=EMPTY_STRUCTURE)
	public void leavesYieldOnlyLeafMaterialAndKeepWholeBlockTools(GameTestHelper h) {
		var pos=site(h);var level=h.getLevel();var state=ModBlocks.BETEL_PALM_LEAVES.defaultBlockState();
		int leaves = 0;
		for(int i=0;i<256;i++) {
			var loot=Block.getDrops(state,level,pos,null,null,ItemStack.EMPTY);
			h.assertTrue(loot.isEmpty() || loot.size()==1&&loot.getFirst().is(ModItems.BETEL_LEAF)&&loot.getFirst().getCount()==1,
					"Foliage yields only leaf material, never loose fruit or saplings");
			leaves += loot.size();
		}
		h.assertTrue(leaves > 60 && leaves < 190, "Leaf drops include successes and misses");
		var loot=Block.getDrops(state,level,pos,null,null,new ItemStack(Items.SHEARS));
		h.assertTrue(loot.size()==1&&loot.getFirst().is(ModBlocks.BETEL_PALM_LEAVES_ITEM),"Shears preserve the building leaf block");
		var silk=new ItemStack(Items.DIAMOND_AXE);
		silk.enchant(level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.ENCHANTMENT)
				.getOrThrow(net.minecraft.world.item.enchantment.Enchantments.SILK_TOUCH),1);
		loot=Block.getDrops(state,level,pos,null,null,silk);
		h.assertTrue(loot.size()==1&&loot.getFirst().is(ModBlocks.BETEL_PALM_LEAVES_ITEM),"Silk Touch also preserves foliage for building");
		level.setBlock(pos,state.setValue(net.minecraft.world.level.block.LeavesBlock.DISTANCE,7),3);
		level.getBlockState(pos).randomTick(level,pos,RandomSource.create(1));
		h.assertTrue(level.getBlockState(pos).isAir()&&drops(h,pos,ModItems.BETEL_LEAF)<=1&&nuts(h,pos)==0,
				"Actual natural decay drops a leaf, never fruit");h.succeed();
	}

	@GameTest(template=EMPTY_STRUCTURE)
	public void fruitingNeedsIntactRootCrownAndClearSpace(GameTestHelper h) {
		var level=h.getLevel();var base=site(h);BetelPalmTreeGenerator.generateVariant(level,base,RandomSource.create(3),9,9,1,false);
		var anchor=base.above(6);h.assertTrue(BetelPalmLogBlock.isFruitingNode(level,anchor),"Existing tree geometry is sufficient, no new saved property is required");
		level.setBlock(base.above(2),Blocks.AIR.defaultBlockState(),3);
		h.assertTrue(!BetelPalmLogBlock.isFruitingNode(level,anchor),"Severed trunk cannot bear fruit");
		level.setBlock(base.above(2),ModBlocks.BETEL_PALM_LOG.defaultBlockState(),3);
		level.setBlock(base.above(9),Blocks.AIR.defaultBlockState(),3);
		h.assertTrue(!BetelPalmLogBlock.isFruitingNode(level,anchor),"Missing crown prevents regrowth");
		level.setBlock(base.above(9),ModBlocks.BETEL_PALM_LEAVES.defaultBlockState(),3);
		for(Direction d:Direction.Plane.HORIZONTAL)level.setBlock(anchor.relative(d),Blocks.STONE.defaultBlockState(),3);
		var random=RandomSource.create(1);for(int i=0;i<500;i++)level.getBlockState(anchor).randomTick(level,anchor,random);
		for(Direction d:Direction.Plane.HORIZONTAL)h.assertTrue(level.getBlockState(anchor.relative(d)).is(Blocks.STONE),"Regrowth cannot replace player blocks");
		level.setBlock(anchor.north(),Blocks.AIR.defaultBlockState(),3);
		for(int i=0;i<500;i++)level.getBlockState(anchor).randomTick(level,anchor,random);
		h.assertTrue(level.getBlockState(anchor.north()).is(ModBlocks.BETEL_NUT_CLUSTER),"A newly freed space grows fruit without a remaining stalk");
		h.assertTrue(!BetelPalmLogBlock.isFruitingNode(level,base)&&!level.getBlockState(base.above(7)).isRandomlyTicking(),"Plain and crownshaft logs are not fruiting nodes");h.succeed();
	}

	@GameTest(template=EMPTY_STRUCTURE)
	public void harvestedMaturitySurvivesPlacementAndCreativePick(GameTestHelper h) {
		var level=h.getLevel();var base=site(h);level.setBlock(base,ModBlocks.BETEL_PALM_LOG.defaultBlockState(),3);
		var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);
		for(int age=1;age<=2;age++) {
			Item item=age==1?ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM:ModBlocks.BETEL_NUT_CLUSTER_ITEM;
			var stack=new ItemStack(item);h.assertTrue(stack.get(DataComponents.BLOCK_STATE).get(BetelNutClusterBlock.AGE)==age,"Inventory default retains maturity");
			player.setItemInHand(InteractionHand.MAIN_HAND,stack);
			stack.useOn(new UseOnContext(player,InteractionHand.MAIN_HAND,new BlockHitResult(Vec3.atCenterOf(base),Direction.NORTH,base,false)));
			var pos=base.north();var state=level.getBlockState(pos);
			h.assertTrue(state.is(ModBlocks.BETEL_NUT_CLUSTER)&&state.getValue(BetelNutClusterBlock.AGE)==age&&stack.isEmpty(),"Actual BlockItem placement preserves green/yellow maturity");
			h.assertTrue(state.getBlock().getCloneItemStack(level,pos,state).is(item),"Creative pick selects the matching color item");
			level.removeBlock(pos,false);
		}
		h.assertTrue(!new ItemStack(ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM).getDescriptionId().equals(new ItemStack(ModBlocks.BETEL_NUT_CLUSTER_ITEM).getDescriptionId()),"Two bunches have distinct inventory names");h.succeed();
	}

	@GameTest(template=EMPTY_STRUCTURE)
	public void bunchProcessingAndYellowOnlySeedsUseActualRecipes(GameTestHelper h) throws Exception {
		var level=h.getLevel();
		for(Item item:List.of(ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM,ModBlocks.BETEL_NUT_CLUSTER_ITEM)) {
			var grid=CraftingInput.of(1,1,List.of(new ItemStack(item)));
			var holder=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).orElseThrow();
			var result=holder.value().assemble(grid,level.registryAccess());
			h.assertTrue(result.is(ModItems.RAW_BETEL_NUT)&&result.getCount()==(item==ModBlocks.BETEL_NUT_CLUSTER_ITEM?5:3),"One whole bunch gives exactly its approved raw yield");
			h.assertTrue(holder.value().getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty),"No reusable bunch remainder duplicates fruit");
		}
		var grid=CraftingInput.of(2,1,List.of(new ItemStack(ModBlocks.BETEL_NUT_CLUSTER_ITEM),new ItemStack(ModItems.BETEL_LEAF)));
		var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level).orElseThrow();
		var result=recipe.value().assemble(grid,level.registryAccess());
		h.assertTrue(result.is(ModBlocks.BETEL_PALM_SAPLING_ITEM)&&result.getCount()==1,"Yellow fruit plus leaf produces one sapling");
		for(Item item:List.of(ModItems.RAW_BETEL_NUT,ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM)) {
			var wrong=CraftingInput.of(2,1,List.of(new ItemStack(item),new ItemStack(ModItems.BETEL_LEAF)));
			h.assertTrue(!recipe.value().matches(wrong,level),"Green/loose fruit cannot bypass the yellow seed route");
		}
		if(net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("farmersdelight"))
			Class.forName("betel.nut.test.FruitFarmerChecks").getMethod("run",GameTestHelper.class).invoke(null,h);
		h.succeed();
	}
	@GameTest(template=EMPTY_STRUCTURE)
	public void repeatedWholeHarvestRegrowsWithoutAnyStalk(GameTestHelper h) {
		var level=h.getLevel();var base=site(h);BetelPalmTreeGenerator.generateVariant(level,base,RandomSource.create(5),9,9,0,true);
		var anchor=base.above(6);var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);
		var shears=new ItemStack(Items.SHEARS);player.setItemInHand(InteractionHand.MAIN_HAND,shears);
		var random=RandomSource.create(8);
		for(int cycle=0;cycle<3;cycle++) {
			int harvested=0;
			for(Direction direction:Direction.Plane.HORIZONTAL) {
				var fruit=anchor.relative(direction);var state=level.getBlockState(fruit);
				if(state.is(ModBlocks.BETEL_NUT_CLUSTER)) {
					state.useItemOn(shears,level,player,InteractionHand.MAIN_HAND,hit(fruit));harvested++;
					h.assertTrue(level.getBlockState(fruit).isAir(),"Every whole harvest removes its block immediately");
				}
			}
			h.assertTrue(harvested>0,"Every cycle has fruit to harvest");
			for(int i=0;i<500;i++)level.getBlockState(anchor).randomTick(level,anchor,random);
			int regrown=0;
			for(Direction direction:Direction.Plane.HORIZONTAL) {
				var fruit=anchor.relative(direction);var state=level.getBlockState(fruit);
				if(state.is(ModBlocks.BETEL_NUT_CLUSTER)) {
					regrown++;h.assertTrue(state.getValue(BetelNutClusterBlock.AGE)==1,"Replacement fruit always begins green");
					for(int i=0;i<500;i++)level.getBlockState(fruit).randomTick(level,fruit,random);
				}
			}
			h.assertTrue(regrown==3,"Completely harvested tree renews at most three bunches, on every cycle");
		}
		h.assertTrue(BetelPalmLogBlock.isFruitingNode(level,anchor),"Repeated harvest preserves the living tree");h.succeed();
	}

	@GameTest(template=EMPTY_STRUCTURE)
	public void greenFruitMaturesOnlyWithEnoughLight(GameTestHelper h) {
		var level=h.getLevel();var pos=bunch(h,1);
		for(BlockPos wall:BlockPos.betweenClosed(pos.offset(-2,-2,-2),pos.offset(2,2,2)))
			if(Math.abs(wall.getX()-pos.getX())==2||Math.abs(wall.getY()-pos.getY())==2||Math.abs(wall.getZ()-pos.getZ())==2)
				level.setBlock(wall,Blocks.STONE.defaultBlockState(),3);
		// Lighting engine must finish applying the opaque shell before the random-growth check.
		h.runAfterDelay(5,()->{
			h.assertTrue(level.getMaxLocalRawBrightness(pos)<9,"Enclosed test fruit is actually dark");
			var random=RandomSource.create(8);for(int i=0;i<500;i++)level.getBlockState(pos).randomTick(level,pos,random);
			h.assertTrue(level.getBlockState(pos).getValue(BetelNutClusterBlock.AGE)==1,"Darkness prevents ripening");
			level.setBlock(pos.below(),Blocks.GLOWSTONE.defaultBlockState(),3);
			h.runAfterDelay(5,()->{
				h.assertTrue(level.getMaxLocalRawBrightness(pos)>=9,"Glowstone supplies actual growth light");
				for(int i=0;i<500;i++)level.getBlockState(pos).randomTick(level,pos,random);
				h.assertTrue(level.getBlockState(pos).getValue(BetelNutClusterBlock.AGE)==2,"Added light allows real green-to-yellow growth");h.succeed();
			});
		});
	}

	@GameTest(template=EMPTY_STRUCTURE)
	public void fourProfilesHaveVariedContinuousFronds(GameTestHelper h) {
		var level=h.getLevel();var fingerprints=new HashSet<String>();
		for(int profile=0;profile<4;profile++){
			var base=site(h);h.assertTrue(BetelPalmTreeGenerator.generateVariant(level,base,RandomSource.create(2),9,9,profile,true),"All profiles grow");
			var crown=base.above(9);var styles=new HashSet<Integer>();var fingerprint=new StringBuilder();
			for(Direction d:Direction.Plane.HORIZONTAL){int style=level.getBlockState(crown.relative(d)).getValue(BetelPalmLeavesBlock.STYLE);styles.add(style);fingerprint.append(style);
				for(int segment=2;segment<=3;segment++)h.assertTrue(level.getBlockState(crown.relative(d,segment)).getValue(BetelPalmLeavesBlock.STYLE)==style,"Segments preserve their frond curve");}
			fingerprints.add(fingerprint.toString());h.assertTrue(styles.size()>1,"Each crown mixes different fronds");
			int fruit=0;for(Direction d:Direction.Plane.HORIZONTAL)if(level.getBlockState(base.above(6).relative(d)).is(ModBlocks.BETEL_NUT_CLUSTER))fruit++;
			h.assertTrue(fruit>=1&&fruit<=3,"Fruit grows below the green crownshaft");
		}
		h.assertTrue(fingerprints.size()==4,"Four distinct crown profiles");h.succeed();
	}
}
