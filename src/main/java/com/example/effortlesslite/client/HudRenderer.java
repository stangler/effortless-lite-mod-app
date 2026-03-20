package com.example.effortlesslite.client;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.build.BuildMode;
import com.example.effortlesslite.build.BuildState;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

/**
 * 画面左上にビルドモードのステータスを表示するHUDレンダラー
 *
 * 表示内容 (NOMALモード以外の場合):
 *   [Effortless Lite] <モード名> | Mirror: <ミラー軸>
 *   Begin: 右クリックで始点を指定  (始点未設定時)
 *   Start: X,Y,Z → 右クリックで終点を指定  (始点設定済み時)
 *
 * 表示位置: 画面左上 (x=8, y=8から下方向に描画)
 */
@EventBusSubscriber(modid = EffortlessLite.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT)
public class HudRenderer {

    /** HUD背景の塗りつぶし色 (半透明の黒) */
    private static final int BG_COLOR = 0x88000000;

    /** テキスト色 */
    private static final int TEXT_COLOR = 0xFFFFFF;

    /** HUDの左マージン */
    private static final int MARGIN_X = 8;

    /** HUDの上マージン */
    private static final int MARGIN_Y = 8;

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiLayerEvent.Post event) {
        // VanillaGuiLayers.CHAT_PANEL の後に描画する
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;

        // NOMALモードは非表示
        if (BuildState.mode == BuildMode.NORMAL) return;

        GuiGraphics graphics = event.getGuiGraphics();
        int screenWidth = mc.getWindow().getGuiScaledWidth();

        // 表示する文字列を構築
        String line1 = "§f[EL] §e" + BuildState.mode.getDisplayName()
                + " §7| Mirror: §b" + BuildState.mirror.getDisplayName();
        String line2 = BuildState.isFirstPositionSet()
                ? "§aStart: §f" + BuildState.firstPos.toShortString() + " §7→ §b右クリックで終点指定"
                : "§7右クリックで始点を指定  §8[Escape: キャンセル]";

        // 文字列の幅を計算 (背景サイズに使用)
        int w1 = mc.font.width(net.minecraft.network.chat.Component.literal(line1));
        int w2 = mc.font.width(net.minecraft.network.chat.Component.literal(line2));
        int boxWidth = Math.max(w1, w2) + 8;
        int boxHeight = 9 + 2 + 9 + 6; // lineHeight + gap + lineHeight + padding

        // 半透明背景
        graphics.fill(MARGIN_X - 3, MARGIN_Y - 2,
                MARGIN_X + boxWidth, MARGIN_Y + boxHeight, BG_COLOR);

        // テキスト描画
        graphics.drawString(mc.font, line1, MARGIN_X, MARGIN_Y, TEXT_COLOR);
        graphics.drawString(mc.font, line2, MARGIN_X, MARGIN_Y + 11, TEXT_COLOR);
    }
}
