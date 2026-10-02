package me.lyeddie.addon.module.impl;

import me.lyeddie.addon.Shoreline;
import me.lyeddie.addon.events.BlockCollisionEvent;
import me.lyeddie.addon.events.PushOutOfBlocksEvent;
import me.lyeddie.addon.events.staged.PostPlayerUpdateEvent;
import me.lyeddie.addon.events.staged.PrePlayerUpdateEvent;
import me.lyeddie.addon.managers.Managers;
import me.lyeddie.addon.module.ObsidianPlacerModule;
import me.lyeddie.addon.util.BlastResistantBlocks;
import me.lyeddie.addon.util.EnumFormatter;
import me.lyeddie.addon.util.literal.EntityUtil;
import me.lyeddie.addon.util.literal.RayCastUtil;
import me.lyeddie.addon.util.literal.RotationUtil;
import meteordevelopment.meteorclient.events.world.TickEvent;
import meteordevelopment.meteorclient.settings.*;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.BlockState;
import net.minecraft.block.ScaffoldingBlock;
import net.minecraft.entity.Entity;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.EnderPearlItem;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.packet.c2s.play.HandSwingC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerActionC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractEntityC2SPacket;
import net.minecraft.network.packet.c2s.play.PlayerInteractItemC2SPacket;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.*;
import net.minecraft.util.shape.VoxelShapes;

public class PhaseII extends ObsidianPlacerModule {
    private static PhaseII INST;
    private final SettingGroup sgGeneral = settings.getDefaultGroup();

    private final Setting<Boolean> strictDirectionConfig = addStrictDirectionConfig(sgGeneral);

    public final Setting<PhaseMode> modeConfig = sgGeneral.add(new EnumSetting.Builder<PhaseMode>()
        .name("Mode")
        .description("The phase mode for clipping into blocks")
        .defaultValue(PhaseMode.NORMAL)
        .build());
    public final Setting<Integer> pitchConfig = sgGeneral.add(new IntSetting.Builder()
        .name("Pitch")
        .description("The pitch to throw pearls")
        .defaultValue(85)
        .min(70)
        .sliderMax(90)
        .visible(() -> modeConfig.get() == PhaseMode.PEARL)
        .build());
    private final Setting<Boolean> swapAltConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("SwapAlternative")
        .description("Uses inventory swap for swapping to pearls")
        .defaultValue(true)
        .visible(() -> modeConfig.get() == PhaseMode.PEARL)
        .build());
    private final Setting<Boolean> attackConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Attack")
        .description("Attacks entities in the way of the pearl phase")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == PhaseMode.PEARL)
        .build());
    private final Setting<Boolean> raytraceConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Raytrace")
        .description("Checks the landing position of the pearl")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == PhaseMode.PEARL)
        .build());
    private final Setting<Boolean> swingConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("Swing")
        .description("Swings the hand when throwing pearls")
        .defaultValue(true)
        .visible(() -> modeConfig.get() == PhaseMode.PEARL)
        .build());
    private final Setting<Boolean> selfFillConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("SelfFill")
        .description("Automatically fills blocks you are phasing on")
        .defaultValue(false)
        .visible(() -> modeConfig.get() == PhaseMode.PEARL)
        .build());
    public final Setting<Double> blocksConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Blocks")
        .description("The blocks distance to phase clip")
        .defaultValue(0.003)
        .min(0.001)
        .sliderMax(10.000)
        .visible(() -> modeConfig.get() != PhaseMode.PEARL && modeConfig.get() != PhaseMode.CLIP)
        .build());
    public final Setting<Double> distanceConfig = sgGeneral.add(new DoubleSetting.Builder()
        .name("Distance")
        .description("The distance to phase")
        .defaultValue(0.2)
        .min(0.0)
        .sliderMax(10.0)
        .visible(() -> modeConfig.get() != PhaseMode.PEARL && modeConfig.get() != PhaseMode.CLIP)
        .build());
    private final Setting<Boolean> autoClipConfig = sgGeneral.add(new BoolSetting.Builder()
        .name("AutoClip")
        .description("Automatically clips into the block")
        .defaultValue(true)
        .visible(() -> modeConfig.get() != PhaseMode.PEARL && modeConfig.get() != PhaseMode.CLIP)
        .build());

    private BlockPos grimPos;

    public PhaseII() {
        super(Shoreline.MAIN, "PhaseII", "Allows player to phase through solid blocks", 1000);
        INST = this;
    }

    public static PhaseII getInstance() {
        return INST;
    }

    @Override
    public String getInfoString() {
        return EnumFormatter.formatEnum(modeConfig.get());
    }

    @Override
    public void onActivate() {
        if (mc.player == null) {
            return;
        }

        if (modeConfig.get() == PhaseMode.PEARL) {
            int pearlSlot = -1;
            for (int i = 0; i < 45; i++) {
                ItemStack stack = mc.player.getInventory().getStack(i);
                if (stack.getItem() instanceof EnderPearlItem) {
                    pearlSlot = i;
                    break;
                }
            }

            if (pearlSlot == -1 || mc.player.getItemCooldownManager().isCoolingDown(Items.ENDER_PEARL)) {
                toggle();
                return;
            }

            float prevYaw = mc.player.getYaw();
            float prevPitch = mc.player.getPitch();
            final Vec3d pearlTargetVec = new Vec3d(Math.floor(mc.player.getX()) + 0.5, 0.0, Math.floor(mc.player.getZ()) + 0.5);
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePos(), pearlTargetVec);
            float yaw = rotations[0] + 180.0f;

            if (attackConfig.get()) {
                BlockHitResult hitResult = (BlockHitResult) RayCastUtil.rayCast(3.0, new float[]{yaw, 60.0f});
                for (Entity entity : mc.world.getOtherEntities(null, new Box(hitResult.getBlockPos()).expand(0.2))) {
                    if (entity instanceof ItemFrameEntity itemFrameEntity) {
                        if (!itemFrameEntity.getHeldItemStack().isEmpty()) {
                            Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                        }
                        Managers.NETWORK.sendPacket(PlayerInteractEntityC2SPacket.attack(entity, mc.player.isSneaking()));
                        Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
                    }
                }

                BlockState state = mc.world.getBlockState(mc.player.getBlockPos());
                if (state.getBlock() instanceof ScaffoldingBlock) {
                    Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, mc.player.getBlockPos(), Direction.UP));
                    Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, mc.player.getBlockPos(), Direction.UP));
                }
            }

            if (selfFillConfig.get()) {
                float yaw1 = yaw % 360.0f;
                if (yaw1 < 0.0f) {
                    yaw1 += 360.0f;
                }

                BlockPos blockPos = mc.player.getBlockPos();
                if (yaw1 >= 22.5 && yaw1 < 67.5) {
                    blockPos = blockPos.south().west();
                } else if (yaw1 >= 67.5 && yaw1 < 112.5) {
                    blockPos = blockPos.west();
                } else if (yaw1 >= 112.5 && yaw1 < 157.5) {
                    blockPos = blockPos.north().west();
                } else if (yaw1 >= 157.5 && yaw1 < 202.5) {
                    blockPos = blockPos.north();
                } else if (yaw1 >= 202.5 && yaw1 < 247.5) {
                    blockPos = blockPos.north().east();
                } else if (yaw1 >= 247.5 && yaw1 < 292.5) {
                    blockPos = blockPos.east();
                } else if (yaw1 >= 292.5 && yaw1 < 337.5) {
                    blockPos = blockPos.south().east();
                } else {
                    blockPos = blockPos.south();
                }

                BlockSlot blockItem = getResistantBlockItem();
                if (blockItem != null && blockPos != null && !mc.world.getBlockState(blockPos.down()).isReplaceable()) {
                    Managers.INTERACT.placeBlock(blockPos, blockItem.block(), blockItem.slot(), strictDirectionConfig.get(), false, true, (state, angles) -> {
                            if (state) {
                                Managers.ROTATION.setRotationSilent(angles[0], angles[1]);
                            } else {
                                Managers.ROTATION.setRotationSilentSync();
                            }
                        });
                }
            }

            setRotationClient(yaw, pitchConfig.get());
            if (swapAltConfig.get()) {
                mc.interactionManager.clickSlot(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, mc.player.getInventory().selectedSlot + 36, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, SlotActionType.PICKUP, mc.player);
            } else if (pearlSlot < 9) {
                Managers.INVENTORY.setSlot(pearlSlot);
            }

            setRotationSilent(yaw, pitchConfig.get());
            Managers.NETWORK.sendSequencedPacket(id -> new PlayerInteractItemC2SPacket(Hand.MAIN_HAND, id, yaw, pitchConfig.get()));
            Managers.PEARL.setLastThrownAngles(new float[]{yaw, pitchConfig.get()});
            if (swingConfig.get()) {
                mc.player.swingHand(Hand.MAIN_HAND);
            } else {
                Managers.NETWORK.sendPacket(new HandSwingC2SPacket(Hand.MAIN_HAND));
            }

            if (swapAltConfig.get()) {
                mc.interactionManager.clickSlot(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, mc.player.getInventory().selectedSlot + 36, 0, SlotActionType.PICKUP, mc.player);
                mc.interactionManager.clickSlot(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, SlotActionType.PICKUP, mc.player);
            } else if (pearlSlot < 9) {
                Managers.INVENTORY.syncToClient();
            }

            Managers.ROTATION.setRotationSilentSync();
            setRotationClient(prevYaw, prevPitch);
            toggle();
        } else if (autoClipConfig.get()) {
            double cos = Math.cos(Math.toRadians(mc.player.getYaw() + 90.0f));
            double sin = Math.sin(Math.toRadians(mc.player.getYaw() + 90.0f));
            mc.player.setPosition(mc.player.getX() + (1.0 * blocksConfig.get() * cos + 0.0 * blocksConfig.get() * sin),
                mc.player.getY(), mc.player.getZ() + (1.0 * blocksConfig.get() * sin - 0.0 * blocksConfig.get() * cos));
        }
    }

    @Override
    public void onDeactivate() {
        grimPos = null;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (modeConfig.get() != PhaseMode.CLIP || !mc.player.isOnGround() || mc.player.isRiding()) {
            return;
        }

        Vec3d vec3d = mc.player.getBlockPos().toCenterPos();
        boolean flagX = (vec3d.x - mc.player.getX()) > 0;
        boolean flagZ = (vec3d.z - mc.player.getZ()) > 0;
        double x = vec3d.x + 0.20000000009497754 * (flagX ? -1 : 1);
        double z = vec3d.z + 0.2000000000949811 * (flagZ ? -1 : 1);
        mc.player.setPosition(x, mc.player.getY(), z);
        toggle();
    }

    @EventHandler
    public void onBlockCollision(BlockCollisionEvent event) {
        if (mc.player == null) {
            return;
        }
        switch (modeConfig.get()) {
            case NORMAL -> {
                if (event.getVoxelShape() != VoxelShapes.empty() && event.getVoxelShape().getBoundingBox().maxY > mc.player.getBoundingBox().minY && mc.player.isSneaking()) {
                    event.cancel();
                    event.setVoxelShape(VoxelShapes.empty());
                }
            }
            case SAND -> {
                event.cancel();
                event.setVoxelShape(VoxelShapes.empty());
                mc.player.noClip = true;
            }
            case CLIMB -> {
                if (mc.player.horizontalCollision) {
                    event.cancel();
                    event.setVoxelShape(VoxelShapes.empty());
                }
                if (mc.player.input.sneaking || (mc.player.input.jumping
                    && event.getPos().getY() > mc.player.getY())) {
                    event.cancel();
                }
            }
            case GRIM -> {
                if (!event.getPos().equals(this.grimPos)) {
                    return;
                }
                event.cancel();
                event.setVoxelShape(VoxelShapes.empty());
            }
        }
    }

    @EventHandler
    public void onPlayerUpdate(PrePlayerUpdateEvent event) {
        handlePlayerUpdate();
    }
    // clearly a retard move
    @EventHandler
    public void onPlayerUpdate(PostPlayerUpdateEvent event) {
        handlePlayerUpdate();
    }

    private void handlePlayerUpdate() {
        switch (modeConfig.get()) {
            case NORMAL -> {
                if (mc.player.isSneaking() && isPhasing()) {
                    float yaw = mc.player.getYaw();
                    mc.player.setBoundingBox(mc.player.getBoundingBox().offset(
                        distanceConfig.get() * Math.cos(Math.toRadians(yaw + 90.0f)),
                        0.0, distanceConfig.get() * Math.sin(Math.toRadians(yaw + 90.0f))));
                }
            }
            case SAND -> {
                Managers.MOVEMENT.setMotionY(0.0);
                if (mc.isWindowFocused()) {
                    if (mc.player.input.jumping) {
                        Managers.MOVEMENT.setMotionY(mc.player.getVelocity().y + 0.3);
                    }
                    if (mc.player.input.sneaking) {
                        Managers.MOVEMENT.setMotionY(mc.player.getVelocity().y - 0.3);
                    }
                }
                mc.player.noClip = true;
            }
            case GRIM -> {
                if (grimPos == null) {
                    BlockPos downPos = EntityUtil.getRoundedBlockPos(mc.player).down();
                    BlockState state1 = mc.world.getBlockState(downPos);
                    if (!state1.isAir() && !BlastResistantBlocks.isUnbreakable(state1.getBlock())) {
                        grimPos = downPos;
                    }
                    return;
                }
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, grimPos, Direction.UP));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.START_DESTROY_BLOCK, grimPos, Direction.UP));
                Managers.NETWORK.sendPacket(new PlayerActionC2SPacket(PlayerActionC2SPacket.Action.STOP_DESTROY_BLOCK, grimPos, Direction.UP));
            }
        }
    }

    @EventHandler
    public void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
        event.cancel();
    }

    public boolean isPhasing() {
        Box bb = mc.player.getBoundingBox();
        for (int x = MathHelper.floor(bb.minX); x < MathHelper.floor(bb.maxX) + 1; x++) {
            for (int y = MathHelper.floor(bb.minY); y < MathHelper.floor(bb.maxY) + 1; y++) {
                for (int z = MathHelper.floor(bb.minZ); z < MathHelper.floor(bb.maxZ) + 1; z++) {
                    if (mc.world.getBlockState(new BlockPos(x, y, z)).blocksMovement()) {
                        if (bb.intersects(new Box(x, y, z, x + 1.0, y + 1.0, z + 1.0))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public boolean shouldRaytrace() {
        return raytraceConfig.get();
    }

    public enum PhaseMode {
        NORMAL,
        SAND,
        CLIMB,
        PEARL,
        CLIP,
        GRIM
    }
}
