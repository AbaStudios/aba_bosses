package com.abastudios.bosses.fabric.client;

import com.abastudios.bosses.client.vfx.LightningLaserTestCommand;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandManager;
import net.fabricmc.fabric.api.client.command.v2.ClientCommandRegistrationCallback;

/**
 * Fabric client command registration for vertical laser VFX tests.
 */
public final class LightningLaserTestCommandFabric {
    private LightningLaserTestCommandFabric() {
    }

    /**
     * Registers /aba_bosses lasers.
     */
    public static void init() {
        ClientCommandRegistrationCallback.EVENT.register((dispatcher, registryAccess) -> dispatcher.register(
                ClientCommandManager.literal(LightningLaserTestCommand.ROOT_COMMAND)
                        .executes(context -> LightningLaserTestCommand.describeUsage())
                        .then(ClientCommandManager.literal(LightningLaserTestCommand.LASERS_TASK)
                                .executes(context -> LightningLaserTestCommand.showcaseLasers()))
        ));
    }
}
