package com.abastudios.bosses.neoforge;

import com.abastudios.bosses.AbaBosses;
import com.abastudios.bosses.client.AbaBossesClient;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * NeoForge entrypoint for Aba's Bosses.
 */
@Mod(AbaBosses.MOD_ID)
public final class AbaBossesNeoForge {
    public AbaBossesNeoForge(ModContainer container) {
        AbaBosses.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            AbaBossesClient.init();
        }
    }
}
