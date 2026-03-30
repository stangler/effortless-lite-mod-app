package com.example.effortlesslite.server;

import com.example.effortlesslite.network.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
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
            if (!(context.player() instanceof ServerPlayer player))
                return;
            ServerLevel level = player.serverLevel();

            ItemStack heldItem = player.getMainHandItem();
            if (!(heldItem.getItem() instanceof BlockItem blockItem))
                return;

            List<BlockPos> positions = packet.positions();
            Direction clickedFace = packet.clickedFace();
            List<BlockPos> placed = new ArrayList<>();

            for (BlockPos pos : positions) {
                if (!level.getBlockState(pos).isAir())
                    continue;

                // クライアントから受け取った clickedFace を使って BlockPlaceContext を生成
                BlockHitResult hitResult = new BlockHitResult(
                        Vec3.atCenterOf(pos),
                        clickedFace,
                        pos,
                        false);
                BlockPlaceContext placeCtx = new BlockPlaceContext(
                        level, player, InteractionHand.MAIN_HAND, heldItem, hitResult);
                BlockState stateToPlace = blockItem.getBlock().getStateForPlacement(placeCtx);
                if (stateToPlace == null)
                    continue;

                // setPlacedBy 前に上下の状態を記録（ドアの上半分など副作用で生成されるブロックを検出するため）
                boolean aboveWasAir = level.getBlockState(pos.above()).isAir();
                boolean belowWasAir = level.getBlockState(pos.below()).isAir();

                level.setBlock(pos, stateToPlace, 3);
                blockItem.getBlock().setPlacedBy(level, pos, stateToPlace, player, heldItem);
                placed.add(pos.immutable());

                // setPlacedBy によって新たに生成されたブロックも Undo 対象に追加
                if (aboveWasAir && !level.getBlockState(pos.above()).isAir()) {
                    placed.add(pos.above().immutable());
                }
                if (belowWasAir && !level.getBlockState(pos.below()).isAir()) {
                    placed.add(pos.below().immutable());
                }
            }

            if (!placed.isEmpty()) {
                PlayerBuildData.pushPlaceUndo(player.getUUID(), placed, level);
            }
        });
    }

    // -------------------------------------------------------
    // ブロック削除
    // -------------------------------------------------------
    public static void handleDeleteBlocks(DeleteBlocksPacket packet, IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player))
                return;
            ServerLevel level = player.serverLevel();

            List<BlockPos> positions = packet.positions();

            Map<BlockPos, BlockState> snapshot = new LinkedHashMap<>();
            for (BlockPos pos : positions) {
                BlockState state = level.getBlockState(pos);
                if (!state.isAir()) {
                    snapshot.put(pos.immutable(), state);
                }
            }

            if (snapshot.isEmpty())
                return;

            for (BlockPos pos : snapshot.keySet()) {
                level.setBlock(pos, Blocks.AIR.defaultBlockState(), 3);
            }

            PlayerBuildData.pushEraseUndo(player.getUUID(), snapshot);
        });
    }

    // -------------------------------------------------------
    // Undo
    // -------------------------------------------------------
    public static void handleUndo(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player))
                return;
            PlayerBuildData.performUndo(player.getUUID(), player.serverLevel());
        });
    }

    // -------------------------------------------------------
    // Redo
    // -------------------------------------------------------
    public static void handleRedo(IPayloadContext context) {
        context.enqueueWork(() -> {
            if (!(context.player() instanceof ServerPlayer player))
                return;
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