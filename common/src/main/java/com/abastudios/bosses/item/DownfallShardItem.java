package com.abastudios.bosses.item;

import com.abastudios.bosses.config.AbaBossesConfig;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

import java.util.List;

/**
 * Material item used by Downfall-related content.
 */
public final class DownfallShardItem extends Item {
    public DownfallShardItem(Properties properties) {
        super(properties);
    }

    @Override
    public void appendHoverText(ItemStack stack, TooltipContext context, List<Component> tooltip, TooltipFlag flag) {
        if (!AbaBossesConfig.downfallShardTooltipsEnabled()) {
            tooltip.add(Component.translatable("tooltip.aba_bosses.downfall_shard.disabled").withStyle(ChatFormatting.DARK_GRAY));
            return;
        }

        tooltip.add(Component.translatable("tooltip.aba_bosses.downfall_shard.line_1").withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.translatable("tooltip.aba_bosses.downfall_shard.line_2").withStyle(ChatFormatting.DARK_PURPLE));
    }
}
