package com.abastudios.bosses.client.tooltip;

import net.sweenus.simplytooltips.api.TooltipProviderRegistry;

/**
 * Client bridge into Simply Tooltips.
 */
public final class AbaBossesTooltipIntegration {
    private AbaBossesTooltipIntegration() {
    }

    /**
     * Registers Aba's Bosses tooltip providers.
     */
    public static void register() {
        TooltipProviderRegistry.register(new DownfallShardTooltipProvider(), 100);
        TooltipProviderRegistry.register(new WarpglassWandTooltipProvider(), 100);
    }
}
