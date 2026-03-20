package com.example.effortlesslite;

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import org.slf4j.Logger;

/**
 * Effortless Lite - メインModクラス
 *
 * 機能一覧:
 *  - ビルドモード: Normal / Line / Wall / Floor / Cube
 *  - ミラー機能: X軸 / Z軸 / XZ軸
 *  - Undo / Redo (最大20操作)
 *  - 拡張リーチ (32ブロック)
 *  - リアルタイムブロックプレビュー
 *
 * キーバインド (デフォルト):
 *  [G]       : ビルドモード切り替え
 *  [H]       : ミラー軸切り替え
 *  [Z]       : Undo
 *  [Y]       : Redo
 *  [Escape]  : キャンセル
 *
 * 使い方:
 *  1. [G]でモードを選択する
 *  2. 右クリックで始点を設定
 *  3. 右クリックで終点を設定 → ブロック一括配置
 */
@Mod(EffortlessLite.MOD_ID)
public class EffortlessLite {

    public static final String MOD_ID = "effortlesslite";
    public static final Logger LOGGER = LogUtils.getLogger();

    public EffortlessLite(IEventBus modEventBus, ModContainer modContainer) {
        // @EventBusSubscriber アノテーションで各クラスが自動登録される
        LOGGER.info("[Effortless Lite] Mod loaded!");
    }
}
