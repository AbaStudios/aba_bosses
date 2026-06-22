package com.abastudios.bosses.entity;

import com.abastudios.bosses.AbaBosses;
import com.abastudios.bosses.registry.AbaBossesGameRules;
import java.util.List;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Summonable black hole anchor with shader-driven lensing and server-side gravity.
 */
public final class BlackHoleEntity extends Entity {
    private static final EntityDataAccessor<Float> DATA_SIZE_SCALE = SynchedEntityData.defineId(BlackHoleEntity.class, EntityDataSerializers.FLOAT);

    /**
     * Apparent Schwarzschild shadow radius used by the Ghostty shader math.
     */
    public static final float SHADOW_RADIUS = 2.5980762f;

    /**
     * Maximum visible lensing/disk extent around the anchor, in entity-local blocks.
     */
    public static final float EFFECT_RADIUS = 10.0f;

    /**
     * Maximum block radius affected by black hole gravity.
     */
    public static final double GRAVITY_RADIUS = 24.0;

    private static final double SOFTENING_RADIUS = 4.0;
    private static final double BASE_GRAVITATIONAL_PARAMETER = 24.0;
    private static final double MAX_ACCELERATION_PER_TICK = 0.22;
    private static final double BASE_TANGENTIAL_ACCELERATION_PER_TICK = 0.002;
    private static final double MAX_TANGENTIAL_ACCELERATION_PER_TICK = 0.01;
    private static final double BASE_TANGENTIAL_DAMPING = 0.025;
    private static final double INNER_TANGENTIAL_DAMPING = 0.14;
    private static final double EDGE_OUTWARD_RESISTANCE = 0.18;
    private static final double INNER_OUTWARD_RESISTANCE = 0.82;
    private static final double EDGE_ALLOWED_OUTWARD_SPEED = 0.20;
    private static final double INNER_ALLOWED_OUTWARD_SPEED = 0.025;
    private static final double MAX_SPEED = 3.0;
    private static final int MAX_GAME_RULE_STRENGTH = 16;
    private static final int MAX_GAME_RULE_SIZE = 16;

    public BlackHoleEntity(EntityType<? extends BlackHoleEntity> entityType, Level level) {
        super(entityType, level);
        this.noPhysics = true;
        this.noCulling = true;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        builder.define(DATA_SIZE_SCALE, 1.0f);
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compoundTag) {
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compoundTag) {
    }

    @Override
    public void tick() {
        super.tick();
        this.noPhysics = true;
        this.noCulling = true;
        this.setDeltaMovement(Vec3.ZERO);

        if (!this.level().isClientSide()) {
            this.syncSizeScaleFromGameRule();
            this.applyBlackHoleGravity();
        }
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    /**
     * Returns the synced black hole size multiplier used by client rendering and server gravity.
     */
    public float getSizeScale() {
        return this.entityData.get(DATA_SIZE_SCALE);
    }

    /**
     * Returns the black hole size multiplier configured for the supplied level.
     */
    public static float getSizeScaleFromGameRule(Level level) {
        return Mth.clamp(level.getGameRules().getInt(AbaBossesGameRules.BLACK_HOLE_SIZE), 1, MAX_GAME_RULE_SIZE);
    }

    private void syncSizeScaleFromGameRule() {
        float nextSizeScale = getSizeScaleFromGameRule(this.level());
        float currentSizeScale = this.entityData.get(DATA_SIZE_SCALE);
        if (currentSizeScale != nextSizeScale) {
            this.entityData.set(DATA_SIZE_SCALE, nextSizeScale);
            AbaBosses.LOGGER.info("Black hole size sync id={} size={} pos={}", this.getId(), nextSizeScale, this.position());
        }
    }

    private void applyBlackHoleGravity() {
        int ruleStrength = this.level().getGameRules().getInt(AbaBossesGameRules.BLACK_HOLE_GRAVITY);
        if (ruleStrength <= 0) {
            return;
        }

        int strength = Mth.clamp(ruleStrength, 1, MAX_GAME_RULE_STRENGTH);
        int tangentialRule = Mth.clamp(this.level().getGameRules().getInt(AbaBossesGameRules.BLACK_HOLE_TANGENTIAL), -MAX_GAME_RULE_STRENGTH, MAX_GAME_RULE_STRENGTH);
        double strengthScale = Math.sqrt(strength);
        double tangentialStrengthScale = Math.sqrt(Math.max(Math.abs(tangentialRule), 1));
        double sizeScale = this.getSizeScale();
        double gravityRadius = GRAVITY_RADIUS * sizeScale;
        double gravityRadiusSquared = gravityRadius * gravityRadius;
        double softeningRadius = SOFTENING_RADIUS * sizeScale;
        double softeningRadiusSquared = softeningRadius * softeningRadius;
        Vec3 blackHoleCenter = this.position();
        AABB gravityBounds = this.getBoundingBox().inflate(gravityRadius);
        List<Entity> targets = this.level().getEntities(this, gravityBounds, BlackHoleEntity::canApplyGravityTo);

        for (Entity target : targets) {
            Vec3 targetCenter = target.getBoundingBox().getCenter();
            Vec3 offset = blackHoleCenter.subtract(targetCenter);
            double distanceSquared = offset.lengthSqr();

            if (distanceSquared <= 1.0e-6 || distanceSquared > gravityRadiusSquared) {
                continue;
            }

            double distance = Math.sqrt(distanceSquared);
            Vec3 direction = offset.scale(1.0 / distance);
            double softenedDistanceSquared = distanceSquared + softeningRadiusSquared;
            double acceleration = BASE_GRAVITATIONAL_PARAMETER * strength * distance / (softenedDistanceSquared * Math.sqrt(softenedDistanceSquared));
            acceleration = Math.min(acceleration, MAX_ACCELERATION_PER_TICK * strengthScale);
            Vec3 movement = target.getDeltaMovement();
            double radialSpeed = movement.dot(direction);

            if (radialSpeed < 0.0) {
                acceleration += calculateOutwardResistance(radialSpeed, distance, gravityRadius, strengthScale);
            }

            Vec3 nextMovement = movement.add(direction.scale(acceleration));
            if (tangentialRule > 0) {
                nextMovement = nextMovement.add(calculateTangentialPull(offset, softenedDistanceSquared, gravityRadiusSquared, tangentialRule, tangentialStrengthScale));
            } else if (tangentialRule < 0) {
                nextMovement = dampTangentialMovement(nextMovement, direction, distance, gravityRadius, -tangentialRule, tangentialStrengthScale);
            }

            double maximumSpeed = MAX_SPEED * strengthScale;
            if (nextMovement.lengthSqr() > maximumSpeed * maximumSpeed) {
                nextMovement = nextMovement.normalize().scale(maximumSpeed);
            }

            target.setDeltaMovement(nextMovement);
            target.hasImpulse = true;
            target.hurtMarked = true;
        }
    }

    private static Vec3 calculateTangentialPull(Vec3 offset, double softenedDistanceSquared, double gravityRadiusSquared, int strength, double strengthScale) {
        if (strength <= 0) {
            return Vec3.ZERO;
        }

        double tangentLengthSquared = offset.x * offset.x + offset.z * offset.z;
        if (tangentLengthSquared <= 1.0e-6) {
            return Vec3.ZERO;
        }

        double tangentialAcceleration = BASE_TANGENTIAL_ACCELERATION_PER_TICK * strength * gravityRadiusSquared / softenedDistanceSquared;
        tangentialAcceleration = Math.min(tangentialAcceleration, MAX_TANGENTIAL_ACCELERATION_PER_TICK * strengthScale);
        double inverseTangentLength = Mth.invSqrt(tangentLengthSquared);
        return new Vec3(-offset.z * inverseTangentLength * tangentialAcceleration, 0.0, offset.x * inverseTangentLength * tangentialAcceleration);
    }

    private static Vec3 dampTangentialMovement(Vec3 movement, Vec3 direction, double distance, double gravityRadius, int strength, double strengthScale) {
        Vec3 radialMovement = direction.scale(movement.dot(direction));
        Vec3 tangentialMovement = movement.subtract(radialMovement);
        double proximity = 1.0 - Mth.clamp(distance / gravityRadius, 0.0, 1.0);
        double damping = Mth.lerp(proximity * proximity, BASE_TANGENTIAL_DAMPING, INNER_TANGENTIAL_DAMPING);
        damping = Math.min(damping * strengthScale, 0.95);
        return radialMovement.add(tangentialMovement.scale(1.0 - damping));
    }

    private static double calculateOutwardResistance(double radialSpeed, double distance, double gravityRadius, double strengthScale) {
        double proximity = 1.0 - Mth.clamp(distance / gravityRadius, 0.0, 1.0);
        double wellDepth = proximity * proximity;
        double allowedOutwardSpeed = Mth.lerp(wellDepth, EDGE_ALLOWED_OUTWARD_SPEED, INNER_ALLOWED_OUTWARD_SPEED) / strengthScale;
        if (radialSpeed >= -allowedOutwardSpeed) {
            return 0.0;
        }

        double resistance = Mth.lerp(wellDepth, EDGE_OUTWARD_RESISTANCE, INNER_OUTWARD_RESISTANCE);
        return (-allowedOutwardSpeed - radialSpeed) * Math.min(resistance * strengthScale, 0.95);
    }

    private static boolean canApplyGravityTo(Entity entity) {
        if (!entity.isAlive()) {
            return false;
        }

        return !(entity instanceof Player player && player.isCreative() && player.getAbilities().flying);
    }
}
