package com.abastudios.bosses.client;

import com.abastudios.bosses.client.tooltip.AbaBossesTooltipIntegration;
import dev.architectury.platform.Platform;

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
        if (Platform.isModLoaded("simplytooltips")) {
            AbaBossesTooltipIntegration.register();
        }
    }
}
