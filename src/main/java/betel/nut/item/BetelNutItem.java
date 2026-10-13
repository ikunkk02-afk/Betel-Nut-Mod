package betel.nut.item;

import java.util.List;
import java.util.function.IntSupplier;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMidnightConfig;
import betel.nut.advancement.BetelQuestAdvancements;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.event.WithdrawalEatingRestrictions;
import betel.nut.skyblock.BetelSkyblockManager;
import betel.nut.effect.ModEffects;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public class BetelNutItem extends BetelNutFoodItem {
	private final IntSupplier addictionIncreaseSupplier;
	private final BetelEffectProfile profile;

	public BetelNutItem(Properties properties, int addictionIncrease, BetelEffectProfile profile) {
		super(properties);
		this.addictionIncreaseSupplier = () -> addictionIncrease;
		this.profile = profile;
	}

	public BetelNutItem(Properties properties, IntSupplier addictionIncreaseSupplier, BetelEffectProfile profile) {
		super(properties);
		this.addictionIncreaseSupplier = addictionIncreaseSupplier;
		this.profile = profile;
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
		// Finish-use must enforce the same repeat guard as right-click, before consuming anything.
		if (isSyntheticWorldBetel && !level.isClientSide() && entity instanceof ServerPlayer player
				&& !BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath
				&& BetelNutEntityComponents.ADDICTION.get(player).hasActiveHechengTianxiaAftermath()) {
			return stack;
		}
		ItemStack result = super.finishUsingItem(stack, level, entity);

		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			var addiction = BetelNutEntityComponents.ADDICTION.get(player);
			addiction.updateDisabledWithdrawalEffects(player);
			BetelNutConfig config = BetelNutConfig.get();
			int rewardStage = config.enableAddictionSystem ? addiction.getAddictionStage() : 0;
			if (this.profile == BetelEffectProfile.HONEY) player.removeEffect(MobEffects.POISON);
			int suppressSlownessTicks = 0;
			int suppressMiningFatigueTicks = 0;
			int suppressWeaknessTicks = 0;
			for (var effect : isSyntheticWorldBetel ? List.<BetelEffectProfile.EffectSpec>of() : this.profile.scaledEffects(rewardStage)) {
				int durationTicks = effect.durationTicks();
				int amplifier = effect.amplifier();
				player.addEffect(new MobEffectInstance(effect.effect(), durationTicks, amplifier));
				if (effect.effect().equals(ModEffects.ECHO_SIGHT)) addiction.startEchoAftermath(durationTicks);
				if (effect.effect().equals(MobEffects.MOVEMENT_SPEED)) {
					suppressSlownessTicks = Math.max(suppressSlownessTicks, durationTicks);
				} else if (effect.effect().equals(MobEffects.DIG_SPEED)) {
					suppressMiningFatigueTicks = Math.max(suppressMiningFatigueTicks, durationTicks);
				} else if (effect.effect().equals(MobEffects.DAMAGE_BOOST)) {
					suppressWeaknessTicks = Math.max(suppressWeaknessTicks, durationTicks);
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
				EnderBetelTeleportHandler.tryTeleport(player, false, rewardStage);
			}
		}

		return result;
	}

	public int getAddictionIncrease() {
		return 1;
	}

	@Override
	public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
		lines.add(Component.translatable("tooltip.betel_nut_mod.effects.base").withStyle(ChatFormatting.GRAY));
		var displayedEffects = this.profile == BetelEffectProfile.ENDER
				? List.of(BetelEffectProfile.enderProtection(0)) : this.profile.baseEffects();
		for (var effect : displayedEffects) {
			Component name = Component.translatable(effect.effect().value().getDescriptionId());
			if (effect.amplifier() > 0) name = Component.translatable("potion.withAmplifier", name,
					Component.translatable("potion.potency." + effect.amplifier()));
			String time = String.format(java.util.Locale.ROOT, "%d:%02d", effect.durationTicks() / 1200,
					(effect.durationTicks() / 20) % 60);
			lines.add((this.profile == BetelEffectProfile.SYNTHETIC ? name.copy()
					: Component.translatable("potion.withDuration", name, time))
					.withStyle(effect.effect().value().getCategory() == net.minecraft.world.effect.MobEffectCategory.BENEFICIAL
							? ChatFormatting.BLUE : ChatFormatting.RED));
		}
		lines.add(Component.translatable("tooltip.betel_nut_mod.effects." + this.profile.name().toLowerCase(java.util.Locale.ROOT))
				.withStyle(ChatFormatting.GRAY));
		lines.add(Component.translatable("tooltip.betel_nut_mod.effects.tolerance").withStyle(ChatFormatting.DARK_GRAY));
	}
}
