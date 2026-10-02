package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.PlayerMoveEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.EnumFormatter;
import me.lyeddie.addon.util.literal.MovementUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.meteorclient.utils.entity.fakeplayer.FakePlayerEntity;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundPlayerPositionPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class SpeedII extends AddonModule {
    private static SpeedII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<SpeedMode> speedModeConfig = sgGeneral.add(new EnumSetting.Builder<SpeedMode>()
        .name("Mode").description("Speed mode")
        .defaultValue(SpeedMode.STRAFE)
        .build());
    public final Setting<Double> collisionDistanceConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("CollisionDistance").description("The distance to apply collision speed")
        .defaultValue(1.5)
        .min(0.5)
        .sliderMax(2.0)
        .visible(() -> speedModeConfig.get() == SpeedMode.GRIM_COLLIDE)
        .build());
    public final Setting<Double> speedConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Speed").description("The speed for alternative modes")
        .defaultValue(4.0)
        .min(0.1)
        .sliderMax(50.0)
        .visible(() -> speedModeConfig.get() == SpeedMode.VANILLA)
        .build());
    private final Setting<Boolean> timerConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("UseTimer").description("Uses timer to increase acceleration")
        .defaultValue(false)
        .visible(this::isStrafe)
        .onChanged(grr -> {
            if (isStrafe()) onConfigUpdate(grr);
        })
        .build());
    private final Setting<Boolean> fastConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Fast").description("Fast timer speed")
        .defaultValue(false)
        .visible(() -> speedModeConfig.get() == SpeedMode.STRAFE_STRICT && timerConfig.get())
        .build());
    private final Setting<Boolean> strafeBoostConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("StrafeBoost").description("Uses explosion velocity to boost Strafe")
        .defaultValue(false)
        .visible(this::isStrafe)
        .build());
    public final Setting<Integer> boostTicksConfig = sgGeneral.add(new IntSetting.Builder()
        .name("BoostTicks").description("The number of ticks to boost strafe")
        .defaultValue(20)
        .min(10)
        .sliderMax(40)
        .visible(() -> isStrafe() && strafeBoostConfig.get())
        .build());
    private final Setting<Boolean> speedWaterConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("SpeedInWater").description("Applies speed even in water and lava")
        .defaultValue(false)
        .build());

    private static final float FRICTION = 159.077f;
    private int strafe = 4;
    private boolean accel;
    private int strictTicks;
    private int strictFastTicks;
    private int boostTicks;
    private double speed;
    private double boostSpeed;
    private double distance;
    private boolean prevTimer;

    public SpeedII() {
        super(Shoreline.MAIN, "SpeedII", "Move faster");
        INST = this;
    }

    public void onConfigUpdate(boolean grr) {
        if (grr) {
            prevTimer = TimerII.getInstance().isActive();
            if (!prevTimer) {
                TimerII.getInstance().toggle();
            }
        } else if (TimerII.getInstance().isActive()) {
            TimerII.getInstance().resetTimer();
            if (!prevTimer) {
                TimerII.getInstance().toggle();
            }
        }
    }

    @Override
    public String getInfoString() {
        if (speedModeConfig.get() == SpeedMode.GRIM_COLLIDE) {
            return "Grim";
        }
        return EnumFormatter.formatEnum(speedModeConfig.get());
    }

    @Override
    public void onActivate() {
        prevTimer = TimerII.getInstance().isActive();
        if (timerConfig.get() && !prevTimer && isStrafe()) {
            TimerII.getInstance().toggle();
        }
    }

    @Override
    public void onDeactivate() {
        resetStrafe();
        if (TimerII.getInstance().isActive()) {
            TimerII.getInstance().resetTimer();
            if (!prevTimer) {
                TimerII.getInstance().toggle();
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        boostTicks++;
        if (boostTicks > boostTicksConfig.get()) {
            boostSpeed = 0.0;
        }
        double dx = mc.player.getX() - mc.player.xo;
        double dz = mc.player.getZ() - mc.player.zo;
        distance = Math.sqrt(dx * dx + dz * dz);
        if (speedModeConfig.get() == SpeedMode.GRIM_COLLIDE && MovementUtil.isInputtingMovement()) {
            int collisions = 0;
            for (Entity entity : mc.level.entitiesForRendering()) {
                if (checkIsCollidingEntity(entity) && Mth.sqrt((float) mc.player.distanceToSqr(entity)) <= collisionDistanceConfig.get()) {
                    collisions++;
                }
            }
            if (collisions > 0) {
                Vec3 velocity = mc.player.getDeltaMovement();
                double factor = 0.08 * collisions;
                Vec2 strafe = handleStrafeMotion((float) factor);
                mc.player.setDeltaMovement(velocity.x + strafe.x, velocity.y, velocity.z + strafe.y);
            }
        }
    }

    @EventHandler
    public void onMove(PlayerMoveEvent event) {
        if (mc.player != null && mc.level != null && event.getType() == MoverType.SELF) {
            if (!MovementUtil.isInputtingMovement()
                || Disabler.getInstance().grimFireworkCheck()
                || mc.player.getAbilities().flying
                || mc.player.isHandsBusy()
                || mc.player.isFallFlying()
                || mc.player.isSuppressingSlidingDownLadder()
                || mc.player.fallDistance > 2.0f
                || (mc.player.isInLava() || mc.player.isInWater())
                && !speedWaterConfig.get()) {
                resetStrafe();
                TimerII.getInstance().setTimer(1.0f);
                return;
            }
            event.cancel();
            double speedEffect = 1.0;
            double slowEffect = 1.0;
            if (mc.player.hasEffect(MobEffects.SPEED)) {
                double amplifier = mc.player.getEffect(MobEffects.SPEED).getAmplifier();
                speedEffect = 1 + (0.2 * (amplifier + 1));
            }
            if (mc.player.hasEffect(MobEffects.SLOWNESS)) {
                double amplifier = mc.player.getEffect(MobEffects.SLOWNESS).getAmplifier();
                slowEffect = 1 + (0.2 * (amplifier + 1));
            }
            final double base = 0.2873f * speedEffect / slowEffect;
            float jumpEffect = 0.0f;
            if (mc.player.hasEffect(MobEffects.JUMP_BOOST)) {
                jumpEffect += (mc.player.getEffect(MobEffects.JUMP_BOOST).getAmplifier() + 1) * 0.1f;
            }
            if (speedModeConfig.get() == SpeedMode.STRAFE || speedModeConfig.get() == SpeedMode.STRAFE_B_HOP) {
                if (!Managers.ANTICHEAT.hasPassed(100)) {
                    return;
                }
                if (timerConfig.get()) {
                    TimerII.getInstance().setTimer(1.0888f);
                }
                if (strafe == 1) {
                    speed = 1.35f * base - 0.01f;
                } else if (strafe == 2) {
                    if (mc.player.input.keyPresses.jump() || !mc.player.onGround()) {
                        return;
                    }
                    float jump = (speedModeConfig.get() == SpeedMode.STRAFE_B_HOP ? 0.4000000059604645f : 0.3999999463558197f) + jumpEffect;
                    event.setY(jump);
                    Managers.MOVEMENT.setMotionY(jump);
                    speed *= speedModeConfig.get() == SpeedMode.STRAFE_B_HOP ? 1.535 : (accel ? 1.6835 : 1.395);
                } else if (strafe == 3) {
                    double moveSpeed = 0.66 * (distance - base);
                    speed = distance - moveSpeed;
                    accel = !accel;
                } else {
                    if ((!mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0,
                        mc.player.getDeltaMovement().y(), 0)) || mc.player.verticalCollision) && strafe > 0) {
                        strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                    }
                    speed = distance - distance / FRICTION;
                }
                speed = Math.max(speed, base);
                if (strafeBoostConfig.get()) {
                    speed += boostSpeed;
                }
                final Vec2 motion = handleStrafeMotion((float) speed);
                event.setX(motion.x);
                event.setZ(motion.y);
                strafe++;
            }
            else if (speedModeConfig.get() == SpeedMode.STRAFE_STRICT) {
                if (!Managers.ANTICHEAT.hasPassed(100)) {
                    return;
                }
                if (timerConfig.get()) {
                    if (fastConfig.get()) {
                        ++strictFastTicks;
                        if (strictFastTicks > 10) {
                            strictFastTicks = 0;
                        }
                        float res = 1.0f + strictFastTicks / 100.0f;
                        TimerII.getInstance().setTimer(Math.max(1.0f, res));
                    } else {
                        TimerII.getInstance().setTimer(1.0888f);
                    }
                }
                if (strafe == 1) {
                    speed = 1.35f * base - 0.01f;
                } else if (strafe == 2) {
                    if (mc.player.input.keyPresses.jump() || !mc.player.onGround()) {
                        return;
                    }
                    float jump = 0.3999999463558197f + jumpEffect;
                    event.setY(jump);
                    Managers.MOVEMENT.setMotionY(jump);
                    speed *= 2.149;
                } else if (strafe == 3) {
                    double moveSpeed = 0.66 * (distance - base);
                    speed = distance - moveSpeed;
                } else {
                    if ((!mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0,
                        mc.player.getDeltaMovement().y(), 0)) || mc.player.verticalCollision) && strafe > 0) {
                        strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                    }
                    speed = distance - distance / FRICTION;
                }
                strictTicks++;
                speed = Math.max(speed, base);
                if (timerConfig.get()) {
                    TimerII.getInstance().setTimer(1.0888f);
                }
                double baseMax = 0.465 * speedEffect / slowEffect;
                double baseMin = 0.44 * speedEffect / slowEffect;
                speed = Math.min(speed, strictTicks > 25 ? baseMax : baseMin);
                if (strafeBoostConfig.get()) {
                    speed += boostSpeed;
                }
                if (strictTicks > 50) {
                    strictTicks = 0;
                }
                final Vec2 motion = handleStrafeMotion((float) speed);
                event.setX(motion.x);
                event.setZ(motion.y);
                strafe++;
            } else if (speedModeConfig.get() == SpeedMode.LOW_HOP) {
                if (!Managers.ANTICHEAT.hasPassed(100)) {
                    return;
                }
                if (timerConfig.get()) {
                    TimerII.getInstance().setTimer(1.0888f);
                }
                if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.4, 3)) {
                    Managers.MOVEMENT.setMotionY(0.31 + jumpEffect);
                    event.setY(0.31 + jumpEffect);
                } else if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.71, 3)) {
                    Managers.MOVEMENT.setMotionY(0.04 + jumpEffect);
                    event.setY(0.04 + jumpEffect);
                } else if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.75, 3)) {
                    Managers.MOVEMENT.setMotionY(-0.2 - jumpEffect);
                    event.setY(-0.2 - jumpEffect);
                } else if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.55, 3)) {
                    Managers.MOVEMENT.setMotionY(-0.14 + jumpEffect);
                    event.setY(-0.14 + jumpEffect);
                } else {
                    if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.41, 3)) {
                        Managers.MOVEMENT.setMotionY(-0.2 + jumpEffect);
                        event.setY(-0.2 + jumpEffect);
                    }
                }
                if (strafe == 1) {
                    speed = 1.35f * base - 0.01f;
                } else if (strafe == 2) {
                    double jump = (isBoxColliding() ? 0.2 : 0.3999) + jumpEffect;
                    Managers.MOVEMENT.setMotionY(jump);
                    event.setY(jump);
                    speed *= accel ? 1.5685 : 1.3445;
                } else if (strafe == 3) {
                    double moveSpeed = 0.66 * (distance - base);
                    speed = distance - moveSpeed;
                    accel = !accel;
                } else {
                    if (mc.player.onGround() && strafe > 0) {
                        strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                    }
                    speed = distance - distance / FRICTION;
                }
                speed = Math.max(speed, base);
                Vec2 motion = handleVanillaMotion((float) speed);
                event.setX(motion.x);
                event.setZ(motion.y);
                strafe++;
            } else if (speedModeConfig.get() == SpeedMode.GAY_HOP) {
                if (!Managers.ANTICHEAT.hasPassed(100)) {
                    strafe = 1;
                    return;
                }
                if (strafe == 1 && mc.player.verticalCollision
                    && MovementUtil.isInputtingMovement()) {
                    speed = 1.25f * base - 0.01f;
                } else if (strafe == 2 && mc.player.verticalCollision
                    && MovementUtil.isInputtingMovement()) {
                    float jump = (isBoxColliding() ? 0.2f : 0.4f) + jumpEffect;
                    event.setY(jump);
                    Managers.MOVEMENT.setMotionY(jump);
                    speed *= 2.149;
                } else if (strafe == 3) {
                    double moveSpeed = 0.66 * (distance - base);
                    speed = distance - moveSpeed;
                } else {
                    if (mc.player.onGround() && strafe > 0) {
                        if (1.35 * base - 0.01 > speed) {
                            strafe = 0;
                        } else {
                            strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                        }
                    }
                    speed = distance - distance / FRICTION;
                }
                speed = Math.max(speed, base);
                if (strafe > 0) {
                    Vec2 motion = handleStrafeMotion((float) speed);
                    event.setX(motion.x);
                    event.setZ(motion.y);
                }
                strafe++;
            } else if (speedModeConfig.get() == SpeedMode.V_HOP) {
                if (!Managers.ANTICHEAT.hasPassed(100)) {
                    strafe = 1;
                    return;
                }
                if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.4, 3)) {
                    Managers.MOVEMENT.setMotionY(0.31 + jumpEffect);
                    event.setY(0.31 + jumpEffect);
                } else if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.71, 3)) {
                    Managers.MOVEMENT.setMotionY(0.04 + jumpEffect);
                    event.setY(0.04 + jumpEffect);
                } else if (round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.75, 3)) {
                    Managers.MOVEMENT.setMotionY(-0.2 - jumpEffect);
                    event.setY(-0.2 - jumpEffect);
                }
                if (!mc.level.noCollision(null, mc.player.getBoundingBox().move(0.0, -0.56, 0.0))
                    && round(mc.player.getY() - (double) (int) mc.player.getY(), 3) == round(0.55, 3)) {
                    Managers.MOVEMENT.setMotionY(-0.14 + jumpEffect);
                    event.setY(-0.14 + jumpEffect);
                }
                if (strafe != 1 || !mc.player.verticalCollision
                    || mc.player.zza == 0.0f && mc.player.xxa == 0.0f) {
                    if (strafe != 2 || !mc.player.verticalCollision
                        || mc.player.zza == 0.0f && mc.player.xxa == 0.0f) {
                        if (strafe == 3) {
                            double moveSpeed = 0.66 * (distance - base);
                            speed = distance - moveSpeed;
                        } else {
                            if (mc.player.onGround() && strafe > 0) {
                                if (1.35 * base - 0.01 > speed) {
                                    strafe = 0;
                                } else {
                                    strafe = MovementUtil.isInputtingMovement() ? 1 : 0;
                                }
                            }
                            speed = distance - distance / FRICTION;
                        }
                    } else {
                        double jump = (isBoxColliding() ? 0.2 : 0.4) + jumpEffect;
                        Managers.MOVEMENT.setMotionY(jump);
                        event.setY(jump);
                        speed *= 2.149;
                    }
                } else {
                    speed = 2.0 * base - 0.01;
                }
                if (strafe > 8) {
                    speed = base;
                }
                speed = Math.max(speed, base);
                Vec2 motion = handleStrafeMotion((float) speed);
                event.setX(motion.x);
                event.setZ(motion.y);
                strafe++;
            } else if (speedModeConfig.get() == SpeedMode.B_HOP) {
                if (!Managers.ANTICHEAT.hasPassed(100)) {
                    strafe = 4;
                    return;
                }
                if (round(mc.player.getY() - ((int) mc.player.getY()), 3) == round(0.138, 3)) {
                    Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y - (0.08 + jumpEffect));
                    event.setY(event.getY() - (0.0931 + jumpEffect));
                    Managers.POSITION.setPositionY(mc.player.getY() - (0.0931 + jumpEffect));
                }
                if (strafe != 2 || mc.player.zza == 0.0f && mc.player.xxa == 0.0f) {
                    if (strafe == 3) {
                        double moveSpeed = 0.66 * (distance - base);
                        speed = distance - moveSpeed;
                    } else {
                        if (mc.player.onGround()) {
                            strafe = 1;
                        }
                        speed = distance - distance / FRICTION;
                    }
                } else {
                    double jump = (isBoxColliding() ? 0.2 : 0.4) + jumpEffect;
                    Managers.MOVEMENT.setMotionY(jump);
                    event.setY(jump);
                    speed *= 2.149;
                }
                speed = Math.max(speed, base);
                Vec2 motion = handleStrafeMotion((float) speed);
                event.setX(motion.x);
                event.setZ(motion.y);
                strafe++;
            } else if (speedModeConfig.get() == SpeedMode.VANILLA) {
                Vec2 motion = handleStrafeMotion((float) (speedConfig.get() / 10.0f));
                event.setX(motion.x);
                event.setZ(motion.y);
            }
        }

    }

    @EventHandler
    public void onPacketIn(PacketEvent.Receive event) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (event.packet instanceof ClientboundSetEntityMotionPacket packet
            && packet.id() == mc.player.getId()) {
            double x = packet.movement().x;
            double z = packet.movement().z;
        } else if (event.packet instanceof ClientboundPlayerPositionPacket) {
            resetStrafe();
        }
    }

    public Vec2 handleStrafeMotion(final float speed) {
        float forward = MovementUtil.getForward(mc.player.input);
        float strafe = MovementUtil.getSideways(mc.player.input);
        float yaw = mc.player.yRotO + (mc.player.getYRot() - mc.player.yRotO) * mc.getDeltaTracker().getGameTimeDeltaPartialTick(true);
        if (forward == 0.0f && strafe == 0.0f) {
            return Vec2.ZERO;
        } else if (forward != 0.0f) {
            if (strafe >= 1.0f) {
                yaw += forward > 0.0f ? -45 : 45;
                strafe = 0.0f;
            } else if (strafe <= -1.0f) {
                yaw += forward > 0.0f ? 45 : -45;
                strafe = 0.0f;
            }
            if (forward > 0.0f) {
                forward = 1.0f;
            } else if (forward < 0.0f) {
                forward = -1.0f;
            }
        }
        float rx = (float) Math.cos(Math.toRadians(yaw));
        float rz = (float) -Math.sin(Math.toRadians(yaw));
        return new Vec2((forward * speed * rz) + (strafe * speed * rx),
            (forward * speed * rx) - (strafe * speed * rz));
    }

    public Vec2 handleVanillaMotion(final float speed) {
        float forward = MovementUtil.getForward(mc.player.input);
        float strafe = MovementUtil.getSideways(mc.player.input);
        if (forward == 0.0f && strafe == 0.0f) {
            return Vec2.ZERO;
        } else if (forward != 0.0f && strafe != 0.0f) {
            forward *= (float) Math.sin(0.7853981633974483);
            strafe *= (float) Math.cos(0.7853981633974483);
        }
        return new Vec2((float) (forward * speed * -Math.sin(Math.toRadians(mc.player.getYRot())) + strafe * speed * Math.cos(Math.toRadians(mc.player.getYRot()))),
            (float) (forward * speed * Math.cos(Math.toRadians(mc.player.getYRot())) - strafe * speed * -Math.sin(Math.toRadians(mc.player.getYRot()))));
    }


    public boolean isBoxColliding() {
        return !mc.level.noCollision(mc.player, mc.player.getBoundingBox().move(0.0, 0.21, 0.0));
    }

    public boolean checkIsCollidingEntity(Entity entity) {
        return entity != null && entity != mc.player && entity instanceof LivingEntity && !(entity instanceof FakePlayerEntity) && !(entity instanceof ArmorStand);
    }

    public void setPrevTimer() {
        prevTimer = !prevTimer;
    }

    public boolean isUsingTimer() {
        return isActive() && timerConfig.get();
    }

    public void resetStrafe() {
        strafe = 4;
        strictTicks = 0;
        strictFastTicks = 0;
        speed = 0.0f;
        distance = 0.0;
        accel = false;
    }

    public boolean isStrafe() {
        return speedModeConfig.get() != SpeedMode.GRIM_COLLIDE && speedModeConfig.get() != SpeedMode.VANILLA;
    }

    public enum SpeedMode {
        STRAFE,
        STRAFE_STRICT,
        STRAFE_B_HOP,
        LOW_HOP,
        GAY_HOP,
        V_HOP,
        B_HOP,
        VANILLA,
        GRIM_COLLIDE
    }

    public static SpeedII getInstance() {
        return INST;
    }
}
