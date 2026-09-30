package me.lyeddie.addon.mixin.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.EntityDeathEvent;
import me.lyeddie.addon.events.ItemUseEvent;
import me.lyeddie.addon.events.irrevocable.RunTickEvent;
import me.lyeddie.addon.mixin.IMinecraftClient;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.Minecraft;
import net.minecraft.client.main.GameConfig;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;

@Mixin(Minecraft.class)
public abstract class MixinMinecraftClient implements IMinecraftClient {

    @Unique
    private final List<Integer> deadList = new ArrayList<>();
    @Shadow
    public ClientLevel level;
    @Shadow
    public LocalPlayer player;
    @Shadow
    @Nullable
    public MultiPlayerGameMode gameMode;
    @Unique
    private boolean leftClick;
    @Unique
    private boolean rightClick;
    @Unique
    private boolean doAttackCalled;
    @Unique
    private boolean doItemUseCalled;

    @Shadow
    protected abstract void startUseItem();

    @Shadow
    protected abstract boolean startAttack();

    @Override
    public void leftClick() {
        leftClick = true;
    }

    @Override
    public void rightClick() {
        rightClick = true;
    }

    @Inject(method = {"<init>"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/network/chat/contents/KeybindResolver;setKeyResolver(Ljava/util/function/Function;)V")})
    private void hookInit(GameConfig args, CallbackInfo info) {
        Shoreline.LOG.info("init mixin at " + getClass().getSimpleName());
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/Minecraft;runTick(Z)V", shift = At.Shift.BEFORE))
    private void hookRun(CallbackInfo ci) {
        final RunTickEvent runTickEvent = new RunTickEvent();
        MeteorClient.EVENT_BUS.post(runTickEvent);
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void hookTickPre(CallbackInfo ci) {
        doAttackCalled = false;
        doItemUseCalled = false;
        if (gameMode == null) {
            return;
        }
        if (leftClick && !doAttackCalled) {
            startAttack();
        }
        if (rightClick && !doItemUseCalled) {
            startUseItem();
        }
        leftClick = false;
        rightClick = false;
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void hookTickPost(CallbackInfo ci) {
        if (player != null && level != null) {
            for (Entity entity : level.entitiesForRendering()) {
                if (entity instanceof LivingEntity e) {
                    if (e.isDeadOrDying() && !deadList.contains(e.getId())) {
                        EntityDeathEvent entityDeathEvent = new EntityDeathEvent(e);
                        MeteorClient.EVENT_BUS.post(entityDeathEvent);
                        deadList.add(e.getId());
                    } else if (!e.isDeadOrDying()) {
                        deadList.remove((Integer) e.getId());
                    }
                }
            }
        }
    }

    @Inject(method = "startUseItem", at = @At(value = "HEAD"), cancellable = true)
    private void hookDoItemUse(CallbackInfo ci) {
        doItemUseCalled = true;
        ItemUseEvent itemUseEvent = new ItemUseEvent();
        MeteorClient.EVENT_BUS.post(itemUseEvent);
        if (itemUseEvent.isCancelled()) {
            ci.cancel();
        }
    }
}
