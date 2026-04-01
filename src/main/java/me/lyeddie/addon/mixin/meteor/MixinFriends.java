package me.lyeddie.addon.mixin.meteor;

import me.lyeddie.addon.events.FriendAddedEvent;
import me.lyeddie.addon.events.FriendRemovedEvent;
import meteordevelopment.meteorclient.MeteorClient;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(value = Friends.class, remap = false)
public class MixinFriends {

    @Inject(method = "add", at = @At("RETURN"), cancellable = true)
    private void onAdd(Friend friend, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) MeteorClient.EVENT_BUS.post(new FriendAddedEvent(friend));
    }

    @Inject(method = "remove", at = @At(value = "RETURN", ordinal = 0), cancellable = true)
    private void onRemove(Friend friend, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) MeteorClient.EVENT_BUS.post(new FriendRemovedEvent(friend));
    }
}

