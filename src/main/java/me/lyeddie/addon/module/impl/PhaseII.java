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
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.protocol.game.ServerboundAttackPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerActionPacket;
import net.minecraft.network.protocol.game.ServerboundSwingPacket;
import net.minecraft.network.protocol.game.ServerboundUseItemPacket;
import net.minecraft.util.Mth;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.inventory.ContainerInput;
import net.minecraft.world.item.EnderpearlItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.ScaffoldingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.Shapes;

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
                ItemStack stack = mc.player.getInventory().getItem(i);
                if (stack.getItem() instanceof EnderpearlItem) {
                    pearlSlot = i;
                    break;
                }
            }

            if (pearlSlot == -1 || mc.player.getCooldowns().isOnCooldown(mc.player.getInventory().getItem(pearlSlot))) {
                toggle();
                return;
            }

            float prevYaw = mc.player.getYRot();
            float prevPitch = mc.player.getXRot();
            final Vec3 pearlTargetVec = new Vec3(Math.floor(mc.player.getX()) + 0.5, 0.0, Math.floor(mc.player.getZ()) + 0.5);
            float[] rotations = RotationUtil.getRotationsTo(mc.player.getEyePosition(), pearlTargetVec);
            float yaw = rotations[0] + 180.0f;

            if (attackConfig.get()) {
                BlockHitResult hitResult = (BlockHitResult) RayCastUtil.rayCast(3.0, new float[]{yaw, 60.0f});
                for (Entity entity : mc.level.getEntities(null, new AABB(hitResult.getBlockPos()).inflate(0.2))) {
                    if (entity instanceof ItemFrame itemFrameEntity) {
                        if (!itemFrameEntity.getItem().isEmpty()) {
                            Managers.NETWORK.sendPacket(new ServerboundAttackPacket(entity.getId()));
                        }
                        Managers.NETWORK.sendPacket(new ServerboundAttackPacket(entity.getId()));
                        Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
                    }
                }

                BlockState state = mc.level.getBlockState(mc.player.blockPosition());
                if (state.getBlock() instanceof ScaffoldingBlock) {
                    Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, mc.player.blockPosition(), Direction.UP));
                    Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, mc.player.blockPosition(), Direction.UP));
                }
            }

            if (selfFillConfig.get()) {
                float yaw1 = yaw % 360.0f;
                if (yaw1 < 0.0f) {
                    yaw1 += 360.0f;
                }

                BlockPos blockPos = mc.player.blockPosition();
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
                if (blockItem != null && blockPos != null && !mc.level.getBlockState(blockPos.below()).canBeReplaced()) {
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
                mc.gameMode.handleContainerInput(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(0, mc.player.getInventory().getSelectedSlot() + 36, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, ContainerInput.PICKUP, mc.player);
            } else if (pearlSlot < 9) {
                Managers.INVENTORY.setSlot(pearlSlot);
            }

            setRotationSilent(yaw, pitchConfig.get());
            Managers.NETWORK.sendSequencedPacket(id -> new ServerboundUseItemPacket(InteractionHand.MAIN_HAND, id, yaw, pitchConfig.get()));
            Managers.PEARL.setLastThrownAngles(new float[]{yaw, pitchConfig.get()});
            if (swingConfig.get()) {
                mc.player.swing(InteractionHand.MAIN_HAND);
            } else {
                Managers.NETWORK.sendPacket(new ServerboundSwingPacket(InteractionHand.MAIN_HAND));
            }

            if (swapAltConfig.get()) {
                mc.gameMode.handleContainerInput(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(0, mc.player.getInventory().getSelectedSlot() + 36, 0, ContainerInput.PICKUP, mc.player);
                mc.gameMode.handleContainerInput(0, pearlSlot < 9 ? pearlSlot + 36 : pearlSlot, 0, ContainerInput.PICKUP, mc.player);
            } else if (pearlSlot < 9) {
                Managers.INVENTORY.syncToClient();
            }

            Managers.ROTATION.setRotationSilentSync();
            setRotationClient(prevYaw, prevPitch);
            toggle();
        } else if (autoClipConfig.get()) {
            double cos = Math.cos(Math.toRadians(mc.player.getYRot() + 90.0f));
            double sin = Math.sin(Math.toRadians(mc.player.getYRot() + 90.0f));
            mc.player.setPos(mc.player.getX() + (1.0 * blocksConfig.get() * cos + 0.0 * blocksConfig.get() * sin),
                mc.player.getY(), mc.player.getZ() + (1.0 * blocksConfig.get() * sin - 0.0 * blocksConfig.get() * cos));
        }
    }

    @Override
    public void onDeactivate() {
        grimPos = null;
    }

    @EventHandler
    public void onTick(TickEvent.Pre event) {
        if (modeConfig.get() != PhaseMode.CLIP || !mc.player.onGround() || mc.player.isHandsBusy()) {
            return;
        }

        Vec3 vec3d = Vec3.atCenterOf(mc.player.blockPosition());
        boolean flagX = (vec3d.x - mc.player.getX()) > 0;
        boolean flagZ = (vec3d.z - mc.player.getZ()) > 0;
        double x = vec3d.x + 0.20000000009497754 * (flagX ? -1 : 1);
        double z = vec3d.z + 0.2000000000949811 * (flagZ ? -1 : 1);
        mc.player.setPos(x, mc.player.getY(), z);
        toggle();
    }

    @EventHandler
    public void onBlockCollision(BlockCollisionEvent event) {
        if (mc.player == null) {
            return;
        }
        switch (modeConfig.get()) {
            case NORMAL -> {
                if (event.getVoxelShape() != Shapes.empty() && event.getVoxelShape().bounds().maxY > mc.player.getBoundingBox().minY && mc.player.isShiftKeyDown()) {
                    event.cancel();
                    event.setVoxelShape(Shapes.empty());
                }
            }
            case SAND -> {
                event.cancel();
                event.setVoxelShape(Shapes.empty());
                mc.player.noPhysics = true;
            }
            case CLIMB -> {
                if (mc.player.horizontalCollision) {
                    event.cancel();
                    event.setVoxelShape(Shapes.empty());
                }
                if (mc.player.input.keyPresses.shift() || (mc.player.input.keyPresses.jump()
                    && event.getPos().getY() > mc.player.getY())) {
                    event.cancel();
                }
            }
            case GRIM -> {
                if (!event.getPos().equals(this.grimPos)) {
                    return;
                }
                event.cancel();
                event.setVoxelShape(Shapes.empty());
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
                if (mc.player.isShiftKeyDown() && isPhasing()) {
                    float yaw = mc.player.getYRot();
                    mc.player.setBoundingBox(mc.player.getBoundingBox().move(
                        distanceConfig.get() * Math.cos(Math.toRadians(yaw + 90.0f)),
                        0.0, distanceConfig.get() * Math.sin(Math.toRadians(yaw + 90.0f))));
                }
            }
            case SAND -> {
                Managers.MOVEMENT.setMotionY(0.0);
                if (mc.isWindowActive()) {
                    if (mc.player.input.keyPresses.jump()) {
                        Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y + 0.3);
                    }
                    if (mc.player.input.keyPresses.shift()) {
                        Managers.MOVEMENT.setMotionY(mc.player.getDeltaMovement().y - 0.3);
                    }
                }
                mc.player.noPhysics = true;
            }
            case GRIM -> {
                if (grimPos == null) {
                    BlockPos downPos = EntityUtil.getRoundedBlockPos(mc.player).below();
                    BlockState state1 = mc.level.getBlockState(downPos);
                    if (!state1.isAir() && !BlastResistantBlocks.isUnbreakable(state1.getBlock())) {
                        grimPos = downPos;
                    }
                    return;
                }
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, grimPos, Direction.UP));
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.START_DESTROY_BLOCK, grimPos, Direction.UP));
                Managers.NETWORK.sendPacket(new ServerboundPlayerActionPacket(ServerboundPlayerActionPacket.Action.STOP_DESTROY_BLOCK, grimPos, Direction.UP));
            }
        }
    }

    @EventHandler
    public void onPushOutOfBlocks(PushOutOfBlocksEvent event) {
        event.cancel();
    }

    public boolean isPhasing() {
        AABB bb = mc.player.getBoundingBox();
        for (int x = Mth.floor(bb.minX); x < Mth.floor(bb.maxX) + 1; x++) {
            for (int y = Mth.floor(bb.minY); y < Mth.floor(bb.maxY) + 1; y++) {
                for (int z = Mth.floor(bb.minZ); z < Mth.floor(bb.maxZ) + 1; z++) {
                    if (mc.level.getBlockState(new BlockPos(x, y, z)).blocksMotion()) {
                        if (bb.intersects(new AABB(x, y, z, x + 1.0, y + 1.0, z + 1.0))) {
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
