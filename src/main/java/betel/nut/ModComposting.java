package betel.nut;

import betel.nut.block.ModBlocks;
import betel.nut.item.ModItems;
import net.fabricmc.fabric.api.registry.CompostingChanceRegistry;

public final class ModComposting {
	public static void registerCompostables() {
		// Residue resources are reserved for the later Betel sky island loop:
		// eating betel nuts creates residue, then composting and compression form early resource cycling.
		CompostingChanceRegistry.INSTANCE.add(ModItems.BETEL_NUT_RESIDUE,
				(float) BetelNutMidnightConfig.residueCompostChance);
		CompostingChanceRegistry.INSTANCE.add(ModBlocks.BETEL_NUT_RESIDUE_BLOCK.asItem(),
				(float) BetelNutMidnightConfig.residueBlockCompostChance);
	}

	private ModComposting() {
	}
}
