package com.example.effortlesslite.client;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.build.BlockCalculator;
import com.example.effortlesslite.build.BuildMode;
import com.example.effortlesslite.build.BuildState;
import com.example.effortlesslite.network.DeleteBlocksPacket;
import com.example.effortlesslite.network.PlaceBlocksPacket;
import com.example.effortlesslite.network.RedoPacket;
import com.example.effortlesslite.network.SyncModePacket;
import com.example.effortlesslite.network.UndoPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = EffortlessLite.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientBuildHandler {

    public static final double EXTENDED_REACH = 32.0;

    /** PreviewRenderer から参照するプレビューブロックリスト */
    public static List<BlockPos> previewBlocks = List.of();

    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {
        DimensionDisplayState.INSTANCE.tick();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            previewBlocks = List.of();
            return;
        }

        BuildMode mode = BuildState.mode;

        if (BuildState.isFirstPositionSet() && mode != BuildMode.NORMAL && mode != BuildMode.ERASE_NORMAL) {
            HitResult hit = mc.player.pick(EXTENDED_REACH, 0.0f, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;

                // 削除モード: クリックしたブロック自体を対象にする
                // 配置モード: クリックした面の隣（空気の位置）を対象にする
                BlockPos cursorPos = mode.isErase()
                        ? blockHit.getBlockPos()
                        : blockHit.getBlockPos().relative(blockHit.getDirection());

                BlockPos playerPos = mc.player.blockPosition();
                previewBlocks = BlockCalculator.calculate(
                        mode.toBaseMode(),
                        BuildState.firstPos,
                        cursorPos,
                        BuildState.mirror,
                        playerPos);

                DimensionDisplayState.INSTANCE.updatePreview(BuildState.firstPos, cursorPos);
            }
        } else {
            previewBlocks = List.of();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null)
            return;

        if (ModKeyBindings.MODE_TOGGLE.consumeClick()) {
            BuildState.cycleMode();
            PacketDistributor.sendToServer(new SyncModePacket(BuildState.mode.ordinal()));
            DimensionDisplayState.INSTANCE.clearPreview();
            sendHudMessage(mc, "§fBuild Mode: §e" + BuildState.mode.getDisplayName());
        }

        if (ModKeyBindings.MIRROR_TOGGLE.consumeClick()) {
            BuildState.cycleMirror();
            sendHudMessage(mc, "§fMirror: §e" + BuildState.mirror.getDisplayName());
        }

        if (ModKeyBindings.UNDO.consumeClick()) {
            PacketDistributor.sendToServer(new UndoPacket());
            sendHudMessage(mc, "§eUndo");
        }

        if (ModKeyBindings.REDO.consumeClick()) {
            PacketDistributor.sendToServer(new RedoPacket());
            sendHudMessage(mc, "§eRedo");
        }

        if (ModKeyBindings.CANCEL.consumeClick()) {
            if (BuildState.isFirstPositionSet()) {
                BuildState.reset();
                DimensionDisplayState.INSTANCE.clearPreview();
                sendHudMessage(mc, "§cBuild cancelled");
            }
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        BuildMode mode = BuildState.mode;

        // ── 単一ブロック削除（ERASE_NORMAL）──
        if (mode == BuildMode.ERASE_NORMAL) {
            event.setCanceled(true);
            event.setSwingHand(false);
            HitResult hit = mc.player.pick(EXTENDED_REACH, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) return;
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos targetPos = blockHit.getBlockPos(); // 削除: クリックしたブロック自体
            PacketDistributor.sendToServer(new DeleteBlocksPacket(List.of(targetPos)));
            DimensionDisplayState.INSTANCE.onPlaced(targetPos, targetPos);
            sendHudMessage(mc, "§c1 ブロックを削除しました");
            return;
        }

        // ── 範囲削除（ERASE_LINE / WALL / FLOOR / CUBE）──
        if (mode.isErase()) {
            event.setCanceled(true);
            event.setSwingHand(false);
            HitResult hit = mc.player.pick(EXTENDED_REACH, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) return;
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos targetPos = blockHit.getBlockPos(); // 削除: クリックしたブロック自体
            processEraseClick(mc, targetPos);
            return;
        }

        // ── 以下は既存の配置処理（変更なし）──

        boolean holdingBlock = mc.player.getMainHandItem().getItem() instanceof BlockItem;

        if (mode == BuildMode.NORMAL) {
            if (!holdingBlock) return;
            event.setCanceled(true);
            event.setSwingHand(false);
            HitResult hit = mc.player.pick(EXTENDED_REACH, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK) return;
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos targetPos = blockHit.getBlockPos().relative(blockHit.getDirection());
            PacketDistributor.sendToServer(new PlaceBlocksPacket(List.of(targetPos)));
            DimensionDisplayState.INSTANCE.onPlaced(targetPos, targetPos);
            sendHudMessage(mc, "§a1 ブロックを配置しました");
            return;
        }

        if (!holdingBlock) return;
        event.setCanceled(true);
        event.setSwingHand(false);

        HitResult hit = mc.player.pick(EXTENDED_REACH, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos targetPos = blockHit.getBlockPos().relative(blockHit.getDirection());
        processBuildClick(mc, targetPos);
    }

    /** 範囲削除の2クリック処理 */
    private static void processEraseClick(Minecraft mc, BlockPos targetPos) {
        if (!BuildState.isFirstPositionSet()) {
            BuildState.firstPos = targetPos;
            sendHudMessage(mc, "§c削除始点: §f" + targetPos.toShortString()
                    + " §7| 次のクリックで終点を設定");
        } else {
            BlockPos playerPos = mc.player.blockPosition();
            List<BlockPos> positions = BlockCalculator.calculate(
                    BuildState.mode.toBaseMode(),
                    BuildState.firstPos,
                    targetPos,
                    BuildState.mirror,
                    playerPos);

            if (positions.isEmpty()) {
                sendHudMessage(mc, "§c削除するブロックがありません");
                return;
            }

            PacketDistributor.sendToServer(new DeleteBlocksPacket(positions));
            DimensionDisplayState.INSTANCE.onPlaced(BuildState.firstPos, targetPos);
            sendHudMessage(mc, "§c" + positions.size() + " ブロックを削除しました");

            BuildState.reset();
        }
    }

    /** 範囲配置の2クリック処理（既存・変更なし） */
    private static void processBuildClick(Minecraft mc, BlockPos targetPos) {
        if (!BuildState.isFirstPositionSet()) {
            BuildState.firstPos = targetPos;
            sendHudMessage(mc, "§aStart: §f" + targetPos.toShortString()
                    + " §7| 次のクリックで終点を設定");
        } else {
            BlockPos playerPos = mc.player.blockPosition();
            List<BlockPos> positions = BlockCalculator.calculate(
                    BuildState.mode,
                    BuildState.firstPos,
                    targetPos,
                    BuildState.mirror,
                    playerPos);

            if (positions.isEmpty()) {
                sendHudMessage(mc, "§c配置するブロックがありません");
                return;
            }

            PacketDistributor.sendToServer(new PlaceBlocksPacket(positions));
            DimensionDisplayState.INSTANCE.onPlaced(BuildState.firstPos, targetPos);
            sendHudMessage(mc, "§a" + positions.size() + " ブロックを配置しました");

            BuildState.reset();
        }
    }

    private static void sendHudMessage(Minecraft mc, String message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), true);
        }
    }
}
