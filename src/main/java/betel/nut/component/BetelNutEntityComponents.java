package betel.nut.component;
import betel.nut.BetelNutMod;
import java.util.function.Supplier;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.attachment.AttachmentType;
import net.neoforged.neoforge.registries.DeferredRegister;
import net.neoforged.neoforge.registries.NeoForgeRegistries;
public final class BetelNutEntityComponents {
 private static final DeferredRegister<AttachmentType<?>> TYPES = DeferredRegister.create(NeoForgeRegistries.ATTACHMENT_TYPES, BetelNutMod.MOD_ID);
 private static final Supplier<AttachmentType<BetelNutAddictionComponent>> ADDICTION_TYPE = TYPES.register("addiction",
  () -> AttachmentType.serializable(holder -> new BetelNutAddictionComponent((Player) holder)).build());
 private static final Supplier<AttachmentType<BetelSkyblockPlayerComponent>> SKYBLOCK_TYPE = TYPES.register("skyblock_player",
  () -> AttachmentType.serializable(holder -> new BetelSkyblockPlayerComponent((Player) holder)).build());
 public record PlayerData<T>(Supplier<AttachmentType<T>> type) { public T get(Player player) { return player.getData(type); } }
 public static final PlayerData<BetelNutAddictionComponent> ADDICTION = new PlayerData<>(ADDICTION_TYPE);
 public static final PlayerData<BetelSkyblockPlayerComponent> SKYBLOCK_PLAYER = new PlayerData<>(SKYBLOCK_TYPE);
 public static void register(IEventBus bus) { TYPES.register(bus); }
 private BetelNutEntityComponents() {}
}
