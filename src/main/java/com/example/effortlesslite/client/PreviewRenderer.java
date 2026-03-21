package com.example.effortlesslite.client;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.build.BlockCalculator;
import com.example.effortlesslite.build.BuildMode;
import com.example.effortlesslite.build.BuildState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import org.joml.Matrix4f;

import java.util.Collections;
import java.util.List;

@EventBusSubscriber(modid = EffortlessLite.MOD_ID,
        bus = EventBusSubscriber.Bus.GAME,
        value = Dist.CLIENT)
public class PreviewRenderer {

    @SubscribeEvent
    public static void onRenderLevelStage(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;

        if (BuildState.mode == BuildMode.NORMAL) return;

        List<BlockPos> previewPositions = getPreviewPositions(mc);

        PoseStack poseStack = event.getPoseStack();
        poseStack.pushPose();

        var camPos = mc.gameRenderer.getMainCamera().getPosition();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        VertexConsumer lineConsumer = mc.renderBuffers()
                .bufferSource()
                .getBuffer(RenderType.lines());

        // 始点マーカー (オレンジ)
        if (BuildState.isFirstPositionSet()) {
            AABB firstBox = new AABB(BuildState.firstPos).inflate(0.005);
            LevelRenderer.renderLineBox(poseStack, lineConsumer, firstBox,
                    1.0f, 0.5f, 0.0f, 1.0f);
        }

        // プレビューブロック (水色)
        for (BlockPos pos : previewPositions) {
            if (BuildState.isFirstPositionSet() && pos.equals(BuildState.firstPos)) continue;
            AABB box = new AABB(pos).inflate(0.002);
            LevelRenderer.renderLineBox(poseStack, lineConsumer, box,
                    0.3f, 0.9f, 1.0f, 0.7f);
        }

        mc.renderBuffers().bufferSource().endBatch(RenderType.lines());

        poseStack.popPose();

        // --- 追加: 3D寸法ラベル ---
        DimensionDisplayState dim = DimensionDisplayState.INSTANCE;
        if (dim.shouldDisplay()) {
            renderDimensionLabels(event, poseStack, dim, camPos.x, camPos.y, camPos.z);
        }
    }

    private static List<BlockPos> getPreviewPositions(Minecraft mc) {
        HitResult hitResult = mc.player.pick(ClientBuildHandler.EXTENDED_REACH, 0.0f, false);

        if (hitResult.getType() != HitResult.Type.BLOCK) {
            return Collections.emptyList();
        }

        BlockPos targetPos = ((BlockHitResult) hitResult).getBlockPos();

        if (!BuildState.isFirstPositionSet()) {
            return List.of(targetPos);
        } else {
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

    // -------------------------------------------------------
    // 3D寸法ラベル描画（追加）
    // -------------------------------------------------------

    private static void renderDimensionLabels(RenderLevelStageEvent event,
                                               PoseStack poseStack,
                                               DimensionDisplayState dim,
                                               double camX, double camY, double camZ) {
        Minecraft mc = Minecraft.getInstance();
        BlockPos c1 = dim.getCorner1();
        BlockPos c2 = dim.getCorner2();

        int minX = Math.min(c1.getX(), c2.getX());
        int minY = Math.min(c1.getY(), c2.getY());
        int minZ = Math.min(c1.getZ(), c2.getZ());
        int maxX = Math.max(c1.getX(), c2.getX()) + 1;
        int maxY = Math.max(c1.getY(), c2.getY()) + 1;
        int maxZ = Math.max(c1.getZ(), c2.getZ()) + 1;

        float alpha = dim.getFadeAlpha();
        int a = Math.max(0, Math.min(255, (int)(alpha * 255)));
        int textColor = (a << 24) | 0xFFFFFF;

        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();

        float midX = (minX + maxX) / 2.0f;
        float midY = minY + 0.5f;
        float midZ = (minZ + maxZ) / 2.0f;

        // X方向の長さ（南・北辺）
        String labelX = String.valueOf(dim.getSizeX());
        drawLabel(poseStack, bufferSource, event, midX, midY, minZ - 0.6f, labelX, textColor, camX, camY, camZ);
        drawLabel(poseStack, bufferSource, event, midX, midY, maxZ + 0.1f, labelX, textColor, camX, camY, camZ);

        // Z方向の長さ（西・東辺）
        String labelZ = String.valueOf(dim.getSizeZ());
        drawLabel(poseStack, bufferSource, event, minX - 0.6f, midY, midZ, labelZ, textColor, camX, camY, camZ);
        drawLabel(poseStack, bufferSource, event, maxX + 0.1f, midY, midZ, labelZ, textColor, camX, camY, camZ);

        // Y方向の長さ（高さ2以上のみ）
        if (dim.getSizeY() > 1) {
            float midY2 = (minY + maxY) / 2.0f;
            String labelY = String.valueOf(dim.getSizeY());
            drawLabel(poseStack, bufferSource, event, minX - 0.6f, midY2, minZ - 0.6f, labelY, textColor, camX, camY, camZ);
        }

        bufferSource.endBatch();
    }

    private static void drawLabel(PoseStack poseStack,
                                   MultiBufferSource bufferSource,
                                   RenderLevelStageEvent event,
                                   float wx, float wy, float wz,
                                   String text, int textColor,
                                   double camX, double camY, double camZ) {
        Minecraft mc = Minecraft.getInstance();
        poseStack.pushPose();

        poseStack.translate(wx - camX, wy - camY, wz - camZ);
        poseStack.mulPose(event.getCamera().rotation());

        float scale = 0.025f;
        poseStack.scale(-scale, -scale, scale);

        Matrix4f matrix = poseStack.last().pose();
        var font = mc.font;
        float tx = -font.width(text) / 2.0f;
        float ty = -font.lineHeight / 2.0f;

        font.drawInBatch(
                Component.literal(text),
                tx, ty,
                textColor,
                false,
                matrix,
                bufferSource,
                Font.DisplayMode.NORMAL,
                0x80000000,
                0xF000F0
        );

        poseStack.popPose();
    }
}
