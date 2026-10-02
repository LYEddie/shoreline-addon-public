package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.BlockCollisionEvent;
import me.lyeddie.addon.events.PlayerClimbEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.AddonModule;
import me.lyeddie.addon.util.literal.BlockUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.Blocks;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.shape.VoxelShapes;

public class Avoid extends AddonModule {
    private static Avoid INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> voidConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Void").description("Prevents player from falling into the void")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> fireConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Fire").description("Prevents player from walking into fire")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> berryBushConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("BerryBush").description("Prevents player from walking into sweet berry bushes")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> cactiConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Cactus").description("Prevents player from walking into cacti")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> unloadedConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Unloaded").description("Prevents player from entering chunks that haven't been loaded")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> noClimbConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Climb").description("Prevents player from climbing up blocks")
        .defaultValue(false)
        .build());

    public Avoid() {
        super(Shoreline.MAIN, "Avoid", "Prevents player from entering harmful areas");
        INST = this;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (voidConfig.get() && !mc.player.isSpectator() && mc.player.getY() < mc.world.getBottomY()) {
            Managers.MOVEMENT.setMotionY(0.0);
        }
    }

    @EventHandler
    public void onBlockCollision(BlockCollisionEvent event) {
        BlockPos pos = event.getPos();
        if (fireConfig.get() && event.getBlock() == Blocks.FIRE
            && mc.player.getY() < pos.getY() + 1.0 || cactiConfig.get()
            && event.getBlock() == Blocks.CACTUS || berryBushConfig.get()
            && event.getBlock() == Blocks.SWEET_BERRY_BUSH || unloadedConfig.get()
            && !BlockUtil.isBlockLoaded(pos.getX(), pos.getZ())) {
            event.cancel();
            event.setVoxelShape(VoxelShapes.fullCube());
        }
    }

    @EventHandler
    public void onClimb(PlayerClimbEvent event) {
        if (noClimbConfig.get()) {
            event.cancel();
        }
    }

    public static Avoid getInstance() {
        return INST;
    }
}
