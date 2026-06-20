package com.abastudios.bosses.fabric.client;

import com.abastudios.bosses.client.vfx.LightningLaserVfx;
import net.minecraft.world.phys.Vec3;
import team.lodestar.lodestone.events.LodestoneRenderEvents;
import team.lodestar.lodestone.handlers.RenderHandler;
import team.lodestar.lodestone.handlers.ScreenshakeHandler;
import team.lodestar.lodestone.systems.easing.Easing;
import team.lodestar.lodestone.systems.screenshake.PositionedScreenshakeInstance;
import team.lodestar.lodestone.systems.screenshake.ScreenshakeInstance;

/**
 * Fabric Lodestone adapter for the LightningLaser recreation.
 */
public final class LightningLaserLodestoneFabric {
    private LightningLaserLodestoneFabric() {
    }

    /**
     * Registers Lodestone render and screenshake hooks.
     */
    public static void init() {
        LodestoneRenderEvents.AFTER_PARTICLES.register((poseStack, partialTick, stage) ->
                LightningLaserVfx.render(partialTick, RenderHandler.DELAYED_RENDER.getTarget()));
        LightningLaserVfx.setScreenshake(LightningLaserLodestoneFabric::shake);
    }

    private static void shake(Vec3 position) {
        ScreenshakeHandler.addScreenshake(new ScreenshakeInstance(5)
                .setIntensity(5.0f, 0.0f)
                .setEasing(Easing.SINE_OUT));
        ScreenshakeHandler.addScreenshake(new PositionedScreenshakeInstance(5, position, 4.0f, 10.0f, Easing.SINE_OUT)
                .setIntensity(5.0f, 0.0f)
                .setEasing(Easing.SINE_OUT));
    }
}
