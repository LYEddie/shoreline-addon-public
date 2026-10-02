package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.*;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.multiplayer.prediction.PredictiveAction;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.border.WorldBorder;
import net.minecraft.world.phys.BlockHitResult;
import org.apache.commons.lang3.mutable.MutableObject;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(MultiPlayerGameMode.class)
public abstract class MixinClientPlayerInteractionManager implements Globals {

    @Shadow
    private GameType localPlayerMode;

    @Shadow
    protected abstract void ensureHasSentCarriedItem();

    @Shadow
    protected abstract void startPrediction(ClientLevel world, PredictiveAction packetCreator);

    @Inject(method = "startDestroyBlock", at = @At(value = "HEAD"), cancellable = true)
    private void hookAttackBlock(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        BlockState state = mc.level.getBlockState(pos);
        final AttackBlockEvent attackBlockEvent = new AttackBlockEvent(pos, state, direction);
        MeteorClient.EVENT_BUS.post(attackBlockEvent);
        if (attackBlockEvent.isCancelled()) {
            cir.cancel();
            cir.setReturnValue(false);
        }
    }

    @Inject(method = "useItemOn", at = @At(value = "HEAD"), cancellable = true)
    private void hookInteractBlock(LocalPlayer player, InteractionHand hand, BlockHitResult hitResult, CallbackInfoReturnable<InteractionResult> cir) {
        InteractBlockEvent interactBlockEvent = new InteractBlockEvent(player, hand, hitResult);
        MeteorClient.EVENT_BUS.post(interactBlockEvent);
        if (interactBlockEvent.isCancelled()) {
            cir.setReturnValue(InteractionResult.SUCCESS);
            cir.cancel();
        }
    }

    @Redirect(method = "useItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/border/WorldBorder;isWithinBounds(Lnet/minecraft/core/BlockPos;)Z"))
    private boolean hookInteractBlock$2(WorldBorder worldBorder, BlockPos pos) {
        InteractBorderEvent interactBorderEvent = new InteractBorderEvent();
        MeteorClient.EVENT_BUS.post(interactBorderEvent);
        if (interactBorderEvent.isCancelled()) {
            return true;
        }
        return worldBorder.isWithinBounds(pos);
    }

    @Inject(method = "useItem", at = @At(value = "HEAD"), cancellable = true)
    public void hookInteractItem(Player player, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        StrafeFixEvent strafeFixEvent = new StrafeFixEvent();
        MeteorClient.EVENT_BUS.post(strafeFixEvent);
        if (strafeFixEvent.isCancelled()) {
            cir.cancel();
            if (this.localPlayerMode == GameType.SPECTATOR) {
                cir.setReturnValue(InteractionResult.PASS);
                return;
            }
            ensureHasSentCarriedItem();
            MutableObject<InteractionResult> mutableObject = new MutableObject<>();
            this.startPrediction(mc.level, (sequence) -> {
                ServerboundUseItemPacket playerInteractItemC2SPacket = new ServerboundUseItemPacket(
                    hand, sequence, Managers.ROTATION.isRotating() ? Managers.ROTATION.getRotationYaw() : player.getYRot(),
                    Managers.ROTATION.isRotating() ? Managers.ROTATION.getRotationPitch() : player.getXRot());
                ItemStack itemStack = player.getItemInHand(hand);
                if (player.getCooldowns().isOnCooldown(itemStack)) {
                    mutableObject.setValue(InteractionResult.PASS);
                    return playerInteractItemC2SPacket;
                } else {
                    InteractionResult actionResult = itemStack.use(mc.level, player, hand);
                    if (actionResult instanceof InteractionResult.Success success) {
                        ItemStack newStack = success.heldItemTransformedTo();
                        if (newStack != null) {
                            player.setItemInHand(hand, newStack);
                        }
                    }

                    mutableObject.setValue(actionResult);
                    return playerInteractItemC2SPacket;
                }
            });
            cir.setReturnValue(mutableObject.getValue());
        }
    }

    @Redirect(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;isSecondaryUseActive()Z"))
    private boolean hookRedirectInteractBlockInternal$shouldCancelInteraction(LocalPlayer player) {
        PacketSneakingEvent packetSneakingEvent = new PacketSneakingEvent();
        MeteorClient.EVENT_BUS.post(packetSneakingEvent);
        return player.isShiftKeyDown() || packetSneakingEvent.isCancelled();
    }

    @Redirect(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/player/LocalPlayer;getItemInHand(Lnet/minecraft/world/InteractionHand;)Lnet/minecraft/world/item/ItemStack;"))
    private ItemStack hookRedirectInteractBlockInternal$getStackInHand(LocalPlayer entity, InteractionHand hand) {
        if (hand.equals(InteractionHand.OFF_HAND)) {
            return entity.getItemInHand(hand);
        }
        ItemDesyncEvent itemDesyncEvent = new ItemDesyncEvent();
        MeteorClient.EVENT_BUS.post(itemDesyncEvent);
        return itemDesyncEvent.isCancelled() ? itemDesyncEvent.getServerItem() : entity.getItemInHand(InteractionHand.MAIN_HAND);
    }

    @Redirect(method = "performUseItemOn", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/item/ItemStack;isEmpty()Z", ordinal = 0))
    private boolean hookRedirectInteractBlockInternal$getMainHandStack(ItemStack instance) {
        ItemDesyncEvent itemDesyncEvent = new ItemDesyncEvent();
        MeteorClient.EVENT_BUS.post(itemDesyncEvent);
        return itemDesyncEvent.isCancelled() ? itemDesyncEvent.getServerItem().isEmpty() : instance.isEmpty();
    }
}
