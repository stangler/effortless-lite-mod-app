package com.example.effortlesslite.network;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.server.ServerBuildHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** クライアント→サーバー: Undo パケット (データなし) */
public record UndoPacket() implements CustomPacketPayload {

    public static final Type<UndoPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EffortlessLite.MOD_ID, "undo")
    );

    public static final StreamCodec<ByteBuf, UndoPacket> STREAM_CODEC =
            StreamCodec.unit(new UndoPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> ServerBuildHandler.handleUndo(context));
    }
}
