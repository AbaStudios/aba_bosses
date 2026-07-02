package com.abastudios.bosses.item;

import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Applies the Warpglass Wand's forward impulse and handles custom decay.
 */
public final class WarpglassDash {
    private static final double IMPULSE = 5.0; // Total distance over 2 ticks (2.5 blocks per tick)
    private static final int MAX_TICKS = 2;
    private static final Map<UUID, DashState> ACTIVE_DASHES = new ConcurrentHashMap<>();

    private WarpglassDash() {
    }

    private static class DashState {
        Vec3 dashVector;
        int ticks;

        DashState(Vec3 dashVector) {
            this.dashVector = dashVector;
            this.ticks = 0;
        }
    }

    /**
     * Adds a warp impulse in the direction the player is looking, including vertical pitch.
     */
    public static void start(Player player) {
        Vec3 direction = player.getLookAngle();
        Vec3 dashStep = direction.scale(IMPULSE / MAX_TICKS);

        // Register active dash state (without modifying deltaMovement)
        ACTIVE_DASHES.put(player.getUUID(), new DashState(dashStep));
    }

    /**
     * Ticks the player dash, applying collision-aware spatial displacement.
     */
    public static void tick(Player player) {
        DashState state = ACTIVE_DASHES.get(player.getUUID());
        if (state == null) {
            return;
        }

        // Apply displacement using Minecraft's collision-aware move method
        player.move(MoverType.SELF, state.dashVector);

        state.ticks++;

        // End dash after max ticks or on collision
        if (player.horizontalCollision || player.verticalCollision || state.ticks >= MAX_TICKS) {
            ACTIVE_DASHES.remove(player.getUUID());
        }
    }
}
