package com.abastudios.bosses.client;

import com.abastudios.bosses.client.render.BlackHoleRenderer;
import com.abastudios.bosses.client.test.ClientTestScriptRunner;
import com.abastudios.bosses.client.tooltip.AbaBossesTooltipIntegration;
import com.abastudios.bosses.client.vfx.LightningLaserVfx;
import com.abastudios.bosses.registry.AbaBossesEntityTypes;
import dev.architectury.platform.Platform;
import dev.architectury.registry.client.level.entity.EntityRendererRegistry;

/**
 * Common client entrypoint for Aba's Bosses.
 */
public final class AbaBossesClient {
    private AbaBossesClient() {
    }

    /**
     * Registers client-only integrations.
     */
    public static void init() {
        EntityRendererRegistry.register(AbaBossesEntityTypes.BLACK_HOLE, BlackHoleRenderer::new);
        LightningLaserVfx.init();
        WarpglassThirdEyeController.register();
        ClientTestScriptRunner.init();

        if (Platform.isModLoaded("simplytooltips")) {
            AbaBossesTooltipIntegration.register();
        }
    }
}
