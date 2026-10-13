package betel.nut.client;

import betel.nut.addiction.AddictionStageUtil;
import betel.nut.block.ModBlocks;
import betel.nut.client.addiction.AddictionHudRenderer;
import betel.nut.client.addiction.AddictionTooltipAppender;
import betel.nut.client.addiction.ClientAddictionData;
import betel.nut.client.gui.HudEditScreen;
import betel.nut.network.AddictionSyncPayload;
import com.mojang.blaze3d.platform.InputConstants;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.network.chat.Component;
import org.lwjgl.glfw.GLFW;

@net.neoforged.fml.common.Mod(value = betel.nut.BetelNutMod.MOD_ID, dist = net.neoforged.api.distmarker.Dist.CLIENT)
public class BetelNutModClient {
	private static KeyMapping editHudKey;


 public BetelNutModClient(net.neoforged.bus.api.IEventBus bus) {
  BetelNutClientConfig.load();
  bus.addListener(BetelPalmMeshModels::register);
  bus.addListener((net.neoforged.neoforge.client.event.RegisterKeyMappingsEvent e) -> {
   editHudKey = new KeyMapping("key.betel_nut_mod.edit_hud", InputConstants.Type.KEYSYM, GLFW.GLFW_KEY_H, "category.betel_nut_mod");
   e.register(editHudKey);
  });
  bus.addListener((net.neoforged.fml.event.lifecycle.FMLClientSetupEvent e) -> e.enqueueWork(() -> {
   net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ModBlocks.BETEL_PALM_LEAVES, RenderType.cutoutMipped());
   net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ModBlocks.BETEL_PALM_SAPLING, RenderType.cutout());
   net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ModBlocks.BETEL_PALM_TRAPDOOR, RenderType.cutout());
   net.minecraft.client.renderer.ItemBlockRenderTypes.setRenderLayer(ModBlocks.BETEL_PALM_DOOR, RenderType.cutout());
  }));
  var events = net.neoforged.neoforge.common.NeoForge.EVENT_BUS;
  events.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingIn e) -> ClientAddictionData.reset());
  events.addListener((net.neoforged.neoforge.client.event.ClientPlayerNetworkEvent.LoggingOut e) -> ClientAddictionData.reset());
  AddictionSyncPayload.clientReceiver = payload -> {
   if (ClientAddictionData.update(payload)) showStageWarning(Minecraft.getInstance(), payload.addictionStage());
  };
  events.addListener((net.neoforged.neoforge.client.event.RenderGuiEvent.Post e) -> AddictionHudRenderer.render(e.getGuiGraphics(),e.getPartialTick()));
  events.addListener((net.neoforged.neoforge.client.event.ClientTickEvent.Post e) -> {
   var client = Minecraft.getInstance();
   if(editHudKey != null) while(editHudKey.consumeClick()) if(!(client.screen instanceof HudEditScreen)) client.setScreen(new HudEditScreen());
  });
  AddictionTooltipAppender.register(); BetelPalmTooltipAppender.register();
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
