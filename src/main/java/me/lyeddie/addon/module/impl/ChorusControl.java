package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.module.AddonModule;
import me.lyeddie.addon.util.tabs.TabConfigs;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class ChorusControl extends AddonModule {

    private boolean cancelChorusTeleport;
    private int chorusTeleportId;
    private ClientboundPlayerPositionPacket teleportPacket;

    public ChorusControl() {
        super(Shoreline.MAIN, "ChorusControl", "Allows player to control chorus teleports");
    }

    @Override
    public void onDeactivate() {
        if (mc.getConnection() != null && teleportPacket != null) {
            mc.getConnection().handleMovePlayer(teleportPacket);
        }
        teleportPacket = null;
        cancelChorusTeleport = false;
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (cancelChorusTeleport) {
            if (event.packet instanceof ServerboundMovePlayerPacket packet && packet.hasPosition()) {
                event.cancel();
            } else if (event.packet instanceof ServerboundAcceptTeleportationPacket packet) {
                event.cancel();
                chorusTeleportId = packet.getId();
            }
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundPlayerPositionPacket packet && cancelChorusTeleport) {
            event.cancel();
            teleportPacket = packet;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player.isShiftKeyDown() && cancelChorusTeleport) {
            if (mc.getConnection() != null && teleportPacket != null) {
                mc.getConnection().handleMovePlayer(teleportPacket);
            }
            teleportPacket = null;
            cancelChorusTeleport = false;
        }
        if (!cancelChorusTeleport && mc.player.isUsingItem()) {
            ItemStack stack = mc.player.getItemInHand(mc.player.getUsedItemHand());
            if (stack.is(Items.CHORUS_FRUIT)
                && stack.getUseDuration(mc.player) - mc.player.getTicksUsingItem() <= 1) {
                cancelChorusTeleport = true;
            }
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (teleportPacket != null) {
            Vec3 vec3d = teleportPacket.change().position();
            AABB teleportBox = mc.player.getDimensions(Pose.STANDING).makeBoundingBox(vec3d);
            event.renderer.box(teleportBox, TabConfigs.get().getClampColor(60), TabConfigs.get().getClampColor(100), ShapeMode.Both, 0);
        }
    }
}
