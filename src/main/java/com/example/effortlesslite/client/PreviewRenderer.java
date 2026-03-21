package com.example.effortlesslite.client;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.build.BuildState;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;

@EventBusSubscriber(modid = EffortlessLite.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class PreviewRenderer {

    @SubscribeEvent
    public static void onRenderLevel(RenderLevelStageEvent event) {
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_TRANSLUCENT_BLOCKS) return;

        // ClientBuildHandler が毎ティック計算したプレビューブロックリストを使う
        if (ClientBuildHandler.previewBlocks.isEmpty()) return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) return;

        // 削除モード = 赤、配置モード = シアン
        boolean isErase = BuildState.mode.isErase();
        float r = isErase ? 1.0f : 0.0f;
        float g = isErase ? 0.0f : 1.0f;
        float b = isErase ? 0.0f : 1.0f;
        float alpha = 0.8f;

        Camera camera = event.getCamera();
        Vec3 camPos = camera.getPosition();

        PoseStack poseStack = event.getPoseStack();
        MultiBufferSource.BufferSource bufferSource = mc.renderBuffers().bufferSource();
        VertexConsumer consumer = bufferSource.getBuffer(RenderType.lines());

        poseStack.pushPose();
        poseStack.translate(-camPos.x, -camPos.y, -camPos.z);

        for (BlockPos pos : ClientBuildHandler.previewBlocks) {
            AABB box = new AABB(pos).inflate(0.002);
            LevelRenderer.renderLineBox(poseStack, consumer, box, r, g, b, alpha);
        }

        poseStack.popPose();
        bufferSource.endBatch(RenderType.lines());
    }
}
