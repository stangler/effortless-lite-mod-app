package com.example.effortlesslite.server;

import com.example.effortlesslite.build.BuildMode;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;

import java.util.*;

/**
 * サーバー側でプレイヤーごとのビルドデータを管理するクラス
 *
 * 保持データ:
 *  - 現在のビルドモード
 *  - Undo スタック (最大20操作)
 *  - Redo スタック
 *
 * データはサーバーのメモリ上に保持し、ログアウト時に破棄される。
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

    private static final Map<UUID, BuildMode>              playerModes    = new HashMap<>();
    private static final Map<UUID, Deque<List<BlockPos>>>  undoStacks     = new HashMap<>();
    private static final Map<UUID, Deque<List<BlockPos>>>  redoStacks     = new HashMap<>();

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
     * 操作履歴をUndoスタックに積む。
     * Redoスタックはクリアされる (新しい操作をするとRedoは消える)。
     *
     * @param uuid      プレイヤーUUID
     * @param positions 配置したブロックの座標リスト (後でUndoで削除する)
     */
    public static void pushUndo(UUID uuid, List<BlockPos> positions) {
        Deque<List<BlockPos>> undo = undoStacks.computeIfAbsent(uuid, k -> new ArrayDeque<>());
        undo.push(new ArrayList<>(positions));
        // スタックがあふれたら一番古い操作を削除
        while (undo.size() > MAX_HISTORY) undo.pollLast();
        // 新しい操作をしたのでRedoをクリア
        getRedoStack(uuid).clear();
    }

    /**
     * Undo: 最後に配置したブロック群の座標を取り出す。
     * 取り出した座標はRedoスタックに積まれる。
     *
     * @return 削除すべきブロック座標リスト。スタックが空の場合は null
     */
    public static List<BlockPos> popUndo(UUID uuid) {
        Deque<List<BlockPos>> undo = undoStacks.get(uuid);
        if (undo == null || undo.isEmpty()) return null;
        List<BlockPos> positions = undo.pop();
        getRedoStack(uuid).push(new ArrayList<>(positions));
        return positions;
    }

    /**
     * Redo: Undoで取り消した操作の座標を取り出す。
     * 取り出した座標はUndoスタックに再度積まれる。
     *
     * @return 再配置すべきブロック座標リスト。スタックが空の場合は null
     */
    public static List<BlockPos> popRedo(UUID uuid) {
        Deque<List<BlockPos>> redo = redoStacks.get(uuid);
        if (redo == null || redo.isEmpty()) return null;
        List<BlockPos> positions = redo.pop();
        getUndoStack(uuid).push(new ArrayList<>(positions));
        return positions;
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

    private static Deque<List<BlockPos>> getUndoStack(UUID uuid) {
        return undoStacks.computeIfAbsent(uuid, k -> new ArrayDeque<>());
    }

    private static Deque<List<BlockPos>> getRedoStack(UUID uuid) {
        return redoStacks.computeIfAbsent(uuid, k -> new ArrayDeque<>());
    }
}
