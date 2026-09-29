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
import net.minecraft.network.packet.s2c.play.BlockBreakingProgressS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;

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

    private final Map<BlockBreakingProgressS2CPacket, Long> breakingProgress = new ConcurrentHashMap<>();

    public BreakHighlight() {
        super(Shoreline.MAIN, "BreakHighlight", "Highlights blocks that are being broken");
        INST = this;
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (event.packet instanceof BlockBreakingProgressS2CPacket packet
            && !BlastResistantBlocks.isUnbreakable(packet.getPos())) {
            BlockBreakingProgressS2CPacket p = getPacketFromPos(packet.getPos());
            if (p != null) {
                breakingProgress.replace(p, System.currentTimeMillis());
            } else {
                breakingProgress.put(packet, System.currentTimeMillis());
            }
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null) {
            return;
        }

        for (Map.Entry<BlockBreakingProgressS2CPacket, Long> mine : breakingProgress.entrySet()) {
            BlockPos mining = mine.getKey().getPos();
            long elapsedTime = System.currentTimeMillis() - mine.getValue();
            long count = breakingProgress.keySet().stream().filter(p -> p.getEntityId() == mine.getKey().getEntityId()).count();
            while (count > 2) {
                breakingProgress.entrySet().stream().filter(p -> p.getKey().getEntityId() == mine.getKey().getEntityId())
                    .min(Comparator.comparingLong(Map.Entry::getValue)).ifPresent(min -> breakingProgress.remove(min.getKey(), min.getValue()));
                count--;
            }
            if (mc.world.isAir(mining) || elapsedTime > 2500) {
                breakingProgress.remove(mine.getKey(), mine.getValue());
                continue;
            }
            double dist = mc.player.squaredDistanceTo(mining.toCenterPos());
            if (dist > getValueSq(rangeConfig.get())) {
                continue;
            }
            VoxelShape outlineShape = mc.world.getBlockState(mining).getOutlineShape(mc.world, mining);
            outlineShape = outlineShape.isEmpty() ? VoxelShapes.fullCube() : outlineShape;
            Box render1 = outlineShape.getBoundingBox();
            Box render = new Box(mining.getX() + render1.minX, mining.getY() + render1.minY,
                mining.getZ() + render1.minZ, mining.getX() + render1.maxX,
                mining.getY() + render1.maxY, mining.getZ() + render1.maxZ);
            Vec3d center = render.getCenter();
            float scale = MathHelper.clamp(elapsedTime / 2500.0f, 0.0f, 1.0f);
            double dx = (render1.maxX - render1.minX) / 2.0;
            double dy = (render1.maxY - render1.minY) / 2.0;
            double dz = (render1.maxZ - render1.minZ) / 2.0;
            final Box scaled = new Box(center, center).expand(dx * scale, dy * scale, dz * scale);
            event.renderer.box(scaled, getClampColor(colorConfig.get(), 40), getClampColor(colorConfig.get(), 100), ShapeMode.Both, 0);
        }
    }

    private BlockBreakingProgressS2CPacket getPacketFromPos(BlockPos pos) {
        return breakingProgress.keySet().stream().filter(p -> p.getPos().equals(pos)).findFirst().orElse(null);
    }

    public static BreakHighlight getInstance() {
        return INST;
    }
}
