package betel.nut.test;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMidnightConfig;
import betel.nut.block.BetelNutClusterBlock;
import betel.nut.block.BetelPalmSaplingBlock;
import betel.nut.block.ModBlocks;
import betel.nut.component.BetelNutEntityComponents;
import betel.nut.component.BetelNutWorldComponents;
import betel.nut.component.BetelSkyblockWorldComponent;
import betel.nut.item.ModItems;
import betel.nut.skyblock.BetelSkyblockManager;
import betel.nut.skyblock.SkyblockComposting;
import betel.nut.skyblock.SkyblockSpawnSafety;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ChunkMap;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ComposterBlock;
import net.minecraft.world.level.chunk.status.WorldGenContext;
import net.minecraft.world.level.levelgen.FlatLevelSource;
import net.minecraft.world.level.levelgen.flat.FlatLayerInfo;
import net.minecraft.world.level.levelgen.flat.FlatLevelGeneratorSettings;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;

public final class BetelSkyblockGameTests implements FabricGameTest {
    private static ServerPlayer player(GameTestHelper h) {
        var cookie = net.minecraft.server.network.CommonListenerCookie.createInitial(
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "skyblock-test"), false);
        var p = new ServerPlayer(h.getLevel().getServer(), h.getLevel(), cookie.gameProfile(), cookie.clientInformation());
        var connection = new net.minecraft.network.Connection(net.minecraft.network.protocol.PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        h.getLevel().getServer().getPlayerList().placeNewPlayer(connection, p, cookie);
        p.setGameMode(GameType.SURVIVAL);
        p.teleportTo(h.absolutePos(new BlockPos(1, 2, 1)).getX(), 200, h.absolutePos(BlockPos.ZERO).getZ());
        return p;
    }
    private static Object invoke(String name, Class<?>[] types, Object... args) throws ReflectiveOperationException {
        Method method = BetelSkyblockManager.class.getDeclaredMethod(name, types);
        method.setAccessible(true); return method.invoke(null, args);
    }
    private static BlockHitResult hit(BlockPos p) { return new BlockHitResult(Vec3.atCenterOf(p), Direction.UP, p, false); }
    private static void clear(ServerLevel level, BlockPos p, int r) {
        for (BlockPos b : BlockPos.betweenClosed(p.offset(-r, -17, -r), p.offset(r, 18, r)))
            level.setBlock(b, Blocks.AIR.defaultBlockState(), 2);
    }

    /** Synchronous fixture changes only preset detection; restore before the server advances a tick. */
    private static final class Preset implements AutoCloseable {
        private final ServerLevel level;
        private final Field context;
        private final Object oldContext;
        private final CompoundTag oldWorld = new CompoundTag();
        private final BlockPos oldSpawn;
        private final float oldAngle;
        private final int classicY = BetelNutMidnightConfig.classicIslandY, oneY = BetelNutMidnightConfig.oneBlockY;
        private final boolean classic = BetelNutMidnightConfig.enableClassicSkyblock, one = BetelNutMidnightConfig.enableOneBlockSkyblock;
        Preset(ServerLevel level, boolean oneBlock) throws ReflectiveOperationException {
            this.level = level; oldSpawn = level.getSharedSpawnPos(); oldAngle = level.getSharedSpawnAngle();
            BetelNutWorldComponents.SKYBLOCK_WORLD.get(level).writeToNbt(oldWorld, level.registryAccess());
            context = ChunkMap.class.getDeclaredField("worldGenContext"); context.setAccessible(true);
            oldContext = context.get(level.getChunkSource().chunkMap);
            var old = (WorldGenContext) oldContext;
            var settings = new FlatLevelGeneratorSettings(Optional.empty(),
                    level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.THE_VOID), List.of());
            if (oneBlock) settings.getLayersInfo().add(new FlatLayerInfo(1, Blocks.AIR));
            settings.updateLayers();
            context.set(level.getChunkSource().chunkMap, new WorldGenContext(level, new FlatLevelSource(settings),
                    old.structureManager(), old.lightEngine(), old.mainThreadMailBox()));
            BetelNutMidnightConfig.enableClassicSkyblock = BetelNutMidnightConfig.enableOneBlockSkyblock = true;
            BetelNutMidnightConfig.classicIslandY = BetelNutMidnightConfig.oneBlockY = 244;
        }
        public void close() throws ReflectiveOperationException {
            context.set(level.getChunkSource().chunkMap, oldContext);
            BetelNutWorldComponents.SKYBLOCK_WORLD.get(level).readFromNbt(oldWorld, level.registryAccess());
            level.setDefaultSpawnPos(oldSpawn, oldAngle);
            BetelNutMidnightConfig.classicIslandY = classicY; BetelNutMidnightConfig.oneBlockY = oneY;
            BetelNutMidnightConfig.enableClassicSkyblock = classic; BetelNutMidnightConfig.enableOneBlockSkyblock = one;
        }
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void voidMonsterSpawnsAreScopedToEnabledSkyblocks(GameTestHelper h) throws ReflectiveOperationException {
        var level = h.getLevel();
        var biome = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(Biomes.THE_VOID);
        var pos = h.absolutePos(new BlockPos(2, 2, 2));
        Method mobsAt = net.minecraft.world.level.NaturalSpawner.class.getDeclaredMethod("mobsAt",
                ServerLevel.class, net.minecraft.world.level.StructureManager.class,
                net.minecraft.world.level.chunk.ChunkGenerator.class, net.minecraft.world.entity.MobCategory.class,
                BlockPos.class, net.minecraft.core.Holder.class);
        mobsAt.setAccessible(true);
        var monsters = net.minecraft.world.entity.MobCategory.MONSTER;
        var creatures = net.minecraft.world.entity.MobCategory.CREATURE;
        java.util.function.Function<net.minecraft.world.entity.MobCategory, net.minecraft.util.random.WeightedRandomList<?>> pool = category -> {
            try {
                return (net.minecraft.util.random.WeightedRandomList<?>) mobsAt.invoke(null, level,
                        level.structureManager(), level.getChunkSource().getGenerator(), category, pos, biome);
            } catch (ReflectiveOperationException e) { throw new RuntimeException(e); }
        };
        h.assertTrue(pool.apply(monsters).isEmpty(), "Ordinary worlds keep the empty void spawn pool");
        for (boolean oneBlock : new boolean[]{false, true}) try (var preset = new Preset(level, oneBlock)) {
            h.assertTrue(pool.apply(monsters).unwrap().equals(level.registryAccess().registryOrThrow(Registries.BIOME)
                    .getOrThrow(Biomes.PLAINS).getMobSettings().getMobs(monsters).unwrap()),
                    "Actual natural-spawn lookup supplies the plains monster pool in both skyblocks");
            h.assertTrue(pool.apply(creatures).isEmpty(), "Other mob categories remain unchanged");
            if (oneBlock) BetelNutMidnightConfig.enableOneBlockSkyblock = false;
            else BetelNutMidnightConfig.enableClassicSkyblock = false;
            h.assertTrue(pool.apply(monsters).isEmpty(), "Disabled modes retain vanilla void behavior");
        }
        h.assertTrue(biome.value().getMobSettings().getMobs(monsters).isEmpty(),
                "The shared vanilla biome is never modified");
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void classicChestSupportsHarvestFarmingAndRecovery(GameTestHelper h) throws ReflectiveOperationException {
        try (var preset = new Preset(h.getLevel(), false)) {
            var level = h.getLevel(); clear(level, new BlockPos(0, 244, 0), 9);
            BetelNutWorldComponents.SKYBLOCK_WORLD.get(level).resetBetelSkyIslandGeneration();
            h.assertTrue(BetelSkyblockManager.generateForCommand(level) == BetelSkyblockManager.GenerationResult.GENERATED, "Classic island generates");
            var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(new BlockPos(2,245,0));
            h.assertTrue(chest.countItem(Items.SHEARS) == 1 && chest.countItem(Items.ICE) == 2
                    && chest.countItem(Items.BONE_MEAL) == 8 && chest.countItem(Items.WHEAT_SEEDS) == 2,
                    "Actual starter chest supplies harvest tools, infinite water and a crop cycle");
            h.assertTrue(chest.countItem(Items.JUNGLE_SAPLING) == 4 && chest.countItem(Items.COCOA_BEANS) == 1
                    && chest.countItem(Items.BUDDING_AMETHYST) == 1 && chest.countItem(Items.MOSS_BLOCK) == 1,
                    "Renewable variant ingredients and soil have starting seeds");
            h.assertTrue(chest.countItem(Items.BREAD) == 8 && chest.countItem(ModItems.RELIEF_POTION) == 2
                    && chest.countItem(ModItems.GOLDEN_APPLE_PASTE) == 1, "Ordinary staple and finite recovery reserve");
            chest.setItem(0, new ItemStack(Items.DIAMOND));
            h.assertTrue(BetelSkyblockManager.generateForCommand(level) == BetelSkyblockManager.GenerationResult.ALREADY_GENERATED
                    && chest.getItem(0).is(Items.DIAMOND), "Generation cannot refill an existing chest");
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void oneBlockStarterIsOnceOnlyAndDoesNotRegenerateTerrain(GameTestHelper h) throws ReflectiveOperationException {
        try (var preset = new Preset(h.getLevel(), true)) {
            var level = h.getLevel(); var center = new BlockPos(0,244,0); clear(level, center, 9);
            var data = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level); data.resetBetelOneBlockIslandGeneration();
            h.assertTrue(BetelSkyblockManager.generateOneBlockForCommand(level) == BetelSkyblockManager.GenerationResult.GENERATED, "One block generates");
            long blocks = BlockPos.betweenClosedStream(center.offset(-2,-1,-2),center.offset(2,5,2))
                    .filter(pos -> !level.getBlockState(pos).isAir()).count();
            h.assertTrue(blocks == 1, "World still starts with exactly one block");
            var p = player(h); p.getInventory().clearContent();
            var types = new Class<?>[]{ServerPlayer.class,ServerLevel.class,BetelSkyblockWorldComponent.class};
            invoke("handleFirstOneBlockSkyblockJoin", types, p, level, data);
            h.assertTrue(p.getInventory().countItem(Items.SHEARS) == 1 && p.getInventory().countItem(Items.ICE) == 2
                    && p.getInventory().countItem(ModItems.BETEL_NUT_RESIDUE) == 18, "Personal kit includes tools and two compressible footholds");
            invoke("handleFirstOneBlockSkyblockJoin", types, p, level, data);
            h.assertTrue(p.getInventory().countItem(Items.SHEARS) == 1, "Joining again cannot repeat supplies");
            level.removeBlock(center, false);
            h.assertTrue(level.getBlockState(center).isAir(), "Mining the original block does not regenerate it");
            level.getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void rawFruitAndLeavesCompostInBothPresets(GameTestHelper h) throws ReflectiveOperationException {
        var level = h.getLevel(); var p = player(h); var pos = h.absolutePos(new BlockPos(2,2,2));
        for (boolean one : new boolean[]{false,true}) try (var preset = new Preset(level, one)) {
            for (var input : List.of(ModItems.RAW_BETEL_NUT,ModItems.BETEL_LEAF,ModBlocks.BETEL_PALM_LEAVES.asItem())) {
                level.setBlock(pos, Blocks.COMPOSTER.defaultBlockState(), 3);
                p.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(input,64));
                int calls = 0;
                while (level.getBlockState(pos).getValue(ComposterBlock.LEVEL) < 7 && calls++ < 64)
                    h.assertTrue(SkyblockComposting.use(p,level,InteractionHand.OFF_HAND,hit(pos)) == InteractionResult.SUCCESS, "Valid manual input is handled");
                h.assertTrue(level.getBlockState(pos).getValue(ComposterBlock.LEVEL) == 7
                        && p.getOffhandItem().getCount() < 64, "Actual inputs fill composter without eating");
                int count = p.getOffhandItem().getCount();
                SkyblockComposting.use(p,level,InteractionHand.OFF_HAND,hit(pos));
                h.assertTrue(count == p.getOffhandItem().getCount(), "Full composter consumes no further inputs");
                level.getBlockState(pos).tick(level,pos,RandomSource.create(0));
                h.assertTrue(level.getBlockState(pos).getValue(ComposterBlock.LEVEL) == 8, "Vanilla maturation produces ready compost");
                p.setItemInHand(InteractionHand.MAIN_HAND, ItemStack.EMPTY);
                level.getBlockState(pos).useWithoutItem(level,p,hit(pos));
                h.assertTrue(level.getBlockState(pos).getValue(ComposterBlock.LEVEL) == 0
                        && !level.getEntitiesOfClass(ItemEntity.class,new AABB(pos).inflate(2), e -> e.getItem().is(Items.BONE_MEAL)).isEmpty(),
                        "Vanilla empty-hand extraction returns real bone meal");
            }
        }
        level.getServer().getPlayerList().remove(p); h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void compostDoesNotChangeOrdinaryWorldsOrDisabledModes(GameTestHelper h) throws ReflectiveOperationException {
        var level = h.getLevel(); var p = player(h); var pos = h.absolutePos(new BlockPos(2,2,2));
        level.setBlock(pos, Blocks.COMPOSTER.defaultBlockState(), 3);
        p.setItemInHand(InteractionHand.MAIN_HAND,new ItemStack(ModItems.RAW_BETEL_NUT,4));
        h.assertTrue(SkyblockComposting.use(p,level,InteractionHand.MAIN_HAND,hit(pos)) == InteractionResult.PASS
                && p.getMainHandItem().getCount() == 4, "Ordinary worlds keep their previous compost inputs");
        try (var preset = new Preset(level,false)) {
            BetelNutMidnightConfig.enableClassicSkyblock = false;
            h.assertTrue(SkyblockComposting.use(p,level,InteractionHand.MAIN_HAND,hit(pos)) == InteractionResult.PASS, "Disabled mode does not enable extra inputs");
        }
        level.getServer().getPlayerList().remove(p); h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void rescueAvoidsObstructionsHazardsAndSupportsSlabs(GameTestHelper h) {
        var level = h.getLevel(); var p = player(h); var origin = h.absolutePos(new BlockPos(2,100,2));
        clear(level,origin,17); level.setBlock(origin.below(),Blocks.STONE.defaultBlockState(),3);
        for (int y = 0; y <= 18; y++) level.setBlock(origin.above(y),Blocks.GLASS.defaultBlockState(),3);
        level.setBlock(origin.east().below(),Blocks.MAGMA_BLOCK.defaultBlockState(),3);
        var safe = origin.west(); level.setBlock(safe.below(),Blocks.OAK_SLAB.defaultBlockState(),3);
        Vec3 target = SkyblockSpawnSafety.findLanding(p,level,origin);
        h.assertTrue(target != null && target.x == safe.getX()+.5 && target.y == safe.getY()-.5,
                "Rescue avoids occupied feet and magma; lands on bottom slab surface");
        h.assertTrue(level.getBlockState(origin).is(Blocks.GLASS), "Player obstruction is preserved");
        level.getServer().getPlayerList().remove(p); h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void missingIslandGetsOneNonFarmableEmergencyFoothold(GameTestHelper h) {
        var level = h.getLevel(); var p = player(h); var origin = h.absolutePos(new BlockPos(2,100,2));
        clear(level,origin,17); boolean emergency = BetelNutMidnightConfig.skyblockEmergencyPlatform;
        try {
            BetelNutMidnightConfig.skyblockEmergencyPlatform = false;
            h.assertTrue(SkyblockSpawnSafety.findLanding(p,level,origin) == null, "Disabled fallback does not edit terrain");
            BetelNutMidnightConfig.skyblockEmergencyPlatform = true;
            Vec3 first = SkyblockSpawnSafety.findLanding(p,level,origin);
            Vec3 second = SkyblockSpawnSafety.findLanding(p,level,origin);
            h.assertTrue(first != null && first.equals(second)
                    && level.getBlockState(BlockPos.containing(first).below()).is(Blocks.BEDROCK), "Missing ground gets one reusable, unmineable foothold");
            long blocks = BlockPos.betweenClosedStream(origin.offset(-4,-4,-4),origin.offset(4,4,4))
                    .filter(pos -> !level.getBlockState(pos).isAir()).count();
            h.assertTrue(blocks == 1, "Repeated rescue never grants additional blocks");
        } finally { BetelNutMidnightConfig.skyblockEmergencyPlatform = emergency; }
        level.getServer().getPlayerList().remove(p); h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void legacySupplyUpgradePersistsWithoutChangingChest(GameTestHelper h) throws ReflectiveOperationException {
        try (var preset = new Preset(h.getLevel(),false)) {
            var level = h.getLevel(); var data = BetelNutWorldComponents.SKYBLOCK_WORLD.get(level);
            var tag = new CompoundTag(); data.markGenerated(new BlockPos(7,140,9)); data.writeToNbt(tag,level.registryAccess());
            tag.remove("survivalSuppliesProvided"); data.readFromNbt(tag,level.registryAccess());
            h.assertTrue(!data.hasSurvivalSuppliesProvided() && data.getIslandCenter().equals(new BlockPos(7,140,9)), "Old save needs upgrade and retains coordinates");
            var chestPos = h.absolutePos(new BlockPos(2,2,2)); level.setBlock(chestPos,Blocks.CHEST.defaultBlockState(),3);
            var chest = (net.minecraft.world.level.block.entity.ChestBlockEntity) level.getBlockEntity(chestPos);
            chest.setItem(0,new ItemStack(Items.DIAMOND,3));
            var p = player(h);
            int before = level.getServer().getPlayerList().getPlayers().stream().mapToInt(v -> v.getInventory().countItem(Items.SHEARS)).sum();
            var types = new Class<?>[]{net.minecraft.server.MinecraftServer.class,ServerLevel.class,BetelSkyblockWorldComponent.class,boolean.class};
            invoke("upgradeSurvivalSupplies",types,level.getServer(),level,data,false);
            invoke("upgradeSurvivalSupplies",types,level.getServer(),level,data,false);
            int after = level.getServer().getPlayerList().getPlayers().stream().mapToInt(v -> v.getInventory().countItem(Items.SHEARS)).sum();
            h.assertTrue(after == before+1 && chest.getItem(0).getCount() == 3 && chest.getItem(0).is(Items.DIAMOND), "Island upgrade gives one shared kit without touching chest");
            var saved = new CompoundTag(); data.writeToNbt(saved,level.registryAccess());
            var restored = new BetelSkyblockWorldComponent(level); restored.readFromNbt(saved,level.registryAccess());
            h.assertTrue(restored.hasSurvivalSuppliesProvided() && restored.getIslandCenter().equals(new BlockPos(7,140,9)), "Claim flag and original coordinates survive reload");
            level.getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }

    @GameTest(template = EMPTY_STRUCTURE)
    public void globalVoidSwitchAndBothRescueRoutesUseSafeGround(GameTestHelper h) throws ReflectiveOperationException {
        var level = h.getLevel(); var p = player(h); var origin = h.absolutePos(new BlockPos(2,100,2));
        clear(level,origin,17); level.setBlock(origin.below(),Blocks.STONE.defaultBlockState(),3);
        var config = BetelNutConfig.get(); boolean enabled = config.enableBetelSkyblockVoidProtection;
        int minY = config.betelSkyblockVoidProtectionMinY;
        boolean classic = BetelNutMidnightConfig.classicEnableVoidRescue, one = BetelNutMidnightConfig.oneBlockEnableVoidRescue;
        try {
            config.betelSkyblockVoidProtectionMinY = 0;
            BetelNutMidnightConfig.classicEnableVoidRescue = BetelNutMidnightConfig.oneBlockEnableVoidRescue = true;
            var data = new BetelSkyblockWorldComponent(level);
            data.markGenerated(origin.below(4)); data.markOneBlockGenerated(origin.below(2));
            for (String method : List.of("handleVoidProtection","handleOneBlockVoidProtection")) {
                p.teleportTo(origin.getX()+.5,-10,origin.getZ()+.5);
                config.enableBetelSkyblockVoidProtection = false;
                var types = new Class<?>[]{ServerPlayer.class,ServerLevel.class,BetelSkyblockWorldComponent.class};
                invoke(method,types,p,level,data);
                h.assertTrue(p.getY() == -10,"Global disable is respected by both rescue routes");
                config.enableBetelSkyblockVoidProtection = true;
                invoke(method,types,p,level,data);
                h.assertTrue(p.blockPosition().equals(origin) && p.fallDistance == 0 && p.getDeltaMovement().equals(Vec3.ZERO),
                        "Actual rescue lands safely and resets fall damage and velocity");
            }
        } finally {
            config.enableBetelSkyblockVoidProtection = enabled; config.betelSkyblockVoidProtectionMinY = minY;
            BetelNutMidnightConfig.classicEnableVoidRescue = classic; BetelNutMidnightConfig.oneBlockEnableVoidRescue = one;
            level.getServer().getPlayerList().remove(p);
        }
        h.succeed();
    }
}
