package betel.nut.test;
import betel.nut.*;
import betel.nut.block.*;
import betel.nut.component.*;
import betel.nut.item.*;
import betel.nut.skyblock.*;
import betel.nut.client.addiction.ClientAddictionData;
import net.minecraft.client.*;
import net.minecraft.client.gui.screens.TitleScreen;
import net.minecraft.client.gui.screens.advancements.AdvancementsScreen;
import net.minecraft.core.*;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.*;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.level.block.*;
import net.minecraft.world.level.levelgen.*;
import net.minecraft.world.level.levelgen.presets.*;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;
import java.util.*;
import java.util.function.Consumer;
@EventBusSubscriber(modid=BetelNutMod.MOD_ID,value=Dist.CLIENT)
public final class PortSmokeClient {
 private static int phase,tick; private static boolean launched;
 private static volatile RuntimeException failure;
 private static String world="PortNormal";
 private static final BlockPos MARKER=new BlockPos(30,80,30);
 @SubscribeEvent public static void tick(ClientTickEvent.Post event) {
  if(!Boolean.getBoolean("betel.portSmoke"))return;
  if(failure!=null)throw failure;
  var c=Minecraft.getInstance();
  if(!launched&&c.screen!=null&&(c.screen instanceof TitleScreen||c.screen.getClass().getSimpleName().equals("AccessibilityOnboardingScreen"))&&c.getOverlay()==null){launched=true;create(c,"PortNormal",WorldPresets.FLAT);return;}
  if(c.player==null||c.level==null||c.getSingleplayerServer()==null||c.getOverlay()!=null)return;
  c.options.pauseOnLostFocus=false;c.getToasts().clear();tick++;
  if(phase==0)normal(c);
  else if(phase==1){
   if(tick==15)server(c,p->{require(BetelNutEntityComponents.ADDICTION.get(p).getAddictionValue()==4,"Native player attachment reload");require(p.serverLevel().getBlockState(MARKER).is(Blocks.DIAMOND_BLOCK),"Normal world edit survives reload");require(!BetelNutWorldComponents.SKYBLOCK_WORLD.get(p.serverLevel()).hasGeneratedBetelSkyIsland(),"Normal world never becomes skyblock");BetelNutMod.LOGGER.info("PORT_NORMAL_SAVE_RELOAD_PASS");});
   if(tick==30)next(c,2,"PortClassic",BetelNutMod.id("betel_skyblock"));
  }else if(phase==2||phase==4)skyblock(c,phase==4);
  else if(phase==3){
   if(tick==20)server(c,p->{var l=p.serverLevel();require(l.getBlockState(BetelSkyblockManager.getIslandCenter().offset(3,2,3)).is(Blocks.DIAMOND_BLOCK),"Classic edits survive reload");BetelNutMod.LOGGER.info("PORT_CLASSIC_SAVE_RELOAD_PASS");});
   if(tick==35)next(c,4,"PortOneBlock",BetelNutMod.id("betel_one_block_skyblock"));
  }else if(phase==5){
   if(tick==20)server(c,p->{var l=p.serverLevel();var center=BetelSkyblockManager.getOneBlockCenter();require(l.getBlockState(center).isAir(),"One-block center remains mined after reload");require(l.getBlockState(center.east()).is(Blocks.DIAMOND_BLOCK),"One-block player edits survive reload");require(p.getInventory().countItem(Items.SHEARS)==1,"One-block starter kit cannot repeat");BetelNutMod.LOGGER.info("PORT_ONE_BLOCK_SAVE_RELOAD_PASS");});
   if(tick==40){BetelNutMod.LOGGER.info("PORT_CLIENT_ALL_CHECKS_PASS");c.stop();}
  }
 }
 private static void normal(Minecraft c){
  if(tick==1){c.options.guiScale().set(2);c.options.renderDistance().set(8);c.resizeDisplay();c.setScreen(null);server(c,p->{
   var l=p.serverLevel();l.setDayTime(6000);l.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0,l.getServer());
   for(BlockPos b:BlockPos.betweenClosed(-24,79,-24,32,79,32))l.setBlock(b,Blocks.SMOOTH_STONE.defaultBlockState(),2);
   for(int i=0;i<4;i++){var base=new BlockPos(-12+i*8,80,-5);l.setBlock(base.below(),Blocks.DIRT.defaultBlockState(),3);require(betel.nut.worldgen.BetelPalmTreeGenerator.generate(l,base,RandomSource.create(101+i),9+i,9+i),"Palm variant grows");}
   l.setBlock(new BlockPos(0,81,-17),ModBlocks.BETEL_PALM_LEAVES.defaultBlockState().setValue(LeavesBlock.PERSISTENT,true),3);
   l.setBlock(new BlockPos(-3,80,-17),ModBlocks.BETEL_PALM_TRAPDOOR.defaultBlockState(),3);l.setBlock(new BlockPos(-3,80,-18),Blocks.MAGENTA_CONCRETE.defaultBlockState(),3);
   l.setBlock(new BlockPos(-5,80,-17),ModBlocks.BETEL_PALM_DOOR.defaultBlockState(),3);l.setBlock(new BlockPos(-5,81,-17),ModBlocks.BETEL_PALM_DOOR.defaultBlockState().setValue(DoorBlock.HALF,net.minecraft.world.level.block.state.properties.DoubleBlockHalf.UPPER),3);
   l.setBlock(MARKER,Blocks.DIAMOND_BLOCK.defaultBlockState(),3);
   p.setInvulnerable(true);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(l,17,88,22,Set.of(),147,7);
   BetelNutEntityComponents.ADDICTION.get(p).setAddictionValue(4);BetelNutEntityComponents.ADDICTION.get(p).setOralExposureDays(3);
  });}
  if(tick==40){require(ClientAddictionData.hasSyncedData()&&ClientAddictionData.getAddictionValue()==4,"Native payload synchronizes HUD");capture(c,"01-palms-hud.png");BetelNutMod.LOGGER.info("PORT_CLIENT_PAYLOAD_PASS");}
  if(tick==45)server(c,p->{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.BETEL_PALM_LEAVES));p.teleportTo(p.serverLevel(),.5,81,-12,Set.of(),180,12);p.inventoryMenu.broadcastChanges();});
  if(tick==65){var model=c.getItemRenderer().getModel(new ItemStack(ModBlocks.BETEL_PALM_LEAVES),c.level,c.player,0);int quads=model.getQuads(null,null,RandomSource.create(0)).size();require(quads>=600&&model.getClass().getSimpleName().equals("PalmModel"),"Inventory uses folded leaf mesh: "+quads);BetelNutMod.LOGGER.info("PORT_CLIENT_MESH_PASS quads={}",quads);capture(c,"02-leaves-placed-right-hand.png");}
  if(tick==70)c.gui.clear();
  if(tick==70)server(c,p->{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(Items.SHEARS));p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModBlocks.BETEL_PALM_LEAVES));p.inventoryMenu.broadcastChanges();});
  if(tick==85)capture(c,"03-leaves-left-hand.png");
  if(tick==90)c.options.setCameraType(CameraType.THIRD_PERSON_BACK);
  if(tick==110)capture(c,"04-leaves-third-person.png");
  if(tick==115){c.options.setCameraType(CameraType.FIRST_PERSON);server(c,p->{p.setItemInHand(InteractionHand.OFF_HAND,ItemStack.EMPTY);p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM));p.inventoryMenu.broadcastChanges();});}
  if(tick==130)capture(c,"05-green-fruit-held.png");
  if(tick==135)server(c,p->{p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModBlocks.BETEL_NUT_CLUSTER_ITEM));p.teleportTo(p.serverLevel(),-4,80,-12,Set.of(),180,8);p.inventoryMenu.broadcastChanges();});
  if(tick==155)capture(c,"06-ripe-fruit-door-trapdoor.png");
  if(tick==160)server(c,p->{var pos=new BlockPos(-3,80,-17);p.serverLevel().setBlock(pos,p.serverLevel().getBlockState(pos).setValue(TrapDoorBlock.OPEN,true),3);});
  if(tick==180)capture(c,"07-open-trapdoor.png");
  if(tick==190)server(c,p->{for(var a:p.server.getAdvancements().getAllAdvancements())if(a.id().getNamespace().equals(BetelNutMod.MOD_ID)&&a.value().display().isPresent())for(String criterion:a.value().criteria().keySet())p.getAdvancements().award(a,criterion);});
  if(tick==210){var data=c.player.connection.getAdvancements();c.setScreen(new AdvancementsScreen(data));data.setSelectedTab(data.getTree().get(BetelNutMod.id("root")).holder(),true); }
  if(tick==220)capture(c,"08a-main-advancements.png");
  if(tick==225){var data=c.player.connection.getAdvancements();data.setSelectedTab(data.getTree().get(BetelNutMod.id("skyblock/enter_betel_skyblock")).holder(),true);}
  if(tick==230)capture(c,"08b-skyblock-advancements.png");
  if(tick==235)c.setScreen(new betel.nut.client.gui.HudEditScreen());
  if(tick==250)capture(c,"09-hud-editor.png");
  if(tick==260){c.setScreen(null);reload(c,1);}
 }
 private static void skyblock(Minecraft c,boolean one){
  if(tick==30)server(c,p->{
   var l=p.serverLevel();var data=BetelNutWorldComponents.SKYBLOCK_WORLD.get(l);
   require(one?BetelSkyblockManager.isOneBlockEnabled(l):BetelSkyblockManager.isEnabled(l),"Native preset detection");
   require(one?data.hasGeneratedBetelOneBlockIsland():data.hasGeneratedBetelSkyIsland(),"Preset island generated once");
   var center=one?BetelSkyblockManager.getOneBlockCenter():BetelSkyblockManager.getIslandCenter();
   if(one){require(l.getBlockState(center).is(ModBlocks.BETEL_NUT_RESIDUE_BLOCK),"One-block preset starts with residue");require(p.getInventory().countItem(Items.SHEARS)==1,"Personal one-block starter kit");}
   else{l.setBlock(center.offset(3,2,3),Blocks.DIAMOND_BLOCK.defaultBlockState(),3);require(data.hasSurvivalSuppliesProvided(),"Classic kit flag persists");}
   p.setGameMode(GameType.CREATIVE);p.getAbilities().mayfly=true;p.getAbilities().flying=true;p.onUpdateAbilities();p.teleportTo(l,center.getX()+10,center.getY()+7,center.getZ()+13,Set.of(),142,23);
   BetelNutMod.LOGGER.info(one?"PORT_ONE_BLOCK_PRESET_PASS":"PORT_CLASSIC_PRESET_PASS");
  });
  if(tick==80)capture(c,one?"11-one-block-preset.png":"10-classic-preset.png");
  if(tick==90){if(one)server(c,p->{var center=BetelSkyblockManager.getOneBlockCenter();p.serverLevel().setBlock(center.east(),Blocks.DIAMOND_BLOCK.defaultBlockState(),3);p.serverLevel().removeBlock(center,false);});}
  if(tick==105)reload(c,one?5:3);
 }
 private static void create(Minecraft c,String name,ResourceKey<WorldPreset> preset){
  world=name;
  c.createWorldOpenFlows().createFreshLevel(name,new LevelSettings(name,GameType.SURVIVAL,false,Difficulty.NORMAL,true,new GameRules(),WorldDataConfiguration.DEFAULT),new WorldOptions(132026L,false,false),r->r.registryOrThrow(Registries.WORLD_PRESET).getHolderOrThrow(preset).value().createWorldDimensions(),new TitleScreen());
 }
 private static void next(Minecraft c,int next,String name,net.minecraft.resources.ResourceLocation preset){c.level.disconnect();c.disconnect();phase=next;tick=0;create(c,name,ResourceKey.create(Registries.WORLD_PRESET,preset));}
 private static void reload(Minecraft c,int next){c.level.disconnect();c.disconnect();phase=next;tick=0;c.createWorldOpenFlows().openWorld(world,()->{throw new IllegalStateException("Reopen failed");});}
 private static void server(Minecraft c,Consumer<ServerPlayer> task){c.getSingleplayerServer().execute(()->{try{task.accept(c.getSingleplayerServer().getPlayerList().getPlayers().getFirst());}catch(Throwable e){failure=new RuntimeException("Port client verification failed",e);}});}
 private static void capture(Minecraft c,String name){Screenshot.grab(c.gameDirectory,name,c.getMainRenderTarget(),message->BetelNutMod.LOGGER.info("PORT_SCREENSHOT {} {}",name,message.getString()));}
 private static void require(boolean pass,String reason){if(!pass)throw new IllegalStateException(reason);}
}
