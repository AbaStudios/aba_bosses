package com.abastudios.bosses.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places jagged amethyst and obsidian shard clusters on Downfall terrain.
 */
public final class DownfallShardClusterFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockState AMETHYST = Blocks.AMETHYST_BLOCK.defaultBlockState();
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState CRYING_OBSIDIAN = Blocks.CRYING_OBSIDIAN.defaultBlockState();

    public DownfallShardClusterFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos generationOrigin = context.origin();
        BlockPos origin = DownfallFeatureSupport.surfaceOrigin(level, context.origin(), random);

        if (level.isOutsideBuildHeight(origin) || !DownfallFeatureSupport.hasSolidAnchor(level, origin.below(), Direction.DOWN, 2)) {
            return false;
        }

        int shards = 3 + random.nextInt(5);
        boolean placed = false;

        for (int shard = 0; shard < shards; shard++) {
            int height = 5 + random.nextInt(15);
            int baseRadius = 1 + random.nextInt(3);
            double leanX = (random.nextDouble() - 0.5D) * 0.42D;
            double leanZ = (random.nextDouble() - 0.5D) * 0.42D;
            BlockPos center = origin.offset(random.nextInt(9) - 4, random.nextInt(3), random.nextInt(9) - 4);
            placed |= placeShard(level, random, generationOrigin, center, height, baseRadius, leanX, leanZ);
        }

        return placed;
    }

    private static boolean placeShard(WorldGenLevel level, RandomSource random, BlockPos placementOrigin, BlockPos origin, int height, int baseRadius, double leanX, double leanZ) {
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int y = 0; y < height; y++) {
            double taper = 1.0D - (double)y / (double)height;
            double radius = Math.max(0.35D, baseRadius * taper + DownfallFeatureSupport.roughness(random, origin.getX(), origin.getY() + y, origin.getZ()) * 0.35D);
            int centerX = origin.getX() + Mth.floor(leanX * y);
            int centerZ = origin.getZ() + Mth.floor(leanZ * y);
            int bound = Mth.ceil(radius + 1.0D);

            for (int x = -bound; x <= bound; x++) {
                for (int z = -bound; z <= bound; z++) {
                    double distance = Math.abs(x) * 0.95D + Math.abs(z) * 0.7D + random.nextDouble() * 0.18D;
                    if (distance > radius) {
                        continue;
                    }

                    mutable.set(centerX + x, origin.getY() + y, centerZ + z);
                    BlockState state = y < 2 && random.nextBoolean() ? OBSIDIAN : random.nextInt(7) == 0 ? CRYING_OBSIDIAN : AMETHYST;
                    placed |= DownfallFeatureSupport.placeIfAir(level, placementOrigin, mutable, state);
                }
            }
        }

        return placed;
    }
}
