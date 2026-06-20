package com.abastudios.bosses.neoforge.client;

import com.abastudios.bosses.client.vfx.LightningLaserTestCommand;
import net.minecraft.commands.Commands;
import net.neoforged.neoforge.client.event.RegisterClientCommandsEvent;
import net.neoforged.neoforge.common.NeoForge;

/**
 * NeoForge client command registration for vertical laser VFX tests.
 */
public final class LightningLaserTestCommandNeoForge {
    private LightningLaserTestCommandNeoForge() {
    }

    /**
     * Registers /aba_bosses lasers.
     */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(LightningLaserTestCommandNeoForge::register);
    }

    private static void register(RegisterClientCommandsEvent event) {
        event.getDispatcher().register(
                Commands.literal(LightningLaserTestCommand.ROOT_COMMAND)
                        .executes(context -> LightningLaserTestCommand.describeUsage())
                        .then(Commands.literal(LightningLaserTestCommand.LASERS_TASK)
                                .executes(context -> LightningLaserTestCommand.showcaseLasers()))
        );
    }
}
