package com.bench.assist;

public final class AssistConfig {
    public boolean aimAssistEnabled = true;
    public boolean triggerBotEnabled = true;
    public boolean keepSprintOnW = true;

    public float minCooldownStrength = 0.96f;

    public float maxYawStepDeg = 4.2f;
    public float maxPitchStepDeg = 2.6f;
    public float fovConeDeg = 42.0f;
    public double maxReach = 3.0;

    public float jitterDeg = 0.35f;
    public int minAttackDelayMs = 35;
    public int maxAttackDelayMs = 85;
}
