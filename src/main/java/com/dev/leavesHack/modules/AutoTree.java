package com.dev.leavesHack.modules;

import com.dev.leavesHack.LeavesHack;
import com.dev.leavesHack.utils.entity.InventoryUtil;
import com.dev.leavesHack.utils.math.Timer;
import com.dev.leavesHack.utils.rotation.Rotation;
import com.dev.leavesHack.utils.world.BlockUtil;
import meteordevelopment.meteorclient.events.entity.player.StartBreakingBlockEvent;
import meteordevelopment.meteorclient.events.render.Render3DEvent;
import meteordevelopment.meteorclient.renderer.ShapeMode;
import meteordevelopment.meteorclient.settings.BoolSetting;
import meteordevelopment.meteorclient.settings.ColorSetting;
import meteordevelopment.meteorclient.settings.EnumSetting;
import meteordevelopment.meteorclient.settings.IntSetting;
import meteordevelopment.meteorclient.settings.Setting;
import meteordevelopment.meteorclient.settings.SettingGroup;
import meteordevelopment.meteorclient.systems.modules.Module;
import meteordevelopment.meteorclient.utils.render.color.SettingColor;
import meteordevelopment.meteorclient.utils.world.BlockUtils;
import meteordevelopment.orbit.EventHandler;
import net.minecraft.block.SaplingBlock;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Items;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;

import java.util.ArrayList;

public class AutoTree extends Module {
    public static AutoTree INSTANCE;

    private final SettingGroup sgGeneral = settings.getDefaultGroup();
    private final SettingGroup sgRender = settings.createGroup("Render");

    private final Setting<Integer> useDelay = sgGeneral.add(new IntSetting.Builder()
        .name("UseDelay")
        .defaultValue(50)
        .min(0)
        .sliderMax(1000)
        .build()
    );

    private final Setting<Integer> blocksPer = sgGeneral.add(new IntSetting.Builder()
        .name("BlocksPer")
        .defaultValue(1)
        .min(0)
        .sliderMax(4)
        .build()
    );

    private final Setting<Boolean> useBoneMeal = sgGeneral.add(new BoolSetting.Builder()
        .name("UseBoneMeal")
        .defaultValue(true)
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

    public final ArrayList<BlockPos> treePos = new ArrayList<>();
    public final Timer timer = new Timer();

    public AutoTree() {
        super(LeavesHack.CATEGORY, "AutoTree", "Automatically replants and grows saplings.");
        INSTANCE = this;
    }

    @Override
    public void onActivate() {
        treePos.clear();
        timer.setMs(999999);
    }

    @EventHandler
    private void onRender(Render3DEvent event) {
        if (mc.player == null || mc.world == null || treePos.isEmpty()) return;

        for (BlockPos pos : treePos) {
            event.renderer.box(pos, sideColor.get(), lineColor.get(), shapeMode.get(), 0);
        }

        if (!timer.passedMs(useDelay.get())) return;

        int placed = 0;
        int oldSlot = mc.player.getInventory().selectedSlot;
        int saplingSlot = InventoryUtil.findClass(SaplingBlock.class);
        int boneMealSlot = InventoryUtil.findItem(Items.BONE_MEAL);

        if (!(mc.player.getInventory().getStack(oldSlot).getItem() instanceof BlockItem treeItem) || !(treeItem.getBlock() instanceof SaplingBlock)) return;

        for (BlockPos pos : treePos) {
            if (placed >= blocksPer.get()) break;
            if (saplingSlot == -1) break;
            if (useBoneMeal.get() && boneMealSlot == -1) return;

            if (BlockUtil.getBlock(pos.up()) instanceof SaplingBlock && useBoneMeal.get()) {
                Direction side = BlockUtil.getClickSide(pos.up());
                InventoryUtil.switchToSlot(boneMealSlot);
                mc.player.swingHand(Hand.MAIN_HAND);
                clickBlock(pos.up(), side, true);
                InventoryUtil.switchToSlot(oldSlot);
                placed++;
            } else if (mc.world.isAir(pos.up()) || mc.world.getBlockState(pos.up()).isReplaceable()) {
                BlockUtil.placeBlock(pos.up(), Direction.DOWN, true);
                placed++;
            }

            if (placed > 0) timer.reset();
        }
    }

    @EventHandler
    private void onStartBreakingBlock(StartBreakingBlockEvent event) {
        if (!BlockUtils.canBreak(event.blockPos)) return;

        event.cancel();
        if (!treePos.contains(event.blockPos)) treePos.add(event.blockPos);
        else treePos.remove(event.blockPos);
    }

    private void clickBlock(BlockPos pos, Direction side, boolean rotate) {
        Vec3d directionVec = new Vec3d(
            pos.getX() + 0.5 + side.getVector().getX() * 0.5,
            pos.getY() + 0.5 + side.getVector().getY() * 0.5,
            pos.getZ() + 0.5 + side.getVector().getZ() * 0.5
        );

        if (rotate) Rotation.snapAt(directionVec);
        mc.player.swingHand(Hand.MAIN_HAND);
        BlockHitResult result = new BlockHitResult(directionVec, side, pos, false);
        mc.interactionManager.interactBlock(mc.player, Hand.MAIN_HAND, result);
        if (rotate) Rotation.snapBack();
    }
}
