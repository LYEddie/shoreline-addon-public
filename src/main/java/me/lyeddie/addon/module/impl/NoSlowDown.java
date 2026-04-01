package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.*;
import me.lyeddie.addon.events.irrevocable.MovementSlowdownEvent;
import me.lyeddie.addon.events.irrevocable.SetCurrentHandEvent;
import me.lyeddie.addon.events.staged.PrePlayerUpdateEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.mixin.accessor.AccessorKeyBinding;
import me.lyeddie.addon.util.literal.PositionUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.*;
import net.minecraft.client.gui.screen.ChatScreen;
import net.minecraft.client.gui.screen.DeathScreen;
import net.minecraft.client.gui.screen.ingame.SignEditScreen;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.*;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.MathHelper;
import org.lwjgl.glfw.GLFW;

import java.util.ArrayList;
import java.util.List;

public class NoSlowDown extends AddonModule {
    private static NoSlowDown INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgChecks = settings.createGroup("Checks");

    private final Setting<Boolean> strictConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Strict")
        .description("Strict NCP bypass for ground slowdowns")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> airStrictConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("AirStrict")
        .description("Strict NCP bypass for air slowdowns")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> grimConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Grim")
        .description("Strict Grim bypass for slowdown")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> grimNewConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("GrimV3")
        .description("Strict GrimV3 bypass for slowdown")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> strafeFixConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("StrafeFix")
        .description("Old NCP bypass for strafe")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> inventoryMoveConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("InventoryMove")
        .description("Allows the player to move while in inventories or screens")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> arrowMoveConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("ArrowMove")
        .description("Allows the player to look while in inventories or screens by using the arrow keys")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> itemsConfig = sgChecks.add(new BoolSetting.Builder()
        .name("Items")
        .description("Removes the slowdown effect caused by using items")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> sneakConfig = sgChecks.add(new BoolSetting.Builder()
        .name("Sneak")
        .description("Removes sneak slowdown")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> crawlConfig = sgChecks.add(new BoolSetting.Builder()
        .name("Crawl")
        .description("Removes crawl slowdown")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> shieldsConfig = sgChecks.add(new BoolSetting.Builder()
        .name("Shields")
        .description("Removes the slowdown effect caused by shields")
        .defaultValue(true)
        .build());
    private final Setting<Boolean> websConfig = sgChecks.add(new BoolSetting.Builder()
        .name("Webs")
        .description("Removes the slowdown caused when moving through webs")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> berryBushConfig = sgChecks.add(new BoolSetting.Builder()
        .name("BerryBush")
        .description("Removes the slowdown caused when moving through webs")
        .defaultValue(false)
        .build());
    public final Setting<Double> webSpeedConfig = sgChecks.add(new DoubleSetting.Builder()
        .name("WebMultiplier")
        .description("Speed to fall through webs")
        .defaultValue(1.0)
        .min(0.0)
        .sliderMax(1.0)
        .visible(() -> websConfig.get() || berryBushConfig.get())
        .build());
    private final Setting<Boolean> soulsandConfig = sgChecks.add(new BoolSetting.Builder()
        .name("SoulSand")
        .description("Removes the slowdown effect caused by walking over SoulSand blocks")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> honeyblockConfig = sgChecks.add(new BoolSetting.Builder()
        .name("HoneyBlock")
        .description("Removes the slowdown effect caused by walking over Honey blocks")
        .defaultValue(false)
        .build());
    private final Setting<Boolean> slimeblockConfig = sgChecks.add(new BoolSetting.Builder()
        .name("SlimeBlock")
        .description("Removes the slowdown effect caused by walking over Slime blocks")
        .defaultValue(false)
        .build());

    private boolean sneaking;

    public NoSlowDown() {
        super(Shoreline.MAIN, "NoSlowDown", "Prevents items from slowing down player");
        INST = this;
    }

    @Override
    public void onDeactivate() {
        if (airStrictConfig.get() && sneaking) {
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
        sneaking = false;
        Managers.TICK.setClientTick(1.0f);
    }

    @EventHandler
    public void onPlayerUpdate(PrePlayerUpdateEvent event) {
        if (grimConfig.get() && mc.player.isUsingItem() && !mc.player.isSneaking() && itemsConfig.get()) {
            if (mc.player.getActiveHand() == Hand.OFF_HAND && checkStack(mc.player.getMainHandStack())) {
                Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            } else if (checkStack(mc.player.getOffHandStack())) {
                Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.OFF_HAND, id, mc.player.getYaw(), mc.player.getPitch()));
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (airStrictConfig.get() && !mc.player.isUsingItem()) {
            sneaking = false;
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player,
                ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
        }
        if (strafeFixConfig.get() && checkSlowed()) {
        }
        if (inventoryMoveConfig.get() && checkScreen()) {
            final long handle = mc.getWindow().getHandle();
            KeyBinding[] keys = new KeyBinding[]{mc.options.jumpKey, mc.options.forwardKey, mc.options.backKey, mc.options.rightKey, mc.options.leftKey};
            for (KeyBinding binding : keys) {
                binding.setPressed(InputUtil.isKeyPressed(handle, ((AccessorKeyBinding) binding).getBoundKey().getCode()));
            }
            if (arrowMoveConfig.get()) {
                float yaw = mc.player.getYaw();
                float pitch = mc.player.getPitch();
                if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_UP)) {
                    pitch -= 3.0f;
                } else if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_DOWN)) {
                    pitch += 3.0f;
                } else if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_LEFT)) {
                    yaw -= 3.0f;
                } else if (InputUtil.isKeyPressed(handle, GLFW.GLFW_KEY_RIGHT)) {
                    yaw += 3.0f;
                }
                mc.player.setYaw(yaw);
                mc.player.setPitch(MathHelper.clamp(pitch, -90.0f, 90.0f));
            }
        }

        if ((grimConfig.get() || grimNewConfig.get()) && websConfig.get()) {
            Box bb = grimConfig.get() ? mc.player.getBoundingBox().expand(1.0) : mc.player.getBoundingBox();
            for (BlockPos pos : getIntersectingWebs(bb)) {
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(
                    PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
            }
        }
    }

    @EventHandler
    public void onSetCurrentHand(SetCurrentHandEvent event) {
        if (airStrictConfig.get() && !sneaking && checkSlowed()) {
            sneaking = true;
            Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player,
                ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
        }
    }

    @EventHandler
    public void onStrafeFix(StrafeFixEvent event) {
        if (strafeFixConfig.get()) {
            float yaw = Managers.ROTATION.getServerYaw();
            float pitch = Managers.ROTATION.getServerPitch();
            if (Managers.ROTATION.isRotating()) {
                yaw = Managers.ROTATION.getRotationYaw();
                pitch = Managers.ROTATION.getRotationPitch();
            }
            event.cancel();
            event.setYaw(yaw);
            event.setPitch(pitch);
        }
    }

    @EventHandler
    public void onSlowMovement(SlowMovementEvent event) {
        Block block = event.getState().getBlock();
        if (block instanceof CobwebBlock && websConfig.get() || block instanceof SweetBerryBushBlock && berryBushConfig.get()) {
            float multiplier = toFloat(webSpeedConfig.get());
            if (webSpeedConfig.get() == 1.0f) {
                multiplier = 0.0f;
            }
            event.cancel();
            event.setMultiplier(multiplier);
        }
    }

    @EventHandler
    public void onMovementSlowdown(MovementSlowdownEvent event) {
        if (sneakConfig.get() && mc.player.isSneaking() || crawlConfig.get() && mc.player.isCrawling()) {
            float f = 1.0f / (float) mc.player.getAttributeValue(EntityAttributes.PLAYER_SNEAKING_SPEED);
            event.input.movementForward *= f;
            event.input.movementSideways *= f;
        }

        if (checkSlowed()) {
            event.input.movementForward *= 5.0f;
            event.input.movementSideways *= 5.0f;
        }
    }

    @EventHandler
    public void onVelocityMultiplier(VelocityMultiplierEvent event) {
        if (event.getBlock() == Blocks.SOUL_SAND && soulsandConfig.get()
            || event.getBlock() == Blocks.HONEY_BLOCK && honeyblockConfig.get()) {
            event.cancel();
        }
    }

    @EventHandler
    public void onSteppedOnSlimeBlock(SteppedOnSlimeBlockEvent event) {
        if (slimeblockConfig.get()) {
            event.cancel();
        }
    }

    @EventHandler
    public void onBlockSlipperiness(BlockSlipperinessEvent event) {
        if (event.getBlock() == Blocks.SLIME_BLOCK
            && slimeblockConfig.get()) {
            event.cancel();
            event.setSlipperiness(0.6f);
        }
    }

    @EventHandler
    public void onPacketOutbound(PacketEvent.Send event) {
        if (mc.player == null || mc.world == null || mc.isInSingleplayer()) {
            return;
        } else if (event.packet instanceof PlayerMoveC2SPacket packet && packet.changesPosition()
            && strictConfig.get() && checkSlowed()) {
            Managers.INVENTORY.setSlotForced(mc.player.getInventory().selectedSlot);
        } else if (event.packet instanceof ClickSlotC2SPacket && strictConfig.get()) {
            if (mc.player.isUsingItem()) {
                mc.player.stopUsingItem();
            }
            if (sneaking || Managers.POSITION.isSneaking()) {
                Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player,
                    ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            }
            if (Managers.POSITION.isSprinting()) {
                Managers.NETWORK.sendPacket(new ClientCommandC2SPacket(mc.player,
                    ClientCommandC2SPacket.Mode.STOP_SPRINTING));
            }
        }
    }

    private boolean checkStack(ItemStack stack) {
        return !stack.getComponents().contains(DataComponentTypes.FOOD) && stack.getItem() != Items.BOW && stack.getItem() != Items.CROSSBOW && stack.getItem() != Items.SHIELD;
    }

    private boolean checkGrimNew() {
        return !mc.player.isSneaking() && !mc.player.isCrawling() && !mc.player.isRiding() &&
            mc.player.getItemUseTimeLeft() < 5 || ((mc.player.getItemUseTime() > 1) && mc.player.getItemUseTime() % 2 != 0);
    }

    public boolean checkSlowed() {
        if (Disabler.getInstance().grimFireworkCheck2()) {
            return true;
        }
        if (!grimNewConfig.get() || checkGrimNew()) {
            return !mc.player.isRiding() && !mc.player.isSneaking() && (mc.player.isUsingItem() && itemsConfig.get()
                || mc.player.isBlocking() && shieldsConfig.get() && !grimNewConfig.get() && !grimConfig.get());
        }
        return false;
    }

    public boolean checkScreen() {
        return mc.currentScreen != null && !(mc.currentScreen instanceof ChatScreen
            || mc.currentScreen instanceof SignEditScreen || mc.currentScreen instanceof DeathScreen);
    }

    public List<BlockPos> getIntersectingWebs(Box boundingBox) {
        final List<BlockPos> blocks = new ArrayList<>();
        for (BlockPos blockPos : PositionUtil.getAllInBox(boundingBox)) {
            BlockState state = mc.world.getBlockState(blockPos);
            if (state.getBlock() instanceof CobwebBlock) {
                blocks.add(blockPos);
            }
        }
        return blocks;
    }

    public static NoSlowDown getInstance() {
        return INST;
    }
}
