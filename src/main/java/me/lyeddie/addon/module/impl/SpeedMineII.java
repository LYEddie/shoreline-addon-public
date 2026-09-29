package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.api.RenderBuffers;
import me.lyeddie.addon.api.RenderManager;
import me.lyeddie.addon.events.AttackBlockEvent;
import me.lyeddie.addon.events.irrevocable.RenderWorldEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.mixin.impl.accessor.AccessorClientPlayerInteractionManager;
import me.lyeddie.addon.module.CombatModule;
import me.lyeddie.addon.tabs.TabConfigs;
import me.lyeddie.addon.util.*;
import me.lyeddie.addon.util.literal.EnchantmentUtil;
import me.lyeddie.addon.util.literal.RotationUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.effect.StatusEffectUtil;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import net.minecraft.world.BlockView;

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
            AccessorClientPlayerInteractionManager interactionManager = (AccessorClientPlayerInteractionManager) mc.interactionManager;
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
            final float damageDelta = calcBlockBreakingDelta(data.getState(), mc.world, data.getPos());
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
        final double distance = mc.player.getEyePos().squaredDistanceTo(miningData2.getPos().toCenterPos());
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

        if (event.getState().getBlock().getHardness() == -1.0f || event.getState().isAir()) {
            return;
        }

        startManualMine(event.getPos(), event.getDirection());
        mc.player.swingHand(Hand.MAIN_HAND);
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (event.packet instanceof PlayerActionC2SPacket packet
            && packet.getAction() == PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK
            && modeConfig.get() == SpeedmineMode.DAMAGE && grimConfig.get()) {
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, packet.getPos().up(500), packet.getDirection()));
        }

        if (event.packet instanceof UpdateSelectedSlotC2SPacket && switchResetConfig.get()
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

        if (event.packet instanceof BlockUpdateS2CPacket packet) {
            handleBlockUpdatePacket(packet);
        } else if (event.packet instanceof BundleS2CPacket packet) {
            for (Packet<?> packet1 : packet.getPackets()) {
                if (packet1 instanceof BlockUpdateS2CPacket packet2) {
                    handleBlockUpdatePacket(packet2);
                }
            }
        }
    }

    private void handleBlockUpdatePacket(BlockUpdateS2CPacket packet) {
        if (!packet.getState().isAir()) {
            return;
        }
        for (MiningData data : miningQueue) {
            if (data.hasAttemptedBreak() && data.getPos().equals(packet.getPos())) {
                data.setAttemptedBreak(false);
            }
        }
    }

    @EventHandler
    public void onRenderWorld(final RenderWorldEvent event) {
        if (mc.player.isCreative() || modeConfig.get() != SpeedmineMode.PACKET) {
            return;
        }

        if (AutoMine.getInstance().isActive()) {
            return;
        }

        RenderBuffers.preRender();
        for (Map.Entry<MiningData, Animation> set : fadeList.entrySet()) {
            MiningData data = set.getKey();
            set.getValue().setState(false);
            int boxAlpha = (int) (40 * set.getValue().getFactor());
            int lineAlpha = (int) (100 * set.getValue().getFactor());

            int boxColor;
            int lineColor;
            if (smoothColorConfig.get()) {
                boxColor = data.getState().isAir() ? getClampColor(colorDoneConfig.get(), boxAlpha).getRGB() :
                    interpolateColor(Math.min(data.getBlockDamage(), 1.0f), getClampColor(colorDoneConfig.get(), boxAlpha), getClampColor(colorConfig.get(), boxAlpha)).getRGB();
                lineColor = data.getState().isAir() ? getClampColor(colorDoneConfig.get(), lineAlpha).getRGB() :
                    interpolateColor(Math.min(data.getBlockDamage(), 1.0f), getClampColor(colorDoneConfig.get(), lineAlpha), getClampColor(colorConfig.get(), lineAlpha)).getRGB();
            } else {
                boxColor = data.getBlockDamage() >= 0.95f || data.getState().isAir() ? getClampColor(colorDoneConfig.get(), boxAlpha).getRGB() : getClampColor(colorConfig.get(), boxAlpha).getRGB();
                lineColor = data.getBlockDamage() >= 0.95f || data.getState().isAir() ? getClampColor(colorDoneConfig.get(), lineAlpha).getRGB() : getClampColor(colorConfig.get(), lineAlpha).getRGB();
            }

            BlockPos mining = data.getPos();
            VoxelShape outlineShape = data.getState().getOutlineShape(mc.world, mining);
            outlineShape = outlineShape.isEmpty() ? VoxelShapes.fullCube() : outlineShape;
            Box render1 = outlineShape.getBoundingBox();
            Box render = new Box(mining.getX() + render1.minX, mining.getY() + render1.minY,
                mining.getZ() + render1.minZ, mining.getX() + render1.maxX,
                mining.getY() + render1.maxY, mining.getZ() + render1.maxZ);
            Vec3d center = render.getCenter();
            float total = isDataPacketMine(data) ? 1.0f : toFloat(speedConfig.get());
            float scale = data.getState().isAir() ? 1.0f : MathHelper.clamp((data.getBlockDamage() + (data.getBlockDamage() - data.getLastDamage()) * event.getTickDelta()) / total, 0.0f, 1.0f);
            double dx = (render1.maxX - render1.minX) / 2.0;
            double dy = (render1.maxY - render1.minY) / 2.0;
            double dz = (render1.maxZ - render1.minZ) / 2.0;
            final Box scaled = new Box(center, center).expand(dx * scale, dy * scale, dz * scale);
            RenderManager.renderBox(event.getMatrices(), scaled, boxColor);
            RenderManager.renderBoundingBox(event.getMatrices(), scaled, 1.5f, lineColor);
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
        RenderBuffers.postRender();
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
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            } else {
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            }

            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            return true;
        }

        Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        return true;
    }

    private void abortMining(MiningData data) {
        if (!data.isStarted() || data.getState().isAir()) {
            return;
        }
        Managers.NETWORK.sendSequencedPacket(id -> new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection(), id));
        Managers.INVENTORY.syncToClient();
    }

    private void stopMining(MiningData data) {
        if (!data.isStarted() || data.getState().isAir()) {
            return;
        }
        if (rotateConfig.get()) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), data.getPos().toCenterPos());
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
        Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }

    private boolean isDataPacketMine(MiningData data) {
        return miningQueue.size() == 2 && data == miningQueue.getLast();
    }

    public float calcBlockBreakingDelta(BlockState state, BlockView world, BlockPos pos) {
        if (swapConfig.get() == Swap.OFF) {
            return state.calcBlockBreakingDelta(mc.player, mc.world, pos);
        }
        float f = state.getHardness(world, pos);
        if (f == -1.0f) {
            return 0.0f;
        } else {
            int i = canHarvest(state) ? 30 : 100;
            return getBlockBreakingSpeed(state) / f / (float) i;
        }
    }

    private float getBlockBreakingSpeed(BlockState block) {
        int tool = AutoTool.getInstance().getBestTool(block);
        float f = mc.player.getInventory().getStack(tool).getMiningSpeedMultiplier(block);
        if (f > 1.0F) {
            ItemStack stack = mc.player.getInventory().getStack(tool);
            int i = EnchantmentUtil.getLevel(stack, Enchantments.EFFICIENCY);
            if (i > 0 && !stack.isEmpty()) {
                f += (float) (i * i + 1);
            }
        }
        if (StatusEffectUtil.hasHaste(mc.player)) {
            f *= 1.0f + (float) (StatusEffectUtil.getHasteAmplifier(mc.player) + 1) * 0.2f;
        }
        if (mc.player.hasStatusEffect(StatusEffects.MINING_FATIGUE)) {
            float g = switch (mc.player.getStatusEffect(StatusEffects.MINING_FATIGUE).getAmplifier()) {
                case 0 -> 0.3f;
                case 1 -> 0.09f;
                case 2 -> 0.0027f;
                default -> 8.1e-4f;
            };
            f *= g;
        }
        if (!mc.player.isOnGround()) {
            f /= 5.0f;
        }
        return f;
    }

    private boolean canHarvest(BlockState state) {
        if (state.isToolRequired()) {
            int tool = AutoTool.getInstance().getBestTool(state);
            return mc.player.getInventory().getStack(tool).isSuitableFor(state);
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
            return mc.world.getBlockState(pos);
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
