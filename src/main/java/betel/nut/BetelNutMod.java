package betel.nut;
import betel.nut.platform.BetelPlatform;
import betel.nut.platform.BetelRegistries;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
@Mod(BetelNutMod.MOD_ID)
public final class BetelNutMod {
 public static final String MOD_ID = "betel_nut_mod";
 public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
 public static ResourceLocation id(String path) { return ResourceLocation.fromNamespaceAndPath(MOD_ID, path); }
 private boolean registriesPrepared;
 public BetelNutMod(IEventBus bus) {
  BetelNutConfig.load(); BetelNutMidnightConfig.load();
  bus.addListener((net.neoforged.neoforge.registries.RegisterEvent event) -> {
   if (!registriesPrepared) {
    registriesPrepared = true;
    betel.nut.effect.ModEffects.register(); betel.nut.block.ModBlocks.register();
    betel.nut.item.ModItems.register(); betel.nut.item.ModItemGroups.register();
    betel.nut.worldgen.ModConfiguredFeatures.register();
   }
   BetelRegistries.register(event);
  });
  bus.addListener(betel.nut.network.AddictionSyncPayload::register);
  bus.addListener(BetelPlatform::creativeContents);
  betel.nut.component.BetelNutEntityComponents.register(bus);
  bus.addListener((FMLCommonSetupEvent event) -> event.enqueueWork(() -> {
   ModComposting.registerCompostables(); BetelPlatform.setupFlammability();

  betel.nut.event.WithdrawalEatingRestrictions.register(); betel.nut.event.BetelNutEvents.register();
  betel.nut.advancement.BetelQuestAdvancements.register(); betel.nut.skyblock.BetelSkyblockManager.register();
  betel.nut.villager.ModVillagerTrades.register(); betel.nut.loot.ModLoot.register();
  betel.nut.command.BetelCommands.register(); BetelPlatform.initialize();
  LOGGER.info("Betel Nut Mod initialized for NeoForge");
  }));
 }
}
