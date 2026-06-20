package com.abastudios.bosses.client.render;

import com.abastudios.bosses.AbaBosses;
import com.abastudios.bosses.entity.BlackHoleEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.client.renderer.culling.Frustum;

/**
 * Empty renderer for the black hole anchor entity.
 */
@Environment(EnvType.CLIENT)
public final class BlackHoleRenderer extends EntityRenderer<BlackHoleEntity> {
    private static final ResourceLocation EMPTY_TEXTURE = AbaBosses.location("textures/vfx/black_hole/empty.png");

    public BlackHoleRenderer(EntityRendererProvider.Context context) {
        super(context);
        this.shadowRadius = 0.0f;
    }

    @Override
    public void render(BlackHoleEntity entity, float entityYaw, float partialTick, PoseStack poseStack, MultiBufferSource bufferSource, int packedLight) {
        super.render(entity, entityYaw, partialTick, poseStack, bufferSource, packedLight);
    }

    @Override
    public boolean shouldRender(BlackHoleEntity entity, Frustum frustum, double cameraX, double cameraY, double cameraZ) {
        return true;
    }

    @Override
    public ResourceLocation getTextureLocation(BlackHoleEntity entity) {
        return EMPTY_TEXTURE;
    }
}
