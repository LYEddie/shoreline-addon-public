package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.util.Interpolation;
import me.lyeddie.addon.events.*;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.events.staged.*;
import me.lyeddie.addon.mixin.IClientPlayerEntity;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.managers.impl.util.Rotation;
import me.lyeddie.addon.util.tabs.TabConfigs;
import me.lyeddie.addon.util.Globals;
import me.lyeddie.addon.util.literal.PlayerUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import static java.lang.Integer.MAX_VALUE;

public class RotationManager implements Globals {
    private final List<Rotation> requests = new CopyOnWriteArrayList<>();
    private float serverYaw, serverPitch, lastServerYaw, lastServerPitch, prevJumpYaw, prevYaw, prevPitch;
    boolean rotate;
    private Rotation rotation;
    private int rotateTicks;
    private boolean webJumpFix;
    private boolean preJumpFix;

    public RotationManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (event.packet instanceof PlayerMoveC2SPacket packet && packet.changesLook()) {
            float packetYaw = packet.getYaw(0.0f);
            float packetPitch = packet.getPitch(0.0f);
            serverYaw = packetYaw;
            serverPitch = packetPitch;
        }
    }

    @EventHandler(priority = -100)
    public void onUpdate(PlayerTickEvent event) {
        webJumpFix = PlayerUtil.inWeb(1.0);

        if (requests.isEmpty()) {
            rotation = null;
            return;
        }
        Rotation request = getRotationRequest();
        if (request == null) {
            if (isDoneRotating()) {
                rotation = null;
                return;
            }
        } else {
            rotation = request;
        }
        if (rotation == null) {
            return;
        }
        rotateTicks = 0;
        rotate = true;
    }

    @EventHandler
    public void onMovementPackets(MovementPacketsEvent event) {
        if (rotation != null) {

            if (rotate) {
                removeRotation(rotation);
                event.cancel();
                event.setYaw(rotation.getYaw());
                event.setPitch(rotation.getPitch());
                rotate = false;
            }

            if (rotation.isSnap()) {
                rotation = null;
            }
        }
    }

    @EventHandler
    public void onPlayerUpdate(final PostPlayerUpdateEvent event) {
            lastServerYaw = ((IClientPlayerEntity) mc.player).getLastSpoofedYaw();
            lastServerPitch = ((IClientPlayerEntity) mc.player).getLastSpoofedPitch();
    }

    @EventHandler
    public void onKeyboardTick(PreKeyboardTickEvent event) {
        applyInputFix();
    }
    // not a smart move
    @EventHandler
    public void onKeyboardTick(PostKeyboardTickEvent event) {
        applyInputFix();
    }

    @EventHandler
    public void onUpdateVelocity(UpdateVelocityEvent event) {
        if (rotation != null && TabConfigs.get().getMovementFix()) {
            event.cancel();
            event.setVelocity(movementInputToVelocity(rotation.getYaw(), event.getMovementInput(), event.getSpeed()));
        }
    }

    @EventHandler
    public void onPlayerJump(PrePlayerJumpEvent event) {
        if (rotation != null && TabConfigs.get().getMovementFix()) {
            prevJumpYaw = mc.player.getYaw();
            mc.player.setYaw(rotation.getYaw());
            if (TabConfigs.get().getWebJumpFix() && webJumpFix) {
                preJumpFix = mc.player.isSprinting();
                mc.player.setSprinting(false);
            }
        }
    }

    @EventHandler
    public void onPlayerJump(PostPlayerJumpEvent event) {
        if (rotation != null && TabConfigs.get().getMovementFix()) {
            mc.player.setYaw(prevJumpYaw);
            if (webJumpFix) {
                mc.player.setSprinting(preJumpFix);
            }
        }
    }

    @EventHandler
    public void onRenderPlayer(RenderPlayerEvent event) {
        if (event.getEntity() == mc.player && rotation != null) {
            event.setYaw(Interpolation.interpolateFloat(prevYaw, getServerYaw(), mc.getTickDelta()));
            event.setPitch(Interpolation.interpolateFloat(prevPitch, getServerPitch(), mc.getTickDelta()));
            prevYaw = event.getYaw();
            prevPitch = event.getPitch();
            event.cancel();
        }
    }

    public void applyInputFix() {
        if (rotation != null && mc.player != null && TabConfigs.get().getMovementFix()) {
            float forward = mc.player.input.movementForward;
            float sideways = mc.player.input.movementSideways;
            float delta = (mc.player.getYaw() - rotation.getYaw()) * MathHelper.RADIANS_PER_DEGREE;
            float cos = MathHelper.cos(delta);
            float sin = MathHelper.sin(delta);
            mc.player.input.movementSideways = Math.round(sideways * cos - forward * sin);
            mc.player.input.movementForward = Math.round(forward * cos + sideways * sin);
        }
    }

    public void setRotation(Rotation rotation) {
        if (TabConfigs.get().mouseSensFixConfig.get()) {
            double fix = Math.pow(mc.options.getMouseSensitivity().getValue() * 0.6 + 0.2, 3.0) * 1.2;
            rotation.setYaw((float) (rotation.getYaw() - (rotation.getYaw() - serverYaw) % fix));
            rotation.setPitch((float) (rotation.getPitch() - (rotation.getPitch() - serverPitch) % fix));
        }
        if (rotation.getPriority() == MAX_VALUE) this.rotation = rotation;

        Rotation request = requests.stream().filter(r -> rotation.getPriority() == r.getPriority()).findFirst().orElse(null);
        if (request == null) {
            requests.add(rotation);
        } else {
            request.setYaw(rotation.getYaw());
            request.setPitch(rotation.getPitch());
        }
    }

    public void setRotationClient(float yaw, float pitch) {
        if (mc.player == null) {
            return;
        }
        mc.player.setYaw(yaw);
        mc.player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
    }

    public void setRotationSilent(float yaw, float pitch) {
        setRotation(new Rotation(MAX_VALUE, yaw, pitch, true));
        Managers.NETWORK.sendPacket(new PlayerMoveC2SPacket.Full(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
    }

    public void setRotationSilentSync() {
        float yaw = mc.player.getYaw();
        float pitch = mc.player.getPitch();
        setRotation(new Rotation(MAX_VALUE, yaw, pitch, true));
        Managers.NETWORK.sendPacket(new PlayerMoveC2SPacket.Full(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.isOnGround()));
    }

    public boolean removeRotation(Rotation request) {
        return requests.remove(request);
    }

    public boolean isRotationBlocked(int priority) {
        return rotation != null && priority < rotation.getPriority();
    }

    public boolean isDoneRotating() {
        return rotateTicks > toFloat(TabConfigs.get().preserveTicksConfig.get());
    }

    public boolean isRotating() {
        return rotation != null;
    }

    public float getRotationYaw() {
        return rotation.getYaw();
    }

    public float getRotationPitch() {
        return rotation.getPitch();
    }

    public float getServerYaw() {
        return serverYaw;
    }

    public float getWrappedYaw() {
        return MathHelper.wrapDegrees(serverYaw);
    }

    public float getServerPitch() {
        return serverPitch;
    }

    private Vec3d movementInputToVelocity(float yaw, Vec3d movementInput, float speed) {
        double d = movementInput.lengthSquared();
        if (d < 1.0E-7) {
            return Vec3d.ZERO;
        }
        Vec3d vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).multiply(speed);
        float f = MathHelper.sin(yaw * MathHelper.RADIANS_PER_DEGREE);
        float g = MathHelper.cos(yaw * MathHelper.RADIANS_PER_DEGREE);
        return new Vec3d(vec3d.x * (double) g - vec3d.z * (double) f, vec3d.y, vec3d.z * (double) g + vec3d.x * (double) f);
    }

    private Rotation getRotationRequest() {
        Rotation rotationRequest = null;
        int priority = 0;
        for (Rotation request : requests) {
            if (request.getPriority() > priority) {
                rotationRequest = request;
                priority = request.getPriority();
            }
        }
        return rotationRequest;
    }

    public float toFloat(double db) { // lol
        return (float) db;
    }
}
