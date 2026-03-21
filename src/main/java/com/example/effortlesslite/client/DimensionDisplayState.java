package com.example.effortlesslite.client;

import net.minecraft.core.BlockPos;

/**
 * 寸法表示の状態を管理するクライアント側シングルトン。
 * プレビュー中と配置後（タイマー）の両方で寸法を保持する。
 */
public class DimensionDisplayState {

    public static final DimensionDisplayState INSTANCE = new DimensionDisplayState();

    /** 配置後に寸法を表示し続けるティック数（20tick = 1秒） */
    private static final int DISPLAY_TICKS = 60; // 3秒

    // 表示する寸法値（ブロック数）
    private int sizeX = 0;
    private int sizeY = 0;
    private int sizeZ = 0;

    // 3D表示用の範囲コーナー
    private BlockPos corner1 = null;
    private BlockPos corner2 = null;

    // プレビュー中かどうか
    private boolean inPreview = false;

    // 配置後タイマー（0になったら非表示）
    private int displayTimer = 0;

    private DimensionDisplayState() {}

    // -------------------------------------------------------
    // プレビュー中の更新（ClientBuildHandler から毎フレーム呼ぶ）
    // -------------------------------------------------------

    /** プレビュー中に呼ぶ。毎フレーム更新される。 */
    public void updatePreview(BlockPos pos1, BlockPos pos2) {
        this.corner1 = pos1;
        this.corner2 = pos2;
        this.sizeX = Math.abs(pos2.getX() - pos1.getX()) + 1;
        this.sizeY = Math.abs(pos2.getY() - pos1.getY()) + 1;
        this.sizeZ = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        this.inPreview = true;
    }

    /** プレビュー終了時（キャンセル時など）に呼ぶ */
    public void clearPreview() {
        this.inPreview = false;
        // タイマーが残っていれば表示は継続される
    }

    // -------------------------------------------------------
    // 配置確定時（ClientBuildHandler から呼ぶ）
    // -------------------------------------------------------

    /** ブロック配置確定時に呼ぶ。タイマーをリセットして表示を継続する。 */
    public void onPlaced(BlockPos pos1, BlockPos pos2) {
        this.corner1 = pos1;
        this.corner2 = pos2;
        this.sizeX = Math.abs(pos2.getX() - pos1.getX()) + 1;
        this.sizeY = Math.abs(pos2.getY() - pos1.getY()) + 1;
        this.sizeZ = Math.abs(pos2.getZ() - pos1.getZ()) + 1;
        this.inPreview = false;
        this.displayTimer = DISPLAY_TICKS;
    }

    // -------------------------------------------------------
    // ティック更新（ClientTickEvent から呼ぶ）
    // -------------------------------------------------------

    public void tick() {
        if (displayTimer > 0) {
            displayTimer--;
        }
    }

    // -------------------------------------------------------
    // 描画判定
    // -------------------------------------------------------

    /** 寸法を表示すべきか（プレビュー中 OR タイマー残り） */
    public boolean shouldDisplay() {
        return (inPreview || displayTimer > 0) && corner1 != null && corner2 != null;
    }

    /** タイマー残量を 0.0〜1.0 で返す（フェードアウト用） */
    public float getFadeAlpha() {
        if (inPreview) return 1.0f;
        return Math.min(1.0f, displayTimer / 20.0f); // 最後の1秒でフェード
    }

    // -------------------------------------------------------
    // Getter
    // -------------------------------------------------------

    public int getSizeX() { return sizeX; }
    public int getSizeY() { return sizeY; }
    public int getSizeZ() { return sizeZ; }
    public BlockPos getCorner1() { return corner1; }
    public BlockPos getCorner2() { return corner2; }
    public boolean isInPreview() { return inPreview; }
}
