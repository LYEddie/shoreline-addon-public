package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.network.packet.s2c.play.PlaySoundFromEntityS2CPacket;
import net.minecraft.network.packet.s2c.play.PlaySoundS2CPacket;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;

import java.util.Set;

public class NoSoundLag extends AddonModule {
    private static NoSoundLag INST;

    private final static Set<SoundEvent> LAG_SOUNDS = Set.of(
        SoundEvents.ITEM_ARMOR_EQUIP_GENERIC,
        SoundEvents.ITEM_ARMOR_EQUIP_ELYTRA,
        SoundEvents.ITEM_ARMOR_EQUIP_NETHERITE,
        SoundEvents.ITEM_ARMOR_EQUIP_DIAMOND,
        SoundEvents.ITEM_ARMOR_EQUIP_IRON,
        SoundEvents.ITEM_ARMOR_EQUIP_GOLD,
        SoundEvents.ITEM_ARMOR_EQUIP_CHAIN,
        SoundEvents.ITEM_ARMOR_EQUIP_LEATHER
    );

    public NoSoundLag() {
        super(Shoreline.MAIN, "NoSoundLag", "Prevents sound effects from lagging the game");
        INST = this;
    }

    @EventHandler
    public void onPacketInbound(PacketEvent.Receive event) {
        if (event.packet instanceof PlaySoundFromEntityS2CPacket packet && LAG_SOUNDS.contains(packet.getSound())
            || event.packet instanceof PlaySoundS2CPacket packet2 && LAG_SOUNDS.contains(packet2.getSound())) {
            event.cancel();
        }
    }

    public static NoSoundLag getInstance() {
        return INST;
    }
}
