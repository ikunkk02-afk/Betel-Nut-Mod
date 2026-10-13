package betel.nut;

import betel.nut.block.ModBlocks;
import betel.nut.item.ModItems;
import net.minecraft.world.level.block.ComposterBlock;

public final class ModComposting {
	public static void registerCompostables() {
		// Residue resources are reserved for the later Betel sky island loop:
		// eating betel nuts creates residue, then composting and compression form early resource cycling.
		ComposterBlock.COMPOSTABLES.put(ModItems.BETEL_NUT_RESIDUE,
				(float) BetelNutMidnightConfig.residueCompostChance);
		ComposterBlock.COMPOSTABLES.put(ModBlocks.BETEL_NUT_RESIDUE_BLOCK.asItem(),
				(float) BetelNutMidnightConfig.residueBlockCompostChance);
	}

	private ModComposting() {
	}
}
