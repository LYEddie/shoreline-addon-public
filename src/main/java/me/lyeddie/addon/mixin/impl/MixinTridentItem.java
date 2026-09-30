package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.events.TridentWaterEvent;
import me.lyeddie.addon.util.Globals;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.core.Holder;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TridentItem;
import net.minecraft.world.item.enchantment.EnchantmentEffectComponents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(TridentItem.class)
public abstract class MixinTridentItem implements Globals {

    @Inject(method = "use", at = @At(value = "HEAD"), cancellable = true)
    private void hookUse(Level world, Player user, InteractionHand hand, CallbackInfoReturnable<InteractionResult> cir) {
        TridentWaterEvent tridentWaterEvent = new TridentWaterEvent();
        MeteorClient.EVENT_BUS.post(tridentWaterEvent);
        if (tridentWaterEvent.isCancelled()) {
            cir.cancel();
            ItemStack itemStack = user.getItemInHand(hand);
            if (itemStack.getDamageValue() >= itemStack.getMaxDamage() - 1) {
                cir.setReturnValue(InteractionResult.FAIL);
                return;
            }
            user.startUsingItem(hand);
            cir.setReturnValue(InteractionResult.CONSUME);
        }
    }

    @Inject(method = "releaseUsing", at = @At(value = "HEAD"), cancellable = true)
    private void hookOnStoppedUsing(ItemStack stack, Level world, LivingEntity user, int remainingUseTicks, CallbackInfoReturnable<Boolean> cir) {
        if (!(user instanceof Player playerEntity)) {
            return;
        }
        int var6 = stack.getUseDuration(user) - remainingUseTicks;
        TridentWaterEvent tridentWaterEvent = new TridentWaterEvent();
        MeteorClient.EVENT_BUS.post(tridentWaterEvent);
        if (tridentWaterEvent.isCancelled()) {
            boolean used = false;
            if (var6 >= 10) {
                float f = EnchantmentHelper.getTridentSpinAttackStrength(stack, playerEntity);
                if (!(f > 0.0F) || playerEntity.isInWaterOrRain()) {
                    if (!stack.nextDamageWillBreak()) {
                        used = true;
                        Holder<SoundEvent> registryEntry = EnchantmentHelper.pickHighestLevel(stack, EnchantmentEffectComponents.TRIDENT_SOUND).orElse(SoundEvents.TRIDENT_THROW);
                        if (!world.isClientSide()) {
                            stack.hurtAndBreak(1, playerEntity, user.getUsedItemHand().asEquipmentSlot());
                            if (f == 0.0F) {
                                ThrownTrident tridentEntity = new ThrownTrident(world, playerEntity, stack);
                                tridentEntity.shootFromRotation(playerEntity, playerEntity.getXRot(), playerEntity.getYRot(), 0.0F, 2.5F, 1.0F);
                                if (playerEntity.hasInfiniteMaterials()) {
                                    tridentEntity.pickup = AbstractArrow.Pickup.CREATIVE_ONLY;
                                }

                                world.addFreshEntity(tridentEntity);
                                world.playSound((Player) null, tridentEntity, (SoundEvent) registryEntry.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                                if (!playerEntity.hasInfiniteMaterials()) {
                                    playerEntity.getInventory().removeItem(stack);
                                }
                            }
                        }

                        playerEntity.awardStat(Stats.ITEM_USED.get((TridentItem) (Object) this));
                        if (f > 0.0F) {
                            float g = playerEntity.getYRot();
                            float h = playerEntity.getXRot();
                            float j = -Mth.sin(g * 0.017453292F) * Mth.cos(h * 0.017453292F);
                            float k = -Mth.sin(h * 0.017453292F);
                            float l = Mth.cos(g * 0.017453292F) * Mth.cos(h * 0.017453292F);
                            float m = Mth.sqrt(j * j + k * k + l * l);
                            j *= f / m;
                            k *= f / m;
                            l *= f / m;
                            playerEntity.push((double) j, (double) k, (double) l);
                            playerEntity.startAutoSpinAttack(20, 8.0F, stack);
                            if (playerEntity.onGround()) {
                                float n = 1.1999999F;
                                playerEntity.move(MoverType.SELF, new Vec3(0.0, 1.1999999284744263, 0.0));
                            }

                            world.playSound((Player) null, playerEntity, (SoundEvent) registryEntry.value(), SoundSource.PLAYERS, 1.0F, 1.0F);
                        }
                    }
                }
            }
            cir.setReturnValue(used);
        }
    }
}
