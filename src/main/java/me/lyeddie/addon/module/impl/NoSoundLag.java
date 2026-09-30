package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.protocol.game.ClientboundSoundEntityPacket;
import net.minecraft.network.protocol.game.ClientboundSoundPacket;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import java.util.Set;

public class NoSoundLag extends AddonModule {
    private static NoSoundLag INST;

    private final static Set<SoundEvent> LAG_SOUNDS = Set.of(
        SoundEvents.ARMOR_EQUIP_GENERIC.value(),
        SoundEvents.ARMOR_EQUIP_ELYTRA.value(),
        SoundEvents.ARMOR_EQUIP_NETHERITE.value(),
        SoundEvents.ARMOR_EQUIP_DIAMOND.value(),
        SoundEvents.ARMOR_EQUIP_IRON.value(),
        SoundEvents.ARMOR_EQUIP_GOLD.value(),
        SoundEvents.ARMOR_EQUIP_CHAIN.value(),
        SoundEvents.ARMOR_EQUIP_LEATHER.value()
    );

    public NoSoundLag() {
        super(Shoreline.MAIN, "NoSoundLag", "Prevents sound effects from lagging the game");
        INST = this;
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (event.packet instanceof ClientboundSoundEntityPacket packet && LAG_SOUNDS.contains(packet.getSound().value())
            || event.packet instanceof ClientboundSoundPacket packet2 && LAG_SOUNDS.contains(packet2.getSound().value())) {
            event.cancel();
        }
    }

    public static NoSoundLag getInstance() {
        return INST;
    }
}
