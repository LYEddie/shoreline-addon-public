package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.ShulkerNestedEvent;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.world.inventory.ShulkerBoxSlot;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ShulkerBoxSlot.class)
public class MixinShulkerBoxSlot {

    @Inject(method = "mayPlace", at = @At(value = "HEAD"), cancellable = true)
    private void hookCanInsert(ItemStack stack, CallbackInfoReturnable<Boolean> cir) {
        ShulkerNestedEvent shulkerNestedEvent = new ShulkerNestedEvent();
        MeteorClient.EVENT_BUS.post(shulkerNestedEvent);
        if (shulkerNestedEvent.isCancelled()) {
            cir.cancel();
            cir.setReturnValue(true);
        }
    }
}
