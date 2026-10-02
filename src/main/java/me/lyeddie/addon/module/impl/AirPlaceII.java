package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.ItemUseEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.mixin.impl.accessor.AccessorMinecraftClient;
import me.lyeddie.addon.module.AddonModule;
import me.lyeddie.addon.util.tabs.TabConfigs;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class AirPlaceII extends AddonModule {
    private static AirPlaceII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> manualConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Click").description("Allow manual air place")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> grimConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Grim").description("Place on air on grim")
        .defaultValue(false)
        .build());
    public final Setting<Double> rangeConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Range").description("The range to air place")
        .defaultValue(4.0)
        .min(1.0)
        .sliderMax(6.0)
        .build());
    private final Setting<Boolean> fluidsConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Fluids").description("Place against fluids")
        .defaultValue(false)
        .build());

    private int airPlaceTicks;

    public AirPlaceII() {
        super(Shoreline.MAIN, "AirPlaceII", "Allows you to place blocks in the air");
        INST = this;
    }

    @Override
    public void onActivate() {
        airPlaceTicks = 0;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (airPlaceTicks > 0) {
            airPlaceTicks--;
        }

        if (mc.player == null || mc.gameMode == null || !manualConfig.get()) {
            return;
        }

        if (mc.hitResult instanceof BlockHitResult result && !mc.level.isEmptyBlock(result.getBlockPos())) {
            return;
        }

        final ItemStack stack = mc.player.getMainHandItem();
        if ((stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) || !mc.options.keyUse.isDown()) {
            return;
        }
        final HitResult result = mc.player.pick(rangeConfig.get(), 1.0f, fluidsConfig.get());
        if (((AccessorMinecraftClient) mc).hookGetItemUseCooldown() == 0 && airPlaceTicks == 0 && !mc.player.isUsingItem()
            && result instanceof BlockHitResult blockHitResult) {
            final BlockPos blockPos = BlockPos.containing(blockHitResult.getLocation());
            if (!mc.level.isEmptyBlock(blockPos) || isEntityInBlockPos(blockPos)) {
                return;
            }
            ((AccessorMinecraftClient) mc).hookSetItemUseCooldown(4);
            airPlaceTicks = 4;
            if (grimConfig.get()) {
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
                mc.gameMode.useItemOn(mc.player, InteractionHand.OFF_HAND, blockHitResult);
                mc.player.swing(InteractionHand.MAIN_HAND, false);
                Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.OFF_HAND));
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.SWAP_ITEM_WITH_OFFHAND, BlockPos.ZERO, Direction.DOWN));
            } else {
                mc.gameMode.useItemOn(mc.player, InteractionHand.MAIN_HAND, blockHitResult);
                mc.player.swing(InteractionHand.MAIN_HAND);
            }
        }
    }

    @EventHandler
    public void onItemUse(ItemUseEvent event) {
        if (airPlaceTicks > 0 && manualConfig.get()) {
            event.cancel();
        }
    }

    @EventHandler
    public void onRender(Render3DEvent event) {
        if (mc.player == null || !manualConfig.get()) {
            return;
        }

        if (mc.hitResult instanceof BlockHitResult result && !mc.level.isEmptyBlock(result.getBlockPos())) {
            return;
        }

        final ItemStack stack = mc.player.getMainHandItem();
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem)) {
            return;
        }
        final HitResult result = mc.player.pick(rangeConfig.get(), 1.0f, fluidsConfig.get());
        if (!(result instanceof BlockHitResult blockHitResult)) {
            return;
        }
        final BlockPos blockPos = BlockPos.containing(blockHitResult.getLocation());
        if (!mc.level.isEmptyBlock(blockPos) || isEntityInBlockPos(blockPos)) {
            return;
        }
        event.renderer.box(blockPos, TabConfigs.get().getClampColor(64), TabConfigs.get().getClampColor(145), ShapeMode.Both, 0);
    }

    private boolean isEntityInBlockPos(final BlockPos blockPos) {
        for (Entity entity : mc.level.entitiesForRendering()) {
            if (entity.getBoundingBox().intersects(new AABB(blockPos))) {
                return true;
            }
        }
        return false;
    }

    public static AirPlaceII getInstance() {
        return INST;
    }
}
