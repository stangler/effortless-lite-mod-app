package com.example.effortlesslite.build;

import net.minecraft.core.BlockPos;

import javax.annotation.Nullable;

/**
 * クライアント側のビルド状態を保持するクラス
 * シングルトン的に static フィールドで管理する
 */
public class BuildState {

    /** 現在のビルドモード */
    public static BuildMode mode = BuildMode.NORMAL;

    /** 現在のミラー軸 */
    public static MirrorAxis mirror = MirrorAxis.NONE;

    /** 始点 (null = まだ設定されていない) */
    @Nullable
    public static BlockPos firstPos = null;

    /** 始点が設定済みかどうか */
    public static boolean isFirstPositionSet() {
        return firstPos != null;
    }

    /** 始点をリセット (モード中断時) */
    public static void reset() {
        firstPos = null;
    }

    /** ビルドモードを次のモードに切り替える */
    public static void cycleMode() {
        mode = mode.next();
        reset(); // 始点もリセット
    }

    /** ミラー軸を次の軸に切り替える */
    public static void cycleMirror() {
        mirror = mirror.next();
    }

    /** HUDに表示するステータス文字列を生成する */
    public static String getStatusText() {
        if (mode == BuildMode.NORMAL) {
            return "§7[Effortless Lite] §fNormal Mode";
        }
        String mirrorStr = mirror == MirrorAxis.NONE ? "" : " §e| Mirror: " + mirror.getDisplayName();
        String posStr = isFirstPositionSet()
                ? " §a| Start: " + firstPos.toShortString() + " → §b右クリックで終点を指定"
                : " §7| §b右クリックで始点を指定";
        return "§7[Effortless Lite] §f" + mode.getDisplayName() + mirrorStr + posStr;
    }
}
