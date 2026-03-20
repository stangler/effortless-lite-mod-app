package com.example.effortlesslite.network;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.server.ServerBuildHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * クライアント→サーバー: ブロック配置パケット
 *
 * クライアントが計算したブロック座標リストをサーバーに送り、
 * サーバー側でパーミッションチェック・実際の配置を行う。
 */
public record PlaceBlocksPacket(List<BlockPos> positions) implements CustomPacketPayload {

    public static final Type<PlaceBlocksPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EffortlessLite.MOD_ID, "place_blocks")
    );

    public static final StreamCodec<ByteBuf, PlaceBlocksPacket> STREAM_CODEC = StreamCodec.composite(
            BlockPos.STREAM_CODEC.apply(ByteBufCodecs.list()),
            PlaceBlocksPacket::positions,
            PlaceBlocksPacket::new
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> ServerBuildHandler.handlePlaceBlocks(this, context));
    }
}
