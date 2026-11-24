package com.bench.assist;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.lwjgl.glfw.GLFW;

import java.awt.Robot;
import java.awt.event.InputEvent;
import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class BenchAssistClient implements ClientModInitializer {
    private final AssistConfig config = new AssistConfig();
    private KeyBinding guiKeyBinding;
    private Robot externalRobot;
    private long nextTriggerTimeMs = 0L;

    @Override
    public void onInitializeClient() {
        try {
            this.externalRobot = new Robot();
            this.externalRobot.setAutoDelay(0);
        } catch (Exception ignored) {
            this.externalRobot = null;
        }

        this.guiKeyBinding = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.benchassist.gui",
                InputUtil.Type.KEYSYM,
                GLFW.GLFW_KEY_RIGHT_SHIFT,
                KeyBinding.Category.MISC
        ));

        ClientTickEvents.END_CLIENT_TICK.register(this::onClientTick);
    }

    private void onClientTick(MinecraftClient client) {
        while (guiKeyBinding.wasPressed()) {
            if (client.currentScreen == null) {
                client.setScreen(new ClickGuiScreen(config));
            }
        }

        if (client.player == null || client.world == null || client.currentScreen != null) {
            return;
        }

        if (config.keepSprintOnW && client.options.forwardKey.isPressed()) {
            if (!client.player.isSprinting() && client.player.getHungerManager().getFoodLevel() > 6) {
                client.player.setSprinting(true);
            }
        }

        if (config.aimAssistEnabled) {
            runAimAssist(client);
        }

        if (config.triggerBotEnabled) {
            runTriggerBot(client);
        }
    }

    private void runAimAssist(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || client.world == null) return;

        Vec3d eyePos = player.getEyePos();
        Box searchBox = player.getBoundingBox().expand(config.maxReach + 1.5);

        Optional<Entity> bestTarget = client.world.getOtherEntities(player, searchBox, e ->
                        e instanceof LivingEntity living
                                && living.isAlive()
                                && !living.isSpectator()
                                && e.canHit())
                .stream()
                .filter(e -> eyePos.distanceTo(RotationMath.aimPoint(e)) <= config.maxReach)
                .min(Comparator.comparingDouble(e -> {
                    float[] d = RotationMath.anglesToPoint(
                            eyePos, RotationMath.aimPoint(e), player.getYaw(), player.getPitch());
                    return Math.hypot(d[0], d[1]);
                }));

        if (bestTarget.isEmpty()) return;

        float[] deltas = RotationMath.anglesToPoint(
                eyePos, RotationMath.aimPoint(bestTarget.get()), player.getYaw(), player.getPitch());
        double angularDist = Math.hypot(deltas[0], deltas[1]);
        if (angularDist > config.fovConeDeg) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        float jitterYaw = (float) rng.nextGaussian() * (config.jitterDeg * 0.5f);
        float jitterPitch = (float) rng.nextGaussian() * (config.jitterDeg * 0.35f);

        float proximityScale = MathHelper.clamp((float) (angularDist / 14.0), 0.25f, 1.0f);
        float yawStep = RotationMath.stepToward(
                deltas[0], config.maxYawStepDeg * proximityScale, jitterYaw);
        float pitchStep = RotationMath.stepToward(
                deltas[1], config.maxPitchStepDeg * proximityScale, jitterPitch);

        player.setYaw(player.getYaw() + yawStep);
        player.setPitch(MathHelper.clamp(player.getPitch() + pitchStep, -90.0f, 90.0f));
    }

    private void runTriggerBot(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || client.interactionManager == null) return;

        float cooldown = player.getAttackCooldownProgress(0.5f);
        if (cooldown < config.minCooldownStrength) return;

        HitResult hit = client.crosshairTarget;
        if (!(hit instanceof EntityHitResult entityHit)) return;

        Entity target = entityHit.getEntity();
        if (!(target instanceof LivingEntity living) || !living.isAlive() || target.isSpectator()) {
            return;
        }

        if (player.distanceTo(target) > config.maxReach) return;

        long now = System.currentTimeMillis();
        if (now < nextTriggerTimeMs) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int delay = rng.nextInt(config.minAttackDelayMs, config.maxAttackDelayMs + 1);
        nextTriggerTimeMs = now + delay;

        if (externalRobot != null && client.isWindowFocused()) {
            externalRobot.mousePress(InputEvent.BUTTON1_DOWN_MASK);
            externalRobot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK);
        } else {
            client.interactionManager.attackEntity(player, target);
            player.swingHand(Hand.MAIN_HAND);
        }
    }
}
