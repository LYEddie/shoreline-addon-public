package me.lyeddie.addon.events;

import meteordevelopment.meteorclient.events.Cancellable;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.phys.Vec3;

public class PlayerMoveEvent extends Cancellable {

    private final MoverType type;
    private double x, y, z;

    public PlayerMoveEvent(MoverType type, Vec3 movement) {
        this.type = type;
        this.x = movement.x();
        this.y = movement.y();
        this.z = movement.z();
    }

    public MoverType getType() {
        return type;
    }

    public Vec3 getMovement() {
        return new Vec3(x, y, z);
    }

    public double getX() {
        return x;
    }

    public void setX(double x) {
        this.x = x;
    }

    public double getY() {
        return y;
    }

    public void setY(double y) {
        this.y = y;
    }

    public double getZ() {
        return z;
    }

    public void setZ(double z) {
        this.z = z;
    }
}
