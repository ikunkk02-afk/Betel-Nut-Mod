package betel.nut.client.gui;

import betel.nut.client.BetelNutClientConfig;
import betel.nut.client.addiction.AddictionHudRenderer;
import betel.nut.client.addiction.AddictionHudRenderer.HudPosition;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

public final class HudEditScreen extends Screen {
	private int hudX;
	private int hudY;
	private boolean dragging;
	private boolean positionChanged;
	private int dragOffsetX;
	private int dragOffsetY;

	public HudEditScreen() {
		super(Component.translatable("screen.betel_nut_mod.hud_edit.title"));
	}

	@Override
	protected void init() {
		HudPosition position = AddictionHudRenderer.getConfiguredPosition(this.minecraft, false);
		this.hudX = position.x();
		this.hudY = position.y();
		clampHudPosition();
	}

	@Override
	public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
		graphics.fill(0, 0, this.width, this.height, 0xA0000000);
		int hintY = 16;
		for (var line : this.font.split(Component.translatable("screen.betel_nut_mod.hud_edit.hint"), Math.max(1, this.width - 20))) {
			graphics.drawString(this.font, line, (this.width - this.font.width(line)) / 2, hintY, 0xFFFFFFFF);
			hintY += this.font.lineHeight + 2;
		}

		clampHudPosition();
		AddictionHudRenderer.renderHud(graphics, this.hudX, this.hudY, partialTick);

		int outlineX = Math.max(0, this.hudX - 2);
		int outlineY = Math.max(0, this.hudY - 2);
		int outlineWidth = Math.min(this.width - outlineX, AddictionHudRenderer.getScaledHudWidth() + 4);
		int outlineHeight = Math.min(this.height - outlineY, AddictionHudRenderer.getScaledHudHeight() + 4);
		graphics.renderOutline(outlineX, outlineY, outlineWidth, outlineHeight, dragging ? 0xFFFFFFFF : 0xCC55D6FF);
		if (!dragging && isInsideHud(mouseX, mouseY)) {
			var lines = AddictionHudRenderer.getDetails().stream()
					.flatMap(line -> this.font.split(line, Math.max(1, this.width - 24)).stream()).toList();
			graphics.renderTooltip(this.font, lines, (screenW, screenH, cursorX, cursorY, tooltipW, tooltipH) ->
					new org.joml.Vector2i(Math.max(8, Math.min(cursorX + 12, screenW - tooltipW - 8)),
							Math.max(8, Math.min(cursorY - tooltipH - 12, screenH - tooltipH - 8))), mouseX, mouseY);
		}
	}

	@Override
	public boolean mouseClicked(double mouseX, double mouseY, int button) {
		if (button == 0 && isInsideHud(mouseX, mouseY)) {
			this.dragging = true;
			this.dragOffsetX = (int) Math.round(mouseX) - this.hudX;
			this.dragOffsetY = (int) Math.round(mouseY) - this.hudY;
			return true;
		}
		return super.mouseClicked(mouseX, mouseY, button);
	}

	@Override
	public boolean mouseDragged(double mouseX, double mouseY, int button, double deltaX, double deltaY) {
		if (this.dragging && button == 0) {
			this.positionChanged = true;
			this.hudX = (int) Math.round(mouseX) - this.dragOffsetX;
			this.hudY = (int) Math.round(mouseY) - this.dragOffsetY;
			clampHudPosition();
			return true;
		}
		return super.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
	}

	@Override
	public boolean mouseReleased(double mouseX, double mouseY, int button) {
		if (button == 0 && this.dragging) {
			this.dragging = false;
			savePosition();
			return true;
		}
		return super.mouseReleased(mouseX, mouseY, button);
	}

	@Override
	public void onClose() {
		savePosition();
		super.onClose();
	}

	@Override
	public boolean isPauseScreen() {
		return false;
	}

	private boolean isInsideHud(double mouseX, double mouseY) {
		return mouseX >= this.hudX
				&& mouseY >= this.hudY
				&& mouseX <= this.hudX + AddictionHudRenderer.getScaledHudWidth()
				&& mouseY <= this.hudY + AddictionHudRenderer.getScaledHudHeight();
	}

	private void clampHudPosition() {
		HudPosition position = AddictionHudRenderer.clampPosition(this.hudX, this.hudY, this.width, this.height);
		this.hudX = position.x();
		this.hudY = position.y();
	}

	private void savePosition() {
		if (!this.positionChanged) return;
		BetelNutClientConfig config = BetelNutClientConfig.get();
		HudPosition position = AddictionHudRenderer.clampPosition(this.hudX, this.hudY, this.width, this.height);
		config.setHudPosition(position.x(), position.y());
		BetelNutClientConfig.save();
		this.hudX = position.x();
		this.hudY = position.y();
	}
}
