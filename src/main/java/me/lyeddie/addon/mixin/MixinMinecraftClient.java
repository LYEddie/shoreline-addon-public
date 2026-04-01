package me.lyeddie.addon.mixin;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.EntityDeathEvent;
import me.lyeddie.addon.events.ItemUseEvent;
import me.lyeddie.addon.events.irrevocable.RunTickEvent;
import me.lyeddie.addon.imixin.impl.IMinecraftClient;
import meteordevelopment.meteorclient.MeteorClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.RunArgs;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.network.ClientPlayerInteractionManager;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.entity.Entity;
import net.minecraft.entity.LivingEntity;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.ArrayList;
import java.util.List;

@Mixin(MinecraftClient.class)
public abstract class MixinMinecraftClient implements IMinecraftClient {

    @Unique
    private final List<Integer> deadList = new ArrayList<>();
    @Shadow
    public ClientWorld world;
    @Shadow
    public ClientPlayerEntity player;
    @Shadow
    @Nullable
    public ClientPlayerInteractionManager interactionManager;
    @Unique
    private boolean leftClick;
    @Unique
    private boolean rightClick;
    @Unique
    private boolean doAttackCalled;
    @Unique
    private boolean doItemUseCalled;

    @Shadow
    protected abstract void doItemUse();

    @Shadow
    protected abstract boolean doAttack();

    @Override
    public void leftClick() {
        leftClick = true;
    }

    @Override
    public void rightClick() {
        rightClick = true;
    }

    @Inject(method = {"<init>"}, at = {@At(value = "INVOKE", target = "Lnet/minecraft/text/KeybindTranslations;setFactory(Ljava/util/function/Function;)V")})
    private void hookInit(RunArgs args, CallbackInfo info) {
        Shoreline.LOG.info("init mixin at " + getClass().getSimpleName());
    }

    @Inject(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/client/MinecraftClient;render(Z)V", shift = At.Shift.BEFORE))
    private void hookRun(CallbackInfo ci) {
        final RunTickEvent runTickEvent = new RunTickEvent();
        MeteorClient.EVENT_BUS.post(runTickEvent);
    }

    @Inject(method = "tick", at = @At(value = "HEAD"))
    private void hookTickPre(CallbackInfo ci) {
        doAttackCalled = false;
        doItemUseCalled = false;
        if (interactionManager == null) {
            return;
        }
        if (leftClick && !doAttackCalled) {
            doAttack();
        }
        if (rightClick && !doItemUseCalled) {
            doItemUse();
        }
        leftClick = false;
        rightClick = false;
    }

    @Inject(method = "tick", at = @At(value = "TAIL"))
    private void hookTickPost(CallbackInfo ci) {
        if (player != null && world != null) {
            for (Entity entity : world.getEntities()) {
                if (entity instanceof LivingEntity e) {
                    if (e.isDead() && !deadList.contains(e.getId())) {
                        EntityDeathEvent entityDeathEvent = new EntityDeathEvent(e);
                        MeteorClient.EVENT_BUS.post(entityDeathEvent);
                        deadList.add(e.getId());
                    } else if (!e.isDead()) {
                        deadList.remove((Integer) e.getId());
                    }
                }
            }
        }
    }

    @Inject(method = "doItemUse", at = @At(value = "HEAD"), cancellable = true)
    private void hookDoItemUse(CallbackInfo ci) {
        doItemUseCalled = true;
        ItemUseEvent itemUseEvent = new ItemUseEvent();
        MeteorClient.EVENT_BUS.post(itemUseEvent);
        if (itemUseEvent.isCancelled()) {
            ci.cancel();
        }
    }
}
