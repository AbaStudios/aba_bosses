package com.abastudios.bosses.fabric;

import com.abastudios.bosses.AbaBosses;
import net.fabricmc.api.ModInitializer;

/**
 * Fabric entrypoint for Aba's Bosses.
 */
public final class AbaBossesFabric implements ModInitializer {
    @Override
    public void onInitialize() {
        AbaBosses.init();
    }
}
