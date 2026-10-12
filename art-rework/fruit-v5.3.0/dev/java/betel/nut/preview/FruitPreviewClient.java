package betel.nut.preview;

import betel.nut.BetelNutMod;
import betel.nut.block.BetelNutClusterBlock;
import betel.nut.block.ModBlocks;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.item.ModItems;
import betel.nut.worldgen.BetelPalmTreeGenerator;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.commands.arguments.EntityAnchorArgument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class FruitPreviewClient implements ClientModInitializer {
    private static final BlockPos BASE=new BlockPos(3,70,4),ANCHOR=BASE.above(6),GREEN=ANCHOR.north(),RIPE=ANCHOR.east();
    private int tick;private volatile RuntimeException failure;
    @Override public void onInitializeClient(){
        ClientTickEvents.END_CLIENT_TICK.register(c->{
            if(failure!=null)throw failure;if(c.player==null||c.level==null||c.getSingleplayerServer()==null)return;
            tick++;c.getToasts().clear();c.gui.clear();c.gui.getChat().clearMessages(false);
            if(tick==1){c.options.pauseOnLostFocus=false;c.options.guiScale().set(3);c.resizeDisplay();c.setScreen(null);server(c,p->{
                var w=p.serverLevel();w.setDayTime(6000);w.getGameRules().getRule(GameRules.RULE_DAYLIGHT).set(false,p.server);
                w.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,p.server);w.setWeatherParameters(0,12000,false,false);
                p.removeAllEffects();BetelNutEntityComponents.ADDICTION.get(p).clearAddiction(p);p.getInventory().clearContent();
                for(var entity:w.getEntitiesOfClass(ItemEntity.class,new AABB(BASE).inflate(20)))entity.discard();
                for(BlockPos pos:BlockPos.betweenClosed(BASE.offset(-7,0,-8),BASE.offset(7,15,7)))w.setBlock(pos,Blocks.AIR.defaultBlockState(),2);
                w.setBlock(BASE.below(),Blocks.DIRT.defaultBlockState(),3);
                require(BetelPalmTreeGenerator.generateVariant(w,BASE,RandomSource.create(3),9,9,0,false),"Preview tree grows");
                w.setBlock(GREEN,ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState().setValue(BetelNutClusterBlock.FACING,Direction.NORTH).setValue(BetelNutClusterBlock.AGE,1),3);
                w.setBlock(RIPE,ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState().setValue(BetelNutClusterBlock.FACING,Direction.EAST).setValue(BetelNutClusterBlock.AGE,2),3);
                for(BlockPos pos:BlockPos.betweenClosed(new BlockPos(-1,74,-2),new BlockPos(7,74,2)))w.setBlock(pos,Blocks.OAK_PLANKS.defaultBlockState(),3);
                for(BlockPos pos:BlockPos.betweenClosed(new BlockPos(6,74,3),new BlockPos(9,74,6)))w.setBlock(pos,Blocks.OAK_PLANKS.defaultBlockState(),3);
                p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();view(p,9,76,-3,Vec3.atCenterOf(ANCHOR));
            });}
            if(tick==30)capture(c,"green-and-yellow-on-tree.png");
            if(tick==40)server(c,p->{p.setGameMode(GameType.SURVIVAL);view(p,3.5,75,.5,new Vec3(3.5,76.3,4.05));hold(p,Items.SHEARS,InteractionHand.MAIN_HAND);});
            if(tick==50)capture(c,"green-before-clipping.png");if(tick==55)clip(c,GREEN,Direction.NORTH,new Vec3(3.5,76.3,4.05));
            if(tick==75)server(c,p->{require(p.serverLevel().getBlockState(GREEN).isAir(),"Actual green shearing packet removes the whole bunch");require(total(p,ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM)==1&&total(p,ModItems.RAW_BETEL_NUT)==0,"Actual green drop is one whole bunch");require(p.getMainHandItem().getDamageValue()==1,"Green clipping costs one durability");});
            if(tick==80)capture(c,"green-removed-no-stalk.png");
            if(tick==90)server(c,p->{p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();view(p,7,75,4.5,new Vec3(4.05,76.3,4.5));});
            if(tick==100)server(c,p->p.setGameMode(GameType.SURVIVAL));
            if(tick==105)clip(c,RIPE,Direction.EAST,new Vec3(4.05,76.3,4.5));
            if(tick==125)server(c,p->{require(p.serverLevel().getBlockState(RIPE).isAir(),"Actual ripe shearing packet removes whole bunch");require(total(p,ModBlocks.BETEL_NUT_CLUSTER_ITEM)==1&&p.getMainHandItem().getDamageValue()==2,"Ripe drop and exact shear cost");});
            if(tick==130)capture(c,"ripe-removed-no-stalk.png");
            if(tick==135)server(c,p->{
                var w=p.serverLevel();var random=RandomSource.create(5);for(int i=0;i<500;i++)w.getBlockState(ANCHOR).randomTick(w,ANCHOR,random);
                int count=0;for(Direction d:Direction.Plane.HORIZONTAL){var state=w.getBlockState(ANCHOR.relative(d));if(state.is(ModBlocks.BETEL_NUT_CLUSTER)){count++;require(state.getValue(BetelNutClusterBlock.AGE)==1,"New fruit is green");}}
                require(count==3,"Intact harvested tree regrows three green bunches without stalks");
                p.setGameMode(GameType.CREATIVE);p.getAbilities().flying=true;p.onUpdateAbilities();view(p,9,76,-3,Vec3.atCenterOf(ANCHOR));
            });
            if(tick==145)capture(c,"regrown-green-bunches.png");
            if(tick==155)server(c,p->{var w=p.serverLevel();var random=RandomSource.create(7);for(Direction d:Direction.Plane.HORIZONTAL){var pos=ANCHOR.relative(d);if(w.getBlockState(pos).is(ModBlocks.BETEL_NUT_CLUSTER)){for(int i=0;i<500;i++)w.getBlockState(pos).randomTick(w,pos,random);require(w.getBlockState(pos).getValue(BetelNutClusterBlock.AGE)==2,"Green bunch becomes yellow on the server");}}});
            if(tick==165)capture(c,"ripened-yellow-bunches.png");
            if(tick==175)server(c,p->{hold(p,ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM,InteractionHand.MAIN_HAND);p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.inventoryMenu.broadcastChanges();});
            if(tick==190)capture(c,"green-first-person-right.png");
            if(tick==200)server(c,p->{p.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);hold(p,ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM,InteractionHand.OFF_HAND);});
            if(tick==215)capture(c,"green-first-person-left.png");
            if(tick==225){c.options.setCameraType(CameraType.THIRD_PERSON_BACK);server(c,p->hold(p,ModBlocks.BETEL_NUT_CLUSTER_ITEM,InteractionHand.MAIN_HAND));}
            if(tick==240)capture(c,"both-bunches-third-person.png");
            if(tick==245){c.options.setCameraType(CameraType.FIRST_PERSON);server(c,p->{p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.getInventory().setItem(1,new ItemStack(ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM));p.getInventory().setItem(2,new ItemStack(ModBlocks.BETEL_PALM_SAPLING_ITEM));p.inventoryMenu.broadcastChanges();});}
            if(tick==255)c.setScreen(new InventoryScreen(c.player));if(tick==265)capture(c,"bunches-inventory.png");
            if(tick==275)c.setScreen(new BunchTooltipScreen());if(tick==285)capture(c,"bunch-names-and-uses.png");
            if(tick==300){BetelNutMod.LOGGER.info("FRUIT_CLIENT_ALL_CHECKS_PASS greenPacket=true ripePacket=true noStalk=true drops=true regrowth=true maturity=true heldModels=true");c.stop();}
        });
    }
    private static int total(ServerPlayer p,Item item){return p.getInventory().countItem(item)+p.serverLevel().getEntitiesOfClass(ItemEntity.class,new AABB(Vec3.atLowerCornerOf(BASE.offset(-8,-2,-8)),Vec3.atLowerCornerOf(BASE.offset(8,15,8)))).stream().filter(e->e.getItem().is(item)).mapToInt(e->e.getItem().getCount()).sum();}
    private static void hold(ServerPlayer p,Item item,InteractionHand hand){p.getInventory().selected=0;p.setItemInHand(hand,new ItemStack(item));p.inventoryMenu.broadcastChanges();}
    private static void view(ServerPlayer p,double x,double y,double z,Vec3 target){p.teleportTo(p.serverLevel(),x,y,z,Set.of(),0,0);p.lookAt(EntityAnchorArgument.Anchor.EYES,target);}
    private static void clip(Minecraft c,BlockPos pos,Direction face,Vec3 location){c.gameMode.useItemOn(c.player,InteractionHand.MAIN_HAND,new BlockHitResult(location,face,pos,false));c.player.swing(InteractionHand.MAIN_HAND);}
    private static void require(boolean good,String message){if(!good)throw new IllegalStateException(message);}
    private void server(Minecraft c,java.util.function.Consumer<ServerPlayer> action){c.getSingleplayerServer().execute(()->{try{action.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Exception e){failure=new RuntimeException("Fruit client verification failed",e);}});}
    private static void capture(Minecraft c,String name){Screenshot.grab(c.gameDirectory,name,c.getMainRenderTarget(),msg->BetelNutMod.LOGGER.info("FRUIT_SCREENSHOT {}",name));}
    private static final class BunchTooltipScreen extends Screen {
        BunchTooltipScreen(){super(Component.empty());}
        @Override public boolean isPauseScreen(){return false;}
        @Override public void render(GuiGraphics g,int mx,int my,float delta){
            Item[] items={ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM,ModBlocks.BETEL_NUT_CLUSTER_ITEM,ModBlocks.BETEL_PALM_SAPLING_ITEM};
            for(int i=0;i<items.length;i++){var stack=new ItemStack(items[i]);g.renderItem(stack,25,25+i*95);g.renderTooltip(font,stack,50,25+i*95);}
        }
    }
}
