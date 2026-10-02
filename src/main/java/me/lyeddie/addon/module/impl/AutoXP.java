package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.irrevocable.PlayerTickEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.RotationModule;
import me.lyeddie.addon.util.literal.InventoryUtil;
import me.lyeddie.addon.managers.impl.util.TickTimer;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ExperienceBottleItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.util.Hand;

public class AutoXP extends RotationModule {
    private static AutoXP INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> multiTaskConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("MultiTask").description("Allows you to throw xp while using items")
        .defaultValue(false)
        .build());
    public final Setting<Double> delayConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Delay").description("Delay to throw xp in ticks")
        .defaultValue(1.0)
        .min(1.0)
        .sliderMax(10.0)
        .build());
    public final Setting<Integer> shiftTicksConfig = sgGeneral.add(new IntSetting.Builder()
        .name("ShiftTicks").description("The number of xp bottles to throw in one tick")
        .defaultValue(1)
        .min(1)
        .sliderMax(64)
        .build());
    private final Setting<Boolean> durabilityCheckConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("DurabilityCheck").description("Check if your armor and held item durability is full then disables if it is")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> rotateConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate").description("Rotates the player while throwing xp")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> swingConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Swing").description("Swings hand while throwing xp")
        .defaultValue(false)
        .build());

    private final TickTimer delayTimer = new TickTimer();

    public AutoXP() {
        super(Shoreline.MAIN, "AutoXP", "Automatically throws xp silently.", 850);
        INST = this;
    }

    @Override
    public String getInfoString() {
        return String.valueOf(InventoryUtil.count(Items.EXPERIENCE_BOTTLE));
    }

    @EventHandler
    public void onPlayerTick(PlayerTickEvent event) {
        if (mc.player == null || !delayTimer.passed(delayConfig.get())) {
            return;
        }

        if (mc.player.isUsingItem() && !multiTaskConfig.get()) {
            return;
        }

        if (durabilityCheckConfig.get() && areItemsFullDura(mc.player)) {
            toggle();
            return;
        }

        int slot = -1;
        for (int i = 0; i < 9; i++) {
            ItemStack stack = mc.player.getInventory().getStack(i);
            if (stack.getItem() instanceof ExperienceBottleItem) {
                slot = i;
                break;
            }
        }
        if (slot == -1) {
            toggle();
            return;
        }

        Managers.INVENTORY.setSlot(slot);
        if (rotateConfig.get()) {
            setRotation(mc.player.getYaw(), 90.0f);
            if (isRotationBlocked()) {
                return;
            }
        }
        for (int i = 0; i < shiftTicksConfig.get(); i++) {
            Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id));
            if (swingConfig.get()) {
                mc.player.swingHand(Hand.MAIN_HAND);
            }
        }
        Managers.INVENTORY.syncToClient();
        delayTimer.reset();
    }

    private boolean areItemsFullDura(PlayerEntity player) {
        if (!isItemFullDura(player.getMainHandStack()) || !isItemFullDura(player.getOffHandStack())) {
            return false;
        }

        for (ItemStack stack : player.getArmorItems()) {
            if (!isItemFullDura(stack)) {
                return false;
            }
        }

        return true;
    }

    private boolean isItemFullDura(ItemStack stack) {
        if (stack.isEmpty()) {
            return true;
        }
        int maxDura = stack.getMaxDamage();
        int currentDura = stack.getDamage();
        return currentDura == 0 || maxDura == 0;
    }

    public static AutoXP getInstance() {
        return INST;
    }
}
