package betel.nut.client;

import betel.nut.addiction.AddictionStageUtil;
import betel.nut.block.ModBlocks;
import betel.nut.client.addiction.AddictionHudRenderer;
import betel.nut.client.addiction.AddictionTooltipAppender;
import betel.nut.client.addiction.ClientAddictionData;
import betel.nut.client.gui.HudEditScreen;
import betel.nut.network.AddictionSyncPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.blockrenderlayer.v1.BlockRenderLayerMap;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.fabricmc.fabric.api.client.rendering.v1.HudRenderCallback;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

public class BetelNutModClient implements ClientModInitializer {
	private static KeyMapping editHudKey;

	@Override
	public void onInitializeClient() {
		BetelPalmMeshModels.register();
		BetelNutClientConfig.load();
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BETEL_PALM_LEAVES, RenderType.cutoutMipped());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BETEL_PALM_SAPLING, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BETEL_PALM_TRAPDOOR, RenderType.cutout());
		BlockRenderLayerMap.INSTANCE.putBlock(ModBlocks.BETEL_PALM_DOOR, RenderType.cutout());
		ClientPlayConnectionEvents.INIT.register((handler, client) -> ClientAddictionData.reset());
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ClientAddictionData.reset());
		ClientPlayNetworking.registerGlobalReceiver(AddictionSyncPayload.TYPE, (payload, context) -> {
			boolean stageIncreased = ClientAddictionData.update(payload);
			if (stageIncreased) {
				showStageWarning(context.client(), payload.addictionStage());
			}
		});
		HudRenderCallback.EVENT.register(AddictionHudRenderer::render);
		registerHudEditKey();
		AddictionTooltipAppender.register();
		BetelPalmTooltipAppender.register();
	}

	private static void registerHudEditKey() {
		editHudKey = KeyBindingHelper.registerKeyBinding(new KeyMapping("key.betel_nut_mod.edit_hud",
				InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.betel_nut_mod"));
		ClientTickEvents.END_CLIENT_TICK.register(client -> {
			while (editHudKey.consumeClick()) {
				if (!(client.screen instanceof HudEditScreen)) {
					client.setScreen(new HudEditScreen());
				}
			}
		});
	}

	private static void showStageWarning(Minecraft client, int stage) {
		if (stage <= 0 || client.player == null) {
			return;
		}

		Component title = Component.translatable(AddictionStageUtil.getWarningTitleKey(stage));
		Component subtitle = Component.translatable(AddictionStageUtil.getWarningSubtitleKey(stage));
		if (stage >= 3) {
			client.gui.setTimes(10, 45, 15);
			client.gui.setSubtitle(subtitle);
			client.gui.setTitle(title);
			return;
		}

		client.gui.setOverlayMessage(Component.translatable(AddictionStageUtil.getWarningTitleKey(stage))
				.append(Component.literal(": "))
				.append(Component.translatable(AddictionStageUtil.getWarningSubtitleKey(stage))), false);
	}
}
