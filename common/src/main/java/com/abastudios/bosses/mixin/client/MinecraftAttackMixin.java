package com.abastudios.bosses.mixin.client;

import com.abastudios.bosses.registry.AbaBossesItems;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Gives held left click to the Warpglass Wand instead of vanilla attack handling.
 */
@Mixin(Minecraft.class)
public abstract class MinecraftAttackMixin {
    @Inject(method = "startAttack", at = @At("HEAD"), cancellable = true)
    private void abaBosses$preventWarpglassAttack(CallbackInfoReturnable<Boolean> callback) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (isHoldingWarpglassWand(minecraft)) {
            callback.setReturnValue(false);
        }
    }

    @Inject(method = "continueAttack", at = @At("HEAD"), cancellable = true)
    private void abaBosses$preventWarpglassBlockBreaking(boolean attackHeld, CallbackInfo callback) {
        Minecraft minecraft = (Minecraft) (Object) this;
        if (isHoldingWarpglassWand(minecraft)) {
            if (minecraft.gameMode != null) {
                minecraft.gameMode.stopDestroyBlock();
            }
            callback.cancel();
        }
    }

    private static boolean isHoldingWarpglassWand(Minecraft minecraft) {
        return minecraft.player != null
                && minecraft.player.getMainHandItem().is(AbaBossesItems.WARPGLASS_WAND.get());
    }
}
