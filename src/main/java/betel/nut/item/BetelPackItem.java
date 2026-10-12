package betel.nut.item;

import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.level.Level;

public final class BetelPackItem extends Item {
    private final int assortment;
    public BetelPackItem(int assortment) { super(new Properties().stacksTo(16)); this.assortment = assortment; }
    public List<Item> contents() {
        return switch (assortment) {
            case 1 -> List.of(ModItems.ROASTED_BETEL_NUT, ModItems.HONEY_BETEL_NUT, ModItems.QUARTZ_BETEL_NUT,
                    ModItems.AMETHYST_BETEL_NUT, ModItems.LAPIS_BETEL_NUT, ModItems.PHANTOM_BETEL_NUT);
            case 2 -> List.of(ModItems.ROASTED_BETEL_NUT, ModItems.HONEY_BETEL_NUT, ModItems.SWEET_BETEL_NUT,
                    ModItems.REFRESHING_BETEL_NUT, ModItems.NIGHT_BETEL_NUT, ModItems.SPICY_BETEL_NUT);
            default -> List.of(ModItems.ROASTED_BETEL_NUT, ModItems.SPICY_BETEL_NUT, ModItems.SWEET_BETEL_NUT,
                    ModItems.REFRESHING_BETEL_NUT, ModItems.NIGHT_BETEL_NUT, ModItems.ENERGIZING_BETEL_NUT);
        };
    }
    public static void give(ServerPlayer player, ItemStack stack) {
        if (!player.getInventory().add(stack) && !stack.isEmpty()) player.drop(stack, false);
    }
    @Override public InteractionResultHolder<ItemStack> use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (stack.isEmpty() || !stack.is(this) || player.getCooldowns().isOnCooldown(this)) return InteractionResultHolder.fail(stack);
        if (player instanceof ServerPlayer serverPlayer) {
            // Consume before issuing contents, including in creative, so one click opens exactly one bag.
            stack.shrink(1);
            for (Item item : contents()) give(serverPlayer, new ItemStack(item));
            give(serverPlayer, LotteryTicketItem.create(level.getRandom()));
            player.displayClientMessage(Component.translatable("message.betel-nut-mod.pack_opened"), true);
            player.getCooldowns().addCooldown(this, 10);
        }
        // Inventory insertion may fill the consumed bag's now-empty hand slot.
        return InteractionResultHolder.sidedSuccess(player.getItemInHand(hand), level.isClientSide());
    }
    @Override public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> lines, TooltipFlag flag) {
        lines.add(Component.translatable("tooltip.betel-nut-mod.pack").withStyle(ChatFormatting.GOLD));
        for (Item item : contents()) lines.add(Component.translatable(item.getDescriptionId()).withStyle(ChatFormatting.GRAY));
        lines.add(Component.translatable("tooltip.betel-nut-mod.pack_seal").withStyle(ChatFormatting.DARK_GRAY));
    }
}
