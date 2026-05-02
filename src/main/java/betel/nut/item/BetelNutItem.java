package betel.nut.item;

import java.util.List;
import java.util.function.IntSupplier;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMidnightConfig;
import betel.nut.advancement.BetelQuestAdvancements;
import betel.nut.addiction.AddictionStageUtil;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.event.WithdrawalEatingRestrictions;
import betel.nut.skyblock.BetelSkyblockManager;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;

public class BetelNutItem extends BetelNutFoodItem {
	private final IntSupplier addictionIncreaseSupplier;
	private final List<EffectSpec> effects;

	public BetelNutItem(Properties properties, int addictionIncrease, List<EffectSpec> effects) {
		super(properties);
		this.addictionIncreaseSupplier = () -> addictionIncrease;
		this.effects = effects;
	}

	public BetelNutItem(Properties properties, IntSupplier addictionIncreaseSupplier, List<EffectSpec> effects) {
		super(properties);
		this.addictionIncreaseSupplier = addictionIncreaseSupplier;
		this.effects = effects;
	}

	@Override
	public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer
				&& stack.is(ModItems.SYNTHETIC_WORLD_BETEL)) {
			var addiction = BetelNutEntityComponents.ADDICTION.get(serverPlayer);
			if (!BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath
					&& addiction.hasActiveHechengTianxiaAftermath()) {
				addiction.sendHechengTianxiaRepeatBlockedMessage(serverPlayer);
				return InteractionResultHolder.fail(stack);
			}
		}
		return super.use(level, player, hand);
	}

	@Override
	public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
		boolean isEnderBetelNut = stack.is(ModItems.ENDER_BETEL_NUT);
		boolean isSyntheticWorldBetel = stack.is(ModItems.SYNTHETIC_WORLD_BETEL);
		ItemStack result = super.finishUsingItem(stack, level, entity);

		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			var addiction = BetelNutEntityComponents.ADDICTION.get(player);
			BetelNutConfig config = BetelNutConfig.get();
			int rewardStage = config.enableAddictionSystem ? addiction.getAddictionStage() : 0;
			int suppressSlownessTicks = 0;
			int suppressMiningFatigueTicks = 0;
			int suppressWeaknessTicks = 0;
			for (EffectSpec effect : this.effects) {
				int durationTicks = getScaledRewardDurationTicks(effect, rewardStage);
				int amplifier = getScaledRewardAmplifier(effect, rewardStage);
				player.addEffect(new MobEffectInstance(effect.effect(), durationTicks, amplifier));
				if (!isSyntheticWorldBetel) {
					if (effect.effect().equals(MobEffects.MOVEMENT_SPEED)) {
						suppressSlownessTicks = Math.max(suppressSlownessTicks, durationTicks);
					} else if (effect.effect().equals(MobEffects.DIG_SPEED)) {
						suppressMiningFatigueTicks = Math.max(suppressMiningFatigueTicks, durationTicks);
					} else if (effect.effect().equals(MobEffects.DAMAGE_BOOST)) {
						suppressWeaknessTicks = Math.max(suppressWeaknessTicks, durationTicks);
					}
				}
			}

			boolean suppressedWithdrawalConflicts = false;
			if (!isSyntheticWorldBetel) {
				suppressedWithdrawalConflicts = addiction.suppressConflictingWithdrawalEffects(player,
						suppressSlownessTicks, suppressMiningFatigueTicks, suppressWeaknessTicks);
			}
			if (isSyntheticWorldBetel) {
				suppressedWithdrawalConflicts = addiction.startHechengTianxiaAftermath(player,
						BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath);
				BetelQuestAdvancements.grantEatHechengTianxia(player);
			}

			if (config.enableAddictionSystem) {
				addiction.eatBetelNut(player,
						Math.max(0, this.addictionIncreaseSupplier.getAsInt()),
						level.getGameTime(),
						WithdrawalEatingRestrictions.shouldClearWithdrawalEffectsOnEat(stack));
				addiction.sendBetelSuppressionFeedbackIfNeeded(player, suppressedWithdrawalConflicts);
			}
			BetelSkyblockManager.onBetelNutEaten(player);

			if (isEnderBetelNut) {
				EnderBetelTeleportHandler.tryTeleport(player);
			}
		}

		return result;
	}

	public int getAddictionIncrease() {
		return Math.max(0, this.addictionIncreaseSupplier.getAsInt());
	}

	private static int getScaledRewardDurationTicks(EffectSpec effect, int addictionStage) {
		int durationTicks = Math.max(1, effect.durationTicks());
		if (!isBeneficialReward(effect)) {
			return durationTicks;
		}
		float multiplier = AddictionStageUtil.getBetelRewardDurationMultiplier(addictionStage);
		return Math.max(1, Math.round(durationTicks * multiplier));
	}

	private static int getScaledRewardAmplifier(EffectSpec effect, int addictionStage) {
		int amplifier = Math.max(0, effect.amplifier());
		if (!isBeneficialReward(effect)) {
			return amplifier;
		}
		int penalty = AddictionStageUtil.getBetelRewardAmplifierPenalty(addictionStage);
		return Math.max(0, amplifier - penalty);
	}

	private static boolean isBeneficialReward(EffectSpec effect) {
		return effect.effect().value().getCategory() == MobEffectCategory.BENEFICIAL;
	}

	public record EffectSpec(Holder<MobEffect> effect, int durationTicks, int amplifier) {
	}
}
