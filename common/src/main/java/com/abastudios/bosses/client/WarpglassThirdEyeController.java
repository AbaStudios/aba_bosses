package com.abastudios.bosses.client;

import com.abastudios.bosses.registry.AbaBossesItems;
import dev.architectury.event.events.client.ClientTickEvent;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;

/**
 * Holds the camera behind the player while the Warpglass Wand's third eye is active.
 */
public final class WarpglassThirdEyeController {
    private static CameraType previousCameraType;

    private WarpglassThirdEyeController() {
    }

    /**
     * Registers the held-input camera handler.
     */
    public static void register() {
        ClientTickEvent.CLIENT_POST.register(WarpglassThirdEyeController::tick);
    }

    private static void tick(Minecraft minecraft) {
        boolean active = minecraft.player != null
                && minecraft.screen == null
                && minecraft.mouseHandler.isMouseGrabbed()
                && minecraft.options.keyAttack.isDown()
                && minecraft.player.getMainHandItem().is(AbaBossesItems.WARPGLASS_WAND.get());

        if (active) {
            if (previousCameraType == null) {
                previousCameraType = minecraft.options.getCameraType();
            }
            setCameraType(minecraft, CameraType.THIRD_PERSON_BACK);
        } else if (previousCameraType != null) {
            setCameraType(minecraft, previousCameraType);
            previousCameraType = null;
        }
    }

    private static void setCameraType(Minecraft minecraft, CameraType cameraType) {
        CameraType currentCameraType = minecraft.options.getCameraType();
        if (currentCameraType == cameraType) {
            return;
        }

        minecraft.options.setCameraType(cameraType);
        if (currentCameraType.isFirstPerson() != cameraType.isFirstPerson()) {
            minecraft.gameRenderer.checkEntityPostEffect(cameraType.isFirstPerson() ? minecraft.getCameraEntity() : null);
        }
    }
}
