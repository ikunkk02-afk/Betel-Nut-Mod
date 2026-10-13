package betel.nut.platform;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.core.registries.BuiltInRegistries;
import net.neoforged.neoforge.registries.DeferredHolder;
import net.neoforged.neoforge.registries.RegisterEvent;
/** Concrete gameplay references registered exclusively during NeoForge registry events. */
public final class BetelRegistries {
 private record Entry<T>(Registry<T> registry, ResourceLocation id, T value) {
  void register(RegisterEvent event) { event.register(registry.key(), id, () -> value); }
 }
 private static final List<Entry<?>> ENTRIES = new ArrayList<>();
 public static <T,V extends T> V add(Registry<T> registry, ResourceLocation id, V value) {
  ENTRIES.add(new Entry<>(registry, id, value)); return value;
 }
 public static <T,V extends T> V add(Registry<T> registry, ResourceKey<T> key, V value) { return add(registry, key.location(), value); }
 public static <T> Holder<T> holder(Registry<T> registry, ResourceLocation id, T value) {
  add(registry, id, value); return DeferredHolder.create(registry.key(), id);
 }
 public static Item item(ResourceKey<Item> key, Item value) {
  if (value instanceof BlockItem block) block.registerBlocks(Item.BY_BLOCK, value);
  return add(BuiltInRegistries.ITEM, key, value);
 }
 public static void register(RegisterEvent event) { ENTRIES.forEach(e -> e.register(event)); }
 private BetelRegistries() {}
}
