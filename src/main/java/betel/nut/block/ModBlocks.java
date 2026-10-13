package betel.nut.block;

import betel.nut.BetelNutMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Items;
import betel.nut.item.BetelNutClusterItem;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import net.minecraft.world.level.block.state.properties.WoodType;

@SuppressWarnings("deprecation")
public final class ModBlocks {
	public static final Block BETEL_PALM_LOG = registerBlock("betel_palm_log",
			new BetelPalmLogBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_LOG).noOcclusion().randomTicks()));
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
	public static final Block BETEL_PALM_TRAPDOOR = registerBlock("betel_palm_trapdoor",
			new TrapDoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_TRAPDOOR)));
	public static final Block BETEL_PALM_DOOR = registerBlock("betel_palm_door",
			new DoorBlock(BlockSetType.OAK, BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_DOOR)));
	public static final Block BETEL_PALM_LEAVES = registerBlock("betel_palm_leaves",
			new BetelPalmLeavesBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_LEAVES).noOcclusion()));
	public static final Block BETEL_PALM_SAPLING = registerBlock("betel_palm_sapling",
			new BetelPalmSaplingBlock(BlockBehaviour.Properties.ofLegacyCopy(Blocks.OAK_SAPLING)));
	public static final Block BETEL_NUT_CLUSTER = registerBlock("betel_nut_cluster",
			new BetelNutClusterBlock(BlockBehaviour.Properties.of().strength(0.3F).noOcclusion().randomTicks().sound(SoundType.CROP)));
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
	public static final Item BETEL_PALM_TRAPDOOR_ITEM = registerBlockItem("betel_palm_trapdoor",
			BETEL_PALM_TRAPDOOR);
	public static final Item BETEL_PALM_DOOR_ITEM = registerBlockItem("betel_palm_door", BETEL_PALM_DOOR);
	public static final Item BETEL_PALM_LEAVES_ITEM = registerBlockItem("betel_palm_leaves", BETEL_PALM_LEAVES);
	public static final Item BETEL_PALM_SAPLING_ITEM = registerBlockItem("betel_palm_sapling", BETEL_PALM_SAPLING);
	// Keep the existing ripe item ID; a distinct green item prevents inventory maturity ambiguity.
	public static final Item UNRIPE_BETEL_NUT_CLUSTER_ITEM = registerClusterItem("unripe_betel_nut_cluster", 1);
	public static final Item BETEL_NUT_CLUSTER_ITEM = registerClusterItem("betel_nut_cluster", 2);
	public static final Item BETEL_NUT_RESIDUE_BLOCK_ITEM = registerBlockItem("betel_nut_residue_block",
			BETEL_NUT_RESIDUE_BLOCK);

	private static Block registerBlock(String path, Block block) {
		return betel.nut.platform.BetelRegistries.add(BuiltInRegistries.BLOCK, BetelNutMod.id(path), block);
	}

	private static Item registerBlockItem(String path, Block block) {
		ResourceLocation id = BetelNutMod.id(path);
		BlockItem blockItem = new BlockItem(block, new Item.Properties());
		return betel.nut.platform.BetelRegistries.item(ResourceKey.create(Registries.ITEM, id), blockItem);
	}

	private static Item registerClusterItem(String path, int age) {
		return betel.nut.platform.BetelRegistries.item(ResourceKey.create(Registries.ITEM, BetelNutMod.id(path)),
				new BetelNutClusterItem(BETEL_NUT_CLUSTER, age));
	}

	public static void register() {
		BetelNutMod.LOGGER.info("Registering betel palm blocks");
		var flammableBlocks = betel.nut.platform.BetelPlatform.FLAMMABILITY;
		flammableBlocks.add(BETEL_PALM_LOG, 5, 5);
		flammableBlocks.add(BETEL_PALM_PLANKS, 5, 20);
		flammableBlocks.add(BETEL_PALM_STAIRS, 5, 20);
		flammableBlocks.add(BETEL_PALM_SLAB, 5, 20);
		flammableBlocks.add(BETEL_PALM_FENCE, 5, 20);
		flammableBlocks.add(BETEL_PALM_FENCE_GATE, 5, 20);
		flammableBlocks.add(BETEL_PALM_BUTTON, 5, 20);
		flammableBlocks.add(BETEL_PALM_PRESSURE_PLATE, 5, 20);
		flammableBlocks.add(BETEL_PALM_TRAPDOOR, 5, 20);
		flammableBlocks.add(BETEL_PALM_DOOR, 5, 20);

		betel.nut.platform.BetelPlatform.fuel(BETEL_PALM_LOG_ITEM, 300);
		betel.nut.platform.BetelPlatform.fuel(BETEL_PALM_PLANKS_ITEM, 300);
		betel.nut.platform.BetelPlatform.fuel(BETEL_PALM_STAIRS_ITEM, 300);
		betel.nut.platform.BetelPlatform.fuel(BETEL_PALM_SLAB_ITEM, 150);
		betel.nut.platform.BetelPlatform.fuel(BETEL_PALM_FENCE_ITEM, 300);
		betel.nut.platform.BetelPlatform.fuel(BETEL_PALM_FENCE_GATE_ITEM, 300);
		betel.nut.platform.BetelPlatform.fuel(BETEL_PALM_BUTTON_ITEM, 100);
		betel.nut.platform.BetelPlatform.fuel(BETEL_PALM_PRESSURE_PLATE_ITEM, 300);
		betel.nut.platform.BetelPlatform.fuel(BETEL_PALM_TRAPDOOR_ITEM, 300);
		betel.nut.platform.BetelPlatform.fuel(BETEL_PALM_DOOR_ITEM, 200);
	}

	private ModBlocks() {
	}
}
