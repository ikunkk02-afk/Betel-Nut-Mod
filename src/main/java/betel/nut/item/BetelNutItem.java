package betel.nut.item;

import java.util.List;
import java.util.function.IntSupplier;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMidnightConfig;
import betel.nut.advancement.BetelQuestAdvancements;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.event.WithdrawalEatingRestrictions;
import betel.nut.skyblock.BetelSkyblockManager;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffectInstance;
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
			for (EffectSpec effect : this.effects) {
				player.addEffect(new MobEffectInstance(effect.effect(), effect.durationTicks(), effect.amplifier()));
			}

			var addiction = BetelNutEntityComponents.ADDICTION.get(player);
			if (isSyntheticWorldBetel) {
				addiction.startHechengTianxiaAftermath(player,
						BetelNutMidnightConfig.hechengTianxiaAllowRepeatBeforeAftermath);
				BetelQuestAdvancements.grantEatHechengTianxia(player);
			}

			if (BetelNutConfig.get().enableAddictionSystem) {
				addiction.eatBetelNut(player,
						Math.max(0, this.addictionIncreaseSupplier.getAsInt()),
						level.getGameTime(),
						WithdrawalEatingRestrictions.shouldClearWithdrawalEffectsOnEat(stack));
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

	public record EffectSpec(Holder<MobEffect> effect, int durationTicks, int amplifier) {
	}
}
