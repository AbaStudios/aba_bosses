package com.abastudios.bosses.client.tooltip;

import com.abastudios.bosses.registry.AbaBossesItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipTheme;

import java.util.List;

/**
 * Simply Tooltips provider for the Warpglass Wand.
 */
public final class WarpglassWandTooltipProvider implements TooltipProvider {
    @Override
    public boolean supports(ItemStack stack) {
        return stack.is(AbaBossesItems.WARPGLASS_WAND.get());
    }

    @Override
    public ModernTooltipModel build(ItemStack stack, List<Component> rawLines, boolean altDown) {
        String title = rawLines.isEmpty() ? stack.getHoverName().getString() : rawLines.getFirst().getString();

        return new ModernTooltipModel(
                title,
                List.of("WAND", "WARPGLASS"),
                TooltipBorderStyle.DEFAULT,
                List.of(
                        ModernTooltipModel.SECTION_MARKER + Component.translatable("tooltip.aba_bosses.warpglass_wand.section").getString(),
                        Component.translatable("tooltip.aba_bosses.warpglass_wand.warp").getString(),
                        Component.translatable("tooltip.aba_bosses.warpglass_wand.third_eye").getString()
                ),
                List.of(),
                List.of(),
                TooltipTheme.defaultTheme(),
                null,
                null,
                "deepdark",
                null
        );
    }
}
