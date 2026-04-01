package me.lyeddie.addon.imixin.impl;

import me.lyeddie.addon.imixin.IMixin;

@IMixin
public interface IClientPlayerEntity {

    float getLastSpoofedYaw();

    float getLastSpoofedPitch();

}
