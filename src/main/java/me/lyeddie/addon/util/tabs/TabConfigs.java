package me.lyeddie.addon.util.tabs;

import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.impl.Disabler;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.meteorclient.systems.System;
import meteordevelopment.meteorclient.systems.Systems;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.math.MathHelper;
import java.awt.Color;

public class TabConfigs extends System<TabConfigs> implements Globals {
    public final Settings settings = new Settings();

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgAnticheat = settings.createGroup("Anticheat");
    private final SettingGroup sgColors = settings.createGroup("Colors");
    private final SettingGroup sgRotations = settings.createGroup("Rotations");
    private final SettingGroup sgServer = settings.createGroup("Server");
    private final SettingGroup sgSocials = settings.createGroup("Socials");

    public final Setting<Boolean> invalidSlotTweak = sgGeneral.add(new BoolSetting.Builder()
            .name("InvalidSlotTweak").description("disabled, tweaks something that conflicts with other client's switch/swapping methods:" +
                    "it might now causes issues with this addon's features")
            .defaultValue(true)
            .build());

    public final Setting<Anticheats> modeConfig = sgAnticheat.add(new EnumSetting.Builder<Anticheats>()
            .name("Mode").description("Applies anticheat optimizations")
            .defaultValue(Anticheats.VANILLA)
            .build());
    public final Setting<Double> entityPlaceConfig = sgAnticheat.add(new DoubleSetting.Builder()
            .name("PlaceThreshold").description("The max ticks to place on entities")
            .defaultValue(2.0)
            .min(0.0)
            .sliderMax(25.0)
            .visible(() -> modeConfig.get() != Anticheats.VANILLA)
            .build());
    public final Setting<Boolean> miningFixConfig = sgAnticheat.add(new BoolSetting.Builder()
            .name("MiningFix").description("Fixes vanilla mining on GrimV3")
            .defaultValue(false)
            .visible(() -> modeConfig.get() == Anticheats.GRIM)
            .build());
    public final Setting<Boolean> webJumpFixConfig = sgAnticheat.add(new BoolSetting.Builder()
            .name("WebJumpFix").description("Fixes sprint jumping in webs on grim")
            .defaultValue(false)
            .visible(() -> modeConfig.get() == Anticheats.GRIM)
            .build());
    public final Setting<Boolean> noScreenCloseConfig = sgAnticheat.add(new BoolSetting.Builder()
            .name("NoScreenClose").description("Prevents the server from closing your inventory screen")
            .defaultValue(false)
            .visible(() -> modeConfig.get() == Anticheats.GRIM)
            .build());
    public final Setting<Boolean> raytraceSpoofConfig = sgAnticheat.add(new BoolSetting.Builder()
            .name("RaytraceFix").description("Allows you to spoof your raytrace")
            .defaultValue(false)
            .visible(() -> modeConfig.get() == Anticheats.N_C_P)
            .onChanged(ts -> {
                if (!ts && Managers.TAB_EVENTS != null) { // Settings reset before managers initialize on startup.
                    Managers.TAB_EVENTS.pitch = Float.NaN;
                }
            })
            .build());

    public final Setting<SettingColor> colorConfig = sgColors.add(new ColorSetting.Builder()
            .name("Global").description("The primary client color")
            .defaultValue(new SettingColor(50, 100, 205))
            .build());

    public final Setting<Double> preserveTicksConfig = sgRotations.add(new DoubleSetting.Builder()
            .name("PreserveTicks").description("Time to preserve rotations after reaching the target rotations")
            .defaultValue(10.0)
            .min(0.0)
            .sliderMax(20.0)
            .build());
    public final Setting<Boolean> movementFixConfig = sgRotations.add(new BoolSetting.Builder()
            .name("MovementFix").description("Fixes movement on Grim when rotating")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> mouseSensFixConfig = sgRotations.add(new BoolSetting.Builder()
            .name("MouseSensFix").description("Fixes movement on Grim when applying mouse sensitivity")
            .defaultValue(false)
            .build());

    public final Setting<Boolean> demoConfig = sgServer.add(new BoolSetting.Builder()
            .name("NoDemo").description("Prevents servers from forcing you to a demo screen")
            .defaultValue(true)
            .build());
    public final Setting<Boolean> resourcePackConfig = sgServer.add(new BoolSetting.Builder()
            .name("NoResourcePack").description("Prevents server from forcing resource pack")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> antiCrashConfig = sgServer.add(new BoolSetting.Builder()
            .name("NoServerCrash").description("Prevents server packets from crashing the client")
            .defaultValue(false)
            .build());
    public final Setting<Boolean> illegalDisconnectConfig = sgServer.add(new BoolSetting.Builder()
            .name("IllegalDisconnect").description("Disconnects by getting kicked from server")
            .defaultValue(false)
            .build());

    public final Setting<Boolean> friendsConfig = sgSocials.add(new BoolSetting.Builder()
            .name("Friends").description("Allows friend system to function")
            .defaultValue(true)
            .visible(() -> false)
            .build());
    public final Setting<Boolean> addNotifyConfig = sgSocials.add(new BoolSetting.Builder()
            .name("AddNotify").description("Notifies players when you add them as a friend")
            .defaultValue(true)
            .visible(friendsConfig::get)
            .build());
    public final Setting<String> customNotifySetting = sgSocials.add(new StringSetting.Builder()
            .name("NotifyMessage").description("Format the message <on friend add> here")
            .defaultValue("I just added you as a friend on Shoreline Addon!")
            .visible(addNotifyConfig::get)
            .build());
    public final Setting<SettingColor> friendsColorConfig = sgSocials.add(new ColorSetting.Builder()
            .name("FriendsColor").description("The color for friends in the client")
            .defaultValue(new SettingColor(102, 255, 255))
            .visible(() -> false)
            .build());

    public TabConfigs(String name) {
        super(name);
    }

    @Override
    public NbtCompound toTag() {
        NbtCompound tag = new NbtCompound();
        tag.put("settings", settings.toTag());
        return tag;
    }

    @Override
    public TabConfigs fromTag(NbtCompound tag) {
        if (tag.contains("settings")) settings.fromTag(tag.getCompound("settings"));
        return this;
    }

    public static TabConfigs get() {
        return Systems.get(TabConfigs.class);
    }

    public SettingColor getColor() {
        return colorConfig.get();
    }

    public int getColorRGB() {
        SettingColor config = colorConfig.get();
        return new Color(config.r, config.g, config.b).getRGB();
    }

    public SettingColor getClampColor(int alpha) {
        SettingColor config = colorConfig.get();
        return new SettingColor(config.r, config.g, config.b, MathHelper.clamp(alpha, 0, 255));
    }

    public boolean getMovementFix() {
        return movementFixConfig.get() && (!Disabler.getInstance().isActive() || !Disabler.getInstance().isYawOverflow());
    }

    public boolean isGrim() {
        return modeConfig.get() == Anticheats.GRIM;
    }

    public boolean isNCP() {
        return modeConfig.get() == Anticheats.N_C_P;
    }

    public boolean getMiningFix() {
        return miningFixConfig.get();
    }

    public boolean getWebJumpFix() {
        return webJumpFixConfig.get();
    }

    public int getEntityPlaceThreshold() {
        return modeConfig.get() == Anticheats.VANILLA ? 0 : Math.round(toFloat(entityPlaceConfig.get()) * 10.0F);
    }

    private float toFloat(double db) {
        return (float) db;
    }

    public enum Anticheats {
        GRIM,
        N_C_P,
        VANILLA
    }
}
