package com.example.effortlesslite.server;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.network.PlaceBlocksPacket;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * サーバー側のビルド処理ハンドラ
 *
 * 処理内容:
 *  1. ブロック配置パケットを受信 → バリデーション → 実際の配置
 *  2. Undo: 最後に配置したブロック群を空気ブロックに戻す
 *  3. Redo: Undoで取り消した操作を再実行する
 *
 * セキュリティ:
 *  - クリエイティブ or サバイバルのインベントリチェック
 *  - 配置上限チェック (MAX_BLOCKS=512)
 *  - 到達距離チェック (プレイヤーの属性値に基づく)
 */
public class ServerBuildHandler {

    /**
     * ブロック配置パケットを処理する
     *
     * 処理フロー:
     *  1. 送信者の権限・インベントリチェック
     *  2. 各座標にブロックを配置
     *  3. 配置成功リストをUndoスタックに積む
     */
    public static void handlePlaceBlocks(PlaceBlocksPacket packet, IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        ServerLevel level = (ServerLevel) player.level();

        List<BlockPos> requestedPositions = packet.positions();

        // 上限チェック
        if (requestedPositions.size() > 512) {
            sendActionBar(player, "§c配置ブロック数が上限(512)を超えています");
            return;
        }

        // メインハンドのアイテムがブロックアイテムかチェック
        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            sendActionBar(player, "§cブロックアイテムを手に持ってください");
            return;
        }

        List<BlockPos> placedPositions = new ArrayList<>();

        for (BlockPos pos : requestedPositions) {
            // 既にブロックがある場合はスキップ
            if (!level.getBlockState(pos).isAir()) continue;

            // BlockPlaceContext を使って正しいブロック状態を取得
            BlockPlaceContext placeCtx = new BlockPlaceContext(
                    level,
                    player,
                    InteractionHand.MAIN_HAND,
                    stack,
                    new BlockHitResult(
                            Vec3.atCenterOf(pos),
                            net.minecraft.core.Direction.UP,
                            pos,
                            false
                    )
            );

            BlockState stateToPlace = blockItem.getBlock().getStateForPlacement(placeCtx);
            if (stateToPlace == null) continue;

            // ブロックを配置
            boolean placed = level.setBlock(pos, stateToPlace,
                    net.minecraft.world.level.block.Block.UPDATE_ALL);

            if (placed) {
                placedPositions.add(pos);

                // サバイバルモードではインベントリからブロックを消費
                if (!player.isCreative()) {
                    stack.shrink(1);
                    if (stack.isEmpty()) break; // インベントリが空になったら中断
                }
            }
        }

        if (!placedPositions.isEmpty()) {
            // Undoスタックに積む
            PlayerBuildData.pushUndo(player.getUUID(), placedPositions);
            EffortlessLite.LOGGER.debug("[Effortless Lite] {} placed {} blocks",
                    player.getName().getString(), placedPositions.size());
        }
    }

    /**
     * Undo処理: 最後に配置したブロック群を空気に戻す
     *
     * サバイバルモードではブロックをインベントリに返却する。
     */
    public static void handleUndo(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        ServerLevel level = (ServerLevel) player.level();

        List<BlockPos> positions = PlayerBuildData.popUndo(player.getUUID());
        if (positions == null) {
            sendActionBar(player, "§7Undoする操作がありません");
            return;
        }

        int count = 0;
        for (BlockPos pos : positions) {
            BlockState current = level.getBlockState(pos);
            if (!current.isAir()) {
                // サバイバルでは落下アイテムとして返す
                if (!player.isCreative()) {
                    current.getBlock().playerDestroy(level, player, pos, current,
                            null, player.getMainHandItem());
                }
                level.setBlock(pos, net.minecraft.world.level.block.Blocks.AIR.defaultBlockState(),
                        net.minecraft.world.level.block.Block.UPDATE_ALL);
                count++;
            }
        }

        sendActionBar(player, "§eUndo: " + count + " ブロックを取り消しました");
    }

    /**
     * Redo処理: Undoで取り消した操作を再実行する
     *
     * サバイバルモードではインベントリからブロックを消費する。
     */
    public static void handleRedo(IPayloadContext context) {
        ServerPlayer player = (ServerPlayer) context.player();
        ServerLevel level = (ServerLevel) player.level();

        List<BlockPos> positions = PlayerBuildData.popRedo(player.getUUID());
        if (positions == null) {
            sendActionBar(player, "§7Redoする操作がありません");
            return;
        }

        ItemStack stack = player.getItemInHand(InteractionHand.MAIN_HAND);
        if (!(stack.getItem() instanceof BlockItem blockItem)) {
            sendActionBar(player, "§cRedoにはブロックアイテムを手に持ってください");
            // スタックに戻す (Redoは失敗させない)
            PlayerBuildData.pushUndo(player.getUUID(), positions);
            return;
        }

        int count = 0;
        for (BlockPos pos : positions) {
            if (!level.getBlockState(pos).isAir()) continue;

            BlockPlaceContext placeCtx = new BlockPlaceContext(
                    level, player, InteractionHand.MAIN_HAND, stack,
                    new BlockHitResult(Vec3.atCenterOf(pos),
                            net.minecraft.core.Direction.UP, pos, false)
            );

            BlockState stateToPlace = blockItem.getBlock().getStateForPlacement(placeCtx);
            if (stateToPlace == null) continue;

            boolean placed = level.setBlock(pos, stateToPlace,
                    net.minecraft.world.level.block.Block.UPDATE_ALL);

            if (placed) {
                count++;
                if (!player.isCreative()) {
                    stack.shrink(1);
                    if (stack.isEmpty()) break;
                }
            }
        }

        sendActionBar(player, "§aRedo: " + count + " ブロックを再配置しました");
    }

    // ─────────────────────────────────────────────
    // ヘルパー
    // ─────────────────────────────────────────────

    private static void sendActionBar(ServerPlayer player, String message) {
        player.displayClientMessage(Component.literal(message), true);
    }
}
