package com.abastudios.bosses.client.vfx;

import net.minecraft.world.phys.Vec3;

/**
 * Client-only magenta vertical laser visuals without lightning arcs.
 */
public final class AstralLaserVfx {
    private AstralLaserVfx() {
    }

    /**
     * Spawns the sustained astral vertical ray column.
     */
    public static void spawnMoving(Vec3 position, float height, int lifetime) {
        LightningLaserVfx.spawnAstralMoving(position, height, lifetime);
    }

    /**
     * Spawns the astral one-shot vertical ray telegraph and strike.
     */
    public static void spawnOneShot(Vec3 position, float height, int attackPreparationTime) {
        LightningLaserVfx.spawnAstralOneShot(position, height, attackPreparationTime);
    }
}
