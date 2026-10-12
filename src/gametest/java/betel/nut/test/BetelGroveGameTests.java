package betel.nut.test;

import betel.nut.BetelNutConfig;
import betel.nut.block.ModBlocks;
import betel.nut.worldgen.BetelPalmGroveGenerator;
import betel.nut.worldgen.BetelPalmGroveGenerator.Habitat;
import java.util.List;
import java.util.function.Function;
import net.fabricmc.fabric.api.gametest.v1.FabricGameTest;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.block.Blocks;

public class BetelGroveGameTests implements FabricGameTest {
    private static final String SITE = "betel_art_tests:grove_site";

    private static BlockPos site(GameTestHelper h) {
        var base = h.absolutePos(new BlockPos(24, 3, 24));
        for (var pos : BlockPos.betweenClosed(base.offset(-22, 0, -22), base.offset(22, 36, 22)))
            h.getLevel().setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        for (var pos : BlockPos.betweenClosed(base.offset(-22, -2, -22), base.offset(22, -1, 22)))
            h.getLevel().setBlock(pos, Blocks.DIRT.defaultBlockState(), 2);
        h.assertTrue(h.getLevel().getHeight(net.minecraft.world.level.levelgen.Heightmap.Types.WORLD_SURFACE,
                base.getX(), base.getZ()) == base.getY(), "Fixture must expose the ground to the sky");
        return base;
    }

    private static BetelNutConfig config() {
        var config = new BetelNutConfig();
        config.betelPalmSpawnChance = 1;
        config.betelPalmMinHeight = 9;
        config.betelPalmMaxHeight = 9;
        return config;
    }

    private static Holder<Biome> biome(GameTestHelper h, ResourceKey<Biome> key) {
        return h.getLevel().registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(key);
    }

    private static void groveChecks(GameTestHelper h, List<BlockPos> bases, int min, int max) {
        h.assertTrue(bases.size() >= min && bases.size() <= max, "Grove size in expected range: " + bases.size());
        for (var pos : bases) {
            h.assertTrue(h.getLevel().getBlockState(pos).is(ModBlocks.BETEL_PALM_LOG), "Every recorded tree has a trunk");
            h.assertTrue(h.getLevel().getFluidState(pos).isEmpty(), "Tree roots are dry");
            for (var other : bases) if (!other.equals(pos)) {
                int dx = other.getX() - pos.getX(), dz = other.getZ() - pos.getZ();
                h.assertTrue(dx * dx + dz * dz >= 25, "Trunks are spaced at least five blocks apart");
            }
        }
    }

    @GameTest(template = SITE)
    public void defaultHabitatSelectionAndSwitches(GameTestHelper h) {
        var config = config();
        h.assertTrue(BetelPalmGroveGenerator.habitatFor(Biomes.SPARSE_JUNGLE, config) == Habitat.SPARSE_JUNGLE, "Sparse jungle enabled");
        h.assertTrue(BetelPalmGroveGenerator.habitatFor(Biomes.JUNGLE, config) == Habitat.JUNGLE, "Jungle enabled");
        h.assertTrue(BetelPalmGroveGenerator.habitatFor(Biomes.BAMBOO_JUNGLE, config) == Habitat.BAMBOO_JUNGLE, "Bamboo jungle retained");
        h.assertTrue(BetelPalmGroveGenerator.habitatFor(Biomes.RIVER, config) == Habitat.RIVERBANK, "River context considered");
        for (var key : List.of(Biomes.PLAINS, Biomes.FROZEN_RIVER, Biomes.DESERT, Biomes.BEACH, Biomes.MANGROVE_SWAMP))
            h.assertTrue(BetelPalmGroveGenerator.habitatFor(key, config) == null, "Unrelated biome disabled by default");
        config.betelPalmGenerateInBambooJungle = false;
        config.betelPalmGenerateInSparseJungle = false;
        config.betelPalmGenerateNearJungleRivers = false;
        h.assertTrue(BetelPalmGroveGenerator.habitatFor(Biomes.BAMBOO_JUNGLE, config) == null
                && BetelPalmGroveGenerator.habitatFor(Biomes.SPARSE_JUNGLE, config) == null
                && BetelPalmGroveGenerator.habitatFor(Biomes.RIVER, config) == null, "New habitat switches work independently");
        config.betelPalmGenerateInJungle = false;
        h.assertTrue(BetelPalmGroveGenerator.habitatFor(Biomes.JUNGLE, config) == null, "Legacy jungle master switch preserved");
        h.succeed();
    }

    @GameTest(template = SITE)
    public void sparseJungleProducesSpacedGrove(GameTestHelper h) {
        var base = site(h);
        var sparse = biome(h, Biomes.SPARSE_JUNGLE);
        var bases = BetelPalmGroveGenerator.generate(h.getLevel(), base, RandomSource.create(11), config(), pos -> sparse);
        groveChecks(h, bases, 4, 8);
        h.succeed();
    }

    @GameTest(template = SITE)
    public void normalJungleAndBambooUseSmallGroups(GameTestHelper h) {
        var base = site(h);
        var jungle = biome(h, Biomes.JUNGLE);
        var bases = BetelPalmGroveGenerator.generate(h.getLevel(), base, RandomSource.create(4096), config(), pos -> jungle);
        groveChecks(h, bases, 1, 3);
        base = site(h);
        var bamboo = biome(h, Biomes.BAMBOO_JUNGLE);
        bases = BetelPalmGroveGenerator.generate(h.getLevel(), base, RandomSource.create(4096), config(), pos -> bamboo);
        groveChecks(h, bases, 1, 3);
        h.succeed();
    }

    @GameTest(template = SITE)
    public void shortUndergrowthDoesNotHideSuitableSoil(GameTestHelper h) {
        var base = site(h);
        for (var pos : BlockPos.betweenClosed(base.offset(-22, 0, -22), base.offset(22, 0, 22)))
            h.getLevel().setBlock(pos, Blocks.SHORT_GRASS.defaultBlockState(), 2);
        var sparse = biome(h, Biomes.SPARSE_JUNGLE);
        var bases = BetelPalmGroveGenerator.generate(h.getLevel(), base, RandomSource.create(11), config(), pos -> sparse);
        groveChecks(h, bases, 4, 8);
        for (var pos : bases) h.assertTrue(pos.getY() == base.getY(), "Trunks start on soil under short grass");
        h.succeed();
    }

    @GameTest(template = SITE)
    public void riverbankNeedsJungleWaterAndRaisedDryGround(GameTestHelper h) {
        var base = site(h);
        var level = h.getLevel();
        for (var pos : BlockPos.betweenClosed(base.offset(-22, -1, -7), base.offset(22, -1, -3)))
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 2);
        for (var pos : BlockPos.betweenClosed(base.offset(-22, -3, -7), base.offset(22, -2, -3)))
            level.setBlock(pos, Blocks.WATER.defaultBlockState(), 2);
        var river = biome(h, Biomes.RIVER);
        var jungle = biome(h, Biomes.JUNGLE);
        Function<BlockPos, Holder<Biome>> biomes = pos -> pos.getZ() > base.getZ() + 8 ? jungle : river;
        h.assertTrue(BetelPalmGroveGenerator.suitableSite(level, base, Habitat.RIVERBANK, config(), biomes), "Raised bank next to jungle river accepted");
        var bases = BetelPalmGroveGenerator.generate(level, base, RandomSource.create(4096), config(), biomes);
        groveChecks(h, bases, 2, 4);
        for (var pos : bases) h.assertTrue(pos.getY() == base.getY(), "No trunk starts in the river");
        h.succeed();
    }

    @GameTest(template = SITE)
    public void ordinaryRiverDoesNotGeneratePalms(GameTestHelper h) {
        var base = site(h);
        var river = biome(h, Biomes.RIVER);
        h.assertTrue(BetelPalmGroveGenerator.generate(h.getLevel(), base, RandomSource.create(4096), config(), pos -> river).isEmpty(),
                "Rivers without nearby jungle are excluded");
        h.succeed();
    }

    @GameTest(template = SITE)
    public void biomeBoundaryIsCheckedForEveryTrunk(GameTestHelper h) {
        var base = site(h);
        var sparse = biome(h, Biomes.SPARSE_JUNGLE);
        var plains = biome(h, Biomes.PLAINS);
        var bases = BetelPalmGroveGenerator.generate(h.getLevel(), base, RandomSource.create(11), config(),
                pos -> pos.getX() <= base.getX() ? sparse : plains);
        groveChecks(h, bases, 1, 8);
        for (var pos : bases) h.assertTrue(pos.getX() <= base.getX(), "Grove does not spill into plains");
        h.succeed();
    }

    @GameTest(template = SITE)
    public void cliffsCanopiesSandAndWaterRejectSites(GameTestHelper h) {
        var base = site(h);
        var level = h.getLevel();
        var sparse = biome(h, Biomes.SPARSE_JUNGLE);
        Function<BlockPos, Holder<Biome>> biomes = pos -> sparse;
        level.setBlock(base.below(), Blocks.SAND.defaultBlockState(), 2);
        h.assertTrue(!BetelPalmGroveGenerator.suitableSite(level, base, Habitat.SPARSE_JUNGLE, config(), biomes), "Sand rejected");
        level.setBlock(base.below(), Blocks.DIRT.defaultBlockState(), 2);
        level.setBlock(base, Blocks.WATER.defaultBlockState(), 2);
        h.assertTrue(!BetelPalmGroveGenerator.suitableSite(level, base, Habitat.SPARSE_JUNGLE, config(), biomes), "Standing water rejected");
        level.setBlock(base, Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(base.above(12), Blocks.OAK_LEAVES.defaultBlockState(), 2);
        h.assertTrue(!BetelPalmGroveGenerator.suitableSite(level, base, Habitat.SPARSE_JUNGLE, config(), biomes), "Existing canopy rejected");
        level.setBlock(base.above(12), Blocks.AIR.defaultBlockState(), 2);
        level.setBlock(base.east(2).above(4), Blocks.STONE.defaultBlockState(), 2);
        h.assertTrue(!BetelPalmGroveGenerator.suitableSite(level, base, Habitat.SPARSE_JUNGLE, config(), biomes), "Steep local height difference rejected");
        h.succeed();
    }

    @GameTest(template = SITE)
    public void disabledDensityAndConfigMigrationPreservePreferences(GameTestHelper h) throws ReflectiveOperationException {
        var base = site(h);
        var sparse = biome(h, Biomes.SPARSE_JUNGLE);
        var config = config();
        config.betelPalmSpawnChance = 0;
        h.assertTrue(BetelPalmGroveGenerator.generate(h.getLevel(), base, RandomSource.create(11), config, pos -> sparse).isEmpty(), "Zero chance generates nothing");
        config.betelPalmSpawnChance = .23;
        config.betelPalmTreesPerChunk = 0;
        config.configVersion = 10;
        config.betelPalmGenerateInJungle = false;
        config.betelPalmMinHeight = 6;
        config.betelPalmMaxHeight = 11;
        config.maxAddictionValue = 100;
        config.minimumAddictionForWithdrawal = 25;
        var validate = BetelNutConfig.class.getDeclaredMethod("validated");
        validate.setAccessible(true);
        validate.invoke(config);
        h.assertTrue(!config.betelPalmGenerateInJungle && config.betelPalmSpawnChance == .23
                && config.betelPalmTreesPerChunk == 0 && config.betelPalmMinHeight == 6 && config.betelPalmMaxHeight == 11
                && config.minimumAddictionForWithdrawal == 3 && config.maxAddictionValue == 7 && config.legacyMaxAddictionValue == 100, "New config version preserves custom settings and zero attempts");
        h.assertTrue(BetelPalmGroveGenerator.generate(h.getLevel(), base, RandomSource.create(11), config, pos -> sparse).isEmpty(), "Zero attempts generate nothing");
        h.succeed();
    }
}
