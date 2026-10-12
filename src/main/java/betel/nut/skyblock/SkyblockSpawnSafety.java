package betel.nut.skyblock;

import betel.nut.BetelNutMidnightConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.core.Direction.Axis;

/** A bounded search around the saved spawn, without clearing player buildings. */
public final class SkyblockSpawnSafety {
    public static Vec3 findLanding(ServerPlayer player, ServerLevel level, BlockPos preferred) {
        // Load only the nearby spawn chunks, including when the last player fell far away.
        level.getChunkAt(preferred);
        for (int radius = 0; radius <= 16; radius++) {
            for (int dy = 0; dy <= 16; dy++) {
                for (int sign : dy == 0 ? new int[]{1} : new int[]{-1, 1}) {
                    for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                        if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                        BlockPos feet = preferred.offset(x, dy * sign, z);
                        Vec3 landing = landing(player, level, feet);
                        if (landing != null) return landing;
                    }
                }
            }
        }
        if (!BetelNutMidnightConfig.skyblockEmergencyPlatform) return null;
        // A single unmineable foothold avoids both an endless fall and a farmable free-block rescue.
        // Never replace a block or clear headroom, even if the saved spawn has been built over.
        for (int radius = 0; radius <= 4; radius++) {
            for (int x = -radius; x <= radius; x++) for (int z = -radius; z <= radius; z++) {
                if (Math.max(Math.abs(x), Math.abs(z)) != radius) continue;
                BlockPos feet = preferred.offset(x, -1, z);
                if (level.isOutsideBuildHeight(feet.below()) || level.isOutsideBuildHeight(feet.above())
                        || !level.getWorldBorder().isWithinBounds(feet)
                        || !level.getBlockState(feet.below()).isAir()
                        || !clear(player, level, Vec3.atBottomCenterOf(feet))) continue;
                level.setBlock(feet.below(), Blocks.BEDROCK.defaultBlockState(), Block.UPDATE_ALL);
                return Vec3.atBottomCenterOf(feet);
            }
        }
        return null;
    }

    private static Vec3 landing(ServerPlayer player, ServerLevel level, BlockPos feet) {
        if (level.isOutsideBuildHeight(feet.below()) || level.isOutsideBuildHeight(feet.above())
                || !level.getWorldBorder().isWithinBounds(feet)) return null;
        var floor = level.getBlockState(feet.below());
        if (hazard(level, feet.below())) return null;
        var shape = floor.getCollisionShape(level, feet.below());
        if (shape.isEmpty()) return null;
        double top = shape.max(Axis.Y);
        // Include bottom slabs, while rejecting narrow stems and unstable partial footholds.
        boolean supported = shape.toAabbs().stream().anyMatch(box -> box.maxY == top
                && box.minX <= .2 && box.maxX >= .8 && box.minZ <= .2 && box.maxZ >= .8);
        Vec3 landing = new Vec3(feet.getX() + .5, feet.getY() - 1 + top, feet.getZ() + .5);
        return supported && clear(player, level, landing) ? landing : null;
    }

    private static boolean clear(ServerPlayer player, ServerLevel level, Vec3 landing) {
        BlockPos feet = BlockPos.containing(landing);
        return !hazard(level, feet) && !hazard(level, feet.above())
                && level.noCollision(player, new AABB(landing.x - .3, landing.y, landing.z - .3,
                        landing.x + .3, landing.y + 1.8, landing.z + .3));
    }

    private static boolean hazard(ServerLevel level, BlockPos pos) {
        var state = level.getBlockState(pos);
        return !state.getFluidState().isEmpty() || state.is(BlockTags.FIRE) || state.is(Blocks.CACTUS)
                || state.is(Blocks.MAGMA_BLOCK) || state.is(Blocks.CAMPFIRE) || state.is(Blocks.SOUL_CAMPFIRE)
                || state.is(Blocks.SWEET_BERRY_BUSH) || state.is(Blocks.WITHER_ROSE) || state.is(Blocks.POWDER_SNOW)
                || state.is(Blocks.NETHER_PORTAL) || state.is(Blocks.END_PORTAL);
    }

    private SkyblockSpawnSafety() {}
}
