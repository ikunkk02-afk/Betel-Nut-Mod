package betel.nut.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;

public final class LotteryTicketItem extends Item {
    public LotteryTicketItem() { super(new Properties().stacksTo(16)); }
    public static int prizeForRoll(int roll) {
        if (roll < 0 || roll >= 100) throw new IllegalArgumentException("Lottery roll out of bounds");
        return roll < 60 ? 0 : roll < 85 ? 1 : roll < 95 ? 2 : roll < 99 ? 3 : 4;
    }
    public static ItemStack create(RandomSource random) {
        ItemStack stack = new ItemStack(ModItems.LOTTERY_TICKET);
        CompoundTag data = new CompoundTag();
        data.putInt("betelPrize", prizeForRoll(random.nextInt(100)));
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(data));
        return stack;
    }
    public static ItemStack reward(int prize) {
        return switch (prize) {
            case 1 -> new ItemStack(ModItems.BETEL_LEAF, 3);
            case 2 -> new ItemStack(Items.EMERALD);
            case 3 -> new ItemStack(ModItems.HONEY_BETEL_NUT);
            case 4 -> new ItemStack(ModItems.GOLDEN_SHELL);
            default -> ItemStack.EMPTY;
        };
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !stack.is(this) || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            CompoundTag data = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
            int prize = data.contains("betelPrize") ? Math.max(0, Math.min(4, data.getInt("betelPrize")))
                    : prizeForRoll(level.getRandom().nextInt(100));
            stack.shrink(1);
            ItemStack reward = reward(prize);
            if (!reward.isEmpty()) BetelPackItem.give(serverPlayer, reward);
            player.displayClientMessage(Component.translatable("message.betel_nut_mod.lottery." + prize), false);
            player.getCooldowns().addCooldown(this, 10);
        }
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.betel_nut_mod.ticket").withStyle(ChatFormatting.YELLOW));
        lines.add(Component.translatable("tooltip.betel_nut_mod.ticket_odds").withStyle(ChatFormatting.GRAY));
    }
}
