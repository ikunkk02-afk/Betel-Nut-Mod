package betel.nut.item;

import java.util.List;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.message.BetelMessages;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.food.FoodProperties;
import net.minecraft.world.food.Foods;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.UseAnim;
import net.minecraft.world.level.Level;

/** Bottled remedies can be swallowed without chewing, even with a locked jaw. */
public final class RecoveryDrinkItem extends Item {
    public enum Kind { GOLDEN_PASTE, ENCHANTED_PASTE, RELIEF }
    private final Kind kind;

    public RecoveryDrinkItem(Kind kind) {
        super(properties(kind));
        this.kind = kind;
    }

    private static Properties properties(Kind kind) {
        FoodProperties food = switch (kind) {
            case GOLDEN_PASTE -> Foods.GOLDEN_APPLE;
            case ENCHANTED_PASTE -> Foods.ENCHANTED_GOLDEN_APPLE;
            case RELIEF -> new FoodProperties.Builder().nutrition(0).saturationModifier(0).alwaysEdible().build();
        };
        return new Properties().stacksTo(16).craftRemainder(Items.GLASS_BOTTLE).food(food)
                .rarity(kind == Kind.ENCHANTED_PASTE ? Rarity.EPIC : kind == Kind.GOLDEN_PASTE ? Rarity.RARE : Rarity.COMMON);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        if (kind == Kind.RELIEF && player instanceof ServerPlayer serverPlayer
                && !BetelNutEntityComponents.ADDICTION.get(serverPlayer).canTakeReliefPotion(serverPlayer)) {
            var state = BetelNutEntityComponents.ADDICTION.get(serverPlayer);
            BetelMessages.send(serverPlayer, state.hasTakenReliefToday(serverPlayer)
                    ? "message.betel-nut-mod.relief_already_used" : "message.betel-nut-mod.relief_not_needed");
            return InteractionResultHolder.fail(player.getItemInHand(hand));
        }
        return super.use(level, player, hand);
    }

    @Override
    public ItemStack finishUsingItem(ItemStack stack, Level level, LivingEntity entity) {
        // Recheck at completion: a second bottle started in the same day must not be consumed.
        if (kind == Kind.RELIEF && entity instanceof ServerPlayer player
                && !BetelNutEntityComponents.ADDICTION.get(player).canTakeReliefPotion(player)) return stack;
        ItemStack result = super.finishUsingItem(stack, level, entity);
        if (entity instanceof Player player && !player.getAbilities().instabuild) {
            if (result.isEmpty()) return new ItemStack(Items.GLASS_BOTTLE);
            if (!level.isClientSide()) {
                ItemStack bottle = new ItemStack(Items.GLASS_BOTTLE);
                if (!player.getInventory().add(bottle)) player.drop(bottle, false);
            }
        }
        return result;
    }

    @Override public UseAnim getUseAnimation(ItemStack stack) { return UseAnim.DRINK; }
    @Override public int getUseDuration(ItemStack stack, LivingEntity entity) { return 32; }
    @Override public SoundEvent getEatingSound() { return SoundEvents.GENERIC_DRINK; }
    @Override public SoundEvent getDrinkingSound() { return SoundEvents.GENERIC_DRINK; }
    @Override public boolean isFoil(ItemStack stack) { return kind == Kind.ENCHANTED_PASTE || super.isFoil(stack); }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        super.appendHoverText(stack, context, lines, flag);
        lines.add(Component.translatable("tooltip.betel-nut-mod.recovery_drink").withStyle(ChatFormatting.AQUA));
        lines.add(Component.translatable("tooltip.betel-nut-mod." + switch (kind) {
            case GOLDEN_PASTE -> "golden_apple_paste";
            case ENCHANTED_PASTE -> "enchanted_golden_apple_paste";
            case RELIEF -> "relief_potion";
        }).withStyle(ChatFormatting.GRAY));
    }
}
