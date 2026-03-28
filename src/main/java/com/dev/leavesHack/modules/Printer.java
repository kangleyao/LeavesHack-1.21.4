package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.events.MoveEvent;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import fi.dy.masa.litematica.world.SchematicWorldHandler;
import fi.dy.masa.litematica.world.WorldSchematic;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BlockListSetting;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.orbit.EventHandler;
import meteordevelopment.orbit.EventPriority;
import net.minecraft.block.AbstractRedstoneGateBlock;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.DaylightDetectorBlock;
import net.minecraft.block.FurnaceBlock;
import net.minecraft.block.HopperBlock;
import net.minecraft.block.ObserverBlock;
import net.minecraft.block.PistonBlock;
import net.minecraft.block.PressurePlateBlock;
import net.minecraft.block.RedstoneLampBlock;
import net.minecraft.block.RedstoneWireBlock;
import net.minecraft.block.SlabBlock;
import net.minecraft.block.TargetBlock;
import net.minecraft.block.TripwireHookBlock;
import net.minecraft.block.enums.SlabType;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.network.packet.c2s.play.ClientCommandC2SPacket;
import net.minecraft.state.property.Properties;
import net.minecraft.text.Text;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;
import java.util.List;

public class Printer extends Module {
    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgShift = settings.createGroup("IgnoreSneak");
    private final SettingGroup sgRender = settings.createGroup("Render");
    private final SettingGroup sgWhitelist = settings.createGroup("Whitelist");

    private final Setting<Boolean> rotate = sgGeneral.add(new BoolSetting.Builder()
        .name("Rotate")
        .description("Rotate towards blocks when placing.")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> printingRange = sgGeneral.add(new IntSetting.Builder()
        .name("PrintingRange")
        .description("How far to place blocks around the player.")
        .defaultValue(4)
        .min(1)
        .sliderMax(6)
        .build()
    );

    private final Setting<Boolean> inventorySwap = sgGeneral.add(new BoolSetting.Builder()
        .name("InventorySwap")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> safeWalk = sgGeneral.add(new BoolSetting.Builder()
        .name("SafeWalk")
        .defaultValue(true)
        .build()
    );

    private final Setting<Boolean> ignoreSneak = sgShift.add(new BoolSetting.Builder()
        .name("IgnoreSneak")
        .defaultValue(true)
        .build()
    );

    private final Setting<Integer> shiftTime = sgShift.add(new IntSetting.Builder()
        .name("ShiftTime")
        .defaultValue(100)
        .min(0)
        .sliderMax(1000)
        .build()
    );

    private final Setting<Integer> sneakSpeed = sgShift.add(new IntSetting.Builder()
        .name("SneakSpeed")
        .description("Movement speed while forced sneaking.")
        .defaultValue(0)
        .min(0)
        .sliderMax(20)
        .build()
    );

    private final Setting<ListMode> listMode = sgWhitelist.add(new EnumSetting.Builder<ListMode>()
        .name("ListMode")
        .description("Selection mode.")
        .defaultValue(ListMode.Blacklist)
        .build()
    );

    private final Setting<List<Block>> blacklist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("BlackList")
        .description("Blocks to ignore.")
        .visible(() -> listMode.get() == ListMode.Blacklist)
        .build()
    );

    private final Setting<List<Block>> whitelist = sgWhitelist.add(new BlockListSetting.Builder()
        .name("WhiteList")
        .description("Blocks to allow.")
        .visible(() -> listMode.get() == ListMode.Whitelist)
        .build()
    );

    private final Setting<ShapeMode> shapeMode = sgRender.add(new EnumSetting.Builder<ShapeMode>()
        .name("ShapeMode")
        .defaultValue(ShapeMode.Both)
        .build()
    );

    private final Setting<SettingColor> lineColor = sgRender.add(new ColorSetting.Builder()
        .name("LineColor")
        .defaultValue(new SettingColor(255, 255, 255, 255))
        .build()
    );

    private final Setting<SettingColor> sideColor = sgRender.add(new ColorSetting.Builder()
        .name("SideColor")
        .defaultValue(new SettingColor(255, 255, 255, 50))
        .build()
    );

    private final Setting<Boolean> debug = sgGeneral.add(new BoolSetting.Builder()
        .name("DeBug")
        .description("Shows development diagnostics.")
        .defaultValue(false)
        .build()
    );

    private final Timer shiftTimer = new Timer();
    private boolean hasSneak = false;

    public Printer() {
        super(LeavesHack.CATEGORY, "printer", "Places blocks based on a Litematica schematic.");
    }

    @Override
    public void onActivate() {
        hasSneak = false;
        shiftTimer.setMs(99999);
    }

    @Override
    public void onDeactivate() {
        if (hasSneak && mc.player != null && mc.getNetworkHandler() != null) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            mc.player.setSneaking(false);
            hasSneak = false;
        }
    }

    @EventHandler
    private void onRender3D(Render3DEvent event) {
        if (mc.player == null || mc.world == null) return;

        WorldSchematic schematic = SchematicWorldHandler.getSchematicWorld();
        if (schematic == null) return;
        if (!shiftTimer.passedMs(shiftTime.get()) && hasSneak && ignoreSneak.get()) return;

        List<BlockPos> sphere = BlockUtil.getSphere(printingRange.get());
        int placed = 0;

        for (BlockPos pos : sphere) {
            BlockState required = schematic.getBlockState(pos);
            if (listMode.get() == ListMode.Blacklist && blacklist.get().contains(required.getBlock())) continue;
            if (listMode.get() == ListMode.Whitelist && !whitelist.get().contains(required.getBlock())) continue;

            if (!required.isAir() && required.getFluidState().isEmpty() && (mc.world.isAir(pos) || BlockUtil.canReplace(pos)) && !BlockUtil.hasEntity(pos, false)) {
                if (placed >= 1) {
                    debugMessage("Placed maximum blocks this tick: " + placed);
                    return;
                }

                int slot = inventorySwap.get() ? InventoryUtil.findBlockInventory(required.getBlock()) : InventoryUtil.findBlock(required.getBlock());
                if (slot == -1) continue;

                int old = mc.player.getInventory().selectedSlot;
                ArrayList<Direction> sides = BlockUtil.getPlaceSides(pos, null, ignoreSneak.get());
                if (sides.isEmpty()) continue;

                event.renderer.box(new Box(pos), sideColor.get(), lineColor.get(), shapeMode.get(), 0);
                Direction target = sides.getFirst();
                Direction facing = getBlockFacing(required);

                if (facing != null && !isRedstoneComponent(required)) {
                    debugMessage("Searching for a side that matches block facing.");
                    boolean found = false;

                    for (Direction direction : sides) {
                        debugMessage("Candidate side: " + direction);
                        if (checkState(pos.offset(direction), required, direction.getOpposite())) {
                            found = true;
                            target = direction;
                        }
                    }

                    if (!found) {
                        debugMessage("No suitable facing side found.");
                        continue;
                    }
                }

                if (required.getBlock() instanceof RedstoneWireBlock && (mc.world.isAir(pos.down()) || mc.world.getBlockState(pos.down()).isReplaceable())) continue;

                if (BlockUtil.needSneak(BlockUtil.getBlock(pos.offset(target))) && !hasSneak) {
                    mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.PRESS_SHIFT_KEY));
                    hasSneak = true;
                    mc.player.setSneaking(true);
                    shiftTimer.reset();
                    return;
                }

                placed++;
                doSwap(slot);
                boolean rotated = false;

                if (rotate.get()) {
                    Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + target.getVector().getX() * 0.5, pos.getY() + 0.5 + target.getVector().getY() * 0.5, pos.getZ() + 0.5 + target.getVector().getZ() * 0.5);
                    Rotation.snapAt(directionVec);
                    rotated = true;
                }

                if (facing != null && isRedstoneComponent(required)) {
                    if (required.getBlock() instanceof ObserverBlock) blockFacing(facing);
                    else blockFacing(facing.getOpposite());
                    rotated = true;
                }

                SlabType type = getSlabType(required);
                if (type != null) {
                    switch (type) {
                        case TOP -> {
                            if (!(BlockUtil.getBlock(pos) instanceof SlabBlock)) BlockUtil.placeSlabBlock(pos, target, Direction.UP, false);
                        }
                        case BOTTOM -> {
                            if (!(BlockUtil.getBlock(pos) instanceof SlabBlock)) BlockUtil.placeSlabBlock(pos, target, Direction.DOWN, false);
                        }
                        case DOUBLE -> BlockUtil.placeBlock(pos, target, false);
                    }
                } else {
                    BlockUtil.placeBlock(pos, target, false);
                }

                if (hasSneak && ignoreSneak.get()) {
                    mc.player.networkHandler.sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
                    mc.player.setSneaking(false);
                    hasSneak = false;
                }

                if (rotated) Rotation.snapBack();
                event.renderer.box(new Box(pos), sideColor.get(), lineColor.get(), shapeMode.get(), 0);

                if (inventorySwap.get()) doSwap(slot);
                else doSwap(old);
            }
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onMove1(MoveEvent event) {
        if (!safeWalk.get() || mc.player == null || mc.world == null) return;

        double x = event.getX();
        double y = event.getY();
        double z = event.getZ();

        if (mc.player.isOnGround()) {
            double increment = 0.05;

            while (x != 0.0 && isOffsetBBEmpty(x, -1.0, 0.0)) {
                if (x < increment && x >= -increment) x = 0.0;
                else if (x > 0.0) x -= increment;
                else x += increment;
            }

            while (z != 0.0 && isOffsetBBEmpty(0.0, -1.0, z)) {
                if (z < increment && z >= -increment) z = 0.0;
                else if (z > 0.0) z -= increment;
                else z += increment;
            }

            while (x != 0.0 && z != 0.0 && isOffsetBBEmpty(x, -1.0, z)) {
                x = x < increment && x >= -increment ? 0.0 : (x > 0.0 ? x - increment : x + increment);
                if (z < increment && z >= -increment) z = 0.0;
                else if (z > 0.0) z -= increment;
                else z += increment;
            }
        }

        event.setX(x);
        event.setY(y);
        event.setZ(z);
    }

    public boolean isOffsetBBEmpty(double offsetX, double offsetY, double offsetZ) {
        return mc.player != null && mc.world != null && !mc.world.canCollide(mc.player, mc.player.getBoundingBox().offset(offsetX, offsetY, offsetZ));
    }

    @EventHandler
    public void onMove2(MoveEvent event) {
        if (mc.player == null || mc.world == null) return;

        if (shiftTimer.passedMs(shiftTime.get() * 2L) && ignoreSneak.get() && hasSneak) {
            mc.getNetworkHandler().sendPacket(new ClientCommandC2SPacket(mc.player, ClientCommandC2SPacket.Mode.RELEASE_SHIFT_KEY));
            mc.player.setSneaking(false);
            hasSneak = false;
            return;
        }

        if (!hasSneak) return;

        double speed = sneakSpeed.get();
        double moveSpeed = 0.2873 / 100 * speed;
        double forward = mc.player.input.movementForward;
        double sideways = mc.player.input.movementSideways;
        double yaw = mc.player.getYaw();

        if (forward == 0.0 && sideways == 0.0) {
            event.setX(0.0);
            event.setZ(0.0);
            return;
        }

        if (forward != 0.0 && sideways != 0.0) {
            forward *= Math.sin(0.7853981633974483);
            sideways *= Math.cos(0.7853981633974483);
        }

        event.setX(forward * moveSpeed * -Math.sin(Math.toRadians(yaw)) + sideways * moveSpeed * Math.cos(Math.toRadians(yaw)));
        event.setZ(forward * moveSpeed * Math.cos(Math.toRadians(yaw)) - sideways * moveSpeed * -Math.sin(Math.toRadians(yaw)));
    }

    public static SlabType getSlabType(BlockState state) {
        if (state.getBlock() instanceof SlabBlock) return state.get(SlabBlock.TYPE);
        return null;
    }

    public void blockFacing(Direction direction) {
        if (direction == Direction.EAST) Rotation.snapAt(-90.0f, 5.0f);
        else if (direction == Direction.WEST) Rotation.snapAt(90.0f, 5.0f);
        else if (direction == Direction.NORTH) Rotation.snapAt(180.0f, 5.0f);
        else if (direction == Direction.SOUTH) Rotation.snapAt(0.0f, 5.0f);
        else if (direction == Direction.UP) Rotation.snapAt(5.0f, -90.0f);
        else if (direction == Direction.DOWN) Rotation.snapAt(5.0f, 90.0f);
    }

    public static boolean isRedstoneComponent(BlockState state) {
        Block block = state.getBlock();

        return block instanceof RedstoneWireBlock
            || block instanceof AbstractRedstoneGateBlock
            || block instanceof PressurePlateBlock
            || block instanceof ObserverBlock
            || block instanceof TargetBlock
            || block instanceof TripwireHookBlock
            || block instanceof DaylightDetectorBlock
            || block instanceof PistonBlock
            || block instanceof RedstoneLampBlock
            || block instanceof FurnaceBlock;
    }

    public boolean checkState(BlockPos pos, BlockState targetState, Direction direction) {
        Vec3d directionVec = new Vec3d(pos.getX() + 0.5 + direction.getVector().getX() * 0.5, pos.getY() + 0.5 + direction.getVector().getY() * 0.5, pos.getZ() + 0.5 + direction.getVector().getZ() * 0.5);
        BlockHitResult hit = new BlockHitResult(directionVec, direction, pos, false);
        ItemPlacementContext ctx = new ItemPlacementContext(mc.player, Hand.MAIN_HAND, mc.player.getMainHandStack(), hit);
        BlockState result = targetState.getBlock().getPlacementState(ctx);

        if (result != null && isSameFacing(result, targetState)) return true;
        if (result == null) debugMessage("Placement state result was null.");
        return false;
    }

    public static Direction getBlockFacing(BlockState state) {
        if (state.getBlock() instanceof HopperBlock) return state.get(HopperBlock.FACING);
        if (state.contains(Properties.HORIZONTAL_FACING)) return state.get(Properties.HORIZONTAL_FACING);
        if (state.contains(Properties.FACING)) return state.get(Properties.FACING);

        if (state.contains(Properties.AXIS)) {
            return switch (state.get(Properties.AXIS)) {
                case X -> Direction.EAST;
                case Y -> Direction.UP;
                case Z -> Direction.SOUTH;
            };
        }

        return null;
    }

    private boolean isSameFacing(BlockState a, BlockState b) {
        if (a.getBlock() != b.getBlock()) return false;

        Direction fa = getBlockFacing(a);
        Direction fb = getBlockFacing(b);
        debugMessage("fa: " + fa + " fb: " + fb);

        if (fa == null || fb == null) return true;
        return fa == fb;
    }

    private void doSwap(int slot) {
        if (!inventorySwap.get()) InventoryUtil.switchToSlot(slot);
        else InventoryUtil.inventorySwap(slot, mc.player.getInventory().selectedSlot);
    }

    private void debugMessage(String message) {
        if (debug.get() && mc.player != null) mc.player.sendMessage(Text.of(message), false);
    }

    public enum ListMode {
        Whitelist,
        Blacklist
    }
}
