package betel.nut.item;

import betel.nut.BetelNutMod;
import betel.nut.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public final class ModItemGroups {
	public static final ResourceKey<CreativeModeTab> BETEL_NUT_GROUP_KEY = ResourceKey.create(
			Registries.CREATIVE_MODE_TAB,
			BetelNutMod.id("betel_nut_mod"));

	public static final CreativeModeTab BETEL_NUT_GROUP = Registry.register(
			BuiltInRegistries.CREATIVE_MODE_TAB,
			BETEL_NUT_GROUP_KEY,
			FabricItemGroup.builder()
					.title(Component.translatable("itemGroup.betel-nut-mod.betel_nut_mod"))
					.icon(() -> new ItemStack(ModItems.ROASTED_BETEL_NUT))
					.displayItems((parameters, output) -> {
						output.accept(ModItems.RAW_BETEL_NUT);
						output.accept(ModItems.ROASTED_BETEL_NUT);
						output.accept(ModItems.BETEL_LEAF);
						output.accept(ModItems.BETEL_NUT_RESIDUE);
						output.accept(ModBlocks.BETEL_NUT_RESIDUE_BLOCK_ITEM);
						output.accept(ModItems.SPICY_BETEL_NUT);
						output.accept(ModItems.SWEET_BETEL_NUT);
						output.accept(ModItems.REFRESHING_BETEL_NUT);
						output.accept(ModItems.NIGHT_BETEL_NUT);
						output.accept(ModItems.ENERGIZING_BETEL_NUT);
						output.accept(ModItems.HONEY_BETEL_NUT);
						output.accept(ModItems.GLOW_BETEL_NUT);
						output.accept(ModItems.PHANTOM_BETEL_NUT);
						output.accept(ModItems.ENDER_BETEL_NUT);
						output.accept(ModItems.LAPIS_BETEL_NUT);
						output.accept(ModItems.QUARTZ_BETEL_NUT);
						output.accept(ModItems.MAGMA_BETEL_NUT);
						output.accept(ModItems.AMETHYST_BETEL_NUT);
						output.accept(ModItems.SYNTHETIC_WORLD_BETEL);
						output.accept(ModItems.RICH_WORLD_BETEL);
						output.accept(ModItems.UNDERGROUND_BETEL);
						output.accept(ModBlocks.BETEL_PALM_LOG_ITEM);
						output.accept(ModBlocks.BETEL_PALM_PLANKS_ITEM);
						output.accept(ModBlocks.BETEL_PALM_STAIRS_ITEM);
						output.accept(ModBlocks.BETEL_PALM_SLAB_ITEM);
						output.accept(ModBlocks.BETEL_PALM_FENCE_ITEM);
						output.accept(ModBlocks.BETEL_PALM_FENCE_GATE_ITEM);
						output.accept(ModBlocks.BETEL_PALM_BUTTON_ITEM);
						output.accept(ModBlocks.BETEL_PALM_PRESSURE_PLATE_ITEM);
						output.accept(ModBlocks.BETEL_PALM_TRAPDOOR_ITEM);
						output.accept(ModBlocks.BETEL_PALM_LEAVES_ITEM);
						output.accept(ModBlocks.BETEL_PALM_SAPLING_ITEM);
					})
					.build());

	public static void register() {
		BetelNutMod.LOGGER.info("Registering betel nut item group");
	}

	private ModItemGroups() {
	}
}
