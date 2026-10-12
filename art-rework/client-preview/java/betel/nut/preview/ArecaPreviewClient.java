package betel.nut.preview;

import betel.nut.BetelNutMod;
import betel.nut.block.BetelNutClusterBlock;
import betel.nut.block.ModBlocks;
import betel.nut.item.ModItems;
import betel.nut.worldgen.BetelPalmTreeGenerator;
import java.util.Set;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class ArecaPreviewClient implements ClientModInitializer {
 private static final BlockPos FRUIT = new BlockPos(-10,88,9);
 private int ticks;
 private boolean prepared;
 private int harvestCount;
 private int ripeCount;
 private volatile RuntimeException failure;
 @Override public void onInitializeClient() {
  if(Boolean.parseBoolean(System.getenv("BETEL_GROVE_PREVIEW"))) {
   new GrovePreviewClient().onInitializeClient();
   return;
  }
  if(Boolean.parseBoolean(System.getenv("BETEL_WOOD_PREVIEW"))) {
   new WoodPreviewClient().onInitializeClient();
   return;
  }
  ClientTickEvents.END_CLIENT_TICK.register(client -> {
   if(failure!=null) throw failure;
   if(client.player==null || client.level==null || client.getSingleplayerServer()==null) return;
   if(!prepared) {
    prepared=true;client.options.pauseOnLostFocus=false;client.setScreen(null);
    client.options.hideGui=true;client.options.guiScale().set(3);client.resizeDisplay();
    onServer(client,()->prepare(client));
   }
   ticks++;
   if(ticks==1 && Boolean.parseBoolean(System.getenv("BETEL_VISUAL_FIX_ONLY")))ticks=580;
   client.getToasts().clear();if(client.screen!=null)client.setScreen(null);
   if(ticks==180)capture(client,"areca-four-variants.png");
   if(ticks==200)onServer(client,()->{
    var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    player.teleportTo(player.serverLevel(),12,86,20,Set.of(),150.5f,-1f);
   });
   if(ticks==230)capture(client,"areca-v3-hero.png");
   if(ticks==235)onServer(client,()->{
    var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    player.teleportTo(player.serverLevel(),11.5,95,17,Set.of(),145.7f,14.1f);
   });
   if(ticks==250)capture(client,"areca-varied-crown.png");
   if(ticks==270) {
    client.options.hideGui=false;
    onServer(client,()->{
     var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
     player.setGameMode(GameType.SURVIVAL);player.getAbilities().mayfly=true;player.getAbilities().flying=true;player.onUpdateAbilities();
     player.getInventory().clearContent();player.getInventory().selected=0;
     player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));player.inventoryMenu.broadcastChanges();
     player.teleportTo(player.serverLevel(),-9.5,87.9,13,Set.of(),180,16.3f);
    });
   }
   if(ticks==300)capture(client,"areca-fruit-before.png");
   if(ticks==310 || ticks==370) {
    var result=client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,aimAtFruit(client));
    BetelNutMod.LOGGER.info("ART_HARVEST_CLIENT_INTERACTION {} {}",ticks,result);
   }
   if(ticks==340)onServer(client,()->verifyHarvest(client,false));
   if(ticks==350)capture(client,"areca-fruit-after.png");
   if(ticks==400) {
    if(client.level.getBlockState(FRUIT).getValue(BetelNutClusterBlock.AGE)!=0 || client.player.getMainHandItem().getDamageValue()!=1)
     throw new IllegalStateException("Client did not receive the authoritative harvest state");
    onServer(client,()->verifyHarvest(client,true));
   }
   if(ticks==420)onServer(client,()->{
    ripeCount=harvestCount;
    var level=client.getSingleplayerServer().overworld();
    level.setBlock(FRUIT,level.getBlockState(FRUIT).setValue(BetelNutClusterBlock.AGE,1),3);
   });
   if(ticks==450)capture(client,"areca-green-fruit-before.png");
   if(ticks==460 || ticks==520)client.gameMode.useItemOn(client.player,InteractionHand.MAIN_HAND,aimAtFruit(client));
   if(ticks==490 || ticks==550)onServer(client,()->{
    var level=client.getSingleplayerServer().overworld();var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(FRUIT).expandTowards(0,-16,0).inflate(3)).stream()
      .filter(e->e.getItem().is(ModItems.RAW_BETEL_NUT)).mapToInt(e->e.getItem().getCount()).sum();
    for(var stack:player.getInventory().items)if(stack.is(ModItems.RAW_BETEL_NUT))count+=stack.getCount();
    if(level.getBlockState(FRUIT).getValue(BetelNutClusterBlock.AGE)!=0 || player.getMainHandItem().getDamageValue()!=2 || count-ripeCount<2 || count-ripeCount>3)
      throw new IllegalStateException("Green harvest did not return 2-3 nuts with one durability cost");
    BetelNutMod.LOGGER.info("ART_GREEN_HARVEST_VERIFIED greenNuts={} shearDamage=2",count-ripeCount);
   });
   if(ticks==560) {
    if(client.level.getBlockState(FRUIT).getValue(BetelNutClusterBlock.AGE)!=0 || client.player.getMainHandItem().getDamageValue()!=2)
     throw new IllegalStateException("Client did not receive green harvest state");
   }
   if(ticks==580){client.options.hideGui=false;client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);}
   if(ticks==580)onServer(client,()->{
    var level=client.getSingleplayerServer().overworld();
    var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    player.setGameMode(GameType.CREATIVE);
    player.getInventory().clearContent();
    player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.BETEL_PALM_LEAVES));
    player.getInventory().setItem(1,new ItemStack(ModBlocks.BETEL_NUT_CLUSTER));
    player.getInventory().setItem(2,new ItemStack(ModBlocks.BETEL_PALM_TRAPDOOR));
    player.inventoryMenu.broadcastChanges();
    level.setBlock(new BlockPos(0,80,-17),ModBlocks.BETEL_PALM_LEAVES.defaultBlockState(),3);
    level.setBlock(new BlockPos(-3,79,-17),Blocks.MAGENTA_CONCRETE.defaultBlockState(),3);
    level.setBlock(new BlockPos(-3,80,-17),ModBlocks.BETEL_PALM_TRAPDOOR.defaultBlockState(),3);
    player.teleportTo(level,.5,80,-13,Set.of(),180,8);
   });
   if(ticks==620){
    var model=client.getItemRenderer().getModel(new ItemStack(ModBlocks.BETEL_PALM_LEAVES),client.level,client.player,0);
    int count=model.getQuads(null,null,RandomSource.create(0)).size();
    if(count<600 || !model.getClass().getSimpleName().equals("PalmModel"))
     throw new IllegalStateException("Inventory leaf model still uses the cube fallback: "+count);
    BetelNutMod.LOGGER.info("ART_ITEM_MESH_VERIFIED quads={} model={}",count,model.getClass().getSimpleName());
    capture(client,"fix-leaves-placed-and-held.png");
   }
   if(ticks==630)onServer(client,()->{
    var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.BETEL_NUT_CLUSTER));
    player.inventoryMenu.broadcastChanges();
   });
   if(ticks==660)capture(client,"fix-fruit-right-hand.png");
   if(ticks==680)onServer(client,()->{
    var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
    player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModBlocks.BETEL_NUT_CLUSTER));
    player.inventoryMenu.broadcastChanges();
   });
   if(ticks==710)capture(client,"fix-fruit-left-hand.png");
   if(ticks==720)onServer(client,()->{
    var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);player.inventoryMenu.broadcastChanges();
    player.teleportTo(player.serverLevel(),-2.5,81,-14.5,Set.of(),180,53);
   });
   if(ticks==750)capture(client,"fix-trapdoor-closed.png");
   if(ticks==760)onServer(client,()->{
    var level=client.getSingleplayerServer().overworld();var pos=new BlockPos(-3,80,-17);
    level.setBlock(pos,level.getBlockState(pos).setValue(TrapDoorBlock.OPEN,true),3);
    var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
    player.teleportTo(level,-2.5,80,-14,Set.of(),180,24);
   });
   if(ticks==790)capture(client,"fix-trapdoor-open.png");
   if(ticks==800){
    client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT);
    onServer(client,()->{
     var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
     player.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.BETEL_NUT_CLUSTER));
     player.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModBlocks.BETEL_PALM_LEAVES));
     player.inventoryMenu.broadcastChanges();
     player.teleportTo(player.serverLevel(),-8,80,-17,Set.of(),180,0);
    });
   }
   if(ticks==830)capture(client,"fix-third-person-items.png");
   if(ticks==840){
    client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON);
    onServer(client,()->{
     var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
     player.setItemInHand(InteractionHand.MAIN_HAND,ItemStack.EMPTY);
     player.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);player.inventoryMenu.broadcastChanges();
     player.teleportTo(player.serverLevel(),3.5,80,-14,Set.of(),135,16);
    });
   }
   if(ticks==870)capture(client,"fix-crown-solid-sides.png");
   if(ticks==890)client.stop();
  });
 }
 private static BlockHitResult aimAtFruit(Minecraft client) {
  Vec3 target=new Vec3(FRUIT.getX()+.5,FRUIT.getY()+.65,FRUIT.getZ()-.08);
  BlockHitResult hit=client.level.clip(new ClipContext(client.player.getEyePosition(),target,ClipContext.Block.OUTLINE,ClipContext.Fluid.NONE,client.player));
  if(!hit.getBlockPos().equals(FRUIT))throw new IllegalStateException("Close-fitting fruit cannot be targeted: "+hit.getBlockPos());
  return hit;
 }
 private void onServer(Minecraft client,Runnable task) {
  client.getSingleplayerServer().execute(()->{try{task.run();}catch(Exception e){failure=new RuntimeException("Art preview verification failed",e);}});
 }
 private void verifyHarvest(Minecraft client,boolean repeated) {
  var level=client.getSingleplayerServer().overworld();var player=client.getSingleplayerServer().getPlayerList().getPlayers().getFirst();
  int count=level.getEntitiesOfClass(ItemEntity.class,new AABB(FRUIT).expandTowards(0,-16,0).inflate(3)).stream()
    .filter(e->e.getItem().is(ModItems.RAW_BETEL_NUT)).mapToInt(e->e.getItem().getCount()).sum();
  for(var stack:player.getInventory().items)if(stack.is(ModItems.RAW_BETEL_NUT))count+=stack.getCount();
  int age=level.getBlockState(FRUIT).getValue(BetelNutClusterBlock.AGE),damage=player.getMainHandItem().getDamageValue();
  if(age!=0 || damage!=1 || count<4 || count>6 || (repeated && count!=harvestCount))
   throw new IllegalStateException("Unexpected harvest: age="+age+" damage="+damage+" count="+count);
  harvestCount=count;
  BetelNutMod.LOGGER.info("ART_HARVEST_VERIFIED serverAge={} shearDamage={} nuts={} repeatChecked={}",age,damage,count,repeated);
 }
 private static void prepare(Minecraft client) {
  var server=client.getSingleplayerServer();var level=server.overworld();
  level.setDayTime(6000);level.setWeatherParameters(0,12000,false,false);
  level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,server);
  // Repeated preview runs reuse a save; discard previous harvests before measuring this run.
  level.getEntitiesOfClass(ItemEntity.class,new AABB(-22,0,-22,23,104,23)).forEach(ItemEntity::discard);
  for(BlockPos p:BlockPos.betweenClosed(new BlockPos(-22,80,-22),new BlockPos(22,102,22)))level.setBlock(p,Blocks.AIR.defaultBlockState(),2);
  for(BlockPos p:BlockPos.betweenClosed(new BlockPos(-22,79,-22),new BlockPos(22,79,22)))level.setBlock(p,Blocks.GRASS_BLOCK.defaultBlockState(),2);
  int[][] trees={{-10,-7,9},{5,-7,12},{-10,8,11},{5,8,14}};
  for(int i=0;i<trees.length;i++){var t=trees[i];if(!BetelPalmTreeGenerator.generateVariant(level,new BlockPos(t[0],80,t[1]),RandomSource.create(i+21),t[2],t[2],i,i!=1))throw new IllegalStateException("Preview tree failed");}
  level.setBlock(FRUIT,ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState().setValue(BetelNutClusterBlock.FACING,Direction.SOUTH).setValue(BetelNutClusterBlock.AGE,2),3);
  var player=server.getPlayerList().getPlayers().getFirst();player.setGameMode(GameType.CREATIVE);
  player.getAbilities().flying=true;player.onUpdateAbilities();
  player.teleportTo(level,24,91,31,Set.of(),138.5f,6.6f);
 }
 private static void capture(Minecraft client,String name) {
  Screenshot.grab(client.gameDirectory,name,client.getMainRenderTarget(),message->BetelNutMod.LOGGER.info("ART_PREVIEW {} {}",name,message.getString()));
 }
}
