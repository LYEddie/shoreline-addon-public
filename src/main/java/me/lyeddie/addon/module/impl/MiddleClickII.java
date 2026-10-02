package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.AddonModule;
import me.lyeddie.addon.util.literal.RayCastUtil;
import meteordevelopment.meteorclient.events.meteor.MouseClickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.friends.Friend;
import meteordevelopment.meteorclient.systems.friends.Friends;
import meteordevelopment.meteorclient.utils.misc.input.KeyAction;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;

public class MiddleClickII extends AddonModule {
    private static MiddleClickII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> friendConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Friend").description("Friends players when middle click")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> pearlConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Pearl").description("Throws a pearl when middle click")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> fireworkConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Firework").description("Uses firework to boost elytra when middle click")
        .defaultValue(false)
        .build());

    public MiddleClickII() {
        super(Shoreline.MAIN, "MiddleClickII", "Adds an additional bind on the mouse middle button");
        INST = this;
    }

    @EventHandler
    public void onMouseClick(MouseClickEvent event) {
        if (mc.player == null || mc.interactionManager == null) {
            return;
        }
        if (event.action == KeyAction.Press && event.button() == 2 && this.mc.currentScreen == null) {
            double d = mc.player.getEntityInteractionRange();
            HitResult result = RayCastUtil.raycastEntity(d);
            if (result != null && result.getType() == HitResult.Type.ENTITY && friendConfig.get() && ((EntityHitResult) result).getEntity() instanceof PlayerEntity target) {
                Friend playerObj = Friends.get().get(target.getName().getString());
                if (Friends.get().isFriend(target)) {
                    Friends.get().remove(playerObj);
                } else {
                    Friends.get().add(playerObj) ;
                }
            } else {
                Item item = null;
                if (mc.player.isGliding() && fireworkConfig.get()) {
                    item = Items.FIREWORK_ROCKET;
                } else if (pearlConfig.get()) {
                    item = Items.ENDER_PEARL;
                }
                if (item == null) {
                    return;
                }
                int slot = -1;
                for (int i = 0; i < 45; i++) {
                    ItemStack stack = mc.player.getInventory().getStack(i);
                    if (stack.getItem() == item) {
                        slot = i;
                        break;
                    }
                }

                if (slot == -1) {
                    return;
                }

                if (slot < 9) {
                    Managers.INVENTORY.setSlot(slot);
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    Managers.INVENTORY.syncToClient();
                } else {
                    mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(0, mc.player.getInventory().getSelectedSlot() + 36, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.interactItem(mc.player, Hand.MAIN_HAND);
                    mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(0, mc.player.getInventory().getSelectedSlot() + 36, 0, SlotActionType.PICKUP, mc.player);
                    mc.interactionManager.clickSlot(0, slot, 0, SlotActionType.PICKUP, mc.player);
                }
            }
        }
    }

    public static MiddleClickII getInstance() {
        return INST;
    }
}
