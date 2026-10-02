package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.JumpRotationEvent;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.events.SprintCancelEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.RotationModule;
import me.lyeddie.addon.util.tabs.TabConfigs;
import me.lyeddie.addon.util.EnumFormatter;
import me.lyeddie.addon.util.literal.MovementUtil;
import me.lyeddie.addon.util.literal.PlayerUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.util.math.MathHelper;

public class SprintII extends RotationModule {
    private static SprintII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<SprintMode> modeConfig = sgGeneral.add(new EnumSetting.Builder<SprintMode>()
        .name("Mode").description("Sprinting mode. Rage allows for multi-directional sprinting.")
        .defaultValue(SprintMode.LEGIT)
        .build());
    private final Setting<Boolean> jumpFixConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("JumpFix").description("Fixes jumping slowdown in Rage sprint")
        .defaultValue(true)
        .visible(() -> modeConfig.get() == SprintMode.RAGE || modeConfig.get() == SprintMode.RAGE_STRICT)
        .build());

    public SprintII() {
        super(Shoreline.MAIN, "SprintII", "Automatically sprints", 110);
        INST = this;
    }

    @Override
    public String getInfoString() {
        return EnumFormatter.formatEnum(modeConfig.get());
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (canSprint()) {
            float sprintYaw = getSprintYaw(mc.player.getYaw());
            if (checkSprintAngle(sprintYaw)) {
                return;
            }
            switch (modeConfig.get()) {
                case LEGIT -> {
                    if (mc.player.input.hasForwardMovement()
                        && (!mc.player.horizontalCollision
                        || mc.player.collidedSoftly)) {
                        mc.player.setSprinting(true);
                    }
                }
                case RAGE, RAGE_STRICT, GRIM -> mc.player.setSprinting(true);
            }
        }
    }

    @EventHandler
    public void onSprintCancel(SprintCancelEvent event) {
        if (canSprint() && (modeConfig.get() == SprintMode.RAGE || modeConfig.get() == SprintMode.RAGE_STRICT)) {
            float sprintYaw = getSprintYaw(mc.player.getYaw());
            if (checkSprintAngle(sprintYaw)) {
                return;
            }
            event.cancel();
        }
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent event) {
        if (canSprint() && modeConfig.get() == SprintMode.RAGE_STRICT) {
            setRotation(getSprintYaw(mc.player.getYaw()), mc.player.getPitch());
        }
    }

    @EventHandler
    public void onJumpYaw(JumpRotationEvent event) {
        if (jumpFixConfig.get() && (modeConfig.get() == SprintMode.RAGE || modeConfig.get() == SprintMode.RAGE_STRICT)) {
            float yaw = event.getYaw();
            float forward = Math.signum(mc.player.input.movementForward);
            float strafe = 90.0f * Math.signum(mc.player.input.movementSideways);
            if (forward != 0.0f) {
                strafe *= (forward * 0.5f);
            }
            yaw -= strafe;
            if (forward < 0.0f) {
                yaw -= 180.0f;
            }

            event.cancel();
            event.setYaw(yaw);
        }
    }

    public static SprintII getInstance() {
        return INST;
    }

    private boolean canSprint() {
        if (TabConfigs.get().getWebJumpFix() && PlayerUtil.inWeb(1.0)) {
            return false;
        }
        return MovementUtil.isInputtingMovement()
            && !mc.player.isSneaking()
            && !mc.player.isRiding()
            && !mc.player.isFallFlying()
            && !mc.player.isTouchingWater()
            && !mc.player.isInLava()
            && !mc.player.isHoldingOntoLadder()
            && !mc.player.hasStatusEffect(StatusEffects.BLINDNESS)
            && mc.player.getHungerManager().getFoodLevel() > 6.0F;
    }

    private boolean checkSprintAngle(float sprintYaw) {
        if (modeConfig.get() == SprintMode.RAGE_STRICT) {
            return MathHelper.angleBetween(sprintYaw, Managers.ROTATION.getServerYaw()) > 0.0f;
        } else if (modeConfig.get() == SprintMode.GRIM) {
            return MathHelper.angleBetween(mc.player.getYaw(), Managers.ROTATION.getServerYaw()) > 0.0f;
        }
        return false;
    }

    public float getSprintYaw(float yaw) {
        boolean forward = mc.options.forwardKey.isPressed();
        boolean backward = mc.options.backKey.isPressed();
        boolean left = mc.options.leftKey.isPressed();
        boolean right = mc.options.rightKey.isPressed();
        if (forward && !backward) {
            if (left && !right) {
                yaw -= 45.0f;
            } else if (right && !left) {
                yaw += 45.0f;
            }
        } else if (backward && !forward) {
            yaw += 180.0f;
            if (left && !right) {
                yaw += 45.0f;
            } else if (right && !left) {
                yaw -= 45.0f;
            }
        } else if (left && !right) {
            yaw -= 90.0f;
        } else if (right && !left) {
            yaw += 90.0f;
        }
        return MathHelper.wrapDegrees(yaw);
    }

    public enum SprintMode {
        LEGIT,
        RAGE,
        RAGE_STRICT,
        GRIM
    }
}
