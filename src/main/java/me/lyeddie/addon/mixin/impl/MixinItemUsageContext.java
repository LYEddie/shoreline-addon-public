package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.ItemDesyncEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemUsageContext.class)
public class MixinItemUsageContext implements Globals {

    @Inject(method = "getStack", at = @At("RETURN"), cancellable = true)
    public void hookGetStack(final CallbackInfoReturnable<ItemStack> info) {
        ItemDesyncEvent itemDesyncEvent = new ItemDesyncEvent();
        MeteorClient.EVENT_BUS.post(itemDesyncEvent);
        if (mc.player != null && info.getReturnValue().equals(mc.player.getMainHandStack()) && itemDesyncEvent.isCancelled()) {
            info.setReturnValue(itemDesyncEvent.getServerItem());
        }
    }
}
