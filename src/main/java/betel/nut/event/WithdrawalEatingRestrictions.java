package betel.nut.event;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.component.BetelNutAddictionComponent;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.item.ModItemTags;
import betel.nut.item.RecoveryDrinkItem;
import betel.nut.effect.ModEffects;
import betel.nut.message.BetelMessages;
import net.fabricmc.fabric.api.event.player.UseItemCallback;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

public final class WithdrawalEatingRestrictions {
	public enum RestrictionLevel {
		NONE("none", ""),
		MOUTH_LOCKED("mouth_locked", "message.betel-nut-mod.mouth_locked"),
		SOFT_FOODS("soft_food_only", BetelMessages.EATING_RESTRICTION_STAGE2),
		ENCHANTED_GOLDEN_APPLE_ONLY("enchanted_golden_apple_only",
				BetelMessages.EATING_RESTRICTION_STAGE3),
		BODY_REJECTS_FOOD("enchanted_golden_apple_only", BetelMessages.EATING_RESTRICTION_STAGE4);

		private final String label;
		private final String blockMessage;

		RestrictionLevel(String label, String blockMessage) {
			this.label = label;
			this.blockMessage = blockMessage;
		}

		public String label() {
			return this.label;
		}

		private String blockMessage() {
			return this.blockMessage;
		}
	}

	public record EatingRestrictionCheck(
			RestrictionLevel restrictionLevel,
			boolean food,
			boolean checkedItem,
			boolean allowed,
			String matchedAllowedTags,
			String reason) {
	}

	private static boolean registered;

	public static void register() {
		if (registered) {
			return;
		}

		UseItemCallback.EVENT.register(WithdrawalEatingRestrictions::onUseItem);
		registered = true;
		BetelNutMod.LOGGER.info("Withdrawal eating restriction handler registered.");
	}

	public static boolean isFeatureEnabled(BetelNutConfig config) {
		return config.enableAddictionSystem && config.enableWithdrawalEatingRestriction;
	}

	public static RestrictionLevel getRestrictionLevel(ServerPlayer player,
			BetelNutAddictionComponent addiction) {
		BetelNutConfig config = BetelNutConfig.get();
		if (player.isCreative() || player.isSpectator()) {
			return RestrictionLevel.NONE;
		}
        int disease = addiction.getOralCancerStage();
        if (player.hasEffect(ModEffects.ORAL_CANCER))
            disease = Math.max(disease, player.getEffect(ModEffects.ORAL_CANCER).getAmplifier() + 1);
        int severity = isFeatureEnabled(config) && addiction.getCleanTime() <= player.level().getGameTime()
                ? addiction.getWithdrawalSeverity() : 0;
        if (player.hasEffect(ModEffects.MOUTH_LOCKED) || disease >= 3 || severity >= 3)
            return RestrictionLevel.MOUTH_LOCKED;
        if (disease >= 2 || severity >= 2) return RestrictionLevel.SOFT_FOODS;
		return RestrictionLevel.NONE;
	}

	public static boolean isCheckedItem(ItemStack stack) {
		return !stack.isEmpty()
				&& (isFoodItem(stack)
						|| stack.is(Items.MILK_BUCKET)
						|| stack.is(Items.POTION)
						|| stack.is(ModItemTags.WITHDRAWAL_STAGE2_ALLOWED_FOODS)
						|| stack.is(ModItemTags.WITHDRAWAL_STAGE3_ALLOWED_FOODS));
	}

	public static boolean isFoodItem(ItemStack stack) {
		return !stack.isEmpty() && (stack.has(DataComponents.FOOD) || stack.is(Items.CAKE));
	}

	public static boolean isBetelNut(ItemStack stack) {
		return !stack.isEmpty() && stack.is(ModItemTags.BETEL_NUTS);
	}

	public static boolean canEatDuringWithdrawal(ItemStack stack) {
		return isBetelNut(stack);
	}

	public static boolean shouldClearWithdrawalEffectsOnEat(ItemStack stack) {
		return false;
	}

	public static EatingRestrictionCheck evaluate(ServerPlayer player,
			BetelNutAddictionComponent addiction, ItemStack stack) {
		RestrictionLevel level = getRestrictionLevel(player, addiction);
		boolean checkedItem = isCheckedItem(stack);
		boolean allowed = level == RestrictionLevel.NONE
				|| !checkedItem
				|| isAllowedAtLevel(stack, level, BetelNutConfig.get());
		return new EatingRestrictionCheck(level, isFoodItem(stack), checkedItem, allowed,
				matchedAllowedTags(stack), reason(player, addiction, stack, level, checkedItem, allowed));
	}

	public static boolean canUseAtCurrentRestriction(ServerPlayer player,
			BetelNutAddictionComponent addiction, ItemStack stack) {
		return evaluate(player, addiction, stack).allowed();
	}

	public static boolean shouldBlockUse(ServerPlayer player, ItemStack stack, boolean notify) {
		if (stack.isEmpty()) {
			return false;
		}

		BetelNutAddictionComponent addiction = BetelNutEntityComponents.ADDICTION.get(player);
		EatingRestrictionCheck check = evaluate(player, addiction, stack);
		if (check.allowed()) {
			return false;
		}

		if (notify) {
			addiction.sendEatingRestrictionMessage(player, check.restrictionLevel().blockMessage());
		}

		BetelNutMod.LOGGER.debug(
				"Blocked withdrawal eating for {}: item={}, addiction={}, stage={}, withdrawal={}, severity={}, restrictionLevel={}, matchedTags={}, reason={}",
				player.getScoreboardName(), BuiltInRegistries.ITEM.getKey(stack.getItem()),
				addiction.getAddictionValue(), addiction.getAddictionStage(), addiction.getWithdrawalValue(),
				addiction.getWithdrawalSeverity(), check.restrictionLevel().label(),
				check.matchedAllowedTags(), check.reason());
		return true;
	}

	private static InteractionResultHolder<ItemStack> onUseItem(Player player, Level level, InteractionHand hand) {
		ItemStack stack = player.getItemInHand(hand);
		if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
			return InteractionResultHolder.pass(stack);
		}

		return shouldBlockUse(serverPlayer, stack, true)
				? InteractionResultHolder.fail(stack)
				: InteractionResultHolder.pass(stack);
	}

    private static boolean isAllowedAtLevel(ItemStack stack, RestrictionLevel level, BetelNutConfig config) {
        if (stack.getItem() instanceof RecoveryDrinkItem) return true;
        return switch (level) {
            case NONE -> true;
            case SOFT_FOODS -> isAllowedAsSoftFood(stack, config);
            case MOUTH_LOCKED, ENCHANTED_GOLDEN_APPLE_ONLY, BODY_REJECTS_FOOD ->
                    !isFoodItem(stack) && (stack.is(Items.POTION) || stack.is(Items.MILK_BUCKET));
        };
    }

    private static boolean isAllowedAsSoftFood(ItemStack stack, BetelNutConfig config) {
        // Betel is a gameplay exception to the soft-food gate, but never to a locked jaw.
        if (isBetelNut(stack)) return true;
        // Hard solids cannot become soft through an old config or a broad liquid-food tag.
        if (stack.is(Items.GOLDEN_APPLE) || stack.is(Items.ENCHANTED_GOLDEN_APPLE)) return false;
        if (stack.is(Items.MILK_BUCKET)) return config.allowMilkInStage2;
        return stack.is(Items.POTION) || stack.is(ModItemTags.LIQUID_FOODS)
                || stack.is(ModItemTags.WITHDRAWAL_STAGE2_ALLOWED_FOODS);
    }

	private static String matchedAllowedTags(ItemStack stack) {
		boolean betelNut = stack.is(ModItemTags.BETEL_NUTS);
		boolean stage2 = stack.is(ModItemTags.WITHDRAWAL_STAGE2_ALLOWED_FOODS);
		boolean stage3 = stack.is(ModItemTags.WITHDRAWAL_STAGE3_ALLOWED_FOODS);
		if (betelNut && stage2 && stage3) {
			return "betel_nuts,withdrawal_stage2_allowed_foods,withdrawal_stage3_allowed_foods";
		}
		if (betelNut && stage2) {
			return "betel_nuts,withdrawal_stage2_allowed_foods";
		}
		if (betelNut && stage3) {
			return "betel_nuts,withdrawal_stage3_allowed_foods";
		}
		if (betelNut) {
			return "betel_nuts";
		}
		if (stage2 && stage3) {
			return "withdrawal_stage2_allowed_foods,withdrawal_stage3_allowed_foods";
		}
		if (stage2) {
			return "withdrawal_stage2_allowed_foods";
		}
		if (stage3) {
			return "withdrawal_stage3_allowed_foods";
		}
		return "none";
	}

	private static String reason(ServerPlayer player, BetelNutAddictionComponent addiction, ItemStack stack,
			RestrictionLevel level, boolean checkedItem, boolean allowed) {
		BetelNutConfig config = BetelNutConfig.get();
		if (stack.isEmpty()) {
			return "empty_hand";
		}
		if (!isFeatureEnabled(config) && level == RestrictionLevel.NONE) return "feature_disabled";
		if (player.isCreative() || player.isSpectator()) {
			return "creative_or_spectator";
		}
		if (level == RestrictionLevel.NONE && addiction.getCleanTime() > player.level().getGameTime()) {
			return "clean_time_protection";
		}
		if (!checkedItem) {
			return "not_food_or_recovery_drink";
		}
		if (level == RestrictionLevel.NONE) {
			return "withdrawal_below_eating_restriction_threshold";
		}
		if (allowed) {
			if (isBetelNut(stack)) {
				return "allowed_betel_nuts";
			}
			if (stack.is(Items.ENCHANTED_GOLDEN_APPLE)) {
				return "allowed_enchanted_golden_apple";
			}
			if (stack.is(Items.MILK_BUCKET)) {
				return "allowed_milk_stage2";
			}
			return "allowed_by_" + matchedAllowedTags(stack);
		}
		return switch (level) {
			case MOUTH_LOCKED -> "blocked_mouth_locked";
			case SOFT_FOODS -> "blocked_soft_food_only";
			case ENCHANTED_GOLDEN_APPLE_ONLY -> "blocked_enchanted_golden_apple_only";
			case BODY_REJECTS_FOOD -> "blocked_body_rejects_food";
			case NONE -> "allowed";
		};
	}

	private WithdrawalEatingRestrictions() {
	}
}
