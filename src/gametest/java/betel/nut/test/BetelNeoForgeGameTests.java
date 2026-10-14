package betel.nut.test;
import betel.nut.*;
import betel.nut.block.*;
import betel.nut.component.*;
import betel.nut.item.*;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.*;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.*;
import net.minecraft.world.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.*;
import net.minecraft.world.item.crafting.*;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.*;
import net.neoforged.fml.ModList;
import net.neoforged.neoforge.gametest.*;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.common.util.FakePlayerFactory;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;
@GameTestHolder("betel_nut_mod") @PrefixGameTestTemplate(false)
public final class BetelNeoForgeGameTests {
 private static ServerPlayer player(GameTestHelper h) {
  return new ServerPlayer(h.getLevel().getServer(), h.getLevel(), new com.mojang.authlib.GameProfile(UUID.randomUUID(),"port-test"), net.minecraft.server.level.ClientInformation.createDefault());
 }
 @GameTest(template="empty") public void nativeAttachmentsSurviveEntitySaveAndClone(GameTestHelper h) {
  var p=player(h);var a=BetelNutEntityComponents.ADDICTION.get(p);a.setAddictionValue(7);a.setOralExposureDays(6);
  BetelNutEntityComponents.SKYBLOCK_PLAYER.get(p).markReceivedBetelOneBlockStarterItems();
  var nbt=new CompoundTag();p.saveWithoutId(nbt);var loaded=player(h);loaded.load(nbt);
  h.assertTrue(BetelNutEntityComponents.ADDICTION.get(loaded).getAddictionValue()==7 && BetelNutEntityComponents.ADDICTION.get(loaded).getOralExposureDays()==6,"Entity serialization saves native attachments");
  h.assertTrue(BetelNutEntityComponents.SKYBLOCK_PLAYER.get(loaded).hasReceivedBetelOneBlockStarterItems(),"Starter ownership survives native entity save");
  var next=player(h);NeoForge.EVENT_BUS.post(new PlayerEvent.Clone(next,p,true));
  h.assertTrue(BetelNutEntityComponents.ADDICTION.get(next).getAddictionValue()==7 && BetelNutEntityComponents.ADDICTION.get(next).getOralExposureDays()==6,"Death keeps dependence and exposure rules");
  h.assertTrue(BetelNutEntityComponents.SKYBLOCK_PLAYER.get(next).hasReceivedBetelOneBlockStarterItems(),"Death cannot repeat starter kit");h.succeed();
 }
 @GameTest(template="empty") public void skyblockSavedDataSurvivesDiskReadback(GameTestHelper h) throws Exception {
  var level=h.getLevel();var data=BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);var before=new CompoundTag();data.writeToNbt(before,level.registryAccess());
  try {
   data.markSurvivalSuppliesProvided();var store=level.getDataStorage();store.save(); net.neoforged.neoforge.common.IOUtilities.waitUntilIOWorkerComplete();
   var field=store.getClass().getDeclaredField("cache");field.setAccessible(true);
   ((Map<?,?>)field.get(store)).remove("betel_nut_mod_skyblock");
   var loaded=BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
   h.assertTrue(loaded!=data && loaded.hasSurvivalSuppliesProvided(),"Native world data is read from disk");
  } finally {BetelNutWorldComponents.SKYBLOCK_WORLD.get(level).readFromNbt(before,level.registryAccess());}
  h.succeed();
 }
 @GameTest(template="empty") public void optionalCreateRecipesLoadOnlyWithCreate(GameTestHelper h) throws Exception {
  var recipes=h.getLevel().getRecipeManager().getRecipes().stream().filter(r->r.id().getNamespace().equals(BetelNutMod.MOD_ID)&&r.id().getPath().startsWith("compat/create/")).toList();
  if(!ModList.get().isLoaded("create")){h.assertTrue(recipes.isEmpty(),"No unknown Create serializer without Create");h.succeed();return;}
  Class.forName("betel.nut.test.CreateRecipeChecks").getMethod("validateAll",GameTestHelper.class).invoke(null,h);
  h.succeed();
 }
 @GameTest(template="empty") @SuppressWarnings({"rawtypes","unchecked"}) public void createSawAndMillUseRealRecipeInputs(GameTestHelper h) {
  if(!ModList.get().isLoaded("create")){h.succeed();return;}
  for(var spec:List.of(new Object[]{"cutting_ripe_cluster",ModBlocks.BETEL_NUT_CLUSTER_ITEM,ModItems.RAW_BETEL_NUT,5},new Object[]{"cutting_unripe_cluster",ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM,ModItems.RAW_BETEL_NUT,3},new Object[]{"milling_shell",ModItems.SHELL,ModItems.SHELL_POWDER,4})) {
   var holder=h.getLevel().getRecipeManager().byKey(BetelNutMod.id("compat/create/"+spec[0])).orElseThrow();Recipe r=holder.value();
   var inv=new net.neoforged.neoforge.items.ItemStackHandler(1);inv.setStackInSlot(0,new ItemStack((Item)spec[1]));var input=new net.neoforged.neoforge.items.wrapper.RecipeWrapper(inv);
   h.assertTrue(r.matches(input,h.getLevel()),"Actual machine input matches "+spec[0]);
   var result=r.assemble(input,h.getLevel().registryAccess());h.assertTrue(result.is((Item)spec[2])&&result.getCount()==(int)spec[3],"Exact machine output "+spec[0]);
   inv.setStackInSlot(0,new ItemStack(Items.DIRT));h.assertTrue(!r.matches(input,h.getLevel()),"Wrong input cannot craft");
  }h.succeed();
 }
 @GameTest(template="empty") public void deployerShearsHarvestOnceForBothMaturities(GameTestHelper h) throws Exception {
  ServerPlayer p=ModList.get().isLoaded("create")?(ServerPlayer)Class.forName("com.simibubi.create.content.kinetics.deployer.DeployerFakePlayer").getConstructor(ServerLevel.class,UUID.class).newInstance(h.getLevel(),UUID.randomUUID()):FakePlayerFactory.getMinecraft(h.getLevel());
  var level=h.getLevel();var pos=h.absolutePos(new BlockPos(3,2,3));p.getAbilities().mayBuild=true;p.getAbilities().instabuild=false;
  for(int age:new int[]{1,2}) {
   level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4)).forEach(ItemEntity::discard);
   level.setBlock(pos.south(),ModBlocks.BETEL_PALM_LOG.defaultBlockState(),3);
   var state=ModBlocks.BETEL_NUT_CLUSTER.defaultBlockState().setValue(BetelNutClusterBlock.AGE,age);level.setBlock(pos,state,3);
   var shears=new ItemStack(Items.SHEARS);p.setItemInHand(InteractionHand.MAIN_HAND,shears);
   var hit=new BlockHitResult(Vec3.atCenterOf(pos),Direction.NORTH,pos,false);
   state.useItemOn(shears,level,p,InteractionHand.MAIN_HAND,hit);state.useItemOn(shears,level,p,InteractionHand.MAIN_HAND,hit);
   var drops=level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(4));
   h.assertTrue(level.getBlockState(pos).isAir()&&shears.getDamageValue()==1&&drops.size()==1,"Automation removes once and charges one durability");
   h.assertTrue(drops.getFirst().getItem().is(age==1?ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM:ModBlocks.BETEL_NUT_CLUSTER_ITEM),"Automation preserves maturity");
  }h.succeed();
 }

 private static void createCheck(GameTestHelper h,String method) throws Exception {
  if(ModList.get().isLoaded("create"))Class.forName("betel.nut.test.CreateRecipeChecks").getMethod(method,GameTestHelper.class).invoke(null,h);
  h.succeed();
 }
 @GameTest(template="empty") public void basinsRequireHeatFluidAndPreparedBases(GameTestHelper h)throws Exception{createCheck(h,"basins");}
 @GameTest(template="empty") public void honeyCoatingUsesActualSpoutAndExactFluid(GameTestHelper h)throws Exception{createCheck(h,"spout");}
 @GameTest(template="empty") public void leafWrappingRequiresOrderedDeployerSteps(GameTestHelper h)throws Exception{createCheck(h,"wrapping");}
 @GameTest(template="empty") public void mechanicalAssemblyRequiresSealsAndRareMaterials(GameTestHelper h)throws Exception{createCheck(h,"crafting");}
 @GameTest(template="empty") public void genericAutomationCannotBypassDedicatedRoutes(GameTestHelper h)throws Exception{createCheck(h,"automation");}
 @GameTest(template="empty") public void waterFanSeparatesShellsAndPreservesGravelProducts(GameTestHelper h)throws Exception{createCheck(h,"washing");}
}
