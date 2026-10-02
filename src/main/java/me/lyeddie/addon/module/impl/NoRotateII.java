package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.ServerRotationEvent;
import me.lyeddie.addon.managers.Managers;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;

public class NoRotateII extends AddonModule {
    private static NoRotateII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> positionAdjustConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("PositionAdjust")
        .description("Adjusts outgoing rotation packets")
        .defaultValue(true)
        .build());

    public NoRotateII() {
        super(Shoreline.MAIN, "NoRotateII", "Prevents server from forcing rotations");
        INST = this;
    }

    @EventHandler
    public void onServerRotation(ServerRotationEvent event) {
        event.cancel();
        if (positionAdjustConfig.get()) {
            float yaw = Managers.ROTATION.getServerYaw();
            float pitch = Managers.ROTATION.getServerPitch();
            if (Managers.ROTATION.isRotating()) {
                yaw = Managers.ROTATION.getRotationYaw();
                pitch = Managers.ROTATION.getRotationPitch();
            }
            event.setYaw(yaw);
            event.setPitch(pitch);
        }
    }

    public static NoRotateII getInstance() {
        return INST;
    }
}
