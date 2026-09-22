package kr.hamzzihouse.hamzziticon.net;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;
import org.jetbrains.annotations.NotNull;

public record TiconPayload(byte @NotNull [] data) implements CustomPacketPayload {

    public static final CustomPacketPayload.Type<TiconPayload> TYPE =
            new CustomPacketPayload.Type<>(Identifier.parse(TiconProtocol.CHANNEL));

    public static final StreamCodec<FriendlyByteBuf, TiconPayload> CODEC = CustomPacketPayload.codec(
            (payload, buffer) -> buffer.writeBytes(payload.data()),
            buffer -> {
                byte[] data = new byte[buffer.readableBytes()];
                buffer.readBytes(data);
                return new TiconPayload(data);
            });

    @Override
    @NotNull
    public CustomPacketPayload.Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}