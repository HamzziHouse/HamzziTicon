package kr.hamzzihouse.hamzziticon.net;

import org.jetbrains.annotations.NotNull;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public final class PacketReader {

    private final ByteBuffer buffer;

    public PacketReader(byte @NotNull [] data) {
        this.buffer = ByteBuffer.wrap(data).asReadOnlyBuffer();
    }

    public byte readByte() {
        require(Byte.BYTES);
        return buffer.get();
    }

    public boolean readBoolean() {
        return readByte() != 0;
    }

    public int readVarInt() {
        int result = 0;
        for (int shift = 0; shift < 35; shift += 7) {
            byte current = readByte();
            result |= (current & 0x7F) << shift;
            if ((current & 0x80) == 0) {
                return result;
            }
        }
        throw new MalformedPacketException("VarInt 가 5바이트를 넘었습니다.");
    }

    @NotNull
    public String readString() {
        return readString(TiconProtocol.MAX_STRING_LENGTH);
    }

    @NotNull
    public String readString(int maxLength) {
        int length = readVarInt();
        if (length < 0 || length > maxLength * 4) {
            throw new MalformedPacketException("문자열 길이가 허용 범위를 벗어났습니다: " + length);
        }
        require(length);
        byte[] bytes = new byte[length];
        buffer.get(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    public int readCount(int limit, @NotNull String what) {
        int count = readVarInt();
        if (count < 0 || count > limit) {
            throw new MalformedPacketException(what + " 개수가 허용 범위를 벗어났습니다: " + count + " (최대 " + limit + ")");
        }
        return count;
    }

    private void require(int bytes) {
        if (buffer.remaining() < bytes) {
            throw new MalformedPacketException(
                    "패킷이 잘렸습니다. " + bytes + "바이트가 필요한데 " + buffer.remaining() + "바이트만 남았습니다.");
        }
    }

    public static final class MalformedPacketException extends RuntimeException {

        public MalformedPacketException(@NotNull String message) {
            super(message);
        }
    }
}