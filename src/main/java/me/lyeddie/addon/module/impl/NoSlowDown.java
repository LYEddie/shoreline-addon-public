package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.*;
import me.lyeddie.addon.events.irrevocable.MovementSlowdownEvent;
import me.lyeddie.addon.events.irrevocable.SetCurrentHandEvent;
import me.lyeddie.addon.events.staged.PrePlayerUpdateEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.mixin.impl.accessor.AccessorKeyBinding;
import me.lyeddie.addon.util.literal.MovementUtil;
import me.lyeddie.addon.util.literal.PositionUtil;
import meteordevelopment.meteorclient.events.packets.PacketEvent;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.DoubleSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import me.lyeddie.addon.module.AddonModule;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.gui.screens.DeathScreen;
import net.minecraft.client.gui.screens.inventory.SignEditScreen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ServerboundContainerClickPacket;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SweetBerryBushBlock;
import net.minecraft.world.level.block.WebBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import org.lwjgl.glfw.GLFW;
import com.mojang.blaze3d.platform.InputConstants;
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
            Managers.MOVEMENT.sendSneaking(false);
        }
        sneaking = false;
        Managers.TICK.setClientTick(1.0f);
    }

    @EventHandler
    public void onPlayerUpdate(PrePlayerUpdateEvent event) {
        if (grimConfig.get() && mc.player.isUsingItem() && !mc.player.isShiftKeyDown() && itemsConfig.get()) {
            if (mc.player.getUsedItemHand() == InteractionHand.OFF_HAND && checkStack(mc.player.getMainHandItem())) {
                Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
            } else if (checkStack(mc.player.getOffhandItem())) {
                Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.OFF_HAND, id, mc.player.getYRot(), mc.player.getXRot()));
            }
        }
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (airStrictConfig.get() && !mc.player.isUsingItem()) {
            sneaking = false;
            Managers.MOVEMENT.sendSneaking(false);
        }
        if (strafeFixConfig.get() && checkSlowed()) {
        }
        if (inventoryMoveConfig.get() && checkScreen()) {
            final var window = mc.getWindow();
            KeyMapping[] keys = new KeyMapping[]{mc.options.keyJump, mc.options.keyUp, mc.options.keyDown, mc.options.keyRight, mc.options.keyLeft};
            for (KeyMapping binding : keys) {
                binding.setDown(InputConstants.isKeyDown(window, ((AccessorKeyBinding) binding).getBoundKey().getValue()));
            }
            if (arrowMoveConfig.get()) {
                float yaw = mc.player.getYRot();
                float pitch = mc.player.getXRot();
                if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_UP)) {
                    pitch -= 3.0f;
                } else if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_DOWN)) {
                    pitch += 3.0f;
                } else if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_LEFT)) {
                    yaw -= 3.0f;
                } else if (InputConstants.isKeyDown(window, GLFW.GLFW_KEY_RIGHT)) {
                    yaw += 3.0f;
                }
                mc.player.setYRot(yaw);
                mc.player.setXRot(Mth.clamp(pitch, -90.0f, 90.0f));
            }
        }

        if ((grimConfig.get() || grimNewConfig.get()) && websConfig.get()) {
            AABB bb = grimConfig.get() ? mc.player.getBoundingBox().inflate(1.0) : mc.player.getBoundingBox();
            for (BlockPos pos : getIntersectingWebs(bb)) {
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(
                    ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, pos, Direction.DOWN));
            }
        }
    }

    @EventHandler
    public void onSetCurrentHand(SetCurrentHandEvent event) {
        if (airStrictConfig.get() && !sneaking && checkSlowed()) {
            sneaking = true;
            Managers.MOVEMENT.sendSneaking(true);
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
        if (block instanceof WebBlock && websConfig.get() || block instanceof SweetBerryBushBlock && berryBushConfig.get()) {
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
        if (sneakConfig.get() && mc.player.isShiftKeyDown() || crawlConfig.get() && mc.player.isVisuallyCrawling()) {
            float f = 1.0f / (float) mc.player.getAttributeValue(Attributes.SNEAKING_SPEED);
            MovementUtil.scale(event.input, f);
        }

        if (checkSlowed()) {
            MovementUtil.scale(event.input, 5.0f);
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
        if (mc.player == null || mc.level == null || mc.isLocalServer()) {
            return;
        } else if (event.packet instanceof ServerboundMovePlayerPacket packet && packet.hasPosition()
            && strictConfig.get() && checkSlowed()) {
            Managers.INVENTORY.setSlotForced(mc.player.getInventory().getSelectedSlot());
        } else if (event.packet instanceof ServerboundContainerClickPacket && strictConfig.get()) {
            if (mc.player.isUsingItem()) {
                mc.player.releaseUsingItem();
            }
            if (sneaking || Managers.POSITION.isSneaking()) {
                Managers.MOVEMENT.sendSneaking(false);
            }
            if (Managers.POSITION.isSprinting()) {
                Managers.NETWORK.sendPacket(new ServerboundPlayerCommandPacket(mc.player,
                    ServerboundPlayerCommandPacket.Action.STOP_SPRINTING));
            }
        }
    }

    private boolean checkStack(ItemStack stack) {
        return !stack.getComponents().has(DataComponents.FOOD) && stack.getItem() != Items.BOW && stack.getItem() != Items.CROSSBOW && stack.getItem() != Items.SHIELD;
    }

    private boolean checkGrimNew() {
        return !mc.player.isShiftKeyDown() && !mc.player.isVisuallyCrawling() && !mc.player.isHandsBusy() &&
            mc.player.getUseItemRemainingTicks() < 5 || ((mc.player.getTicksUsingItem() > 1) && mc.player.getTicksUsingItem() % 2 != 0);
    }

    public boolean checkSlowed() {
        if (Disabler.getInstance().grimFireworkCheck2()) {
            return true;
        }
        if (!grimNewConfig.get() || checkGrimNew()) {
            return !mc.player.isHandsBusy() && !mc.player.isShiftKeyDown() && (mc.player.isUsingItem() && itemsConfig.get()
                || mc.player.isBlocking() && shieldsConfig.get() && !grimNewConfig.get() && !grimConfig.get());
        }
        return false;
    }

    public boolean checkScreen() {
        return mc.screen != null && !(mc.screen instanceof ChatScreen
            || mc.screen instanceof SignEditScreen || mc.screen instanceof DeathScreen);
    }

    public List<BlockPos> getIntersectingWebs(AABB boundingBox) {
        final List<BlockPos> blocks = new ArrayList<>();
        for (BlockPos blockPos : PositionUtil.getAllInBox(boundingBox)) {
            BlockState state = mc.level.getBlockState(blockPos);
            if (state.getBlock() instanceof WebBlock) {
                blocks.add(blockPos);
            }
        }
        return blocks;
    }

    public static NoSlowDown getInstance() {
        return INST;
    }
}
