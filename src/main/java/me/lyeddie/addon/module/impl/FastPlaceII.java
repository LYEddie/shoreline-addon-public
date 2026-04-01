package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.mixin.accessor.AccessorMinecraftClient;
import me.lyeddie.addon.util.CacheTimer;
import me.lyeddie.addon.util.SneakBlocks;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractBlockC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class FastPlaceII extends AddonModule {
    private static FastPlaceII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgSel = settings.createGroup("Selection");

    public final Setting<Selection> selectionConfig = sgSel.add(new EnumSetting.Builder<Selection>()
        .name("Selection").description("The selection of items to apply fast placements")
        .defaultValue(Selection.WHITELIST)
        .build());
    public final Setting<Integer> delayConfig = sgGeneral.add(new IntSetting.Builder()
        .name("Delay").description("Fast place click delay")
        .defaultValue(1)
        .min(0)
        .sliderMax(4)
        .build());
    public final Setting<Double> startDelayConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("StartDelay").description("Fast place start delay")
        .defaultValue(0.0)
        .min(0.0)
        .sliderMax(1.0)
        .build());
    private final Setting<Boolean> ghostFixConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("GhostFix").description("Fixes item ghosting issue on some servers")
        .defaultValue(false)
        .build());
    private final Setting<List<Item>> whitelistConfig = sgSel.add(new ItemListSetting.Builder()
        .name("Whitelist").description("Valid item whitelist")
        .defaultValue(Items.EXPERIENCE_BOTTLE, Items.SNOWBALL, Items.EGG)
        .visible(() -> selectionConfig.get() == Selection.WHITELIST)
        .build());
    private final Setting<List<Item>> blacklistConfig = sgSel.add(new ItemListSetting.Builder()
        .name("Blacklist").description("Valid item blacklist")
        .defaultValue(Items.ENDER_PEARL, Items.ENDER_EYE)
        .visible(() -> selectionConfig.get() == Selection.BLACKLIST)
        .build());

    private final CacheTimer startTimer = new CacheTimer();

    public FastPlaceII() {
        super(Shoreline.MAIN, "FastPlaceII", "Place items and blocks faster");
        INST = this;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (!mc.options.useKey.isPressed()) {
            startTimer.reset();
        } else if (startTimer.passed(startDelayConfig.get(), TimeUnit.SECONDS)
            && ((AccessorMinecraftClient) mc).hookGetItemUseCooldown() > delayConfig.get()
            && placeCheck(mc.player.getMainHandStack())) {
            if (ghostFixConfig.get()) {
                Managers.NETWORK.sendSequencedPacket(id ->
                    new PlayerInteractItemC2SPacket(mc.player.getActiveHand(), id, mc.player.getYaw(), mc.player.getPitch()));
            }
            ((AccessorMinecraftClient) mc).hookSetItemUseCooldown(delayConfig.get());
        }
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null) {
            return;
        }
        if (event.packet instanceof PlayerInteractBlockC2SPacket packet
            && ghostFixConfig.get() && !Managers.NETWORK.isCached(event.packet)
            && placeCheck(mc.player.getStackInHand(packet.getHand()))) {
            BlockState state = mc.world.getBlockState(packet.getBlockHitResult().getBlockPos());
            if (!SneakBlocks.isSneakBlock(state)) {
                event.cancel();
            }
        }
    }

    private boolean placeCheck(ItemStack held) {
        return switch (selectionConfig.get()) {
            case WHITELIST -> (whitelistConfig.get()).contains(held.getItem());
            case BLACKLIST -> !(blacklistConfig.get()).contains(held.getItem());
            case ALL -> true;
        };
    }

    public enum Selection {
        WHITELIST,
        BLACKLIST,
        ALL
    }

    public static FastPlaceII getInstance() {
        return INST;
    }
}
