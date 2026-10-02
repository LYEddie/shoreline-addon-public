package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.ObsidianPlacerModule;
import me.lyeddie.addon.util.tabs.TabConfigs;
import me.lyeddie.addon.util.Animation;
import me.lyeddie.addon.util.literal.EntityUtil;
import me.lyeddie.addon.util.literal.ExplosionUtil;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Vec3d;
import net.minecraft.util.math.Vec3i;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class BasePlace extends ObsidianPlacerModule {
    private static BasePlace INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> multitaskConfig = addMultitaskConfig(sgGeneral);
    private final Setting<Boolean> strictDirectionConfig = addStrictDirectionConfig(sgGeneral);
    private final Setting<Boolean> rotateConfig = addRotateConfig(sgGeneral);
    private final Setting<Boolean> stopMotionConfig = addStopMotionConfig(sgGeneral);

    private final SettingGroup sgRender = settings.createGroup("Render");

    public final Setting<Double> placeRangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("PlaceRange").description("The placement range for bases")
        .defaultValue(4.0)
        .min(0.0)
        .sliderMax(6.0)
        .build());
    public final Setting<Double> enemyRangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("EnemyRange").description("The maximum range of targets")
        .defaultValue(10.0)
        .min(0.1)
        .sliderMax(15.0)
        .build());
    public final Setting<Double> shiftDelayConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("ShiftDelay").description("The delay between each block placement interval")
        .defaultValue(1.0)
        .min(0.0)
        .sliderMax(5.0)
        .build());
    public final Setting<Double> minDamageConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("MinDamage").description("Minimum damage required to place base")
        .defaultValue(4.0)
        .min(1.0)
        .sliderMax(10.0)
        .build());
    private final Setting<Boolean> assumeArmorConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("AssumeBestArmor").description("Assumes Prot 0 armor is max armor")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> renderConfig = sgRender.add(new BoolSetting.Builder()
        .name("Render").description("Renders where base blocks are being placed")
        .defaultValue(true)
        .build());
    public final Setting<Double> fadeTimeConfig = sgRender.add(new DoubleSetting.Builder()
        .name("Fade-Time").description("Time to fade")
        .defaultValue(250)
        .min(0)
        .sliderMax(1000)
        .build());

    private final Map<BlockPos, Long> packets = new HashMap<>();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private BlockPos crystalBase;
    private int startCooldown;

    public BasePlace() {
        super(Shoreline.MAIN, "BasePlace", "Places obsidian for crystal placements");
        INST = this;
    }

    @Override
    public void onDeactivate() {
        crystalBase = null;
        packets.clear();
        fadeList.clear();
    }

    @EventHandler(priority = -100)
    public void onTick(PlayerTickEvent event) { // seems bad¿
        if ((!multitaskConfig.get() && checkMultitask()) || (stopMotionConfig.get() && !mc.player.isOnGround())) {
            crystalBase = null;
            return;
        }

        if (!AutoCrystal.getInstance().isActive() || AutoCrystal.getInstance().isPlacing()) {
            crystalBase = null;
            startCooldown = 5;
            return;
        }

        startCooldown--;
        if (startCooldown > 0) {
            return;
        }

        PlayerEntity target = getClosestPlayer(enemyRangeConfig.get());
        if (target == null) {
            return;
        }

        crystalBase = getCrystalBase(target);
        if (crystalBase == null) {
            return;
        }

        BlockState state = mc.world.getBlockState(crystalBase);
        BlockSlot blockItem = this.getResistantBlockItem();

        if (blockItem == null || !state.isReplaceable()) {
            return;
        }

        Vec3d prevMotion = mc.player.getVelocity();
        if (stopMotionConfig.get()) {
            mc.player.setVelocity(0.0, 0.0, 0.0);
        }

        placeBlock(crystalBase, blockItem);

        if (stopMotionConfig.get()) {
            mc.player.setVelocity(prevMotion);
        }
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

            if (crystalBase != null && mc.world.isAir(crystalBase)) {
                Animation animation = new Animation(true, toFloat(fadeTimeConfig.get()));
                fadeList.put(crystalBase, animation);
            }
        }

        fadeList.entrySet().removeIf(e ->
            e.getValue().getFactor() == 0.0);
    }

    private void placeBlock(BlockPos pos, BlockSlot blockItem) {
        Managers.INTERACT.placeBlock(pos, blockItem.block() , blockItem.slot(), strictDirectionConfig.get(), false, true, (state, angles) -> {
            if (rotateConfig.get()) {
                if (state) {
                    Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
                } else {
                    Managers.ROTATION.setRotationSilentSync();
                }
            }
        });
        packets.put(pos, System.currentTimeMillis());
    }

    private BlockPos getCrystalBase(PlayerEntity player) {
        List<BlockPos> targetBlocks = getSphere(placeRangeConfig.get(), mc.player.getEyePos());
        double damage = 0.0f;
        BlockPos crystalBase = null;
        for (BlockPos pos : targetBlocks) {
            final BlockPos basePos = pos.down();
            BlockState state = mc.world.getBlockState(basePos);

            if (basePos.getY() >= EntityUtil.getRoundedBlockPos(player).getY() + 1.0F) {
                continue;
            }

            if (AutoCrystal.getInstance().isActive() && !AutoCrystal.getInstance().isPlacing() && !state.isReplaceable()) {
                continue;
            }

            Long placed = packets.get(basePos);
            if (shiftDelayConfig.get() > 0.0f && placed != null && (System.currentTimeMillis() - placed) < shiftDelayConfig.get() * 50.0f) {
                continue;
            }

            if (!AutoCrystal.getInstance().isCrystalHitboxClear(basePos)) {
                continue;
            }

            double dist = mc.player.squaredDistanceTo(basePos.toCenterPos());
            if (dist > getValueSq(placeRangeConfig.get())) {
                continue;
            }

            double dmg1 = ExplosionUtil.getDamageTo(player, pos.toCenterPos(), assumeArmorConfig.get());
            if (dmg1 < minDamageConfig.get()) {
                continue;
            }

            if (!AirPlaceII.getInstance().isActive()
                && Managers.INTERACT.getInteractDirectionInternal(basePos, strictDirectionConfig.get()) == null) {
                continue;
            }


            if (!Managers.INTERACT.canPlace(pos, Blocks.OBSIDIAN)) {
                continue;
            }

            if (dmg1 > damage) {
                crystalBase = basePos;
                damage = dmg1;
            }
        }

        return crystalBase;
    }

    private List<BlockPos> getSphere(double rad, Vec3d origin) {
        List<BlockPos> sphere = new ArrayList<>();
        for (double x = -rad; x <= rad; ++x) {
            for (double y = -rad; y <= rad; ++y) {
                for (double z = -rad; z <= rad; ++z) {
                    Vec3i pos = new Vec3i((int) (origin.getX() + x),
                        (int) (origin.getY() + y), (int) (origin.getZ() + z));
                    final BlockPos p = new BlockPos(pos);
                    sphere.add(p);
                }
            }
        }
        return sphere;
    }

    public static BasePlace getInstance() {
        return INST;
    }
}
