package betel.nut.item;

import betel.nut.BetelNutMod;
import betel.nut.block.ModBlocks;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.CreativeModeTabs;
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
						output.accept(ModItems.GOLDEN_APPLE_PASTE);
                        output.accept(ModItems.ENCHANTED_GOLDEN_APPLE_PASTE);
                        output.accept(ModItems.RELIEF_POTION);
                        output.accept(ModItems.SHELL);
						output.accept(ModItems.SHELL_POWDER);
						output.accept(ModItems.ANCIENT_SHELL);
						output.accept(ModItems.GOLDEN_SHELL);
						output.accept(ModItems.WRAPPED_RAW_BETEL);
						output.accept(ModItems.ECHO_BETEL);
						output.accept(ModItems.EMPTY_PACK);
						output.accept(ModItems.BRAND_SEAL);
						output.accept(ModItems.KOUWEIWANG_PACK);
						output.accept(ModItems.HECHENG_TIANXIA_PACK);
						output.accept(ModItems.XIANGTAN_PUZI_PACK);
						output.accept(ModItems.LOTTERY_TICKET);
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
						output.accept(ModBlocks.BETEL_PALM_DOOR_ITEM);
						output.accept(ModBlocks.BETEL_PALM_LEAVES_ITEM);
						output.accept(ModBlocks.BETEL_PALM_SAPLING_ITEM);
						output.accept(ModBlocks.BETEL_NUT_CLUSTER_ITEM);
						output.accept(ModBlocks.UNRIPE_BETEL_NUT_CLUSTER_ITEM);
					})
					.build());

	public static void register() {
		BetelNutMod.LOGGER.info("Registering betel nut item group");
        ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.FOOD_AND_DRINKS).register(entries -> {
            entries.accept(ModItems.GOLDEN_APPLE_PASTE);
            entries.accept(ModItems.ENCHANTED_GOLDEN_APPLE_PASTE);
            entries.accept(ModItems.RELIEF_POTION);
        });
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.BUILDING_BLOCKS).register(entries -> {
			entries.accept(ModBlocks.BETEL_PALM_LOG_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_PLANKS_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_STAIRS_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_SLAB_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_FENCE_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_FENCE_GATE_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_DOOR_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_TRAPDOOR_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_PRESSURE_PLATE_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_BUTTON_ITEM);
		});
		ItemGroupEvents.modifyEntriesEvent(CreativeModeTabs.REDSTONE_BLOCKS).register(entries -> {
			entries.accept(ModBlocks.BETEL_PALM_BUTTON_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_PRESSURE_PLATE_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_FENCE_GATE_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_DOOR_ITEM);
			entries.accept(ModBlocks.BETEL_PALM_TRAPDOOR_ITEM);
		});
	}

	private ModItemGroups() {
	}
}
