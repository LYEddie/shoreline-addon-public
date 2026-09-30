package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;

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
        if (mc.player != null && mc.level != null) {
            if (event.packet instanceof ServerboundMovePlayerPacket packet) {
                onGround = packet.isOnGround();
                if (packet.hasPosition()) {
                    x = packet.getX(x);
                    y = packet.getY(y);
                    z = packet.getZ(z);
                    blockPos = BlockPos.containing(x, y, z);
                }
            } else if (event.packet instanceof ServerboundPlayerCommandPacket packet) {
                switch (packet.getAction()) {
                    case START_SPRINTING -> sprinting = true;
                    case STOP_SPRINTING -> sprinting = false;
                }
            } else if (event.packet instanceof ServerboundPlayerInputPacket packet) {
                sneaking = packet.input().shift();
            }
        }
    }

    public void setPosition(Vec3 vec3d) {
        setPosition(vec3d.x(), vec3d.y(), vec3d.z());
    }

    public void setPosition(double x, double y, double z) {
        setPositionClient(x, y, z);
        Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.Pos(x, y, z, isOnGround(), mc.player.horizontalCollision));
    }

    public void setPositionClient(double x, double y, double z) {
        if (mc.player.isHandsBusy()) {
            mc.player.getVehicle().setPos(x, y, z);
            return;
        }
        mc.player.setPos(x, y, z);
    }

    public void setPositionY(double y) {
        setPosition(x, y, z);
    }

    public Vec3 getPos() {
        return new Vec3(getX(), getY(), getZ());
    }

    public Vec3 getEyePos() {
        return getPos().add(0.0, mc.player.getEyeHeight(), 0.0);
    }

    public double squaredDistanceTo(Entity entity) {
        float f = (float) (getX() - entity.getX());
        float g = (float) (getY() - entity.getY());
        float h = (float) (getZ() - entity.getZ());
        return Mth.lengthSquared(f, g, h);
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
