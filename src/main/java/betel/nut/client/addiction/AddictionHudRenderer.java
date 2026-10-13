package betel.nut.client.addiction;

import betel.nut.addiction.AddictionStageUtil;
import betel.nut.client.BetelNutClientConfig;
import betel.nut.item.ModItems;
import betel.nut.effect.ModEffects;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;

/** A small gameplay strip; detailed health information belongs in the editor tooltip. */
public final class AddictionHudRenderer {
    private static final int BASE_HEIGHT = 26;
    private static final ItemStack ICON = new ItemStack(ModItems.WRAPPED_RAW_BETEL);
    public record HudPosition(int x, int y) { }
    private record Alert(Component text, int color) { }
    private record Display(int days, int max, int stage, int sober, boolean used, int cancer, int mouth, int withdrawal) { }

    private static Display data() {
        return new Display(ClientAddictionData.getAddictionValue(), ClientAddictionData.getMaxAddictionValue(),
                ClientAddictionData.getAddictionStage(), ClientAddictionData.getSoberDays(), ClientAddictionData.hasEatenToday(),
                ClientAddictionData.getOralCancerStage(), ClientAddictionData.getMouthRestrictionLevel(), ClientAddictionData.getWithdrawalSeverity());
    }
    private static Component stage(Display d) { return Component.translatable("hud.betel_nut_mod.compact.stage." + d.stage); }
    private static Component days(Display d) { return Component.translatable("hud.betel_nut_mod.compact.days", d.days, d.max); }
    private static List<Alert> alerts(Display d) {
        List<Alert> result = new ArrayList<>();
        int symptoms = symptomStage();
        if (symptoms > 0) result.add(new Alert(Component.translatable("hud.betel_nut_mod.compact.symptoms", roman(symptoms)), 0xFFE8B47B));
        if (d.cancer > 0) result.add(new Alert(Component.translatable("hud.betel_nut_mod.compact.oral", roman(d.cancer)), 0xFFFF929C));
        if (d.mouth > 0) result.add(new Alert(Component.translatable("hud.betel_nut_mod.compact.mouth." + d.mouth), 0xFFFFD293));
        if (d.withdrawal > 0) result.add(new Alert(Component.translatable("hud.betel_nut_mod.compact.withdrawal", roman(d.withdrawal)), 0xFFFFB978));
        return result;
    }
    private static int symptomStage() {
        var player = Minecraft.getInstance().player;
        var effect = player == null ? null : player.getEffect(ModEffects.BETEL_DEPENDENCE);
        return effect == null ? 0 : Math.min(5, effect.getAmplifier() + 1);
    }
    public static List<Component> getDetails() {
        Display d = data();
        List<Component> lines = new ArrayList<>();
        lines.add(Component.translatable("hud.betel_nut_mod.title"));
        lines.add(Component.translatable("hud.betel_nut_mod.days", d.days, d.max));
        lines.add(Component.translatable(AddictionStageUtil.getStageTranslationKey(d.stage)));
        int symptoms = symptomStage();
        if (symptoms > 0) lines.add(Component.translatable("hud.betel_nut_mod.symptoms." + symptoms));
        lines.add(Component.translatable("hud.betel_nut_mod.today." + (d.used ? "used" : "unused")));
        lines.add(Component.translatable("hud.betel_nut_mod.sober", d.sober));
        lines.add(d.stage >= 5 ? Component.translatable("hud.betel_nut_mod.maximum")
                : Component.translatable("hud.betel_nut_mod.next", Math.max(0, AddictionStageUtil.getNextStageThreshold(d.days, d.max) - d.days)));
        if (d.withdrawal > 0) lines.add(Component.translatable("hud.betel_nut_mod.withdrawal", d.withdrawal));
        lines.add(Component.translatable("hud.betel_nut_mod.oral." + d.cancer));
        lines.add(Component.translatable("hud.betel_nut_mod.mouth." + d.mouth));
        lines.add(Component.translatable("hud.betel_nut_mod.compact.legend"));
        return lines;
    }
    public static int getHudWidth() {
        var font = Minecraft.getInstance().font; Display d = data();
        int width = Math.max(124, 23 + font.width(stage(d)) + font.width(days(d)) + 16);
        List<Alert> alerts = alerts(d);
        int alertWidth = alerts.stream().mapToInt(a -> font.width(a.text)).sum() + Math.max(0, alerts.size() - 1) * 8 + 10;
        return Math.max(width, alertWidth);
    }
    public static int getHudHeight() { return BASE_HEIGHT + (alerts(data()).isEmpty() ? 0 : 14); }
    private static float effectiveScale(Minecraft client) {
        return (float) Math.min(BetelNutClientConfig.get().hudScale, Math.max(0.1,
                Math.min((client.getWindow().getGuiScaledWidth() - 8.0) / getHudWidth(),
                        (client.getWindow().getGuiScaledHeight() - 8.0) / getHudHeight())));
    }
    public static int getScaledHudWidth() { return scaled(getHudWidth(), effectiveScale(Minecraft.getInstance())); }
    public static int getScaledHudHeight() { return scaled(getHudHeight(), effectiveScale(Minecraft.getInstance())); }
    public static void render(GuiGraphics graphics, DeltaTracker ticks) {
        Minecraft client = Minecraft.getInstance();
        if (client.player == null || client.level == null || client.options.hideGui || client.screen != null
                || client.getDebugOverlay().showDebugScreen() || !BetelNutClientConfig.get().hudEnabled
                || !ClientAddictionData.shouldRenderHud()) return;
        HudPosition p = getConfiguredPosition(client, false);
        renderHud(graphics, p.x, p.y, 0);
    }
    public static HudPosition getConfiguredPosition(Minecraft client, boolean saveClamp) {
        BetelNutClientConfig config = BetelNutClientConfig.get();
        int screenWidth = client.getWindow().getGuiScaledWidth(), screenHeight = client.getWindow().getGuiScaledHeight();
        int width = getScaledHudWidth(), height = getScaledHudHeight();
        int x, y;
        if (config.hudUseCustomPosition) {
            x = config.hudX; y = config.hudY;
        } else {
            x = switch (config.hudAnchor) {
                case TOP_RIGHT, BOTTOM_RIGHT -> screenWidth - width - config.hudOffsetX;
                case CENTER_TOP, CENTER_BOTTOM -> screenWidth / 2 - width / 2 + config.hudOffsetX;
                case TOP_LEFT, BOTTOM_LEFT -> config.hudOffsetX;
            };
            y = switch (config.hudAnchor) {
                case BOTTOM_LEFT, BOTTOM_RIGHT, CENTER_BOTTOM -> screenHeight - height - config.hudOffsetY;
                case TOP_LEFT, TOP_RIGHT, CENTER_TOP -> config.hudOffsetY;
            };
            // Keep anchored strips clear of the hotbar, hearts, armor and air at large GUI scales.
            if ((config.hudAnchor == BetelNutClientConfig.HudAnchor.BOTTOM_LEFT
                    || config.hudAnchor == BetelNutClientConfig.HudAnchor.BOTTOM_RIGHT
                    || config.hudAnchor == BetelNutClientConfig.HudAnchor.CENTER_BOTTOM)
                    && x + width > screenWidth / 2 - 100 && x < screenWidth / 2 + 100) {
                y = Math.min(y, screenHeight - 72 - height);
            }
        }
        HudPosition clamped = clampPosition(x, y, screenWidth, screenHeight, width, height);
        if (saveClamp && config.hudUseCustomPosition && (clamped.x != config.hudX || clamped.y != config.hudY)) {
            config.setHudPosition(clamped.x, clamped.y); BetelNutClientConfig.save();
        }
        return clamped;
    }
    public static HudPosition clampPosition(int x, int y, int screenWidth, int screenHeight) {
        return clampPosition(x, y, screenWidth, screenHeight, getScaledHudWidth(), getScaledHudHeight());
    }
    public static void renderHud(GuiGraphics g, int x, int y, float tickDelta) {
        Minecraft client = Minecraft.getInstance(); var font = client.font; Display d = data();
        int width = getHudWidth();
        int color = d.stage == 5 ? 0xFFFF748B : AddictionStageUtil.getStageColor(d.stage);
        g.pose().pushPose(); g.pose().translate(x, y, 0); float scale = effectiveScale(client); g.pose().scale(scale, scale, 1);
        g.fill(0, 0, width, BASE_HEIGHT, 0x88101417);
        g.renderItem(ICON, 3, 4);
        g.fill(9, 21, 12, 24, d.used ? 0xFF79DEB0 : 0xFF596466);
        g.drawString(font, stage(d), 23, 5, color, true);
        g.drawString(font, days(d), width - 5 - font.width(days(d)), 5, 0xFFE1E6E4, true);
        int segments = Math.max(1, Math.min(8, d.max)), span = width - 28;
        for (int i = 0; i < segments; i++) {
            int left = 23 + i * span / segments, right = 23 + (i + 1) * span / segments - 2;
            boolean filled = (long) i * d.max < (long) d.days * segments;
            g.fill(left, 18, right, 21, filled ? color : 0xFF445052);
        }
        List<Alert> alerts = alerts(d);
        if (!alerts.isEmpty()) {
            g.fill(0, BASE_HEIGHT + 1, width, getHudHeight(), 0x88101417);
            int left = 5;
            for (int i = 0; i < alerts.size(); i++) {
                Alert a = alerts.get(i);
                if (i > 0) g.fill(left - 5, 32, left - 3, 34, 0xFF7F8886);
                g.drawString(font, a.text, left, 29, a.color, true);
                left += font.width(a.text) + 8;
            }
        }
        g.pose().popPose();
    }
    private static String roman(int level) { return switch (level) { case 1 -> "I"; case 2 -> "II"; case 3 -> "III"; case 4 -> "IV"; default -> "V"; }; }
    private static HudPosition clampPosition(int x, int y, int screenWidth, int screenHeight, int width, int height) {
        return new HudPosition(Math.max(0, Math.min(Math.max(0, screenWidth - width), x)),
                Math.max(0, Math.min(Math.max(0, screenHeight - height), y)));
    }
    private static int scaled(int value, float scale) { return Math.max(1, (int) Math.ceil(value * scale)); }
    private AddictionHudRenderer() { }
}
