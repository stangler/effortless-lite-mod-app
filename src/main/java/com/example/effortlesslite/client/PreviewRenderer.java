package com.example.effortlesslite.client;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.build.BlockCalculator;
import com.example.effortlesslite.build.BuildMode;
import com.example.effortlesslite.build.BuildState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

import java.util.Collections;
import java.util.List;

/**
 * ブロックプレビュー (ゴーストブロック) のレンダラー
 *
 * 描画内容:
 *  - 始点未設定時 : プレイヤーが見ているブロックに白いアウトライン
 *  - 始点設定済み : 配置予定のブロック全てに水色のアウトライン
 *  - 始点マーカー : オレンジのアウトライン
 *
 * 描画タイミング: AFTER_TRANSLUCENT_BLOCKS (半透明ブロックの後)
 */
@EventBusSubscriber(modid = EffortlessLite.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT)
public class PreviewRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        // NORMAL モードでは何も描画しない
        if (BuildState.mode == BuildMode.NORMAL) return;

        // プレビュー座標リストを取得
        List<BlockPos> previewPositions = getPreviewPositions(mc);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        // カメラ座標系に変換 (Minecraftのレンダリング基準)
        var camPos = mc.gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumer lineConsumer = mc.renderBuffers()
                .bufferSource()
                .getBuffer(RenderType.lines());

        // 始点マーカーを描画 (オレンジ)
        if (BuildState.isFirstPositionSet()) {
            AABB firstBox = new AABB(BuildState.firstPos).inflate(0.005);
            LevelRenderer.renderLineBox(poseStack, lineConsumer, firstBox,
                    1.0f, 0.5f, 0.0f, 1.0f); // RGBA: オレンジ
        }

        // プレビューブロックを描画 (水色)
        for (BlockPos pos : previewPositions) {
            // 始点は既に描画済みなのでスキップ
            if (BuildState.isFirstPositionSet() && pos.equals(BuildState.firstPos)) continue;

            AABB box = new AABB(pos).inflate(0.002);
            LevelRenderer.renderLineBox(poseStack, lineConsumer, box,
                    0.3f, 0.9f, 1.0f, 0.7f); // RGBA: 水色
        }

        // バッファをフラッシュ
        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());

        poseStack.popPose();
    }

    /**
     * プレビュー表示するブロック座標リストを取得する
     *
     * - 始点未設定: プレイヤーが向いているブロック1つ (ハイライト)
     * - 始点設定済み: 計算された全配置予定ブロック
     */
    private static List<BlockPos> getPreviewPositions(Minecraft mc) {
        HitResult hitResult = mc.player.pick(ClientBuildHandler.EXTENDED_REACH, 0.0f, false);

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return Collections.emptyList();
        }

        BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();

        if (!BuildState.isFirstPositionSet()) {
            // 始点未設定 → ターゲットブロックのみハイライト
            return List.of(targetPos);
        } else {
            // 始点設定済み → 全配置予定ブロックを表示
            BlockPos playerPos = mc.player.blockPosition();
            return BlockCalculator.calculate(
                    BuildState.mode,
                    BuildState.firstPos,
                    targetPos,
                    BuildState.mirror,
                    playerPos
            );
        }
    }
}
