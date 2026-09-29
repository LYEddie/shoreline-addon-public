package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.ObsidianPlacerModule;
import me.lyeddie.addon.util.tabs.TabConfigs;
import me.lyeddie.addon.util.Animation;
import me.lyeddie.addon.util.BlastResistantBlocks;
import me.lyeddie.addon.util.literal.EntityUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.entity.Entity;
import net.minecraft.entity.ExperienceOrbEntity;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.network.packet.Packet;
import net.minecraft.network.packet.s2c.play.BlockUpdateS2CPacket;
import net.minecraft.network.packet.s2c.play.BundleS2CPacket;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.*;
import java.util.List;

public class CrawlTrap extends ObsidianPlacerModule {
    private static CrawlTrap INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> multitaskConfig = addMultitaskConfig(sgGeneral);
    private final Setting<Boolean> strictDirectionConfig = addStrictDirectionConfig(sgGeneral);
    private final Setting<Boolean> rotateConfig = addRotateConfig(sgGeneral);
    private final Setting<Boolean> stopMotionConfig = addStopMotionConfig(sgGeneral);

    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");
    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<Double> rangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("PlaceRange").description("The range to trap enemies")
        .defaultValue(4.0)
        .min(0.1)
        .sliderMax(6.0)
        .build());
    public final Setting<Double> enemyRangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("EnemyRange").description("The maximum range of targets")
        .defaultValue(10.0)
        .min(0.1)
        .sliderMax(15.0)
        .build());
    private final Setting<Boolean> downConfig = sgMisc.add(new BoolSetting.Builder()
        .name("PreventDownwards").description("Prevents digging downwards")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> serverHitboxConfig = sgMisc.add(new BoolSetting.Builder()
        .name("HitboxSync").description("Places on serverside crawling hitboxes")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> mineIgnoreConfig = sgMisc.add(new BoolSetting.Builder()
        .name("PreventMine").description("Prevents enemies from mining the trap")
        .defaultValue(false)
        .build());
    public final Setting<Integer> shiftTicksConfig = sgGeneral.add(new IntSetting.Builder()
        .name("ShiftTicks").description("The number of blocks to place per tick")
        .defaultValue(2)
        .min(1)
        .sliderMax(10)
        .build());
    public final Setting<Double> shiftDelayConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("ShiftDelay").description("The delay between each block placement interval")
        .defaultValue(1.0)
        .min(0.0)
        .sliderMax(5.0)
        .build());
    public final Setting<Integer> extrapolateTicksConfig = sgMisc.add(new IntSetting.Builder()
        .name("ExtrapolationTicks").description("Accounts for motion when calculating enemy positions, not fully accurate.")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build());
    private final Setting<Boolean> renderConfig = sgRender.add(new BoolSetting.Builder()
        .name("Render").description("Renders trap placements")
        .defaultValue(false)
        .build());
    public final Setting<Integer> fadeTimeConfig = sgRender.add(new IntSetting.Builder()
        .name("Fade-Time").description("Time to fade")
        .defaultValue(250)
        .min(0)
        .sliderMax(1000)
        .build());

    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private List<BlockPos> surround = new ArrayList<>();
    private List<BlockPos> placements = new ArrayList<>();
    private PlayerEntity target;
    private int blocksPlaced;

    public CrawlTrap() {
        super(Shoreline.MAIN, "CrawlTrap", "Automatically places blocks to keep enemies in crawl");
        INST = this;
    }

    @Override
    public void onDeactivate() {
        surround.clear();
        placements.clear();
        fadeList.clear();
        target = null;
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent event) {
        blocksPlaced = 0;

        if ((!multitaskConfig.get() && checkMultitask()) || (stopMotionConfig.get() && !mc.player.isOnGround())) {
            surround.clear();
            placements.clear();
            return;
        }

        BlockSlot blockItem = getResistantBlockItem();

        if (blockItem == null) {
            surround.clear();
            placements.clear();
            return;
        }
        target = getClosestPlayer(enemyRangeConfig.get());
        if (target == null) {
            surround.clear();
            placements.clear();
            return;
        }

        BlockPos targetPos = EntityUtil.getRoundedBlockPos(target);
        surround = getCrawlTrap(target, targetPos);
        if (!canCrawlTrap(target, targetPos) || surround.isEmpty()) {
            return;
        }

        placements = getPlacementsFromTrap(surround, blockItem.block());
        if (placements.isEmpty()) {
            return;
        }

        placements.sort(Comparator.comparingInt(Vec3i::getY));

        Vec3d prevMotion = mc.player.getVelocity();
        if (stopMotionConfig.get()) mc.player.setVelocity(0.0, 0.0, 0.0);

        while (blocksPlaced < shiftTicksConfig.get()) {
            if (blocksPlaced >= placements.size()) {
                break;
            }
            BlockPos targetPlacePos = placements.get(blocksPlaced);
            placeBlock(targetPlacePos, blockItem);
            blocksPlaced++;
        }

        if (rotateConfig.get()) {
            Managers.ROTATION.setRotationSilentSync();
        }

        if (stopMotionConfig.get()) mc.player.setVelocity(prevMotion);
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (event.packet instanceof BundleS2CPacket packet) {
            for (Packet<?> packet1 : packet.getPackets()) {
                handlePackets(packet1);
            }
        } else {
            handlePackets(event.packet);
        }
    }

    private void handlePackets(Packet<?> serverPacket) {
        if (serverPacket instanceof BlockUpdateS2CPacket packet) {
            final BlockState blockState = packet.getState();
            final BlockPos targetPos = packet.getPos();
            if (surround.contains(targetPos)) {
                if (blockState.isReplaceable()) {
                    BlockSlot blockItem = getResistantBlockItem();
                    if (blockItem == null) return;
                    placeBlock(targetPos, blockItem);
                } else if (BlastResistantBlocks.isBlastResistant(blockState)) {
                    packets.remove(targetPos);
                }
            }
        }
    }

    private void placeBlock(BlockPos pos, BlockSlot blockItem) {
        Managers.INTERACT.placeBlock(pos, blockItem.block(), blockItem.slot(), strictDirectionConfig.get(), false, true, (state, angles) -> {
            if (rotateConfig.get() && state) {
                Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
            }
        });
        packets.put(pos, System.currentTimeMillis());
    }

    public List<BlockPos> getPlacementsFromTrap(List<BlockPos> surround, Block block) {
        List<BlockPos> placements = new ArrayList<>();
        for (BlockPos surroundPos : surround) {
            Long placed = packets.get(surroundPos);
            if (shiftDelayConfig.get() > 0.0f && placed != null && (System.currentTimeMillis() - placed) < shiftDelayConfig.get() * 50.0f) {
                continue;
            }

            final Box surroundBox = new Box(surroundPos);
            List<Entity> invalid = mc.world.getOtherEntities(null, surroundBox).stream().filter(this::invalidEntity).toList();
            boolean serverCrawling = invalid.stream().allMatch(e -> Managers.HITBOX.isServerCrawling(e)
                && Managers.HITBOX.getCrawlingBoundingBox(e).intersects(surroundBox));

            if (!mc.world.getBlockState(surroundPos).isReplaceable()
                && !(serverCrawling && serverHitboxConfig.get())
                && !(Managers.BLOCK.isPassed(surroundPos, 0.7f) && mineIgnoreConfig.get())) {
                continue;
            }
            double dist = mc.player.squaredDistanceTo(surroundPos.toCenterPos());
            if (dist > getValueSq(rangeConfig.get())) {
                continue;
            }

            if (Managers.INTERACT.canPlace(surroundPos, block)) {
                placements.add(surroundPos);
            }
        }

        return placements;
    }

    public List<BlockPos> getCrawlTrap(PlayerEntity entity, BlockPos playerPos) {
        final List<BlockPos> crawlTrap = new ArrayList<>();
        crawlTrap.add(playerPos.up());
        if (downConfig.get()) {
            crawlTrap.add(playerPos.down());
        }

        double x = entity.getX();
        double y = entity.getY();
        double z = entity.getZ();

        int ticks = 0;
        while (ticks <= extrapolateTicksConfig.get()) {
            double ox = (x - entity.prevX) * ticks;
            double oz = (z - entity.prevZ) * ticks;
            BlockPos blockPos = BlockPos.ofFloored(x + ox, y, z + oz);
            if (!crawlTrap.contains(blockPos.up())) {
                crawlTrap.add(blockPos.up());
            }
            if (downConfig.get() && !crawlTrap.contains(blockPos.down())) {
                crawlTrap.add(blockPos.down());
            }
            ticks++;
        }
        return crawlTrap;
    }

    public boolean invalidEntity(Entity entity) {
        return !(entity instanceof ItemEntity) && !(entity instanceof ExperienceOrbEntity) && !(entity instanceof ArrowEntity);
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (renderConfig.get()) {
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet()) {
                set.getValue().setState(false);
                int boxAlpha = (int) (40 * set.getValue().getFactor());
                int lineAlpha = (int) (100 * set.getValue().getFactor());
                event.renderer.box(set.getKey(), TabConfigs.get().getClampColor(boxAlpha), TabConfigs.get().getClampColor(lineAlpha), ShapeMode.Both, 0);
            }

            if (placements.isEmpty()) {
                return;
            }

            for (BlockPos pos : placements) {
                Animation animation = new Animation(true, fadeTimeConfig.get());
                fadeList.put(pos, animation);
            }
        }

        fadeList.entrySet().removeIf(e -> e.getValue().getFactor() == 0.0);
    }

    private boolean canCrawlTrap(PlayerEntity player, BlockPos playerPos) {
        return player.isOnGround() || !mc.world.getBlockState(playerPos.up()).isReplaceable() || !mc.world.getBlockState(playerPos.up(2)).isReplaceable();
    }

    public boolean isPlacing() {
        return !placements.isEmpty();
    }

    public static CrawlTrap getInstance() {
        return INST;
    }
}
