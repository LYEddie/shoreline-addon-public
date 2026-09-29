package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.module.AddonModule;
import me.lyeddie.addon.tabs.TabConfigs;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ChorusFruitItem;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerMoveC2SPacket;
import net.minecraft.network.packet.c2s.play.TeleportConfirmC2SPacket;
import net.minecraft.network.packet.s2c.play.PlayerPositionLookS2CPacket;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;

public class ChorusControl extends AddonModule {

    private boolean cancelChorusTeleport;
    private int chorusTeleportId;
    private PlayerPositionLookS2CPacket teleportPacket;

    public ChorusControl() {
        super(Shoreline.MAIN, "ChorusControl", "Allows player to control chorus teleports");
    }

    @Override
    public void onDeactivate() {
        if (mc.getNetworkHandler() != null && teleportPacket != null) {
            mc.getNetworkHandler().onPlayerPositionLook(teleportPacket);
        }
        teleportPacket = null;
        cancelChorusTeleport = false;
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (cancelChorusTeleport) {
            if (event.packet instanceof PlayerMoveC2SPacket packet && packet.changesPosition()) {
                event.cancel();
            } else if (event.packet instanceof TeleportConfirmC2SPacket packet) {
                event.cancel();
                chorusTeleportId = packet.getTeleportId();
            }
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (event.packet instanceof PlayerPositionLookS2CPacket packet && cancelChorusTeleport) {
            event.cancel();
            teleportPacket = packet;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player.isSneaking() && cancelChorusTeleport) {
            if (mc.getNetworkHandler() != null && teleportPacket != null) {
                mc.getNetworkHandler().onPlayerPositionLook(teleportPacket);
            }
            teleportPacket = null;
            cancelChorusTeleport = false;
        }
        if (!cancelChorusTeleport && mc.player.isUsingItem()) {
            ItemStack stack = mc.player.getStackInHand(mc.player.getActiveHand());
            if (stack.getItem() instanceof ChorusFruitItem
                && stack.getMaxUseTime(mc.player) - mc.player.getItemUseTime() <= 1) {
                cancelChorusTeleport = true;
            }
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (teleportPacket != null) {
            Vec3d vec3d = new Vec3d(teleportPacket.getX(), teleportPacket.getY(), teleportPacket.getZ());
            Box teleportBox = PlayerEntity.STANDING_DIMENSIONS.getBoxAt(vec3d);
            event.renderer.box(teleportBox, TabConfigs.get().getClampColor(60), TabConfigs.get().getClampColor(100), ShapeMode.Both, 0);
        }
    }
}
