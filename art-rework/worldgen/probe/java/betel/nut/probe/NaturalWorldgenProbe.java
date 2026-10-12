package betel.nut.probe;

import betel.nut.BetelNutConfig;
import betel.nut.BetelNutMod;
import betel.nut.block.ModBlocks;
import betel.nut.worldgen.BetelPalmGroveGenerator;
import com.google.gson.GsonBuilder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.levelgen.Heightmap;

public class NaturalWorldgenProbe implements ModInitializer {
    private boolean started;
    private int ticks;
    @Override public void onInitialize() {
        ServerTickEvents.END_SERVER_TICK.register(server -> {
            if (!started && ++ticks == 10) {
                started = true;
                try { inspect(server); }
                catch (Exception e) {
                    BetelNutMod.LOGGER.error("NATURAL_WORLDGEN_PROBE_FAILED", e);
                    throw new RuntimeException(e);
                }
                server.halt(false);
            }
        });
    }

    private static void inspect(MinecraftServer server) throws Exception {
        var level = server.overworld();
        level.getGameRules().getRule(GameRules.RULE_RANDOMTICKING).set(0, server);
        List<Map<String, Object>> samples = new ArrayList<>();
        for (var biome : List.of(Biomes.SPARSE_JUNGLE, Biomes.JUNGLE, Biomes.BAMBOO_JUNGLE)) {
            var found = level.findClosestBiome3d(holder -> holder.is(biome), new BlockPos(0, 64, 0), 6400, 32, 64);
            if (found == null) throw new IllegalStateException("Missing test biome " + biome.location());
            BetelNutMod.LOGGER.info("NATURAL_PROBE_LOCATED {} {}", biome.location(), found.getFirst());
            samples.add(sample(level, biome, found.getFirst(), biome.equals(Biomes.BAMBOO_JUNGLE) ? 12 : 6));
        }
        var jungleSample = samples.getFirst();
        @SuppressWarnings("unchecked") var center = (List<Integer>) jungleSample.get("center");
        var river = level.findClosestBiome3d(holder -> holder.is(Biomes.RIVER), new BlockPos(center.get(0),64,center.get(2)), 512, 8, 32);
        if (river == null) throw new IllegalStateException("No river around located jungle");
        samples.add(sample(level, Biomes.RIVER, river.getFirst(), 6));
        var report = new LinkedHashMap<String, Object>();
        report.put("seed", level.getSeed());
        report.put("spawnChance", BetelNutConfig.get().betelPalmSpawnChance);
        report.put("groveAttemptsPerChunk", BetelNutConfig.get().betelPalmTreesPerChunk);
        report.put("naturalTerrain", true);
        report.put("samples", samples);
        Files.writeString(Path.of("../natural-world-report.json"), new GsonBuilder().setPrettyPrinting().create().toJson(report));
        BetelNutMod.LOGGER.info("NATURAL_WORLDGEN_PROBE_PASSED samples={} trees={}", samples.size(),
                samples.stream().mapToInt(s -> (Integer)s.get("trees")).sum());
    }

    private static Map<String, Object> sample(ServerLevel level, ResourceKey<Biome> wanted, BlockPos center, int radius) {
        List<List<Integer>> roots = new ArrayList<>();
        Map<String, Integer> actualBiomes = new LinkedHashMap<>();
        int cx = center.getX() >> 4, cz = center.getZ() >> 4;
        for (int dx = -radius; dx <= radius; dx++) for (int dz = -radius; dz <= radius; dz++) {
            var chunk = level.getChunk(cx + dx, cz + dz);
            for (int sectionIndex = 0; sectionIndex < chunk.getSections().length; sectionIndex++) {
                var section = chunk.getSections()[sectionIndex];
                if (!section.maybeHas(state -> state.is(ModBlocks.BETEL_PALM_LOG))) continue;
                int bottom = level.getMinBuildHeight() + sectionIndex * 16;
                for (int x = 0; x < 16; x++) for (int y = 0; y < 16; y++) for (int z = 0; z < 16; z++) {
                    if (!section.getBlockState(x,y,z).is(ModBlocks.BETEL_PALM_LOG)) continue;
                    var pos = new BlockPos((cx+dx)*16+x, bottom+y, (cz+dz)*16+z);
                    if (level.getBlockState(pos.below()).is(ModBlocks.BETEL_PALM_LOG)) continue;
                    var key = level.getBiome(pos).unwrapKey().orElseThrow();
                    if (BetelPalmGroveGenerator.habitatFor(key, BetelNutConfig.get()) == null)
                        throw new IllegalStateException("Palm escaped allowed biome at " + pos + " " + key.location());
                    if (!level.getFluidState(pos).isEmpty()) throw new IllegalStateException("Underwater palm at " + pos);
                    roots.add(List.of(pos.getX(),pos.getY(),pos.getZ()));
                    actualBiomes.merge(key.location().toString(),1,Integer::sum);
                }
            }
        }
        var generation = level.registryAccess().registryOrThrow(Registries.BIOME).getHolderOrThrow(wanted).value().getGenerationSettings();
        boolean injected = generation.features().stream().flatMap(set -> set.stream()).anyMatch(holder ->
                holder.unwrapKey().map(key -> key.location().equals(BetelNutMod.id("betel_palm_tree"))).orElse(false));
        if (!injected) throw new IllegalStateException("Feature not injected into " + wanted.location());
        var result = new LinkedHashMap<String,Object>();
        result.put("biome", wanted.location().toString());
        result.put("center", List.of(center.getX(),center.getY(),center.getZ()));
        result.put("chunksScanned", (radius*2+1)*(radius*2+1));
        result.put("featureInjected", true);
        result.put("trees", roots.size());
        result.put("actualRootBiomes", actualBiomes);
        result.put("roots", roots);
        BetelNutMod.LOGGER.info("NATURAL_PROBE_SAMPLE {} trees={} biomes={}", wanted.location(), roots.size(), actualBiomes);
        return result;
    }
}
