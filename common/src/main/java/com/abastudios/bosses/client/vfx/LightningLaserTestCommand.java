package com.abastudios.bosses.client.vfx;

import com.abastudios.bosses.AbaBosses;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.Vec3;

/**
 * Client-only debug executor for LightningLaser and AstralLaser visuals.
 */
public final class LightningLaserTestCommand {
    public static final String ROOT_COMMAND = AbaBosses.MOD_ID;
    public static final String LASERS_TASK = "lasers";

    private LightningLaserTestCommand() {
    }

    /**
     * Sends usage text for the laser VFX test command.
     */
    public static int describeUsage() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }

        describe(player, "Usage: /aba_bosses lasers. It showcases four lasers 7 blocks ahead: sustained LightningLaser, one-shot LightningLaser, sustained AstralLaser, and one-shot AstralLaser.");
        return 1;
    }

    /**
     * Spawns the complete laser showcase in front of the local player.
     */
    public static int showcaseLasers() {
        LocalPlayer player = Minecraft.getInstance().player;
        if (player == null) {
            return 0;
        }

        LightningLaserVfx.spawnMoving(testPosition(player, -5.4), 48.0f, 200);
        LightningLaserVfx.spawnOneShot(testPosition(player, -1.8), 48.0f, 60);
        AstralLaserVfx.spawnMoving(testPosition(player, 1.8), 48.0f, 200);
        AstralLaserVfx.spawnOneShot(testPosition(player, 5.4), 48.0f, 60);
        describe(player, "Laser showcase: 7 blocks ahead, left to right. Sustained LightningLaser: yellow, thin center, wide animated lightning forks for 10 seconds. One-shot LightningLaser: red/yellow target for 3 seconds, then a longer gold strike, ground debris, and screenshake. Sustained AstralLaser: magenta/violet column for 10 seconds with moving internal noise bands and no lightning. One-shot AstralLaser: red/magenta target for 3 seconds, then a longer smooth astral strike, ground debris, and screenshake.");
        return 1;
    }

    private static void describe(LocalPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), false);
    }

    private static Vec3 testPosition(LocalPlayer player, double sideOffset) {
        Vec3 look = player.getLookAngle();
        Vec3 forward = new Vec3(look.x, 0.0, look.z);
        if (forward.lengthSqr() < 1.0E-4) {
            forward = Vec3.directionFromRotation(0.0f, player.getYRot());
        }
        forward = forward.normalize();
        Vec3 right = new Vec3(-forward.z, 0.0, forward.x);
        return player.position().add(forward.scale(7.0)).add(right.scale(sideOffset));
    }
}
