package com.example.effortlesslite.network;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.build.BuildMode;
import com.example.effortlesslite.server.PlayerBuildData;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * クライアント→サーバー: ビルドモード同期パケット
 *
 * クライアントでビルドモードが変更された際にサーバーへ通知する。
 * サーバーはモードに応じて BLOCK_INTERACTION_RANGE 属性を変更し、
 * 拡張リーチを有効/無効にする。
 */
public record SyncModePacket(int modeOrdinal) implements CustomPacketPayload {

    public static final Type<SyncModePacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EffortlessLite.MOD_ID, "sync_mode")
    );

    public static final StreamCodec<ByteBuf, SyncModePacket> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.INT,
            SyncModePacket::modeOrdinal,
            SyncModePacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> {
            ServerPlayer player = (ServerPlayer) context.player();
            BuildMode[] modes = BuildMode.values();
            BuildMode mode = (modeOrdinal >= 0 && modeOrdinal < modes.length)
                    ? modes[modeOrdinal] : BuildMode.NORMAL;

            PlayerBuildData.setMode(player.getUUID(), mode);

            // ─ 拡張リーチ属性の更新 ─
            // Minecraft 1.20.5+ のバニラ属性 BLOCK_INTERACTION_RANGE を使用
            var reachAttr = player.getAttribute(Attributes.BLOCK_INTERACTION_RANGE);
            if (reachAttr != null) {
                // 既存のモディファイアを削除してから再適用
                reachAttr.removeModifier(PlayerBuildData.REACH_MODIFIER_ID);
                if (mode != BuildMode.NORMAL) {
                    reachAttr.addTransientModifier(new AttributeModifier(
                            PlayerBuildData.REACH_MODIFIER_ID,
                            PlayerBuildData.EXTENDED_REACH_BONUS,
                            AttributeModifier.Operation.ADD_VALUE
                    ));
                }
            }

            EffortlessLite.LOGGER.debug("[Effortless Lite] Player {} switched to mode: {}",
                    player.getName().getString(), mode);
        });
    }
}
