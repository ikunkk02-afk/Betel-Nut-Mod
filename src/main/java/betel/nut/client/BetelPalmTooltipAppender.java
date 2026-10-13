package betel.nut.client;

import betel.nut.block.ModBlocks;
import betel.nut.client.BetelClientEvents;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class BetelPalmTooltipAppender {
	public static void register() {
		BetelClientEvents.tooltip((stack, context, flags, lines) -> {
			if (stack.is(ModBlocks.BETEL_PALM_SAPLING_ITEM)) {
				lines.add(Component.translatable("tooltip.betel_nut_mod.sapling.climate").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.betel_nut_mod.sapling.cultivation").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.betel_nut_mod.sapling.seed").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.betel_nut_mod.cluster.harvest").withStyle(ChatFormatting.GRAY));
			}
			if (stack.is(ModBlocks.BETEL_NUT_CLUSTER_ITEM) || stack.is(ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM)) {
				lines.add(Component.translatable("tooltip.betel_nut_mod.cluster.harvest").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.betel_nut_mod.cluster.regrow").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable(stack.is(ModBlocks.BETEL_NUT_CLUSTER_ITEM)
						? "tooltip.betel_nut_mod.cluster.ripe" : "tooltip.betel_nut_mod.cluster.unripe").withStyle(ChatFormatting.GRAY));
			}
		});
	}

	private BetelPalmTooltipAppender() {}
}
