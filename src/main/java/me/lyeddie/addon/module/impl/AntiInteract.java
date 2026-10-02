package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.InteractBlockEvent;
import me.lyeddie.addon.events.InteractBorderEvent;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ServerboundUseItemOnPacket;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;

public class AntiInteract extends AddonModule {
    private static AntiInteract INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<List<Block>> blacklistConfig = sgGeneral.add(new BlockListSetting.Builder()
        .name("Blacklist").description("Valid block blacklist")
        .build());
    private final Setting<Boolean> borderConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Border").description("Prevents interacting with the world border")
        .defaultValue(true)
        .build());

    public AntiInteract() {
        super(Shoreline.MAIN, "AntiInteract", "Prevents player from interacting with certain objects");
        INST = this;
    }

    @EventHandler
    public void onInteractBlock(InteractBlockEvent event) {
        BlockPos pos = event.getHitResult().getBlockPos();
        BlockState state = mc.level.getBlockState(pos);
        if (blacklistConfig.get().contains(state.getBlock())) {
            event.cancel();
        }
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (mc.player == null || mc.level == null) {
            return;
        }
        if (event.packet instanceof ServerboundUseItemOnPacket packet) {
            BlockPos pos = packet.getHitResult().getBlockPos();
            BlockState state = mc.level.getBlockState(pos);
            if (blacklistConfig.get().contains(state.getBlock())) {
                event.cancel();
            }
        }
    }

    @EventHandler
    public void onInteractBorder(InteractBorderEvent event) {
        if (!borderConfig.get() || mc.player.getMainHandItem().getItem() instanceof BlockItem) {
            return;
        }
        event.cancel();
    }

    public static AntiInteract getInstance() {
        return INST;
    }
}
