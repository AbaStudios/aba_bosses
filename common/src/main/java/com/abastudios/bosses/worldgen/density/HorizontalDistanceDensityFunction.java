package com.abastudios.bosses.worldgen.density;

import com.mojang.serialization.MapCodec;
import net.minecraft.util.KeyDispatchDataCodec;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Returns the horizontal distance from the world origin in blocks.
 */
public final class HorizontalDistanceDensityFunction implements DensityFunction.SimpleFunction {
    public static final HorizontalDistanceDensityFunction INSTANCE = new HorizontalDistanceDensityFunction();
    public static final MapCodec<HorizontalDistanceDensityFunction> MAP_CODEC = MapCodec.unit(INSTANCE);
    public static final KeyDispatchDataCodec<HorizontalDistanceDensityFunction> CODEC = KeyDispatchDataCodec.of(MAP_CODEC);

    private HorizontalDistanceDensityFunction() {
    }

    @Override
    public double compute(FunctionContext context) {
        return Math.hypot(context.blockX(), context.blockZ());
    }

    @Override
    public double minValue() {
        return 0.0;
    }

    @Override
    public double maxValue() {
        return Math.hypot(30_000_000.0, 30_000_000.0);
    }

    @Override
    public KeyDispatchDataCodec<? extends DensityFunction> codec() {
        return CODEC;
    }
}
