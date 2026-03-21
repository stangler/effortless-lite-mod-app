package com.example.effortlesslite.network;

import com.example.effortlesslite.server.ServerBuildHandler;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.PacketDistributor;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

@EventBusSubscriber(modid = "effortlesslite", bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {

    @SubscribeEvent
    public static void onRegisterPayloads(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // ブロック配置
        registrar.playToServer(
                PlaceBlocksPacket.TYPE,
                PlaceBlocksPacket.STREAM_CODEC,
                (packet, ctx) -> packet.handle(ctx));

        // ブロック削除（新規追加）
        registrar.playToServer(
                DeleteBlocksPacket.TYPE,
                DeleteBlocksPacket.STREAM_CODEC,
                (packet, ctx) -> packet.handle(ctx));

        // Undo
        registrar.playToServer(
                UndoPacket.TYPE,
                UndoPacket.STREAM_CODEC,
                (packet, ctx) -> packet.handle(ctx));

        // Redo
        registrar.playToServer(
                RedoPacket.TYPE,
                RedoPacket.STREAM_CODEC,
                (packet, ctx) -> packet.handle(ctx));

        // モード同期
        registrar.playToServer(
                SyncModePacket.TYPE,
                SyncModePacket.STREAM_CODEC,
                (packet, ctx) -> packet.handle(ctx));
    }

    /** クライアント → サーバーへパケット送信 */
    public static <T extends net.minecraft.network.protocol.common.custom.CustomPacketPayload> void sendToServer(T packet) {
        PacketDistributor.sendToServer(packet);
    }
}
