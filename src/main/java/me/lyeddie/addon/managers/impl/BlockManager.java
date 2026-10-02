package me.lyeddie.addon.managers.impl;

import me.lyeddie.addon.events.staged.PrePlayerUpdateEvent;
import me.lyeddie.addon.module.impl.SpeedMineII;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundBlockDestructionPacket;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Collectors;

public class BlockManager implements Globals {
    private final List<BreakEntry> breakPositions = new CopyOnWriteArrayList<>();

    public BlockManager() {
        MeteorClient.EVENT_BUS.subscribe(this);
    }

    @EventHandler // check for pre/pos
    public void onTick(PrePlayerUpdateEvent event) {
        if (mc.player == null || mc.level == null) {
            breakPositions.clear();
            return;
        }

        for (BreakEntry blockEntry : breakPositions) {
            blockEntry.updateDamage();
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (event.packet instanceof ClientboundBlockDestructionPacket packet) {
            if (countBreaks(packet.getId()) >= 2) {
                breakPositions.stream().filter(d -> d.getEntityId() == packet.getId()).min(Comparator.comparingLong(BreakEntry::getStartTime)).ifPresent(breakPositions::remove);
            }
            BreakEntry data = new BreakEntry(packet.getId(), packet.getPos());
            data.startMining();
            breakPositions.add(data);
        }
    }

    public long countBreaks(int entityId) {
        return breakPositions.stream().filter(d -> d.getEntityId() == entityId).count();
    }

    public boolean isInstantMine(BlockPos pos) {
        return breakPositions.getFirst().getPos().equals(pos);
    }

    public boolean isBreaking(BlockPos pos) {
        return breakPositions.stream().anyMatch(d -> d.getPos().equals(pos));
    }

    public boolean isPassed(BlockPos pos, float blockDamage) {
        return breakPositions.stream().anyMatch(d -> d.getPos().equals(pos) && d.getBlockDamage() >= blockDamage);
    }

    public Set<BlockPos> getMines(float blockDamage) {
        return breakPositions.stream().filter(d -> isPassed(d.getPos(), blockDamage)).map(BreakEntry::getPos).collect(Collectors.toSet());
    }

    public static class BreakEntry {
        private final int entityId;
        private final BlockPos pos;
        private long startTime;
        private float blockDamage;
        private boolean started;

        public BreakEntry(int entityId, BlockPos pos) {
            this.entityId = entityId;
            this.pos = pos;
        }

        public void updateDamage() {
            if (started) {
                blockDamage += SpeedMineII.getInstance().calcBlockBreakingDelta(mc.level.getBlockState(pos), mc.level, pos);
            }
        }

        public void startMining() {
            started = true;
            startTime = System.currentTimeMillis();
        }

        public BlockPos getPos() {
            return pos;
        }

        public float getBlockDamage() {
            return Math.min(blockDamage, 1.0f);
        }

        public int getEntityId() {
            return entityId;
        }

        public long getStartTime() {
            return startTime;
        }
    }
}
