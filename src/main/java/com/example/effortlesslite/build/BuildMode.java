package com.example.effortlesslite.build;

public enum BuildMode {
    NORMAL,
    LINE,
    WALL,
    FLOOR,
    CUBE,
    ERASE_NORMAL,
    ERASE_LINE,
    ERASE_WALL,
    ERASE_FLOOR,
    ERASE_CUBE;

    /** 削除モードかどうか */
    public boolean isErase() {
        return this.ordinal() >= ERASE_NORMAL.ordinal();
    }

    /**
     * 対応するベースモードを返す。
     * 削除モードでも BlockCalculator の計算はベースモードで行う。
     */
    public BuildMode toBaseMode() {
        return switch (this) {
            case ERASE_NORMAL -> NORMAL;
            case ERASE_LINE   -> LINE;
            case ERASE_WALL   -> WALL;
            case ERASE_FLOOR  -> FLOOR;
            case ERASE_CUBE   -> CUBE;
            default           -> this;
        };
    }

    /** Gキーで次のモードへ進む */
    public BuildMode next() {
        BuildMode[] values = BuildMode.values();
        return values[(this.ordinal() + 1) % values.length];
    }

    /** HUD・ログ表示用の名前（既存コードの getDisplayName() に合わせる） */
    public String getDisplayName() {
        return switch (this) {
            case NORMAL       -> "Normal";
            case LINE         -> "Line";
            case WALL         -> "Wall";
            case FLOOR        -> "Floor";
            case CUBE         -> "Cube";
            case ERASE_NORMAL -> "削除: Normal";
            case ERASE_LINE   -> "削除: Line";
            case ERASE_WALL   -> "削除: Wall";
            case ERASE_FLOOR  -> "削除: Floor";
            case ERASE_CUBE   -> "削除: Cube";
        };
    }
}
