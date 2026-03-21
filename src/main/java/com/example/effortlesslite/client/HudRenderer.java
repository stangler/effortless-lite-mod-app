package com.example.effortlesslite.client;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.build.BuildMode;
import com.example.effortlesslite.build.BuildState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = EffortlessLite.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT)
public class HudRenderer {

    private static final int BG_COLOR      = 0x88000000;
    private static final int BG_DIM_COLOR  = 0xCC003344; // 寸法表示時の背景（濃いシアン系）
    private static final int TEXT_COLOR    = 0xFFFFFF;
    private static final int MARGIN_X      = 8;
    private static final int MARGIN_Y      = 8;

    @SubscribeEvent
    public static void onRenderGuiOverlay(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.options.hideGui) return;
        if (BuildState.mode == BuildMode.NORMAL) return;

        GuiGraphics graphics = event.getGuiGraphics();
        DimensionDisplayState dim = DimensionDisplayState.INSTANCE;

        // --- 既存: モード・状態の2行 ---
        String line1 = "§f[EL] §e" + BuildState.mode.getDisplayName()
                + " §7| Mirror: §b" + BuildState.mirror.getDisplayName();
        String line2 = BuildState.isFirstPositionSet()
                ? "§aStart: §f" + BuildState.firstPos.toShortString() + " §7→ §b右クリックで終点指定"
                : "§7右クリックで始点を指定  §8[Escape: キャンセル]";

        int w1 = mc.font.width(net.minecraft.network.chat.Component.literal(line1));
        int w2 = mc.font.width(net.minecraft.network.chat.Component.literal(line2));
        int maxW = Math.max(w1, w2);

        // 通常の2行背景
        int boxHeight = 11 + 11 + 4;
        graphics.fill(MARGIN_X - 3, MARGIN_Y - 2,
                MARGIN_X + maxW + 8, MARGIN_Y + boxHeight, BG_COLOR);
        graphics.drawString(mc.font, line1, MARGIN_X, MARGIN_Y, TEXT_COLOR);
        graphics.drawString(mc.font, line2, MARGIN_X, MARGIN_Y + 11, TEXT_COLOR);

        // --- 追加: 寸法表示ブロック（目立つ別枠） ---
        if (dim.shouldDisplay()) {
            int sX = dim.getSizeX();
            int sY = dim.getSizeY();
            int sZ = dim.getSizeZ();
            float alpha = dim.getFadeAlpha();

            // 「横 × 縦」形式のメイン寸法テキスト
            String dimMain;
            if (sY > 1) {
                dimMain = sX + "ブロック × " + sZ + "ブロック × 高さ" + sY;
            } else {
                dimMain = sX + "ブロック × " + sZ + "ブロック";
            }
            String dimSub = "合計 " + (sX * sY * sZ) + " ブロック";

            int dimY = MARGIN_Y + boxHeight + 6; // 通常HUDの直下

            // フェードアウト用アルファ計算
            int bgAlpha  = (int)(0xCC * alpha) << 24;
            int bgColor  = bgAlpha | 0x003344;
            int txtAlpha = (int)(0xFF * alpha) << 24;

            int wMain = mc.font.width(net.minecraft.network.chat.Component.literal(dimMain));
            int wSub  = mc.font.width(net.minecraft.network.chat.Component.literal(dimSub));
            int dimBoxW = Math.max(wMain, wSub) + 10;
            int dimBoxH = 11 + 11 + 6;

            // 目立つ背景（シアン系）
            graphics.fill(MARGIN_X - 3, dimY - 2,
                    MARGIN_X + dimBoxW, dimY + dimBoxH, bgColor);

            // 上辺にラインを引いてさらに強調
            graphics.fill(MARGIN_X - 3, dimY - 2,
                    MARGIN_X + dimBoxW, dimY - 1, txtAlpha | 0x00FFFF);

            // メイン寸法テキスト（明るいシアン）
            int mainColor = txtAlpha | 0x00FFFF;
            graphics.drawString(mc.font, dimMain, MARGIN_X, dimY, mainColor, true);

            // サブテキスト（薄いグレー）
            int subColor = txtAlpha | 0xAAAAAA;
            graphics.drawString(mc.font, dimSub, MARGIN_X, dimY + 11, subColor, true);

            // プレビュー中は点滅マーカーを追加
            if (dim.isInPreview()) {
                long tick = System.currentTimeMillis() / 500;
                if (tick % 2 == 0) {
                    graphics.drawString(mc.font, "▶", MARGIN_X + dimBoxW + 2, dimY + 2,
                            txtAlpha | 0x00FFFF, true);
                }
            }
        }
    }
}
