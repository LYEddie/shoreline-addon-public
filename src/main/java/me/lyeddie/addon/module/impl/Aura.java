package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.module.CombatModule;
import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.util.Interpolation;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.events.irrevocable.RemoveEntityEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.managers.impl.util.TickSync;
import me.lyeddie.addon.util.tabs.TabConfigs;
import me.lyeddie.addon.util.*;
import me.lyeddie.addon.util.literal.EnchantmentUtil;
import me.lyeddie.addon.util.literal.EntityUtil;
import me.lyeddie.addon.util.literal.PlayerUtil;
import me.lyeddie.addon.util.literal.RotationUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.AttributeModifiersComponent;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.decoration.EndCrystalEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.entity.projectile.ArrowEntity;
import net.minecraft.entity.projectile.thrown.ExperienceBottleEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.RaycastContext;
import org.apache.commons.lang3.mutable.MutableDouble;

import java.util.Comparator;
import java.util.stream.Stream;

import static me.lyeddie.addon.util.Globals.RANDOM;

public class Aura extends CombatModule {
    private static Aura INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRanges = settings.createGroup("Range");
    private final SettingGroup sgSpeed = settings.createGroup("Attack Speeds");
    private final SettingGroup sgSwitch = settings.createGroup("Weapons");
    private final SettingGroup sgRots = settings.createGroup("Rotations");
    private final SettingGroup sgMisc = settings.createGroup("Miscellaneous");
    private final SettingGroup sgTargets = settings.createGroup("Targets");

    public final Setting<Boolean> multitaskConfig = addMultitaskConfig(sgGeneral);

    private final Setting<Boolean> swingConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Swing").description("Swings the hand after attacking")
        .defaultValue(true)
        .build());
    public final Setting<TargetMode> modeConfig = sgGeneral.add(new EnumSetting.Builder<TargetMode>()
        .name("Mode").description("The mode for targeting entities to attack")
        .defaultValue(TargetMode.SWITCH)
        .build());
    public final Setting<Priority> priorityConfig = sgGeneral.add(new EnumSetting.Builder<Priority>()
        .name("Priority").description("The value to prioritize when searching for targets")
        .defaultValue(Priority.HEALTH)
        .build());
    public final Setting<Double> searchRangeConfig = sgRanges.add(new DoubleSetting.Builder()
        .name("EnemyRange").description("Range to search for targets")
        .defaultValue(5.0)
        .min(1.0)
        .sliderMax(10.0)
        .build());
    public final Setting<Double> rangeConfig = sgRanges.add(new DoubleSetting.Builder()
        .name("Range").description("Range to attack entities")
        .defaultValue(4.5)
        .min(1.0)
        .sliderMax(6.0)
        .build());
    public final Setting<Double> wallRangeConfig = sgRanges.add(new DoubleSetting.Builder()
        .name("WallRange").description("Range to attack entities through walls")
        .defaultValue(4.5)
        .min(1.0)
        .sliderMax(6.0)
        .build());
    private final Setting<Boolean> vanillaRangeConfig = sgRanges.add(new BoolSetting.Builder()
        .name("VanillaRange").description("Only attack within vanilla range")
        .defaultValue(false)
        .build());
    public final Setting<Double> fovConfig = sgRanges.add(new DoubleSetting.Builder()
        .name("FOV").description("Field of view to attack entities")
        .defaultValue(180.0)
        .min(1.0)
        .sliderMax(180.0)
        .build());
    private final Setting<Boolean> attackDelayConfig = sgSpeed.add(new BoolSetting.Builder()
        .name("AttackDelay").description("Delays attacks according to minecraft hit delays for maximum damage per attack")
        .defaultValue(true)
        .build());
    public final Setting<Double> attackSpeedConfig = sgSpeed.add(new DoubleSetting.Builder()
        .name("AttackSpeed").description("Delay for attacks (Only functions if AttackDelay is off)")
        .defaultValue(20.0)
        .min(1.0)
        .sliderMax(20.0)
        .build());
    public final Setting<Double> randomSpeedConfig = sgSpeed.add(new DoubleSetting.Builder()
        .name("RandomSpeed").description("Randomized delay for attacks (Only functions if AttackDelay is off)")
        .defaultValue(0.0)
        .min(0.0)
        .sliderMax(10.0)
        .visible(() -> !attackDelayConfig.get())
        .build());
    public final Setting<Double> swapDelayConfig = sgSwitch.add(new DoubleSetting.Builder()
        .name("SwapPenalty").description("Delay for attacking after swapping items which prevents NCP flags")
        .defaultValue(0.0)
        .min(0.0)
        .sliderMax(10.0)
        .build());
    public final Setting<TickSync> tpsSyncConfig = sgSpeed.add(new EnumSetting.Builder<TickSync>()
        .name("TPS-Sync").description("Syncs the attacks with the server TPS")
        .defaultValue(TickSync.NONE)
        .build());
    public final Setting<Swap> autoSwapConfig = sgSwitch.add(new EnumSetting.Builder<Swap>()
        .name("AutoSwap").description("Automatically swaps to a weapon before attacking")
        .defaultValue(Swap.OFF)
        .build());
    private final Setting<Boolean> swordCheckConfig = sgSwitch.add(new BoolSetting.Builder()
        .name("Sword-Check").description("Checks if a weapon is in the hand before attacking")
        .defaultValue(true)
        .build());
    public final Setting<Vector> hitVectorConfig = sgMisc.add(new EnumSetting.Builder<Vector>()
        .name("HitVector").description("The vector to aim for when attacking entities")
        .defaultValue(Vector.FEET)
        .build());
    private final Setting<Boolean> rotateConfig = sgRots.add(new BoolSetting.Builder()
        .name("Rotate").description("Rotate before attacking")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> silentRotateConfig = sgRots.add(new BoolSetting.Builder()
        .name("RotateSilent").description("Rotates silently to server")
        .defaultValue(false)
        .visible(rotateConfig::get)
        .build());
    private final Setting<Boolean> strictRotateConfig = sgRots.add(new BoolSetting.Builder()
        .name("YawStep").description("Rotates yaw over multiple ticks to prevent certain rotation flags in NCP")
        .defaultValue(false)
        .visible(rotateConfig::get)
        .build());
    public final Setting<Integer> rotateLimitConfig = sgRots.add(new IntSetting.Builder()
        .name("YawStep-Limit").description("Maximum yaw rotation in degrees for one tick")
        .defaultValue(180)
        .min(1)
        .sliderMax(180)
        .visible(() -> (rotateConfig.get() && strictRotateConfig.get()))
        .build());
    public final Setting<Integer> ticksExistedConfig = sgTargets.add(new IntSetting.Builder()
        .name("TicksExisted").description("The minimum age of the entity to be considered for attack")
        .defaultValue(0)
        .min(0)
        .sliderMax(200)
        .build());
    private final Setting<Boolean> armorCheckConfig = sgMisc.add(new BoolSetting.Builder()
        .name("ArmorCheck").description("Checks if target has armor before attacking")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> stopSprintConfig = sgMisc.add(new BoolSetting.Builder()
        .name("StopSprint").description("Stops sprinting before attacking to maintain vanilla behavior")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> stopShieldConfig = sgMisc.add(new BoolSetting.Builder()
        .name("StopShield").description("Automatically handles shielding before attacking")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> maceBreachConfig = sgSwitch.add(new BoolSetting.Builder()
        .name("MaceBreach").description("Abuses vanilla exploit to apply breach enchantment to swords")
        .defaultValue(false)
        .visible(() -> (autoSwapConfig.get() != Swap.SILENT))
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
    private final Setting<Boolean> invisiblesConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Invisibles").description("Target invisible entities")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> renderConfig = sgTargets.add(new BoolSetting.Builder()
        .name("Render").description("Renders an indicator over the target")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> disableDeathConfig = sgMisc.add(new BoolSetting.Builder()
        .name("DisableOnDeath").description("Disables during disconnect/death")
        .defaultValue(false)
        .build());

    private final Timer autoSwapTimer = new CacheTimer();
    private final Timer switchTimer = new CacheTimer();
    private Entity entityTarget;
    private long randomDelay = -1;
    private boolean shielding;
    private boolean sneaking;
    private boolean sprinting;
    private long lastAttackTime;
    private boolean rotated;

    private float[] silentRotations;

    public Aura() {
        super(Shoreline.MAIN, "Aura", "Attacks nearby entities", 700);
        INST = this;
    }

    public static Aura getInstance() {
        return INST;
    }

    @Override
    public String getInfoString() {
        return EnumFormatter.formatEnum(modeConfig.get());
    }

    @Override
    public void onDeactivate() {
        entityTarget = null;
        silentRotations = null;
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        if (disableDeathConfig.get()) {
            toggle();
        }
    }

    @EventHandler
    public void onRemoveEntity(RemoveEntityEvent event) {
        if (disableDeathConfig.get() && event.getEntity() == mc.player) {
            toggle();
        }
    }

    @EventHandler
    public void onPlayerUpdate(PlayerTickEvent event) {
        if (AutoCrystal.getInstance().isAttacking()
            || AutoCrystal.getInstance().isPlacing()
            || autoSwapConfig.get() == Swap.SILENT && AutoMine.getInstance().isSilentSwapping()
            || mc.player.isSpectator()) {
            return;
        }

        if (!multitaskConfig.get() && checkMultitask(true)) {
            return;
        }

        final Vec3d eyepos = Managers.POSITION.getEyePos();
        entityTarget = switch (modeConfig.get()) {
            case SWITCH -> getAttackTarget(eyepos);
            case SINGLE -> {
                if (entityTarget == null || !entityTarget.isAlive()
                    || !isInAttackRange(eyepos, entityTarget)) {
                    yield getAttackTarget(eyepos);
                }
                yield entityTarget;
            }
        };
        if (entityTarget == null || !switchTimer.passed(swapDelayConfig.get() * 25.0f)) {
            silentRotations = null;
            return;
        }
        if (mc.player.isUsingItem() && mc.player.getActiveHand() == Hand.MAIN_HAND
            || mc.options.attackKey.isPressed() || PlayerUtil.isHotbarKeysPressed()) {
            autoSwapTimer.reset();
        }

        int slot = getSwordSlot();
        boolean silentSwapped = false;
        if (!(mc.player.getMainHandStack().getItem() instanceof SwordItem) && slot != -1) {
            switch (autoSwapConfig.get()) {
                case NORMAL -> {
                    if (autoSwapTimer.passed(500)) {
                        Managers.INVENTORY.setClientSlot(slot);
                    }
                }
                case SILENT -> {
                    Managers.INVENTORY.setSlot(slot);
                    silentSwapped = true;
                }
            }
        }
        if (!isHoldingSword() && autoSwapConfig.get() != Swap.SILENT) {
            return;
        }
        if (rotateConfig.get()) {
            float[] rotation = RotationUtil.getRotationsTo(mc.player.getEyePos(),
                getAttackRotateVec(entityTarget));
            if (!silentRotateConfig.get() && strictRotateConfig.get()) {
                float serverYaw = Managers.ROTATION.getWrappedYaw();
                float diff = serverYaw - rotation[0];
                float diff1 = Math.abs(diff);
                if (diff1 > 180.0f) {
                    diff += diff > 0.0f ? -360.0f : 360.0f;
                }
                int dir = diff > 0.0f ? -1 : 1;
                float deltaYaw = dir * rotateLimitConfig.get();
                float yaw;
                if (diff1 > rotateLimitConfig.get()) {
                    yaw = serverYaw + deltaYaw;
                    rotated = false;
                } else {
                    yaw = rotation[0];
                    rotated = true;
                }
                rotation[0] = yaw;
            } else {
                rotated = true;
            }
            if (silentRotateConfig.get()) {
                silentRotations = rotation;
            } else {
                setRotation(rotation[0], rotation[1]);
            }
        }
        if (isRotationBlocked() || !rotated && rotateConfig.get() || !isInAttackRange(eyepos, entityTarget)) {
            Managers.INVENTORY.syncToClient();
            return;
        }
        if (attackDelayConfig.get()) {
            PlayerInventory inventory = mc.player.getInventory();
            ItemStack itemStack = inventory.getStack((slot == -1 || !swordCheckConfig.get()) ? mc.player.getInventory().selectedSlot : slot);

            MutableDouble attackSpeed = new MutableDouble(mc.player.getAttributeBaseValue(EntityAttributes.GENERIC_ATTACK_SPEED));

            AttributeModifiersComponent attributeModifiers = itemStack.get(DataComponentTypes.ATTRIBUTE_MODIFIERS);
            if (attributeModifiers != null) {
                attributeModifiers.applyModifiers(EquipmentSlot.MAINHAND, (entry, modifier) -> {
                    if (entry == EntityAttributes.GENERIC_ATTACK_SPEED) {
                        attackSpeed.add(modifier.value());
                    }
                });
            }

            double attackCooldownTicks = 1.0 / attackSpeed.getValue() * 20.0;

            int breachSlot = getBreachMaceSlot();
            if (autoSwapConfig.get() != Swap.SILENT && maceBreachConfig.get() && breachSlot != -1) {
                Managers.INVENTORY.setSlot(breachSlot);
            }

            float ticks = 20.0f - Managers.TICK.getTickSync(tpsSyncConfig.get());
            float currentTime = (System.currentTimeMillis() - lastAttackTime) + (ticks * 50.0f);
            if ((currentTime / 50.0f) >= attackCooldownTicks && attackTarget(entityTarget)) {
                lastAttackTime = System.currentTimeMillis();
            }

            if (autoSwapConfig.get() != Swap.SILENT && maceBreachConfig.get() && breachSlot != -1) {
                Managers.INVENTORY.syncToClient();
            }
        } else {
            if (randomDelay < 0) {
                randomDelay = (long) RANDOM.nextFloat((float) ((randomSpeedConfig.get() * 10.0f) + 1.0f));
            }
            float delay = (float) ((attackSpeedConfig.get() * 50.0f) + randomDelay);

            int breachSlot = getBreachMaceSlot();
            if (autoSwapConfig.get() != Swap.SILENT && maceBreachConfig.get() && breachSlot != -1) {
                Managers.INVENTORY.setSlot(breachSlot);
            }

            long currentTime = System.currentTimeMillis() - lastAttackTime;
            if (currentTime >= 1000.0f - delay && attackTarget(entityTarget)) {
                randomDelay = -1;
                lastAttackTime = System.currentTimeMillis();
            }

            if (autoSwapConfig.get() != Swap.SILENT && maceBreachConfig.get() && breachSlot != -1) {
                Managers.INVENTORY.syncToClient();
            }
        }

        if (autoSwapConfig.get() == Swap.SILENT && silentSwapped) {
            Managers.INVENTORY.syncToClient();
        }
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (mc.player == null) {
            return;
        }
        if (event.packet instanceof UpdateSelectedSlotC2SPacket) {
            switchTimer.reset();
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (AutoCrystal.getInstance().isAttacking() || AutoCrystal.getInstance().isPlacing() || mc.player.isSpectator()) {
            return;
        }
        if (entityTarget != null && renderConfig.get() && (isHoldingSword() || autoSwapConfig.get() == Swap.SILENT)) {
            long currentTime = System.currentTimeMillis() - lastAttackTime;
            float animFactor = 1.0f - MathHelper.clamp(currentTime / 1000f, 0.0f, 1.0f);
            int attackDelay = (int) (70.0 * animFactor);

            SettingColor set = TabConfigs.get().getColor();
            event.renderer.box(Interpolation.getInterpolatedEntityBox(entityTarget), TabConfigs.get().getClampColor(30 + attackDelay), new SettingColor(set.r, set.g, set.b), ShapeMode.Both, 0);
        }
    }

    private boolean attackTarget(Entity entity) {
        preAttackTarget();

        if (silentRotateConfig.get() && silentRotations != null) {
            setRotationSilent(silentRotations[0], silentRotations[1]);
        }

        PlayerInteractEntityC2SPacket packet = PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking());
        Managers.NETWORK.sendPacket(packet);
        if (swingConfig.get()) {
            mc.player.swingHand(Hand.MAIN_HAND);
        } else {
            Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
        }
        postAttackTarget(entity);

        if (silentRotateConfig.get()) {
            Managers.ROTATION.setRotationSilentSync();
        }
        return true;
    }

    private int getSwordSlot() {
        float sharp = 0.0f;
        int slot = -1;
        for (int i = 0; i < 9; i++) {
            final ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof SwordItem swordItem) {
                float sharpness = EnchantmentUtil.getLevel(stack,
                    Enchantments.SHARPNESS) * 0.5f + 0.5f;
                float dmg = swordItem.getMaterial().getAttackDamage() + sharpness;
                if (dmg > sharp) {
                    sharp = dmg;
                    slot = i;
                }
            } else if (stack.getItem() instanceof AxeItem axeItem) {
                float sharpness = EnchantmentUtil.getLevel(stack,
                    Enchantments.SHARPNESS) * 0.5f + 0.5f;
                float dmg = axeItem.getMaterial().getAttackDamage() + sharpness;
                if (dmg > sharp) {
                    sharp = dmg;
                    slot = i;
                }
            } else if (stack.getItem() instanceof TridentItem) {
                float sharpness = EnchantmentUtil.getLevel(stack,
                    Enchantments.SHARPNESS) * 0.5f + 0.5f;
                float dmg = TridentItem.ATTACK_DAMAGE + sharpness;
                if (dmg > sharp) {
                    sharp = dmg;
                    slot = i;
                }
            } else if (stack.getItem() instanceof MaceItem) {
                float sharpness = EnchantmentUtil.getLevel(stack,
                    Enchantments.SHARPNESS) * 0.5f + 0.5f;
                float dmg = 5.0f + sharpness;
                if (dmg > sharp) {
                    sharp = dmg;
                    slot = i;
                }
            }
        }
        return slot;
    }

    private int getBreachMaceSlot() {
        int slot = -1;
        int maxBreach = 0;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (!(stack.getItem() instanceof MaceItem)) {
                continue;
            }
            int breach = EnchantmentUtil.getLevel(stack, Enchantments.BREACH);
            if (breach > maxBreach) {
                slot = i;
                maxBreach = breach;
            }
        }
        return slot;
    }

    private void preAttackTarget() {
        final ItemStack offhand = mc.player.getOffHandStack();
        shielding = false;
        if (stopShieldConfig.get()) {
            shielding = offhand.getItem() == Items.SHIELD && mc.player.isBlocking();
            if (shielding) {
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM,
                    Managers.POSITION.getBlockPos(), Direction.getFacing(mc.player.getX(),
                    mc.player.getY(), mc.player.getZ())));
            }
        }
        sneaking = false;
        sprinting = false;
        if (stopSprintConfig.get()) {
            sneaking = Managers.POSITION.isSneaking();
            if (sneaking) {
                Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player,
                    ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            }
            sprinting = Managers.POSITION.isSprinting();
            if (sprinting) {
                Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player,
                    ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
        }
    }

    private void postAttackTarget(Entity entity) {
        if (shielding) {
            Managers.NETWORK.sendSequencedPacket(s ->
                new PlayerInteractItemC2SPacket(Hand.OFF_HAND, s, mc.player.getYaw(), mc.player.getPitch()));
        }
        if (sneaking) {
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player,
                ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        }
        if (sprinting) {
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player,
                ClientCommandC2SPacket.Mode.START_SPRINTING));
        }
    }

    private Entity getAttackTarget(Vec3d pos) {
        double min = Double.MAX_VALUE;
        Entity attackTarget = null;
        for (Entity entity : mc.world.getEntities()) {
            if (entity == null || entity == mc.player
                || !entity.isAlive() || !isEnemy(entity)
                || (entity instanceof PlayerEntity ent && (Friends.get().isFriend(ent)))
                || entity instanceof EndCrystalEntity
                || entity instanceof ItemEntity
                || entity instanceof ArrowEntity
                || entity instanceof ExperienceBottleEntity) {
                continue;
            }
            if (armorCheckConfig.get()
                && entity instanceof LivingEntity livingEntity
                && !livingEntity.getArmorItems().iterator().hasNext()) {
                continue;
            }
            double dist = pos.distanceTo(entity.getPos());
            if (dist <= searchRangeConfig.get()) {
                if (entity.age < ticksExistedConfig.get()) {
                    continue;
                }
                switch (priorityConfig.get()) {
                    case DISTANCE -> {
                        if (dist < min) {
                            min = dist;
                            attackTarget = entity;
                        }
                    }
                    case HEALTH -> {
                        if (entity instanceof LivingEntity e) {
                            float health = e.getHealth() + e.getAbsorptionAmount();
                            if (health < min) {
                                min = health;
                                attackTarget = entity;
                            }
                        }
                    }
                    case ARMOR -> {
                        if (entity instanceof LivingEntity e) {
                            float armor = getArmorDurability(e);
                            if (armor < min) {
                                min = armor;
                                attackTarget = entity;
                            }
                        }
                    }
                }
            }
        }
        return attackTarget;
    }

    private float getArmorDurability(LivingEntity e) {
        float edmg = 0.0f;
        float emax = 0.0f;
        for (ItemStack armor : e.getArmorItems()) {
            if (armor != null && !armor.isEmpty()) {
                edmg += armor.getDamage();
                emax += armor.getMaxDamage();
            }
        }
        return 100.0f - edmg / emax;
    }

    public boolean isInAttackRange(Vec3d pos, Entity entity) {
        final Vec3d entityPos = getAttackRotateVec(entity);
        double dist = pos.distanceTo(entityPos);
        return isInAttackRange(dist, pos, entityPos);
    }

    public boolean isInAttackRange(double dist, Vec3d pos, Vec3d entityPos) {
        if (vanillaRangeConfig.get() && dist > 3.0f) {
            return false;
        }
        if (dist > rangeConfig.get()) {
            return false;
        }
        BlockHitResult result = mc.world.raycast(new RaycastContext(
            pos, entityPos,
            RaycastContext.ShapeType.COLLIDER,
            RaycastContext.FluidHandling.NONE, mc.player));
        if (result != null && !result.getBlockPos().equals(BlockPos.ofFloored(entityPos)) && dist > wallRangeConfig.get()) {
            return false;
        }
        if (fovConfig.get() != 180.0f) {
            float[] rots = RotationUtil.getRotationsTo(pos, entityPos);
            float diff = MathHelper.wrapDegrees(mc.player.getYaw()) - rots[0];
            float magnitude = Math.abs(diff);
            return magnitude <= fovConfig.get();
        }
        return true;
    }

    public boolean isHoldingSword() {
        return !swordCheckConfig.get() || mc.player.getMainHandStack().getItem() instanceof SwordItem
            || mc.player.getMainHandStack().getItem() instanceof AxeItem
            || mc.player.getMainHandStack().getItem() instanceof TridentItem
            || mc.player.getMainHandStack().getItem() instanceof MaceItem;
    }

    private Vec3d getAttackRotateVec(Entity entity) {
        Vec3d feetPos = entity.getPos();
        return switch (hitVectorConfig.get()) {
            case FEET -> feetPos;
            case TORSO -> feetPos.add(0.0, entity.getHeight() / 2.0f, 0.0);
            case EYES -> entity.getEyePos();
            case AUTO -> {
                Vec3d torsoPos = feetPos.add(0.0, entity.getHeight() / 2.0f, 0.0);
                Vec3d eyesPos = entity.getEyePos();
                yield Stream.of(feetPos, torsoPos, eyesPos).min(Comparator.comparing(b -> mc.player.getEyePos().squaredDistanceTo(b))).orElse(eyesPos);
            }
        };
    }

    private boolean isEnemy(Entity e) {
        return (!e.isInvisible() || invisiblesConfig.get())
            && e instanceof PlayerEntity && playersConfig.get()
            || EntityUtil.isMonster(e) && monstersConfig.get()
            || EntityUtil.isNeutral(e) && neutralsConfig.get()
            || EntityUtil.isPassive(e) && animalsConfig.get();
    }

    public enum TargetMode {
        SWITCH,
        SINGLE
    }

    public enum Swap {
        NORMAL,
        SILENT,
        OFF
    }

    public enum Vector {
        EYES,
        TORSO,
        FEET,
        AUTO
    }

    public enum Priority {
        HEALTH,
        DISTANCE,
        ARMOR
    }
}
