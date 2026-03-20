package com.example.effortlesslite.build;

/**
 * ミラー軸列挙型
 * NONE : ミラーなし
 * X    : プレイヤー位置を中心にZ方向へミラー (東西対称)
 * Z    : プレイヤー位置を中心にX方向へミラー (南北対称)
 * XZ   : 両軸ミラー (4方向対称)
 */
public enum MirrorAxis {
    NONE("None"),
    X("X-Axis"),
    Z("Z-Axis"),
    XZ("X+Z Axes");

    private final String displayName;

    MirrorAxis(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 次のミラー軸に循環切り替え */
    public MirrorAxis next() {
        MirrorAxis[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
