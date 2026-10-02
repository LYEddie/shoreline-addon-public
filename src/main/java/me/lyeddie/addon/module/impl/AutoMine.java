package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.AttackBlockEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.CombatModule;
import me.lyeddie.addon.util.tabs.TabConfigs;
import me.lyeddie.addon.util.*;
import me.lyeddie.addon.util.Timer;
import me.lyeddie.addon.util.literal.EntityUtil;
import me.lyeddie.addon.util.literal.PositionUtil;
import me.lyeddie.addon.util.literal.RotationUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.block.ShapeContext;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.UpdateSelectedSlotC2SPacket;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.util.Hand;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.util.shape.VoxelShapes;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.util.*;
import java.util.List;

public class AutoMine extends CombatModule {
    private static AutoMine INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRanges = settings.createGroup("Range");
    private final SettingGroup sgTargets = settings.createGroup("Targets");
    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Boolean> multitaskConfig = addMultitaskConfig(sgGeneral);

    private final Setting<Boolean> autoConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Auto").description("Automatically mines nearby players feet")
        .defaultValue(false)
        .build());
    public final Setting<Selection> selectionConfig = sgGeneral.add(new EnumSetting.Builder<Selection>()
        .name("Selection").description("The selection of blocks mine")
        .defaultValue(Selection.ALL)
        .visible(autoConfig::get)
        .build());
    private final Setting<List<Block>> whitelistConfig = sgGeneral.add(new BlockListSetting.Builder()
        .name("Whitelist").description("Valid block whitelist")
        .defaultValue(Blocks.OBSIDIAN, Blocks.ENDER_CHEST)
        .visible(() -> selectionConfig.get() == Selection.WHITELIST)
        .build());
    private final Setting<List<Block>> blacklistConfig = sgGeneral.add(new BlockListSetting.Builder()
        .name("Blacklist").description("Valid block blacklist")
        .defaultValue(Blocks.SHULKER_BOX)
        .visible(() -> selectionConfig.get() == Selection.BLACKLIST)
        .build());
    private final Setting<Boolean> avoidSelfConfig = sgTargets.add(new BoolSetting.Builder()
        .name("AvoidSelf").description("Avoids mining blocks in your surround")
        .defaultValue(false)
        .visible(autoConfig::get)
        .build());
    private final Setting<Boolean> strictDirectionConfig = sgMisc.add(new BoolSetting.Builder()
        .name("StrictDirection").description("Only mines on visible faces")
        .defaultValue(false)
        .visible(autoConfig::get)
        .build());
    public final Setting<Double> enemyRangeConfig = sgRanges.add(new DoubleSetting.Builder()
        .name("EnemyRange").description("Range to search for targets")
        .defaultValue(5.0)
        .min(1.0)
        .sliderMax(10.0)
        .visible(autoConfig::get)
        .build());
    private final Setting<Boolean> antiCrawlConfig = sgTargets.add(new BoolSetting.Builder()
        .name("AntiCrawl").description("Attempts to stop player from crawling")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> headConfig = sgTargets.add(new BoolSetting.Builder()
        .name("TargetBody").description("Attempts to mine players face blocks")
        .defaultValue(false)
        .visible(autoConfig::get)
        .build());
    private final Setting<Boolean> aboveHeadConfig = sgTargets.add(new BoolSetting.Builder()
        .name("TargetHead").description("Attempts to mine above players head")
        .defaultValue(false)
        .visible(autoConfig::get)
        .build());
    private final Setting<Boolean> doubleBreakConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("DoubleBreak").description("Allows you to mine two blocks at once")
        .defaultValue(false)
        .build());
    public final Setting<Integer> mineTicksConfig = sgGeneral.add(new IntSetting.Builder()
        .name("MiningTicks").description("The max number of ticks to hold a pickaxe for the packet mine")
        .defaultValue(20)
        .min(5)
        .sliderMax(60)
        .visible(doubleBreakConfig::get)
        .build());
    public final Setting<RemineMode> remineConfig = sgMisc.add(new EnumSetting.Builder<RemineMode>()
        .name("Remine").description("Remines already mined blocks")
        .defaultValue(RemineMode.NORMAL)
        .visible(autoConfig::get)
        .build());
    private final Setting<Boolean> packetInstantConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Fast").description("Instant mines on packet")
        .defaultValue(false)
        .visible(() -> remineConfig.get() == RemineMode.INSTANT)
        .build());
    public final Setting<Double> rangeConfig = sgRanges.add(new DoubleSetting.Builder()
        .name("Range").description("The range to mine blocks")
        .defaultValue(4.0)
        .min(0.1)
        .sliderMax(6.0)
        .build());
    public final Setting<Double> speedConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Speed").description("The speed to mine blocks")
        .defaultValue(1.0)
        .min(0.1)
        .sliderMax(1.0)
        .build());
    public final Setting<Swap> swapConfig = sgMisc.add(new EnumSetting.Builder<Swap>()
        .name("AutoSwap").description("Swaps to the best tool once the mining is complete")
        .defaultValue(Swap.SILENT)
        .build());
    private final Setting<Boolean> swapBeforeConfig = sgMisc.add(new BoolSetting.Builder()
        .name("SwapBefore").description("Swaps before fully done mining")
        .defaultValue(false)
        .visible(() -> swapConfig.get() != Swap.OFF)
        .build());
    private final Setting<Boolean> rotateConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Rotate").description("Rotates when mining the block")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> switchResetConfig = sgMisc.add(new BoolSetting.Builder()
        .name("SwitchReset").description("Resets mining after switching items")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> grimConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Grim").description("Uses grim block breaking speeds")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> grimNewConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("GrimV3").description("Allows mining on new grim servers")
        .defaultValue(false)
        .visible(grimConfig::get)
        .build());
    private final Setting<SettingColor> colorConfig = sgRender.add(new ColorSetting.Builder()
        .name("MineColor").description("The mine render color")
        .defaultValue(new SettingColor(Color.RED))
        .build());
    private final Setting<SettingColor> colorDoneConfig = sgRender.add(new ColorSetting.Builder()
        .name("DoneColor").description("The done render color")
        .defaultValue(new SettingColor(Color.GREEN))
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

    private final Timer remineTimer = new CacheTimer();
    private final Queue<MineData> autoMineQueue = new ArrayDeque<>();
    private PlayerEntity playerTarget;
    private MineData packetMine, instantMine;
    private boolean packetSwapBack;
    private boolean manualOverride;
    private boolean changedInstantMine;
    private boolean waitForPacketMine;
    private boolean packetMineStuck;
    private boolean antiCrawlOverride;
    private int antiCrawlTicks;
    private int autoMineTickDelay;
    private MineAnimation packetMineAnim = new MineAnimation(MineData.empty(), new Animation(true, 200));
    private MineAnimation instantMineAnim = new MineAnimation(MineData.empty(), new Animation(true, 200));

    public AutoMine() {
        super(Shoreline.MAIN, "AutoMine", "Automatically mines blocks", 900);
        INST = this;
    }

    @Override
    public String getInfoString() {
        if (instantMine != null) {
            return String.format("%.1f", Math.min(instantMine.getBlockDamage(), 1.0f));
        }
        return null;
    }

    @Override
    public void onDeactivate() {
        autoMineQueue.clear();
        playerTarget = null;
        packetMine = null;
        if (instantMine != null) {
            abortMining(instantMine);
            instantMine = null;
        }
        packetMineAnim = new MineAnimation(MineData.empty(), new Animation(true, 200));
        instantMineAnim = new MineAnimation(MineData.empty(), new Animation(true, 200));
        autoMineTickDelay = 0;
        antiCrawlTicks = 0;
        manualOverride = false;
        antiCrawlOverride = false;
        waitForPacketMine = false;
        packetMineStuck = false;
        if (packetSwapBack) {
            Managers.INVENTORY.syncToClient();
            packetSwapBack = false;
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        PlayerEntity currentTarget = getClosestPlayer(enemyRangeConfig.get());
        boolean targetChanged = playerTarget != null && playerTarget != currentTarget;
        playerTarget = currentTarget;

        if (isInstantMineComplete()) {
            if (changedInstantMine) {
                changedInstantMine = false;
            }
            if (waitForPacketMine) {
                waitForPacketMine = false;
            }
        }

        autoMineTickDelay--;
        antiCrawlTicks--;

        if (packetMine != null && packetMine.getTicksMining() > mineTicksConfig.get()) {
            packetMineStuck = true;
            packetMineAnim.animation.setState(false);
            if (packetSwapBack) {
                Managers.INVENTORY.syncToClient();
                packetSwapBack = false;
            }
            packetMine = null;
            if (!isInstantMineComplete()) {
                waitForPacketMine = true;
            }
        }

        if (packetMine != null) {
            final float damageDelta = SpeedMineII.getInstance().calcBlockBreakingDelta(
                packetMine.getState(), mc.world, packetMine.getPos());
            packetMine.addBlockDamage(damageDelta);

            int slot = packetMine.getBestSlot();
            float damageDone = packetMine.getBlockDamage() + (swapBeforeConfig.get()
                || packetMineStuck ? damageDelta : 0.0f);
            if (damageDone >= 1.0f && slot != -1 && !checkMultitask()) {
                Managers.INVENTORY.setSlot(slot);
                packetSwapBack = true;
                if (packetMineStuck) {
                    packetMineStuck = false;
                }
            }
        }

        if (packetSwapBack) {
            if (packetMine != null && canMine(packetMine.getState())) {
                packetMine.markAttemptedMine();
            } else {
                Managers.INVENTORY.syncToClient();
                packetSwapBack = false;
                packetMineAnim.animation.setState(false);
                packetMine = null;
                if (!isInstantMineComplete()) {
                    waitForPacketMine = true;
                }
            }
        }

        if (instantMine != null) {
            final double distance = mc.player.getEyePos().squaredDistanceTo(instantMine.getPos().toCenterPos());
            if (distance > getValueSq(rangeConfig.get()) || instantMine.getTicksMining() > mineTicksConfig.get()) {
                abortMining(instantMine);
                instantMineAnim.animation.setState(false);
                instantMine = null;
            }
        }

        if (instantMine != null) {
            final float damageDelta = SpeedMineII.getInstance().calcBlockBreakingDelta(
                instantMine.getState(), mc.world, instantMine.getPos());
            instantMine.addBlockDamage(damageDelta);

            if (instantMine.getBlockDamage() >= speedConfig.get()) {
                boolean canMine = canMine(instantMine.getState());
                boolean canPlace = mc.world.canPlace(instantMine.getState(), instantMine.getPos(), ShapeContext.absent());
                if (canMine) {
                    instantMine.markAttemptedMine();
                } else {
                    instantMine.resetMiningTicks();
                    if (remineConfig.get() == RemineMode.NORMAL || remineConfig.get() == RemineMode.FAST) {
                        instantMine.setTotalBlockDamage(0.0f, 0.0f);
                    }

                    if (manualOverride) {
                        manualOverride = false;
                        abortMining(instantMine);
                        instantMineAnim.animation.setState(false);
                        instantMine = null;
                    }
                }

                boolean passedRemine = remineConfig.get() == RemineMode.INSTANT || remineTimer.passed(500);
                if (instantMine != null && (remineConfig.get() == RemineMode.INSTANT
                    && packetInstantConfig.get() && packetMine == null && canPlace || canMine && passedRemine)
                    && (!checkMultitask() || multitaskConfig.get() || swapConfig.get() == Swap.OFF)) {
                    stopMining(instantMine);
                    remineTimer.reset();

                    if (AutoCrystal.getInstance().isActive()
                        && AutoCrystal.getInstance().shouldPreForcePlace()) {
                        AutoCrystal.getInstance().placeCrystalForTarget(playerTarget, instantMine.getPos().down());
                    }

                    if (remineConfig.get() == RemineMode.FAST) {
                        startMining(instantMine);
                    }
                }
            }
        }

        if (manualOverride && (instantMine == null || instantMine.getGoal() != MiningGoal.MANUAL)) {
            manualOverride = false;
        }

        if (antiCrawlOverride && (instantMine == null || instantMine.getGoal() != MiningGoal.PREVENT_CRAWL)) {
            antiCrawlOverride = false;
        }

        if (autoConfig.get()) {
            if (!autoMineQueue.isEmpty() && autoMineTickDelay <= 0) {
                MineData nextMine = autoMineQueue.poll();
                if (nextMine != null) {
                    startMining(nextMine);
                    autoMineTickDelay = 5;
                }
            }

            BlockPos antiCrawlPos = getAntiCrawlPos(playerTarget);
            if (antiCrawlOverride) {
                if (mc.player.getPose().equals(EntityPose.SWIMMING)) {
                    antiCrawlTicks = 10;
                }

                if (antiCrawlTicks <= 0 || !isInstantMineComplete() && antiCrawlPos != null
                    && !instantMine.getPos().equals(antiCrawlPos)) {
                    antiCrawlOverride = false;
                }
            }

            if (autoMineQueue.isEmpty() && !manualOverride && !antiCrawlOverride) {
                if (antiCrawlConfig.get() && mc.player.getPose().equals(EntityPose.SWIMMING) && antiCrawlPos != null) {
                    MineData data = new MineData(antiCrawlPos, strictDirectionConfig.get() ?
                        Managers.INTERACT.getInteractDirection(antiCrawlPos, false) : Direction.UP, MiningGoal.PREVENT_CRAWL);
                    if (isInstantMineComplete() || !instantMine.equals(data)) {
                        startAutoMine(data);
                        antiCrawlOverride = true;
                    }
                } else if (playerTarget != null && !targetChanged) {
                    BlockPos targetPos = EntityUtil.getRoundedBlockPos(playerTarget);
                    boolean bedrockPhased = PositionUtil.isBedrock(playerTarget.getBoundingBox(), targetPos) && !playerTarget.isCrawling();

                    if (!isInstantMineComplete() && checkDataY(instantMine, targetPos, bedrockPhased)) {
                        abortMining(instantMine);
                        instantMineAnim.animation.setState(false);
                        instantMine = null;
                    } else if (packetMine != null && checkDataY(packetMine, targetPos, bedrockPhased)) {
                        packetMineAnim.animation.setState(false);
                        if (packetSwapBack) {
                            Managers.INVENTORY.syncToClient();
                            packetSwapBack = false;
                        }
                        packetMine = null;
                        waitForPacketMine = false;
                    } else {
                        List<BlockPos> phasedBlocks = getPhaseBlocks(playerTarget, targetPos, bedrockPhased);

                        MineData bestMine;
                        if (!phasedBlocks.isEmpty()) {
                            BlockPos pos1 = phasedBlocks.removeFirst();
                            bestMine = new MineData(pos1, strictDirectionConfig.get() ?
                                Managers.INTERACT.getInteractDirection(pos1, false) : Direction.UP);

                            if (packetMine == null && doubleBreakConfig.get() || isInstantMineComplete()) {
                                startAutoMine(bestMine);
                            }
                        } else {
                            List<BlockPos> miningBlocks = getMiningBlocks(playerTarget, targetPos, bedrockPhased);
                            bestMine = getInstantMine(miningBlocks, bedrockPhased);

                            if (bestMine != null && (packetMine == null && !changedInstantMine
                                && doubleBreakConfig.get() || isInstantMineComplete())) {
                                startAutoMine(bestMine);
                            }
                        }
                    }
                } else {
                    if (!isInstantMineComplete() && instantMine.getGoal() == MiningGoal.MINING_ENEMY) {
                        abortMining(instantMine);
                        instantMineAnim.animation.setState(false);
                        instantMine = null;
                    }

                    if (packetMine != null && packetMine.getGoal() == MiningGoal.MINING_ENEMY) {
                        packetMineAnim.animation.setState(false);
                        if (packetSwapBack) {
                            Managers.INVENTORY.syncToClient();
                            packetSwapBack = false;
                        }
                        packetMine = null;
                        waitForPacketMine = false;
                    }
                }
            }
        }
    }

    @EventHandler
    public void onAttackBlock(AttackBlockEvent event) {
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }
        event.cancel();
        if (event.getState().getBlock().getHardness() == -1.0f || !canMine(event.getState()) || isMining(event.getPos())) {
            return;
        }

        MineData data = new MineData(event.getPos(), event.getDirection(), MiningGoal.MANUAL);

        if (instantMine != null && instantMine.getGoal() == MiningGoal.MINING_ENEMY
            || packetMine != null && packetMine.getGoal() == MiningGoal.MINING_ENEMY) {
            manualOverride = true;
        }

        if (!doubleBreakConfig.get()) {
            instantMine = data;
            startMining(instantMine);
            mc.player.swingHand(Hand.MAIN_HAND, false);
            return;
        }

        boolean updateChanged = false;
        if (!isInstantMineComplete() && !changedInstantMine) {
            if (packetMine == null) {
                packetMine = instantMine.copy();
                packetMineAnim = new MineAnimation(packetMine,
                    new Animation(true, fadeTimeConfig.get()));
            } else {
                updateChanged = true;
            }
        }

        instantMine = data;
        startMining(instantMine);
        mc.player.swingHand(Hand.MAIN_HAND, false);
        if (updateChanged) {
            changedInstantMine = true;
        }
    }

    @EventHandler(priority = 200)
    public void onPacketOutbound(PacketEvent.Send event) {
        if (event.packet instanceof UpdateSelectedSlotC2SPacket && switchResetConfig.get() && instantMine != null) {
            instantMine.setTotalBlockDamage(0.0f, 0.0f);
        }
    }

    @EventHandler(priority = 200)
    public void onPacketInbound(PacketEvent.Receive event) {
        if (event.packet instanceof BlockUpdateS2CPacket packet && canMine(packet.getState())) {
            if (antiCrawlOverride && packet.getPos().equals(getAntiCrawlPos(playerTarget))) {
                antiCrawlTicks = 10;
            }
        }
    }

    public void startAutoMine(MineData data) {
        if (!canMine(data.getState()) || isMining(data.getPos())) {
            return;
        }

        if (!doubleBreakConfig.get()) {
            instantMine = data;
            autoMineQueue.offer(data);
            return;
        }

        if (changedInstantMine && !isInstantMineComplete() || waitForPacketMine) {
            return;
        }

        boolean updateChanged = false;
        if (!isInstantMineComplete() && !changedInstantMine) {
            if (packetMine == null) {
                packetMine = instantMine.copy();
                packetMineAnim = new MineAnimation(packetMine,
                    new Animation(true, fadeTimeConfig.get()));
            } else {
                updateChanged = true;
            }
        }

        instantMine = data;
        autoMineQueue.offer(data);

        if (updateChanged) {
            changedInstantMine = true;
        }
    }

    public MineData getInstantMine(List<BlockPos> miningBlocks, boolean bedrockPhased) {
        PriorityQueue<MineData> validInstantMines = new PriorityQueue<>();
        for (BlockPos blockPos : miningBlocks) {
            BlockState state1 = mc.world.getBlockState(blockPos);
            if (!isAutoMineBlock(state1.getBlock())) {
                continue;
            }

            double dist = mc.player.getEyePos().squaredDistanceTo(blockPos.toCenterPos());
            if (dist > getValueSq(rangeConfig.get())) {
                continue;
            }

            BlockState state2 = mc.world.getBlockState(blockPos.down());
            if (bedrockPhased || state2.isOf(Blocks.OBSIDIAN) || state2.isOf(Blocks.BEDROCK)) {
                Direction direction = strictDirectionConfig.get() ?
                    Managers.INTERACT.getInteractDirection(blockPos, false) : Direction.UP;

                validInstantMines.add(new MineData(blockPos, direction));
            }
        }

        if (validInstantMines.isEmpty()) {
            return null;
        }

        return validInstantMines.peek();
    }

    public List<BlockPos> getPhaseBlocks(PlayerEntity player, BlockPos playerPos, boolean targetBedrockPhased) {
        List<BlockPos> phaseBlocks = PositionUtil.getAllInBox(player.getBoundingBox(),
            targetBedrockPhased && headConfig.get() ? playerPos.up() : playerPos);

        phaseBlocks.removeIf(p -> {
            BlockState state = mc.world.getBlockState(p);
            if (!isAutoMineBlock(state.getBlock()) || !canMine(state) || isMining(p)) {
                return true;
            }

            double dist = mc.player.getEyePos().squaredDistanceTo(p.toCenterPos());
            if (dist > getValueSq(rangeConfig.get())) {
                return true;
            }

            return avoidSelfConfig.get() && intersectsPlayer(p);
        });

        if (targetBedrockPhased && aboveHeadConfig.get()) {
            phaseBlocks.add(playerPos.up(2));
        }

        return phaseBlocks;
    }

    public List<BlockPos> getMiningBlocks(PlayerEntity player, BlockPos playerPos, boolean bedrockPhased) {
        List<BlockPos> surroundingBlocks = SurroundII.getInstance().getSurroundNoDown(player, toFloat(rangeConfig.get()));
        List<BlockPos> miningBlocks;
        if (bedrockPhased) {
            List<BlockPos> facePlaceBlocks = new ArrayList<>();
            if (headConfig.get()) {
                facePlaceBlocks.addAll(surroundingBlocks.stream().map(BlockPos::up).toList());
            }

            BlockState belowFeet = mc.world.getBlockState(playerPos.down());
            if (canMine(belowFeet)) {
                facePlaceBlocks.add(playerPos.down());
            }
            miningBlocks = facePlaceBlocks;
        } else {
            miningBlocks = surroundingBlocks;
        }

        miningBlocks.removeIf(p -> avoidSelfConfig.get() && intersectsPlayer(p));
        return miningBlocks;
    }

    private BlockPos getAntiCrawlPos(PlayerEntity playerTarget) {
        if (!mc.player.isOnGround()) {
            return null;
        }
        BlockPos crawlingPos = EntityUtil.getRoundedBlockPos(mc.player);
        boolean playerBelow = playerTarget != null && EntityUtil.getRoundedBlockPos(playerTarget).getY() < crawlingPos.getY();
        if (playerBelow) {
            BlockState state = mc.world.getBlockState(crawlingPos.down());
            if (isAutoMineBlock(state.getBlock()) && canMine(state)) {
                return crawlingPos.down();
            }
        } else {
            BlockState state = mc.world.getBlockState(crawlingPos.up());
            if (isAutoMineBlock(state.getBlock()) && canMine(state)) {
                return crawlingPos.up();
            }
        }
        return null;
    }

    private boolean checkDataY(MineData data, BlockPos targetPos, boolean bedrockPhased) {
        return data.getGoal() == MiningGoal.MINING_ENEMY && !bedrockPhased && data.getPos().getY() != targetPos.getY();
    }

    private boolean intersectsPlayer(BlockPos pos) {
        List<BlockPos> playerBlocks = SurroundII.getInstance().getPlayerBlocks(mc.player);
        List<BlockPos> surroundingBlocks = SurroundII.getInstance().getSurroundNoDown(mc.player);
        return playerBlocks.contains(pos) || surroundingBlocks.contains(pos);
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (mc.player.isCreative() || mc.player.isSpectator()) {
            return;
        }

        if (instantMineAnim != null && instantMineAnim.animation().getFactor() > 0.01f) {
            renderMiningData(event,
                instantMineAnim, true);
        }

        if (doubleBreakConfig.get() && packetMineAnim != null && packetMineAnim.animation().getFactor() > 0.01f) {
            renderMiningData(event,
                packetMineAnim, false);
        }
    }

    public void renderMiningData(Render3DEvent event, MineAnimation mineAnimation, boolean instantMine) {
        MineData data = mineAnimation.data();
        Animation animation = mineAnimation.animation();
        int boxAlpha = (int) (40 * animation.getFactor());
        int lineAlpha = (int) (100 * animation.getFactor());

        SettingColor boxColor;
        SettingColor lineColor;
        if (smoothColorConfig.get()) {
            boxColor = !canMine(data.getState()) ? getClampColor(colorDoneConfig.get(), boxAlpha) :
                interpolateColor(Math.min(data.getBlockDamage(), 1.0f), getClampColor(colorDoneConfig.get(), boxAlpha), getClampColor(colorConfig.get(), boxAlpha));
            lineColor = !canMine(data.getState()) ? getClampColor(colorDoneConfig.get(), lineAlpha) :
                interpolateColor(Math.min(data.getBlockDamage(), 1.0f), getClampColor(colorDoneConfig.get(), lineAlpha), getClampColor(colorConfig.get(), lineAlpha));
        } else {
            boxColor = data.getBlockDamage() >= 0.95f || !canMine(data.getState()) ? getClampColor(colorDoneConfig.get(), boxAlpha) : getClampColor(colorConfig.get(), boxAlpha);
            lineColor = data.getBlockDamage() >= 0.95f || !canMine(data.getState()) ? getClampColor(colorDoneConfig.get(), lineAlpha) : getClampColor(colorConfig.get(), lineAlpha);
        }

        BlockPos mining = data.getPos();
        VoxelShape outlineShape = VoxelShapes.fullCube();
        if (!instantMine || data.getBlockDamage() < speedConfig.get()) {
            outlineShape = data.getState().getOutlineShape(mc.world, mining);
            outlineShape = outlineShape.isEmpty() ? VoxelShapes.fullCube() : outlineShape;
        }
        Box render1 = outlineShape.getBoundingBox();
        Vec3d center = render1.offset(mining).getCenter();
        float total = instantMine ? toFloat(speedConfig.get()) : 1.0f;
        float scale = (instantMine && data.getBlockDamage() >= speedConfig.get()) || !canMine(data.getState()) ? 1.0f :
            MathHelper.clamp((data.getBlockDamage() + (data.getBlockDamage() - data.getLastDamage()) * event.tickDelta) / total, 0.0f, 1.0f);
        double dx = (render1.maxX - render1.minX) / 2.0;
        double dy = (render1.maxY - render1.minY) / 2.0;
        double dz = (render1.maxZ - render1.minZ) / 2.0;
        final Box scaled = new Box(center, center).expand(dx * scale, dy * scale, dz * scale);
        event.renderer.box(scaled, boxColor, lineColor, ShapeMode.Both, 0);
    }

    public void startMining(MineData data) {
        if (rotateConfig.get()) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), data.getPos().toCenterPos());
            if (grimConfig.get()) {
                setRotationSilent(rotations[0], rotations[1]);
            } else {
                setRotation(rotations[0], rotations[1]);
            }
        }

        if (doubleBreakConfig.get()) {
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
            } else {
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
                Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }
        } else {
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
                PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        }

        if (rotateConfig.get() && grimConfig.get()) {
            Managers.ROTATION.setRotationSilentSync();
        }

        instantMineAnim = new MineAnimation(data, new Animation(true, fadeTimeConfig.get()));
    }

    public void abortMining(MineData data) {
        Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }

    public void stopMining(MineData data) {
        if (rotateConfig.get()) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), data.getPos().toCenterPos());
            if (grimConfig.get()) {
                setRotationSilent(rotations[0], rotations[1]);
            } else {
                setRotation(rotations[0], rotations[1]);
            }
        }

        int slot = data.getBestSlot();
        if (slot != -1) {
            swapTo(slot);
        }

        stopMiningInternal(data);

        if (slot != -1) {
            swapSync(slot);
        }

        if (rotateConfig.get() && grimConfig.get()) {
            Managers.ROTATION.setRotationSilentSync();
        }
    }

    private void stopMiningInternal(MineData data) {
        Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, data.getPos(), data.getDirection()));
        Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
            PlayerActionC2SPacket.Action.ABORT_DESTROY_BLOCK, data.getPos(), data.getDirection()));
    }

    public boolean isInstantMineComplete() {
        return instantMine == null || instantMine.getBlockDamage() >= speedConfig.get() && !canMine(instantMine.getState());
    }

    public BlockPos getMiningBlock() {
        if (instantMine != null) {
            double damage = instantMine.getBlockDamage() / speedConfig.get();
            if (damage > 0.75) {
                return instantMine.getPos();
            }
        }
        return null;
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

    public boolean isSilentSwapping() {
        return packetSwapBack;
    }

    private boolean isMining(BlockPos blockPos) {
        return instantMine != null && instantMine.getPos().equals(blockPos) ||
            packetMine != null && packetMine.getPos().equals(blockPos);
    }

    private boolean isAutoMineBlock(Block block) {
        if (BlastResistantBlocks.isUnbreakable(block)) {
            return false;
        }
        return switch (selectionConfig.get()) {
            case WHITELIST -> whitelistConfig.get().contains(block);
            case BLACKLIST -> !blacklistConfig.get().contains(block);
            case ALL -> true;
        };
    }

    public boolean canMine(BlockState state) {
        return !state.isAir() && state.getFluidState().isEmpty();
    }

    public static AutoMine getInstance() {
        return INST;
    }

    public enum MiningGoal {
        MANUAL,
        MINING_ENEMY,
        PREVENT_CRAWL
    }

    public enum RemineMode {
        INSTANT,
        NORMAL,
        FAST
    }

    public enum Selection {
        WHITELIST,
        BLACKLIST,
        ALL
    }

    public enum Swap {
        NORMAL,
        SILENT,
        SILENT_ALT,
        OFF
    }

    public static class MineData implements Comparable<MineData>, Globals {
        private final BlockPos pos;
        private final Direction direction;
        private final MiningGoal goal;
        private int ticksMining;
        private float blockDamage, lastDamage;

        public MineData(BlockPos pos, Direction direction) {
            this.pos = pos;
            this.direction = direction;
            this.goal = MiningGoal.MINING_ENEMY;
        }

        public MineData(BlockPos pos, Direction direction, MiningGoal goal) {
            this.pos = pos;
            this.direction = direction;
            this.goal = goal;
        }

        public static MineData empty() {
            return new MineData(BlockPos.ORIGIN, Direction.UP);
        }

        private double getPriority() {
            double dist = mc.player.getEyePos().squaredDistanceTo(pos.down().toCenterPos());
            if (dist <= AutoCrystal.getInstance().getPlaceRange()) {
                return 10.0f;
            }

            return 0.0f;
        }

        @Override
        public int compareTo(@NotNull MineData o) {
            return Double.compare(getPriority(), o.getPriority());
        }

        @Override
        public boolean equals(Object obj) {
            return obj instanceof MineData d && d.getPos().equals(pos);
        }

        public void resetMiningTicks() {
            ticksMining = 0;
        }

        public void markAttemptedMine() {
            ticksMining++;
        }

        public void addBlockDamage(float blockDamage) {
            this.lastDamage = this.blockDamage;
            this.blockDamage += blockDamage;
        }

        public void setTotalBlockDamage(float blockDamage, float lastDamage) {
            this.blockDamage = blockDamage;
            this.lastDamage = lastDamage;
        }

        public BlockPos getPos() {
            return pos;
        }

        public Direction getDirection() {
            return direction;
        }

        public MiningGoal getGoal() {
            return goal;
        }

        public int getTicksMining() {
            return ticksMining;
        }

        public float getBlockDamage() {
            return blockDamage;
        }

        public float getLastDamage() {
            return lastDamage;
        }

        public MineData copy() {
            final MineData data = new MineData(pos, direction, goal);
            data.setTotalBlockDamage(blockDamage, lastDamage);
            return data;
        }

        public BlockState getState() {
            return mc.world.getBlockState(pos);
        }

        public int getBestSlot() {
            return AutoTool.getInstance().getBestToolNoFallback(getState());
        }
    }

    public record MineAnimation(MineData data, Animation animation) {
    }
}
