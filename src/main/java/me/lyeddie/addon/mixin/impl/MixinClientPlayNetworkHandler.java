package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.ServerRotationEvent;
import me.lyeddie.addon.mixin.IClientPlayNetworkHandler;
import me.lyeddie.addon.mixin.impl.accessor.AccessorClientConnection;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketUtils;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.PositionMoveRotation;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class MixinClientPlayNetworkHandler implements IClientPlayNetworkHandler, Globals {

    @Shadow
    public abstract Connection getConnection();

    @Inject(method = "handleMovePlayer", at = @At(value = "HEAD"), cancellable = true)
    private void onPlayerPositionLook(ClientboundPlayerPositionPacket packet, CallbackInfo ci) {
        ServerRotationEvent serverRotationEvent = new ServerRotationEvent();
        MeteorClient.EVENT_BUS.post(serverRotationEvent);
        if (serverRotationEvent.isCancelled()) {
            ci.cancel();
            PacketUtils.ensureRunningOnSameThread(packet, (ClientPacketListener) (Object) this, mc.packetProcessor());
            LocalPlayer playerEntity = mc.player;
            PositionMoveRotation resolved = PositionMoveRotation.calculateAbsolute(PositionMoveRotation.of(playerEntity), packet.change(), packet.relatives());
            Vec3 position = resolved.position();
            Vec3 change = packet.change().position();
            if (packet.relatives().contains(Relative.X)) {
                playerEntity.xOld += change.x;
                playerEntity.xo += change.x;
            } else {
                playerEntity.xOld = position.x;
                playerEntity.xo = position.x;
            }
            if (packet.relatives().contains(Relative.Y)) {
                playerEntity.yOld += change.y;
                playerEntity.yo += change.y;
            } else {
                playerEntity.yOld = position.y;
                playerEntity.yo = position.y;
            }
            if (packet.relatives().contains(Relative.Z)) {
                playerEntity.zOld += change.z;
                playerEntity.zo += change.z;
            } else {
                playerEntity.zOld = position.z;
                playerEntity.zo = position.z;
            }
            float yaw = serverRotationEvent.getYaw();
            float pitch = serverRotationEvent.getPitch();
            playerEntity.setPos(position);
            playerEntity.setDeltaMovement(resolved.deltaMovement());
            getConnection().send(new ServerboundAcceptTeleportationPacket(packet.id()));
            getConnection().send(new ServerboundMovePlayerPacket.PosRot(playerEntity.getX(), playerEntity.getY(),
                playerEntity.getZ(), Float.isNaN(yaw) ? playerEntity.getYRot() : yaw,
                Float.isNaN(pitch) ? playerEntity.getXRot() : pitch, false, playerEntity.horizontalCollision));
        }
    }

    @Override
    public void sendQuietPacket(Packet<?> packet) {
        ((AccessorClientConnection) getConnection()).hookSendInternal(packet, null, true);
    }
}
