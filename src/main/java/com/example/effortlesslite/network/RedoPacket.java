package com.example.effortlesslite.network;

import com.example.effortlesslite.EffortlessLite;
import com.example.effortlesslite.server.ServerBuildHandler;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** クライアント→サーバー: Redo パケット (データなし) */
public record RedoPacket() implements CustomPacketPayload {

    public static final Type<RedoPacket> TYPE = new Type<>(
            ResourceLocation.fromNamespaceAndPath(EffortlessLite.MOD_ID, "redo")
    );

    public static final StreamCodec<ByteBuf, RedoPacket> STREAM_CODEC =
            StreamCodec.unit(new RedoPacket());

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public void handle(IPayloadContext context) {
        context.enqueueWork(() -> ServerBuildHandler.handleRedo(context));
    }
}
