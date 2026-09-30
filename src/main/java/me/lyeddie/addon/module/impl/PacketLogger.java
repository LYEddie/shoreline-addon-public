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
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.common.ServerboundPongPacket;
import net.minecraft.network.protocol.game.ServerboundAcceptTeleportationPacket;
import net.minecraft.network.protocol.game.ServerboundClientCommandPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundContainerClosePacket;
import net.minecraft.network.protocol.game.ServerboundInteractPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundMoveVehiclePacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromBlockPacket;
import net.minecraft.network.protocol.game.ServerboundPickItemFromEntityPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.BlockHitResult;
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
        .name("PickItem").description("Logs pick-item packets")
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

        Map<Identifier, Integer> packetCountMap = new HashMap<>();
        for (PacketLog packetLog : packetLogs) {
            Packet<?> packet = packetLog.packet();
            Identifier identifier = packet.type().id();
            if (packetCountMap.containsKey(identifier)) {
                packetCountMap.replace(identifier, packetCountMap.get(identifier) + 1);
            } else {
                packetCountMap.put(identifier, 1);
            }
        }

        List<String> strings = new ArrayList<>();
        for (Map.Entry<Identifier, Integer> entry : packetCountMap.entrySet()) {
            Identifier packet = entry.getKey();
            strings.add(packet.toShortLanguageKey() + ": " + entry.getValue());
        }

        info("PacketLogger", String.join(",", strings));
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundMovePlayerPacket.PosRot packet && moveFullConfig.get()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove Full - ");
            if (packet.hasPosition()) {
                builder.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
            }
            if (packet.hasRotation()) {
                builder.append("yaw: ").append(packet.getYRot(0.0f)).append(", pitch: ").append(packet.getXRot(0.0f)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(packet, builder.toString());
        }
        if (event.packet instanceof ServerboundMovePlayerPacket.Pos packet && movePosConfig.get()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove PosGround - ");
            if (packet.hasPosition()) {
                builder.append("x: ").append(packet.getX(0.0)).append(", y: ").append(packet.getY(0.0)).append(", z: ").append(packet.getZ(0.0)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(packet, builder.toString());
        }
        if (event.packet instanceof ServerboundMovePlayerPacket.Rot packet && moveLookConfig.get()) {
            StringBuilder builder = new StringBuilder();
            builder.append("PlayerMove LookGround - ");
            if (packet.hasRotation()) {
                builder.append("yaw: ").append(packet.getYRot(0.0f)).append(", pitch: ").append(packet.getXRot(0.0f)).append(" ");
            }
            builder.append(" onground: ").append(packet.isOnGround());
            logPacket(packet, builder.toString());
        }
        if (event.packet instanceof ServerboundMovePlayerPacket.StatusOnly packet && moveGroundConfig.get()) {
            String s = "PlayerMove Ground - onground: " + packet.isOnGround();
            logPacket(packet, s);
        }
        if (event.packet instanceof ServerboundMoveVehiclePacket packet && vehicleMoveConfig.get()) {
            logPacket(packet, "VehicleMove - pos: %s, yaw: %s, pitch: %s", packet.position(), packet.yRot(), packet.xRot());
        }
        if (event.packet instanceof ServerboundPlayerActionPacket packet && playerActionConfig.get()) {
            logPacket(packet, "PlayerAction - action: %s, direction: %s, pos: %s", packet.getAction().name(), packet.getDirection().name(), packet.getPos().toShortString());
        }
        if (event.packet instanceof ServerboundSetCarriedItemPacket packet && updateSlotConfig.get()) {
            logPacket(packet, "UpdateSlot - slot: %d", packet.getSlot());
        }
        if (event.packet instanceof ServerboundSwingPacket packet && handSwingConfig.get()) {
            logPacket(packet, "HandSwing - hand: %s", packet.getHand().name());
        }
        if (event.packet instanceof ServerboundPongPacket packet && pongConfig.get()) {
            logPacket(packet, "Pong - %d", packet.getId());
        }
        if (event.packet instanceof ServerboundInteractPacket packet && mc.level != null && interactEntityConfig.get()) {
            logPacket(packet, "InteractEntity");
        }
        if (event.packet instanceof ServerboundUseItemOnPacket packet && interactBlockConfig.get()) {
            BlockHitResult blockHitResult = packet.getHitResult();
            logPacket(packet, "InteractBlock - pos: %s, dir: %s, hand: %s", blockHitResult.getBlockPos().toShortString(), blockHitResult.getDirection().name(), packet.getHand().name());
        }
        if (event.packet instanceof ServerboundUseItemPacket packet && interactItemConfig.get()) {
            logPacket(packet, "InteractItem - hand: %s", packet.getHand().name());
        }
        if (event.packet instanceof ServerboundContainerClosePacket packet && closeScreenConfig.get()) {
            logPacket(packet, "CloseScreen - id: %s", packet.getContainerId());
        }
        if (event.packet instanceof ServerboundPlayerCommandPacket packet && commandConfig.get()) {
            logPacket(packet, "ClientCommand - mode: %s", packet.getAction().name());
        }
        if (event.packet instanceof ServerboundClientCommandPacket packet && statusConfig.get()) {
            logPacket(packet, "ClientStatus - mode: %s", packet.getAction().name());
        }
        if (event.packet instanceof ServerboundContainerClickPacket packet && clickSlotConfig.get()) {
            logPacket(packet, "ClickSlot - type: %s, slot: %s, button: %s, id: %s", packet.containerInput().name(), packet.slotNum(), packet.buttonNum(), packet.containerId());
        }
        if (event.packet instanceof ServerboundPickItemFromBlockPacket packet && pickInventoryConfig.get()) {
            logPacket(packet, "PickBlock - pos: %s, includeData: %s", packet.pos(), packet.includeData());
        }
        if (event.packet instanceof ServerboundPickItemFromEntityPacket packet && pickInventoryConfig.get()) {
            logPacket(packet, "PickEntity - id: %s, includeData: %s", packet.id(), packet.includeData());
        }
        if (event.packet instanceof ServerboundAcceptTeleportationPacket packet && teleportConfirmConfig.get()) {
            logPacket(packet, "TeleportConfirm - id: %s", packet.getId());
        }
    }

    public record PacketLog(Packet<?> packet, long time) {
    }

    public static PacketLogger getInstance() {
        return INST;
    }
}
