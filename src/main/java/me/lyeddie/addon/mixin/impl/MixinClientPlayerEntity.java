package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.*;
import me.lyeddie.addon.events.irrevocable.MovementSlowdownEvent;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.events.irrevocable.SetCurrentHandEvent;
import me.lyeddie.addon.events.staged.PostPlayerUpdateEvent;
import me.lyeddie.addon.events.staged.PrePlayerUpdateEvent;
import me.lyeddie.addon.mixin.IClientPlayerEntity;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.input.Input;
import net.minecraft.client.network.AbstractClientPlayerEntity;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.MovementType;
import net.minecraft.network.packet.c2s.play.PlayerInputC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.PlayerInput;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayerEntity.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayerEntity implements Globals, IClientPlayerEntity {

    @Shadow
    @Final
    public ClientPlayNetworkHandler networkHandler;
    @Shadow
    private double lastXClient;
    @Shadow
    private double lastYClient;
    @Shadow
    private double lastZClient;
    @Shadow
    public Input input;
    @Shadow
    @Final
    protected MinecraftClient client;
    @Shadow
    private PlayerInput lastPlayerInput;
    @Shadow
    private float lastYawClient;
    @Shadow
    private float lastPitchClient;
    @Shadow
    private boolean lastOnGround;
    @Shadow
    private int ticksSinceLastPositionPacketSent;
    @Shadow
    private boolean autoJumpEnabled;

    public MixinClientPlayerEntity() {
        super(MinecraftClient.getInstance().world, MinecraftClient.getInstance().player.getGameProfile());
    }

    @Shadow
    protected abstract void sendSprintingPacket();

    @Shadow
    protected abstract boolean isCamera();

    @Shadow
    protected abstract void autoJump(float dx, float dz);

    @Shadow
    public abstract void tick();

    @Inject(method = "sendMovementPackets", at = @At(value = "HEAD"), cancellable = true)
    private void hookSendMovementPackets(CallbackInfo ci) {
        PrePlayerUpdateEvent playerUpdateEvent = new PrePlayerUpdateEvent();
        MeteorClient.EVENT_BUS.post(playerUpdateEvent);
        MovementPacketsEvent movementPacketsEvent = new MovementPacketsEvent(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYaw(), mc.player.getPitch(), mc.player.isOnGround());
        MeteorClient.EVENT_BUS.post(movementPacketsEvent);

        double x = movementPacketsEvent.getX();
        double y = movementPacketsEvent.getY();
        double z = movementPacketsEvent.getZ();
        float yaw = movementPacketsEvent.getYaw();
        float pitch = movementPacketsEvent.getPitch();
        boolean ground = movementPacketsEvent.getOnGround();

        EncodeYawEvent encodeYawEvent = new EncodeYawEvent();
        MeteorClient.EVENT_BUS.post(encodeYawEvent);
        if (encodeYawEvent.isCancelled()) {
            yaw += 36000000.0f;
        }

        if (movementPacketsEvent.isCancelled() || encodeYawEvent.isCancelled()) {
            ci.cancel();
            sendSprintingPacket();
            PlayerInput currentInput = input.playerInput;
            if (!currentInput.equals(lastPlayerInput)) {
                networkHandler.sendPacket(new PlayerInputC2SPacket(currentInput));
                lastPlayerInput = currentInput;
            }
            if (isCamera()) {
                double d = x - lastXClient;
                double e = y - lastYClient;
                double f = z - lastZClient;
                double g = yaw - lastYawClient;
                double h = pitch - lastPitchClient;
                ++ticksSinceLastPositionPacketSent;
                boolean bl2 = MathHelper.squaredMagnitude(d, e, f) > MathHelper.square(2.0E-4) || ticksSinceLastPositionPacketSent >= 20;
                boolean bl3 = g != 0.0 || h != 0.0;
                if (hasVehicle()) {
                    Vec3d vec3d = getVelocity();
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(vec3d.x, -999.0, vec3d.z, getYaw(), getPitch(), ground, horizontalCollision));
                    bl2 = false;
                } else if (bl2 && bl3) {
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.Full(x, y, z, yaw, pitch, ground, horizontalCollision));
                } else if (bl2) {
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.PositionAndOnGround(x, y, z, ground, horizontalCollision));
                } else if (bl3) {
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.LookAndOnGround(yaw, pitch, ground, horizontalCollision));
                } else if (lastOnGround != isOnGround()) {
                    networkHandler.sendPacket(new PlayerMoveC2SPacket.OnGroundOnly(ground, horizontalCollision));
                }
                if (bl2) {
                    lastXClient = x;
                    lastYClient = y;
                    lastZClient = z;
                    ticksSinceLastPositionPacketSent = 0;
                }
                if (bl3) {
                    lastYawClient = yaw;
                    lastPitchClient = pitch;
                }
                lastOnGround = ground;
                autoJumpEnabled = client.options.getAutoJump().getValue();
            }
        }
        PostPlayerUpdateEvent playerUpdateEvent1 = new PostPlayerUpdateEvent();
        MeteorClient.EVENT_BUS.post(playerUpdateEvent1);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/" +
        "minecraft/client/network/AbstractClientPlayerEntity;tick()V", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookTickPre(CallbackInfo ci) {
        PlayerTickEvent playerTickEvent = new PlayerTickEvent();
        MeteorClient.EVENT_BUS.post(playerTickEvent);
    }

    @Inject(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/input/Input;tick()V", shift = At.Shift.AFTER))
    private void hookTickMovementPost(CallbackInfo ci) {
        MovementSlowdownEvent movementUpdateEvent = new MovementSlowdownEvent(input);
        MeteorClient.EVENT_BUS.post(movementUpdateEvent);
    }

    @Inject(method = "move", at = @At(value = "HEAD"), cancellable = true)
    private void hookMove(MovementType movementType, Vec3d movement, CallbackInfo ci) {
        final PlayerMoveEvent playerMoveEvent = new PlayerMoveEvent(movementType, movement);
        MeteorClient.EVENT_BUS.post(playerMoveEvent);
        if (playerMoveEvent.isCancelled()) {
            ci.cancel();
            double d = getX();
            double e = getZ();
            super.move(movementType, playerMoveEvent.getMovement());
            autoJump((float) (getX() - d), (float) (getZ() - e));
        }
    }

    @Inject(method = "pushOutOfBlocks", at = @At(value = "HEAD"), cancellable = true)
    private void onPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        PushOutOfBlocksEvent pushOutOfBlocksEvent = new PushOutOfBlocksEvent();
        MeteorClient.EVENT_BUS.post(pushOutOfBlocksEvent);
        if (pushOutOfBlocksEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "setCurrentHand", at = @At(value = "HEAD"))
    private void hookSetCurrentHand(Hand hand, CallbackInfo ci) {
        SetCurrentHandEvent setCurrentHandEvent = new SetCurrentHandEvent(hand);
        MeteorClient.EVENT_BUS.post(setCurrentHandEvent);
    }

    @ModifyArg(method = "tickMovement", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;setSprinting(Z)V", ordinal = 3), index = 0)
    private boolean hookSetSprinting(boolean sprinting) {
        final SprintCancelEvent sprintEvent = new SprintCancelEvent();
        MeteorClient.EVENT_BUS.post(sprintEvent);
        return sprintEvent.isCancelled() ? true : sprinting;
    }

    @Override
    public float getLastSpoofedYaw() {
        return lastYawClient;
    }

    @Override
    public float getLastSpoofedPitch() {
        return lastPitchClient;
    }
}
