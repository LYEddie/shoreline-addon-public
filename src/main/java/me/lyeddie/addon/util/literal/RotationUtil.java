package me.lyeddie.addon.util.literal;

import me.lyeddie.addon.util.Globals;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;

public class RotationUtil implements Globals {

    public static float[] getRotationsTo(Vec3 src, Vec3 dest) {
        float yaw = (float) (Math.toDegrees(Math.atan2(dest.subtract(src).z, dest.subtract(src).x)) - 90);
        float pitch = (float) Math.toDegrees(-Math.atan2(dest.subtract(src).y, Math.hypot(dest.subtract(src).x, dest.subtract(src).z)));
        return new float[] {
            Mth.wrapDegrees(yaw),
            Mth.wrapDegrees(pitch)
        };
    }

    public static Vec3 getRotationVector(float pitch, float yaw) {
        float f = pitch * ((float) Math.PI / 180.0f);
        float g = -yaw * ((float) Math.PI / 180.0f);
        float h = Mth.cos(g);
        float i = Mth.sin(g);
        float j = Mth.cos(f);
        float k = Mth.sin(f);
        return new Vec3(i * j, -k, h * j);
    }
}
