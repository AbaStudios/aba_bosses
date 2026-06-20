package com.abastudios.bosses.client.vfx;

import com.abastudios.bosses.AbaBosses;
import com.abastudios.bosses.client.render.AbaBossesRenderTypes;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.particles.BlockParticleOption;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

/**
 * Client-only vertical laser visuals for Aba's Bosses.
 */
public final class LightningLaserVfx {
    private static final ResourceLocation TARGET_TEXTURE = AbaBosses.location("textures/vfx/laser/target_large.png");
    private static final ResourceLocation IMPACT_FLARE_TEXTURE = AbaBosses.location("textures/vfx/laser/electric_pop_b.png");
    private static final Vector3f LIGHTNING_DUST = new Vector3f(1.0f, 0.84f, 0.16f);
    private static final Vector3f LIGHTNING_SECONDARY_DUST = new Vector3f(1.0f, 0.42f, 0.06f);
    private static final Vector3f ASTRAL_DUST = new Vector3f(1.0f, 0.16f, 0.9f);
    private static final Vector3f ASTRAL_SECONDARY_DUST = new Vector3f(0.55f, 0.08f, 1.0f);
    private static final float BEAM_LENGTH_MULTIPLIER = 1.12f;
    private static final int STRIKE_IN = 0;
    private static final int STRIKE_STAY = 14;
    private static final int STRIKE_OUT = 10;
    private static final int STRIKE_TIME = STRIKE_IN + STRIKE_STAY + STRIKE_OUT;
    private static final AtomicInteger NEXT_SEED = new AtomicInteger(1);
    private static final List<VerticalLaserInstance> INSTANCES = new ArrayList<>();
    private static final List<VerticalLaserInstance> PENDING_INSTANCES = new ArrayList<>();
    private static Consumer<Vec3> screenshake = position -> {
    };

    private LightningLaserVfx() {
    }

    /**
     * Registers common client ticking for active laser lifecycles.
     */
    public static void init() {
        ClientTickEvent.CLIENT_POST.register(client -> tick());
    }

    /**
     * Registers the platform-specific Lodestone screenshake implementation.
     */
    public static void setScreenshake(Consumer<Vec3> screenshake) {
        LightningLaserVfx.screenshake = screenshake;
    }

    /**
     * Spawns the sustained moving vertical ray column.
     */
    public static void spawnMoving(Vec3 position, float height, int lifetime) {
        INSTANCES.add(new MovingVerticalLaser(position, height, lifetime, LaserStyle.LIGHTNING));
    }

    /**
     * Spawns the one-shot vertical ray telegraph and strike.
     */
    public static void spawnOneShot(Vec3 position, float height, int attackPreparationTime) {
        INSTANCES.add(new OneShotVerticalLaser(position, height, attackPreparationTime, LaserStyle.LIGHTNING));
    }

    /**
     * Spawns the sustained astral vertical ray column.
     */
    public static void spawnAstralMoving(Vec3 position, float height, int lifetime) {
        INSTANCES.add(new MovingVerticalLaser(position, height, lifetime, LaserStyle.ASTRAL));
    }

    /**
     * Spawns the astral one-shot vertical ray telegraph and strike.
     */
    public static void spawnAstralOneShot(Vec3 position, float height, int attackPreparationTime) {
        INSTANCES.add(new OneShotVerticalLaser(position, height, attackPreparationTime, LaserStyle.ASTRAL));
    }

    /**
     * Returns true while client-side laser instances need rendering.
     */
    public static boolean hasActiveInstances() {
        return !INSTANCES.isEmpty();
    }

    /**
     * Renders all active lasers into the caller-provided client buffer source.
     */
    public static void render(float partialTicks, MultiBufferSource bufferSource) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null || INSTANCES.isEmpty()) {
            return;
        }

        Camera camera = minecraft.gameRenderer.getMainCamera();
        PoseStack poseStack = new PoseStack();
        for (VerticalLaserInstance instance : INSTANCES) {
            instance.render(poseStack, bufferSource, camera, partialTicks);
        }
    }

    private static void tick() {
        Minecraft minecraft = Minecraft.getInstance();
        ClientLevel level = minecraft.level;
        if (level == null || minecraft.isPaused()) {
            return;
        }

        Iterator<VerticalLaserInstance> iterator = INSTANCES.iterator();
        while (iterator.hasNext()) {
            VerticalLaserInstance instance = iterator.next();
            instance.tick(level);
            if (instance.isExpired()) {
                iterator.remove();
            }
        }
        if (!PENDING_INSTANCES.isEmpty()) {
            INSTANCES.addAll(PENDING_INSTANCES);
            PENDING_INSTANCES.clear();
        }
    }

    private static void spawnDust(ClientLevel level, Vec3 position, Vec3 velocity, float scale) {
        spawnDust(level, position, velocity, LaserStyle.LIGHTNING.primaryDust, scale);
    }

    private static void spawnDust(ClientLevel level, Vec3 position, Vec3 velocity, Vector3f color, float scale) {
        level.addParticle(new DustParticleOptions(color, scale), true, position.x, position.y, position.z, velocity.x, velocity.y, velocity.z);
    }

    private static void spawnImpactBloom(ClientLevel level, Vec3 position, LaserStyle style) {
        spawnDust(level, position, Vec3.ZERO, style.primaryDust, 14.0f);
        spawnBlockImpactParticles(level, position);
    }

    private static void spawnBlockImpactParticles(ClientLevel level, Vec3 position) {
        BlockState impactState = findImpactState(level, position);
        if (impactState == null) {
            return;
        }

        RandomSource random = level.random;
        BlockParticleOption debris = new BlockParticleOption(ParticleTypes.BLOCK, impactState);
        for (int i = 0; i < 42; i++) {
            float angle = random.nextFloat() * Mth.TWO_PI;
            float radius = 0.15f + random.nextFloat() * 0.85f;
            float speed = 0.18f + random.nextFloat() * 0.55f;
            double x = position.x + Mth.cos(angle) * radius;
            double z = position.z + Mth.sin(angle) * radius;
            double vx = Mth.cos(angle) * speed;
            double vy = 0.08 + random.nextFloat() * 0.42;
            double vz = Mth.sin(angle) * speed;
            level.addParticle(debris, true, x, position.y + 0.08, z, vx, vy, vz);
        }
    }

    private static BlockState findImpactState(ClientLevel level, Vec3 position) {
        BlockPos origin = BlockPos.containing(position.x, position.y - 0.08, position.z);
        for (int i = 0; i < 6; i++) {
            BlockState state = level.getBlockState(origin.below(i));
            if (!state.isAir()) {
                return state;
            }
        }
        return null;
    }

    private static float easeOut(float progress) {
        float clamped = Mth.clamp(progress, 0.0f, 1.0f);
        return 1.0f - (1.0f - clamped) * (1.0f - clamped);
    }

    private static float reversedEaseOut(float progress) {
        return 1.0f - easeOut(progress);
    }

    private enum LaserStyle {
        LIGHTNING(true, LIGHTNING_DUST, LIGHTNING_SECONDARY_DUST),
        ASTRAL(false, ASTRAL_DUST, ASTRAL_SECONDARY_DUST);

        private final boolean lightning;
        private final Vector3f primaryDust;
        private final Vector3f secondaryDust;

        LaserStyle(boolean lightning, Vector3f primaryDust, Vector3f secondaryDust) {
            this.lightning = lightning;
            this.primaryDust = primaryDust;
            this.secondaryDust = secondaryDust;
        }
    }

    private abstract static class VerticalLaserInstance {
        protected final Vec3 position;
        protected final float height;
        protected final LaserStyle style;
        protected final int seed;
        protected int age;

        protected VerticalLaserInstance(Vec3 position, float height, LaserStyle style) {
            this.position = position;
            this.height = height;
            this.style = style;
            this.seed = NEXT_SEED.getAndIncrement();
        }

        protected abstract void tick(ClientLevel level);

        protected abstract void render(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTicks);

        protected abstract boolean isExpired();

        protected void renderMovingColumn(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTicks) {
            poseStack.pushPose();

            Vec3 cameraPosition = camera.getPosition();
            Vec3 relative = position.subtract(cameraPosition);
            poseStack.translate(relative.x, relative.y, relative.z);
            double angle = Math.atan2(position.x - cameraPosition.x, position.z - cameraPosition.z);
            poseStack.mulPose(Axis.YP.rotation(Mth.PI + (float) angle));

            VertexConsumer lightning = bufferSource.getBuffer(AbaBossesRenderTypes.LIGHTNING_NO_CULL);
            float beamHeight = height * BEAM_LENGTH_MULTIPLIER;
            if (style.lightning) {
                renderFastLightning(new Matrix4f(poseStack.last().pose()).translate(0.0f, 0.0f, -0.06f), lightning, beamHeight, 0.24f, 1.25f, 9, levelTime(), seed, 1.0f, 0.62f, 0.04f, 0.98f);
                renderFastLightning(new Matrix4f(poseStack.last().pose()).translate(0.0f, 0.0f, 0.04f), lightning, beamHeight, 0.12f, 0.75f, 7, levelTime() + 3L, seed * 31, 1.0f, 0.9f, 0.38f, 1.0f);
                renderFastLightning(new Matrix4f(poseStack.last().pose()).translate(0.0f, 0.0f, 0.1f), lightning, beamHeight, 0.06f, 0.38f, 6, levelTime() + 7L, seed * 53, 1.0f, 1.0f, 0.86f, 0.9f);
            }

            VertexConsumer gradient = bufferSource.getBuffer(RenderType.lightning());
            Matrix4f matrix = poseStack.last().pose();
            renderStyledBeam(matrix, gradient, beamHeight, style.lightning ? 0.42f : 0.62f, 0.88f, style);
            renderBeamNoise(matrix, gradient, beamHeight, style.lightning ? 0.74f : 0.82f, 0.6f, style, age + partialTicks, seed);

            poseStack.popPose();
        }

        protected void renderStrikeColumn(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float alpha, float partialTicks) {
            poseStack.pushPose();

            Vec3 cameraPosition = camera.getPosition();
            Vec3 relative = position.subtract(cameraPosition);
            poseStack.translate(relative.x, relative.y, relative.z);
            double angle = Math.atan2(position.x - cameraPosition.x, position.z - cameraPosition.z);
            poseStack.mulPose(Axis.YP.rotation(Mth.PI + (float) angle));

            float width = 0.5f * alpha;
            float beamHeight = height * BEAM_LENGTH_MULTIPLIER;
            Matrix4f matrix = poseStack.last().pose();
            VertexConsumer gradient = bufferSource.getBuffer(AbaBossesRenderTypes.LIGHTNING_NO_CULL);
            renderStyledBeam(matrix, gradient, beamHeight, Math.max(0.18f, width * (style.lightning ? 1.15f : 1.8f)), alpha, style);
            renderBeamNoise(matrix, gradient, beamHeight, Math.max(0.7f, width * 2.25f), alpha * 0.72f, style, age + partialTicks, seed * 19);

            if (style.lightning && alpha > 0.001f) {
                long time = levelTime();
                renderFastLightning(new Matrix4f(matrix).translate(0.0f, 0.0f, 0.04f), gradient, beamHeight, width * 0.72f, 2.1f, 10, time, seed * 37, 1.0f, 0.52f, 0.02f, alpha);
                renderFastLightning(new Matrix4f(matrix).translate(0.0f, 0.0f, -0.04f), gradient, beamHeight, width * 0.4f, 1.4f, 8, time + 5L, seed * 71, 1.0f, 0.82f, 0.18f, alpha);
                renderFastLightning(new Matrix4f(matrix).translate(0.0f, 0.0f, 0.11f), gradient, beamHeight, width * 0.22f, 0.85f, 7, time + 11L, seed * 97, 1.0f, 0.98f, 0.72f, alpha);
                renderFastLightning(new Matrix4f(matrix).translate(0.0f, 0.0f, -0.12f), gradient, beamHeight, width * 0.1f, 0.45f, 5, time + 17L, seed * 131, 1.0f, 1.0f, 1.0f, alpha * 0.92f);
            }

            poseStack.popPose();
        }

        protected void renderImpactFlare(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float alpha, float partialTicks) {
            float progress = Mth.clamp((age + partialTicks) / STRIKE_TIME, 0.0f, 1.0f);
            float flareAlpha = alpha * reversedEaseOut(progress);
            if (flareAlpha <= 0.001f) {
                return;
            }

            poseStack.pushPose();
            Vec3 relative = position.subtract(camera.getPosition());
            poseStack.translate(relative.x, relative.y + 0.06f, relative.z);

            VertexConsumer vertex = bufferSource.getBuffer(RenderType.eyes(IMPACT_FLARE_TEXTURE));
            renderGroundTextureLayer(poseStack, vertex, 3.4f + progress * 2.4f, (age + partialTicks) * -18.0f, style.primaryDust.x, style.primaryDust.y, style.primaryDust.z, flareAlpha * 0.82f);
            renderGroundTextureLayer(poseStack, vertex, 1.45f + progress * 1.2f, (age + partialTicks) * 44.0f, 1.0f, 0.96f, 0.76f, flareAlpha);
            poseStack.popPose();
        }

        private long levelTime() {
            ClientLevel level = Minecraft.getInstance().level;
            return level == null ? age : level.getGameTime();
        }
    }

    private static final class MovingVerticalLaser extends VerticalLaserInstance {
        private final int lifetime;

        private MovingVerticalLaser(Vec3 position, float height, int lifetime, LaserStyle style) {
            super(position, height, style);
            this.lifetime = lifetime;
        }

        @Override
        protected void tick(ClientLevel level) {
            RandomSource random = level.random;
            for (int i = 0; i < 5; i++) {
                float angle = random.nextFloat() * Mth.TWO_PI;
                float speed = 0.3f;
                Vec3 velocity = new Vec3(Mth.cos(angle) * speed, random.nextFloat() * 0.1f, Mth.sin(angle) * speed);
                spawnDust(level, position, velocity, style.secondaryDust, 0.44f);
            }

            Vec3 top = position.add(0.0, height, 0.0);
            for (int i = 0; i < 2; i++) {
                float radius = random.nextFloat() * 2.0f;
                float angle = random.nextFloat() * Mth.TWO_PI;
                Vec3 offset = new Vec3(Mth.cos(angle) * radius, 0.0, Mth.sin(angle) * radius);
                spawnDust(level, top.add(offset), Vec3.ZERO, style.primaryDust, 0.22f + (1.0f - radius / 2.0f) * 1.2f);
            }
            age++;
        }

        @Override
        protected void render(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTicks) {
            renderMovingColumn(poseStack, bufferSource, camera, partialTicks);
        }

        @Override
        protected boolean isExpired() {
            return age >= lifetime;
        }
    }

    private static final class OneShotVerticalLaser extends VerticalLaserInstance {
        private final int attackPreparationTime;
        private boolean burstSpawned;
        private boolean strikeSpawned;

        private OneShotVerticalLaser(Vec3 position, float height, int attackPreparationTime, LaserStyle style) {
            super(position, height, style);
            this.attackPreparationTime = attackPreparationTime;
        }

        @Override
        protected void tick(ClientLevel level) {
            if (age < attackPreparationTime / 2.0f) {
                spawnPrepareParticles(level, attackPreparationTime / 2);
            }
            if (!burstSpawned && age >= attackPreparationTime - 1) {
                spawnBurstParticles(level);
                burstSpawned = true;
            }
            if (!strikeSpawned && age >= attackPreparationTime) {
                spawnImpactBloom(level, position, style);
                PENDING_INSTANCES.add(new StrikeVerticalLaser(position, height, style));
                screenshake.accept(position);
                strikeSpawned = true;
            }
            age++;
        }

        @Override
        protected void render(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTicks) {
            renderTelegraph(poseStack, bufferSource, camera, partialTicks);
        }

        @Override
        protected boolean isExpired() {
            return strikeSpawned;
        }

        private void renderTelegraph(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTicks) {
            float time = age + partialTicks;
            float rawProgress = Mth.clamp(time / attackPreparationTime, 0.0f, 1.0f);
            float progress = easeOut(rawProgress);
            float pulse = 0.5f + 0.5f * Mth.sin(time * 0.55f);
            float finalWarning = Mth.clamp((rawProgress - 0.68f) / 0.32f, 0.0f, 1.0f);
            poseStack.pushPose();
            Vec3 relative = position.subtract(camera.getPosition());
            poseStack.translate(relative.x, relative.y + 0.05f, relative.z);

            VertexConsumer vertex = bufferSource.getBuffer(RenderType.eyes(TARGET_TEXTURE));
            renderGroundTextureLayer(poseStack, vertex, (1.45f + pulse * 0.24f) * progress, progress * 900.0f, 1.0f, 0.08f, 0.04f, 0.46f + finalWarning * 0.24f);
            renderGroundTextureLayer(poseStack, vertex, (2.05f + finalWarning * 0.7f) * progress, progress * -540.0f, 1.0f, 0.16f, 0.06f, 0.38f + finalWarning * 0.38f);
            renderGroundTextureLayer(poseStack, vertex, (2.65f + pulse * 0.52f) * progress, progress * 180.0f, style.secondaryDust.x, style.secondaryDust.y, style.secondaryDust.z, 0.18f + finalWarning * 0.18f);
            poseStack.popPose();
        }

        private void spawnPrepareParticles(ClientLevel level, int lifetime) {
            float progress = age / (float) lifetime;
            RandomSource random = level.random;
            for (int i = 0; i < height; i += 3) {
                Vec3 center = position.add(0.0, i + random.nextFloat() - 0.5f, 0.0);
                float angle = random.nextFloat() * Mth.TWO_PI + age * 0.45f;
                Vec3 spawnOffset = new Vec3(Mth.cos(angle) * 2.0f, 0.0, Mth.sin(angle) * 2.0f);
                Vec3 spawn = center.add(spawnOffset);
                spawnDust(level, spawn, Vec3.ZERO, style.primaryDust, 0.42f * Math.max(0.08f, progress));
            }
        }

        private void spawnBurstParticles(ClientLevel level) {
            RandomSource random = level.random;
            for (int i = 0; i < 36; i++) {
                float angle = random.nextFloat() * Mth.TWO_PI;
                float speed = 0.6f * random.nextFloat();
                Vec3 velocity = new Vec3(Mth.cos(angle) * speed, random.nextFloat() * 0.3f, Mth.sin(angle) * speed);
                spawnDust(level, position, velocity, style.primaryDust, 0.5f);
            }

            for (int pass = 0; pass < 2; pass++) {
                for (int i = 0; i < height; i += 3) {
                    Vec3 spawn = position.add(0.0, i + random.nextFloat() - 0.5f, 0.0);
                    float angle = random.nextFloat() * Mth.TWO_PI;
                    float speed = 2.0f * (random.nextFloat() * 0.5f + 0.5f);
                    Vec3 velocity = new Vec3(Mth.cos(angle) * speed, 0.0, Mth.sin(angle) * speed);
                    spawnDust(level, spawn, velocity, style.secondaryDust, 0.48f);
                }
            }
        }
    }

    private static final class StrikeVerticalLaser extends VerticalLaserInstance {
        private StrikeVerticalLaser(Vec3 position, float height, LaserStyle style) {
            super(position, height, style);
        }

        @Override
        protected void tick(ClientLevel level) {
            age++;
        }

        @Override
        protected void render(PoseStack poseStack, MultiBufferSource bufferSource, Camera camera, float partialTicks) {
            float alpha = strikeAlpha(age);
            renderStrikeColumn(poseStack, bufferSource, camera, alpha, partialTicks);
            renderImpactFlare(poseStack, bufferSource, camera, alpha, partialTicks);
        }

        @Override
        protected boolean isExpired() {
            return age >= STRIKE_TIME;
        }

        private float strikeAlpha(float strikeAge) {
            if (strikeAge < STRIKE_IN) {
                return STRIKE_IN == 0 ? 1.0f : easeOut(strikeAge / STRIKE_IN);
            }
            if (strikeAge < STRIKE_IN + STRIKE_STAY) {
                return 1.0f;
            }
            return reversedEaseOut((strikeAge - STRIKE_IN - STRIKE_STAY) / STRIKE_OUT);
        }
    }

    private static void renderGradientColumn(Matrix4f matrix, VertexConsumer vertex, float height, float width, float zOffset, float red, float green, float blue, float alpha) {
        vertex.addVertex(matrix, 0.0f, 0.0f, zOffset).setColor(red, green, blue, alpha);
        vertex.addVertex(matrix, 0.0f, height, zOffset).setColor(red, green, blue, alpha);
        vertex.addVertex(matrix, -width, height, zOffset).setColor(red, green, blue, 0.0f);
        vertex.addVertex(matrix, -width, 0.0f, zOffset).setColor(red, green, blue, 0.0f);

        vertex.addVertex(matrix, width, 0.0f, zOffset).setColor(red, green, blue, 0.0f);
        vertex.addVertex(matrix, width, height, zOffset).setColor(red, green, blue, 0.0f);
        vertex.addVertex(matrix, 0.0f, height, zOffset).setColor(red, green, blue, alpha);
        vertex.addVertex(matrix, 0.0f, 0.0f, zOffset).setColor(red, green, blue, alpha);
    }

    private static void renderStyledBeam(Matrix4f matrix, VertexConsumer vertex, float height, float width, float alpha, LaserStyle style) {
        if (style == LaserStyle.LIGHTNING) {
            renderGradientColumn(matrix, vertex, height, width * 1.7f, -0.055f, 1.0f, 0.35f, 0.02f, alpha * 0.2f);
            renderGradientColumn(matrix, vertex, height, width * 1.0f, -0.025f, 1.0f, 0.68f, 0.05f, alpha * 0.36f);
            renderGradientColumn(matrix, vertex, height, width * 0.5f, 0.0f, 1.0f, 0.94f, 0.18f, alpha * 0.62f);
            renderGradientColumn(matrix, vertex, height, width * 0.18f, 0.025f, 1.0f, 1.0f, 0.7f, alpha * 0.72f);
            renderGradientColumn(matrix, vertex, height, width * 0.06f, 0.045f, 1.0f, 1.0f, 1.0f, alpha * 0.55f);
            return;
        }

        renderGradientColumn(matrix, vertex, height, width * 2.1f, -0.055f, 0.48f, 0.02f, 1.0f, alpha * 0.26f);
        renderGradientColumn(matrix, vertex, height, width * 1.45f, -0.025f, 1.0f, 0.02f, 0.72f, alpha * 0.5f);
        renderGradientColumn(matrix, vertex, height, width * 0.82f, 0.0f, 1.0f, 0.12f, 0.92f, alpha * 0.94f);
        renderGradientColumn(matrix, vertex, height, width * 0.32f, 0.025f, 0.9f, 0.72f, 1.0f, alpha);
        renderGradientColumn(matrix, vertex, height, width * 0.11f, 0.045f, 1.0f, 1.0f, 1.0f, alpha);
    }

    private static void renderBeamNoise(Matrix4f matrix, VertexConsumer vertex, float height, float width, float alpha, LaserStyle style, float time, int seed) {
        if (height <= 0.001f || width <= 0.001f || alpha <= 0.001f) {
            return;
        }

        int bands = style.lightning ? 14 : 22;
        float speed = style.lightning ? 0.055f : 0.032f;
        for (int i = 0; i < bands; i++) {
            float stream = wrap01(i * 0.173f + time * speed + seed * 0.011f);
            float yCenter = stream * height;
            float bandHeight = height * (style.lightning ? 0.032f : 0.045f) * (0.65f + hashUnit(seed, i, 11) * 0.8f);
            float lane = hashSigned(seed * 17L, seed, i) * width * (style.lightning ? 0.55f : 0.38f);
            float bandWidth = width * (style.lightning ? 0.12f : 0.18f) * (0.75f + hashUnit(seed, i, 23) * 0.8f);
            float pulse = 0.5f + 0.5f * Mth.sin(time * (style.lightning ? 0.72f : 0.38f) + i * 1.87f);
            float bandAlpha = alpha * (0.2f + pulse * 0.55f);
            float z = -0.03f + hashSigned(seed * 29L, seed, i) * 0.08f;

            if (style.lightning) {
                if (i % 3 == 0) {
                    renderNoiseDash(matrix, vertex, lane, yCenter, bandWidth, bandHeight, z, 1.0f, 1.0f, 0.86f, bandAlpha);
                } else if (i % 3 == 1) {
                    renderNoiseDash(matrix, vertex, lane, yCenter, bandWidth * 1.25f, bandHeight, z, 1.0f, 0.72f, 0.06f, bandAlpha * 0.95f);
                } else {
                    renderNoiseDash(matrix, vertex, lane, yCenter, bandWidth * 1.45f, bandHeight, z, 1.0f, 0.28f, 0.02f, bandAlpha * 0.72f);
                }
            } else if (i % 4 == 0) {
                renderNoiseDash(matrix, vertex, lane, yCenter, bandWidth * 0.9f, bandHeight, z, 1.0f, 0.82f, 1.0f, bandAlpha);
            } else if (i % 4 == 1) {
                renderNoiseDash(matrix, vertex, lane, yCenter, bandWidth * 1.2f, bandHeight, z, 1.0f, 0.08f, 0.88f, bandAlpha * 0.95f);
            } else if (i % 4 == 2) {
                renderNoiseDash(matrix, vertex, lane, yCenter, bandWidth * 1.35f, bandHeight, z, 0.46f, 0.04f, 1.0f, bandAlpha * 0.82f);
            } else {
                renderNoiseDash(matrix, vertex, lane, yCenter, bandWidth * 0.75f, bandHeight, z, 0.25f, 0.9f, 1.0f, bandAlpha * 0.58f);
            }
        }
    }

    private static void renderNoiseDash(Matrix4f matrix, VertexConsumer vertex, float centerX, float centerY, float halfWidth, float halfHeight, float z, float red, float green, float blue, float alpha) {
        float y0 = centerY - halfHeight;
        float y1 = centerY + halfHeight;
        vertex.addVertex(matrix, centerX - halfWidth, y0, z).setColor(red, green, blue, 0.0f);
        vertex.addVertex(matrix, centerX - halfWidth, y1, z).setColor(red, green, blue, 0.0f);
        vertex.addVertex(matrix, centerX, y1, z).setColor(red, green, blue, alpha);
        vertex.addVertex(matrix, centerX, y0, z).setColor(red, green, blue, alpha);

        vertex.addVertex(matrix, centerX, y0, z).setColor(red, green, blue, alpha);
        vertex.addVertex(matrix, centerX, y1, z).setColor(red, green, blue, alpha);
        vertex.addVertex(matrix, centerX + halfWidth, y1, z).setColor(red, green, blue, 0.0f);
        vertex.addVertex(matrix, centerX + halfWidth, y0, z).setColor(red, green, blue, 0.0f);
    }

    private static void renderGroundTextureLayer(PoseStack poseStack, VertexConsumer vertex, float size, float rotationDegrees, float red, float green, float blue, float alpha) {
        if (size <= 0.001f || alpha <= 0.001f) {
            return;
        }

        poseStack.pushPose();
        poseStack.mulPose(Axis.YP.rotationDegrees(rotationDegrees));
        poseStack.scale(size, size, size);
        renderGroundTextureQuad(poseStack.last().pose(), vertex, red, green, blue, alpha);
        poseStack.popPose();
    }

    private static void renderGroundTextureQuad(Matrix4f matrix, VertexConsumer vertex, float red, float green, float blue, float alpha) {
        int light = LightTexture.FULL_BRIGHT;
        addTexturedGroundVertex(vertex, matrix, -0.5f, 0.5f, 0.0f, 1.0f, red, green, blue, alpha, light);
        addTexturedGroundVertex(vertex, matrix, 0.5f, 0.5f, 1.0f, 1.0f, red, green, blue, alpha, light);
        addTexturedGroundVertex(vertex, matrix, 0.5f, -0.5f, 1.0f, 0.0f, red, green, blue, alpha, light);
        addTexturedGroundVertex(vertex, matrix, -0.5f, -0.5f, 0.0f, 0.0f, red, green, blue, alpha, light);
    }

    private static void addTexturedGroundVertex(VertexConsumer vertex, Matrix4f matrix, float x, float z, float u, float v, float red, float green, float blue, float alpha, int light) {
        vertex.addVertex(matrix, x, 0.0f, z)
                .setColor(red, green, blue, alpha)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0f, 1.0f, 0.0f);
    }

    private static void renderFastLightning(
            Matrix4f matrix,
            VertexConsumer vertex,
            float height,
            float lightningWidth,
            float spread,
            int segments,
            long levelTime,
            int seed,
            float red,
            float green,
            float blue,
            float alpha
    ) {
        if (segments <= 0 || lightningWidth <= 0.001f || alpha <= 0.001f) {
            return;
        }

        float previousX = 0.0f;
        float previousY = 0.0f;
        for (int i = 1; i <= segments; i++) {
            float nextY = height * i / segments;
            float endWeight = i == segments ? 0.0f : Mth.sin(Mth.PI * i / segments);
            float nextX = hashSigned(levelTime, seed, i) * spread * endWeight;
            renderFastLightningSegment(matrix, vertex, previousX, previousY, nextX, nextY, lightningWidth, red, green, blue, alpha);
            previousX = nextX;
            previousY = nextY;
        }
    }

    private static void renderFastLightningSegment(Matrix4f matrix, VertexConsumer vertex, float x0, float y0, float x1, float y1, float width, float red, float green, float blue, float alpha) {
        float dx = x1 - x0;
        float dy = y1 - y0;
        float length = Mth.sqrt(dx * dx + dy * dy);
        if (length <= 1.0E-4f) {
            return;
        }

        float nx = -dy / length * width;
        float ny = dx / length * width;

        vertex.addVertex(matrix, x0, y0, 0.0f).setColor(red, green, blue, alpha);
        vertex.addVertex(matrix, x0 + nx, y0 + ny, 0.0f).setColor(red, green, blue, 0.0f);
        vertex.addVertex(matrix, x1 + nx, y1 + ny, 0.0f).setColor(red, green, blue, 0.0f);
        vertex.addVertex(matrix, x1, y1, 0.0f).setColor(red, green, blue, alpha);

        vertex.addVertex(matrix, x0, y0, 0.0f).setColor(red, green, blue, alpha);
        vertex.addVertex(matrix, x0 - nx, y0 - ny, 0.0f).setColor(red, green, blue, 0.0f);
        vertex.addVertex(matrix, x1 - nx, y1 - ny, 0.0f).setColor(red, green, blue, 0.0f);
        vertex.addVertex(matrix, x1, y1, 0.0f).setColor(red, green, blue, alpha);
    }

    private static float hashSigned(long time, int seed, int index) {
        int hash = seed;
        hash ^= (int) (time * 73428767L);
        hash ^= index * 0x9E3779B9;
        hash ^= hash >>> 16;
        hash *= 0x7FEB352D;
        hash ^= hash >>> 15;
        hash *= 0x846CA68B;
        hash ^= hash >>> 16;
        return ((hash & 0xFFFF) / 32767.5f) - 1.0f;
    }

    private static float hashUnit(int seed, int index, int salt) {
        return (hashSigned(salt, seed, index) + 1.0f) * 0.5f;
    }

    private static float wrap01(float value) {
        return value - Mth.floor(value);
    }
}
