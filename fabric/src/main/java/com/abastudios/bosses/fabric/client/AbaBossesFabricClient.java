package com.abastudios.bosses.fabric.client;

import com.abastudios.bosses.client.AbaBossesClient;
import com.abastudios.bosses.client.render.BlackHolePostEffect;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;

/**
 * Fabric client entrypoint for Aba's Bosses.
 */
public final class AbaBossesFabricClient implements ClientModInitializer {
    @Override
    public void onInitializeClient() {
        WorldRenderEvents.END.register(context -> BlackHolePostEffect.process(
                context.camera(),
                context.positionMatrix(),
                context.projectionMatrix(),
                context.tickCounter().getGameTimeDeltaPartialTick(false)
        ));
        AbaBossesClient.init();

        if (FabricLoader.getInstance().isModLoaded("lodestone")) {
            LightningLaserLodestoneFabric.init();
            LightningLaserTestCommandFabric.init();
        }
    }
}
