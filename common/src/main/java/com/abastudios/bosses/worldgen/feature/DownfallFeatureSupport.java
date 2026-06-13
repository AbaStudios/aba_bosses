package com.abastudios.bosses.worldgen.feature;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.SectionPos;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.Heightmap;

final class DownfallFeatureSupport {
    private DownfallFeatureSupport() {
    }

    static BlockPos surfaceOrigin(WorldGenLevel level, BlockPos origin, RandomSource random) {
        int x = origin.getX() + random.nextInt(16) - 8;
        int z = origin.getZ() + random.nextInt(16) - 8;
        int y = level.getHeight(Heightmap.Types.WORLD_SURFACE_WG, x, z);
        return new BlockPos(x, y, z);
    }

    static boolean hasSolidAnchor(WorldGenLevel level, BlockPos origin, int radius) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();

        for (Direction direction : Direction.values()) {
            for (int distance = 1; distance <= radius; distance++) {
                mutable.setWithOffset(
                        origin,
                        direction.getStepX() * distance,
                        direction.getStepY() * distance,
                        direction.getStepZ() * distance
                );
                if (level.getBlockState(mutable).isSolidRender(level, mutable)) {
                    return true;
                }
            }
        }

        return false;
    }

    static boolean canReplace(WorldGenLevel level, BlockPos pos) {
        return level.isOutsideBuildHeight(pos) || level.getBlockState(pos).isAir();
    }

    static boolean placeIfAir(WorldGenLevel level, BlockPos origin, BlockPos.MutableBlockPos pos, BlockState state) {
        if (level.isOutsideBuildHeight(pos) || !isNearOriginChunk(origin, pos, 0)) {
            return false;
        }

        if (!level.getBlockState(pos).isAir()) {
            return false;
        }

        level.setBlock(pos, state, 3);
        return true;
    }

    private static boolean isNearOriginChunk(BlockPos origin, BlockPos pos, int chunkRadius) {
        int originChunkX = SectionPos.blockToSectionCoord(origin.getX());
        int originChunkZ = SectionPos.blockToSectionCoord(origin.getZ());
        int targetChunkX = SectionPos.blockToSectionCoord(pos.getX());
        int targetChunkZ = SectionPos.blockToSectionCoord(pos.getZ());

        return Math.abs(targetChunkX - originChunkX) <= chunkRadius && Math.abs(targetChunkZ - originChunkZ) <= chunkRadius;
    }

    static double roughness(RandomSource random, int x, int y, int z) {
        long seed = (long)x * 341873128712L ^ (long)y * 132897987541L ^ (long)z * 42317861L ^ random.nextLong();
        seed ^= seed >>> 33;
        seed *= 0xff51afd7ed558ccdL;
        seed ^= seed >>> 33;
        return ((seed & 1023L) / 1023.0D) - 0.5D;
    }
}
