package com.abastudios.bosses.registry;

import com.abastudios.bosses.AbaBosses;
import com.abastudios.bosses.worldgen.feature.BrokenArchFeature;
import com.abastudios.bosses.worldgen.feature.DownfallShardClusterFeature;
import com.abastudios.bosses.worldgen.feature.InvertedPyramidFeature;
import com.abastudios.bosses.worldgen.feature.LatticeRibFeature;
import com.abastudios.bosses.worldgen.feature.SuspendedRingFeature;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.level.levelgen.feature.Feature;
import net.minecraft.world.level.levelgen.feature.configurations.NoneFeatureConfiguration;

/**
 * Registers common world generation feature types for Aba's Bosses.
 */
public final class AbaBossesFeatures {
    public static final DeferredRegister<Feature<?>> FEATURES = DeferredRegister.create(AbaBosses.MOD_ID, Registries.FEATURE);

    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> DOWNFALL_SHARD_CLUSTER = FEATURES.register(
            "downfall_shard_cluster",
            () -> new DownfallShardClusterFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> BROKEN_ARCH = FEATURES.register(
            "broken_arch",
            () -> new BrokenArchFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> SUSPENDED_RING = FEATURES.register(
            "suspended_ring",
            () -> new SuspendedRingFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> LATTICE_RIB = FEATURES.register(
            "lattice_rib",
            () -> new LatticeRibFeature(NoneFeatureConfiguration.CODEC)
    );
    public static final RegistrySupplier<Feature<NoneFeatureConfiguration>> INVERTED_PYRAMID = FEATURES.register(
            "inverted_pyramid",
            () -> new InvertedPyramidFeature(NoneFeatureConfiguration.CODEC)
    );

    private AbaBossesFeatures() {
    }

    /**
     * Registers feature types on the active loader.
     */
    public static void register() {
        FEATURES.register();
    }
}
