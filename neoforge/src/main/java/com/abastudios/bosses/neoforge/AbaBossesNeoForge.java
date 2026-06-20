package com.abastudios.bosses.neoforge;

import com.abastudios.bosses.AbaBosses;
import com.abastudios.bosses.client.AbaBossesClient;
import com.abastudios.bosses.neoforge.client.AbaBossesNeoForgeClient;
import com.abastudios.bosses.neoforge.client.LightningLaserLodestoneNeoForge;
import com.abastudios.bosses.neoforge.client.LightningLaserTestCommandNeoForge;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.fml.ModList;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.loading.FMLEnvironment;

/**
 * NeoForge entrypoint for Aba's Bosses.
 */
@Mod(AbaBosses.MOD_ID)
public final class AbaBossesNeoForge {
    public AbaBossesNeoForge(IEventBus modEventBus, ModContainer container) {
        AbaBosses.init();

        if (FMLEnvironment.dist == Dist.CLIENT) {
            AbaBossesNeoForgeClient.init(modEventBus);
            AbaBossesClient.init();
            if (ModList.get().isLoaded("lodestone")) {
                LightningLaserLodestoneNeoForge.init();
                LightningLaserTestCommandNeoForge.init();
            }
        }
    }
}
