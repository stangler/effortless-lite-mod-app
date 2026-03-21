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
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.InputEvent;
import net.neoforged.neoforge.event.tick.LevelTickEvent;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;

@EventBusSubscriber(modid = EffortlessLite.MOD_ID, bus = EventBusSubscriber.Bus.GAME, value = Dist.CLIENT)
public class ClientBuildHandler {

    public static final double EXTENDED_REACH = 32.0;

    /** PreviewRenderer から参照するプレビューブロックリスト */
    public static List<BlockPos> previewBlocks = List.of();

    @SubscribeEvent
    public static void onClientTick(LevelTickEvent.Post event) {
        DimensionDisplayState.INSTANCE.tick();

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null) {
            previewBlocks = List.of();
            return;
        }

        if (BuildState.isFirstPositionSet() && BuildState.mode != BuildMode.NORMAL) {
            HitResult hit = mc.player.pick(EXTENDED_REACH, 0.0f, false);
            if (hit.getType() == HitResult.Type.BLOCK) {
                BlockHitResult blockHit = (BlockHitResult) hit;
                BlockPos cursorPos = blockHit.getBlockPos().relative(blockHit.getDirection());

                BlockPos playerPos = mc.player.blockPosition();
                previewBlocks = BlockCalculator.calculate(
                        BuildState.mode,
                        BuildState.firstPos,
                        cursorPos,
                        BuildState.mirror,
                        playerPos);

                DimensionDisplayState.INSTANCE.updatePreview(BuildState.firstPos, cursorPos);
            }
        } else {
            previewBlocks = List.of();
        }
    }

    @SubscribeEvent
    public static void onKeyInput(InputEvent.Key event) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.screen != null)
            return;

        if (ModKeyBindings.MODE_TOGGLE.consumeClick()) {
            BuildState.cycleMode();
            PacketDistributor.sendToServer(new SyncModePacket(BuildState.mode.ordinal()));
            DimensionDisplayState.INSTANCE.clearPreview();
            sendHudMessage(mc, "§fBuild Mode: §e" + BuildState.mode.getDisplayName());
        }

        if (ModKeyBindings.MIRROR_TOGGLE.consumeClick()) {
            BuildState.cycleMirror();
            sendHudMessage(mc, "§fMirror: §e" + BuildState.mirror.getDisplayName());
        }

        if (ModKeyBindings.UNDO.consumeClick()) {
            PacketDistributor.sendToServer(new UndoPacket());
            sendHudMessage(mc, "§eUndo");
        }

        if (ModKeyBindings.REDO.consumeClick()) {
            PacketDistributor.sendToServer(new RedoPacket());
            sendHudMessage(mc, "§eRedo");
        }

        if (ModKeyBindings.CANCEL.consumeClick()) {
            if (BuildState.isFirstPositionSet()) {
                BuildState.reset();
                DimensionDisplayState.INSTANCE.clearPreview();
                sendHudMessage(mc, "§cBuild cancelled");
            }
        }
    }

    @SubscribeEvent
    public static void onInteractionKey(InputEvent.InteractionKeyMappingTriggered event) {
        if (!event.isUseItem() || event.getHand() != InteractionHand.MAIN_HAND)
            return;

        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null)
            return;

        // ★ 修正: 手持ちアイテムが BlockItem でない場合はバニラに任せる
        boolean holdingBlock = mc.player.getMainHandItem().getItem() instanceof BlockItem;

        if (BuildState.mode == BuildMode.NORMAL) {
            // BlockItem でない場合はキャンセルせずバニラ処理に流す
            if (!holdingBlock) return;

            event.setCanceled(true);
            event.setSwingHand(false);
            HitResult hit = mc.player.pick(EXTENDED_REACH, 0.0f, false);
            if (hit.getType() != HitResult.Type.BLOCK)
                return;
            BlockHitResult blockHit = (BlockHitResult) hit;
            BlockPos targetPos = blockHit.getBlockPos().relative(blockHit.getDirection());
            PacketDistributor.sendToServer(new PlaceBlocksPacket(List.of(targetPos)));
            DimensionDisplayState.INSTANCE.onPlaced(targetPos, targetPos);
            sendHudMessage(mc, "§a1 ブロックを配置しました");
            return;
        }

        // Normal 以外のモードも同様に BlockItem でない場合はバニラに任せる
        if (!holdingBlock) return;

        event.setCanceled(true);
        event.setSwingHand(false);

        HitResult hit = mc.player.pick(EXTENDED_REACH, 0.0f, false);
        if (hit.getType() != HitResult.Type.BLOCK)
            return;

        BlockHitResult blockHit = (BlockHitResult) hit;
        BlockPos targetPos = blockHit.getBlockPos().relative(blockHit.getDirection());

        processBuildClick(mc, targetPos);
    }

    private static void processBuildClick(Minecraft mc, BlockPos targetPos) {
        if (!BuildState.isFirstPositionSet()) {
            BuildState.firstPos = targetPos;
            sendHudMessage(mc, "§aStart: §f" + targetPos.toShortString()
                    + " §7| 次のクリックで終点を設定");
        } else {
            BlockPos playerPos = mc.player.blockPosition();
            List<BlockPos> positions = BlockCalculator.calculate(
                    BuildState.mode,
                    BuildState.firstPos,
                    targetPos,
                    BuildState.mirror,
                    playerPos);

            if (positions.isEmpty()) {
                sendHudMessage(mc, "§c配置するブロックがありません");
                return;
            }

            PacketDistributor.sendToServer(new PlaceBlocksPacket(positions));
            DimensionDisplayState.INSTANCE.onPlaced(BuildState.firstPos, targetPos);
            sendHudMessage(mc, "§a" + positions.size() + " ブロックを配置しました");

            BuildState.reset();
        }
    }

    private static void sendHudMessage(Minecraft mc, String message) {
        if (mc.player != null) {
            mc.player.displayClientMessage(Component.literal(message), true);
        }
    }
}
