package me.lyeddie.addon.util;

import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class Interpolation implements Globals {

    public static Vec3 getInterpolatedPosition(Entity entity, float tickDelta) {
        return new Vec3(entity.xo + ((entity.getX() - entity.xo) * tickDelta),
            entity.yo + ((entity.getY() - entity.yo) * tickDelta),
            entity.zo + ((entity.getZ() - entity.zo) * tickDelta));
    }

    public static float interpolateFloat(float prev, float value, float factor) {
        return prev + ((value - prev) * factor);
    }

    public static double interpolateDouble(double prev, double value, double factor) {
        return prev + ((value - prev) * factor);
    }

    public static AABB getInterpolatedBox(AABB prevBox, AABB box) {
        double delta = mc.isPaused() ? 1f : mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        return new AABB(interpolateDouble(prevBox.minX, box.minX, delta),
            interpolateDouble(prevBox.minY, box.minY, delta),
            interpolateDouble(prevBox.minZ, box.minZ, delta),
            interpolateDouble(prevBox.maxX, box.maxX, delta),
            interpolateDouble(prevBox.maxY, box.maxY, delta),
            interpolateDouble(prevBox.maxZ, box.maxZ, delta));
    }

    public static AABB getInterpolatedEntityBox(Entity entity) {
        AABB box = entity.getBoundingBox();
        AABB prevBox = entity.getBoundingBox().move(entity.xo - entity.getX(), entity.yo - entity.getY(), entity.zo - entity.getZ());
        return getInterpolatedBox(prevBox, box);
    }
}
