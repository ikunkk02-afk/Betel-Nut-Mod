package betel.nut.block;

import betel.nut.BetelNutMod;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.RotatedPillarBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

@SuppressWarnings("deprecation")
public final class ModBlocks {
	public static final Block BETEL_PALM_LOG = registerBlock("betel_palm_log",
			new RotatedPillarBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_LOG)));
	public static final Block BETEL_PALM_PLANKS = registerBlock("betel_palm_planks",
			new Block(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_PLANKS)));
	public static final Block BETEL_PALM_STAIRS = registerBlock("betel_palm_stairs",
			new StairBlock(BETEL_PALM_PLANKS.defaultBlockState(),
					BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_STAIRS)));
	public static final Block BETEL_PALM_SLAB = registerBlock("betel_palm_slab",
			new SlabBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_SLAB)));
	public static final Block BETEL_PALM_FENCE = registerBlock("betel_palm_fence",
			new FenceBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_FENCE)));
	public static final Block BETEL_PALM_FENCE_GATE = registerBlock("betel_palm_fence_gate",
			new FenceGateBlock(WoodType.OAK, BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_FENCE_GATE)));
	public static final Block BETEL_PALM_BUTTON = registerBlock("betel_palm_button",
			new ButtonBlock(BlockSetType.OAK, 30, BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_BUTTON)));
	public static final Block BETEL_PALM_PRESSURE_PLATE = registerBlock("betel_palm_pressure_plate",
			new PressurePlateBlock(BlockSetType.OAK,
					BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_PRESSURE_PLATE)));
	public static final Block BETEL_PALM_LEAVES = registerBlock("betel_palm_leaves",
			new LeavesBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_LEAVES)));
	public static final Block BETEL_PALM_SAPLING = registerBlock("betel_palm_sapling",
			new BetelPalmSaplingBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_SAPLING)));
	public static final Block BETEL_NUT_RESIDUE_BLOCK = registerBlock("betel_nut_residue_block",
			new Block(BlockBehaviour.Properties.of().strength(0.6F).sound(SoundType.MUD)));

	public static final Item BETEL_PALM_LOG_ITEM = registerBlockItem("betel_palm_log", BETEL_PALM_LOG);
	public static final Item BETEL_PALM_PLANKS_ITEM = registerBlockItem("betel_palm_planks", BETEL_PALM_PLANKS);
	public static final Item BETEL_PALM_STAIRS_ITEM = registerBlockItem("betel_palm_stairs", BETEL_PALM_STAIRS);
	public static final Item BETEL_PALM_SLAB_ITEM = registerBlockItem("betel_palm_slab", BETEL_PALM_SLAB);
	public static final Item BETEL_PALM_FENCE_ITEM = registerBlockItem("betel_palm_fence", BETEL_PALM_FENCE);
	public static final Item BETEL_PALM_FENCE_GATE_ITEM = registerBlockItem("betel_palm_fence_gate",
			BETEL_PALM_FENCE_GATE);
	public static final Item BETEL_PALM_BUTTON_ITEM = registerBlockItem("betel_palm_button", BETEL_PALM_BUTTON);
	public static final Item BETEL_PALM_PRESSURE_PLATE_ITEM = registerBlockItem("betel_palm_pressure_plate",
			BETEL_PALM_PRESSURE_PLATE);
	public static final Item BETEL_PALM_LEAVES_ITEM = registerBlockItem("betel_palm_leaves", BETEL_PALM_LEAVES);
	public static final Item BETEL_PALM_SAPLING_ITEM = registerBlockItem("betel_palm_sapling", BETEL_PALM_SAPLING);
	public static final Item BETEL_NUT_RESIDUE_BLOCK_ITEM = registerBlockItem("betel_nut_residue_block",
			BETEL_NUT_RESIDUE_BLOCK);

	private static Block registerBlock(String path, Block block) {
		return Registry.register(BuiltInRegistries.BLOCK, BetelNutMod.id(path), block);
	}

	private static Item registerBlockItem(String path, Block block) {
		ResourceLocation id = BetelNutMod.id(path);
		BlockItem blockItem = new BlockItem(block, new Item.Properties());
		return Items.registerItem(ResourceKey.create(Registries.ITEM, id), blockItem);
	}

	public static void register() {
		BetelNutMod.LOGGER.info("Registering betel palm blocks");
		FlammableBlockRegistry flammableBlocks = FlammableBlockRegistry.getDefaultInstance();
		flammableBlocks.add(BETEL_PALM_LOG, 5, 5);
		flammableBlocks.add(BETEL_PALM_PLANKS, 5, 20);
		flammableBlocks.add(BETEL_PALM_STAIRS, 5, 20);
		flammableBlocks.add(BETEL_PALM_SLAB, 5, 20);
		flammableBlocks.add(BETEL_PALM_FENCE, 5, 20);
		flammableBlocks.add(BETEL_PALM_FENCE_GATE, 5, 20);
		flammableBlocks.add(BETEL_PALM_BUTTON, 5, 20);
		flammableBlocks.add(BETEL_PALM_PRESSURE_PLATE, 5, 20);

		FuelRegistry.INSTANCE.add(BETEL_PALM_LOG_ITEM, 300);
		FuelRegistry.INSTANCE.add(BETEL_PALM_PLANKS_ITEM, 300);
		FuelRegistry.INSTANCE.add(BETEL_PALM_STAIRS_ITEM, 300);
		FuelRegistry.INSTANCE.add(BETEL_PALM_SLAB_ITEM, 150);
		FuelRegistry.INSTANCE.add(BETEL_PALM_FENCE_ITEM, 300);
		FuelRegistry.INSTANCE.add(BETEL_PALM_FENCE_GATE_ITEM, 300);
		FuelRegistry.INSTANCE.add(BETEL_PALM_BUTTON_ITEM, 100);
		FuelRegistry.INSTANCE.add(BETEL_PALM_PRESSURE_PLATE_ITEM, 300);
	}

	private ModBlocks() {
	}
}
