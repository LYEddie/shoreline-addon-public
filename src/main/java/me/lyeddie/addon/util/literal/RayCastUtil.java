package me.lyeddie.addon.util.literal;

import me.lyeddie.addon.util.Globals;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.projectile.ProjectileUtil;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public final class RayCastUtil implements Globals {

    public static HitResult raycastEntity(final double reach) {
        Camera view = mc.gameRenderer.getMainCamera();
        Vec3 vec3d = view.position();
        Vec3 vec3d2 = RotationUtil.getRotationVector(view.xRot(), view.yRot());
        Vec3 vec3d3 = vec3d.add(vec3d2.x * reach, vec3d2.y * reach, vec3d2.z * reach);
        AABB box = view.entity().getBoundingBox().expandTowards(vec3d2.scale(reach)).inflate(1.0, 1.0, 1.0);
        return ProjectileUtil.getEntityHitResult(view.entity(), vec3d, vec3d3, box, entity -> !entity.isSpectator() && entity.isPickable(), reach * reach);
    }

    public static HitResult rayCast(final double reach, final float[] angles) {
        final double eyeHeight = mc.player.getEyeHeight();
        final Vec3 eyes = new Vec3(mc.player.getX(), mc.player.getY() + eyeHeight, mc.player.getZ());
        return rayCast(reach, eyes, angles);
    }

    public static HitResult rayCast(final double reach, Vec3 position, final float[] angles) {
        if (Float.isNaN(angles[0]) || Float.isNaN(angles[1])) {
            return null;
        }

        final Vec3 rotationVector = RotationUtil.getRotationVector(angles[1], angles[0]);
        return mc.level.clip(new ClipContext(
            position,
            position.add(rotationVector.x * reach, rotationVector.y * reach, rotationVector.z * reach),
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE,
            mc.player));
    }
}
