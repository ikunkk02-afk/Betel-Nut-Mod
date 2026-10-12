package betel.nut.mixin;

import betel.nut.skyblock.BetelSkyblockManager;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.random.WeightedRandomList;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.level.NaturalSpawner;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.biome.Biomes;
import net.minecraft.world.level.biome.MobSpawnSettings;
import net.minecraft.world.level.chunk.ChunkGenerator;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(NaturalSpawner.class)
public abstract class NaturalSpawnerMixin {
    /** Keep the saved void biome; supply ordinary overworld monsters only in enabled skyblocks. */
    @Inject(method = "mobsAt", at = @At("RETURN"), cancellable = true)
    private static void betel$skyblockMonsters(ServerLevel level, StructureManager structures,
            ChunkGenerator generator, MobCategory category, BlockPos pos, Holder<Biome> biome,
            CallbackInfoReturnable<WeightedRandomList<MobSpawnSettings.SpawnerData>> cir) {
        if (category != MobCategory.MONSTER || !cir.getReturnValue().isEmpty()
                || (!BetelSkyblockManager.isEnabled(level) && !BetelSkyblockManager.isOneBlockEnabled(level))
                || !(biome == null ? level.getBiome(pos) : biome).is(Biomes.THE_VOID)) return;
        cir.setReturnValue(level.registryAccess().registryOrThrow(Registries.BIOME)
                .getOrThrow(Biomes.PLAINS).getMobSettings().getMobs(category));
    }
}
