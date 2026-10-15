package betel.nut.client.addiction;

import java.util.List;

import betel.nut.addiction.AddictionStageUtil;
import betel.nut.item.BetelNutItem;
import betel.nut.item.ModItemTags;
import betel.nut.item.ModItems;
import betel.nut.client.BetelClientEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

public final class AddictionTooltipAppender {
	private static final int WARNING_SECONDS = 60;
	private static final int DANGER_SECONDS = 20;
	private static final int TICKS_PER_SECOND = 20;

	public static void register() {
		BetelClientEvents.tooltip(AddictionTooltipAppender::append);
	}

	private static void append(ItemStack stack, Item.TooltipContext context, TooltipFlag type,
			List<Component> lines) {
		if (stack.is(ModItems.BETEL_NUT_RESIDUE)) {
			lines.add(Component.translatable("tooltip.betel_nut_mod.residue.source")
					.withStyle(ChatFormatting.GRAY));
		}
		if (stack.getItem() instanceof BetelNutItem) {
			lines.add(Component.translatable("tooltip.betel_nut_mod.residue.on_eat")
					.withStyle(ChatFormatting.GRAY));
		}
		if (!stack.is(ModItemTags.BETEL_FOODS) && !(stack.getItem() instanceof BetelNutItem)) {
			return;
		}

		lines.add(Component.translatable("betel_nut_mod.tooltip.addiction.risk")
				.withStyle(ChatFormatting.GRAY));

		if (!ClientAddictionData.hasSyncedData()) {
			return;
		}

		lines.add(Component.translatable(ClientAddictionData.hasEatenToday()
				? "tooltip.betel_nut_mod.counted_today" : "tooltip.betel_nut_mod.counts_today").withStyle(ChatFormatting.AQUA));
		int stage = ClientAddictionData.getAddictionStage();
		lines.add(Component.translatable("betel_nut_mod.tooltip.addiction.current",
				Component.translatable(AddictionStageUtil.getStageTranslationKey(stage)))
				.withStyle(AddictionStageUtil.getStageFormatting(stage)));

		if (stack.getItem() instanceof BetelNutItem betelNutItem
				&& ClientAddictionData.wouldReachNextStage(betelNutItem.getAddictionIncrease())) {
			lines.add(Component.translatable("betel_nut_mod.tooltip.addiction.increase")
					.withStyle(ChatFormatting.YELLOW));
		}

		if (stage >= 3) {
			lines.add(Component.translatable("betel_nut_mod.tooltip.withdrawal.severe")
					.withStyle(ChatFormatting.RED));
		}

		int remainingTicks = ClientAddictionData.getRemainingWithdrawalTicks();
		if (remainingTicks >= 0) {
			ChatFormatting color = countdownColor(stage, remainingTicks);
			lines.add(Component.translatable("betel_nut_mod.tooltip.withdrawal.remaining",
					AddictionStageUtil.formatTicksAsTime(remainingTicks)).withStyle(color));
		}
	}

	private static ChatFormatting countdownColor(int stage, int remainingTicks) {
		if (stage >= 3 && remainingTicks <= DANGER_SECONDS * TICKS_PER_SECOND) {
			return ChatFormatting.RED;
		}
		if (stage >= 2 && remainingTicks <= WARNING_SECONDS * TICKS_PER_SECOND) {
			return ChatFormatting.GOLD;
		}
		return ChatFormatting.GRAY;
	}

	private AddictionTooltipAppender() {
	}
}
