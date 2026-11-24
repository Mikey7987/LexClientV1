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
