package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.ServerRotationEvent;
import me.lyeddie.addon.mixin.IClientPlayNetworkHandler;
import me.lyeddie.addon.mixin.impl.accessor.AccessorClientConnection;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.network.ClientPlayNetworkHandler;
import net.minecraft.client.network.ClientPlayerEntity;
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
            double i;
            double h;
            double g;
            double f;
            double e;
            double d;
            NetworkThreadUtils.forceMainThread(packet, (ClientPlayNetworkHandler) (Object) this, mc);
            ClientPlayerEntity playerEntity = mc.player;
            Vec3d vec3d = playerEntity.getVelocity();
            boolean bl = packet.getFlags().contains(PositionFlag.X);
            boolean bl2 = packet.getFlags().contains(PositionFlag.Y);
            boolean bl3 = packet.getFlags().contains(PositionFlag.Z);
            if (bl) {
                d = vec3d.getX();
                e = playerEntity.getX() + packet.getX();
                playerEntity.lastRenderX += packet.getX();
                playerEntity.prevX += packet.getX();
            } else {
                d = 0.0;
                playerEntity.lastRenderX = e = packet.getX();
                playerEntity.prevX = e;
            }
            if (bl2) {
                f = vec3d.getY();
                g = playerEntity.getY() + packet.getY();
                playerEntity.lastRenderY += packet.getY();
                playerEntity.prevY += packet.getY();
            } else {
                f = 0.0;
                playerEntity.lastRenderY = g = packet.getY();
                playerEntity.prevY = g;
            }
            if (bl3) {
                h = vec3d.getZ();
                i = playerEntity.getZ() + packet.getZ();
                playerEntity.lastRenderZ += packet.getZ();
                playerEntity.prevZ += packet.getZ();
            } else {
                h = 0.0;
                playerEntity.lastRenderZ = i = packet.getZ();
                playerEntity.prevZ = i;
            }
            float yaw = serverRotationEvent.getYaw();
            float pitch = serverRotationEvent.getPitch();
            playerEntity.setPosition(e, g, i);
            playerEntity.setVelocity(d, f, h);
            getConnection().send(new TeleportConfirmC2SPacket(packet.getTeleportId()));
            getConnection().send(new PlayerMoveC2SPacket.Full(playerEntity.getX(), playerEntity.getY(),
                playerEntity.getZ(), Float.isNaN(yaw) ? playerEntity.getYaw() : yaw,
                Float.isNaN(pitch) ? playerEntity.getPitch() : pitch, false));
        }
    }

    @Override
    public void sendQuietPacket(Packet<?> packet) {
        ((AccessorClientConnection) getConnection()).hookSendInternal(packet, null, true);
    }
}
