package betel.nut.client;
import java.util.List;
import net.minecraft.world.item.*;
import net.minecraft.network.chat.Component;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.entity.player.ItemTooltipEvent;
public final class BetelClientEvents {
 @FunctionalInterface public interface Tooltip { void append(ItemStack stack, Item.TooltipContext context, TooltipFlag flag, List<Component> lines); }
 public static void tooltip(Tooltip c) { NeoForge.EVENT_BUS.addListener((ItemTooltipEvent e) -> c.append(e.getItemStack(),e.getContext(),e.getFlags(),e.getToolTip())); }
 private BetelClientEvents() {}
}
