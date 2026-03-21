package com.example.effortlesslite.server;

import com.example.effortlesslite.build.BuildMode;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.block.state.BlockState;

import java.util.*;

/**
 * サーバー側でプレイヤーごとのビルドデータを管理するクラス
 *
 * 保持データ:
 *  - 現在のビルドモード
 *  - Undo スタック (最大20操作)
 *  - Redo スタック
 *
 * Undo/Redo エントリは Map<BlockPos, BlockState> で統一管理する:
 *   - ブロック配置のUndo: { pos → AIR }    → Undoで空気に戻す
 *   - ブロック削除のUndo: { pos → 元State } → Undoで元のブロックに戻す
 *   どちらも「マップの値の状態に戻す」という操作で統一されている。
 */
public class PlayerBuildData {

    /** 拡張リーチのモディファイアID */
    public static final ResourceLocation REACH_MODIFIER_ID =
            ResourceLocation.fromNamespaceAndPath("effortlesslite", "extended_reach");

    /** 拡張リーチで加算する距離 (バニラ默認5 + 27 = 32ブロック) */
    public static final double EXTENDED_REACH_BONUS = 27.0;

    /** Undo/Redo スタックの最大保持数 */
    public static final int MAX_HISTORY = 20;

    // ─────────────────────────────────────────────
    // プレイヤーごとのデータ (UUID → 各データ)
    // ─────────────────────────────────────────────

    private static final Map<UUID, BuildMode>                            playerModes = new HashMap<>();
    private static final Map<UUID, Deque<Map<BlockPos, BlockState>>>     undoStacks  = new HashMap<>();
    private static final Map<UUID, Deque<Map<BlockPos, BlockState>>>     redoStacks  = new HashMap<>();

    // ─────────────────────────────────────────────
    // ビルドモード
    // ─────────────────────────────────────────────

    public static void setMode(UUID uuid, BuildMode mode) {
        playerModes.put(uuid, mode);
    }

    public static BuildMode getMode(UUID uuid) {
        return playerModes.getOrDefault(uuid, BuildMode.NORMAL);
    }

    // ─────────────────────────────────────────────
    // Undo / Redo スタック操作
    // ─────────────────────────────────────────────

    /**
     * ブロック配置操作をUndoスタックに積む。
     * 「Undoしたときにこれらの座標を空気に戻す」という情報を保存する。
     *
     * @param uuid      プレイヤーUUID
     * @param positions 配置したブロックの座標リスト
     */
    public static void pushPlaceUndo(UUID uuid, List<BlockPos> positions,
                                     net.minecraft.server.level.ServerLevel level) {
        if (positions.isEmpty()) return;
        // 配置前の状態（=AIR）をスナップショットとして保存
        // ※ このメソッドは配置「後」に呼ばれるため、現在は配置済み状態だが
        //   Undoで戻す先は AIR なので AIR を記録する
        Map<BlockPos, BlockState> snapshot = new LinkedHashMap<>();
        for (BlockPos pos : positions) {
            snapshot.put(pos.immutable(), net.minecraft.world.level.block.Blocks.AIR.defaultBlockState());
        }
        pushUndo(uuid, snapshot);
    }

    /**
     * ブロック削除操作をUndoスタックに積む。
     * 「Undoしたときにこれらの座標を元の状態に戻す」という情報を保存する。
     *
     * @param uuid     プレイヤーUUID
     * @param snapshot 削除前のブロック状態 { pos → 元のBlockState }
     */
    public static void pushEraseUndo(UUID uuid, Map<BlockPos, BlockState> snapshot) {
        if (snapshot.isEmpty()) return;
        pushUndo(uuid, snapshot);
    }

    /**
     * Undo を実行する。
     * スタックから取り出したマップの通りにブロックを復元する。
     * 復元前の状態を Redo スタックに積む。
     *
     * @param uuid  プレイヤーUUID
     * @param level サーバーレベル
     * @return Undo が実行されたか
     */
    public static boolean performUndo(UUID uuid, net.minecraft.server.level.ServerLevel level) {
        Deque<Map<BlockPos, BlockState>> undo = undoStacks.get(uuid);
        if (undo == null || undo.isEmpty()) return false;

        Map<BlockPos, BlockState> restore = undo.pop();

        // Redo 用：復元前の現在状態をキャプチャ
        Map<BlockPos, BlockState> redoSnapshot = new LinkedHashMap<>();
        for (BlockPos pos : restore.keySet()) {
            redoSnapshot.put(pos, level.getBlockState(pos));
        }
        getRedoStack(uuid).push(redoSnapshot);

        // 復元実行
        for (Map.Entry<BlockPos, BlockState> e : restore.entrySet()) {
            level.setBlock(e.getKey(), e.getValue(), 3);
        }
        return true;
    }

    /**
     * Redo を実行する。
     * スタックから取り出したマップの通りにブロックを復元する。
     * 復元前の状態を Undo スタックに積む。
     *
     * @param uuid  プレイヤーUUID
     * @param level サーバーレベル
     * @return Redo が実行されたか
     */
    public static boolean performRedo(UUID uuid, net.minecraft.server.level.ServerLevel level) {
        Deque<Map<BlockPos, BlockState>> redo = redoStacks.get(uuid);
        if (redo == null || redo.isEmpty()) return false;

        Map<BlockPos, BlockState> restore = redo.pop();

        // Undo 用：復元前の現在状態をキャプチャ
        Map<BlockPos, BlockState> undoSnapshot = new LinkedHashMap<>();
        for (BlockPos pos : restore.keySet()) {
            undoSnapshot.put(pos, level.getBlockState(pos));
        }
        getUndoStack(uuid).push(undoSnapshot);

        // 復元実行
        for (Map.Entry<BlockPos, BlockState> e : restore.entrySet()) {
            level.setBlock(e.getKey(), e.getValue(), 3);
        }
        return true;
    }

    // ─────────────────────────────────────────────
    // プレイヤーログアウト時のクリーンアップ
    // ─────────────────────────────────────────────

    public static void onPlayerLogout(UUID uuid) {
        playerModes.remove(uuid);
        undoStacks.remove(uuid);
        redoStacks.remove(uuid);
    }

    // ─────────────────────────────────────────────
    // 内部ヘルパー
    // ─────────────────────────────────────────────

    private static void pushUndo(UUID uuid, Map<BlockPos, BlockState> snapshot) {
        Deque<Map<BlockPos, BlockState>> undo = getUndoStack(uuid);
        undo.push(new LinkedHashMap<>(snapshot));
        while (undo.size() > MAX_HISTORY) undo.pollLast();
        // 新しい操作が積まれたら Redo をクリア
        getRedoStack(uuid).clear();
    }

    private static Deque<Map<BlockPos, BlockState>> getUndoStack(UUID uuid) {
        return undoStacks.computeIfAbsent(uuid, k -> new ArrayDeque<>());
    }

    private static Deque<Map<BlockPos, BlockState>> getRedoStack(UUID uuid) {
        return redoStacks.computeIfAbsent(uuid, k -> new ArrayDeque<>());
    }
}
