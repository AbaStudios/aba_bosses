package com.abastudios.bosses.client.test;

import com.abastudios.bosses.AbaBosses;
import dev.architectury.event.events.client.ClientTickEvent;
import dev.architectury.platform.Platform;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

/**
 * Executes an opt-in Java test script after the client joins a world.
 */
public final class ClientTestScriptRunner {
    private static List<Step> steps = List.of();
    private static int stepIndex;
    private static int waitTicks;
    private static boolean loadedForWorld;
    private static boolean running;

    private ClientTestScriptRunner() {
    }

    public static void init() {
        if (Platform.isDevelopmentEnvironment()) {
            ClientTickEvent.CLIENT_POST.register(ClientTestScriptRunner::tick);
        }
    }

    private static void tick(Minecraft minecraft) {
        if (minecraft.level == null || minecraft.player == null || minecraft.player.connection == null) {
            loadedForWorld = false;
            running = false;
            steps = List.of();
            return;
        }
        if (!loadedForWorld) {
            loadedForWorld = true;
            Script script = new Script();
            if (ClientTestScript.ENABLED) {
                ClientTestScript.configure(script);
            }
            steps = List.copyOf(script.steps);
            stepIndex = 0;
            waitTicks = 0;
            running = !steps.isEmpty();
            AbaBosses.LOGGER.info("Started client test script steps={}", steps.size());
            return;
        }
        if (!running) {
            return;
        }
        if (waitTicks > 0) {
            waitTicks--;
            return;
        }
        if (stepIndex >= steps.size()) {
            running = false;
            AbaBosses.LOGGER.info("Completed client test script steps={}", steps.size());
            return;
        }

        Step step = steps.get(stepIndex++);
        if (step.waitTicks() > 0) {
            waitTicks = step.waitTicks();
            return;
        }
        AbaBosses.LOGGER.info("Client test action={}", step.description());
        step.action().accept(minecraft);
    }

    private static void logCamera(Minecraft minecraft) {
        Camera camera = minecraft.gameRenderer.getMainCamera();
        Vec3 position = camera.getPosition();
        AbaBosses.LOGGER.info(
                "Client test camera position=({}, {}, {}) command={}",
                position.x,
                position.y,
                position.z,
                String.format(
                        Locale.ROOT,
                        "/tp @s %.2f %.2f %.2f %.2f %.2f",
                        minecraft.player.getX(),
                        minecraft.player.getY(),
                        minecraft.player.getZ(),
                        camera.getYRot(),
                        camera.getXRot()
                )
        );
    }

    private record Step(int waitTicks, String description, Consumer<Minecraft> action) {
    }

    static final class Script {
        private final List<Step> steps = new ArrayList<>();

        Script waitSeconds(double seconds) {
            this.steps.add(new Step((int) Math.ceil(seconds * 20.0), "wait", minecraft -> {
            }));
            return this;
        }

        Script command(String command) {
            String normalized = command.startsWith("/") ? command.substring(1) : command;
            return this.run("command /" + normalized, minecraft -> minecraft.player.connection.sendCommand(normalized));
        }

        Script logCamera() {
            return this.run("log camera", ClientTestScriptRunner::logCamera);
        }

        Script screenshot(String name) {
            return this.run("screenshot " + name, minecraft -> Screenshot.grab(
                    minecraft.gameDirectory,
                    name,
                    minecraft.getMainRenderTarget(),
                    result -> AbaBosses.LOGGER.info("Client test screenshot result={}", result.getString())
            ));
        }

        private Script run(String description, Consumer<Minecraft> action) {
            this.steps.add(new Step(0, description, action));
            return this;
        }
    }
}
