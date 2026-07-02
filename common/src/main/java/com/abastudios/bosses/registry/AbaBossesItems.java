package com.abastudios.bosses.registry;

import com.abastudios.bosses.AbaBosses;
import com.abastudios.bosses.item.DownfallShardItem;
import com.abastudios.bosses.item.WarpglassWandItem;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.Item;

/**
 * Item registry for Aba's Bosses.
 */
public final class AbaBossesItems {
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(AbaBosses.MOD_ID, Registries.ITEM);

    public static final RegistrySupplier<Item> DOWNFALL_SHARD = ITEMS.register(
            "downfall_shard",
            () -> new DownfallShardItem(new Item.Properties().arch$tab(AbaBossesTabs.ABA_BOSSES))
    );
    public static final RegistrySupplier<Item> WARPGLASS_WAND = ITEMS.register(
            "warpglass_wand",
            () -> new WarpglassWandItem(new Item.Properties().stacksTo(1).arch$tab(AbaBossesTabs.ABA_BOSSES))
    );

    private AbaBossesItems() {
    }

    /**
     * Registers all items.
     */
    public static void register() {
        ITEMS.register();
    }
}
