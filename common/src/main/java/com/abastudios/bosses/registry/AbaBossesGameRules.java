package com.abastudios.bosses.registry;

import net.minecraft.world.level.GameRules;

/**
 * Game rule registry for Aba's Bosses.
 */
public final class AbaBossesGameRules {
    /**
     * Scales black hole entity gravity. Values less than or equal to zero disable the pull.
     */
    public static final GameRules.Key<GameRules.IntegerValue> BLACK_HOLE_GRAVITY = GameRules.register(
            "blackHoleGravity",
            GameRules.Category.MISC,
            GameRules.IntegerValue.create(1)
    );

    /**
     * Controls black hole tangential motion. Positive values add drift, negative values damp sideways velocity, and zero disables both.
     */
    public static final GameRules.Key<GameRules.IntegerValue> BLACK_HOLE_TANGENTIAL = GameRules.register(
            "blackHoleTangential",
            GameRules.Category.MISC,
            GameRules.IntegerValue.create(1)
    );

    /**
     * Scales black hole visual radius and gravity reach. Values less than one use the default size.
     */
    public static final GameRules.Key<GameRules.IntegerValue> BLACK_HOLE_SIZE = GameRules.register(
            "blackHoleSize",
            GameRules.Category.MISC,
            GameRules.IntegerValue.create(1)
    );

    private AbaBossesGameRules() {
    }

    /**
     * Initializes static game rule registration.
     */
    public static void register() {
    }
}
