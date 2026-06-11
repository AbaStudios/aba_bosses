package com.abastudios.bosses.config;

import com.abastudios.bosses.AbaBosses;
import me.fzzyhmstrs.fzzy_config.api.ConfigApiJava;
import me.fzzyhmstrs.fzzy_config.api.RegisterType;
import me.fzzyhmstrs.fzzy_config.config.Config;
import me.fzzyhmstrs.fzzy_config.validation.misc.ValidatedBoolean;
import net.minecraft.resources.ResourceLocation;

/**
 * Fzzy Config-backed client settings for Aba's Bosses.
 */
public final class AbaBossesConfig extends Config {
    public static final AbaBossesConfig INSTANCE = ConfigApiJava.registerAndLoadConfig(AbaBossesConfig::new, RegisterType.CLIENT);

    public ValidatedBoolean enableDownfallShardTooltips = new ValidatedBoolean(true);

    public AbaBossesConfig() {
        super(ResourceLocation.fromNamespaceAndPath(AbaBosses.MOD_ID, "config"));
    }

    /**
     * Initializes and loads the shared client config.
     */
    public static void init() {
        INSTANCE.getId();
    }

    /**
     * Returns whether the Downfall Shard should use the Simply Tooltips provider.
     */
    public static boolean downfallShardTooltipsEnabled() {
        return INSTANCE.enableDownfallShardTooltips.get();
    }
}
