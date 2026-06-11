package com.abastudios.bosses.client.tooltip;

import com.abastudios.bosses.config.AbaBossesConfig;
import com.abastudios.bosses.registry.AbaBossesItems;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.sweenus.simplytooltips.api.ModernTooltipModel;
import net.sweenus.simplytooltips.api.TooltipBorderStyle;
import net.sweenus.simplytooltips.api.TooltipProvider;
import net.sweenus.simplytooltips.api.TooltipTheme;

import java.util.List;

/**
 * Simply Tooltips provider for the Downfall Shard.
 */
public final class DownfallShardTooltipProvider implements TooltipProvider {
    @Override
    public boolean supports(ItemStack stack) {
        return AbaBossesConfig.downfallShardTooltipsEnabled() && stack.is(AbaBossesItems.DOWNFALL_SHARD.get());
    }

    @Override
    public ModernTooltipModel build(ItemStack stack, List<Component> rawLines, boolean altDown) {
        String title = rawLines.isEmpty() ? stack.getHoverName().getString() : rawLines.getFirst().getString();

        return new ModernTooltipModel(
                title,
                List.of("BOSS MATERIAL", "DOWNFALL"),
                TooltipBorderStyle.DEFAULT,
                List.of(
                        ModernTooltipModel.SECTION_MARKER + Component.translatable("tooltip.aba_bosses.downfall_shard.section").getString(),
                        Component.translatable("tooltip.aba_bosses.downfall_shard.recovered").getString(),
                        "",
                        Component.translatable("tooltip.aba_bosses.downfall_shard.description").getString()
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
