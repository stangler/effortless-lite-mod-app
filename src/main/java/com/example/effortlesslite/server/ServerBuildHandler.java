package com.example.effortlesslite.server;

import com.example.effortlesslite.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ServerBuildHandler {

    // -------------------------------------------------------
    // ブロック配置
    // -------------------------------------------------------
    public static void handlePlaceBlocks(PlaceBlocksPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();

            // プレイヤーの手持ちアイテムからブロック状態を取得
            ItemStack heldItem = player.getMainHandItem();
            if (!(heldItem.getItem() instanceof BlockItem blockItem)) return;
            BlockState blockState = blockItem.getBlock().defaultBlockState();

            List<BlockPos> positions = packet.positions();
            List<BlockPos> placed = new ArrayList<>();

            for (BlockPos pos : positions) {
                if (level.getBlockState(pos).isAir()) {
                    level.setBlock(pos, blockState, 3);
                    placed.add(pos.immutable());
                }
            }

            if (!placed.isEmpty()) {
                PlayerBuildData.pushPlaceUndo(player.getUUID(), placed, level);
            }
        });
    }

    // -------------------------------------------------------
    // ブロック削除（新規）
    // -------------------------------------------------------
    public static void handleDeleteBlocks(DeleteBlocksPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            ServerLevel level = player.serverLevel();

            List<BlockPos> positions = packet.positions();

            // 削除前のスナップショットを取る（空気以外のブロックのみ）
            Map<BlockPos, BlockState> snapshot = new LinkedHashMap<>();
            for (BlockPos pos : positions) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    snapshot.put(pos.immutable(), state);
                }
            }

            if (snapshot.isEmpty()) return;

            // ブロックを削除（空気に置換）
            for (BlockPos pos : snapshot.keySet()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }

            // 削除前の状態をUndoスタックに積む
            PlayerBuildData.pushEraseUndo(player.getUUID(), snapshot);
        });
    }

    // -------------------------------------------------------
    // Undo（UndoPacket.java の呼び出し形式に合わせて IPayloadContext のみ）
    // -------------------------------------------------------
    public static void handleUndo(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            PlayerBuildData.performUndo(player.getUUID(), player.serverLevel());
        });
    }

    // -------------------------------------------------------
    // Redo（RedoPacket.java の呼び出し形式に合わせて IPayloadContext のみ）
    // -------------------------------------------------------
    public static void handleRedo(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player)) return;
            PlayerBuildData.performRedo(player.getUUID(), player.serverLevel());
        });
    }

    // -------------------------------------------------------
    // モード同期
    // -------------------------------------------------------
    public static void handleSyncMode(SyncModePacket packet, IPayloadContext context) {
        // SyncModePacket.java の handle() 内で処理済みのため、ここは空でOK
    }
}
