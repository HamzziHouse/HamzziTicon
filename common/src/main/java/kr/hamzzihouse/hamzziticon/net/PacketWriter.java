package kr.hamzzihouse.hamzziticon.net;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;

public final class PacketWriter {

    private final ByteArrayOutputStream out = new ByteArrayOutputStream(64);

    @NotNull
    public PacketWriter writeByte(int value) {
        out.write(value);
        return this;
    }

    @NotNull
    public PacketWriter writeBoolean(boolean value) {
        return writeByte(value ? 1 : 0);
    }

    @NotNull
    public PacketWriter writeVarInt(int value) {
        int remaining = value;
        while ((remaining & ~0x7F) != 0) {
            out.write((remaining & 0x7F) | 0x80);
            remaining >>>= 7;
        }
        out.write(remaining);
        return this;
    }

    @NotNull
    public PacketWriter writeString(@NotNull String value) {
        byte[] bytes = value.getBytes(StandardCharsets.UTF_8);
        writeVarInt(bytes.length);
        out.writeBytes(bytes);
        return this;
    }

    public byte @NotNull [] toByteArray() {
        return out.toByteArray();
    }
}