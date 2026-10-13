package betel.nut.test;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.component.BetelNutAddictionComponent;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.effect.ModEffects;
import betel.nut.event.WithdrawalEatingRestrictions;
import betel.nut.item.ModItems;
import betel.nut.item.RecoveryDrinkItem;
import java.util.ArrayList;
import java.util.List;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.neoforged.fml.ModList;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CakeBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

@GameTestHolder("betel_nut_mod")
@PrefixGameTestTemplate(false)
public final class BetelRecoveryGameTests {
    private static ServerPlayer player(GameTestHelper h) {
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "recovery-test"),false);
        var p=new ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(GameType.SURVIVAL);p.getFoodData().setFoodLevel(6);return p;
    }
    private static boolean allowed(ServerPlayer p, Item item) {
        return !WithdrawalEatingRestrictions.shouldBlockUse(p,new ItemStack(item),false);
    }
    private static void disease(ServerPlayer p,int days) {
        var s=BetelNutEntityComponents.ADDICTION.get(p);s.setOralExposureDays(days);s.maintainOralEffects(p);
    }
    private static ItemStack drink(ServerPlayer p,Item item,int count) {
        p.getFoodData().setFoodLevel(6);
        var stack=new ItemStack(item,count);p.setItemInHand(InteractionHand.MAIN_HAND,stack);
        var result=stack.finishUsingItem(p.level(),p);p.setItemInHand(InteractionHand.MAIN_HAND,result);return result;
    }

    @GameTest(template="empty")
    public void stageTwoSoftFoodsCannotAdmitGoldenApplesOrBetel(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);disease(p,5);
        s.applyMilkRelief(p,p.level().getGameTime());
        h.assertTrue(s.getCleanTime()>p.level().getGameTime(),"Test disease restriction during temporary relief");
        for(Item item:List.of(Items.BREAD,Items.BAKED_POTATO,Items.PUMPKIN_PIE,Items.CAKE,Items.MUSHROOM_STEW,Items.HONEY_BOTTLE,
                ModItems.GOLDEN_APPLE_PASTE,ModItems.ENCHANTED_GOLDEN_APPLE_PASTE,ModItems.RELIEF_POTION))
            h.assertTrue(allowed(p,item),"Stage II allows soft food or medicine: "+item);
        for(Item item:List.of(Items.GOLDEN_APPLE,Items.ENCHANTED_GOLDEN_APPLE,Items.APPLE,Items.COOKED_BEEF,Items.CARROT,
                ModItems.ROASTED_BETEL_NUT,ModItems.WRAPPED_RAW_BETEL,ModItems.SWEET_BETEL_NUT)) {
            h.assertTrue(!allowed(p,item),"Stage II rejects hard food: "+item);
            ItemStack stack=new ItemStack(item,2);stack.finishUsingItem(p.level(),p);
            h.assertTrue(stack.getCount()==2,"Rejected food cannot finish consuming");
        }
        s.clearAddiction(p);h.succeed();
    }

    @GameTest(template="empty")
    public void stageThreeAndStandaloneJawBlockEveryOrdinaryFood(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);
        for(int scenario=0;scenario<3;scenario++) {
            s.clearAddiction(p);p.removeAllEffects();
            if(scenario==0)disease(p,7);
            if(scenario==1)p.addEffect(new MobEffectInstance(ModEffects.MOUTH_LOCKED,200));
            if(scenario==2)p.addEffect(new MobEffectInstance(ModEffects.ORAL_CANCER,200,2));
            h.assertTrue(WithdrawalEatingRestrictions.getRestrictionLevel(p,s)==WithdrawalEatingRestrictions.RestrictionLevel.MOUTH_LOCKED,
                    "Disease III or standalone status locks food immediately");
            for(Item item:BuiltInRegistries.ITEM) {
                var stack=new ItemStack(item);
                if(WithdrawalEatingRestrictions.isFoodItem(stack) && !(item instanceof RecoveryDrinkItem))
                    h.assertTrue(!allowed(p,item),"Every ordinary food is blocked, including liquid foods: "+item);
            }
            for(Item item:List.of(ModItems.GOLDEN_APPLE_PASTE,ModItems.ENCHANTED_GOLDEN_APPLE_PASTE,ModItems.RELIEF_POTION,Items.MILK_BUCKET,Items.POTION))
                h.assertTrue(allowed(p,item),"Drinkable recovery remains usable");
        }
        p.removeAllEffects();s.clearAddiction(p);h.succeed();
    }

    @GameTest(template="empty")
    public void fiveRealEatingDatesStopFurtherChewingAtStageTwo(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);var world=p.server.overworld();long before=world.getDayTime();
        try {
            world.setDayTime(3000000000L);s.beginCalendarSession(p);
            for(int day=0;day<5;day++) {
                world.setDayTime(3000000000L+day*24000L);drink(p,ModItems.WRAPPED_RAW_BETEL,1);
                h.assertTrue(s.getAddictionValue()==day+1&&s.getOralExposureDays()==day+1,"Each actual eating date counts once until soft-food restriction");
            }
            world.setDayTime(3000000000L+5*24000L);
            h.assertTrue(drink(p,ModItems.WRAPPED_RAW_BETEL,1).getCount()==1,"Sixth date cannot force chewing past cancer II");
            h.assertTrue(s.getAddictionValue()==5&&s.getOralExposureDays()==5,"Blocked chewing adds no dependence or exposure");
        } finally {world.setDayTime(before);s.clearAddiction(p);}
        h.succeed();
    }

    @GameTest(template="empty")
    public void lockedJawDuringUseAndPlacedCakeCannotBypassGate(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);disease(p,5);
        var bread=new ItemStack(Items.BREAD,2);p.setItemInHand(InteractionHand.MAIN_HAND,bread);
        h.assertTrue(bread.use(p.level(),p,InteractionHand.MAIN_HAND).getResult().consumesAction(),"Bread can start at II");
        disease(p,7);int food=p.getFoodData().getFoodLevel();bread.finishUsingItem(p.level(),p);
        h.assertTrue(bread.getCount()==2&&p.getFoodData().getFoodLevel()==food,"A restriction arising during animation prevents the final bite");
        BlockPos pos=h.absolutePos(new BlockPos(2,2,2));h.getLevel().setBlock(pos.below(),Blocks.STONE.defaultBlockState(),3);
        for(var block:List.of(Blocks.CAKE,Blocks.CANDLE_CAKE)) {
            h.getLevel().setBlock(pos,block.defaultBlockState(),3);
            var state=h.getLevel().getBlockState(pos);
            state.useWithoutItem(h.getLevel(),p,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
            h.assertTrue(h.getLevel().getBlockState(pos).equals(state)&&p.getFoodData().getFoodLevel()==food,"Placed cake and candle cake cannot bypass locked jaw");
        }
        disease(p,5);h.getLevel().setBlock(pos,Blocks.CAKE.defaultBlockState(),3);
        h.getLevel().getBlockState(pos).useWithoutItem(h.getLevel(),p,new BlockHitResult(Vec3.atCenterOf(pos),Direction.UP,pos,false));
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(CakeBlock.BITES)==1,"Soft cake remains edible at stage II");
        h.getLevel().setBlock(pos,Blocks.AIR.defaultBlockState(),3);s.clearAddiction(p);h.succeed();
    }

    @GameTest(template="empty")
    public void pastesKeepVanillaBuffsAndApplyRecoveryExactlyOnce(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);s.setAddictionValue(7);disease(p,7);
        long time=p.level().getGameTime();
        h.assertTrue(drink(p,ModItems.GOLDEN_APPLE_PASTE,1).is(Items.GLASS_BOTTLE),"Last paste returns exactly one bottle");
        h.assertTrue(s.getAddictionValue()==6&&s.getOralExposureDays()==7,"Golden paste removes one dependence day but does not invent oral healing");
        h.assertTrue(p.hasEffect(MobEffects.REGENERATION)&&p.hasEffect(MobEffects.ABSORPTION),"Golden paste keeps vanilla golden apple buffs");
        h.assertTrue(drink(p,ModItems.ENCHANTED_GOLDEN_APPLE_PASTE,1).is(Items.GLASS_BOTTLE),"Enchanted paste also returns its bottle");
        h.assertTrue(s.getAddictionValue()==1&&s.getOralExposureDays()==0&&!p.hasEffect(ModEffects.MOUTH_LOCKED),"Enchanted recovery runs once and releases the jaw immediately");
        h.assertTrue(p.hasEffect(MobEffects.FIRE_RESISTANCE)&&p.hasEffect(MobEffects.DAMAGE_RESISTANCE),"Enchanted paste keeps vanilla enchanted apple buffs");
        h.assertTrue(s.getCleanTime()>=time+BetelNutConfig.get().enchantedGoldenAppleCleanTimeTicks,"Enchanted recovery protection survives");
        for(Item item:List.of(ModItems.GOLDEN_APPLE_PASTE,ModItems.ENCHANTED_GOLDEN_APPLE_PASTE,ModItems.RELIEF_POTION))
            h.assertTrue(new ItemStack(item).getUseAnimation()==UseAnim.DRINK&&new ItemStack(item).getUseDuration(p)==32,"All remedies drink in 32 ticks");
        h.assertTrue(new ItemStack(ModItems.ENCHANTED_GOLDEN_APPLE_PASTE).hasFoil(),"Enchanted paste has a real enchantment glint");
        s.clearAddiction(p);h.succeed();
    }

    @GameTest(template="empty")
    public void stackedPastesAndFullInventoryReturnOnlyOneBottle(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);s.setAddictionValue(7);disease(p,7);
        p.getInventory().clearContent();
        var result=drink(p,ModItems.GOLDEN_APPLE_PASTE,2);
        h.assertTrue(result.is(ModItems.GOLDEN_APPLE_PASTE)&&result.getCount()==1,"Only one paste is consumed from a stack");
        int bottles=p.getInventory().items.stream().filter(x->x.is(Items.GLASS_BOTTLE)).mapToInt(ItemStack::getCount).sum();
        h.assertTrue(bottles==1&&s.getAddictionValue()==6,"One empty bottle and one recovery per serving");
        for(int i=0;i<p.getInventory().items.size();i++)p.getInventory().items.set(i,new ItemStack(Items.STONE,64));
        p.setPos(Vec3.atCenterOf(h.absolutePos(new BlockPos(1,2,1))));
        long before=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(3)).stream().filter(x->x.getItem().is(Items.GLASS_BOTTLE)).mapToInt(x->x.getItem().getCount()).sum();
        drink(p,ModItems.GOLDEN_APPLE_PASTE,2);
        long after=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(3)).stream().filter(x->x.getItem().is(Items.GLASS_BOTTLE)).mapToInt(x->x.getItem().getCount()).sum();
        h.assertTrue(after==before+1,"Full inventory drops exactly one bottle");
        p.setGameMode(GameType.CREATIVE);result=drink(p,ModItems.GOLDEN_APPLE_PASTE,2);
        h.assertTrue(result.getCount()==2,"Creative use does not consume remedies");
        s.clearAddiction(p);h.succeed();
    }

    @GameTest(template="empty")
    public void affordableReliefIsGradualDailyAndPersistsThroughReloadAndDeath(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);var world=p.server.overworld();long before=world.getDayTime();
        try {
            world.setDayTime(3100000000L);s.setAddictionValue(7);disease(p,7);s.setWithdrawalValue(p,100);
            h.assertTrue(drink(p,ModItems.RELIEF_POTION,1).is(Items.GLASS_BOTTLE),"First daily relief is consumed");
            h.assertTrue(s.getAddictionValue()==6&&s.getOralExposureDays()==6&&s.getWithdrawalValue()==0,"Cheap remedy removes one day and current withdrawal");
            h.assertTrue(s.getCleanTime()>=p.level().getGameTime()+2400&&!p.hasEffect(ModEffects.MOUTH_LOCKED),"Two-minute relief immediately releases stage III into stage II");
            h.assertTrue(!allowed(p,Items.GOLDEN_APPLE)&&allowed(p,Items.BREAD),"Soft-food restriction still applies after partial recovery");
            h.assertTrue(drink(p,ModItems.RELIEF_POTION,2).getCount()==2&&s.getOralExposureDays()==6,"Repeat bottle neither consumes nor heals");
            var stack=new ItemStack(ModItems.RELIEF_POTION);p.setItemInHand(InteractionHand.OFF_HAND,stack);
            h.assertTrue(stack.use(p.level(),p,InteractionHand.OFF_HAND).getResult()==net.minecraft.world.InteractionResult.FAIL,"Repeat use is blocked at the start as well");
            CompoundTag saved=new CompoundTag();s.writeToNbt(saved,p.registryAccess());var restored=new BetelNutAddictionComponent(p);restored.readFromNbt(saved,p.registryAccess());
            h.assertTrue(!restored.canTakeReliefPotion(p),"Save/load preserves daily dose");
            var next=player(h);var copied=BetelNutEntityComponents.ADDICTION.get(next);copied.copyForRespawn(s,false);
            h.assertTrue(copied.hasTakenReliefToday(next),"Death preserves daily dose");
            world.setDayTime(3100000000L-24000);h.assertTrue(!s.canTakeReliefPotion(p),"Clock rollback cannot reset dose");
            world.setDayTime(3100000000L+24000);drink(p,ModItems.RELIEF_POTION,1);
            h.assertTrue(s.getOralExposureDays()==5&&s.getAddictionValue()==5,"A new day permits one more gradual step");
            world.setDayTime(3100000000L+48000);drink(p,ModItems.RELIEF_POTION,1);
            h.assertTrue(s.getOralExposureDays()==4&&s.getOralCancerStage()==1&&allowed(p,Items.GOLDEN_APPLE),"Three daily doses reopen ordinary food through gradual recovery");
            CompoundTag old=new CompoundTag();old.putInt("dailySchema",2);old.putInt("addictionValue",2);
            restored.readFromNbt(old,p.registryAccess());h.assertTrue(restored.canTakeReliefPotion(p),"Old saves without dose field get their first dose");
            s.clearAddiction(p);h.assertTrue(!s.canTakeReliefPotion(p),"Healthy players do not waste medicine");copied.clearAddiction(next);
        } finally {world.setDayTime(before);s.clearAddiction(p);}
        h.succeed();
    }

    public static List<RecipeHolder<?>> recipes(GameTestHelper h) {
        return h.getLevel().getRecipeManager().getRecipes().stream().filter(r->r.id().getNamespace().equals(BetelNutMod.MOD_ID) && !r.id().getPath().startsWith("compat/create/")
                && r.value().getResultItem(h.getLevel().registryAccess()).getItem() instanceof RecoveryDrinkItem).toList();
    }

    @GameTest(template="empty")
    public void recoveryRecipesRequireIngredientsAndBottleInBothEnvironments(GameTestHelper h) throws Exception {
        boolean delight=ModList.get().isLoaded("farmersdelight");var recipes=recipes(h);
        h.assertTrue(recipes.size()==3,"Exactly one active route for each remedy");
        for(var holder:recipes) {
            h.assertTrue(BuiltInRegistries.RECIPE_TYPE.getKey(holder.value().getType()).toString().equals(delight?"farmersdelight:cooking":"minecraft:crafting"),"Correct environment route");
            h.assertTrue(h.getLevel().getServer().getAdvancements().get(BetelNutMod.id("recipes/"+holder.id().getPath()))!=null,"Active route has a recipe-book unlock");
            if(holder.value() instanceof CraftingRecipe recipe) {
                var inputs=new ArrayList<>(BetelRecipeGameTests.inputs(holder));while(inputs.size()<9)inputs.add(ItemStack.EMPTY);
                var grid=CraftingInput.of(3,3,inputs);var match=h.getLevel().getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,h.getLevel());
                h.assertTrue(match.isPresent()&&match.get().id().equals(holder.id()),"Actual crafting finds the exact remedy");
                h.assertTrue(ItemStack.matches(recipe.assemble(grid,h.getLevel().registryAccess()),recipe.getResultItem(h.getLevel().registryAccess())),"Exact one-bottle result");
                h.assertTrue(recipe.getRemainingItems(grid).stream().filter(x->x.is(Items.GLASS_BOTTLE)).mapToInt(ItemStack::getCount).sum()==1,"Honey's original bottle returns once");
                for(int i=0;i<inputs.size();i++)if(inputs.get(i).is(Items.GLASS_BOTTLE))inputs.set(i,ItemStack.EMPTY);
                h.assertTrue(!recipe.matches(CraftingInput.of(3,3,inputs),h.getLevel()),"No free output container without an empty bottle");
            }
        }
        if(delight)Class.forName("betel.nut.test.FarmerRecipeChecks").getMethod("runRecovery",GameTestHelper.class).invoke(null,h);
        h.succeed();
    }
}
