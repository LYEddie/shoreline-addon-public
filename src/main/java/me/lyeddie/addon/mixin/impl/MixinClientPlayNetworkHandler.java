package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.ServerRotationEvent;
import me.lyeddie.addon.mixin.IClientPlayNetworkHandler;
import me.lyeddie.addon.mixin.impl.accessor.AccessorClientConnection;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.entity.player.PlayerPosition;
import net.minecraft.network.ClientConnection;
import net.minecraft.network.NetworkThreadUtils;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.*;
import net.minecraft.util.math.Vec3d;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPlayNetworkHandler.class)
public abstract class MixinClientPlayNetworkHandler implements IClientPlayNetworkHandler, Globals {

    @Shadow
    public abstract ClientConnection getConnection();

    @Inject(method = "onPlayerPositionLook", at = @At(value = "HEAD"), cancellable = true)
    private void onPlayerPositionLook(PlayerPositionLookS2CPacket packet, CallbackInfo ci) {
        ServerRotationEvent serverRotationEvent = new ServerRotationEvent();
        MeteorClient.EVENT_BUS.post(serverRotationEvent);
        if (serverRotationEvent.isCancelled()) {
            ci.cancel();
            NetworkThreadUtils.forceMainThread(packet, (ClientPlayNetworkHandler) (Object) this, mc);
            ClientPlayerEntity playerEntity = mc.player;
            PlayerPosition resolved = PlayerPosition.apply(PlayerPosition.fromEntity(playerEntity), packet.change(), packet.relatives());
            Vec3d position = resolved.position();
            Vec3d change = packet.change().position();
            if (packet.relatives().contains(PositionFlag.X)) {
                playerEntity.lastRenderX += change.x;
                playerEntity.prevX += change.x;
            } else {
                playerEntity.lastRenderX = position.x;
                playerEntity.prevX = position.x;
            }
            if (packet.relatives().contains(PositionFlag.Y)) {
                playerEntity.lastRenderY += change.y;
                playerEntity.prevY += change.y;
            } else {
                playerEntity.lastRenderY = position.y;
                playerEntity.prevY = position.y;
            }
            if (packet.relatives().contains(PositionFlag.Z)) {
                playerEntity.lastRenderZ += change.z;
                playerEntity.prevZ += change.z;
            } else {
                playerEntity.lastRenderZ = position.z;
                playerEntity.prevZ = position.z;
            }
            float yaw = serverRotationEvent.getYaw();
            float pitch = serverRotationEvent.getPitch();
            playerEntity.setPosition(position);
            playerEntity.setVelocity(resolved.deltaMovement());
            getConnection().send(new TeleportConfirmC2SPacket(packet.teleportId()));
            getConnection().send(new PlayerMoveC2SPacket.Full(playerEntity.getX(), playerEntity.getY(),
                playerEntity.getZ(), Float.isNaN(yaw) ? playerEntity.getYaw() : yaw,
                Float.isNaN(pitch) ? playerEntity.getPitch() : pitch, false, playerEntity.horizontalCollision));
        }
    }

    @Override
    public void sendQuietPacket(Packet<?> packet) {
        ((AccessorClientConnection) getConnection()).hookSendInternal(packet, null, true);
    }
}
