package com.bench.assist;

public final class AssistConfig {
    public boolean aimAssistEnabled = true;
    public boolean triggerBotEnabled = true;
    public boolean keepSprintOnW = true;

    // 1.21x PvP cooldown threshold (0.0 - 1.0)
    public float minCooldownStrength = 0.92f;

    // Smoothness (0.05 = gentle glide, 0.50 = snappy), FOV cone, and reach
    public float smoothFactor = 0.16f;
    public float fovConeDeg = 45.0f;
    public double maxReach = 3.2;

    // TriggerBot delay window in ms
    public int minAttackDelayMs = 45;
    public int maxAttackDelayMs = 95;
}
