package com.example.effortlesslite.client;

import com.example.effortlesslite.build.BuildMode;
import com.example.effortlesslite.build.BuildState;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderGuiLayerEvent;
import net.neoforged.neoforge.client.gui.VanillaGuiLayers;

@EventBusSubscriber(modid = "effortlesslite", value = Dist.CLIENT)
public class HudRenderer {

    @SubscribeEvent
    public static void onRenderGuiLayer(RenderGuiLayerEvent.Post event) {
        if (!event.getName().equals(VanillaGuiLayers.HOTBAR)) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;
        if (mc.options.hideGui) return;

        BuildMode mode = BuildState.mode;
        boolean isErase = mode.isErase();

        GuiGraphics graphics = event.getGuiGraphics();
        int x = 6;
        int y = 6;
        int lineHeight = 10;

        // ---- 行1: タイトル + モード名 ----
        // 削除モードは赤文字、配置モードはシアン文字
        String modeText = isErase
                ? "§c" + mode.getDisplayName()
                : "§b" + mode.getDisplayName();
        String mirrorText = "§7| Mirror: " + BuildState.mirror.getDisplayName();
        graphics.drawString(mc.font, "§f[EL] " + modeText + " " + mirrorText,
                x, y, 0xFFFFFFFF, false);
        y += lineHeight;

        // ---- 行2: 操作ヒント ----
        if (!BuildState.isFirstPositionSet()) {
            String action = (mode == BuildMode.NORMAL || mode == BuildMode.ERASE_NORMAL)
                    ? (isErase ? "右クリックで1ブロック削除" : "右クリックで1ブロック配置")
                    : "右クリックで始点を指定";
            graphics.drawString(mc.font,
                    "§7" + action + "  §8[Escape: キャンセル]",
                    x, y, 0xFFFFFFFF, false);
        } else {
            graphics.drawString(mc.font,
                    "§7右クリックで終点を指定" + (isErase ? "（削除確定）" : "") + "  §8[Escape: キャンセル]",
                    x, y, 0xFFFFFFFF, false);
        }
        y += lineHeight;

        // ---- 仕切りライン ----
        int lineColor = isErase ? 0xFFCC0000 : 0xFF00CCCC;
        graphics.fill(x, y, x + 140, y + 1, lineColor);
        y += 4;

        // ---- 行3: 寸法表示 ----
        DimensionDisplayState dim = DimensionDisplayState.INSTANCE;
        if (dim.shouldDisplay()) {
            float alpha = dim.getFadeAlpha();
            int a = Math.max(1, (int) (alpha * 255));

            // getSizeX/Y/Z() を使う（getDimensions()は存在しない）
            int dx = dim.getSizeX();
            int dy = dim.getSizeY();
            int dz = dim.getSizeZ();
            String dimText = formatDimText(dx, dy, dz);

            // プレビュー中は点滅矢印を付ける
            boolean previewing = BuildState.isFirstPositionSet();
            String arrow = previewing ? " §e▶" : "";

            int dimColor = (a << 24) | 0xFFFFFF;
            graphics.drawString(mc.font, dimText + arrow, x, y, dimColor, false);
            y += lineHeight;

            int total = dx * dy * dz;
            String totalLabel = isErase
                    ? "§c合計 " + total + " ブロック削除"
                    : "§f合計 " + total + " ブロック";
            int totalColor = (a << 24) | (isErase ? 0xFF4444 : 0xFFFFFF);
            graphics.drawString(mc.font, totalLabel, x, y, totalColor, false);
        }
    }

    /** 寸法テキストのフォーマット（1の軸は省略） */
    private static String formatDimText(int dx, int dy, int dz) {
        if (dy == 1 && dz == 1) return dx + " ブロック";
        if (dy == 1)             return dx + "ブロック × " + dz + "ブロック";
        if (dz == 1)             return dx + "ブロック × " + dy + "ブロック";
        return dx + " × " + dy + " × " + dz + " ブロック";
    }
}
