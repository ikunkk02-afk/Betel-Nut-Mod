package betel.nut.test;

import betel.nut.BetelNutMod;
import betel.nut.addiction.AddictionStageUtil;
import betel.nut.addiction.DailyUseTracker;
import betel.nut.component.BetelNutAddictionComponent;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.effect.ModEffects;
import betel.nut.event.WithdrawalEatingRestrictions;
import betel.nut.item.BetelPackItem;
import betel.nut.item.LotteryTicketItem;
import betel.nut.item.ModItems;
import betel.nut.network.AddictionSyncPayload;
import io.netty.buffer.Unpooled;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

@GameTestHolder("betel_nut_mod")
@PrefixGameTestTemplate(false)
public final class BetelGameplayGameTests {
    private static net.minecraft.server.level.ServerPlayer player(GameTestHelper h) {
        var cookie=net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "gameplay-test"),false);
        var p=new net.minecraft.server.level.ServerPlayer(h.getLevel().getServer(),h.getLevel(),cookie.gameProfile(),cookie.clientInformation());
        var connection=new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        net.neoforged.neoforge.network.registration.NetworkRegistry.configureMockConnection(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection,p,cookie);
        p.setGameMode(GameType.SURVIVAL); return p;
    }
    @GameTest(template="empty")
    public void calendarCountsDistinctDatesAndSleep(GameTestHelper h) {
        DailyUseTracker c = new DailyUseTracker(); c.beginSession(10);
        h.assertTrue(c.eat(10), "First nut on date counts");
        for (int i=0;i<20;i++) h.assertTrue(!c.eat(10), "More nuts on same date never count");
        c.advance(11); h.assertTrue(c.eat(11), "Sleep/date boundary allows one more day");
        c.advance(8); h.assertTrue(!c.eat(8), "Clock rewind cannot duplicate use history");
        c.advance(11); h.assertTrue(!c.eat(11), "Returning to consumed date cannot count twice");
        c.advance(12); h.assertTrue(c.eat(12), "New high-water date still counts"); h.succeed();
    }
    @GameTest(template="empty")
    public void sobrietyRequiresWholeObservedDays(GameTestHelper h) {
        DailyUseTracker c = new DailyUseTracker(); c.beginSession(10); c.eat(10);
        h.assertTrue(!c.advance(11) && c.soberDays()==0, "Eating date is not a sober date");
        h.assertTrue(c.advance(12) && c.soberDays()==1, "First complete sober date settles at next dawn");
        h.assertTrue(c.advance(13) && c.soberDays()==2, "Second sober day");
        h.assertTrue(c.advance(14) && c.soberDays()==3, "Third sober day allows recovery");
        c.beginSession(100); h.assertTrue(c.soberDays()==3, "Offline dates do not accumulate recovery");
        h.assertTrue(!c.advance(101), "Partial login day is not counted as complete");
        h.assertTrue(c.advance(102) && c.soberDays()==4, "Next complete online date does count");
        h.assertTrue(!c.advance(200) && c.soberDays()==4, "Admin jumps do not invent sober days");
        h.succeed();
    }
    @GameTest(template="empty")
    public void actualEatingCountsOnceAndPlayersAreIndependent(GameTestHelper h) {
        var a=player(h); var b=player(h);
        var sa=BetelNutEntityComponents.ADDICTION.get(a); var sb=BetelNutEntityComponents.ADDICTION.get(b);
        sa.clearAddiction(a); sb.clearAddiction(b);
        new ItemStack(ModItems.WRAPPED_RAW_BETEL).finishUsingItem(h.getLevel(),a);
        new ItemStack(ModItems.SWEET_BETEL_NUT).finishUsingItem(h.getLevel(),a);
        new ItemStack(ModItems.SPICY_BETEL_NUT).finishUsingItem(h.getLevel(),a);
        h.assertTrue(sa.getAddictionValue()==1 && sa.getOralExposureDays()==1, "Actual foods share one daily dependence/exposure increment");
        h.assertTrue(sb.getAddictionValue()==0 && sb.getOralExposureDays()==0, "Another player's state stays independent");
        h.succeed();
    }
    @GameTest(template="empty")
    public void actualCalendarSettlesRecoveryAndKeepsDiseaseSeparate(GameTestHelper h) {
        var p=player(h); var world=p.server.overworld(); long before=world.getDayTime();
        try {
            world.setDayTime(2400000000L);
            var s=new BetelNutAddictionComponent(p); s.beginCalendarSession(p); s.setAddictionValue(5);
            s.eatBetelNut(p,45,world.getGameTime());
            h.assertTrue(s.getAddictionValue()==6,"A new calendar date adds exactly one dependence day");
            s.setOralExposureDays(betel.nut.BetelNutConfig.get().oralCancerStage2Days);
            for(int i=1;i<=4;i++) {world.setDayTime(2400000000L+i*24000L);s.serverTick(p);}
            h.assertTrue(s.getSoberDays()==3 && s.getAddictionValue()==5,"Third completed abstinent date starts daily recovery");
            h.assertTrue(s.getWithdrawalSeverity()==3 && s.getOralCancerStage()==2,"Withdrawal grows by abstinent days while oral exposure stays independent");
            var fresh=new betel.nut.BetelNutConfig();
            h.assertTrue(fresh.configVersion==15 && fresh.maxAddictionValue==7 && fresh.legacyMaxAddictionValue==100
                    && fresh.minimumAddictionForWithdrawal==2,"Fresh configuration keeps the legacy save migration scale");
        } finally {world.setDayTime(before);}
        h.succeed();
    }
    @GameTest(template="empty")
    public void stagesAndLegacyMigrationAreStable(GameTestHelper h) {
        int[] start={0,2,3,4,5,7};
        for(int stage=1;stage<=5;stage++) {
            h.assertTrue(AddictionStageUtil.getStage(start[stage],7)==stage,"Correct day threshold");
            h.assertTrue(AddictionStageUtil.getStage(start[stage]-1,7)==stage-1,"Below threshold stays in previous stage");
        }
        var p=player(h); CompoundTag old=new CompoundTag();old.putInt("addictionValue",45);old.putInt("withdrawalValue",50);
        var s=new BetelNutAddictionComponent(p);s.readFromNbt(old,h.getLevel().registryAccess());s.beginCalendarSession(p);
        h.assertTrue(s.getAddictionValue()==4 && s.getWithdrawalValue()==50,"Old heavy score maps to heavy day threshold and preserves withdrawal");
        int exposure=betel.nut.BetelNutConfig.get().oralCancerStage2Days;
        s.setOralExposureDays(exposure);CompoundTag saved=new CompoundTag();s.writeToNbt(saved,h.getLevel().registryAccess());
        var loaded=new BetelNutAddictionComponent(p);loaded.readFromNbt(saved,h.getLevel().registryAccess());loaded.beginCalendarSession(p);
        loaded.eatBetelNut(p,45,h.getLevel().getGameTime());
        h.assertTrue(loaded.getAddictionValue()==4 && loaded.getOralExposureDays()==exposure,"Reload neither migrates twice nor recounts current day");
        var respawn=new BetelNutAddictionComponent(p);respawn.copyForRespawn(loaded,false);
        h.assertTrue(respawn.getAddictionValue()==4 && respawn.getOralCancerStage()==2 && respawn.hasEatenToday(),"Death preserves calendar and oral state");
        h.succeed();
    }
    @GameTest(template="empty")
    public void fullDependenceFitsFiveToEightDays(GameTestHelper h) {
        for (int max=5; max<=8; max++) {
            int previous=0;
            for (int stage=1; stage<=5; stage++) {
                int threshold=AddictionStageUtil.getStageStartThreshold(stage,max);
                h.assertTrue(threshold>previous && threshold<=max,"All five stages remain reachable in the supported range");
                h.assertTrue(AddictionStageUtil.getStage(threshold,max)==stage,"Stage starts at its threshold");
                h.assertTrue(AddictionStageUtil.getStage(threshold-1,max)==stage-1,"Boundary never skips a stage");
                previous=threshold;
            }
            h.assertTrue(AddictionStageUtil.getStage(max,max)==5 && AddictionStageUtil.getStage(max-1,max)==4,"Full dependence begins precisely on the configured day");
            h.assertTrue(AddictionStageUtil.getStageProgress(max,max)==1F,"Full dependence has full progress");
            h.assertTrue(!AddictionStageUtil.wouldReachNextStage(max,1,max),"No further stage after full dependence");
            h.assertTrue(AddictionStageUtil.migrateLegacyScore(90,100,max)==max,"Old full dependency honors the chosen new maximum");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public void sevenActualEatingDatesReachFullDependence(GameTestHelper h) {
        var p=player(h); var world=p.server.overworld(); long before=world.getDayTime();
        var config=betel.nut.BetelNutConfig.get();boolean oral=config.enableOralDisease;
        try {
            config.enableOralDisease=false; // Isolate calendar/dependence from the new disease eating gate.
            world.setDayTime(2400000000L);
            var s=BetelNutEntityComponents.ADDICTION.get(p); s.clearAddiction(p); s.beginCalendarSession(p);
            p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(10);
            for(int day=1;day<=8;day++) {
                world.setDayTime(2400000000L+(day-1)*24000L);
                for(int sameDay=0;sameDay<10;sameDay++) new ItemStack(ModItems.WRAPPED_RAW_BETEL).finishUsingItem(world,p);
                h.assertTrue(s.getAddictionValue()==Math.min(day,7),"Repeated foods cannot accelerate the calendar");
                h.assertTrue(s.getOralExposureDays()==Math.min(day,7),"Exposure remains capped independently of the dependence calendar");
                h.assertTrue((s.getAddictionStage()==5)==(day>=7),"Seventh eating date reaches full addiction");
                int symptoms=new int[]{0,0,1,2,3,4,4,5,5}[day];
                assertDependence(h,p,symptoms);
                int cancer=0;
                assertOral(h,p,cancer);
                h.assertTrue(s.getWithdrawalValue()==0 && s.getCleanTime()>world.getGameTime(),
                        "Daily symptoms apply even with zero withdrawal and fresh betel relief");
            }
        } finally {config.enableOralDisease=oral;world.setDayTime(before);BetelNutEntityComponents.ADDICTION.get(p).clearAddiction(p);}
        h.succeed();
    }
    private static void assertDependence(GameTestHelper h, net.minecraft.server.level.ServerPlayer p, int stage) {
        var effect=p.getEffect(ModEffects.BETEL_DEPENDENCE);
        h.assertTrue(stage==0?effect==null:effect!=null&&effect.getAmplifier()==stage-1,"Actual negative effect matches eating-day stage");
        // Remove only beneficial rewards to measure the dependence modifiers independently.
        for(var present:new ArrayList<>(p.getActiveEffects()))
            if(present.getEffect().value().getCategory()==net.minecraft.world.effect.MobEffectCategory.BENEFICIAL)
                p.removeEffect(present.getEffect());
        var movement=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.MOVEMENT_SPEED);
        var mining=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.BLOCK_BREAK_SPEED);
        var attack=p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE);
        h.assertTrue(Math.abs(movement.getValue()-movement.getBaseValue()*(1-.05*stage))<.00001,"Movement is really slower");
        h.assertTrue(Math.abs(mining.getValue()-mining.getBaseValue()*(1-.10*stage))<.00001,"Mining is really slower");
        h.assertTrue(Math.abs(attack.getValue()-Math.max(0,attack.getBaseValue()-.5*Math.max(0,stage-1)))<.00001,
                "Attack loss begins at stage II and scales with days");
    }
    @GameTest(template="empty")
    public void dailySymptomsSurviveBetelSuppressionAndMilk(GameTestHelper h) {
        var p=player(h); var s=BetelNutEntityComponents.ADDICTION.get(p); s.clearAddiction(p); s.setAddictionValue(7);
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(10);
        s.maintainDependenceEffects(p);
        s.suppressConflictingWithdrawalEffects(p,1200,1200,1200);
        new ItemStack(ModItems.ROASTED_BETEL_NUT).finishUsingItem(h.getLevel(),p);
        h.assertTrue(s.getAddictionValue()==7&&s.getWithdrawalSeverity()==0,"Full active use remains independent of withdrawal");
        assertDependence(h,p,5);
        // Milk removes status effects before the finished-use recovery handler runs.
        p.removeAllEffects(); s.applyMilkRelief(p,h.getLevel().getGameTime());
        assertDependence(h,p,5);
        float exhaustion=p.getFoodData().getExhaustionLevel();
        ModEffects.BETEL_DEPENDENCE.value().applyEffectTick(p,4);
        h.assertTrue(Math.abs(p.getFoodData().getExhaustionLevel()-exhaustion-.2F)<.00001,"Full dependence actually adds hunger exhaustion");
        h.assertTrue(WithdrawalEatingRestrictions.canUseAtCurrentRestriction(p,s,new ItemStack(ModItems.WRAPPED_RAW_BETEL)),
                "Daily dependence alone does not falsely lock the jaw before day seven");
        s.clearAddiction(p); assertDependence(h,p,0); h.succeed();
    }
    @GameTest(template="empty")
    public void recoveryImmediatelyDowngradesAndRemovesDailySymptoms(GameTestHelper h) {
        var p=player(h); var s=BetelNutEntityComponents.ADDICTION.get(p); s.clearAddiction(p); s.setAddictionValue(7);
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(10);
        s.maintainDependenceEffects(p); assertDependence(h,p,5);
        new ItemStack(Items.GOLDEN_APPLE).finishUsingItem(h.getLevel(),p);
        h.assertTrue(s.getAddictionValue()==6,"Golden apple removes one dependence day"); assertDependence(h,p,4);
        new ItemStack(Items.ENCHANTED_GOLDEN_APPLE).finishUsingItem(h.getLevel(),p);
        h.assertTrue(s.getAddictionValue()==1,"Enchanted apple removes five more days"); assertDependence(h,p,0);
        s.setAddictionValue(3);s.maintainDependenceEffects(p);assertDependence(h,p,2);
        s.setAddictionValue(2);s.maintainDependenceEffects(p);assertDependence(h,p,1);
        s.clearAddiction(p);assertDependence(h,p,0);h.succeed();
    }
    @GameTest(template="empty")
    public void oldDailySaveReceivesSymptomsAndGameModesStaySafe(GameTestHelper h) {
        var p=player(h); var s=BetelNutEntityComponents.ADDICTION.get(p); s.clearAddiction(p);
        p.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(10);
        CompoundTag old=new CompoundTag();old.putInt("dailySchema",2);old.putInt("addictionValue",7);
        s.readFromNbt(old,h.getLevel().registryAccess());s.beginCalendarSession(p);s.serverTick(p);assertDependence(h,p,5);
        h.assertTrue(s.getAddictionValue()==7&&s.getOralExposureDays()==0,"Loading symptoms invents no eating days or oral exposure");
        CompoundTag saved=new CompoundTag();s.writeToNbt(saved,h.getLevel().registryAccess());
        var respawn=player(h);var copied=BetelNutEntityComponents.ADDICTION.get(respawn);
        respawn.getAttribute(net.minecraft.world.entity.ai.attributes.Attributes.ATTACK_DAMAGE).setBaseValue(10);
        copied.copyForRespawn(s,false);copied.maintainDependenceEffects(respawn);assertDependence(h,respawn,5);
        p.setGameMode(GameType.CREATIVE);s.serverTick(p);assertDependence(h,p,0);
        p.setGameMode(GameType.SURVIVAL);s.serverTick(p);assertDependence(h,p,5);
        p.setGameMode(GameType.SPECTATOR);s.serverTick(p);assertDependence(h,p,0);
        p.setGameMode(GameType.SURVIVAL);
        var config=betel.nut.BetelNutConfig.get();boolean enabled=config.enableAddictionSystem;
        try {config.enableAddictionSystem=false;s.serverTick(p);assertDependence(h,p,0);}
        finally {config.enableAddictionSystem=enabled;}
        s.serverTick(p);assertDependence(h,p,5);s.clearAddiction(p);copied.clearAddiction(respawn);h.succeed();
    }
    @GameTest(template="empty")
    public void dailySavesAndVersionTwelveConfigMigrate(GameTestHelper h) throws ReflectiveOperationException {
        var config=new betel.nut.BetelNutConfig(); config.configVersion=12; config.maxAddictionValue=30;
        config.minimumAddictionForWithdrawal=3; config.enableOralDisease=false; config.oralCancerStage2Days=24;
        var validate=betel.nut.BetelNutConfig.class.getDeclaredMethod("validated"); validate.setAccessible(true); validate.invoke(config);
        h.assertTrue(config.maxAddictionValue==7 && config.minimumAddictionForWithdrawal==2 && config.configVersion==15,"Existing default config adopts seven-day timing");
        h.assertTrue(!config.enableOralDisease && config.oralCancerStage2Days==24 && config.legacyMaxAddictionValue==100,"Unrelated custom settings survive migration");
        for(int chosen=5;chosen<=8;chosen++) {
            config.maxAddictionValue=chosen; validate.invoke(config);
            h.assertTrue(config.maxAddictionValue==chosen,"Every supported timing remains configurable");
        }
        config.maxAddictionValue=100; validate.invoke(config); h.assertTrue(config.maxAddictionValue==8,"Out-of-range upper limit is bounded");
        config.maxAddictionValue=1; validate.invoke(config); h.assertTrue(config.maxAddictionValue==5,"Out-of-range lower limit is bounded");
        var p=player(h);
        for(int actualDays : new int[]{1,6,7,21,30}) {
            CompoundTag saved=new CompoundTag(); saved.putInt("dailySchema",2); saved.putInt("addictionValue",actualDays);
            var state=new BetelNutAddictionComponent(p); state.readFromNbt(saved,h.getLevel().registryAccess());
            h.assertTrue(state.getAddictionValue()==Math.min(actualDays,7),"Real eating days are capped, never rescaled as scores");
            CompoundTag again=new CompoundTag(); state.writeToNbt(again,h.getLevel().registryAccess());
            var reloaded=new BetelNutAddictionComponent(p); reloaded.readFromNbt(again,h.getLevel().registryAccess());
            h.assertTrue(reloaded.getAddictionValue()==state.getAddictionValue(),"Reload is idempotent");
        }
        h.succeed();
    }
    @GameTest(template="empty")
    public void cancerHasThreeRealEffectAmplifiers(GameTestHelper h) {
        var p=player(h);p.setGameMode(GameType.SURVIVAL);var s=BetelNutEntityComponents.ADDICTION.get(p);s.clearAddiction(p);
        int[] days={3,5,7};
        for(int i=0;i<3;i++) {s.setOralExposureDays(days[i]);s.maintainOralEffects(p);
            h.assertTrue(p.hasEffect(ModEffects.ORAL_CANCER)&&p.getEffect(ModEffects.ORAL_CANCER).getAmplifier()==i,"Cancer I/II/III is registered and applied");
            h.assertTrue(Math.abs(p.getMaxHealth()-(20-2*(i+1)))<0.001,"Cancer applies distinct max-health penalties");}
        h.assertTrue(p.hasEffect(ModEffects.MOUTH_LOCKED),"Cancer III applies locked jaw effect");
        s.setOralExposureDays(0);s.maintainOralEffects(p);
        h.assertTrue(!p.hasEffect(ModEffects.ORAL_CANCER)&&!p.hasEffect(ModEffects.MOUTH_LOCKED)&&p.getMaxHealth()==20,"Cure removes effects and health modifiers");
        s.setAddictionValue(21);s.setWithdrawalValue(p,100);s.setOralExposureDays(30);s.maintainOralEffects(p);
        h.assertTrue(p.getMaxHealth()>=betel.nut.BetelNutConfig.get().minimumMaxHealthAfterPenalty,
                "Disease and withdrawal together preserve the configured health floor immediately");
        s.clearAddiction(p);h.succeed();
    }
    @GameTest(template="empty")
    public void lockedJawBlocksBetelAndFinishUseButAllowsRecovery(GameTestHelper h) {
        var p=player(h);p.setGameMode(GameType.SURVIVAL);var s=BetelNutEntityComponents.ADDICTION.get(p);s.clearAddiction(p);s.setOralExposureDays(30);s.maintainOralEffects(p);
        for(Item food:List.of(Items.BREAD,Items.APPLE,Items.GOLDEN_APPLE,Items.ENCHANTED_GOLDEN_APPLE,Items.HONEY_BOTTLE,Items.MUSHROOM_STEW,ModItems.ROASTED_BETEL_NUT,ModItems.WRAPPED_RAW_BETEL)) {
            ItemStack stack=new ItemStack(food,2);p.setItemInHand(InteractionHand.OFF_HAND,stack);
            h.assertTrue(WithdrawalEatingRestrictions.shouldBlockUse(p,stack,false),"Locked jaw must block " + food + ": " + WithdrawalEatingRestrictions.evaluate(p,s,stack) + " creative=" + p.isCreative());
            stack.finishUsingItem(h.getLevel(),p);h.assertTrue(stack.getCount()==2,"Finish-use injection prevents consuming blocked solids");}
        for(Item food:List.of(Items.MILK_BUCKET,Items.POTION,ModItems.GOLDEN_APPLE_PASTE,ModItems.ENCHANTED_GOLDEN_APPLE_PASTE,ModItems.RELIEF_POTION))
            h.assertTrue(!WithdrawalEatingRestrictions.shouldBlockUse(p,new ItemStack(food),false),"Liquids and magical recovery remain usable");
        s.applyEnchantedGoldenAppleRecovery(p,h.getLevel().getGameTime());s.maintainOralEffects(p);
        h.assertTrue(s.getOralExposureDays()==0&&!p.hasEffect(ModEffects.MOUTH_LOCKED),"Magic recovery reduces independent oral exposure and releases jaw");h.succeed();
    }
    @GameTest(template="empty")
    public void realMilkRestoresDiseaseAfterVanillaClearing(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);s.clearAddiction(p);
        s.setAddictionValue(7);s.setOralExposureDays(7);s.refreshWithdrawalEffects(p);
        p.addEffect(new MobEffectInstance(MobEffects.POISON,200));
        ItemStack milk=new ItemStack(Items.MILK_BUCKET);
        ItemStack result=milk.finishUsingItem(h.getLevel(),p);
        h.assertTrue(result.is(Items.BUCKET)&&milk.isEmpty(),"The last milk item is actually consumed and returns a bucket");
        h.assertTrue(!p.hasEffect(MobEffects.POISON),"Vanilla milk still clears temporary effects");
        assertOral(h,p,3);assertDependence(h,p,5);
        h.assertTrue(s.getOralExposureDays()==7,"Milk never erases accumulated disease exposure");
        ItemStack cure=new ItemStack(ModItems.ENCHANTED_GOLDEN_APPLE_PASTE);
        h.assertTrue(cure.finishUsingItem(h.getLevel(),p).is(Items.GLASS_BOTTLE),"Paste returns its bottle");
        h.assertTrue(cure.isEmpty()&&s.getAddictionValue()==2&&s.getOralExposureDays()==0,"Last magical paste applies recovery exactly once after consumption");
        assertOral(h,p,0);assertDependence(h,p,1);
        h.assertTrue(!WithdrawalEatingRestrictions.shouldBlockUse(p,new ItemStack(Items.BREAD),false),"Recovery immediately unlocks food use");
        s.clearAddiction(p);h.succeed();
    }
    @GameTest(template="empty")
    public void withdrawalJawUpdatesImmediatelyWithoutCancer(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);s.clearAddiction(p);
        s.setAddictionValue(7);s.setWithdrawalValue(p,100);
        h.assertTrue(p.hasEffect(ModEffects.MOUTH_LOCKED)&&!p.hasEffect(ModEffects.ORAL_CANCER),"Severe withdrawal immediately supplies its own locked-jaw effect");
        new ItemStack(Items.MILK_BUCKET).finishUsingItem(h.getLevel(),p);
        assertOral(h,p,0);
        h.assertTrue(!WithdrawalEatingRestrictions.shouldBlockUse(p,new ItemStack(Items.BREAD),false),"Milk relief immediately clears withdrawal-only locked jaw");
        s.clearAddiction(p);h.succeed();
    }
    @GameTest(template="empty")
    public void oralEffectsRestoreOnTickAndRespawnAndRespectSwitches(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);s.clearAddiction(p);
        CompoundTag old=new CompoundTag();old.putInt("dailySchema",2);old.putInt("addictionValue",7);old.putInt("oralExposureDays",7);
        s.readFromNbt(old,h.getLevel().registryAccess());s.beginCalendarSession(p);s.serverTick(p);assertOral(h,p,3);
        p.removeAllEffects();s.serverTick(p);assertOral(h,p,3);
        var next=player(h);var copied=BetelNutEntityComponents.ADDICTION.get(next);copied.copyForRespawn(s,false);
        net.neoforged.neoforge.common.NeoForge.EVENT_BUS.post(new net.neoforged.neoforge.event.entity.player.PlayerEvent.PlayerRespawnEvent(next,false));
        assertOral(h,next,3);
        var config=betel.nut.BetelNutConfig.get();boolean oral=config.enableOralDisease,enabled=config.enableAddictionSystem;
        try {
            config.enableOralDisease=false;s.serverTick(p);assertOral(h,p,0);
            config.enableOralDisease=true;s.serverTick(p);assertOral(h,p,3);
            config.enableAddictionSystem=false;s.serverTick(p);assertOral(h,p,0);
        } finally {config.enableOralDisease=oral;config.enableAddictionSystem=enabled;}
        s.serverTick(p);assertOral(h,p,3);s.clearAddiction(p);copied.clearAddiction(next);h.succeed();
    }
    @GameTest(template="empty")
    public void oldOralDefaultsMigrateAndCustomSchedulesSurvive(GameTestHelper h) throws ReflectiveOperationException {
        var validate=betel.nut.BetelNutConfig.class.getDeclaredMethod("validated");validate.setAccessible(true);
        var defaults=new betel.nut.BetelNutConfig();defaults.configVersion=13;
        defaults.oralCancerStage1Days=12;defaults.oralCancerStage2Days=21;defaults.oralCancerStage3Days=30;
        validate.invoke(defaults);
        h.assertTrue(defaults.configVersion==15&&defaults.oralCancerStage1Days==3&&defaults.oralCancerStage2Days==5&&defaults.oralCancerStage3Days==7,"Version 13 default disease schedule upgrades to the new seven-day cycle");
        var custom=new betel.nut.BetelNutConfig();custom.configVersion=13;custom.enableOralDisease=false;
        custom.oralCancerStage1Days=12;custom.oralCancerStage2Days=24;custom.oralCancerStage3Days=30;
        validate.invoke(custom);
        h.assertTrue(!custom.enableOralDisease&&custom.oralCancerStage1Days==12&&custom.oralCancerStage2Days==24&&custom.oralCancerStage3Days==30,"Custom disease timing and disabled switches are preserved");
        defaults.oralCancerStage1Days=12;defaults.oralCancerStage2Days=21;defaults.oralCancerStage3Days=30;validate.invoke(defaults);
        h.assertTrue(defaults.oralCancerStage3Days==30,"Current-version intentional custom old values are not repeatedly migrated");h.succeed();
    }
    @GameTest(template="empty")
    public void customOralExposureStillOutlivesDependenceCap(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);var world=p.server.overworld();
        var config=betel.nut.BetelNutConfig.get();int threshold=config.oralCancerStage3Days,soft=config.oralCancerStage2Days;long before=world.getDayTime();
        try {
            config.oralCancerStage3Days=30;config.oralCancerStage2Days=8;world.setDayTime(2400000000L);s.clearAddiction(p);s.beginCalendarSession(p);
            for(int day=1;day<=8;day++) {world.setDayTime(2400000000L+(day-1)*24000L);new ItemStack(ModItems.WRAPPED_RAW_BETEL).finishUsingItem(world,p);}
            h.assertTrue(s.getAddictionValue()==7&&s.getOralExposureDays()==8&&s.getOralCancerStage()==2,"Customized disease exposure remains independent of the seven-day dependence cap");
        } finally {config.oralCancerStage3Days=threshold;config.oralCancerStage2Days=soft;world.setDayTime(before);s.clearAddiction(p);}
        h.succeed();
    }
    private static void assertOral(GameTestHelper h, net.minecraft.server.level.ServerPlayer p,int stage) {
        var effect=p.getEffect(ModEffects.ORAL_CANCER);
        h.assertTrue(stage==0?effect==null:effect!=null&&effect.getAmplifier()==stage-1,"Actual oral-cancer amplifier is correct immediately, without another tick");
        h.assertTrue(p.hasEffect(ModEffects.MOUTH_LOCKED)==(stage==3),"Locked-jaw icon agrees with disease stage");
        h.assertTrue(Math.abs(p.getMaxHealth()-(20-2*stage))<.001,"Disease health modifier agrees with its displayed level");
    }
    @GameTest(template="empty")
    public void packsContainSixDistinctNutsAndCannotReplay(GameTestHelper h) {
        var p=player(h);p.setGameMode(GameType.SURVIVAL);
        for(Item item:List.of(ModItems.KOUWEIWANG_PACK,ModItems.HECHENG_TIANXIA_PACK,ModItems.XIANGTAN_PUZI_PACK)) {
            p.getInventory().clearContent();p.getCooldowns().removeCooldown(item);
            var pack=(BetelPackItem)item;h.assertTrue(new HashSet<>(pack.contents()).size()==6,"Six distinct flavors per brand");
            p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(item));
            p.gameMode.useItem(p,h.getLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND);
            int count=0,tickets=0;for(var stack:p.getInventory().items){count+=stack.getCount();if(stack.is(ModItems.LOTTERY_TICKET))tickets+=stack.getCount();}
            h.assertTrue(count==7&&tickets==1,"Opening consumes bag and issues six nuts plus one ticket");
            p.getCooldowns().removeCooldown(item);pack.use(h.getLevel(),p,InteractionHand.MAIN_HAND);
            int after=0;for(var stack:p.getInventory().items)after+=stack.getCount();
            h.assertTrue(after==count,"Stale empty-hand replay cannot issue contents");
        }h.succeed();
    }
    @GameTest(template="empty")
    public void lastTicketRewardSurvivesHandReplacementAndFullBagsDropOverflow(GameTestHelper h) {
        var p=player(h);p.getInventory().clearContent();
        var ticket=new ItemStack(ModItems.LOTTERY_TICKET); var data=new CompoundTag();data.putInt("betelPrize",4);
        ticket.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA,net.minecraft.world.item.component.CustomData.of(data));
        p.setItemInHand(InteractionHand.MAIN_HAND,ticket);
        p.gameMode.useItem(p,h.getLevel(),p.getMainHandItem(),InteractionHand.MAIN_HAND);
        h.assertTrue(p.getMainHandItem().is(ModItems.GOLDEN_SHELL) && p.getMainHandItem().getCount()==1,
                "A last-ticket reward in the freed hand slot survives the real use-item result");
        p.teleportTo(h.getLevel(),h.absoluteVec(new Vec3(1,2,1)).x,h.absoluteVec(new Vec3(1,2,1)).y,h.absoluteVec(new Vec3(1,2,1)).z,java.util.Set.of(),0,0);
        for(int i=0;i<36;i++)p.getInventory().setItem(i,new ItemStack(Items.COBBLESTONE,64));
        p.setItemInHand(InteractionHand.OFF_HAND,new ItemStack(ModItems.KOUWEIWANG_PACK));
        p.gameMode.useItem(p,h.getLevel(),p.getOffhandItem(),InteractionHand.OFF_HAND);
        int drops=h.getLevel().getEntitiesOfClass(net.minecraft.world.entity.item.ItemEntity.class,p.getBoundingBox().inflate(3)).stream()
                .filter(e -> e.getItem().is(ModItems.LOTTERY_TICKET) || ((BetelPackItem)ModItems.KOUWEIWANG_PACK).contents().contains(e.getItem().getItem()))
                .mapToInt(e -> e.getItem().getCount()).sum();
        h.assertTrue(p.getOffhandItem().isEmpty() && drops==7,"A full inventory drops all six flavors and ticket without overwriting existing stacks");
        h.succeed();
    }
    @GameTest(template="empty")
    public void recipesConsumeSealAndPreserveAssortments(GameTestHelper h) {
        var level=h.getLevel();
        for(Item item:List.of(ModItems.KOUWEIWANG_PACK,ModItems.HECHENG_TIANXIA_PACK,ModItems.XIANGTAN_PUZI_PACK)) {
            List<ItemStack> inputs=new ArrayList<>();for(Item nut:((BetelPackItem)item).contents())inputs.add(new ItemStack(nut));
            inputs.add(new ItemStack(ModItems.EMPTY_PACK));inputs.add(new ItemStack(ModItems.BRAND_SEAL));inputs.add(ItemStack.EMPTY);
            CraftingInput grid=CraftingInput.of(3,3,inputs);var recipe=level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,grid,level);
            h.assertTrue(recipe.isPresent()&&recipe.get().value().assemble(grid,level.registryAccess()).is(item),"Loaded brand recipe matches its actual six flavors");
            h.assertTrue(recipe.get().value().getRemainingItems(grid).stream().allMatch(ItemStack::isEmpty),"No seal or bag is returned");
            inputs.set(7,ItemStack.EMPTY);h.assertTrue(level.getRecipeManager().getRecipeFor(RecipeType.CRAFTING,CraftingInput.of(3,3,inputs),level).isEmpty(),"Cannot craft a free prize ticket without paid seal");
        }
        String echoRecipe = net.neoforged.fml.ModList.get().isLoaded("farmersdelight")
                ? "compat/farmersdelight/cooking_echo_betel" : "echo_betel";
        h.assertTrue(level.getRecipeManager().byKey(BetelNutMod.id(echoRecipe)).isPresent(),"Ancient-shell recipe loads in the active processing environment");h.succeed();
    }
    @GameTest(template="empty")
    public void lotteryOddsAndIssuedTicketSurviveSerialization(GameTestHelper h) {
        int[] counts=new int[5];for(int i=0;i<100;i++)counts[LotteryTicketItem.prizeForRoll(i)]++;
        h.assertTrue(java.util.Arrays.equals(counts,new int[]{60,25,10,4,1}),"Published probabilities match every possible roll");
        ItemStack ticket=LotteryTicketItem.create(net.minecraft.util.RandomSource.create(13));
        ItemStack copy=ItemStack.parseOptional(h.getLevel().registryAccess(),(CompoundTag)ticket.save(h.getLevel().registryAccess()));
        h.assertTrue(ItemStack.isSameItemSameComponents(ticket,copy),"Already-issued prize persists in item components");
        var p=player(h);p.getInventory().clearContent();p.setItemInHand(InteractionHand.MAIN_HAND,ticket);
        ((LotteryTicketItem)ModItems.LOTTERY_TICKET).use(h.getLevel(),p,InteractionHand.MAIN_HAND);
        h.assertTrue(!p.getMainHandItem().is(ModItems.LOTTERY_TICKET),"Redeeming consumes ticket");h.succeed();
    }
    @GameTest(template="empty")
    public void ancientCityLootAppendsRatherThanReplaces(GameTestHelper h) {
        var level=h.getLevel();var key=ResourceKey.create(Registries.LOOT_TABLE,ResourceLocation.withDefaultNamespace("chests/ancient_city"));
        var table=level.getServer().reloadableRegistries().getLootTable(key);
        var params=new LootParams.Builder(level).withParameter(LootContextParams.ORIGIN,Vec3.atCenterOf(h.absolutePos(net.minecraft.core.BlockPos.ZERO))).create(LootContextParamSets.CHEST);
        int shells=0,other=0;for(int i=0;i<256;i++)for(ItemStack stack:table.getRandomItems(params,(long)i)) {
            if(stack.is(ModItems.ANCIENT_SHELL)){h.assertTrue(stack.getCount()<=2,"Ancient shell drop count is bounded");shells+=stack.getCount();}else other+=stack.getCount();}
        h.assertTrue(shells>0&&other>500,"Ancient shell appears while vanilla chest contents remain");h.succeed();
    }
    @GameTest(template="empty")
    public void extendedHudPayloadRoundTrips(GameTestHelper h) {
        var payload=new AddictionSyncPayload(21,5,3,1234,30,3,true,3,2);
        var buf=new RegistryFriendlyByteBuf(Unpooled.buffer(),h.getLevel().registryAccess());
        try{AddictionSyncPayload.CODEC.encode(buf,payload);h.assertTrue(AddictionSyncPayload.CODEC.decode(buf).equals(payload),"All daily, disease and mouth HUD fields round-trip");}finally{buf.release();}
        h.succeed();
    }
    @GameTest(template="empty")
    public void echoBetelAppliesActualExplorationEffect(GameTestHelper h) {
        var p=player(h);var s=BetelNutEntityComponents.ADDICTION.get(p);s.clearAddiction(p);
        new ItemStack(ModItems.ECHO_BETEL).finishUsingItem(h.getLevel(),p);
        h.assertTrue(p.hasEffect(ModEffects.ECHO_SIGHT),"Special nut applies registered echo sight");
        p.addEffect(new MobEffectInstance(MobEffects.DARKNESS,200));ModEffects.ECHO_SIGHT.value().applyEffectTick(p,0);
        h.assertTrue(!p.hasEffect(MobEffects.DARKNESS),"Echo sight actively suppresses darkness");h.succeed();
    }
}
