package com.example.effortlesslite.server;

import com.example.effortlesslite.EffortlessLite;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.event.entity.player.PlayerEvent;

/**
 * サーバーサイドのプレイヤーイベントハンドラ
 *
 * 処理内容:
 *  - ログアウト時: 拡張リーチ属性を削除 & メモリ上のデータをクリーンアップ
 *  - ログイン時: 念のためリーチ属性をリセット (サーバー再起動直後の対応)
 */
@EventBusSubscriber(modid = EffortlessLite.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME)
public class ServerEventHandler {

    /** プレイヤーログアウト時の後処理 */
    @SubscribeEvent
    public static void onPlayerLogout(PlayerEvent.PlayerLoggedOutEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 拡張リーチ属性モディファイアを削除
        var reachAttr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (reachAttr != null) {
            reachAttr.removeModifier(PlayerBuildData.REACH_MODIFIER_ID);
        }

        // メモリ上のデータをクリーンアップ
        PlayerBuildData.onPlayerLogout(player.getUUID());

        EffortlessLite.LOGGER.debug("[Effortless Lite] Cleaned up data for: {}",
                player.getName().getString());
    }

    /** プレイヤーログイン時の初期化 (念のためリセット) */
    @SubscribeEvent
    public static void onPlayerLogin(PlayerEvent.PlayerLoggedInEvent event) {
        if (!(event.getEntity() instanceof ServerPlayer player)) return;

        // 万が一モディファイアが残っていたら削除する
        var reachAttr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
        if (reachAttr != null) {
            reachAttr.removeModifier(PlayerBuildData.REACH_MODIFIER_ID);
        }
    }
}
