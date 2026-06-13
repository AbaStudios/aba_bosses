package com.abastudios.bosses.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places fractured rib-like lattice spans along void cuts.
 */
public final class LatticeRibFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockState DEEPSLATE = Blocks.DEEPSLATE.defaultBlockState();
    private static final BlockState SCULK = Blocks.SCULK.defaultBlockState();
    private static final BlockState BASALT = Blocks.SMOOTH_BASALT.defaultBlockState();

    public LatticeRibFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos generationOrigin = context.origin();
        BlockPos origin = DownfallFeatureSupport.surfaceOrigin(level, context.origin(), random).above(random.nextInt(8));

        if (level.isOutsideBuildHeight(origin)) {
            return false;
        }

        int length = 12 + random.nextInt(24);
        double yaw = random.nextDouble() * Math.PI * 2.0D;
        double rise = (random.nextDouble() - 0.35D) * 0.55D;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int i = 0; i < length; i++) {
            if (random.nextDouble() < 0.18D) {
                continue;
            }

            int centerX = origin.getX() + Mth.floor(Math.cos(yaw) * i);
            int centerY = origin.getY() + Mth.floor(rise * i + Math.sin(i * 0.7D) * 2.0D);
            int centerZ = origin.getZ() + Mth.floor(Math.sin(yaw) * i);
            int radius = i % 5 == 0 ? 2 : 1;

            for (int x = -radius; x <= radius; x++) {
                for (int y = -radius; y <= radius; y++) {
                    for (int z = -radius; z <= radius; z++) {
                        if (Math.abs(x) + Math.abs(y) + Math.abs(z) > radius + 1 || random.nextDouble() < 0.2D) {
                            continue;
                        }

                        mutable.set(centerX + x, centerY + y, centerZ + z);
                        BlockState state = random.nextInt(13) == 0 ? SCULK : random.nextBoolean() ? DEEPSLATE : BASALT;
                        placed |= DownfallFeatureSupport.placeIfAir(level, generationOrigin, mutable, state);
                    }
                }
            }
        }

        return placed;
    }
}
