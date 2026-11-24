package com.bench.assist;

public final class AssistConfig {
    public boolean aimAssistEnabled = true;
    public boolean triggerBotEnabled = true;
    public boolean keepSprintOnW = true;

    // Aim Assist humanization settings
    public float smoothFactor = 0.16f;      // 0.05 - 0.50
    public float fovConeDeg = 45.0f;        // 10 - 90 deg
    public double maxReach = 3.2;           // 2.5 - 4.5 blocks
    public float deadzoneDeg = 0.40f;       // 0.05 - 1.50 deg
    public float microJitterDeg = 0.14f;    // 0.00 - 0.50 deg subtle organic tremor
    public float pitchScale = 0.55f;        // 0.10 - 1.20 relative pitch tracking

    // TriggerBot humanization settings
    public float minCooldownStrength = 0.92f; // 0.50 - 1.00
    public int minAttackDelayMs = 45;         // 15 - 250 ms
    public int maxAttackDelayMs = 105;        // 25 - 320 ms
    public float missChancePct = 4.0f;        // 0.0 - 25.0 % occasional human miss
}
