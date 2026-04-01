package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.EncodeYawEvent;
import me.lyeddie.addon.events.FireworkVelocityEvent;
import me.lyeddie.addon.events.TridentWaterEvent;
import me.lyeddie.addon.events.irrevocable.DisconnectEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.mixin.accessor.AccessorFireworkRocketEntity;
import me.lyeddie.addon.module.AddonModule;
import me.lyeddie.addon.util.CacheTimer;
import me.lyeddie.addon.util.EnumFormatter;
import me.lyeddie.addon.util.Timer;
import me.lyeddie.addon.util.literal.EnchantmentUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.enchantment.Enchantments;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.projectile.FireworkRocketEntity;
import net.minecraft.item.*;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;

public class Disabler extends AddonModule {
    private static Disabler INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    public final Setting<Mode> modeConfig = sgGeneral.add(new EnumSetting.Builder<Mode>()
        .name("Mode").description("The mode for disabling anticheat checks")
        .defaultValue(Mode.GRIM_TRIDENT)
        .build());

    private final Timer fireworkTimer = new CacheTimer();

    public Disabler() {
        super(Shoreline.MAIN, "Disabler", "Disables anticheat checks");
        INST = this;
    }

    @Override
    public String getInfoString() {
        return isGrim() ? "Grim" : EnumFormatter.formatEnum(modeConfig.get());
    }

    @EventHandler
    public void onDisconnect(DisconnectEvent event) {
        toggle();
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (modeConfig.get() == Mode.GRIM_TRIDENT) {
            if (mc.player.isUsingItem()) {
                return;
            }

            int tridentSlot = -1;
            for (int i = 0; i < 9; ++i) {
                final ItemStack stack = mc.player.getInventory().getStack(i);
                if (!stack.isEmpty() && stack.getItem() instanceof TridentItem) {
                    if (EnchantmentUtil.getLevel(stack, Enchantments.RIPTIDE) > 0) {
                        tridentSlot = i;
                        break;
                    }
                }
            }

            if (tridentSlot == -1) {
                return;
            }

            Managers.INVENTORY.setSlot(tridentSlot);
            Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.RELEASE_USE_ITEM, BlockPos.ORIGIN, Direction.DOWN));
            Managers.INVENTORY.syncToClient();
        } else if (modeConfig.get() == Mode.GRIM_FIREWORK) {
            int elytraSlot = -1;
            int fireworkSlot = -1;
            for (int i = 0; i < 36; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() instanceof FireworkRocketItem && i < 9) {
                    fireworkSlot = i;
                }
                if (stack.getItem() instanceof ElytraItem) {
                    elytraSlot = i;
                }
            }
            if (!isBoostedByRocket() && !mc.player.isInFluid() && fireworkSlot != -1) {
                if (mc.player.isOnGround()) {
                    mc.player.jump();
                } else {
                    Managers.MOVEMENT.setMotionY(-0.05);
                }
                if (fireworkTimer.passed(1700) && !mc.player.isOnGround() && mc.player.getVelocity().y < 0.0) {
                    if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() != Items.ELYTRA && elytraSlot != -1) {
                        mc.interactionManager.clickSlot(0, elytraSlot < 9 ? elytraSlot + 36 : elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, 6, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, elytraSlot < 9 ? elytraSlot + 36 : elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                    }
                    Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.START_FALL_FLYING));
                    Managers.INVENTORY.setSlot(fireworkSlot);
                    Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
                    Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    Managers.INVENTORY.syncToClient();
                    fireworkTimer.reset();
                    if (mc.player.getEquippedStack(EquipmentSlot.CHEST).getItem() == Items.ELYTRA && elytraSlot != -1) {
                        mc.interactionManager.clickSlot(0, 6, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, elytraSlot < 9 ? elytraSlot + 36 : elytraSlot, 0, SlotActionType.PICKUP, mc.player);
                        mc.interactionManager.clickSlot(0, 6, 0, SlotActionType.PICKUP, mc.player);
                    }
                }
            }
        }
    }

    @EventHandler
    public void onTridentWaterCheck(TridentWaterEvent event) {
        if (modeConfig.get().equals(Mode.GRIM_TRIDENT)) {
            event.cancel();
        }
    }

    @EventHandler
    public void onFireworkVelocity(FireworkVelocityEvent event) {
        if (modeConfig.get() == Mode.GRIM_FIREWORK) {
            event.cancel();
        }
    }

    @EventHandler
    public void onEncodeYaw(EncodeYawEvent event) {
        if (isYawOverflow()) {
            event.cancel();
        }
    }

    public boolean isBoostedByRocket() {
        for (Entity entity : mc.world.getEntities()) {
            if (entity instanceof FireworkRocketEntity rocket
                && ((AccessorFireworkRocketEntity) rocket).hookWasShotByEntity()
                && ((AccessorFireworkRocketEntity) rocket).hookGetShooter() == mc.player) {
                return true;
            }
        }
        return false;
    }

    private boolean isGrim() {
        return modeConfig.get().name().toLowerCase().contains("grim");
    }

    public boolean isGrimFirework() {
        return this.isActive() && modeConfig.get() == Mode.GRIM_FIREWORK;
    }

    public boolean isYawOverflow() {
        return !mc.player.isFallFlying() && modeConfig.get() == Mode.GRIM_OVERFLOW;
    }

    public boolean grimFireworkCheck() {
        return isGrimFirework() && !isBoostedByRocket();
    }

    public boolean grimFireworkCheck2() {
        return isGrimFirework() && isBoostedByRocket();
    }

    public enum Mode {
        GRIM_TRIDENT,
        GRIM_FIREWORK,
        GRIM_OVERFLOW
    }

    public static Disabler getInstance() {
        return INST;
    }
}
