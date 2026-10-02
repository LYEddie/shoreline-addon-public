package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.AttackBlockEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.mixin.impl.accessor.AccessorClientPlayerInteractionManager;
import me.lyeddie.addon.module.CombatModule;
import me.lyeddie.addon.util.tabs.TabConfigs;
import me.lyeddie.addon.util.*;
import me.lyeddie.addon.util.literal.EnchantmentUtil;
import me.lyeddie.addon.util.literal.RotationUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import java.awt.*;
import java.util.HashMap;
import java.util.Map;

public class SpeedMineII extends CombatModule {
    private static SpeedMineII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");
    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<SpeedmineMode> modeConfig = sgGeneral.add(new EnumSetting.Builder<SpeedmineMode>()
        .name("Mode").description("The mining mode for speedmine")
        .defaultValue(SpeedmineMode.PACKET)
        .build());
    private final Setting<Boolean> multitaskConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Multitask").description("Allows mining while using items")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build());
    private final Setting<Boolean> doubleBreakConfig = sgMisc.add(new BoolSetting.Builder()
        .name("DoubleBreak").description("Allows you to mine two blocks at once")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .onChanged(me -> onConfigUpdate())
        .build());
    public final Setting<Double> rangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range").description("The range to mine blocks")
        .defaultValue(4.0)
        .min(0.1)
        .sliderMax(6.0)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build());
    public final Setting<Double> speedConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Speed").description("The speed to mine blocks")
        .defaultValue(1.0)
        .min(0.1)
        .sliderMax(1.0)
        .build());
    private final Setting<Boolean> instantConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Instant").description("Instantly mines already broken blocks")
        .defaultValue(false)
        .build());
    public final Setting<Swap> swapConfig = sgMisc.add(new EnumSetting.Builder<Swap>()
        .name("AutoSwap").description("Swaps to the best tool once the mining is complete")
        .defaultValue(Swap.SILENT)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build());
    private final Setting<Boolean> rotateConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate").description("Rotates when mining the block")
        .defaultValue(true)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build());
    private final Setting<Boolean> switchResetConfig = sgMisc.add(new BoolSetting.Builder()
        .name("SwitchReset").description("Resets mining after switching items")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build());
    private final Setting<Boolean> grimConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Grim").description("Uses grim block breaking speeds")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> grimNewConfig = sgMisc.add(new BoolSetting.Builder()
        .name("GrimV3").description("Uses new grim block breaking speeds")
        .defaultValue(false)
        .visible(grimConfig::get)
        .build());
    private final Setting<SettingColor> colorConfig = sgRender.add(new ColorSetting.Builder()
        .name("MineColor").description("The mine render color")
        .defaultValue(new SettingColor(Color.RED))
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build());
    private final Setting<SettingColor> colorDoneConfig = sgRender.add(new ColorSetting.Builder()
        .name("DoneColor").description("The done render color")
        .defaultValue(new SettingColor(Color.GREEN))
        .visible(() -> modeConfig.get() == SpeedmineMode.PACKET)
        .build());
    public final Setting<Integer> fadeTimeConfig = sgRender.add(new IntSetting.Builder()
        .name("Fade-Time").description("Time to fade")
        .defaultValue(250)
        .min(0)
        .sliderMax(1000)
        .build());
    public final Setting<Boolean> smoothColorConfig = sgRender.add(new BoolSetting.Builder()
        .name("ColorSmooth").description("Interpolates from start to done color")
        .defaultValue(false)
        .build());

    private final Map<MiningData, Animation> fadeList = new HashMap<>();
    private FirstOutQueue<MiningData> miningQueue = new FirstOutQueue<>(2);
    private long lastBreak;

    public SpeedMineII() {
        super(Shoreline.MAIN, "SpeedMineII", "Mines blocks faster", 900);
        INST = this;
    }

    public void onConfigUpdate() {
        if (doubleBreakConfig.get()) {
            miningQueue = new FirstOutQueue<>(2);
        } else {
            miningQueue = new FirstOutQueue<>(1);
        }
    }

    @Override
    public String getInfoString() {
        if (modeConfig.get() == SpeedmineMode.PACKET) {
            MiningData miningData = miningQueue.peek();
            if (miningData != null) {
                return String.format("%.1f", Math.min(miningData.getBlockDamage(), 1.0f));
            }
        }
        return null;
    }

    @Override
    public void onDeactivate() {
        miningQueue.clear();
        fadeList.clear();
        Managers.INVENTORY.syncToClient();
    }

    @Override
    public void onActivate() {
        if (doubleBreakConfig.get()) {
            miningQueue = new FirstOutQueue<>(2);
        } else {
            miningQueue = new FirstOutQueue<>(1);
        }
    }

    @EventHandler
    public void onPlayerTick(final TickEvent.Pre event) {
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        if (modeConfig.get() == SpeedmineMode.DAMAGE) {
            AccessorClientPlayerInteractionManager interactionManager = (AccessorClientPlayerInteractionManager) mc.gameMode;
            if (interactionManager.hookGetCurrentBreakingProgress() >= speedConfig.get()) {
                interactionManager.hookSetCurrentBreakingProgress(1.0f);
            }
            return;
        }

        if (AutoMine.getInstance().isActive()) {
            return;
        }

        if (miningQueue.isEmpty()) {
            return;
        }
        for (MiningData data : miningQueue) {
            if (data.getState().isAir()) {
                data.resetBreakTime();
            }
            if (isDataPacketMine(data) && (data.getState().isAir() || data.hasAttemptedBreak()
                && data.passedAttemptedBreakTime(500))) {
                Managers.INVENTORY.syncToClient();
                miningQueue.remove(data);
                continue;
            }
            final float damageDelta = calcBlockBreakingDelta(data.getState(), mc.level, data.getPos());
            data.damage(damageDelta);
            if (isDataPacketMine(data) && data.getBlockDamage() >= 1.0f && data.getSlot() != -1) {
                if (mc.player.isUsingItem() && !multitaskConfig.get()) {
                    return;
                }

                if (data.getSlot() != Managers.INVENTORY.getServerSlot()) {
                    Managers.INVENTORY.setSlot(data.getSlot());
                }
                if (!data.hasAttemptedBreak()) {
                    data.setAttemptedBreak(true);
                }
            }
        }
        MiningData miningData2 = miningQueue.getFirst();
        final double distance = mc.player.getEyePosition().distanceToSqr(miningData2.getPos().getCenter());
        if (distance > getValueSq(rangeConfig.get())) {
            miningQueue.remove(miningData2);
            return;
        }
        if (miningData2.getState().isAir()) {
            return;
        }
        if (miningData2.getBlockDamage() >= speedConfig.get() && miningData2.hasAttemptedBreak()
            && miningData2.passedAttemptedBreakTime(500)) {
            abortMining(miningData2);
            miningQueue.remove(miningData2);
        }
        if (miningData2.getBlockDamage() >= speedConfig.get()) {
            if (mc.player.isUsingItem() && !multitaskConfig.get()) {
                return;
            }
            stopMining(miningData2);

            if (!instantConfig.get()) {
                miningQueue.remove(miningData2);
            }

            if (!miningData2.hasAttemptedBreak()) {
                miningData2.setAttemptedBreak(true);
            }
        }
    }

    @EventHandler
    public void onAttackBlock(final AttackBlockEvent event) {
        if (mc.player.isCreative() || mc.player.isSpectator() || modeConfig.get() != SpeedmineMode.PACKET) {
            return;
        }

        if (AutoMine.getInstance().isActive()) {
            return;
        }
        event.cancel();

        if (event.getState().getBlock().defaultDestroyTime() == -1.0f || event.getState().isAir()) {
            return;
        }

        startManualMine(event.getPos(), event.getDirection());
        mc.player.swing(InteractionHand.MAIN_HAND);
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (event.packet instanceof ServerboundPlayerActionPacket packet
            && packet.getAction() == ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK
            && modeConfig.get() == SpeedmineMode.DAMAGE && grimConfig.get()) {
            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(
                ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos().above(500), packet.getDirection()));
        }

        if (event.packet instanceof ServerboundSetCarriedItemPacket && switchResetConfig.get()
            && modeConfig.get() == SpeedmineMode.PACKET) {
            for (MiningData data : miningQueue) {
                data.resetDamage();
            }
        }
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || modeConfig.get() != SpeedmineMode.PACKET) {
            return;
        }

        if (AutoMine.getInstance().isActive()) {
            return;
        }

        if (event.packet instanceof ClientboundBlockUpdatePacket packet) {
            handleBlockUpdatePacket(packet);
        } else if (event.packet instanceof ClientboundBundlePacket packet) {
            for (Packet<?> packet1 : packet.subPackets()) {
                if (packet1 instanceof ClientboundBlockUpdatePacket packet2) {
                    handleBlockUpdatePacket(packet2);
                }
            }
        }
    }

    private void handleBlockUpdatePacket(ClientboundBlockUpdatePacket packet) {
        if (!packet.getBlockState().isAir()) {
            return;
        }
        for (MiningData data : miningQueue) {
            if (data.hasAttemptedBreak() && data.getPos().equals(packet.getPos())) {
                data.setAttemptedBreak(false);
            }
        }
    }

    @EventHandler
    public void onRender(final Render3DEvent event) {
        if (mc.player.isCreative() || modeConfig.get() != SpeedmineMode.PACKET) {
            return;
        }

        if (AutoMine.getInstance().isActive()) {
            return;
        }

        for (Map.Entry<MiningData, Animation> set : fadeList.entrySet()) {
            MiningData data = set.getKey();
            set.getValue().setState(false);
            int boxAlpha = (int) (40 * set.getValue().getFactor());
            int lineAlpha = (int) (100 * set.getValue().getFactor());

            SettingColor boxColor;
            SettingColor lineColor;
            if (smoothColorConfig.get()) {
                boxColor = data.getState().isAir() ? getClampColor(colorDoneConfig.get(), boxAlpha) :
                    interpolateColor(Math.min(data.getBlockDamage(), 1.0f), getClampColor(colorDoneConfig.get(), boxAlpha), getClampColor(colorConfig.get(), boxAlpha));
                lineColor = data.getState().isAir() ? getClampColor(colorDoneConfig.get(), lineAlpha) :
                    interpolateColor(Math.min(data.getBlockDamage(), 1.0f), getClampColor(colorDoneConfig.get(), lineAlpha), getClampColor(colorConfig.get(), lineAlpha));
            } else {
                boxColor = data.getBlockDamage() >= 0.95f || data.getState().isAir() ? getClampColor(colorDoneConfig.get(), boxAlpha) : getClampColor(colorConfig.get(), boxAlpha);
                lineColor = data.getBlockDamage() >= 0.95f || data.getState().isAir() ? getClampColor(colorDoneConfig.get(), lineAlpha) : getClampColor(colorConfig.get(), lineAlpha);
            }

            BlockPos mining = data.getPos();
            VoxelShape outlineShape = data.getState().getShape(mc.level, mining);
            outlineShape = outlineShape.isEmpty() ? Shapes.block() : outlineShape;
            AABB render1 = outlineShape.bounds();
            AABB render = new AABB(mining.getX() + render1.minX, mining.getY() + render1.minY,
                mining.getZ() + render1.minZ, mining.getX() + render1.maxX,
                mining.getY() + render1.maxY, mining.getZ() + render1.maxZ);
            Vec3 center = render.getCenter();
            float total = isDataPacketMine(data) ? 1.0f : toFloat(speedConfig.get());
            float scale = data.getState().isAir() ? 1.0f : Mth.clamp((data.getBlockDamage() + (data.getBlockDamage() - data.getLastDamage()) * event.tickDelta) / total, 0.0f, 1.0f);
            double dx = (render1.maxX - render1.minX) / 2.0;
            double dy = (render1.maxY - render1.minY) / 2.0;
            double dz = (render1.maxZ - render1.minZ) / 2.0;
            final AABB scaled = new AABB(center, center).inflate(dx * scale, dy * scale, dz * scale);
            event.renderer.box(scaled, boxColor, lineColor, ShapeMode.Both, 0);
        }
        for (MiningData data : miningQueue) {
            if (data.getState().isAir()) {
                continue;
            }
            Animation animation = new Animation(true, fadeTimeConfig.get());
            fadeList.put(data, animation);
        }
        fadeList.entrySet().removeIf(e ->
            e.getValue().getFactor() == 0.0);
    }

    private void startManualMine(BlockPos pos, Direction direction) {
        clickMine(new MiningData(pos, direction));
    }

    public void clickMine(MiningData miningData) {
        int queueSize = miningQueue.size();
        if (queueSize <= 2) {
            queueMiningData(miningData);
        }
    }

    private void queueMiningData(MiningData data) {
        if (data.getState().isAir()) {
            return;
        }
        if (startMining(data)) {
            if (miningQueue.stream().anyMatch(p1 -> data.getPos().equals(p1.getPos()))) {
                return;
            }
            miningQueue.addFirst(data);
        }
    }

    private boolean startMining(MiningData data) {
        if (data.isStarted()) {
            return false;
        }

        data.setStarted();
        if (grimNewConfig.get()) {
            if (!TabConfigs.get().getMiningFix()) {
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            } else {
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            }

            Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            return true;
        }

        Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
        return true;
    }

    private void abortMining(MiningData data) {
        if (!data.isStarted() || data.getState().isAir()) {
            return;
        }
        Managers.NETWORK.sendSequencedPacket(id -> new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection(), id));
        Managers.INVENTORY.syncToClient();
    }

    private void stopMining(MiningData data) {
        if (!data.isStarted() || data.getState().isAir()) {
            return;
        }
        if (rotateConfig.get()) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), data.getPos().getCenter());
            if (grimConfig.get()) {
                setRotationSilent(rotations[0], rotations[1]);
            } else {
                setRotation(rotations[0], rotations[1]);
            }
        }
        int slot = data.getSlot();
        boolean canSwap = slot != -1 && slot != Managers.INVENTORY.getServerSlot();
        if (canSwap) {
            swapTo(slot);
        }
        stopMiningInternal(data);
        lastBreak = System.currentTimeMillis();
        if (canSwap) {
            swapSync(slot);
        }
        if (rotateConfig.get()) {
            Managers.ROTATION.setRotationSilentSync();
        }
    }

    private void swapTo(int slot) {
        switch (swapConfig.get()) {
            case NORMAL -> Managers.INVENTORY.setClientSlot(slot);
            case SILENT -> Managers.INVENTORY.setSlot(slot);
            case SILENT_ALT -> Managers.INVENTORY.setSlotAlt(slot);
        }
    }

    private void swapSync(int slot) {
        switch (swapConfig.get()) {
            case SILENT -> Managers.INVENTORY.syncToClient();
            case SILENT_ALT -> Managers.INVENTORY.setSlotAlt(slot);
        }
    }

    private void stopMiningInternal(MiningData data) {
        Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }

    private boolean isDataPacketMine(MiningData data) {
        return miningQueue.size() == 2 && data == miningQueue.getLast();
    }

    public float calcBlockBreakingDelta(BlockState state, BlockGetter world, BlockPos pos) {
        if (swapConfig.get() == Swap.OFF) {
            return state.getDestroyProgress(mc.player, mc.level, pos);
        }
        float f = state.getDestroySpeed(world, pos);
        if (f == -1.0f) {
            return 0.0f;
        } else {
            int i = canHarvest(state) ? 30 : 100;
            return getBlockBreakingSpeed(state) / f / (float) i;
        }
    }

    private float getBlockBreakingSpeed(BlockState block) {
        int tool = AutoTool.getInstance().getBestTool(block);
        float f = mc.player.getInventory().getItem(tool).getDestroySpeed(block);
        if (f > 1.0F) {
            ItemStack stack = mc.player.getInventory().getItem(tool);
            int i = EnchantmentUtil.getLevel(stack, Enchantments.EFFICIENCY);
            if (i > 0 && !stack.isEmpty()) {
                f += (float) (i * i + 1);
            }
        }
        if (MobEffectUtil.hasDigSpeed(mc.player)) {
            f *= 1.0f + (float) (MobEffectUtil.getDigSpeedAmplification(mc.player) + 1) * 0.2f;
        }
        if (mc.player.hasEffect(MobEffects.MINING_FATIGUE)) {
            float g = switch (mc.player.getEffect(MobEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1e-4f;
            };
            f *= g;
        }
        if (!mc.player.onGround()) {
            f /= 5.0f;
        }
        return f;
    }

    private boolean canHarvest(BlockState state) {
        if (state.requiresCorrectToolForDrops()) {
            int tool = AutoTool.getInstance().getBestTool(state);
            return mc.player.getInventory().getItem(tool).isCorrectToolForDrops(state);
        }
        return true;
    }

    public static SpeedMineII getInstance() {
        return INST;
    }

    public enum SpeedmineMode {
        PACKET,
        DAMAGE
    }

    public enum Swap {
        NORMAL,
        SILENT,
        SILENT_ALT,
        OFF
    }

    public static class MiningData implements Globals {
        private final BlockPos pos;
        private final Direction direction;
        private boolean attemptedBreak;
        private long breakTime;
        private float lastDamage;
        private float blockDamage;
        private boolean started;

        public MiningData(BlockPos pos, Direction direction) {
            this.pos = pos;
            this.direction = direction;
        }

        public void setAttemptedBreak(boolean attemptedBreak) {
            this.attemptedBreak = attemptedBreak;
            if (attemptedBreak) {
                resetBreakTime();
            }
        }

        public void resetBreakTime() {
            breakTime = System.currentTimeMillis();
        }

        public boolean hasAttemptedBreak() {
            return attemptedBreak;
        }

        public boolean passedAttemptedBreakTime(long time) {
            return System.currentTimeMillis() - breakTime >= time;
        }

        public float damage(final float dmg) {
            lastDamage = blockDamage;
            blockDamage += dmg;
            return blockDamage;
        }

        public void resetDamage() {
            started = false;
            blockDamage = 0.0f;
        }

        public BlockPos getPos() {
            return pos;
        }

        public Direction getDirection() {
            return direction;
        }

        public int getSlot() {
            return AutoTool.getInstance().getBestToolNoFallback(getState());
        }

        public BlockState getState() {
            return mc.level.getBlockState(pos);
        }

        public float getBlockDamage() {
            return blockDamage;
        }

        public float getLastDamage() {
            return lastDamage;
        }

        public boolean isStarted() {
            return started;
        }

        public void setStarted() {
            this.started = true;
        }
    }
}
