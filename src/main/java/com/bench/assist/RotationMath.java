package com.bench.assist;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
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
        Box box = target.getBoundingBox();
        double centerX = (box.minX + box.maxX) * 0.5;
        double centerZ = (box.minZ + box.maxZ) * 0.5;
        // Aim at center-upper torso so movement doesn't bounce between head and feet
        double targetY = box.minY + (box.maxY - box.minY) * 0.62;
        return new Vec3d(centerX, targetY, centerZ);
    }

    public static boolean isLookingAtBox(Vec3d eyePos, Vec3d lookVec, Box box, double maxDist) {
        Box expanded = box.expand(0.12);
        Vec3d end = eyePos.add(lookVec.multiply(maxDist));
        return expanded.raycast(eyePos, end).isPresent();
    }
}
