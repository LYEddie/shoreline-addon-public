package me.lyeddie.addon.module.impl;

import com.google.common.collect.Lists;
import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.*;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.events.irrevocable.RunTickEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.CombatModule;
import me.lyeddie.addon.util.tabs.TabConfigs;
import me.lyeddie.addon.util.*;
import me.lyeddie.addon.util.literal.*;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundBundlePacket;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundSetCarriedItemPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.tags.ItemTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ExperienceOrb;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.AxeItem;
import net.minecraft.world.item.EndCrystalItem;
import net.minecraft.world.item.ExperienceBottleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import java.awt.*;
import java.text.DecimalFormat;
import java.util.*;
import java.util.List;
import java.util.concurrent.*;

public class AutoCrystal extends CombatModule {
    private static AutoCrystal INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgHidden = settings.createGroup("Hidden");
    private final SettingGroup sgPlace = settings.createGroup("Place");
    private final SettingGroup sgBreak = settings.createGroup("Break");
    private final SettingGroup sgDmg = settings.createGroup("Damage");
    private final SettingGroup sgRots = settings.createGroup("Rotations");
    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");
    private final SettingGroup sgTargets = settings.createGroup("Targets");
    private final SettingGroup sgRender = settings.createGroup("Renders");
    private final SettingGroup sgDebug = settings.createGroup("Debug");

    private final Setting<Boolean> multitaskConfig = addMultitaskConfig(sgGeneral);

    private final Setting<Boolean> whileMiningConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("WhileMining").description("Allows attacking while mining blocks")
        .defaultValue(false)
        .build());
    public final Setting<Double> targetRangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("EnemyRange").description("Range to search for potential enemies")
        .defaultValue(10.0)
        .min(1.0)
        .sliderMax(13.0)
        .build());
    private final Setting<Boolean> instantConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Instant").description("Instantly attacks crystals when they spawn")
        .defaultValue(false)
        .build());
    public final Setting<Sequential> sequentialConfig = sgGeneral.add(new EnumSetting.Builder<Sequential>()
        .name("Sequential").description("Places a crystal after spawn")
        .defaultValue(Sequential.NONE)
        .build());
    private final Setting<Boolean> idPredictConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("BreakPredict").description("Attempts to predict crystal entity ids")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> instantCalcConfig = sgHidden.add(new BoolSetting.Builder()
        .name("Instant-Calc").description("Calculates a crystal when it spawns and attacks if it meets MINIMUM requirements, this will result in non-ideal crystal attacks")
        .defaultValue(false)
        .build());
    public final Setting<Double> instantDamageConfig = sgHidden.add(new DoubleSetting.Builder()
        .name("InstantDamage").description("Minimum damage to attack crystals instantly")
        .defaultValue(6.0)
        .min(1.0)
        .sliderMax(10.0)
        .build());
    private final Setting<Boolean> instantMaxConfig = sgHidden.add(new BoolSetting.Builder()
        .name("InstantMax").description("Attacks crystals instantly if they exceed the previous max attack damage (Note: This is still not a perfect check because the next tick could have better damages)")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> raytraceConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Raytrace").description("Raytrace to crystal position")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> swingConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Swing")
        .description("Swing hand when placing and attacking crystals")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> rotateConfig = sgRots.add(new BoolSetting.Builder()
        .name("Rotate").description("Rotate before placing and breaking")
        .defaultValue(false)
        .build());
    public final Setting<Rotate> strictRotateConfig = sgRots.add(new EnumSetting.Builder<Rotate>()
        .name("YawStep").description("Rotates yaw over multiple ticks to prevent certain rotation flags in NCP")
        .defaultValue(Rotate.OFF)
        .build());
    public final Setting<Integer> rotateLimitConfig = sgRots.add(new IntSetting.Builder()
        .name("YawStep-Limit").description("Maximum yaw rotation in degrees for one tick")
        .defaultValue(180)
        .min(1)
        .sliderMax(180)
        .visible(() -> rotateConfig.get() && strictRotateConfig.get() != Rotate.OFF)
        .build());
    private final Setting<Boolean> playersConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Players").description("Target players")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> monstersConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Monsters").description("Target monsters")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> neutralsConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Neutrals").description("Target neutrals")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> animalsConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Animals").description("Target animals")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> shulkersConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Shulkers").description("Target shulker boxes")
        .defaultValue(false)
        .build());
    public final Setting<Double> breakSpeedConfig = sgBreak.add(new DoubleSetting.Builder()
        .name("BreakSpeed").description("Speed to break crystals")
        .defaultValue(18.0)
        .min(0.1)
        .sliderMax(20.0)
        .build());
    public final Setting<Double> attackDelayConfig = sgBreak.add(new DoubleSetting.Builder()
        .name("AttackDelay").description("Added delays")
        .defaultValue(0.0)
        .min(0.0)
        .sliderMax(5.0)
        .build());
    public final Setting<Integer> attackFactorConfig = sgBreak.add(new IntSetting.Builder()
        .name("AttackFactor").description("Factor of attack delay")
        .defaultValue(0)
        .min(0)
        .sliderMax(3)
        .visible(() -> attackDelayConfig.get() > 0.0)
        .build());
    public final Setting<Double> attackLimitConfig = sgBreak.add(new DoubleSetting.Builder()
        .name("AttackLimit").description("The number of attacks before considering a crystal unbreakable")
        .defaultValue(1.5)
        .min(0.5)
        .sliderMax(20.0)
        .build());
    private final Setting<Boolean> breakDelayConfig = sgBreak.add(new BoolSetting.Builder()
        .name("BreakDelay").description("Uses attack latency to calculate break delays")
        .defaultValue(false)
        .build());
    public final Setting<Double> breakTimeoutConfig = sgBreak.add(new DoubleSetting.Builder()
        .name("BreakTimeout").description("Time after waiting for the average break time before considering a crystal attack failed")
        .defaultValue(3.0)
        .min(0.0)
        .sliderMax(10.0)
        .visible(breakDelayConfig::get)
        .build());
    public final Setting<Double> minTimeoutConfig = sgBreak.add(new DoubleSetting.Builder()
        .name("MinTimeout").description("Minimum time before considering a crystal break/place failed")
        .defaultValue(5.0)
        .min(0.0)
        .sliderMax(20.0)
        .visible(breakDelayConfig::get)
        .build());
    public final Setting<Integer> ticksExistedConfig = sgBreak.add(new IntSetting.Builder()
        .name("TicksExisted").description("Minimum ticks alive to consider crystals for attack")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build());
    public final Setting<Double> breakRangeConfig = sgBreak.add(new DoubleSetting.Builder()
        .name("BreakRange").description("Range to break crystals")
        .defaultValue(4.0)
        .min(0.1)
        .sliderMax(6.0)
        .build());
    public final Setting<Double> maxYOffsetConfig = sgBreak.add(new DoubleSetting.Builder()
        .name("MaxYOffset").description("Maximum crystal y-offset difference")
        .defaultValue(5.0)
        .min(1.0)
        .sliderMax(10.0)
        .build());
    public final Setting<Double> breakWallRangeConfig = sgBreak.add(new DoubleSetting.Builder()
        .name("BreakWallRange").description("Range to break crystals through walls")
        .defaultValue(4.0)
        .min(0.1)
        .sliderMax(6.0)
        .build());
    public final Setting<Swap> antiWeaknessConfig = sgMisc.add(new EnumSetting.Builder<Swap>()
        .name("AntiWeakness").description("Swap to tools before attacking crystals")
        .defaultValue(Swap.OFF)
        .build());
    public final Setting<Double> swapDelayConfig = sgMisc.add(new DoubleSetting.Builder()
        .name("SwapPenalty").description("Delay for attacking after swapping items which prevents NCP flags")
        .defaultValue(0.0)
        .min(0.0)
        .sliderMax(10.0)
        .build());
    private final Setting<Boolean> inhibitConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Inhibit").description("Prevents excessive attacks")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> placeConfig = sgPlace.add(new BoolSetting.Builder()
        .name("Place").description("Places crystals to damage enemies. Place settings will only function if this setting is enabled.")
        .defaultValue(true)
        .build());
    public final Setting<Double> placeSpeedConfig = sgPlace.add(new DoubleSetting.Builder()
        .name("PlaceSpeed").description("Speed to place crystals")
        .defaultValue(18.0)
        .min(0.1)
        .sliderMax(20.0)
        .visible(placeConfig::get)
        .build());
    public final Setting<Double> placeRangeConfig = sgPlace.add(new DoubleSetting.Builder()
        .name("PlaceRange").description("Range to place crystals")
        .defaultValue(4.0)
        .min(0.1)
        .sliderMax(6.0)
        .visible(placeConfig::get)
        .build());
    public final Setting<Double> placeWallRangeConfig = sgPlace.add(new DoubleSetting.Builder()
        .name("PlaceWallRange").description("Range to place crystals through walls")
        .defaultValue(4.0)
        .min(0.1)
        .sliderMax(6.0)
        .visible(placeConfig::get)
        .build());
    private final Setting<Boolean> placeRangeEyeConfig = sgPlace.add(new BoolSetting.Builder()
        .name("PlaceRangeEye").description("Calculates place ranges starting from the eye position of the player")
        .defaultValue(false)
        .visible(placeConfig::get)
        .build());
    private final Setting<Boolean> placeRangeCenterConfig = sgPlace.add(new BoolSetting.Builder()
        .name("PlaceRangeCenter").description("Calculates place ranges to the center of the block")
        .defaultValue(true)
        .visible(placeConfig::get)
        .build());
    public final Setting<Swap> autoSwapConfig = sgMisc.add(new EnumSetting.Builder<Swap>()
        .name("Swap").description("Swaps to an end crystal before placing if the player is not holding one")
        .defaultValue(Swap.OFF)
        .visible(placeConfig::get)
        .build());
    private final Setting<Boolean> antiSurroundConfig = sgPlace.add(new BoolSetting.Builder()
        .name("AntiSurround")
        .description("Places on mining blocks that when broken, can be placed on to damage enemies. Instantly destroys items spawned from breaking block and allows faster placing")
        .defaultValue(false)
        .visible(placeConfig::get)
        .build());
    public final Setting<ForcePlace> forcePlaceConfig = sgPlace.add(new EnumSetting.Builder<ForcePlace>()
        .name("PreventReplace").description("Attempts to replace crystals in surrounds")
        .defaultValue(ForcePlace.NONE)
        .build());
    private final Setting<Boolean> breakValidConfig = sgMisc.add(new BoolSetting.Builder()
        .name("Strict").description("Only places crystals that can be attacked")
        .defaultValue(false)
        .visible(placeConfig::get)
        .build());
    private final Setting<Boolean> strictDirectionConfig = sgMisc.add(new BoolSetting.Builder()
        .name("StrictDirection").description("Interacts with only visible directions when placing crystals")
        .defaultValue(false)
        .visible(placeConfig::get)
        .build());
    public final Setting<Placements> placementsConfig = sgPlace.add(new EnumSetting.Builder<Placements>()
        .name("Placements").description("Version standard for placing end crystals")
        .defaultValue(Placements.NATIVE)
        .visible(placeConfig::get)
        .build());
    public final Setting<Double> minDamageConfig = sgDmg.add(new DoubleSetting.Builder()
        .name("MinDamage").description("Minimum damage required to consider attacking or placing an end crystal")
        .defaultValue(4.0)
        .min(1.0)
        .sliderMax(10.0)
        .build());
    public final Setting<Double> maxLocalDamageConfig = sgDmg.add(new DoubleSetting.Builder()
        .name("MaxLocalDamage").description("The maximum player damage")
        .defaultValue(12.0)
        .min(4.0)
        .sliderMax(20.0)
        .build());
    private final Setting<Boolean> assumeArmorConfig = sgDmg.add(new BoolSetting.Builder()
        .name("AssumeBestArmor").description("Assumes Prot 0 armor is max armor")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> armorBreakerConfig = sgDmg.add(new BoolSetting.Builder()
        .name("ArmorBreaker").description("Attempts to break enemy armor with crystals")
        .defaultValue(true)
        .build());
    public final Setting<Double> armorScaleConfig = sgDmg.add(new DoubleSetting.Builder()
        .name("ArmorScale").description("Armor damage scale before attempting to break enemy armor with crystals")
        .defaultValue(5.0)
        .min(1.0)
        .sliderMax(20.0)
        .visible(armorBreakerConfig::get)
        .build());
    public final Setting<Double> lethalMultiplier = sgDmg.add(new DoubleSetting.Builder()
        .name("LethalMultiplier").description("If we can kill an enemy with this many crystals, disregard damage values")
        .defaultValue(1.5)
        .min(0.0)
        .sliderMax(4.0)
        .build());
    private final Setting<Boolean> antiTotemConfig = sgDmg.add(new BoolSetting.Builder()
        .name("Lethal-Totem").description("Predicts totems and places crystals to instantly double pop and kill the target")
        .defaultValue(false)
        .visible(placeConfig::get)
        .build());
    private final Setting<Boolean> lethalDamageConfig = sgDmg.add(new BoolSetting.Builder()
        .name("Lethal-DamageTick").description("Places lethal crystals only on ticks where they damage entities")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> safetyConfig = sgDmg.add(new BoolSetting.Builder()
        .name("Safety").description("Accounts for total player safety when attacking and placing crystals")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> safetyOverride = sgDmg.add(new BoolSetting.Builder()
        .name("SafetyOverride").description("Overrides the safety checks if the crystal will kill an enemy")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> blockDestructionConfig = sgDmg.add(new BoolSetting.Builder()
        .name("BlockDestruction").description("Accounts for explosion block destruction when calculating damages")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> selfExtrapolateConfig = sgDmg.add(new BoolSetting.Builder()
        .name("SelfExtrapolate").description("Accounts for motion when calculating self damage")
        .defaultValue(false)
        .build());
    public final Setting<Integer> extrapolateTicksConfig = sgDmg.add(new IntSetting.Builder()
        .name("ExtrapolationTicks").description("Accounts for motion when calculating enemy positions, not fully accurate.")
        .defaultValue(0)
        .min(0)
        .sliderMax(10)
        .build());
    private final Setting<Boolean> renderConfig = sgRender.add(new BoolSetting.Builder()
        .name("Render").description("Renders the current placement")
        .defaultValue(true)
        .build());
    public final Setting<Integer> fadeTimeConfig = sgRender.add(new IntSetting.Builder()
        .name("Fade-Time").description("Timer for the fade")
        .defaultValue(250)
        .min(0)
        .sliderMax(1000)
        .build());
    private final Setting<Boolean> disableDeathConfig = sgMisc.add(new BoolSetting.Builder()
        .name("DisableOnDeath").description("Disables during disconnect/death")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> debugConfig = sgDebug.add(new BoolSetting.Builder()
        .name("Debug").description("Adds extra debug info to arraylist")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> debugDamageConfig = sgDebug.add(new BoolSetting.Builder()
        .name("Debug-Damage").description("Renders damage")
        .defaultValue(false)
        .visible(renderConfig::get)
        .build());

    private static final AABB FULL_CRYSTAL_BB = new AABB(0.0, 0.0, 0.0, 1.0, 2.0, 1.0);
    private static final AABB HALF_CRYSTAL_BB = new AABB(0.0, 0.0, 0.0, 1.0, 1.0, 1.0);
    private final CacheTimer lastAttackTimer = new CacheTimer();
    private final me.lyeddie.addon.util.Timer lastPlaceTimer = new CacheTimer();
    private final me.lyeddie.addon.util.Timer lastSwapTimer = new CacheTimer();
    private final me.lyeddie.addon.util.Timer autoSwapTimer = new CacheTimer();
    private final Deque<Long> attackLatency = new EvictingQueue<>(20);
    private final Map<Integer, Long> attackPackets = Collections.synchronizedMap(new ConcurrentHashMap<>());
    private final Map<BlockPos, Long> placePackets = Collections.synchronizedMap(new ConcurrentHashMap<>());
    private final PerSecondCounter crystalCounter = new PerSecondCounter();
    private final Map<BlockPos, Animation> fadeList = new HashMap<>();
    private final Map<Integer, Integer> antiStuckCrystals = new HashMap<>();
    private final List<AntiStuckData> stuckCrystals = new CopyOnWriteArrayList<>();
    private DamageData<EndCrystal> attackCrystal;
    private DamageData<BlockPos> placeCrystal;
    private BlockPos renderPos;
    private double renderDamage;
    private Vec3 crystalRotation;
    private boolean attackRotate;
    private boolean rotated;
    private float[] silentRotations;
    private float calculatePlaceCrystalTime = 0;
    private long predictId;

    public AutoCrystal() {
        super(Shoreline.MAIN, "AutoCrystal", "Attacks entities with end crystals", 750);
        INST = this;
    }

    @Override
    public String getInfoString() {
        if (debugConfig.get()) {
            return String.format("%sms, %.0f, %dms, %d".formatted(
                new DecimalFormat("0.00")
                    .format(calculatePlaceCrystalTime / 1E6),
                placeCrystal == null ? 0 : lastAttackTimer.getLastResetTime() / 1E6,
                lastAttackTimer.passed(((20.0f - breakSpeedConfig.get()) * 50.0f) + 2000.0f) ? 0 : getBreakMs(),
                crystalCounter.getPerSecond()));
        } else {
            return String.format("%dms, %d",
                lastAttackTimer.passed(((20.0f - breakSpeedConfig.get()) * 50.0f) + 2000.0f) ? 0 : getBreakMs(),
                crystalCounter.getPerSecond());
        }
    }

    @Override
    public void onDeactivate() {
        renderPos = null;
        attackCrystal = null;
        placeCrystal = null;
        crystalRotation = null;
        silentRotations = null;
        calculatePlaceCrystalTime = 0;
        stuckCrystals.clear();
        attackPackets.clear();
        antiStuckCrystals.clear();
        placePackets.clear();
        attackLatency.clear();
        fadeList.clear();
        setStage("NONE");
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        if (disableDeathConfig.get()) {
            toggle();
        } else {
            onDeactivate();
        }
    }

    @EventHandler
    public void onPlayerUpdate(PlayerTickEvent event) {
        if (mc.player.isSpectator() || isSilentSwap(autoSwapConfig.get()) && AutoMine.getInstance().isSilentSwapping()) {
            return;
        }

        for (AntiStuckData d : stuckCrystals) {
            double dist = mc.player.distanceToSqr(d.pos());
            double diff = d.stuckDist() - dist;
            if (diff > 0.5) {
                stuckCrystals.remove(d);
            }
        }

        if (mc.player.isUsingItem() && mc.player.getUsedItemHand() == InteractionHand.MAIN_HAND
            || mc.options.keyAttack.isDown() || PlayerUtil.isHotbarKeysPressed()) {
            autoSwapTimer.reset();
        }
        renderPos = null;
        ArrayList<Entity> entities = Lists.newArrayList(mc.level.entitiesForRendering());
        List<BlockPos> blocks = getSphere(placeRangeEyeConfig.get() ? mc.player.getEyePosition() : mc.player.position());
        long timePre = System.nanoTime();
        if (placeConfig.get()) {
            placeCrystal = calculatePlaceCrystal(blocks, entities);
        }
        attackCrystal = calculateAttackCrystal(entities);
        if (attackCrystal == null) {
            if (placeCrystal != null) {
                EndCrystal crystalEntity = intersectingCrystalCheck(placeCrystal.getDamageData());
                if (crystalEntity != null) {
                    double self = ExplosionUtil.getDamageTo(mc.player, crystalEntity.position(),
                        blockDestructionConfig.get(), selfExtrapolateConfig.get() ? extrapolateTicksConfig.get() : 0, false);
                    if (!safetyConfig.get() || !playerDamageCheck(self)) {
                        attackCrystal = new DamageData<>(crystalEntity, placeCrystal.getAttackTarget(),
                            placeCrystal.getDamage(), self, crystalEntity.blockPosition().below(), false);
                    }
                }
            }
            calculatePlaceCrystalTime = System.nanoTime() - timePre;
        }

        if (inhibitConfig.get() && attackCrystal != null
            && attackPackets.containsKey(attackCrystal.getDamageData().getId())) {
            float delay;
            if (attackDelayConfig.get() > 0.0) {
                float attackFactor = 50.0f / Math.max(1.0f, attackFactorConfig.get());
                delay = (float) (attackDelayConfig.get() * attackFactor);
            } else {
                delay = (float) (1000.0f - breakSpeedConfig.get() * 50.0f);
            }
            lastAttackTimer.setDelay(delay + 100.0f);
            attackPackets.remove(attackCrystal.getDamageData().getId());
        }

        float breakDelay = getBreakDelay();
        if (breakDelayConfig.get()) {
            breakDelay = (float) Math.max(minTimeoutConfig.get() * 50.0f, getBreakMs() + breakTimeoutConfig.get() * 50.0f);
        }
        attackRotate = attackCrystal != null && attackDelayConfig.get() <= 0.0 && lastAttackTimer.passed(breakDelay);
        if (attackCrystal != null) {
            crystalRotation = attackCrystal.damageData.position();
        } else if (placeCrystal != null) {
            crystalRotation = placeCrystal.damageData.getCenter().add(0.0, 0.5, 0.0);
        }
        if (rotateConfig.get() && crystalRotation != null && (placeCrystal == null || canHoldCrystal())) {
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), crystalRotation);
            if (strictRotateConfig.get() == Rotate.FULL || strictRotateConfig.get() == Rotate.SEMI && attackRotate) {
                float yaw;
                float serverYaw = Managers.ROTATION.getWrappedYaw();
                float diff = serverYaw - rotations[0];
                float diff1 = Math.abs(diff);
                if (diff1 > 180.0f) {
                    diff += diff > 0.0f ? -360.0f : 360.0f;
                }
                int dir = diff > 0.0f ? -1 : 1;
                float deltaYaw = dir * rotateLimitConfig.get();
                if (diff1 > rotateLimitConfig.get()) {
                    yaw = serverYaw + deltaYaw;
                    rotated = false;
                } else {
                    yaw = rotations[0];
                    rotated = true;
                    crystalRotation = null;
                }
                rotations[0] = yaw;
            } else {
                rotated = true;
                crystalRotation = null;
            }
            setRotation(rotations[0], rotations[1]);
        } else {
            silentRotations = null;
        }
        if (isRotationBlocked() || !rotated && rotateConfig.get()) {
            return;
        }
        final InteractionHand hand = getCrystalHand();
        if (attackCrystal != null) {
            if (attackRotate) {
                attackCrystal(attackCrystal.getDamageData(), hand);
                setStage("ATTACKING");
                lastAttackTimer.reset();
            }
        }
        boolean placeRotate = lastPlaceTimer.passed(1000.0f - placeSpeedConfig.get() * 50.0f);
        if (placeCrystal != null) {
            renderPos = placeCrystal.getDamageData();
            renderDamage = placeCrystal.getDamage();
            if (placeRotate) {
                placeCrystal(placeCrystal.getDamageData(), hand);
                setStage("PLACING");
                lastPlaceTimer.reset();
            }
        }
    }

    @EventHandler
    public void onRunTick(RunTickEvent event) {
        if (mc.player == null) {
            return;
        }
        final InteractionHand hand = getCrystalHand();
        if (attackDelayConfig.get() > 0.0) {
            float attackFactor = 50.0f / Math.max(1.0f, attackFactorConfig.get());
            if (attackCrystal != null && lastAttackTimer.passed(attackDelayConfig.get() * attackFactor)) {
                attackCrystal(attackCrystal.getDamageData(), hand);
                lastAttackTimer.reset();
            }
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (renderConfig.get()) {
            BlockPos renderPos1 = null;
            double factor = 0.0f;
            for (Map.Entry<BlockPos, Animation> set : fadeList.entrySet()) {
                if (set.getKey() == renderPos) {
                    continue;
                }

                if (set.getValue().getFactor() > factor) {
                    renderPos1 = set.getKey();
                    factor = set.getValue().getFactor();
                }

                set.getValue().setState(false);
                int boxAlpha = (int) (40 * set.getValue().getFactor());
                int lineAlpha = (int) (100 * set.getValue().getFactor());
                event.renderer.box(set.getKey(), TabConfigs.get().getClampColor(boxAlpha), TabConfigs.get().getClampColor(lineAlpha), ShapeMode.Both, 0);
            }

            if (debugDamageConfig.get() && renderPos1 != null) {
                Managers.RENDERS2D.renderSign(String.format("%.1f", renderDamage),
                    renderPos1.getCenter(), new Color(255, 255, 255, (int) (255.0f * factor)).getRGB());
            }

            fadeList.entrySet().removeIf(e ->
                e.getValue().getFactor() == 0.0);

            if (renderPos != null && isHoldingCrystal()) {
                Animation animation = new Animation(true, fadeTimeConfig.get());
                fadeList.put(renderPos, animation);
            }
        }
    }

    @EventHandler(priority = Integer.MAX_VALUE)
    public void onPacketInbound(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null) {
            return;
        }

        if (event.packet instanceof ClientboundBundlePacket packet) {
            for (Packet<?> packet1 : packet.subPackets()) {
                handleServerPackets(packet1);
            }
        } else {
            handleServerPackets(event.packet);
        }
    }

    private void handleServerPackets(Packet<?> serverPacket) {
        if (serverPacket instanceof ClientboundExplodePacket packet) {
            for (Entity entity : Lists.newArrayList(mc.level.entitiesForRendering())) {
                if (entity instanceof EndCrystal && entity.distanceToSqr(packet.center()) < 144.0) {
                    mc.executeIfPossible(() -> mc.level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED));
                    antiStuckCrystals.remove(entity.getId());
                    Long attackTime = attackPackets.remove(entity.getId());
                    if (attackTime != null) {
                        attackLatency.add(System.currentTimeMillis() - attackTime);
                    }
                }
            }
        }

        if (serverPacket instanceof ClientboundSoundPacket packet) {
            if (packet.getSound().value() == SoundEvents.GENERIC_EXPLODE.value() && packet.getSource() == SoundSource.BLOCKS) {
                for (Entity entity : Lists.newArrayList(mc.level.entitiesForRendering())) {
                    if (entity instanceof EndCrystal && entity.distanceToSqr(packet.getX(), packet.getY(), packet.getZ()) < 144.0) {
                        mc.executeIfPossible(() -> mc.level.removeEntity(entity.getId(), Entity.RemovalReason.DISCARDED));
                        antiStuckCrystals.remove(entity.getId());
                        Long attackTime = attackPackets.remove(entity.getId());
                        if (attackTime != null) {
                            attackLatency.add(System.currentTimeMillis() - attackTime);
                        }
                    }
                }
            }
        }

        if (serverPacket instanceof ClientboundRemoveEntitiesPacket packet) {
            for (int id : packet.getEntityIds()) {
                antiStuckCrystals.remove(id);
                Long attackTime = attackPackets.remove(id);
                if (attackTime != null) {
                    attackLatency.add(System.currentTimeMillis() - attackTime);
                }
            }
        }

        if (serverPacket instanceof ClientboundAddEntityPacket packet && packet.getId() > predictId) {
            predictId = packet.getId();
        }
    }

    @EventHandler
    public void onAddEntity(AddEntityEvent event) {
        if (!(event.getEntity() instanceof EndCrystal crystalEntity)) {
            return;
        }
        Vec3 crystalPos = crystalEntity.position();
        BlockPos blockPos = BlockPos.containing(crystalPos.add(0.0, -1.0, 0.0));
        Long time = placePackets.remove(blockPos);
        attackRotate = time != null;
        if (attackRotate) {
            crystalCounter.updateCounter();
        }
        if (!instantConfig.get()) {
            return;
        }
        if (attackRotate) {
            final InteractionHand hand = getCrystalHand();
            attackInternal(crystalEntity, hand);
            setStage("ATTACKING");
            lastAttackTimer.reset();
            if (sequentialConfig.get() == Sequential.NORMAL) {
                placeSequentialCrystal(hand);
            }
        } else if (instantCalcConfig.get()) {
            if (attackRangeCheck(crystalPos)) {
                return;
            }
            double selfDamage = ExplosionUtil.getDamageTo(mc.player, crystalPos,
                blockDestructionConfig.get(), selfExtrapolateConfig.get() ? extrapolateTicksConfig.get() : 0, false);
            if (playerDamageCheck(selfDamage)) {
                return;
            }
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (entity == null || !entity.isAlive() || entity == mc.player
                    || !isValidTarget(entity)
                    || (entity instanceof Player ent && Friends.get().isFriend(ent))) {
                    continue;
                }
                double crystalDist = crystalPos.distanceToSqr(entity.position());
                if (crystalDist > 144.0f) {
                    continue;
                }
                double dist = mc.player.distanceToSqr(entity);
                if (dist > targetRangeConfig.get() * targetRangeConfig.get()) {
                    continue;
                }

                double damage = ExplosionUtil.getDamageTo(entity, crystalPos, blockDestructionConfig.get(),
                    extrapolateTicksConfig.get(), assumeArmorConfig.get());
                DamageData<EndCrystal> data = new DamageData<>(crystalEntity,
                    entity, damage, selfDamage, crystalEntity.blockPosition().below(), false);
                attackRotate = damage > instantDamageConfig.get() || attackCrystal != null
                    && damage >= attackCrystal.getDamage() && instantMaxConfig.get()
                    || entity instanceof LivingEntity entity1 && isCrystalLethalTo(data, entity1);
                if (attackRotate) {
                    final InteractionHand hand = getCrystalHand();
                    attackInternal(crystalEntity, hand);
                    setStage("ATTACKING");
                    lastAttackTimer.reset();
                    if (sequentialConfig.get() == Sequential.NORMAL) {
                        placeSequentialCrystal(hand);
                    }
                    break;
                }
            }
        }
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (mc.player == null) {
            return;
        }
        if (event.packet instanceof ServerboundSetCarriedItemPacket) {
            lastSwapTimer.reset();
        }
    }

    public boolean isAttacking() {
        return attackCrystal != null;
    }

    public boolean isPlacing() {
        return placeCrystal != null && isHoldingCrystal();
    }

    public void attackCrystal(EndCrystal entity, InteractionHand hand) {
        if (attackCheckPre(hand)) {
            return;
        }
        MobEffectInstance weakness = mc.player.getEffect(MobEffects.WEAKNESS);
        MobEffectInstance strength = mc.player.getEffect(MobEffects.STRENGTH);
        if (weakness != null && (strength == null || weakness.getAmplifier() > strength.getAmplifier())) {
            int slot = -1;
            for (int i = 0; i < 9; ++i) {
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (!stack.isEmpty() && (stack.has(DataComponents.WEAPON)
                    || stack.getItem() instanceof AxeItem
                    || stack.is(ItemTags.PICKAXES))) {
                    slot = i;
                    break;
                }
            }
            if (slot != -1) {
                boolean canSwap = slot != Managers.INVENTORY.getServerSlot() && (antiWeaknessConfig.get() != Swap.NORMAL || autoSwapTimer.passed(500));
                if (antiWeaknessConfig.get() != Swap.OFF && canSwap) {
                    if (antiWeaknessConfig.get() == Swap.SILENT_ALT) {
                        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,
                            slot + 36, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                    } else if (antiWeaknessConfig.get() == Swap.SILENT) {
                        Managers.INVENTORY.setSlot(slot);
                    } else {
                        Managers.INVENTORY.setClientSlot(slot);
                    }
                }
                attackInternal(entity, InteractionHand.MAIN_HAND);
                if (canSwap) {
                    if (antiWeaknessConfig.get() == Swap.SILENT_ALT) {
                        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,
                            slot + 36, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                    } else if (antiWeaknessConfig.get() == Swap.SILENT) {
                        Managers.INVENTORY.syncToClient();
                    }
                }

                if (sequentialConfig.get() == Sequential.STRICT) {
                    placeSequentialCrystal(hand);
                }
            }
        } else {
            attackInternal(entity, hand);
            if (sequentialConfig.get() == Sequential.STRICT) {
                placeSequentialCrystal(hand);
            }
        }
    }

    private void attackInternal(EndCrystal crystalEntity, InteractionHand hand) {
        attackInternal(crystalEntity.getId(), hand);
    }

    private void attackInternal(int crystalEntity, InteractionHand hand) {
        hand = hand != null ? hand : InteractionHand.MAIN_HAND;
        EndCrystal entity2 = new EndCrystal(mc.level, 0.0, 0.0, 0.0);
        entity2.setId(crystalEntity);
        ServerboundAttackPacket packet = new ServerboundAttackPacket(entity2.getId());
        Managers.NETWORK.sendPacket(packet);
        if (swingConfig.get()) {
            mc.player.swing(hand);
        } else {
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(hand));
        }

        attackPackets.put(crystalEntity, System.currentTimeMillis());
        Integer antiStuckCount = antiStuckCrystals.get(crystalEntity);
        if (antiStuckCount != null) {
            antiStuckCrystals.replace(crystalEntity, antiStuckCount + 1);
        } else {
            antiStuckCrystals.put(crystalEntity, 1);
        }
    }

    private void placeSequentialCrystal(InteractionHand hand) {
        if (placeCrystal == null) {
            return;
        }
        int latency = FastLatency.getInstance().isActive() ? (int)
            FastLatency.getInstance().getLatency() : Managers.NETWORK.getClientLatency();
        if (!Managers.NETWORK.is2b2t() || latency >= 50) {
            placeCrystal(placeCrystal.getBlockPos(), hand);
        }
    }

    private void placeCrystal(BlockPos blockPos, InteractionHand hand) {
        if (isRotationBlocked() || !rotated && rotateConfig.get()) {
            return;
        }

        placeCrystal(blockPos, hand, true);
    }

    public void placeCrystal(BlockPos blockPos, InteractionHand hand, boolean checkPlacement) {
        if (checkPlacement && checkCanUseCrystal()) {
            return;
        }
        Direction sidePlace = getPlaceDirection(blockPos);
        BlockHitResult result = new BlockHitResult(blockPos.getCenter(), sidePlace, blockPos, false);
        if (autoSwapConfig.get() != Swap.OFF && hand != InteractionHand.OFF_HAND && getCrystalHand() == null) {
            if (isSilentSwap(autoSwapConfig.get()) && InventoryUtil.count(Items.END_CRYSTAL) == 0) {
                return;
            }
            int crystalSlot = getCrystalSlot();
            if (crystalSlot != -1) {
                boolean canSwap = crystalSlot != Managers.INVENTORY.getServerSlot() && (autoSwapConfig.get() != Swap.NORMAL || autoSwapTimer.passed(500));
                if (canSwap) {
                    if (autoSwapConfig.get() == Swap.SILENT_ALT) {
                        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,
                            crystalSlot + 36, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                    } else if (autoSwapConfig.get() == Swap.SILENT) {
                        Managers.INVENTORY.setSlot(crystalSlot);
                    } else {
                        Managers.INVENTORY.setClientSlot(crystalSlot);
                    }
                }
                placeInternal(result, InteractionHand.MAIN_HAND);
                placePackets.put(blockPos, System.currentTimeMillis());
                if (canSwap) {
                    if (autoSwapConfig.get() == Swap.SILENT_ALT) {
                        mc.gameMode.handleContainerInput(mc.player.inventoryMenu.containerId,
                            crystalSlot + 36, mc.player.getInventory().getSelectedSlot(), ContainerInput.SWAP, mc.player);
                    } else if (autoSwapConfig.get() == Swap.SILENT) {
                        Managers.INVENTORY.syncToClient();
                    }
                }
            }
        } else if (isHoldingCrystal()) {
            placeInternal(result, hand);
            placePackets.put(blockPos, System.currentTimeMillis());
        }
    }

    private void placeInternal(BlockHitResult result, InteractionHand hand) {
        if (hand == null) {
            return;
        }
        Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemOnPacket(hand, result, id));
        if (swingConfig.get()) {
            mc.player.swing(hand);
        } else {
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(hand));
        }

        if (idPredictConfig.get()) {
            boolean flag = AutoXP.getInstance().isActive() || mc.player.isUsingItem() && mc.player.getItemInHand(mc.player.getUsedItemHand()).getItem() instanceof ExperienceBottleItem;
            int id = (int) (predictId + 1);
            if (flag || attackPackets.containsKey(id)) {
                return;
            }
            Entity entity = mc.level.getEntity(id);
            if (entity != null && !(entity instanceof EndCrystal)) {
                return;
            }
            EndCrystal entity2 = new EndCrystal(mc.level, 0.0, 0.0, 0.0);
            entity2.setId(id);
            ServerboundAttackPacket packet = new ServerboundAttackPacket(entity2.getId());
            Managers.NETWORK.sendPacket(packet);
            Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            attackPackets.put(id, System.currentTimeMillis());
        }
    }

    private boolean isSilentSwap(Swap swap) {
        return swap == Swap.SILENT || swap == Swap.SILENT_ALT;
    }

    private int getCrystalSlot() {
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getItem(i);
            if (stack.getItem() instanceof EndCrystalItem) {
                slot = i;
                break;
            }
        }
        return slot;
    }

    private Direction getPlaceDirection(BlockPos blockPos) {
        int x = blockPos.getX();
        int y = blockPos.getY();
        int z = blockPos.getZ();
        if (strictDirectionConfig.get()) {
            if (mc.player.getY() >= blockPos.getY()) {
                return Direction.UP;
            }
            BlockHitResult result = mc.level.clip(new ClipContext(
                mc.player.getEyePosition(), new Vec3(x + 0.5, y + 0.5, z + 0.5),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, mc.player));
            if (result != null && result.getType() == HitResult.Type.BLOCK) {
                return result.getDirection();
            }
        } else {
            if (mc.level.isInWorldBounds(blockPos)) {
                return Direction.DOWN;
            }
            BlockHitResult result = mc.level.clip(new ClipContext(
                mc.player.getEyePosition(), new Vec3(x + 0.5, y + 0.5, z + 0.5),
                ClipContext.Block.OUTLINE,
                ClipContext.Fluid.NONE, mc.player));
            if (result != null && result.getType() == HitResult.Type.BLOCK) {
                return result.getDirection();
            }
        }
        return Direction.UP;
    }

    private DamageData<EndCrystal> calculateAttackCrystal(List<Entity> entities) {
        if (entities.isEmpty()) {
            return null;
        }

        final List<DamageData<EndCrystal>> validData = new ArrayList<>();

        DamageData<EndCrystal> data = null;
        for (Entity crystal : entities) {
            if (!(crystal instanceof EndCrystal crystal1) || !crystal.isAlive()
                || stuckCrystals.stream().anyMatch(d -> d.id() == crystal.getId())) {
                continue;
            }
            Long time = attackPackets.get(crystal.getId());
            boolean attacked = time != null && time < getBreakMs();
            if ((crystal.tickCount < ticksExistedConfig.get() || attacked) && inhibitConfig.get()) {
                continue;
            }
            if (attackRangeCheck(crystal1)) {
                continue;
            }
            double selfDamage = ExplosionUtil.getDamageTo(mc.player, crystal.position(),
                blockDestructionConfig.get(), selfExtrapolateConfig.get() ? extrapolateTicksConfig.get() : 0, false);
            boolean unsafeToPlayer = playerDamageCheck(selfDamage);
            if (unsafeToPlayer && !safetyOverride.get()) {
                continue;
            }
            for (Entity entity : entities) {
                if (entity == null || !entity.isAlive() || entity == mc.player
                    || !isValidTarget(entity)
                    || (entity instanceof Player ent && Friends.get().isFriend(ent))) {
                    continue;
                }
                double crystalDist = crystal.distanceToSqr(entity);
                if (crystalDist > 144.0f) {
                    continue;
                }
                double dist = mc.player.distanceToSqr(entity);
                if (dist > targetRangeConfig.get() * targetRangeConfig.get()) {
                    continue;
                }

                boolean antiSurround = false;
                if (antiSurroundConfig.get() && entity instanceof Player player
                    && !BlastResistantBlocks.isUnbreakable(player.blockPosition())) {
                    Set<BlockPos> miningPositions = new HashSet<>();
                    BlockPos miningBlock = AutoMine.getInstance().getMiningBlock();
                    if (AutoMine.getInstance().isActive() && miningBlock != null) {
                        miningPositions.add(miningBlock);
                    }
                    if (Managers.BLOCK.getMines(0.75f).contains(player.blockPosition().above())) {
                        miningPositions.add(player.blockPosition().above());
                    }
                    for (BlockPos miningBlockPos : miningPositions) {
                        if (!SurroundII.getInstance().getSurroundNoDown(player).contains(miningBlockPos)) {
                            continue;
                        }
                        for (Direction direction : Direction.values()) {
                            BlockPos pos1 = miningBlockPos.relative(direction);
                            if (crystal.blockPosition().equals(pos1.below())) {
                                antiSurround = true;
                            }
                        }
                    }
                }

                double damage = ExplosionUtil.getDamageTo(entity, crystal.position(), blockDestructionConfig.get(),
                    extrapolateTicksConfig.get(), assumeArmorConfig.get());
                if (checkOverrideSafety(unsafeToPlayer, damage, entity)) {
                    continue;
                }

                DamageData<EndCrystal> currentData = new DamageData<>(crystal1, entity,
                    damage, selfDamage, crystal1.blockPosition().below(), antiSurround);
                validData.add(currentData);
                if (data == null || damage > data.getDamage()) {
                    data = currentData;
                }
            }
        }
        if (data == null || targetDamageCheck(data)) {
            if (antiSurroundConfig.get()) {
                return validData.stream()
                    .filter(DamageData::isAntiSurround)
                    .min(Comparator.comparingDouble(d -> mc.player.distanceToSqr(d.getBlockPos().getCenter())))
                    .orElse(null);
            }
            return null;
        }
        return data;
    }

    private boolean attackRangeCheck(EndCrystal entity) {
        return attackRangeCheck(entity.position());
    }

    private boolean attackRangeCheck(Vec3 entityPos) {
        double breakRange = breakRangeConfig.get();
        double breakWallRange = breakWallRangeConfig.get();
        Vec3 playerPos = mc.player.getEyePosition();
        double dist = playerPos.distanceToSqr(entityPos);
        if (dist > breakRange * breakRange) {
            return true;
        }
        double yOff = Math.abs(entityPos.y() - mc.player.getY());
        if (yOff > maxYOffsetConfig.get()) {
            return true;
        }
        BlockHitResult result = mc.level.clip(new ClipContext(
            playerPos, entityPos, ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, mc.player));
        return result.getType() != HitResult.Type.MISS && dist > breakWallRange * breakWallRange;
    }

    private DamageData<BlockPos> calculatePlaceCrystal(List<BlockPos> placeBlocks, List<Entity> entities) {
        if (placeBlocks.isEmpty() || entities.isEmpty()) {
            return null;
        }

        final List<DamageData<BlockPos>> validData = new ArrayList<>();

        DamageData<BlockPos> data = null;
        for (BlockPos pos : placeBlocks) {
            if (!canUseCrystalOnBlock(pos) || placeRangeCheck(pos) || intersectingAntiStuckCheck(pos)) {
                continue;
            }
            double selfDamage = ExplosionUtil.getDamageTo(mc.player, crystalDamageVec(pos),
                blockDestructionConfig.get(), selfExtrapolateConfig.get() ? extrapolateTicksConfig.get() : 0, false);
            boolean unsafeToPlayer = playerDamageCheck(selfDamage);
            if (unsafeToPlayer && !safetyOverride.get()) {
                continue;
            }
            for (Entity entity : entities) {
                if (entity == null || !entity.isAlive() || entity == mc.player
                    || !isValidTarget(entity)
                    || (entity instanceof Player ent && Friends.get().isFriend(ent))) {
                    continue;
                }
                double blockDist = pos.distToCenterSqr(entity.position());
                if (blockDist > 144.0f) {
                    continue;
                }
                double dist = mc.player.distanceToSqr(entity);
                if (dist > targetRangeConfig.get() * targetRangeConfig.get()) {
                    continue;
                }

                boolean antiSurround = false;
                if (antiSurroundConfig.get() && entity instanceof Player player
                    && !BlastResistantBlocks.isUnbreakable(player.blockPosition())) {
                    Set<BlockPos> miningPositions = new HashSet<>();
                    BlockPos miningBlock = AutoMine.getInstance().getMiningBlock();
                    if (AutoMine.getInstance().isActive() && miningBlock != null) {
                        miningPositions.add(miningBlock);
                    }
                    if (Managers.BLOCK.getMines(0.75f).contains(player.blockPosition().above())) {
                        miningPositions.add(player.blockPosition().above());
                    }
                    for (BlockPos miningBlockPos : miningPositions) {
                        if (!SurroundII.getInstance().getSurroundNoDown(player).contains(miningBlockPos)) {
                            continue;
                        }
                        for (Direction direction : Direction.values()) {
                            BlockPos pos1 = miningBlockPos.relative(direction);
                            if (pos.equals(pos1.below())) {
                                antiSurround = true;
                            }
                        }
                    }
                }

                double damage;
                damage = ExplosionUtil.getDamageTo(entity, crystalDamageVec(pos), blockDestructionConfig.get(),
                    extrapolateTicksConfig.get(), assumeArmorConfig.get());
                if (checkOverrideSafety(unsafeToPlayer, damage, entity)) {
                    continue;
                }

                DamageData<BlockPos> currentData = new DamageData<>(pos, entity,
                    damage, selfDamage, antiSurround);
                validData.add(currentData);
                if (data == null || damage > data.getDamage()) {
                    data = currentData;
                }
            }
        }
        if (data == null || targetDamageCheck(data)) {
            if (antiSurroundConfig.get()) {
                return validData.stream()
                    .filter(DamageData::isAntiSurround)
                    .min(Comparator.comparingDouble(d -> mc.player.distanceToSqr(d.getBlockPos().getCenter())))
                    .orElse(null);
            }
            return null;
        }
        return data;
    }

    private boolean placeRangeCheck(BlockPos pos) {
        double placeRange = placeRangeConfig.get();
        double placeWallRange = placeWallRangeConfig.get();
        Vec3 player = placeRangeEyeConfig.get() ? mc.player.getEyePosition() : mc.player.position();
        double dist = placeRangeCenterConfig.get() ?
            player.distanceToSqr(pos.getCenter()) : pos.distToLowCornerSqr(player.x, player.y, player.z);
        if (dist > placeRange * placeRange) {
            return true;
        }
        Vec3 raytrace = Vec3.atLowerCornerOf(pos).add(0.5, 2.70000004768372, 0.5);
        BlockHitResult result = mc.level.clip(new ClipContext(
            mc.player.getEyePosition(), raytrace,
            ClipContext.Block.COLLIDER,
            ClipContext.Fluid.NONE, mc.player));
        float maxDist = (float) (breakRangeConfig.get() * breakRangeConfig.get());
        if (result != null && result.getType() == HitResult.Type.BLOCK && !result.getBlockPos().equals(pos)) {
            maxDist = (float) (breakWallRangeConfig.get() * breakWallRangeConfig.get());
            if (!raytraceConfig.get() || dist > placeWallRange * placeWallRange) {
                return true;
            }
        }
        return breakValidConfig.get() && dist > maxDist;
    }

    public void placeCrystalForTarget(Player target, BlockPos blockPos) {
        if (target == null || target.isDeadOrDying() || placeRangeCheck(blockPos) || !canUseCrystalOnBlock(blockPos)) {
            return;
        }
        double selfDamage = ExplosionUtil.getDamageTo(mc.player, crystalDamageVec(blockPos),
            blockDestructionConfig.get(), Set.of(blockPos), selfExtrapolateConfig.get() ? extrapolateTicksConfig.get() : 0, false);
        if (playerDamageCheck(selfDamage)) {
            return;
        }
        double damage = ExplosionUtil.getDamageTo(target, crystalDamageVec(blockPos), blockDestructionConfig.get(),
            Set.of(blockPos), extrapolateTicksConfig.get(), assumeArmorConfig.get());
        if (damage < minDamageConfig.get() && !isCrystalLethalTo(damage, target)
            || placeCrystal != null && placeCrystal.getDamage() >= damage) {
            return;
        }

        float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), blockPos.getCenter());
        setRotation(rotations[0], rotations[1]);
        placeCrystal(blockPos, InteractionHand.MAIN_HAND, false);
        fadeList.put(blockPos, new Animation(true, fadeTimeConfig.get()));
    }

    private boolean checkOverrideSafety(boolean unsafeToPlayer, double damage, Entity entity) {
        return safetyOverride.get() && unsafeToPlayer && damage < EntityUtil.getHealth(entity) + 0.5;
    }

    private boolean targetDamageCheck(DamageData<?> crystal) {
        double minDmg = minDamageConfig.get();
        if (crystal.getAttackTarget() instanceof LivingEntity entity && isCrystalLethalTo(crystal, entity)) {
            minDmg = 2.0f;
        }
        return crystal.getDamage() < minDmg;
    }

    private boolean playerDamageCheck(double playerDamage) {
        if (!mc.player.isCreative()) {
            float health = mc.player.getHealth() + mc.player.getAbsorptionAmount();
            if (safetyConfig.get() && playerDamage >= health + 0.5f) {
                return true;
            }
            return playerDamage > maxLocalDamageConfig.get();
        }
        return false;
    }

    private boolean checkAntiTotem(double damage, LivingEntity entity) {
        if (entity instanceof Player p) {
            float phealth = EntityUtil.getHealth(p);
            if (phealth <= 2.0f && phealth - damage < 0.5f) {
                long time = Managers.TOTEM.getLastPopTime(p);
                if (time != -1) {
                    return System.currentTimeMillis() - time <= 500;
                }
            }
        }
        return false;
    }

    private boolean isCrystalLethalTo(DamageData<?> crystal, LivingEntity entity) {
        return isCrystalLethalTo(crystal.getDamage(), entity);
    }

    private boolean isCrystalLethalTo(double damage, LivingEntity entity) {
        if (lethalDamageConfig.get() && lastAttackTimer.passed(500)) {
            return true;
        }

        if (antiTotemConfig.get() && checkAntiTotem(damage, entity)) {
            return true;
        }
        float health = entity.getHealth() + entity.getAbsorptionAmount();
        if (damage * (1.0f + lethalMultiplier.get()) >= health + 0.5f) {
            return true;
        }
        if (armorBreakerConfig.get()) {
            for (ItemStack armorStack : EntityUtil.getArmorItems(entity)) {
                int n = armorStack.getDamageValue();
                int n1 = armorStack.getMaxDamage();
                float durability = ((n1 - n) / (float) n1) * 100.0f;
                if (durability < armorScaleConfig.get()) {
                    return true;
                }
            }
        }

        if (shulkersConfig.get() && entity instanceof Player) {
            for (BlockPos pos : getSphere(3.0f, entity.position())) {
                BlockState state = mc.level.getBlockState(pos);
                if (state.getBlock() instanceof ShulkerBoxBlock) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean attackCheckPre(InteractionHand hand) {
        if (!lastSwapTimer.passed(swapDelayConfig.get() * 25.0f)) {
            return true;
        }
        if (hand == InteractionHand.MAIN_HAND) {
            return checkCanUseCrystal();
        }
        return false;
    }

    private boolean checkCanUseCrystal() {
        return !multitaskConfig.get() && checkMultitask()
            || !whileMiningConfig.get() && mc.gameMode.isDestroying();
    }

    private boolean isHoldingCrystal() {
        if (!checkCanUseCrystal() && (autoSwapConfig.get() == Swap.SILENT || autoSwapConfig.get() == Swap.SILENT_ALT)) {
            return true;
        }
        return getCrystalHand() != null;
    }

    private Vec3 crystalDamageVec(BlockPos pos) {
        return Vec3.atLowerCornerOf(pos).add(0.5, 1.0, 0.5);
    }

    private boolean isValidTarget(Entity e) {
        return e instanceof Player && playersConfig.get()
            || EntityUtil.isMonster(e) && monstersConfig.get()
            || EntityUtil.isNeutral(e) && neutralsConfig.get()
            || EntityUtil.isPassive(e) && animalsConfig.get();
    }

    public boolean canUseCrystalOnBlock(BlockPos pos) {
        BlockState state = mc.level.getBlockState(pos);
        if (!state.is(Blocks.OBSIDIAN) && !state.is(Blocks.BEDROCK)) {
            return false;
        }
        return isCrystalHitboxClear(pos);
    }

    public boolean isCrystalHitboxClear(BlockPos pos) {
        BlockPos p2 = pos.above();
        BlockState state2 = mc.level.getBlockState(p2);
        if (placementsConfig.get() == Placements.PROTOCOL && !mc.level.isEmptyBlock(p2.above())) {
            return false;
        }
        if (!mc.level.isEmptyBlock(p2) && !state2.is(Blocks.FIRE)) {
            return false;
        } else {
            final AABB bb = Managers.NETWORK.isCrystalPvpCC() ? HALF_CRYSTAL_BB : FULL_CRYSTAL_BB;
            double d = p2.getX();
            double e = p2.getY();
            double f = p2.getZ();
            List<Entity> list = getEntitiesBlockingCrystal(new AABB(d, e, f,
                d + bb.maxX, e + bb.maxY, f + bb.maxZ));
            return list.isEmpty();
        }
    }

    private List<Entity> getEntitiesBlockingCrystal(AABB box) {
        List<Entity> entities = new CopyOnWriteArrayList<>(
            mc.level.getEntities(null, box));
        for (Entity entity : entities) {
            if (entity == null || !entity.isAlive()
                || entity instanceof ExperienceOrb
                || forcePlaceConfig.get() != ForcePlace.NONE
                && entity instanceof ItemEntity && entity.tickCount <= 10) {
                entities.remove(entity);
            } else if (entity instanceof EndCrystal entity1
                && entity1.getBoundingBox().intersects(box)) {
                Integer antiStuckAttacks = antiStuckCrystals.get(entity1.getId());
                if (!attackRangeCheck(entity1) && (antiStuckAttacks == null || antiStuckAttacks <= attackLimitConfig.get() * 10.0f)) {
                    entities.remove(entity);
                } else {
                    double dist = mc.player.distanceToSqr(entity1);
                    stuckCrystals.add(new AntiStuckData(entity1.getId(), entity1.blockPosition(), entity1.position(), dist));
                }
            }
        }
        return entities;
    }

    private boolean intersectingAntiStuckCheck(BlockPos blockPos) {
        if (stuckCrystals.isEmpty()) {
            return false;
        }
        return stuckCrystals.stream().anyMatch(d -> d.blockPos().equals(blockPos.above()));
    }

    private EndCrystal intersectingCrystalCheck(BlockPos pos) {
        return (EndCrystal) mc.level.getEntities(null, new AABB(pos)).stream()
            .filter(e -> e instanceof EndCrystal).min(Comparator.comparingDouble(e -> mc.player.distanceTo(e))).orElse(null);
    }

    private List<BlockPos> getSphere(Vec3 origin) {
        double rad = Math.ceil(placeRangeConfig.get());
        return getSphere(rad, origin);
    }

    private List<BlockPos> getSphere(double rad, Vec3 origin) {
        List<BlockPos> sphere = new ArrayList<>();
        for (double x = -rad; x <= rad; ++x) {
            for (double y = -rad; y <= rad; ++y) {
                for (double z = -rad; z <= rad; ++z) {
                    Vec3i pos = new Vec3i((int) (origin.x() + x),
                        (int) (origin.y() + y), (int) (origin.z() + z));
                    final BlockPos p = new BlockPos(pos);
                    sphere.add(p);
                }
            }
        }
        return sphere;
    }

    private boolean canHoldCrystal() {
        return isHoldingCrystal() || autoSwapConfig.get() != Swap.OFF && getCrystalSlot() != -1;
    }

    private InteractionHand getCrystalHand() {
        final ItemStack offhand = mc.player.getOffhandItem();
        final ItemStack mainhand = mc.player.getMainHandItem();
        if (offhand.getItem() instanceof EndCrystalItem) {
            return InteractionHand.OFF_HAND;
        } else if (mainhand.getItem() instanceof EndCrystalItem) {
            return InteractionHand.MAIN_HAND;
        }
        return null;
    }

    public float getBreakDelay() {
        return (float) (1000.0f - breakSpeedConfig.get() * 50.0f);
    }

    public void setStage(String crystalStage) {
    }

    public int getBreakMs() {
        if (attackLatency.isEmpty()) {
            return 0;
        }
        float avg = 0.0f;
        ArrayList<Long> latencyCopy = Lists.newArrayList(attackLatency);
        if (!latencyCopy.isEmpty()) {
            for (float t : latencyCopy) {
                avg += t;
            }
            avg /= latencyCopy.size();
        }
        return (int) avg;
    }

    public boolean shouldPreForcePlace() {
        return forcePlaceConfig.get() == ForcePlace.PRE;
    }

    public float getPlaceRange() {
        return toFloat(placeRangeConfig.get());
    }

    public static AutoCrystal getInstance() {
        return INST;
    }

    public enum Swap {
        NORMAL,
        SILENT,
        SILENT_ALT,
        OFF
    }

    public enum Sequential {
        NORMAL,
        STRICT,
        NONE
    }

    public enum ForcePlace {
        PRE,
        POST,
        NONE
    }

    public enum Placements {
        NATIVE,
        PROTOCOL
    }

    public enum Rotate {
        FULL,
        SEMI,
        OFF
    }

    private record AntiStuckData(int id, BlockPos blockPos, Vec3 pos, double stuckDist) {
    }

    private static class DamageData<T> {
        private T damageData;
        private Entity attackTarget;
        private BlockPos blockPos;
        private double damage, selfDamage;
        private boolean antiSurround;

        public DamageData() {
        }

        @SuppressWarnings("unchecked")
        public DamageData(BlockPos damageData, Entity attackTarget, double damage, double selfDamage, boolean antiSurround) {
            this.damageData = (T) damageData;
            this.attackTarget = attackTarget;
            this.damage = damage;
            this.selfDamage = selfDamage;
            this.blockPos = damageData;
            this.antiSurround = antiSurround;
        }

        public DamageData(T damageData, Entity attackTarget, double damage, double selfDamage, BlockPos blockPos, boolean antiSurround) {
            this.damageData = damageData;
            this.attackTarget = attackTarget;
            this.damage = damage;
            this.selfDamage = selfDamage;
            this.blockPos = blockPos;
            this.antiSurround = antiSurround;
        }

        public T getDamageData() {
            return damageData;
        }

        public Entity getAttackTarget() {
            return attackTarget;
        }

        public double getDamage() {
            return damage;
        }


        public BlockPos getBlockPos() {
            return blockPos;
        }

        public boolean isAntiSurround() {
            return antiSurround;
        }
    }
}
