package betel.nut.worldgen;

import betel.nut.BetelNutConfig;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.WorldGenRegion;
import net.minecraft.tags.FluidTags;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

/** Finds a clearing before placing a bounded, irregular group of palms. Sapling cultivation is independent. */
public final class BetelPalmGroveGenerator {
    public enum Habitat {
        SPARSE_JUNGLE(4, 8, 1.0), JUNGLE(1, 3, .6), BAMBOO_JUNGLE(1, 3, .4),
        RIVERBANK(2, 4, .8), LEGACY_OPTIONAL(1, 2, .35);

        public final int minTrees;
        public final int maxTrees;
        public final double chanceMultiplier;

        Habitat(int minTrees, int maxTrees, double chanceMultiplier) {
            this.minTrees = minTrees;
            this.maxTrees = maxTrees;
            this.chanceMultiplier = chanceMultiplier;
        }
    }

    public static Habitat habitatFor(ResourceKey<Biome> biome, BetelNutConfig config) {
        if (config.betelPalmGenerateInJungle) {
            if (biome.equals(Biomes.SPARSE_JUNGLE) && config.betelPalmGenerateInSparseJungle) return Habitat.SPARSE_JUNGLE;
            if (biome.equals(Biomes.JUNGLE)) return Habitat.JUNGLE;
            if (biome.equals(Biomes.BAMBOO_JUNGLE) && config.betelPalmGenerateInBambooJungle) return Habitat.BAMBOO_JUNGLE;
            if (biome.equals(Biomes.RIVER) && config.betelPalmGenerateNearJungleRivers) return Habitat.RIVERBANK;
        }
        // Retain explicitly enabled legacy biome options; these remain disabled by default.
        if (biome.equals(Biomes.BEACH) && config.betelPalmGenerateInBeach) return Habitat.LEGACY_OPTIONAL;
        if (biome.equals(Biomes.MANGROVE_SWAMP) && config.betelPalmGenerateInMangroveSwamp) return Habitat.LEGACY_OPTIONAL;
        return null;
    }

    public static List<BlockPos> generate(WorldGenLevel level, BlockPos origin, RandomSource random, BetelNutConfig config) {
        // Fuzzy biome lookup samples adjacent quart cells. Use the seeded biome source directly so
        // a point near the decoration boundary cannot request an unavailable neighboring chunk.
        var serverLevel = level.getLevel();
        var biomes = serverLevel.getBiomeManager().withDifferentSource(serverLevel::getUncachedNoiseBiome);
        return generate(level, origin, random, config, biomes::getBiome);
    }

    /** Explicit biome lookup allows controlled terrain tests without changing registered biomes or global config. */
    public static List<BlockPos> generate(WorldGenLevel level, BlockPos origin, RandomSource random,
            BetelNutConfig config, Function<BlockPos, Holder<Biome>> biomes) {
        List<BlockPos> placed = new ArrayList<>();
        if (!config.enableBetelPalmWorldGeneration || !level.getLevel().dimension().equals(Level.OVERWORLD)
                || config.betelPalmTreesPerChunk <= 0 || config.betelPalmSpawnChance <= 0 || !canRead(level, origin)) return placed;
        Habitat habitat = biomes.apply(origin).unwrapKey().map(key -> habitatFor(key, config)).orElse(null);
        if (habitat == null) return placed;
        for (int grove = 0; grove < Math.min(8, config.betelPalmTreesPerChunk); grove++) {
            if (random.nextDouble() >= config.betelPalmSpawnChance * habitat.chanceMultiplier) continue;
            if (habitat == Habitat.RIVERBANK && !nearJungle(level, origin, config, biomes)) continue;
            BlockPos center = null;
            // An origin over river water may select dry ground nearby; no underwater trees are placed.
            for (int search = 0; search < 24; search++) {
                int dx = search == 0 ? 0 : random.nextInt(17) - 8;
                int dz = search == 0 ? 0 : random.nextInt(17) - 8;
                BlockPos candidate = surface(level, origin.offset(dx, 0, dz));
                if (candidate != null && suitableSite(level, candidate, habitat, config, biomes)) {
                    center = candidate;
                    break;
                }
            }
            if (center == null) continue;
            int target = habitat.minTrees + random.nextInt(habitat.maxTrees - habitat.minTrees + 1);
            int count = 0;
            for (int attempt = 0; attempt < target * 12 && count < target; attempt++) {
                BlockPos candidate = attempt == 0 ? center : surface(level, center.offset(random.nextInt(21) - 10, 0, random.nextInt(21) - 10));
                if (candidate == null || placed.stream().anyMatch(other -> horizontalDistanceSquared(candidate, other) < 25)
                        || !suitableSite(level, candidate, habitat, config, biomes)) continue;
                if (BetelPalmTreeGenerator.generate(level, candidate, random, config.betelPalmMinHeight, config.betelPalmMaxHeight)) {
                    placed.add(candidate.immutable());
                    count++;
                }
            }
        }
        return placed;
    }

    public static boolean suitableSite(WorldGenLevel level, BlockPos pos, Habitat habitat, BetelNutConfig config,
            Function<BlockPos, Holder<Biome>> biomes) {
        if (!canRead(level, pos) || !BetelPalmTreeGenerator.canWriteAt(level, pos)
                || !BetelPalmTreeGenerator.isValidGround(level.getBlockState(pos.below()))
                || !level.getFluidState(pos).isEmpty()) return false;
        var holder = biomes.apply(pos);
        var key = holder.unwrapKey().orElse(null);
        if (key == null) return false;
        if (habitat == Habitat.RIVERBANK) {
            // River's own temperature is lower than jungle's. Nearby jungle supplies the tropical context.
            if (!(key.equals(Biomes.RIVER) || enabledJungle(key, config))
                    || !nearJungle(level, pos, config, biomes) || !raisedRiverbank(level, pos, biomes)) return false;
        } else if (habitatFor(key, config) != habitat || holder.value().getBaseTemperature() < .8F
                || !holder.value().hasPrecipitation()) return false;
        // Height maps work during terrain decoration before skylight has been calculated.
        if (level.getHeight(surfaceMap(level), pos.getX(), pos.getZ()) > pos.getY() + 2) return false;
        for (int dx = -2; dx <= 2; dx += 2) for (int dz = -2; dz <= 2; dz += 2) {
            BlockPos neighbor = surface(level, pos.offset(dx, 0, dz));
            if (neighbor == null || Math.abs(neighbor.getY() - pos.getY()) > 2) return false;
        }
        return true;
    }

    private static boolean enabledJungle(ResourceKey<Biome> biome, BetelNutConfig config) {
        Habitat habitat = habitatFor(biome, config);
        return habitat == Habitat.SPARSE_JUNGLE || habitat == Habitat.JUNGLE || habitat == Habitat.BAMBOO_JUNGLE;
    }

    private static boolean nearJungle(WorldGenLevel level, BlockPos pos, BetelNutConfig config,
            Function<BlockPos, Holder<Biome>> biomes) {
        for (int dx = -24; dx <= 24; dx += 8) for (int dz = -24; dz <= 24; dz += 8) {
            if (dx * dx + dz * dz > 24 * 24) continue;
            BlockPos sample = pos.offset(dx, 0, dz);
            if (level.getLevel().getWorldBorder().isWithinBounds(sample)
                    && biomes.apply(sample).unwrapKey().map(key -> enabledJungle(key, config)).orElse(false)) return true;
        }
        return false;
    }

    private static boolean raisedRiverbank(WorldGenLevel level, BlockPos pos, Function<BlockPos, Holder<Biome>> biomes) {
        for (int dx = -8; dx <= 8; dx += 2) for (int dz = -8; dz <= 8; dz += 2) {
            if (dx * dx + dz * dz > 8 * 8) continue;
            BlockPos surface = surface(level, pos.offset(dx, 0, dz));
            if (surface == null) continue;
            int rise = pos.getY() - surface.getY();
            if (rise >= 1 && rise <= 4 && level.getFluidState(surface.below()).is(FluidTags.WATER)
                    && biomes.apply(surface).is(Biomes.RIVER)) return true;
        }
        return false;
    }

    private static BlockPos surface(WorldGenLevel level, BlockPos horizontal) {
        if (!canRead(level, horizontal)) return null;
        BlockPos pos = level.getHeightmapPos(surfaceMap(level), horizontal);
        // WORLD_SURFACE includes grass and ferns. Look through short replaceable plants
        // so an otherwise open jungle clearing is not mistaken for an invalid soil block.
        for (int depth = 0; depth < 2; depth++) {
            var below = level.getBlockState(pos.below());
            if (BetelPalmTreeGenerator.isValidGround(below) || !below.getFluidState().isEmpty()
                    || !below.canBeReplaced()) break;
            pos = pos.below();
        }
        return pos;
    }

    private static Heightmap.Types surfaceMap(WorldGenLevel level) {
        return level instanceof Level ? Heightmap.Types.WORLD_SURFACE : Heightmap.Types.WORLD_SURFACE_WG;
    }

    private static boolean canRead(WorldGenLevel level, BlockPos pos) {
        // FEATURES only guarantees terrain data in the adjacent chunk ring, even when a more
        // distant chunk happens to be present in the region cache with a lower required status.
        if (level instanceof WorldGenRegion region
                && (Math.abs((pos.getX() >> 4) - region.getCenter().x) > 1
                    || Math.abs((pos.getZ() >> 4) - region.getCenter().z) > 1)) return false;
        return level.hasChunk(pos.getX() >> 4, pos.getZ() >> 4)
                && level.getLevel().getWorldBorder().isWithinBounds(pos);
    }

    private static int horizontalDistanceSquared(BlockPos first, BlockPos second) {
        int dx = first.getX() - second.getX(), dz = first.getZ() - second.getZ();
        return dx * dx + dz * dz;
    }

    private BetelPalmGroveGenerator() { }
}
