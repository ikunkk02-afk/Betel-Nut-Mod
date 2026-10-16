package betel.nut.test;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMidnightConfig;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.effect.HechengTianxiaEffects;
import betel.nut.effect.ModEffects;
import betel.nut.item.BetelNutItem;
import betel.nut.item.EnderBetelTeleportHandler;
import betel.nut.item.ModItems;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.Holder;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;

public final class BetelEffectGameTests implements FabricGameTest {
    private record Reward(Holder<MobEffect> effect, int seconds, int amplifier) {}
    private record Food(Item item, List<Reward> rewards) {}
    private static Reward reward(Holder<MobEffect> effect, int seconds, int amplifier) {
        return new Reward(effect, seconds, amplifier);
    }
    private static Food food(Item item, Reward... rewards) { return new Food(item, List.of(rewards)); }
    private static ServerPlayer player(GameTestHelper h) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "effect-test"), false);
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        BetelNutEntityComponents.ADDICTION.get(p).clearAddiction(p);
        return p;
    }
    private static void effect(GameTestHelper h, ServerPlayer p, Holder<MobEffect> effect, int ticks, int amplifier) {
        var actual = p.getEffect(effect);
        h.assertTrue(actual != null && actual.getDuration() == ticks && actual.getAmplifier() == amplifier,
                "Expected " + effect + " duration=" + ticks + " amplifier=" + amplifier + "; actual=" + actual);
    }

    private static List<Item> betelFoods(GameTestHelper h) {
        var foods = BuiltInRegistries.ITEM.stream().filter(item -> item instanceof BetelNutItem).toList();
        h.assertTrue(foods.size() == 19, "Exercise every registered edible betel nut");
        return foods;
    }

    private static void finishBite(ServerPlayer p, InteractionHand hand) throws ReflectiveOperationException {
        p.startUsingItem(hand);
        // Exercise vanilla's hand replacement as well as the item's returned stack.
        var complete = LivingEntity.class.getDeclaredMethod("completeUsingItem");
        complete.setAccessible(true);
        complete.invoke(p);
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void everyBetelFoodReturnsOneResidueOnLastBiteInEitherHand(GameTestHelper h)
            throws ReflectiveOperationException {
        var p = player(h);
        var state = BetelNutEntityComponents.ADDICTION.get(p);
        var config = BetelNutConfig.get();
        boolean teleport = config.enableEnderBetelTeleport;
        boolean repeat = BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath;
        try {
            config.enableEnderBetelTeleport = false;
            BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath = true;
            for (var item : betelFoods(h)) for (var hand : InteractionHand.values()) {
                state.clearAddiction(p); p.removeAllEffects(); p.getInventory().clearContent();
                p.setItemInHand(hand, new ItemStack(item));
                finishBite(p, hand);
                var result = p.getItemInHand(hand);
                h.assertTrue(result.is(ModItems.BETEL_NUT_RESIDUE) && result.getCount() == 1,
                        "Last bite leaves one residue in " + hand + ": " + item);
                h.assertTrue(p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE) == 1,
                        "No extra residue on last bite: " + item);
            }
        } finally {
            config.enableEnderBetelTeleport = teleport;
            BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath = repeat;
            state.clearAddiction(p); p.removeAllEffects();
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void everyBetelFoodGrantsOneResiduePerCreativeBite(GameTestHelper h)
            throws ReflectiveOperationException {
        var p = player(h);
        p.setGameMode(GameType.CREATIVE);
        var state = BetelNutEntityComponents.ADDICTION.get(p);
        var config = BetelNutConfig.get();
        boolean teleport = config.enableEnderBetelTeleport;
        boolean repeat = BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath;
        try {
            config.enableEnderBetelTeleport = false;
            BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath = true;
            for (var item : betelFoods(h)) for (int count : new int[] {1, 2}) {
                state.clearAddiction(p); p.removeAllEffects(); p.getInventory().clearContent();
                p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(item, count));
                for (int bite = 1; bite <= 2; bite++) {
                    finishBite(p, InteractionHand.OFF_HAND);
                    var remaining = p.getOffhandItem();
                    h.assertTrue(remaining.is(item) && remaining.getCount() == count,
                            "Creative retains the original nuts: " + item);
                    h.assertTrue(p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE) == bite,
                            "Exactly one residue for each creative bite: " + item);
                }
            }
        } finally {
            config.enableEnderBetelTeleport = teleport;
            BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath = repeat;
            state.clearAddiction(p); p.removeAllEffects();
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void everyBetelFoodDropsOneResidueWhenInventoryIsFull(GameTestHelper h)
            throws ReflectiveOperationException {
        var p = player(h);
        var state = BetelNutEntityComponents.ADDICTION.get(p);
        var config = BetelNutConfig.get();
        boolean teleport = config.enableEnderBetelTeleport;
        boolean repeat = BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath;
        try {
            config.enableEnderBetelTeleport = false;
            BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath = true;
            for (var mode : new GameType[] {GameType.SURVIVAL, GameType.CREATIVE}) {
                p.setGameMode(mode);
                for (var item : betelFoods(h)) {
                    state.clearAddiction(p); p.removeAllEffects(); p.getInventory().clearContent();
                    for (int slot = 0; slot < p.getInventory().items.size(); slot++) {
                        p.getInventory().items.set(slot, new ItemStack(Items.STONE, 64));
                    }
                    p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
                    var existingDrops = h.getLevel().getEntitiesOfClass(ItemEntity.class,
                            p.getBoundingBox().inflate(8)).stream().map(ItemEntity::getUUID).toList();
                    finishBite(p, InteractionHand.MAIN_HAND);
                    int remaining = mode == GameType.CREATIVE ? 2 : 1;
                    h.assertTrue(p.getMainHandItem().is(item) && p.getMainHandItem().getCount() == remaining,
                            "Correct remaining nuts with full inventory: " + item);
                    var drops = h.getLevel().getEntitiesOfClass(ItemEntity.class,
                            p.getBoundingBox().inflate(8), drop -> !existingDrops.contains(drop.getUUID())
                                    && drop.getItem().is(ModItems.BETEL_NUT_RESIDUE));
                    h.assertTrue(drops.size() == 1 && drops.getFirst().getItem().getCount() == 1,
                            "Exactly one residue drops with full inventory in " + mode + ": " + item);
                    h.assertTrue(p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE) == 0,
                            "A full inventory cannot also receive residue");
                    drops.forEach(ItemEntity::discard);
                }
            }
        } finally {
            config.enableEnderBetelTeleport = teleport;
            BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath = repeat;
            state.clearAddiction(p); p.removeAllEffects();
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void interruptedOrBlockedBitesDoNotGrantResidue(GameTestHelper h) {
        var p = player(h);
        var state = BetelNutEntityComponents.ADDICTION.get(p);
        try {
            for (var item : betelFoods(h)) {
                state.clearAddiction(p); p.removeAllEffects(); p.getInventory().clearContent();
                p.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 2));
                p.startUsingItem(InteractionHand.MAIN_HAND);
                p.releaseUsingItem();
                h.assertTrue(p.getMainHandItem().getCount() == 2
                                && p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE) == 0,
                        "Interrupted bite grants no residue: " + item);
                p.addEffect(new MobEffectInstance(ModEffects.MOUTH_LOCKED, 100));
                var result = p.getMainHandItem().finishUsingItem(h.getLevel(), p);
                h.assertTrue(result.is(item) && result.getCount() == 2
                                && p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE) == 0,
                        "Blocked bite grants no residue: " + item);
            }
        } finally {
            state.clearAddiction(p); p.removeAllEffects();
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void allNineteenFoodsConsumeAndGrantTheirDocumentedRewards(GameTestHelper h) {
        var p = player(h);
        var state = BetelNutEntityComponents.ADDICTION.get(p);
        var config = BetelNutConfig.get();
        boolean teleport = config.enableEnderBetelTeleport;
        int duration = BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds;
        try {
            config.enableEnderBetelTeleport = false;
            BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds = 120;
            var foods = List.of(
                food(ModItems.WRAPPED_RAW_BETEL, reward(MobEffects.MOVEMENT_SPEED, 10, 0)),
                food(ModItems.ROASTED_BETEL_NUT, reward(MobEffects.MOVEMENT_SPEED, 20, 0)),
                food(ModItems.SWEET_BETEL_NUT, reward(MobEffects.MOVEMENT_SPEED, 40, 0)),
                food(ModItems.SPICY_BETEL_NUT, reward(MobEffects.DAMAGE_BOOST, 20, 0), reward(MobEffects.HUNGER, 10, 0)),
                food(ModItems.HONEY_BETEL_NUT, reward(MobEffects.REGENERATION, 10, 0)),
                food(ModItems.REFRESHING_BETEL_NUT, reward(MobEffects.WATER_BREATHING, 60, 0)),
                food(ModItems.NIGHT_BETEL_NUT, reward(MobEffects.NIGHT_VISION, 90, 0)),
                food(ModItems.ENERGIZING_BETEL_NUT, reward(MobEffects.MOVEMENT_SPEED, 45, 1), reward(MobEffects.DIG_SPEED, 45, 0), reward(MobEffects.HUNGER, 15, 0)),
                food(ModItems.GLOW_BETEL_NUT, reward(MobEffects.NIGHT_VISION, 45, 0), reward(MobEffects.GLOWING, 45, 0)),
                food(ModItems.PHANTOM_BETEL_NUT, reward(MobEffects.SLOW_FALLING, 60, 0), reward(MobEffects.JUMP, 30, 0)),
                food(ModItems.ENDER_BETEL_NUT),
                food(ModItems.LAPIS_BETEL_NUT, reward(MobEffects.LUCK, 120, 0), reward(MobEffects.JUMP, 30, 0)),
                food(ModItems.QUARTZ_BETEL_NUT, reward(MobEffects.DIG_SPEED, 60, 0)),
                food(ModItems.MAGMA_BETEL_NUT, reward(MobEffects.FIRE_RESISTANCE, 90, 0), reward(MobEffects.DAMAGE_BOOST, 30, 0), reward(MobEffects.HUNGER, 15, 0)),
                food(ModItems.AMETHYST_BETEL_NUT, reward(MobEffects.DAMAGE_RESISTANCE, 30, 0)),
                food(ModItems.ECHO_BETEL, reward(ModEffects.ECHO_SIGHT, 45, 0)),
                food(ModItems.SYNTHETIC_WORLD_BETEL, reward(MobEffects.MOVEMENT_SPEED, 120, 1), reward(MobEffects.DIG_SPEED, 120, 1), reward(MobEffects.DAMAGE_RESISTANCE, 60, 0), reward(MobEffects.ABSORPTION, 30, 0)),
                food(ModItems.RICH_WORLD_BETEL, reward(MobEffects.MOVEMENT_SPEED, 75, 0), reward(MobEffects.NIGHT_VISION, 90, 0), reward(MobEffects.DAMAGE_RESISTANCE, 30, 0), reward(MobEffects.GLOWING, 15, 0)),
                food(ModItems.UNDERGROUND_BETEL, reward(MobEffects.NIGHT_VISION, 120, 0), reward(MobEffects.DIG_SPEED, 90, 1), reward(MobEffects.DAMAGE_RESISTANCE, 30, 0), reward(ModEffects.ECHO_SIGHT, 30, 0), reward(MobEffects.MOVEMENT_SLOWDOWN, 10, 0)));
            for (var food : foods) {
                state.clearAddiction(p); p.removeAllEffects(); p.getInventory().clearContent();
                p.getFoodData().setFoodLevel(0);
                var stack = new ItemStack(food.item(), 2);
                stack.finishUsingItem(h.getLevel(), p);
                h.assertTrue(stack.getCount() == 1, "Exactly one food consumed: " + food.item());
                h.assertTrue(p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE) == 1, "One residue per food");
                h.assertTrue(state.getAddictionValue() == 1 && state.getOralExposureDays() == 1, "Daily risks still apply to every food");
                h.assertTrue(p.getActiveEffects().size() == food.rewards().size(), "No unrelated buffs for " + food.item());
                for (var expected : food.rewards()) effect(h, p, expected.effect(), expected.seconds() * 20, expected.amplifier());
                h.assertTrue(p.getFoodData().getFoodLevel() <= 3, "A nut is no longer a large meal");
            }
        } finally {
            config.enableEnderBetelTeleport = teleport;
            BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds = duration;
            state.clearAddiction(p); p.removeAllEffects();
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void dependenceWeakensBenefitsButNotHungerOrGlowing(GameTestHelper h) {
        var p = player(h); var state = BetelNutEntityComponents.ADDICTION.get(p);
        int[] days = {0, 2, 3, 4, 5, 7};
        float[] multipliers = {1, .8F, .65F, .5F, .35F, .25F};
        for (int stage = 0; stage <= 5; stage++) {
            state.clearAddiction(p); p.removeAllEffects(); state.setAddictionValue(days[stage]);
            new ItemStack(ModItems.ENERGIZING_BETEL_NUT).finishUsingItem(h.getLevel(), p);
            effect(h, p, MobEffects.MOVEMENT_SPEED, Math.round(900 * multipliers[stage]), stage == 0 ? 1 : 0);
            effect(h, p, MobEffects.HUNGER, 300, 0);
            new ItemStack(ModItems.GLOW_BETEL_NUT).finishUsingItem(h.getLevel(), p);
            effect(h, p, MobEffects.GLOWING, 900, 0);
        }
        state.clearAddiction(p); h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void honeyClearsPoisonWithoutCuringDependenceOrDisease(GameTestHelper h) {
        var p = player(h); var state = BetelNutEntityComponents.ADDICTION.get(p);
        state.setAddictionValue(3); state.setOralExposureDays(3); state.maintainOralEffects(p);
        p.addEffect(new MobEffectInstance(MobEffects.POISON, 400, 1));
        new ItemStack(ModItems.HONEY_BETEL_NUT).finishUsingItem(h.getLevel(), p);
        h.assertTrue(!p.hasEffect(MobEffects.POISON) && p.hasEffect(MobEffects.REGENERATION), "Honey cleanses poison and grants mild regeneration");
        h.assertTrue(state.getAddictionValue() >= 3 && state.getOralExposureDays() >= 3 && p.hasEffect(ModEffects.ORAL_CANCER), "Honey never cures persistent disease");
        h.assertTrue(!p.hasEffect(MobEffects.ABSORPTION), "Honey is not a golden apple substitute");
        state.clearAddiction(p); h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void honeyStillHealsOnceAtFullDependence(GameTestHelper h) throws ReflectiveOperationException {
        var p = player(h); var state = BetelNutEntityComponents.ADDICTION.get(p);
        state.setAddictionValue(7); p.setHealth(10);
        new ItemStack(ModItems.HONEY_BETEL_NUT).finishUsingItem(h.getLevel(), p);
        effect(h, p, MobEffects.REGENERATION, 50, 0);
        var tickEffects = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("tickEffects");
        tickEffects.setAccessible(true); tickEffects.invoke(p);
        h.assertTrue(p.getHealth() > 10, "The shortest regeneration still reaches a real healing tick");
        state.clearAddiction(p); h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void stageTwoBetelExceptionCoversEveryFlavorAndBothHands(GameTestHelper h)
            throws ReflectiveOperationException {
        var p = player(h); var state = BetelNutEntityComponents.ADDICTION.get(p);
        var config = BetelNutConfig.get(); boolean teleport = config.enableEnderBetelTeleport;
        boolean repeat = BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath;
        try {
            config.enableEnderBetelTeleport = false;
            // The separate synthetic-repeat test covers that independent restriction.
            BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath = true;
            for (var item : betelFoods(h)) for (var hand : InteractionHand.values()) {
                state.clearAddiction(p); p.removeAllEffects(); p.getInventory().clearContent();
                state.setOralExposureDays(config.oralCancerStage2Days); state.maintainOralEffects(p);
                var stack = new ItemStack(item); p.setItemInHand(hand, stack);
                h.assertTrue(stack.use(p.level(), p, hand).getResult().consumesAction(),
                        "Stage II can start every betel flavor in either hand: " + item);
                finishBite(p, hand);
                h.assertTrue(p.getItemInHand(hand).is(ModItems.BETEL_NUT_RESIDUE)
                        && p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE) == 1,
                        "Stage II actually consumes betel and yields exactly one residue: " + item);
                h.assertTrue(state.getAddictionValue() == 1
                        && state.getOralExposureDays() == config.oralCancerStage2Days + 1,
                        "Allowed betel still advances daily dependence and oral exposure: " + item);

                state.clearAddiction(p); p.removeAllEffects(); p.getInventory().clearContent();
                state.setOralExposureDays(config.oralCancerStage2Days); state.maintainOralEffects(p);
                stack = new ItemStack(item, 2); p.setItemInHand(hand, stack);
                h.assertTrue(stack.use(p.level(), p, hand).getResult().consumesAction(),
                        "Betel can start before the jaw locks: " + item);
                state.setOralExposureDays(config.oralCancerStage3Days); state.maintainOralEffects(p);
                finishBite(p, hand);
                h.assertTrue(p.getItemInHand(hand).is(item) && p.getItemInHand(hand).getCount() == 2
                        && state.getAddictionValue() == 0
                        && p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE) == 0,
                        "A jaw locking during use blocks the bite, rewards and residue: " + item);
            }
        } finally {
            config.enableEnderBetelTeleport = teleport;
            BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath = repeat;
            state.clearAddiction(p); p.removeAllEffects();
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void oralStageThreeBlocksEveryNutBeforeAnyReward(GameTestHelper h) {
        var p = player(h); var state = BetelNutEntityComponents.ADDICTION.get(p);
        state.setOralExposureDays(BetelNutConfig.get().oralCancerStage3Days); state.maintainOralEffects(p);
        for (var item : net.minecraft.core.registries.BuiltInRegistries.ITEM) {
            if (!(item instanceof BetelNutItem)) continue;
            var stack = new ItemStack(item, 2);
            stack.finishUsingItem(h.getLevel(), p);
            h.assertTrue(stack.getCount() == 2 && state.getAddictionValue() == 0, "Blocked nut cannot consume or grant a reward: " + item);
        }
        h.assertTrue(p.getActiveEffects().size() == 2 && p.hasEffect(ModEffects.ORAL_CANCER)
                && p.hasEffect(ModEffects.MOUTH_LOCKED), "Only the existing oral disease and locked jaw remain");
        state.clearAddiction(p); h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void syntheticUsesToleranceAndRejectsRepeatAtFinish(GameTestHelper h) {
        var p = player(h); var state = BetelNutEntityComponents.ADDICTION.get(p);
        int old = BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds;
        boolean repeat = BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath;
        try {
            BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds = 120;
            BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath = false;
            state.setAddictionValue(7);
            new ItemStack(ModItems.SYNTHETIC_WORLD_BETEL).finishUsingItem(h.getLevel(), p);
            effect(h, p, MobEffects.MOVEMENT_SPEED, 600, 0);
            effect(h, p, MobEffects.DIG_SPEED, 600, 0);
            h.assertTrue(!p.hasEffect(MobEffects.DAMAGE_BOOST) && !p.hasEffect(MobEffects.FIRE_RESISTANCE) && !p.hasEffect(MobEffects.WATER_BREATHING), "No hidden all-potion burst");
            var save = new CompoundTag(); state.writeToNbt(save, h.getLevel().registryAccess());
            h.assertTrue(save.getInt("hechengTianxiaPositiveTicks") == 600, "Crash starts when the tolerance-shortened burst ends");
            var second = new ItemStack(ModItems.SYNTHETIC_WORLD_BETEL, 2);
            int residue = p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE);
            second.finishUsingItem(h.getLevel(), p);
            h.assertTrue(second.getCount() == 2 && p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE) == residue, "Repeat finish cannot eat or issue residue");
            p.setItemInHand(InteractionHand.MAIN_HAND, second);
            h.assertTrue(ModItems.SYNTHETIC_WORLD_BETEL.use(h.getLevel(), p, InteractionHand.MAIN_HAND).getResult() == net.minecraft.world.InteractionResult.FAIL, "Normal right-click also rejects repeats");
        } finally {
            BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds = old;
            BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath = repeat;
            state.clearAddiction(p);
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void syntheticCrashPersistsAndRecoversWithoutExtraBlindness(GameTestHelper h) throws ReflectiveOperationException {
        var p = player(h); var state = BetelNutEntityComponents.ADDICTION.get(p);
        var save = new CompoundTag(); save.putInt("dailySchema", 2); save.putInt("hechengTianxiaPositiveTicks", 1);
        state.readFromNbt(save, h.getLevel().registryAccess());
        state.tickHechengTianxiaAftereffects(p);
        h.assertTrue(state.isHechengTianxiaComatose(), "Saved pending burst enters blackout after reload");
        h.assertTrue(p.hasEffect(MobEffects.BLINDNESS), "Blackout is visible");
        var reload = new CompoundTag(); state.writeToNbt(reload, h.getLevel().registryAccess());
        state.readFromNbt(reload, h.getLevel().registryAccess());
        var tickEffects = net.minecraft.world.entity.LivingEntity.class.getDeclaredMethod("tickEffects");
        tickEffects.setAccessible(true);
        for (int i = 0; i < HechengTianxiaEffects.comaDurationTicks(); i++) {
            tickEffects.invoke(p); state.tickHechengTianxiaAftereffects(p);
        }
        h.assertTrue(!state.isHechengTianxiaComatose() && state.hasActiveHechengTianxiaAftermath(), "Short blackout gives way to fatigue");
        effect(h, p, MobEffects.WEAKNESS, 40, 0);
        h.assertTrue(!p.hasEffect(MobEffects.DARKNESS) && !p.hasEffect(MobEffects.UNLUCK), "Fatigue has no unrelated darkness or bad luck");
        for (int i = 0; i < HechengTianxiaEffects.negativeDurationTicks() + 40; i++) {
            tickEffects.invoke(p); state.tickHechengTianxiaAftereffects(p);
        }
        h.assertTrue(!state.hasActiveHechengTianxiaAftermath() && !p.hasEffect(MobEffects.MOVEMENT_SLOWDOWN)
                && !p.hasEffect(MobEffects.DIG_SLOWDOWN) && !p.hasEffect(MobEffects.WEAKNESS)
                && !p.hasEffect(MobEffects.BLINDNESS), "All crash restrictions expire");
        state.clearAddiction(p); h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void echoAftermathMatchesAppliedDurationEvenWithAddictionDisabled(GameTestHelper h) {
        var p = player(h); var state = BetelNutEntityComponents.ADDICTION.get(p);
        var config = BetelNutConfig.get(); boolean enabled = config.enableAddictionSystem;
        try {
            state.setAddictionValue(7); config.enableAddictionSystem = false;
            new ItemStack(ModItems.ECHO_BETEL).finishUsingItem(h.getLevel(), p);
            effect(h, p, ModEffects.ECHO_SIGHT, 900, 0);
            for (int i = 0; i < 899; i++) state.serverTick(p);
            h.assertTrue(!p.hasEffect(MobEffects.CONFUSION), "Nausea does not arrive during active echo sight");
            state.serverTick(p); effect(h, p, MobEffects.CONFUSION, 160, 0);
            state.serverTick(p); effect(h, p, MobEffects.CONFUSION, 160, 0);
            new ItemStack(ModItems.SPICY_BETEL_NUT).finishUsingItem(h.getLevel(), p);
            for (int i = 0; i < 3; i++) state.serverTick(p);
            effect(h, p, MobEffects.HUNGER, 200, 0);
        } finally { config.enableAddictionSystem = enabled; state.clearAddiction(p); }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void legacySyntheticDefaultsUpgradeOnceAndPreserveCustomValues(GameTestHelper h) throws ReflectiveOperationException {
        int positive = BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds;
        int coma = BetelNutMidnightConfig.hechengTianxiaComaDurationSeconds;
        int negative = BetelNutMidnightConfig.hechengTianxiaNegativeDurationSeconds;
        int version = BetelNutMidnightConfig.effectBalanceVersion;
        var upgrade = BetelNutMidnightConfig.class.getDeclaredMethod("upgradeLegacyEffectDefaults"); upgrade.setAccessible(true);
        try {
            BetelNutMidnightConfig.effectBalanceVersion = 0;
            BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds = 300;
            BetelNutMidnightConfig.hechengTianxiaComaDurationSeconds = 120;
            BetelNutMidnightConfig.hechengTianxiaNegativeDurationSeconds = 600;
            upgrade.invoke(null);
            h.assertTrue(HechengTianxiaEffects.positiveDurationTicks() == 2400 && HechengTianxiaEffects.comaDurationTicks() == 100 && HechengTianxiaEffects.negativeDurationTicks() == 1200, "Old defaults adopt the new 120/5/60-second balance");
            BetelNutMidnightConfig.hechengTianxiaComaDurationSeconds = 120; upgrade.invoke(null);
            h.assertTrue(BetelNutMidnightConfig.hechengTianxiaComaDurationSeconds == 120, "An intentional setting after migration is preserved");
            BetelNutMidnightConfig.effectBalanceVersion = 0;
            BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds = 80;
            BetelNutMidnightConfig.hechengTianxiaComaDurationSeconds = 3;
            BetelNutMidnightConfig.hechengTianxiaNegativeDurationSeconds = 25;
            upgrade.invoke(null);
            h.assertTrue(HechengTianxiaEffects.positiveDurationTicks() == 1600 && HechengTianxiaEffects.comaDurationTicks() == 60 && HechengTianxiaEffects.negativeDurationTicks() == 500, "Custom timing survives migration");
        } finally {
            BetelNutMidnightConfig.hechengTianxiaPositiveDurationSeconds = positive;
            BetelNutMidnightConfig.hechengTianxiaComaDurationSeconds = coma;
            BetelNutMidnightConfig.hechengTianxiaNegativeDurationSeconds = negative;
            BetelNutMidnightConfig.effectBalanceVersion = version;
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void failedOrDisabledEnderTeleportGrantsNoProtection(GameTestHelper h) {
        var p = player(h); var config = BetelNutConfig.get(); boolean enabled = config.enableEnderBetelTeleport;
        try {
            config.enableEnderBetelTeleport = false;
            h.assertTrue(!EnderBetelTeleportHandler.tryTeleport(p) && !p.hasEffect(MobEffects.DAMAGE_RESISTANCE), "Disabled teleport grants no protection");
            config.enableEnderBetelTeleport = true;
            p.teleportTo(p.getX(), h.getLevel().getMaxBuildHeight() + 100, p.getZ());
            h.assertTrue(!EnderBetelTeleportHandler.tryTeleport(p) && !p.hasEffect(MobEffects.DAMAGE_RESISTANCE), "No safe landing means no protection");
        } finally { config.enableEnderBetelTeleport = enabled; }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void successfulEnderTeleportUsesScaledFiveSecondProtection(GameTestHelper h) {
        var p = player(h); var state = BetelNutEntityComponents.ADDICTION.get(p);
        var config = BetelNutConfig.get(); boolean enabled = config.enableEnderBetelTeleport;
        int min = config.enderBetelTeleportRadiusMin, max = config.enderBetelTeleportRadiusMax;
        try {
            config.enableEnderBetelTeleport = true;
            config.enderBetelTeleportRadiusMin = config.enderBetelTeleportRadiusMax = 8;
            var origin = h.absolutePos(new net.minecraft.core.BlockPos(2, 4, 2));
            p.teleportTo(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
            var random = net.minecraft.util.RandomSource.create(777);
            random.nextInt(1); double angle = random.nextDouble() * Math.PI * 2;
            var target = origin.offset((int) Math.round(Math.cos(angle) * 8), 8, (int) Math.round(Math.sin(angle) * 8));
            h.getLevel().getChunkAt(target);
            h.getLevel().setBlock(target.below(), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState(), 3);
            h.getLevel().setBlock(target, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            h.getLevel().setBlock(target.above(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(), 3);
            for (int days : new int[]{0, 7}) {
                state.clearAddiction(p); p.removeAllEffects(); state.setAddictionValue(days);
                p.teleportTo(origin.getX() + .5, origin.getY(), origin.getZ() + .5);
                p.getRandom().setSeed(777);
                h.assertTrue(EnderBetelTeleportHandler.tryTeleport(p) && p.blockPosition().equals(target), "Uses the actual safe landing search");
                effect(h, p, MobEffects.DAMAGE_RESISTANCE, days == 0 ? 100 : 25, 0);
            }
        } finally {
            config.enableEnderBetelTeleport = enabled; config.enderBetelTeleportRadiusMin = min; config.enderBetelTeleportRadiusMax = max;
            state.clearAddiction(p);
        }
        h.succeed();
    }
}
