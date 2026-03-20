package com.example.effortlesslite.network;

import com.example.effortlesslite.EffortlessLite;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.network.event.RegisterPayloadHandlersEvent;
import net.neoforged.neoforge.network.registration.PayloadRegistrar;

/**
 * ネットワークパケットの登録
 * 全パケットは クライアント→サーバー 方向のみ
 */
@EventBusSubscriber(modid = EffortlessLite.MOD_ID,
        bus = EventBusSubscriber.Bus.MOD)
public class ModNetwork {

    @SubscribeEvent
    public static void onRegisterPayloadHandlers(RegisterPayloadHandlersEvent event) {
        PayloadRegistrar registrar = event.registrar("1");

        // ブロック一括配置
        registrar.playToServer(
                PlaceBlocksPacket.TYPE,
                PlaceBlocksPacket.STREAM_CODEC,
                PlaceBlocksPacket::handle
        );

        // Undo
        registrar.playToServer(
                UndoPacket.TYPE,
                UndoPacket.STREAM_CODEC,
                UndoPacket::handle
        );

        // Redo
        registrar.playToServer(
                RedoPacket.TYPE,
                RedoPacket.STREAM_CODEC,
                RedoPacket::handle
        );

        // ビルドモード同期 (拡張リーチのため)
        registrar.playToServer(
                SyncModePacket.TYPE,
                SyncModePacket.STREAM_CODEC,
                SyncModePacket::handle
        );
    }
}
