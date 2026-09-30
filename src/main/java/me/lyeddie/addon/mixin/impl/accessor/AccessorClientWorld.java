package me.lyeddie.addon.mixin.impl.accessor;

import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.prediction.BlockStatePredictionHandler;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(ClientLevel.class)
public interface AccessorClientWorld {

    @Invoker("playSound")
    void hookPlaySound(double x, double y, double z, SoundEvent event, SoundSource category, float volume, float pitch, boolean useDistance, long seed);

    @Invoker("getBlockStatePredictionHandler")
    BlockStatePredictionHandler hookGetPendingUpdateManager();
}
