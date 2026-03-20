package com.example.effortlesslite.build;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * ビルドモードに応じたブロック座標リストを計算するクラス
 *
 * 各モードの挙動:
 *  LINE  : 始点と終点を結ぶ直線上のブロック (最長軸に沿って補間)
 *  WALL  : 始点と終点の間の垂直な壁 (水平距離が大きい軸方向に展開)
 *  FLOOR : 始点と終点で囲まれる水平な床 (Y座標は始点に固定)
 *  CUBE  : 始点と終点で囲まれるバウンディングボックス全体
 */
public class BlockCalculator {

    /** 一度に配置できるブロックの最大数 (負荷対策) */
    public static final int MAX_BLOCKS = 512;

    /**
     * ブロック座標リストを計算する
     *
     * @param mode      ビルドモード
     * @param start     始点
     * @param end       終点
     * @param mirror    ミラー軸
     * @param playerPos ミラーの中心となるプレイヤー座標
     * @return 配置するブロック座標のリスト
     */
    public static List<BlockPos> calculate(BuildMode mode, BlockPos start, BlockPos end,
                                           MirrorAxis mirror, BlockPos playerPos) {
        List<BlockPos> positions = new ArrayList<>();

        switch (mode) {
            case LINE  -> calculateLine(positions, start, end);
            case WALL  -> calculateWall(positions, start, end);
            case FLOOR -> calculateFloor(positions, start, end);
            case CUBE  -> calculateCube(positions, start, end);
            default    -> positions.add(start);
        }

        // ミラー処理
        if (mirror != MirrorAxis.NONE) {
            applyMirror(positions, mirror, playerPos);
        }

        // 上限チェック
        if (positions.size() > MAX_BLOCKS) {
            return positions.subList(0, MAX_BLOCKS);
        }

        return positions;
    }

    // ─────────────────────────────────────────────
    // ビルドモード別の計算ロジック
    // ─────────────────────────────────────────────

    /** LINE: 始点→終点を結ぶ直線 */
    private static void calculateLine(List<BlockPos> positions, BlockPos start, BlockPos end) {
        int dx = end.getX() - start.getX();
        int dy = end.getY() - start.getY();
        int dz = end.getZ() - start.getZ();
        int steps = Math.max(Math.max(Math.abs(dx), Math.abs(dy)), Math.abs(dz));

        if (steps == 0) {
            positions.add(start);
            return;
        }

        Set<BlockPos> seen = new HashSet<>();
        for (int i = 0; i <= steps; i++) {
            int x = start.getX() + (int) Math.round((double) dx * i / steps);
            int y = start.getY() + (int) Math.round((double) dy * i / steps);
            int z = start.getZ() + (int) Math.round((double) dz * i / steps);
            BlockPos pos = new BlockPos(x, y, z);
            if (seen.add(pos)) positions.add(pos);
        }
    }

    /**
     * WALL: 始点と終点の間の垂直な壁
     * 水平距離 (dx vs dz) が大きい軸方向に展開し、Y方向にも展開する
     */
    private static void calculateWall(List<BlockPos> positions, BlockPos start, BlockPos end) {
        int minY = Math.min(start.getY(), end.getY());
        int maxY = Math.max(start.getY(), end.getY());

        int dx = Math.abs(end.getX() - start.getX());
        int dz = Math.abs(end.getZ() - start.getZ());

        if (dx >= dz) {
            // X軸方向に展開 (Z = 始点のZ座標固定)
            int minX = Math.min(start.getX(), end.getX());
            int maxX = Math.max(start.getX(), end.getX());
            for (int x = minX; x <= maxX; x++) {
                for (int y = minY; y <= maxY; y++) {
                    positions.add(new BlockPos(x, y, start.getZ()));
                }
            }
        } else {
            // Z軸方向に展開 (X = 始点のX座標固定)
            int minZ = Math.min(start.getZ(), end.getZ());
            int maxZ = Math.max(start.getZ(), end.getZ());
            for (int z = minZ; z <= maxZ; z++) {
                for (int y = minY; y <= maxY; y++) {
                    positions.add(new BlockPos(start.getX(), y, z));
                }
            }
        }
    }

    /** FLOOR: 始点と終点で囲まれた水平な床 (Y = 始点のY座標) */
    private static void calculateFloor(List<BlockPos> positions, BlockPos start, BlockPos end) {
        int y = start.getY();
        int minX = Math.min(start.getX(), end.getX());
        int maxX = Math.max(start.getX(), end.getX());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxZ = Math.max(start.getZ(), end.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int z = minZ; z <= maxZ; z++) {
                positions.add(new BlockPos(x, y, z));
            }
        }
    }

    /** CUBE: 始点と終点で囲まれるバウンディングボックス全体を埋める */
    private static void calculateCube(List<BlockPos> positions, BlockPos start, BlockPos end) {
        int minX = Math.min(start.getX(), end.getX());
        int maxX = Math.max(start.getX(), end.getX());
        int minY = Math.min(start.getY(), end.getY());
        int maxY = Math.max(start.getY(), end.getY());
        int minZ = Math.min(start.getZ(), end.getZ());
        int maxZ = Math.max(start.getZ(), end.getZ());

        for (int x = minX; x <= maxX; x++) {
            for (int y = minY; y <= maxY; y++) {
                for (int z = minZ; z <= maxZ; z++) {
                    positions.add(new BlockPos(x, y, z));
                }
            }
        }
    }

    // ─────────────────────────────────────────────
    // ミラー処理
    // ─────────────────────────────────────────────

    /**
     * ミラー処理: プレイヤー座標を中心に既存ブロック座標を反転して追加する
     *
     * ミラー方向:
     *  X軸ミラー → Z座標を反転 (東西対称)
     *  Z軸ミラー → X座標を反転 (南北対称)
     *  XZ軸ミラー → X・Z両方を反転 + 斜め方向にも追加
     */
    private static void applyMirror(List<BlockPos> positions, MirrorAxis mirror, BlockPos playerPos) {
        int baseCount = positions.size();
        Set<BlockPos> existing = new HashSet<>(positions);

        for (int i = 0; i < baseCount; i++) {
            BlockPos pos = positions.get(i);

            if (mirror == MirrorAxis.X || mirror == MirrorAxis.XZ) {
                // Z座標を反転 (プレイヤーのZ座標を中心)
                BlockPos mZ = new BlockPos(pos.getX(), pos.getY(),
                        2 * playerPos.getZ() - pos.getZ());
                if (existing.add(mZ)) positions.add(mZ);
            }

            if (mirror == MirrorAxis.Z || mirror == MirrorAxis.XZ) {
                // X座標を反転 (プレイヤーのX座標を中心)
                BlockPos mX = new BlockPos(2 * playerPos.getX() - pos.getX(),
                        pos.getY(), pos.getZ());
                if (existing.add(mX)) positions.add(mX);
            }

            if (mirror == MirrorAxis.XZ) {
                // X・Z両方反転 (対角ミラー)
                BlockPos mXZ = new BlockPos(2 * playerPos.getX() - pos.getX(),
                        pos.getY(), 2 * playerPos.getZ() - pos.getZ());
                if (existing.add(mXZ)) positions.add(mXZ);
            }
        }
    }
}
