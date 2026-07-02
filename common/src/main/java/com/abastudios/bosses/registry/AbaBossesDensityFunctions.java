package com.abastudios.bosses.registry;

import com.abastudios.bosses.AbaBosses;
import com.abastudios.bosses.worldgen.density.HorizontalDistanceDensityFunction;
import com.mojang.serialization.MapCodec;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.DensityFunction;

/**
 * Registers density function types used by Aba's Bosses world generation.
 */
public final class AbaBossesDensityFunctions {
    public static final DeferredRegister<MapCodec<? extends DensityFunction>> DENSITY_FUNCTION_TYPES =
            DeferredRegister.create(AbaBosses.MOD_ID, Registries.DENSITY_FUNCTION_TYPE);

    public static final RegistrySupplier<MapCodec<? extends DensityFunction>> HORIZONTAL_DISTANCE = DENSITY_FUNCTION_TYPES.register(
            "horizontal_distance",
            () -> HorizontalDistanceDensityFunction.MAP_CODEC
    );

    private AbaBossesDensityFunctions() {
    }

    /**
     * Registers all custom density function types.
     */
    public static void register() {
        DENSITY_FUNCTION_TYPES.register();
    }
}
