package betel.nut.component;
import net.minecraft.world.level.Level;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.saveddata.SavedData;
public final class BetelNutWorldComponents {
 public static final WorldData SKYBLOCK_WORLD = new WorldData();
 public static final class WorldData {
  public BetelSkyblockWorldComponent get(Level level) {
   if (!(level instanceof ServerLevel server)) throw new IllegalArgumentException("Skyblock data is server-authoritative");
   ServerLevel overworld = server.getServer().overworld();
   return overworld.getDataStorage().computeIfAbsent(new SavedData.Factory<>(
    () -> new BetelSkyblockWorldComponent(overworld), (tag, lookup) -> {
     var data = new BetelSkyblockWorldComponent(overworld); data.readFromNbt(tag, lookup); return data;
    }), "betel_nut_mod_skyblock");
  }
 }
 private BetelNutWorldComponents() {}
}
