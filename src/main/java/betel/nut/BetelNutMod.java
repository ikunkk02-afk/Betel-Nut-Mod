package betel.nut;

import betel.nut.event.BetelNutEvents;
import betel.nut.event.WithdrawalEatingRestrictions;
import betel.nut.block.ModBlocks;
import betel.nut.command.BetelCommands;
import betel.nut.item.ModItemGroups;
import betel.nut.item.ModItems;
import betel.nut.network.AddictionSyncPayload;
import betel.nut.villager.ModVillagerTrades;
import betel.nut.worldgen.ModWorldGeneration;
import net.fabricmc.api.ModInitializer;
import net.minecraft.resources.ResourceLocation;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BetelNutMod implements ModInitializer {
	public static final String MOD_ID = "betel-nut-mod";

	// This logger is used to write text to the console and the log file.
	// It is considered best practice to use your mod id as the logger's name.
	// That way, it's clear which mod wrote info, warnings, and errors.
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	public static ResourceLocation id(String path) {
		return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		BetelNutConfig.load();
		AddictionSyncPayload.register();
		ModBlocks.register();
		ModItems.register();
		ModComposting.registerCompostables();
		ModItemGroups.register();
		WithdrawalEatingRestrictions.register();
		BetelNutEvents.register();
		ModWorldGeneration.register();
		ModVillagerTrades.registerTrades();
		BetelCommands.register();

		LOGGER.info("Betel Nut Mod initialized");
	}
}
