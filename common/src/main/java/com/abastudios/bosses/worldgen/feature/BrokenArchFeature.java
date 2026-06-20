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
 * Places partial basalt-blackstone arches with intentionally fractured silhouettes.
 */
public final class BrokenArchFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState BASALT = Blocks.BASALT.defaultBlockState();
    private static final BlockState CRYING_OBSIDIAN = Blocks.CRYING_OBSIDIAN.defaultBlockState();

    public BrokenArchFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos generationOrigin = context.origin();
        BlockPos origin = DownfallFeatureSupport.surfaceOrigin(level, context.origin(), random);

        if (level.isOutsideBuildHeight(origin) || !DownfallFeatureSupport.hasSolidAnchor(level, origin.below(), Direction.DOWN, 3)) {
            return false;
        }

        int radius = 6 + random.nextInt(8);
        int thickness = 2 + random.nextInt(2);
        double yaw = random.nextDouble() * Math.PI;
        double missingStart = 0.15D + random.nextDouble() * 0.28D;
        double missingEnd = 0.68D + random.nextDouble() * 0.22D;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int step = 0; step <= 44; step++) {
            double t = (double)step / 44.0D;
            if (t > missingStart && t < missingEnd && random.nextDouble() < 0.72D) {
                continue;
            }

            double angle = Math.PI * t;
            int archX = Mth.floor(Math.cos(yaw) * Math.cos(angle) * radius);
            int archZ = Mth.floor(Math.sin(yaw) * Math.cos(angle) * radius);
            int archY = Mth.floor(Math.sin(angle) * (radius * 0.82D)) + 1;

            for (int x = -thickness; x <= thickness; x++) {
                for (int z = -thickness; z <= thickness; z++) {
                    for (int y = -thickness; y <= thickness; y++) {
                        if (Math.abs(x) + Math.abs(z) + Math.abs(y) > thickness + random.nextInt(2)) {
                            continue;
                        }

                        mutable.set(origin.getX() + archX + x, origin.getY() + archY + y, origin.getZ() + archZ + z);
                        BlockState state = random.nextInt(9) == 0 ? CRYING_OBSIDIAN : random.nextBoolean() ? BLACKSTONE : BASALT;
                        placed |= DownfallFeatureSupport.placeIfAir(level, generationOrigin, mutable, state);
                    }
                }
            }
        }

        return placed;
    }
}
