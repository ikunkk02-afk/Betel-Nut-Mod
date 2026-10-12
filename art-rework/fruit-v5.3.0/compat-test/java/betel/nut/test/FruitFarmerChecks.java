package betel.nut.test;

import betel.nut.block.ModBlocks;
import betel.nut.item.ModItems;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import vectorwing.farmersdelight.common.block.entity.CuttingBoardBlockEntity;

/** Real board interactions verify the optional knife route and foliage-only output. */
public final class FruitFarmerChecks {
    public static void run(GameTestHelper h) {
        var level=h.getLevel();var pos=h.absolutePos(new BlockPos(2,3,2));
        level.setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        level.setBlock(pos,vectorwing.farmersdelight.common.registry.ModBlocks.CUTTING_BOARD.get().defaultBlockState(),3);
        var board=(CuttingBoardBlockEntity)level.getBlockEntity(pos);
        var player=h.makeMockServerPlayerInLevel();player.setGameMode(GameType.SURVIVAL);
        var knife=new ItemStack(vectorwing.farmersdelight.common.registry.ModItems.IRON_KNIFE.get());
        player.setItemInHand(net.minecraft.world.InteractionHand.MAIN_HAND,knife);
        var materials=java.util.List.of(ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM,ModBlocks.BETEL_NUT_CLUSTER_ITEM,ModBlocks.BETEL_PALM_LEAVES_ITEM);
        for(int i=0;i<materials.size();i++) {
            for(var entity:level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3)))entity.discard();
            board.addItem(new ItemStack(materials.get(i)));
            h.assertTrue(!board.processStoredItemUsingTool(new ItemStack(Items.STICK),player),"Wrong cutting tool cannot process a bunch");
            h.assertTrue(board.getStoredItem().is(materials.get(i)),"Wrong tool consumes no ingredient");
            h.assertTrue(board.processStoredItemUsingTool(knife,player),"Actual knife processes the actual cutting board");
            h.assertTrue(board.isEmpty(),"Cutting consumes one complete input");
            int raw=0,leaf=0;
            for(var entity:level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(3))) {
                if(entity.getItem().is(ModItems.RAW_BETEL_NUT))raw+=entity.getItem().getCount();
                else if(entity.getItem().is(ModItems.BETEL_LEAF))leaf+=entity.getItem().getCount();
                else h.fail("Unexpected cutting output: "+entity.getItem());
            }
            h.assertTrue(raw==(i==0?3:i==1?5:0)&&leaf==(i==2?2:0),"Green yields 3, ripe yields 5, foliage yields only 2 leaves");
        }
        level.setBlock(pos,Blocks.AIR.defaultBlockState(),3);
    }
}
