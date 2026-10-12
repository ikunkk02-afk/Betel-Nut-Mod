package betel.nut.client;

import betel.nut.block.ModBlocks;
import net.fabricmc.fabric.api.client.item.v1.ItemTooltipCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;

public final class BetelPalmTooltipAppender {
	public static void register() {
		ItemTooltipCallback.EVENT.register((stack, context, flags, lines) -> {
			if (stack.is(ModBlocks.BETEL_PALM_SAPLING_ITEM)) {
				lines.add(Component.translatable("tooltip.betel-nut-mod.sapling.climate").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.betel-nut-mod.sapling.cultivation").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.betel-nut-mod.sapling.seed").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.betel-nut-mod.cluster.harvest").withStyle(ChatFormatting.GRAY));
			}
			if (stack.is(ModBlocks.BETEL_NUT_CLUSTER_ITEM) || stack.is(ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM)) {
				lines.add(Component.translatable("tooltip.betel-nut-mod.cluster.harvest").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable("tooltip.betel-nut-mod.cluster.regrow").withStyle(ChatFormatting.GRAY));
				lines.add(Component.translatable(stack.is(ModBlocks.BETEL_NUT_CLUSTER_ITEM)
						? "tooltip.betel-nut-mod.cluster.ripe" : "tooltip.betel-nut-mod.cluster.unripe").withStyle(ChatFormatting.GRAY));
			}
		});
	}

	private BetelPalmTooltipAppender() {}
}
