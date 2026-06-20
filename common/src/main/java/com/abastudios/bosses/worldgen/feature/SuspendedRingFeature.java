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
 * Places imperfect suspended rings above island shelves.
 */
public final class SuspendedRingFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();
    private static final BlockState CRYING_OBSIDIAN = Blocks.CRYING_OBSIDIAN.defaultBlockState();
    private static final BlockState END_STONE_BRICKS = Blocks.END_STONE_BRICKS.defaultBlockState();

    public SuspendedRingFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos generationOrigin = context.origin();
        BlockPos origin = DownfallFeatureSupport.surfaceOrigin(level, context.origin(), random).above(8 + random.nextInt(18));

        if (level.isOutsideBuildHeight(origin) || !DownfallFeatureSupport.hasSolidAnchor(level, origin.below(12), Direction.DOWN, 16)) {
            return false;
        }

        int radius = 5 + random.nextInt(10);
        int thickness = 1 + random.nextInt(2);
        double tilt = (random.nextDouble() - 0.5D) * 0.55D;
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int step = 0; step < 96; step++) {
            if (random.nextDouble() < 0.16D) {
                continue;
            }

            double angle = Math.PI * 2.0D * step / 96.0D;
            double roughRadius = radius + Math.sin(angle * 5.0D + random.nextDouble()) * 0.9D;
            int centerX = Mth.floor(Math.cos(angle) * roughRadius);
            int centerZ = Mth.floor(Math.sin(angle) * roughRadius);
            int centerY = Mth.floor(Math.sin(angle * 2.0D) * tilt * radius);

            for (int x = -thickness; x <= thickness; x++) {
                for (int y = -thickness; y <= thickness; y++) {
                    for (int z = -thickness; z <= thickness; z++) {
                        if (Math.abs(x) + Math.abs(y) + Math.abs(z) > thickness + 1) {
                            continue;
                        }

                        mutable.set(origin.getX() + centerX + x, origin.getY() + centerY + y, origin.getZ() + centerZ + z);
                        BlockState state = random.nextInt(11) == 0 ? CRYING_OBSIDIAN : random.nextInt(5) == 0 ? END_STONE_BRICKS : OBSIDIAN;
                        placed |= DownfallFeatureSupport.placeIfAir(level, generationOrigin, mutable, state);
                    }
                }
            }
        }

        return placed;
    }
}
