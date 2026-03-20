package com.example.effortlesslite.client;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.build.BlockCalculator;
import com.example.effortlesslite.build.BuildMode;
import com.example.effortlesslite.build.BuildState;
import com.example.effortlesslite.network.PlaceBlocksPacket;
import com.example.effortlesslite.network.RedoPacket;
import com.example.effortlesslite.network.SyncModePacket;
import com.example.effortlesslite.network.UndoPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

/**
 * クライアント側のキー入力・インタラクションを処理するハンドラ
 *
 * 処理フロー:
 *  1. [G] キーでビルドモードを切り替え
 *  2. 右クリックで始点を設定 (画面に「始点設定済み」と表示)
 *  3. 右クリックで終点を設定 → ブロック座標を計算してサーバーへ送信
 *
 * 拡張リーチ (最大32ブロック):
 *  通常の右クリックをインターセプトし、カスタムレイキャストを実行する。
 *  これにより最大32ブロック先のブロックを指定できる。
 */
@EventBusSubscriber(modid = EffortlessLite.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT)
public class ClientBuildHandler {

    /** 拡張リーチの距離 (ブロック数) */
    public static final double EXTENDED_REACH = 32.0;

    // ─────────────────────────────────────────────
    // キー入力処理
    // ─────────────────────────────────────────────

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) return;

        // [G] ビルドモード切り替え
        if (ModKeyBindings.MODE_TOGGLE.consumeClick()) {
            BuildState.cycleMode();
            // サーバーにモード変更を通知 (拡張リーチ属性の更新のため)
            PacketDistributor.sendToServer(new SyncModePacket(BuildState.mode.ordinal()));
            sendHudMessage(mc, "§fBuild Mode: §e" + BuildState.mode.getDisplayName());
        }

        // [H] ミラー軸切り替え
        if (ModKeyBindings.MIRROR_TOGGLE.consumeClick()) {
            BuildState.cycleMirror();
            sendHudMessage(mc, "§fMirror: §e" + BuildState.mirror.getDisplayName());
        }

        // [Z] Undo
        if (ModKeyBindings.UNDO.consumeClick()) {
            PacketDistributor.sendToServer(new UndoPacket());
            sendHudMessage(mc, "§eUndo");
        }

        // [Y] Redo
        if (ModKeyBindings.REDO.consumeClick()) {
            PacketDistributor.sendToServer(new RedoPacket());
            sendHudMessage(mc, "§eRedo");
        }

        // [Escape] キャンセル
        if (ModKeyBindings.CANCEL.consumeClick()) {
            if (BuildState.isFirstPositionSet()) {
                BuildState.reset();
                sendHudMessage(mc, "§cBuild cancelled");
            }
        }
    }

    // ─────────────────────────────────────────────
    // 右クリック (Use Item キー) のインターセプト
    // ─────────────────────────────────────────────

    /**
     * 右クリックをインターセプトしてビルド処理を行う。
     * NORMAL モードの場合は通常の右クリックをそのまま通す。
     * それ以外のモードでは、カスタムレイキャスト(32ブロック)でターゲットを取得する。
     */
    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        // メインハンドの「使用」キーのみ処理
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // NORMAL モードは通常処理に任せる
        if (BuildState.mode == BuildMode.NORMAL) return;

        // イベントをキャンセルして独自処理を行う
        event.setCanceled(true);
        event.setSwingHand(false);

        // 拡張リーチによるレイキャスト
        HitResult hit = mc.player.pick(EXTENDED_REACH, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK) return;

        BlockPos targetPos = ((BlockHitResult) hit).getBlockPos();
        processBuildClick(mc, targetPos);
    }

    // ─────────────────────────────────────────────
    // ビルドロジック
    // ─────────────────────────────────────────────

    private static void processBuildClick(Minecraft mc, BlockPos targetPos) {
        if (!BuildState.isFirstPositionSet()) {
            // 始点を設定
            BuildState.firstPos = targetPos;
            sendHudMessage(mc, "§aStart: §f" + targetPos.toShortString()
                    + " §7| 次のクリックで終点を設定");
        } else {
            // 終点を設定してブロックを配置
            BlockPos playerPos = mc.player.blockPosition();
            List<BlockPos> positions = BlockCalculator.calculate(
                    BuildState.mode,
                    BuildState.firstPos,
                    targetPos,
                    BuildState.mirror,
                    playerPos
            );

            if (positions.isEmpty()) {
                sendHudMessage(mc, "§c配置するブロックがありません");
                return;
            }

            // サーバーへ送信
            PacketDistributor.sendToServer(new PlaceBlocksPacket(positions));
            sendHudMessage(mc, "§a" + positions.size() + " ブロックを配置しました");

            // 始点をリセット (次の操作に備える)
            BuildState.reset();
        }
    }

    // ─────────────────────────────────────────────
    // HUD メッセージ
    // ─────────────────────────────────────────────

    /** アクションバー (画面中央下) にメッセージを表示する */
    private static void sendHudMessage(Minecraft mc, String message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), true);
        }
    }
}
