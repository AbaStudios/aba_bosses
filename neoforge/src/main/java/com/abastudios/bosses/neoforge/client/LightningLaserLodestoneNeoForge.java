package com.abastudios.bosses.neoforge.client;

import com.abastudios.bosses.client.vfx.LightningLaserVfx;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import team.lodestar.lodestone.handlers.ScreenshakeHandler;
import team.lodestar.lodestone.modules.core.easing.Easing;

/**
 * NeoForge Lodestone adapter for the LightningLaser recreation.
 */
public final class LightningLaserLodestoneNeoForge {
    private static final ByteBufferBuilder DIRECT_BUFFER = new ByteBufferBuilder(786432);
    private static final MultiBufferSource.BufferSource DIRECT_BUFFER_SOURCE = MultiBufferSource.immediate(DIRECT_BUFFER);

    private LightningLaserLodestoneNeoForge() {
    }

    /**
     * Registers Lodestone render and screenshake hooks.
     */
    public static void init() {
        NeoForge.EVENT_BUS.addListener(EventPriority.NORMAL, LightningLaserLodestoneNeoForge::render);
        LightningLaserVfx.setScreenshake(LightningLaserLodestoneNeoForge::shake);
    }

    private static void render(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_WEATHER) {
            return;
        }

        if (!LightningLaserVfx.hasActiveInstances()) {
            return;
        }

        LightningLaserVfx.render(event.getPartialTick().getGameTimeDeltaPartialTick(false), DIRECT_BUFFER_SOURCE);
        DIRECT_BUFFER_SOURCE.endBatch();
    }

    private static void shake(Vec3 position) {
        ScreenshakeHandler.addScreenshake(builder -> builder
                .setDuration(5)
                .setStrength(5.0f, 0.0f)
                .setEasing(Easing.SINE_OUT)
                .placedAt(position, 10.0f));
    }
}
