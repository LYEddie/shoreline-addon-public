package me.lyeddie.addon.util;

import net.minecraft.entity.Entity;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class Interpolation implements Globals {

    public static Vec3d getInterpolatedPosition(Entity entity, float tickDelta) {
        return new Vec3d(entity.lastX + ((entity.getX() - entity.lastX) * tickDelta),
            entity.lastY + ((entity.getY() - entity.lastY) * tickDelta),
            entity.lastZ + ((entity.getZ() - entity.lastZ) * tickDelta));
    }

    public static float interpolateFloat(float prev, float value, float factor) {
        return prev + ((value - prev) * factor);
    }

    public static double interpolateDouble(double prev, double value, double factor) {
        return prev + ((value - prev) * factor);
    }

    public static Box getInterpolatedBox(Box prevBox, Box box) {
        double delta = mc.isPaused() ? 1f : mc.getRenderTickCounter().getTickProgress(true);
        return new Box(interpolateDouble(prevBox.minX, box.minX, delta),
            interpolateDouble(prevBox.minY, box.minY, delta),
            interpolateDouble(prevBox.minZ, box.minZ, delta),
            interpolateDouble(prevBox.maxX, box.maxX, delta),
            interpolateDouble(prevBox.maxY, box.maxY, delta),
            interpolateDouble(prevBox.maxZ, box.maxZ, delta));
    }

    public static Box getInterpolatedEntityBox(Entity entity) {
        Box box = entity.getBoundingBox();
        Box prevBox = entity.getBoundingBox().offset(entity.lastX - entity.getX(), entity.lastY - entity.getY(), entity.lastZ - entity.getZ());
        return getInterpolatedBox(prevBox, box);
    }
}
