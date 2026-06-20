package com.abastudios.bosses.client.render;

import com.abastudios.bosses.AbaBosses;
import com.abastudios.bosses.entity.BlackHoleEntity;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.PostChain;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector4f;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Full-screen black hole lensing pass driven by summonable black hole entities.
 */
@Environment(EnvType.CLIENT)
public final class BlackHolePostEffect {
    private static PostChain postChain;
    private static int width;
    private static int height;
    private static boolean loadFailed;
    private static final Map<Integer, String> LAST_PROJECTION_STATE = new HashMap<>();
    private static int lastBlackHoleCount = -1;
    private static int lastSelectedBlackHoleId = -1;
    private static boolean lastProjectionActive;

    private BlackHolePostEffect() {
    }

    /**
     * Applies framebuffer lensing for the closest visible black hole.
     */
    public static void process(Camera camera, Matrix4f modelViewMatrix, Matrix4f projectionMatrix, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        if (minecraft.level == null || minecraft.player == null) {
            return;
        }

        SelectionTrace trace = findVisibleBlackHole(camera, projectionMatrix, partialTick);
        if (trace.blackHoleCount != lastBlackHoleCount) {
            lastBlackHoleCount = trace.blackHoleCount;
            AbaBosses.LOGGER.info("Black hole render set size={} selectedId={} projectedCount={}", trace.blackHoleCount, trace.selectedBlackHoleId, trace.projectedCount);
        }

        boolean projectionActive = trace.projection != null;
        if (projectionActive != lastProjectionActive) {
            lastProjectionActive = projectionActive;
            AbaBosses.LOGGER.info(
                    "Black hole post effect active={} count={} projectedCount={} selectedId={}",
                    projectionActive,
                    trace.blackHoleCount,
                    trace.projectedCount,
                    trace.selectedBlackHoleId
            );
        }

        if (trace.projection == null) {
            return;
        }

        if (trace.selectedBlackHoleId != lastSelectedBlackHoleId) {
            lastSelectedBlackHoleId = trace.selectedBlackHoleId;
            AbaBosses.LOGGER.info(
                    "Black hole post effect selected id={} worldScale={} occlusionRadius={} cameraLocal={} cameraForwardLocal={}",
                    trace.selectedBlackHoleId,
                    trace.projection.worldScale,
                    trace.projection.occlusionRadius,
                    trace.projection.cameraLocal,
                    trace.projection.cameraForwardLocal
            );
        }

        PostChain chain = getOrCreatePostChain(minecraft);
        if (chain == null) {
            return;
        }

        Projection projection = trace.projection;
        chain.setUniform("BlackHoleActive", 1.0f);
        setVec3(chain, "BlackHoleCameraLocal", projection.cameraLocal);
        setVec3(chain, "BlackHoleCameraRightLocal", projection.cameraRightLocal);
        setVec3(chain, "BlackHoleCameraUpLocal", projection.cameraUpLocal);
        setVec3(chain, "BlackHoleCameraForwardLocal", projection.cameraForwardLocal);
        chain.setUniform("BlackHoleProjectionScaleX", projection.projectionScaleX);
        chain.setUniform("BlackHoleProjectionScaleY", projection.projectionScaleY);
        chain.setUniform("BlackHoleDepthA", projection.depthA);
        chain.setUniform("BlackHoleDepthB", projection.depthB);
        chain.setUniform("BlackHoleOcclusionRadius", projection.occlusionRadius);
        chain.setUniform("BlackHoleWorldScale", projection.worldScale);
        chain.setUniform("BlackHoleTime", (minecraft.level.getGameTime() + partialTick) / 20.0f);
        chain.process(partialTick);
        minecraft.getMainRenderTarget().bindWrite(false);
    }

    private static SelectionTrace findVisibleBlackHole(Camera camera, Matrix4f projectionMatrix, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        Vec3 cameraPosition = camera.getPosition();
        Projection closest = null;
        double closestDistance = Double.MAX_VALUE;
        int blackHoleCount = 0;
        int projectedCount = 0;
        int selectedBlackHoleId = -1;

        for (Entity entity : minecraft.level.entitiesForRendering()) {
            if (!(entity instanceof BlackHoleEntity blackHole)) {
                continue;
            }
            blackHoleCount++;

            Vec3 position = blackHole.getPosition(partialTick);
            double distance = position.distanceToSqr(cameraPosition);
            if (distance >= closestDistance) {
                continue;
            }

            Projection projected = projectBlackHole(blackHole, position, camera, projectionMatrix, partialTick);
            if (projected != null) {
                projectedCount++;
                closest = projected;
                closestDistance = distance;
                selectedBlackHoleId = blackHole.getId();
            }
        }

        return new SelectionTrace(closest, blackHoleCount, projectedCount, selectedBlackHoleId);
    }

    private static Projection projectBlackHole(BlackHoleEntity blackHole, Vec3 position, Camera camera, Matrix4f projectionMatrix, float partialTick) {
        Minecraft minecraft = Minecraft.getInstance();
        int targetWidth = minecraft.getMainRenderTarget().width;
        int targetHeight = minecraft.getMainRenderTarget().height;

        Vec3 cameraPosition = camera.getPosition();
        float sizeScale = blackHole.getSizeScale();
        float shadowRadiusBlocks = BlackHoleEntity.SHADOW_RADIUS * sizeScale;
        float effectRadiusBlocks = BlackHoleEntity.EFFECT_RADIUS * sizeScale;
        LocalBasis localBasis = createLocalBasis(blackHole, partialTick);
        Vec3 cameraLocal = toLocal(cameraPosition.subtract(position), localBasis);
        Vec3 cameraRightLocal = toLocal(new Vec3(camera.getLeftVector()).reverse(), localBasis);
        Vec3 cameraUpLocal = toLocal(new Vec3(camera.getUpVector()), localBasis);
        Vec3 cameraForwardLocal = toLocal(new Vec3(camera.getLookVector()), localBasis);
        DepthMapping depthMapping = createDepthMapping(camera, projectionMatrix, targetWidth, targetHeight);
        if (depthMapping == null) {
            logProjectionState(blackHole, "depth_mapping_failed", position, sizeScale, shadowRadiusBlocks, effectRadiusBlocks);
            return null;
        }

        logProjectionState(blackHole, "projected", position, sizeScale, shadowRadiusBlocks, effectRadiusBlocks);

        return new Projection(
                cameraLocal,
                cameraRightLocal,
                cameraUpLocal,
                cameraForwardLocal,
                projectionMatrix.m00(),
                projectionMatrix.m11(),
                depthMapping.a,
                depthMapping.b,
                shadowRadiusBlocks,
                sizeScale
        );
    }

    private static ProjectedPoint project(Vec3 cameraRelativePosition, Camera camera, Matrix4f projectionMatrix, int targetWidth, int targetHeight) {
        Vec3 right = new Vec3(camera.getLeftVector()).reverse();
        Vec3 up = new Vec3(camera.getUpVector());
        Vec3 forward = new Vec3(camera.getLookVector());
        double forwardDistance = cameraRelativePosition.dot(forward);
        if (forwardDistance <= 1.0e-4) {
            return null;
        }

        double viewX = cameraRelativePosition.dot(right);
        double viewY = cameraRelativePosition.dot(up);
        Vector4f clip = new Vector4f((float) viewX, (float) viewY, (float) -forwardDistance, 1.0f);
        projectionMatrix.transform(clip);
        if (clip.w() <= 0.0f) {
            return null;
        }

        float ndcX = clip.x() / clip.w();
        float ndcY = clip.y() / clip.w();
        if (!Float.isFinite(ndcX) || !Float.isFinite(ndcY)) {
            return null;
        }

        return new ProjectedPoint(
                (ndcX * 0.5f + 0.5f) * targetWidth,
                (ndcY * 0.5f + 0.5f) * targetHeight,
                clip.z() / clip.w() * 0.5f + 0.5f
        );
    }

    private static DepthMapping createDepthMapping(Camera camera, Matrix4f projectionMatrix, int targetWidth, int targetHeight) {
        ProjectedPoint near = project(new Vec3(camera.getLookVector()).scale(1.0), camera, projectionMatrix, targetWidth, targetHeight);
        ProjectedPoint far = project(new Vec3(camera.getLookVector()).scale(256.0), camera, projectionMatrix, targetWidth, targetHeight);
        if (near == null || far == null) {
            return null;
        }

        float inverseNearDistance = 1.0f;
        float inverseFarDistance = 1.0f / 256.0f;
        float b = (near.depth - far.depth) / (inverseNearDistance - inverseFarDistance);
        float a = near.depth - b * inverseNearDistance;
        if (!Float.isFinite(a) || !Float.isFinite(b) || Math.abs(b) < 1.0e-6f) {
            return null;
        }

        return new DepthMapping(a, b);
    }

    private static PostChain getOrCreatePostChain(Minecraft minecraft) {
        int targetWidth = minecraft.getMainRenderTarget().width;
        int targetHeight = minecraft.getMainRenderTarget().height;

        if (postChain != null) {
            if (targetWidth != width || targetHeight != height) {
                width = targetWidth;
                height = targetHeight;
                postChain.resize(targetWidth, targetHeight);
                AbaBosses.LOGGER.info("Resized black hole post chain to {}x{}", targetWidth, targetHeight);
            }
            return postChain;
        }
        if (loadFailed) {
            return null;
        }

        try {
            postChain = new PostChain(
                    minecraft.getTextureManager(),
                    minecraft.getResourceManager(),
                    minecraft.getMainRenderTarget(),
                    AbaBosses.location("shaders/post/black_hole.json")
            );
            width = targetWidth;
            height = targetHeight;
            postChain.resize(targetWidth, targetHeight);
            AbaBosses.LOGGER.info("Loaded black hole post chain at {}x{}", targetWidth, targetHeight);
            return postChain;
        } catch (IOException exception) {
            AbaBosses.LOGGER.error("Failed to load black hole post shader", exception);
            loadFailed = true;
            return null;
        }
    }

    private static LocalBasis createLocalBasis(BlackHoleEntity blackHole, float partialTick) {
        float yaw = blackHole.getViewYRot(partialTick);
        float pitch = blackHole.getViewXRot(partialTick);
        Vec3 horizontalForward = Vec3.directionFromRotation(0.0f, yaw).normalize();
        Vec3 right = new Vec3(horizontalForward.z, 0.0, -horizontalForward.x).normalize();
        Vec3 forward = Vec3.directionFromRotation(pitch, yaw).normalize();
        Vec3 up = forward.cross(right).normalize();
        return new LocalBasis(right, up, forward);
    }

    private static Vec3 toLocal(Vec3 vector, LocalBasis localBasis) {
        return new Vec3(
                vector.dot(localBasis.right),
                vector.dot(localBasis.up),
                vector.dot(localBasis.forward)
        );
    }

    private static void setVec3(PostChain chain, String name, Vec3 vector) {
        chain.setUniform(name + "X", (float) vector.x);
        chain.setUniform(name + "Y", (float) vector.y);
        chain.setUniform(name + "Z", (float) vector.z);
    }

    private static void logProjectionState(BlackHoleEntity blackHole, String state, Vec3 position, float sizeScale, float shadowRadiusBlocks, float effectRadiusBlocks) {
        int id = blackHole.getId();
        String previous = LAST_PROJECTION_STATE.put(id, state);
        if (!state.equals(previous)) {
            AbaBosses.LOGGER.info(
                    "Black hole projection id={} state={} pos={} size={} shadowRadiusBlocks={} effectRadiusBlocks={}",
                    id,
                    state,
                    position,
                    sizeScale,
                    shadowRadiusBlocks,
                    effectRadiusBlocks
            );
        }
    }

    private record LocalBasis(Vec3 right, Vec3 up, Vec3 forward) {
    }

    private record ProjectedPoint(float x, float y, float depth) {
    }

    private record DepthMapping(float a, float b) {
    }

    private record SelectionTrace(Projection projection, int blackHoleCount, int projectedCount, int selectedBlackHoleId) {
    }

    private record Projection(
            Vec3 cameraLocal,
            Vec3 cameraRightLocal,
            Vec3 cameraUpLocal,
            Vec3 cameraForwardLocal,
            float projectionScaleX,
            float projectionScaleY,
            float depthA,
            float depthB,
            float occlusionRadius,
            float worldScale
    ) {
    }
}
