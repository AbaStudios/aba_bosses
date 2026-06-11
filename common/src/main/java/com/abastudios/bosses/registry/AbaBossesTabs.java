package com.abastudios.bosses.registry;

import com.abastudios.bosses.AbaBosses;
import dev.architectury.registry.CreativeTabRegistry;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

/**
 * Creative tab registry for Aba's Bosses.
 */
public final class AbaBossesTabs {
    public static final DeferredRegister<CreativeModeTab> TABS = DeferredRegister.create(AbaBosses.MOD_ID, Registries.CREATIVE_MODE_TAB);

    public static final RegistrySupplier<CreativeModeTab> ABA_BOSSES = TABS.register(
            "aba_bosses",
            () -> CreativeTabRegistry.create(
                    Component.translatable("itemGroup.aba_bosses.aba_bosses"),
                    () -> new ItemStack(AbaBossesItems.DOWNFALL_SHARD.get())
            )
    );

    private AbaBossesTabs() {
    }

    /**
     * Registers all creative tabs.
     */
    public static void register() {
        TABS.register();
    }
}
