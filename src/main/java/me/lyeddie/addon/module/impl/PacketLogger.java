package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.util.Helpers;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.common.CommonPongC2SPacket;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.hit.BlockHitResult;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class PacketLogger extends AddonModule implements Helpers {
    private static PacketLogger INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgPackets = settings.createGroup("Packet Types");

    private final Setting<Boolean> logConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Log").description("Adds packets to the logs")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> chatConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("LogChat").description("Logs packets in the chats")
        .defaultValue(false)
        .build());

    private final Setting<Boolean> disconnectConfig = sgPackets.add(new BoolSetting.Builder()
        .name("LogDisconnect").description("Logs packets on client disconnect")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> moveFullConfig = sgPackets.add(new BoolSetting.Builder()
        .name("PlayerMoveFull").description("Logs PlayerMoveC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> moveLookConfig = sgPackets.add(new BoolSetting.Builder()
        .name("PlayerMoveLook").description("Logs PlayerMoveC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> movePosConfig = sgPackets.add(new BoolSetting.Builder()
        .name("PlayerMovePosition").description("Logs PlayerMoveC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> moveGroundConfig = sgPackets.add(new BoolSetting.Builder()
        .name("PlayerMoveGround").description("Logs PlayerMoveC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> vehicleMoveConfig = sgPackets.add(new BoolSetting.Builder()
        .name("VehicleMove").description("Logs VehicleMoveC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> playerActionConfig = sgPackets.add(new BoolSetting.Builder()
        .name("PlayerAction").description("Logs PlayerActionC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> updateSlotConfig = sgPackets.add(new BoolSetting.Builder()
        .name("UpdateSelectedSlot").description("Logs UpdateSelectedSlotC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> clickSlotConfig = sgPackets.add(new BoolSetting.Builder()
        .name("ClickSlot").description("Logs ClickSlotC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> pickInventoryConfig = sgPackets.add(new BoolSetting.Builder()
        .name("PickInventory").description("Logs PickFromInventoryC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> handSwingConfig = sgPackets.add(new BoolSetting.Builder()
        .name("HandSwing").description("Logs HandSwingC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> interactEntityConfig = sgPackets.add(new BoolSetting.Builder()
        .name("InteractEntity").description("Logs PlayerInteractEntityC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> interactBlockConfig = sgPackets.add(new BoolSetting.Builder()
        .name("InteractBlock").description("Logs PlayerInteractBlockC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> interactItemConfig = sgPackets.add(new BoolSetting.Builder()
        .name("InteractItem").description("Logs PlayerInteractItemC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> commandConfig = sgPackets.add(new BoolSetting.Builder()
        .name("ClientCommand").description("Logs ClientCommandC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> statusConfig = sgPackets.add(new BoolSetting.Builder()
        .name("ClientStatus").description("Logs ClientStatusC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> closeScreenConfig = sgPackets.add(new BoolSetting.Builder()
        .name("CloseScreen").description("Logs CloseHandledScreenC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> teleportConfirmConfig = sgPackets.add(new BoolSetting.Builder()
        .name("TeleportConfirm").description("Logs TeleportConfirmC2SPacket")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> pongConfig = sgPackets.add(new BoolSetting.Builder()
        .name("Pong").description("Logs CommonPongC2SPacket")
        .defaultValue(false)
        .build());

    private final List<PacketLog> packetLogs = new CopyOnWriteArrayList<>();

    public PacketLogger() {
        super(Shoreline.MAIN, "PacketLogger", "Logs client packets");
        INST = this;
    }

    @Override
    public void onActivate() {
        info("PacketLogger", "PacketLogger enabled ...");
    }

    @Override
    public void onDeactivate() {
        info("PacketLogger", "PacketLogger disabled ...");
    }

    private void logPacket(Packet<?> packet, String msg, Object... args) {
        String s = String.format(msg, args);
        if (logConfig.get()) {
            info("PacketLogger", s);
        }
        if (chatConfig.get()) {
            info(s);
        }

        packetLogs.add(new PacketLog(packet, System.currentTimeMillis()));
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        packetLogs.removeIf(l -> System.currentTimeMillis() - l.time() > 3000);
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        if (!disconnectConfig.get()) {
            return;
        }

        Map<String, Integer> packetCountMap = new HashMap<>();
        for (PacketLog packetLog : packetLogs) {
            Packet<?> packet = packetLog.packet();
            String identifier = packet.getClass().getSimpleName();
            if (packetCountMap.containsKey(identifier)) {
                packetCountMap.replace(identifier, packetCountMap.get(identifier) + 1);
            } else {
                packetCountMap.put(identifier, 1);
            }
        }

        List<String> strings = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : packetCountMap.entrySet()) {
            strings.add(entry.getKey() + ": " + entry.getValue());
        }

        info("PacketLogger", String.join(",", strings));
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (event.packet instanceof PlayerMoveC2SPacket.Full packet && moveFullConfig.get()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove Full - ");
            if (packet.changesPosition()) {
                builder.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
            }
            if (packet.changesLook()) {
                builder.append("yaw: ").append(packet.getYaw(0.0f)).append(", pitch: ").append(packet.getPitch(0.0f)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(packet, builder.toString());
        }
        if (event.packet instanceof PlayerMoveC2SPacket.PositionAndOnGround packet && movePosConfig.get()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove PosGround - ");
            if (packet.changesPosition()) {
                builder.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(packet, builder.toString());
        }
        if (event.packet instanceof PlayerMoveC2SPacket.LookAndOnGround packet && moveLookConfig.get()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove LookGround - ");
            if (packet.changesLook()) {
                builder.append("yaw: ").append(packet.getYaw(0.0f)).append(", pitch: ").append(packet.getPitch(0.0f)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(packet, builder.toString());
        }
        if (event.packet instanceof PlayerMoveC2SPacket.OnGroundOnly packet && moveGroundConfig.get()) {
            String s = "PlayerMove Ground - onground: " + packet.isOnGround();
            logPacket(packet, s);
        }
        if (event.packet instanceof VehicleMoveC2SPacket packet && vehicleMoveConfig.get()) {
            logPacket(packet, "VehicleMove - x: %s, y: %s, z: %s, yaw: %s, pitch: %s", packet.getX(), packet.getY(), packet.getZ(), packet.getYaw(), packet.getPitch());
        }
        if (event.packet instanceof PlayerActionC2SPacket packet && playerActionConfig.get()) {
            logPacket(packet, "PlayerAction - action: %s, direction: %s, pos: %s", packet.getAction().name(), packet.getDirection().name(), packet.getPos().toShortString());
        }
        if (event.packet instanceof UpdateSelectedSlotC2SPacket packet && updateSlotConfig.get()) {
            logPacket(packet, "UpdateSlot - slot: %d", packet.getSelectedSlot());
        }
        if (event.packet instanceof HandSwingC2SPacket packet && handSwingConfig.get()) {
            logPacket(packet, "HandSwing - hand: %s", packet.getHand().name());
        }
        if (event.packet instanceof CommonPongC2SPacket packet && pongConfig.get()) {
            logPacket(packet, "Pong - %d", packet.getParameter());
        }
        if (event.packet instanceof PlayerInteractEntityC2SPacket packet && mc.world != null && interactEntityConfig.get()) {
            logPacket(packet, "InteractEntity");
        }
        if (event.packet instanceof PlayerInteractBlockC2SPacket packet && interactBlockConfig.get()) {
            BlockHitResult blockHitResult = packet.getBlockHitResult();
            logPacket(packet, "InteractBlock - pos: %s, dir: %s, hand: %s", blockHitResult.getBlockPos().toShortString(), blockHitResult.getSide().name(), packet.getHand().name());
        }
        if (event.packet instanceof PlayerInteractItemC2SPacket packet && interactItemConfig.get()) {
            logPacket(packet, "InteractItem - hand: %s", packet.getHand().name());
        }
        if (event.packet instanceof CloseHandledScreenC2SPacket packet && closeScreenConfig.get()) {
            logPacket(packet, "CloseScreen - id: %s", packet.getSyncId());
        }
        if (event.packet instanceof ClientCommandC2SPacket packet && commandConfig.get()) {
            logPacket(packet, "ClientCommand - mode: %s", packet.getMode().name());
        }
        if (event.packet instanceof ClientStatusC2SPacket packet && statusConfig.get()) {
            logPacket(packet, "ClientStatus - mode: %s", packet.getMode().name());
        }
        if (event.packet instanceof ClickSlotC2SPacket packet && clickSlotConfig.get()) {
            logPacket(packet, "ClickSlot - type: %s, slot: %s, button: %s, id: %s", packet.getActionType().name(), packet.getSlot(), packet.getButton(), packet.getSyncId());
        }
        if (event.packet instanceof PickFromInventoryC2SPacket packet && pickInventoryConfig.get()) {
            logPacket(packet, "PickInventory - slot: %s", packet.getSlot());
        }
        if (event.packet instanceof TeleportConfirmC2SPacket packet && teleportConfirmConfig.get()) {
            logPacket(packet, "TeleportConfirm - id: %s", packet.getTeleportId());
        }
    }

    public record PacketLog(Packet<?> packet, long time) {
    }

    public static PacketLogger getInstance() {
        return INST;
    }
}
