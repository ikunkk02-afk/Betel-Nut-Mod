package betel.nut.test;

import betel.nut.BetelNutMod;
import betel.nut.block.ModBlocks;
import betel.nut.item.ModItemGroups;
import com.mojang.authlib.GameProfile;
import io.netty.channel.embedded.EmbeddedChannel;
import java.util.List;
import java.util.UUID;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.fabricmc.fabric.api.registry.FlammableBlockRegistry;
import net.fabricmc.fabric.api.registry.FuelRegistry;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.tags.BlockTags;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.CreativeModeTabs;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public class BetelWoodGameTests implements FabricGameTest {
    private static BlockPos site(GameTestHelper h) {
        var base = h.absolutePos(new BlockPos(2, 2, 2));
        for (var pos : BlockPos.betweenClosed(base.offset(-1, 0, -1), base.offset(3, 4, 3)))
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        for (var pos : BlockPos.betweenClosed(base.offset(-1, -1, -1), base.offset(3, -1, 3)))
            h.getLevel().setBlock(pos, Blocks.STONE.defaultBlockState(), 3);
        return base;
    }

    private static ServerPlayer player(GameTestHelper h) {
        // GameTest's stock mock hardcodes isSpectator=false/isCreative=true regardless of game mode.
        // Use a real ServerPlayer with an embedded connection to exercise survival and spectator rules.
        var level = h.getLevel();
        var cookie = CommonListenerCookie.createInitial(new GameProfile(UUID.randomUUID(), "wood-test-player"), false);
        var player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        var connection = new Connection(PacketFlow.SERVERBOUND);
        new EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        player.setGameMode(GameType.SURVIVAL);
        return player;
    }

    private static void placeDoor(GameTestHelper h, ServerPlayer player, BlockPos pos) {
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()).add(0, .5, 0), Direction.UP, pos.below(), false);
        var result = player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        h.assertTrue(result.consumesAction(), "Valid door placement succeeds");
    }

    private static void pair(GameTestHelper h, BlockPos pos, boolean open, boolean powered) {
        for (var half : new DoubleBlockHalf[]{DoubleBlockHalf.LOWER, DoubleBlockHalf.UPPER}) {
            var state = h.getLevel().getBlockState(half == DoubleBlockHalf.LOWER ? pos : pos.above());
            h.assertTrue(state.is(ModBlocks.BETEL_PALM_DOOR) && state.getValue(DoorBlock.HALF) == half,
                    "Both door halves exist");
            h.assertTrue(state.getValue(DoorBlock.OPEN) == open && state.getValue(DoorBlock.POWERED) == powered,
                    "Both halves synchronize open and powered state");
        }
    }

    private static int doorDrops(GameTestHelper h, BlockPos pos) {
        return h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).stream()
                .filter(e -> e.getItem().is(ModBlocks.BETEL_PALM_DOOR_ITEM)).mapToInt(e -> e.getItem().getCount()).sum();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void doorPlacesInFourDirectionsAndConsumesOneItem(GameTestHelper h) {
        var pos = site(h);
        var player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.BETEL_PALM_DOOR_ITEM, 4));
        for (var facing : Direction.Plane.HORIZONTAL) {
            player.setYRot(facing.toYRot());
            placeDoor(h, player, pos);
            pair(h, pos, false, false);
            h.assertTrue(h.getLevel().getBlockState(pos).getValue(DoorBlock.FACING) == facing, "Door faces player direction");
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
        }
        h.assertTrue(player.getMainHandItem().isEmpty(), "Each complete two-block door consumes exactly one item");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void blockedDoorPlacementLeavesItemsAndWorldIntact(GameTestHelper h) {
        var pos = site(h);
        var player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.BETEL_PALM_DOOR_ITEM, 2));
        h.getLevel().setBlock(pos.above(), Blocks.STONE.defaultBlockState(), 3);
        var hit = new BlockHitResult(Vec3.atCenterOf(pos.below()), Direction.UP, pos.below(), false);
        player.getMainHandItem().useOn(new UseOnContext(player, InteractionHand.MAIN_HAND, hit));
        h.assertTrue(h.getLevel().getBlockState(pos).isAir() && h.getLevel().getBlockState(pos.above()).is(Blocks.STONE),
                "Obstructed upper half does not create a partial door or replace the obstruction");
        h.assertTrue(player.getMainHandItem().getCount() == 2, "Rejected placement consumes no item");
        h.getLevel().setBlock(pos.above(), Blocks.AIR.defaultBlockState(), 3);
        h.getLevel().setBlock(pos.below(), Blocks.AIR.defaultBlockState(), 3);
        h.assertTrue(!ModBlocks.BETEL_PALM_DOOR.defaultBlockState().canSurvive(h.getLevel(), pos), "Door requires support");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void adjacentDoorsUseOppositeHinges(GameTestHelper h) {
        var pos = site(h);
        var player = player(h);
        player.setYRot(Direction.NORTH.toYRot());
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.BETEL_PALM_DOOR_ITEM, 2));
        placeDoor(h, player, pos);
        placeDoor(h, player, pos.east());
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(DoorBlock.HINGE)
                        != h.getLevel().getBlockState(pos.east()).getValue(DoorBlock.HINGE), "Double doors select opposite hinges");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void manualAndRedstoneControlSynchronizeBothHalves(GameTestHelper h) {
        var pos = site(h);
        var player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.BETEL_PALM_DOOR_ITEM));
        placeDoor(h, player, pos);
        for (var clicked : new BlockPos[]{pos.above(), pos}) {
            h.getLevel().getBlockState(clicked).useWithoutItem(h.getLevel(), player,
                    new BlockHitResult(Vec3.atCenterOf(clicked), Direction.NORTH, clicked, false));
            pair(h, pos, clicked.equals(pos.above()), false);
        }
        for (var power : new BlockPos[]{pos.east(), pos.above().east()}) {
            h.getLevel().setBlock(power, Blocks.REDSTONE_BLOCK.defaultBlockState(), 3);
            pair(h, pos, true, true);
            h.getLevel().setBlock(power, Blocks.AIR.defaultBlockState(), 3);
            pair(h, pos, false, false);
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void breakingEitherHalfDropsExactlyOneDoor(GameTestHelper h) {
        var pos = site(h);
        var player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.BETEL_PALM_DOOR_ITEM, 2));
        for (var broken : new BlockPos[]{pos, pos.above()}) {
            placeDoor(h, player, pos);
            h.getLevel().destroyBlock(broken, true);
            h.assertTrue(h.getLevel().getBlockState(pos).isAir() && h.getLevel().getBlockState(pos.above()).isAir(),
                    "Breaking either half removes the complete door");
            h.assertTrue(doorDrops(h, pos) == 1, "One complete door drops exactly one item");
            h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(3)).forEach(ItemEntity::discard);
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void removingSupportDropsOneDoor(GameTestHelper h) {
        var pos = site(h);
        var player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.BETEL_PALM_DOOR_ITEM));
        placeDoor(h, player, pos);
        h.getLevel().destroyBlock(pos.below(), true);
        h.assertTrue(h.getLevel().getBlockState(pos).isAir() && h.getLevel().getBlockState(pos.above()).isAir(), "No floating door halves");
        h.assertTrue(doorDrops(h, pos) == 1, "Lost support drops exactly one door");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE, timeoutTicks = 80)
    public void droppedItemOpensDoorAndPlateReleases(GameTestHelper h) {
        var pos = site(h);
        var plate = pos.south();
        var player = player(h);
        player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(ModBlocks.BETEL_PALM_DOOR_ITEM));
        placeDoor(h, player, pos);
        player.setPos(Vec3.atCenterOf(pos.offset(3, 2, 3)));
        h.getLevel().setBlock(plate, ModBlocks.BETEL_PALM_PRESSURE_PLATE.defaultBlockState(), 3);
        var item = new ItemEntity(h.getLevel(), plate.getX() + .5, plate.getY() + .03, plate.getZ() + .5, new ItemStack(Items.STICK));
        item.setNoGravity(true);
        item.setDeltaMovement(Vec3.ZERO);
        h.getLevel().addFreshEntity(item);
        h.getLevel().getBlockState(plate).entityInside(h.getLevel(), plate, item);
        h.assertTrue(h.getLevel().getBlockState(plate).getValue(PressurePlateBlock.POWERED), "Dropped item activates wooden plate");
        h.assertTrue(h.getLevel().getBlockState(plate).getSignal(h.getLevel(), plate, Direction.NORTH) == 15, "Wooden plate outputs 15");
        pair(h, pos, true, true);
        item.discard();
        h.runAfterDelay(25, () -> {
            h.assertTrue(!h.getLevel().getBlockState(plate).getValue(PressurePlateBlock.POWERED), "Plate releases on scheduled recheck");
            pair(h, pos, false, false);
            h.succeed();
        });
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void playersAndMobsTriggerButSpectatorsDoNot(GameTestHelper h) {
        var pos = site(h);
        var player = player(h);
        h.getLevel().setBlock(pos, ModBlocks.BETEL_PALM_PRESSURE_PLATE.defaultBlockState(), 3);
        player.setPos(Vec3.atBottomCenterOf(pos));
        h.getLevel().getBlockState(pos).entityInside(h.getLevel(), pos, player);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(PressurePlateBlock.POWERED), "Survival player activates plate");
        player.setGameMode(GameType.SPECTATOR);
        h.getLevel().setBlock(pos, ModBlocks.BETEL_PALM_PRESSURE_PLATE.defaultBlockState(), 3);
        h.getLevel().getBlockState(pos).entityInside(h.getLevel(), pos, player);
        h.assertTrue(!h.getLevel().getBlockState(pos).getValue(PressurePlateBlock.POWERED), "Spectator does not activate plate");
        player.setPos(Vec3.atCenterOf(pos.offset(3, 2, 3)));
        var pig = EntityType.PIG.create(h.getLevel());
        pig.setPos(Vec3.atBottomCenterOf(pos));
        pig.setNoAi(true);
        h.getLevel().addFreshEntity(pig);
        h.getLevel().getBlockState(pos).entityInside(h.getLevel(), pos, pig);
        h.assertTrue(h.getLevel().getBlockState(pos).getValue(PressurePlateBlock.POWERED), "Mob activates wooden plate");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void woodRecipesAndAutomaticBookUnlocks(GameTestHelper h) {
        var level = h.getLevel();
        var manager = level.getRecipeManager();
        var plank = new ItemStack(ModBlocks.BETEL_PALM_PLANKS_ITEM);
        var doorInput = CraftingInput.of(2, 3, List.of(plank, plank, plank, plank, plank, plank));
        var plateInput = CraftingInput.of(2, 1, List.of(plank, plank));
        var door = manager.getRecipeFor(RecipeType.CRAFTING, doorInput, level).orElseThrow();
        var plate = manager.getRecipeFor(RecipeType.CRAFTING, plateInput, level).orElseThrow();
        var result = door.value().assemble(doorInput, level.registryAccess());
        h.assertTrue(result.is(ModBlocks.BETEL_PALM_DOOR_ITEM) && result.getCount() == 3, "Six planks craft three doors");
        result = plate.value().assemble(plateInput, level.registryAccess());
        h.assertTrue(result.is(ModBlocks.BETEL_PALM_PRESSURE_PLATE_ITEM) && result.getCount() == 1, "Two planks craft one plate");
        var wrong = CraftingInput.of(2, 1, List.of(plank, new ItemStack(Items.OAK_PLANKS)));
        h.assertTrue(!plate.value().matches(wrong, level), "Palm pressure plate cannot consume mixed wood");
        var player = player(h);
        player.getInventory().add(plank.copy());
        CriteriaTriggers.INVENTORY_CHANGED.trigger(player, player.getInventory(), plank);
        for (var name : List.of("door", "pressure_plate", "trapdoor", "button", "stairs", "slab", "fence", "fence_gate"))
            h.assertTrue(player.getRecipeBook().contains(BetelNutMod.id("betel_palm_" + name)), "Planks unlock wood recipe: " + name);
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void tagsFuelFireAndCreativeEntriesAreAvailable(GameTestHelper h) {
        h.assertTrue(ModBlocks.BETEL_PALM_DOOR.defaultBlockState().is(BlockTags.WOODEN_DOORS), "Door has wooden door block tag");
        h.assertTrue(new ItemStack(ModBlocks.BETEL_PALM_DOOR_ITEM).is(ItemTags.WOODEN_DOORS), "Door has wooden door item tag");
        h.assertTrue(ModBlocks.BETEL_PALM_TRAPDOOR.defaultBlockState().is(BlockTags.WOODEN_TRAPDOORS)
                && new ItemStack(ModBlocks.BETEL_PALM_TRAPDOOR_ITEM).is(ItemTags.WOODEN_TRAPDOORS), "Trapdoor has wooden tags");
        h.assertTrue(ModBlocks.BETEL_PALM_PRESSURE_PLATE.defaultBlockState().is(BlockTags.WOODEN_PRESSURE_PLATES)
                && new ItemStack(ModBlocks.BETEL_PALM_PRESSURE_PLATE_ITEM).is(ItemTags.WOODEN_PRESSURE_PLATES), "Plate has wooden tags");
        for (var block : List.of(ModBlocks.BETEL_PALM_DOOR, ModBlocks.BETEL_PALM_TRAPDOOR, ModBlocks.BETEL_PALM_PRESSURE_PLATE)) {
            h.assertTrue(block.defaultBlockState().is(BlockTags.MINEABLE_WITH_AXE), "Wood is mined efficiently with an axe");
            h.assertTrue(FlammableBlockRegistry.getDefaultInstance().get(block) != null, "Wood can burn");
            h.assertTrue(FuelRegistry.INSTANCE.get(block.asItem()) > 0, "Wood can fuel a furnace");
        }
        CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(), false, h.getLevel().registryAccess());
        for (var key : List.of(ModItemGroups.BETEL_NUT_GROUP_KEY, CreativeModeTabs.BUILDING_BLOCKS, CreativeModeTabs.REDSTONE_BLOCKS)) {
            var tab = BuiltInRegistries.CREATIVE_MODE_TAB.get(key);
            for (var item : List.of(ModBlocks.BETEL_PALM_DOOR_ITEM, ModBlocks.BETEL_PALM_PRESSURE_PLATE_ITEM))
                h.assertTrue(tab.contains(new ItemStack(item)), "Door and plate appear in creative tab " + key.location());
        }
        h.succeed();
    }
}
