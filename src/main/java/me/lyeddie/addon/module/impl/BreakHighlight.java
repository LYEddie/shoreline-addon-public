package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.util.BlastResistantBlocks;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.awt.*;
import java.util.Comparator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class BreakHighlight extends AddonModule {
    private static BreakHighlight INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Double> rangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range").description("The range to render breaking blocks")
        .defaultValue(10.0)
        .min(5.0)
        .sliderMax(50.0)
        .build());
    private final Setting<SettingColor> colorConfig = sgGeneral.add(new ColorSetting.Builder()
        .name("Color").description("The break highlight color")
        .defaultValue(new SettingColor(255, 0, 0))
        .build());

    private final Map<ClientboundBlockDestructionPacket, Long> breakingProgress = new ConcurrentHashMap<>();

    public BreakHighlight() {
        super(Shoreline.MAIN, "BreakHighlight", "Highlights blocks that are being broken");
        INST = this;
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundBlockDestructionPacket packet
            && !BlastResistantBlocks.isUnbreakable(packet.getPos())) {
            ClientboundBlockDestructionPacket p = getPacketFromPos(packet.getPos());
            if (p != null) {
                breakingProgress.replace(p, System.currentTimeMillis());
            } else {
                breakingProgress.put(packet, System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        for (Map.Entry<ClientboundBlockDestructionPacket, Long> mine : breakingProgress.entrySet()) {
            BlockPos mining = mine.getKey().getPos();
            long elapsedTime = System.currentTimeMillis() - mine.getValue();
            long count = breakingProgress.keySet().stream().filter(p -> p.getId() == mine.getKey().getId()).count();
            while (count > 2) {
                breakingProgress.entrySet().stream().filter(p -> p.getKey().getId() == mine.getKey().getId())
                    .min(Comparator.comparingLong(Map.Entry::getValue)).ifPresent(min -> breakingProgress.remove(min.getKey(), min.getValue()));
                count--;
            }
            if (mc.level.isEmptyBlock(mining) || elapsedTime > 2500) {
                breakingProgress.remove(mine.getKey(), mine.getValue());
                continue;
            }
            double dist = mc.player.distanceToSqr(Vec3.atCenterOf(mining));
            if (dist > getValueSq(rangeConfig.get())) {
                continue;
            }
            VoxelShape outlineShape = mc.level.getBlockState(mining).getShape(mc.level, mining);
            outlineShape = outlineShape.isEmpty() ? Shapes.block() : outlineShape;
            AABB render1 = outlineShape.bounds();
            AABB render = new AABB(mining.getX() + render1.minX, mining.getY() + render1.minY,
                mining.getZ() + render1.minZ, mining.getX() + render1.maxX,
                mining.getY() + render1.maxY, mining.getZ() + render1.maxZ);
            Vec3 center = render.getCenter();
            float scale = Mth.clamp(elapsedTime / 2500.0f, 0.0f, 1.0f);
            double dx = (render1.maxX - render1.minX) / 2.0;
            double dy = (render1.maxY - render1.minY) / 2.0;
            double dz = (render1.maxZ - render1.minZ) / 2.0;
            final AABB scaled = new AABB(center, center).inflate(dx * scale, dy * scale, dz * scale);
            event.renderer.box(scaled, getClampColor(colorConfig.get(), 40), getClampColor(colorConfig.get(), 100), ShapeMode.Both, 0);
        }
    }

    private ClientboundBlockDestructionPacket getPacketFromPos(BlockPos pos) {
        return breakingProgress.keySet().stream().filter(p -> p.getPos().equals(pos)).findFirst().orElse(null);
    }

    public static BreakHighlight getInstance() {
        return INST;
    }
}
