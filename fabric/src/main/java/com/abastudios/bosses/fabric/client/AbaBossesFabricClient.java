package com.abastudios.bosses.fabric.client;

import com.abastudios.bosses.client.AbaBossesClient;
import net.fabricmc.api.ClientModInitializer;

/**
 * Fabric client entrypoint for Aba's Bosses.
 */
public final class AbaBossesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        AbaBossesClient.init();
    }
}
