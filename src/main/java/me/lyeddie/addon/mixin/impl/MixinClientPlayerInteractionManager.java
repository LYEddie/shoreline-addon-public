package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.*;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.block.BlockState;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.network.SequencedPacketCreator;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.GameMode;
import net.minecraft.world.border.WorldBorder;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ClientPlayerInteractionManager.class)
public abstract class MixinClientPlayerInteractionManager implements Globals {

    @Shadow
    private GameMode gameMode;

    @Shadow
    protected abstract void syncSelectedSlot();

    @Shadow
    protected abstract void sendSequencedPacket(ClientWorld world, SequencedPacketCreator packetCreator);

    @Inject(method = "attackBlock", at = @At(value = "HEAD"), cancellable = true)
    private void hookAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = mc.world.getBlockState(pos);
        final AttackBlockEvent attackBlockEvent = new AttackBlockEvent(pos, state, direction);
        MeteorClient.EVENT_BUS.post(attackBlockEvent);
        if (attackBlockEvent.isCancelled()) {
            cir.cancel();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "interactBlock", at = @At(value = "HEAD"), cancellable = true)
    private void hookInteractBlock(ClientPlayerEntity player, Hand hand, BlockHitResult hitResult, CallbackInfoReturnable<ActionResult> cir) {
        InteractBlockEvent interactBlockEvent = new InteractBlockEvent(player, hand, hitResult);
        MeteorClient.EVENT_BUS.post(interactBlockEvent);
        if (interactBlockEvent.isCancelled()) {
            cir.setReturnValue(ActionResult.SUCCESS);
            cir.cancel();
        }
    }

    @Redirect(method = "interactBlock", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/border/WorldBorder;contains(Lnet/minecraft/util/math/BlockPos;)Z"))
    private boolean hookInteractBlock$2(WorldBorder worldBorder, BlockPos pos) {
        InteractBorderEvent interactBorderEvent = new InteractBorderEvent();
        MeteorClient.EVENT_BUS.post(interactBorderEvent);
        if (interactBorderEvent.isCancelled()) {
            return true;
        }
        return worldBorder.contains(pos);
    }

    @Inject(method = "interactItem", at = @At(value = "HEAD"), cancellable = true)
    public void hookInteractItem(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        StrafeFixEvent strafeFixEvent = new StrafeFixEvent();
        MeteorClient.EVENT_BUS.post(strafeFixEvent);
        if (strafeFixEvent.isCancelled()) {
            cir.cancel();
            if (this.gameMode == GameMode.SPECTATOR) {
                cir.setReturnValue(ActionResult.PASS);
                return;
            }
            syncSelectedSlot();
            MutableObject<ActionResult> mutableObject = new MutableObject<>();
            this.sendSequencedPacket(mc.world, (sequence) -> {
                PlayerInteractItemC2SPacket playerInteractItemC2SPacket = new PlayerInteractItemC2SPacket(
                    hand, sequence, Managers.ROTATION.isRotating() ? Managers.ROTATION.getRotationYaw() : player.getYaw(),
                    Managers.ROTATION.isRotating() ? Managers.ROTATION.getRotationPitch() : player.getPitch());
                ItemStack itemStack = player.getStackInHand(hand);
                if (player.getItemCooldownManager().isCoolingDown(itemStack.getItem())) {
                    mutableObject.setValue(ActionResult.PASS);
                    return playerInteractItemC2SPacket;
                } else {
                    TypedActionResult<ItemStack> typedActionResult = itemStack.use(mc.world, player, hand);
                    ItemStack itemStack2 = typedActionResult.getValue();
                    if (itemStack2 != itemStack) {
                        player.setStackInHand(hand, itemStack2);
                    }

                    mutableObject.setValue(typedActionResult.getResult());
                    return playerInteractItemC2SPacket;
                }
            });
            cir.setReturnValue(mutableObject.getValue());
        }
    }

    @Redirect(method = "interactBlockInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;shouldCancelInteraction()Z"))
    private boolean hookRedirectInteractBlockInternal$shouldCancelInteraction(ClientPlayerEntity player) {
        PacketSneakingEvent packetSneakingEvent = new PacketSneakingEvent();
        MeteorClient.EVENT_BUS.post(packetSneakingEvent);
        return player.isSneaking() || packetSneakingEvent.isCancelled();
    }

    @Redirect(method = "interactBlockInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/network/ClientPlayerEntity;getStackInHand(Lnet/minecraft/util/Hand;)Lnet/minecraft/item/ItemStack;"))
    private ItemStack hookRedirectInteractBlockInternal$getStackInHand(ClientPlayerEntity entity, Hand hand) {
        if (hand.equals(Hand.OFF_HAND)) {
            return entity.getStackInHand(hand);
        }
        ItemDesyncEvent itemDesyncEvent = new ItemDesyncEvent();
        MeteorClient.EVENT_BUS.post(itemDesyncEvent);
        return itemDesyncEvent.isCancelled() ? itemDesyncEvent.getServerItem() : entity.getStackInHand(Hand.MAIN_HAND);
    }

    @Redirect(method = "interactBlockInternal", at = @At(value = "INVOKE", target = "Lnet/minecraft/item/ItemStack;isEmpty()Z", ordinal = 0))
    private boolean hookRedirectInteractBlockInternal$getMainHandStack(ItemStack instance) {
        ItemDesyncEvent itemDesyncEvent = new ItemDesyncEvent();
        MeteorClient.EVENT_BUS.post(itemDesyncEvent);
        return itemDesyncEvent.isCancelled() ? itemDesyncEvent.getServerItem().isEmpty() : instance.isEmpty();
    }
}
