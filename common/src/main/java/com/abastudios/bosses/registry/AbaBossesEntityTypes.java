package com.abastudios.bosses.registry;

import com.abastudios.bosses.AbaBosses;
import com.abastudios.bosses.entity.BlackHoleEntity;
import dev.architectury.registry.registries.DeferredRegister;
import dev.architectury.registry.registries.RegistrySupplier;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

/**
 * Entity type registry for Aba's Bosses.
 */
public final class AbaBossesEntityTypes {
    public static final DeferredRegister<EntityType<?>> ENTITY_TYPES = DeferredRegister.create(AbaBosses.MOD_ID, Registries.ENTITY_TYPE);

    public static final RegistrySupplier<EntityType<BlackHoleEntity>> BLACK_HOLE = ENTITY_TYPES.register(
            "black_hole",
            () -> EntityType.Builder.<BlackHoleEntity>of(BlackHoleEntity::new, MobCategory.MISC)
                    .fireImmune()
                    .sized(2.7f, 2.7f)
                    .clientTrackingRange(16)
                    .updateInterval(Integer.MAX_VALUE)
                    .build("black_hole")
    );

    private AbaBossesEntityTypes() {
    }

    /**
     * Registers all entity types.
     */
    public static void register() {
        ENTITY_TYPES.register();
    }
}
