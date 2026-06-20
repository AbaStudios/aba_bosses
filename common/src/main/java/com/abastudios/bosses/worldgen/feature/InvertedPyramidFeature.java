package com.abastudios.bosses.worldgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.FeaturePlaceContext;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Places weathered inverted pyramids hanging from terrain undersides.
 */
public final class InvertedPyramidFeature extends Feature<NoneFeatureConfiguration> {
    private static final BlockState END_STONE = Blocks.END_STONE.defaultBlockState();
    private static final BlockState BLACKSTONE = Blocks.BLACKSTONE.defaultBlockState();
    private static final BlockState OBSIDIAN = Blocks.OBSIDIAN.defaultBlockState();

    public InvertedPyramidFeature(Codec<NoneFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(FeaturePlaceContext<NoneFeatureConfiguration> context) {
        WorldGenLevel level = context.level();
        RandomSource random = context.random();
        BlockPos generationOrigin = context.origin();
        BlockPos surface = DownfallFeatureSupport.surfaceOrigin(level, context.origin(), random);
        BlockPos origin = surface.below(3 + random.nextInt(8));

        if (level.isOutsideBuildHeight(origin) || !DownfallFeatureSupport.hasSolidAnchor(level, surface.below(), Direction.UP, 3)) {
            return false;
        }

        int radius = 4 + random.nextInt(8);
        int height = 8 + random.nextInt(16);
        BlockPos.MutableBlockPos mutable = new BlockPos.MutableBlockPos();
        boolean placed = false;

        for (int y = 0; y < height; y++) {
            int layerRadius = Math.max(0, radius - y / 2);

            for (int x = -layerRadius; x <= layerRadius; x++) {
                for (int z = -layerRadius; z <= layerRadius; z++) {
                    int taxicab = Math.abs(x) + Math.abs(z);
                    if (taxicab > layerRadius + random.nextInt(2) || random.nextDouble() < 0.11D + y * 0.008D) {
                        continue;
                    }

                    mutable.set(origin.getX() + x, origin.getY() - y, origin.getZ() + z);
                    BlockState state = taxicab > layerRadius - 1 ? OBSIDIAN : random.nextInt(5) == 0 ? BLACKSTONE : END_STONE;
                    placed |= DownfallFeatureSupport.placeIfAir(level, generationOrigin, mutable, state);
                }
            }
        }

        return placed;
    }
}
