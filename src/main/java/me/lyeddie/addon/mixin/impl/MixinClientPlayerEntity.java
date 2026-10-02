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
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.player.ClientInput;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerInputPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Input;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class MixinClientPlayerEntity extends AbstractClientPlayer implements Globals, IClientPlayerEntity {

    @Shadow
    @Final
    public ClientPacketListener connection;
    @Shadow
    private double xLast;
    @Shadow
    private double yLast;
    @Shadow
    private double zLast;
    @Shadow
    public ClientInput input;
    @Shadow
    @Final
    protected Minecraft minecraft;
    @Shadow
    private Input lastSentInput;
    @Shadow
    private float yRotLast;
    @Shadow
    private float xRotLast;
    @Shadow
    private boolean lastOnGround;
    @Shadow
    private int positionReminder;
    @Shadow
    private boolean autoJumpEnabled;

    public MixinClientPlayerEntity() {
        super(Minecraft.getInstance().level, Minecraft.getInstance().player.getGameProfile());
    }

    @Shadow
    protected abstract void sendIsSprintingIfNeeded();

    @Shadow
    protected abstract boolean isControlledCamera();

    @Shadow
    protected abstract void updateAutoJump(float dx, float dz);

    @Shadow
    public abstract void tick();

    @Inject(method = "sendPosition", at = @At(value = "HEAD"), cancellable = true)
    private void hookSendMovementPackets(CallbackInfo ci) {
        PrePlayerUpdateEvent playerUpdateEvent = new PrePlayerUpdateEvent();
        MeteorClient.EVENT_BUS.post(playerUpdateEvent);
        MovementPacketsEvent movementPacketsEvent = new MovementPacketsEvent(mc.player.getX(), mc.player.getY(), mc.player.getZ(), mc.player.getYRot(), mc.player.getXRot(), mc.player.onGround());
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
            sendIsSprintingIfNeeded();
            Input currentInput = input.keyPresses;
            if (!currentInput.equals(lastSentInput)) {
                connection.send(new ServerboundPlayerInputPacket(currentInput));
                lastSentInput = currentInput;
            }
            if (isControlledCamera()) {
                double d = x - xLast;
                double e = y - yLast;
                double f = z - zLast;
                double g = yaw - yRotLast;
                double h = pitch - xRotLast;
                ++positionReminder;
                boolean bl2 = Mth.lengthSquared(d, e, f) > Mth.square(2.0E-4) || positionReminder >= 20;
                boolean bl3 = g != 0.0 || h != 0.0;
                if (isPassenger()) {
                    Vec3 vec3d = getDeltaMovement();
                    connection.send(new ServerboundMovePlayerPacket.PosRot(vec3d.x, -999.0, vec3d.z, getYRot(), getXRot(), ground, horizontalCollision));
                    bl2 = false;
                } else if (bl2 && bl3) {
                    connection.send(new ServerboundMovePlayerPacket.PosRot(x, y, z, yaw, pitch, ground, horizontalCollision));
                } else if (bl2) {
                    connection.send(new ServerboundMovePlayerPacket.Pos(x, y, z, ground, horizontalCollision));
                } else if (bl3) {
                    connection.send(new ServerboundMovePlayerPacket.Rot(yaw, pitch, ground, horizontalCollision));
                } else if (lastOnGround != onGround()) {
                    connection.send(new ServerboundMovePlayerPacket.StatusOnly(ground, horizontalCollision));
                }
                if (bl2) {
                    xLast = x;
                    yLast = y;
                    zLast = z;
                    positionReminder = 0;
                }
                if (bl3) {
                    yRotLast = yaw;
                    xRotLast = pitch;
                }
                lastOnGround = ground;
                autoJumpEnabled = minecraft.options.autoJump().get();
            }
        }
        PostPlayerUpdateEvent playerUpdateEvent1 = new PostPlayerUpdateEvent();
        MeteorClient.EVENT_BUS.post(playerUpdateEvent1);
    }

    @Inject(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/AbstractClientPlayer;tick()V", shift = At.Shift.BEFORE, ordinal = 0))
    private void hookTickPre(CallbackInfo ci) {
        PlayerTickEvent playerTickEvent = new PlayerTickEvent();
        MeteorClient.EVENT_BUS.post(playerTickEvent);
    }

    @Inject(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/ClientInput;tick()V", shift = At.Shift.AFTER))
    private void hookTickMovementPost(CallbackInfo ci) {
        MovementSlowdownEvent movementUpdateEvent = new MovementSlowdownEvent(input);
        MeteorClient.EVENT_BUS.post(movementUpdateEvent);
    }

    @Inject(method = "move", at = @At(value = "HEAD"), cancellable = true)
    private void hookMove(MoverType movementType, Vec3 movement, CallbackInfo ci) {
        final PlayerMoveEvent playerMoveEvent = new PlayerMoveEvent(movementType, movement);
        MeteorClient.EVENT_BUS.post(playerMoveEvent);
        if (playerMoveEvent.isCancelled()) {
            ci.cancel();
            double d = getX();
            double e = getZ();
            super.move(movementType, playerMoveEvent.getMovement());
            updateAutoJump((float) (getX() - d), (float) (getZ() - e));
        }
    }

    @Inject(method = "moveTowardsClosestSpace", at = @At(value = "HEAD"), cancellable = true)
    private void onPushOutOfBlocks(double x, double z, CallbackInfo ci) {
        PushOutOfBlocksEvent pushOutOfBlocksEvent = new PushOutOfBlocksEvent();
        MeteorClient.EVENT_BUS.post(pushOutOfBlocksEvent);
        if (pushOutOfBlocksEvent.isCancelled()) {
            ci.cancel();
        }
    }

    @Inject(method = "startUsingItem", at = @At(value = "HEAD"))
    private void hookSetCurrentHand(InteractionHand hand, CallbackInfo ci) {
        SetCurrentHandEvent setCurrentHandEvent = new SetCurrentHandEvent(hand);
        MeteorClient.EVENT_BUS.post(setCurrentHandEvent);
    }

    @Redirect(method = "aiStep", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;setSprinting(Z)V", ordinal = 3))
    private void hookSetSprinting(LocalPlayer instance, boolean b) {
        final SprintCancelEvent sprintEvent = new SprintCancelEvent();
        MeteorClient.EVENT_BUS.post(sprintEvent);
        if (sprintEvent.isCancelled()) {
            instance.setSprinting(true);
        } else {
            instance.setSprinting(b);
        }
    }

    @Override
    public float getLastSpoofedYaw() {
        return yRotLast;
    }

    @Override
    public float getLastSpoofedPitch() {
        return xRotLast;
    }
}
