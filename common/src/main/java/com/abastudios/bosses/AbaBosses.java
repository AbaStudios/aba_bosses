package com.abastudios.bosses;

import com.abastudios.bosses.config.AbaBossesConfig;
import com.abastudios.bosses.registry.AbaBossesEntityTypes;
import com.abastudios.bosses.registry.AbaBossesFeatures;
import com.abastudios.bosses.registry.AbaBossesGameRules;
import com.abastudios.bosses.registry.AbaBossesItems;
import com.abastudios.bosses.registry.AbaBossesTabs;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Common entrypoint for Aba's Bosses.
 */
public final class AbaBosses {
    public static final String MOD_ID = "aba_bosses";
    public static final String MOD_NAME = "Aba's Bosses";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    private AbaBosses() {
    }

    /**
     * Registers common content and services.
     */
    public static void init() {
        AbaBossesConfig.init();
        AbaBossesGameRules.register();
        AbaBossesEntityTypes.register();
        AbaBossesFeatures.register();
        AbaBossesTabs.register();
        AbaBossesItems.register();
    }

    /**
     * Creates a resource location in the Aba's Bosses namespace.
     */
    public static ResourceLocation location(String path) {
        return ResourceLocation.fromNamespaceAndPath(MOD_ID, path);
    }
}
