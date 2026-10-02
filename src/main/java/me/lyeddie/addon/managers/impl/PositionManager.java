package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.Entity;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

public class PositionManager implements Globals {
    private double x, y, z;
    private BlockPos blockPos;
    private boolean sneaking, sprinting;
    private boolean onGround;

    public PositionManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (mc.player != null && mc.world != null) {
            if (event.packet instanceof PlayerMoveC2SPacket packet) {
                onGround = packet.isOnGround();
                if (packet.changesPosition()) {
                    x = packet.getX(x);
                    y = packet.getY(y);
                    z = packet.getZ(z);
                    blockPos = BlockPos.ofFloored(x, y, z);
                }
            } else if (event.packet instanceof ClientCommandC2SPacket packet) {
                switch (packet.getMode()) {
                    case START_SPRINTING -> sprinting = true;
                    case STOP_SPRINTING -> sprinting = false;
                    case PRESS_SHIFT_KEY -> sneaking = true;
                    case RELEASE_SHIFT_KEY -> sneaking = false;
                }
            }
        }
    }

    public void setPosition(Vec3d vec3d) {
        setPosition(vec3d.getX(), vec3d.getY(), vec3d.getZ());
    }

    public void setPosition(double x, double y, double z) {
        setPositionClient(x, y, z);
        Managers.NETWORK.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, isOnGround()));
    }

    public void setPositionClient(double x, double y, double z) {
        if (mc.player.isRiding()) {
            mc.player.getVehicle().setPosition(x, y, z);
            return;
        }
        mc.player.setPosition(x, y, z);
    }

    public void setPositionY(double y) {
        setPosition(x, y, z);
    }

    public Vec3d getPos() {
        return new Vec3d(getX(), getY(), getZ());
    }

    public Vec3d getEyePos() {
        return getPos().add(0.0, mc.player.getStandingEyeHeight(), 0.0);
    }

    public double squaredDistanceTo(Entity entity) {
        float f = (float) (getX() - entity.getX());
        float g = (float) (getY() - entity.getY());
        float h = (float) (getZ() - entity.getZ());
        return MathHelper.squaredMagnitude(f, g, h);
    }

    public double getX() {
        return x;
    }

    public double getY() {
        return y;
    }

    public double getZ() {
        return z;
    }

    public BlockPos getBlockPos() {
        return blockPos;
    }

    public boolean isSneaking() {
        return sneaking;
    }

    public boolean isSprinting() {
        return sprinting;
    }

    public boolean isOnGround() {
        return onGround;
    }
}
