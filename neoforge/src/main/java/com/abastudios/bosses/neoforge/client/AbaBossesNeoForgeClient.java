package com.abastudios.bosses.neoforge.client;

import com.abastudios.bosses.client.render.BlackHolePostEffect;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

/**
 * NeoForge-only client event registration.
 */
public final class AbaBossesNeoForgeClient {
    private AbaBossesNeoForgeClient() {
    }

    /**
     * Registers NeoForge client events.
     */
    public static void init(IEventBus modEventBus) {
        NeoForge.EVENT_BUS.addListener(AbaBossesNeoForgeClient::renderLevelStage);
    }

    private static void renderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) {
            return;
        }
        BlackHolePostEffect.process(
                event.getCamera(),
                event.getModelViewMatrix(),
                event.getProjectionMatrix(),
                event.getPartialTick().getGameTimeDeltaPartialTick(false)
        );
    }
}
