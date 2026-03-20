package com.example.effortlesslite.build;

/**
 * ビルドモード列挙型
 * NORMAL  : 通常の1ブロック配置
 * LINE    : 始点→終点の直線
 * WALL    : 始点→終点の垂直な壁
 * FLOOR   : 始点→終点の水平な床
 * CUBE    : 始点→終点の立方体 (全埋め)
 */
public enum BuildMode {
    NORMAL("Normal"),
    LINE("Line"),
    WALL("Wall"),
    FLOOR("Floor"),
    CUBE("Cube");

    private final String displayName;

    BuildMode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    /** 次のモードに循環切り替え */
    public BuildMode next() {
        BuildMode[] values = values();
        return values[(ordinal() + 1) % values.length];
    }
}
