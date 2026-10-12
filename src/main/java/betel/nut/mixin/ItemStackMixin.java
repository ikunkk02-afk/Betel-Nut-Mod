package betel.nut.mixin;

import betel.nut.component.BetelNutAddictionComponent;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.event.BetelNutEvents;
import betel.nut.event.WithdrawalEatingRestrictions;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemStack.class)
public class ItemStackMixin {
	@Inject(method = "use", at = @At("HEAD"), cancellable = true)
	private void betelNutMod$blockRestrictedUse(Level level, Player player, InteractionHand hand,
			CallbackInfoReturnable<InteractionResultHolder<ItemStack>> cir) {
		if (!level.isClientSide() && player instanceof ServerPlayer serverPlayer) {
			ItemStack stack = (ItemStack) (Object) this;
			BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(serverPlayer);
			if (addiction.blockHechengTianxiaActionIfComatose(serverPlayer, true)) {
				cir.setReturnValue(InteractionResultHolder.fail(stack));
				return;
			}
			if (WithdrawalEatingRestrictions.shouldBlockUse(serverPlayer, stack, true)) {
				cir.setReturnValue(InteractionResultHolder.fail(stack));
			}
		}
	}

	@WrapMethod(method = "finishUsingItem")
	private ItemStack betelNutMod$handleFinishedUsingItem(Level level, LivingEntity entity,
			Operation<ItemStack> original) {
		if (!level.isClientSide() && entity instanceof ServerPlayer player) {
			ItemStack stack = (ItemStack) (Object) this;
			BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
			if (addiction.blockHechengTianxiaActionIfComatose(player, true)) {
				return stack;
			}
			if (WithdrawalEatingRestrictions.shouldBlockUse(player, stack, true)) {
				return stack;
			}
			// Keep the consumed identity even when the last item becomes empty or a bucket.
			ItemStack consumed = stack.copyWithCount(1);
			ItemStack result = original.call(level, entity);
			// Milk must finish clearing vanilla effects before persistent symptoms are restored.
			BetelNutEvents.handleFinishedUsingItem(player, consumed, level.getGameTime());
			return result;
		}
		return original.call(level, entity);
	}
}
