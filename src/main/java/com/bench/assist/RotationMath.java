package com.bench.assist;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public final class RotationMath {
    private RotationMath() {}

    public static float[] anglesToPoint(Vec3d eyePos, Vec3d targetPos, float currentYaw, float currentPitch) {
        double dx = targetPos.x - eyePos.x;
        double dy = targetPos.y - eyePos.y;
        double dz = targetPos.z - eyePos.z;
        double horizDist = Math.sqrt(dx * dx + dz * dz);

        float targetYaw = (float) Math.toDegrees(Math.atan2(-dx, dz));
        float targetPitch = (float) Math.toDegrees(-Math.atan2(dy, horizDist));

        float deltaYaw = MathHelper.wrapDegrees(targetYaw - currentYaw);
        float deltaPitch = MathHelper.wrapDegrees(targetPitch - currentPitch);
        return new float[]{deltaYaw, deltaPitch};
    }

    public static Vec3d aimPoint(Entity target) {
        double y = target.getY() + target.getHeight() * 0.68;
        return new Vec3d(target.getX(), y, target.getZ());
    }

    public static float stepToward(float currentDelta, float maxStep, float jitter) {
        float clamped = MathHelper.clamp(currentDelta, -maxStep, maxStep);
        float quantized = Math.round(clamped / 0.05f) * 0.05f;
        return quantized + jitter;
    }
}
