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

import java.util.Comparator;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

public final class BenchAssistClient implements ClientModInitializer {
    private final AssistConfig config = new AssistConfig();
    private KeyBinding guiKeyBinding;
    private long nextTriggerTimeMs = 0L;
    private int lockedEntityId = -1;

    @Override
    public void onInitializeClient() {
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
        Box searchBox = player.getBoundingBox().expand(config.maxReach + 2.0);

        // Keep current locked target if still valid and in reach; otherwise pick closest in FOV
        Entity locked = lockedEntityId != -1 ? client.world.getEntityById(lockedEntityId) : null;
        Optional<Entity> chosen = Optional.empty();

        if (locked instanceof LivingEntity livingLocked
                && livingLocked.isAlive()
                && !livingLocked.isSpectator()
                && eyePos.distanceTo(RotationMath.aimPoint(locked)) <= config.maxReach + 0.6) {
            float[] lockedAngles = RotationMath.anglesToPoint(
                    eyePos, RotationMath.aimPoint(locked), player.getYaw(), player.getPitch());
            if (Math.hypot(lockedAngles[0], lockedAngles[1]) <= config.fovConeDeg * 1.15) {
                chosen = Optional.of(locked);
            }
        }

        if (chosen.isEmpty()) {
            chosen = client.world.getOtherEntities(player, searchBox, e ->
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
        }

        if (chosen.isEmpty()) {
            lockedEntityId = -1;
            return;
        }

        Entity target = chosen.get();
        lockedEntityId = target.getId();

        float[] deltas = RotationMath.anglesToPoint(
                eyePos, RotationMath.aimPoint(target), player.getYaw(), player.getPitch());
        double angularDist = Math.hypot(deltas[0], deltas[1]);
        if (angularDist > config.fovConeDeg) return;

        // Deadzone prevents micro-oscillation right on top of the target
        float deadzoneDeg = 0.35f;
        float yawDelta = Math.abs(deltas[0]) > deadzoneDeg ? deltas[0] : 0.0f;
        float pitchDelta = Math.abs(deltas[1]) > deadzoneDeg ? deltas[1] : 0.0f;

        float t = MathHelper.clamp(config.smoothFactor, 0.02f, 0.65f);
        float nextYaw = player.getYaw() + yawDelta * t;
        float nextPitch = MathHelper.clamp(player.getPitch() + pitchDelta * t, -90.0f, 90.0f);

        player.setYaw(nextYaw);
        player.setPitch(nextPitch);
    }

    private void runTriggerBot(MinecraftClient client) {
        PlayerEntity player = client.player;
        if (player == null || client.world == null || client.interactionManager == null) return;

        float cooldown = player.getAttackCooldownProgress(0.5f);
        if (cooldown < config.minCooldownStrength) return;

        long now = System.currentTimeMillis();
        if (now < nextTriggerTimeMs) return;

        Entity target = null;
        HitResult hit = client.crosshairTarget;
        if (hit instanceof EntityHitResult entityHit) {
            target = entityHit.getEntity();
        }

        // Fallback direct raycast check if crosshairTarget missed on touch/camera update
        if (target == null) {
            Vec3d eyePos = player.getEyePos();
            Vec3d lookVec = player.getRotationVec(1.0f);
            Box box = player.getBoundingBox().expand(config.maxReach + 1.0);
            target = client.world.getOtherEntities(player, box, e ->
                            e instanceof LivingEntity living
                                    && living.isAlive()
                                    && !living.isSpectator()
                                    && e.canHit()
                                    && eyePos.distanceTo(RotationMath.aimPoint(e)) <= config.maxReach
                                    && RotationMath.isLookingAtBox(eyePos, lookVec, e.getBoundingBox(), config.maxReach))
                    .stream()
                    .min(Comparator.comparingDouble(e -> eyePos.distanceTo(RotationMath.aimPoint(e))))
                    .orElse(null);
        }

        if (!(target instanceof LivingEntity living) || !living.isAlive() || target.isSpectator()) {
            return;
        }

        if (player.distanceTo(target) > config.maxReach + 0.5) return;

        ThreadLocalRandom rng = ThreadLocalRandom.current();
        int minMs = Math.max(15, config.minAttackDelayMs);
        int maxMs = Math.max(minMs + 10, config.maxAttackDelayMs);
        nextTriggerTimeMs = now + rng.nextInt(minMs, maxMs + 1);

        client.interactionManager.attackEntity(player, target);
        player.swingHand(Hand.MAIN_HAND);
        player.resetLastAttackedTicks();
    }
}
