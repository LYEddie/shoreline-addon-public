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
import me.lyeddie.addon.util.literal.MovementUtil;
import me.lyeddie.addon.util.literal.PlayerUtil;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
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
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (event.packet instanceof ServerboundMovePlayerPacket packet && packet.hasRotation()) {
            float packetYaw = packet.getYRot(0.0f);
            float packetPitch = packet.getXRot(0.0f);
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
            prevJumpYaw = mc.player.getYRot();
            mc.player.setYRot(rotation.getYaw());
            if (TabConfigs.get().getWebJumpFix() && webJumpFix) {
                preJumpFix = mc.player.isSprinting();
                mc.player.setSprinting(false);
            }
        }
    }

    @EventHandler
    public void onPlayerJump(PostPlayerJumpEvent event) {
        if (rotation != null && TabConfigs.get().getMovementFix()) {
            mc.player.setYRot(prevJumpYaw);
            if (webJumpFix) {
                mc.player.setSprinting(preJumpFix);
            }
        }
    }

    @EventHandler
    public void onRenderPlayer(RenderPlayerEvent event) {
        if (event.getEntity() == mc.player && rotation != null) {
            event.setYaw(Interpolation.interpolateFloat(prevYaw, getServerYaw(), mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)));
            event.setPitch(Interpolation.interpolateFloat(prevPitch, getServerPitch(), mc.getDeltaTracker().getGameTimeDeltaPartialTick(true)));
            prevYaw = event.getYaw();
            prevPitch = event.getPitch();
            event.cancel();
        }
    }

    public void applyInputFix() {
        if (rotation != null && mc.player != null && TabConfigs.get().getMovementFix()) {
            float forward = MovementUtil.getForward(mc.player.input);
            float sideways = MovementUtil.getSideways(mc.player.input);
            float delta = (mc.player.getYRot() - rotation.getYaw()) * Mth.DEG_TO_RAD;
            float cos = Mth.cos(delta);
            float sin = Mth.sin(delta);
            MovementUtil.set(mc.player.input, Math.round(sideways * cos - forward * sin),
                Math.round(forward * cos + sideways * sin));
        }
    }

    public void setRotation(Rotation rotation) {
        if (TabConfigs.get().mouseSensFixConfig.get()) {
            double fix = Math.pow(mc.options.sensitivity().get() * 0.6 + 0.2, 3.0) * 1.2;
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
        mc.player.setYRot(yaw);
        mc.player.setXRot(Mth.clamp(pitch, -90.0f, 90.0f));
    }

    public void setRotationSilent(float yaw, float pitch) {
        setRotation(new Rotation(MAX_VALUE, yaw, pitch, true));
        Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.PosRot(
            mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
    }

    public void setRotationSilentSync() {
        float yaw = mc.player.getYRot();
        float pitch = mc.player.getXRot();
        setRotation(new Rotation(MAX_VALUE, yaw, pitch, true));
        Managers.NETWORK.sendPacket(new ServerboundMovePlayerPacket.PosRot(mc.player.getX(), mc.player.getY(), mc.player.getZ(), yaw, pitch, mc.player.onGround(), mc.player.horizontalCollision));
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
        return Mth.wrapDegrees(serverYaw);
    }

    public float getServerPitch() {
        return serverPitch;
    }

    private Vec3 movementInputToVelocity(float yaw, Vec3 movementInput, float speed) {
        double d = movementInput.lengthSqr();
        if (d < 1.0E-7) {
            return Vec3.ZERO;
        }
        Vec3 vec3d = (d > 1.0 ? movementInput.normalize() : movementInput).scale(speed);
        float f = Mth.sin(yaw * Mth.DEG_TO_RAD);
        float g = Mth.cos(yaw * Mth.DEG_TO_RAD);
        return new Vec3(vec3d.x * (double) g - vec3d.z * (double) f, vec3d.y, vec3d.z * (double) g + vec3d.x * (double) f);
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
