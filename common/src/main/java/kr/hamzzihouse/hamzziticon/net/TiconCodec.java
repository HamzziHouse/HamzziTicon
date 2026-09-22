package kr.hamzzihouse.hamzziticon.net;

import kr.hamzzihouse.hamzziticon.catalog.Ticon;
import kr.hamzzihouse.hamzziticon.catalog.TiconCategory;
import kr.hamzzihouse.hamzziticon.catalog.TiconCatalog;
import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class TiconCodec {

    public byte @NotNull [] encodeHello(@NotNull String modVersion, int cachedRevision) {
        return new PacketWriter()
                .writeByte(TiconProtocol.VERSION)
                .writeByte(TiconProtocol.C2S_HELLO)
                .writeString(modVersion)
                .writeVarInt(cachedRevision)
                .toByteArray();
    }

    @NotNull
    public ServerPacket decode(byte @NotNull [] payload) {
        PacketReader reader = new PacketReader(payload);

        byte version = reader.readByte();
        if (version != TiconProtocol.VERSION) {
            throw new PacketReader.MalformedPacketException(
                    "프로토콜 버전이 다릅니다. 서버 " + version + " / 모드 " + TiconProtocol.VERSION
                            + " — 모드를 최신 버전으로 업데이트해 주세요.");
        }

        byte packetId = reader.readByte();
        return switch (packetId) {
            case TiconProtocol.S2C_CATALOG -> new ServerPacket.Catalog(decodeCatalog(reader));
            case TiconProtocol.S2C_UP_TO_DATE -> new ServerPacket.UpToDate(reader.readVarInt());
            case TiconProtocol.S2C_DISABLED -> new ServerPacket.Disabled(reader.readString(TiconProtocol.MAX_REASON_LENGTH));
            default -> throw new PacketReader.MalformedPacketException("알 수 없는 패킷 번호입니다: " + packetId);
        };
    }

    public byte @NotNull [] encodeCatalog(@NotNull TiconCatalog catalog) {
        List<TiconCategory> categories = catalog.categories();

        PacketWriter writer = new PacketWriter()
                .writeByte(TiconProtocol.VERSION)
                .writeByte(TiconProtocol.S2C_CATALOG)
                .writeVarInt(catalog.revision())
                .writeString(catalog.font())
                .writeVarInt(categories.size());

        for (TiconCategory category : categories) {
            writer.writeString(category.id())
                    .writeString(category.displayName())
                    .writeString(category.iconTiconId() == null ? "" : category.iconTiconId())
                    .writeVarInt(category.order());
        }

        List<Ticon> ticons = catalog.ticons();
        writer.writeVarInt(ticons.size());

        for (Ticon ticon : ticons) {
            int categoryIndex = indexOfCategory(categories, ticon.categoryId());
            writer.writeString(ticon.id())
                    .writeString(ticon.displayName())
                    .writeString(ticon.character())
                    .writeVarInt(categoryIndex)
                    .writeString(ticon.texture())
                    .writeBoolean(ticon.locked())
                    .writeVarInt(ticon.aliases().size());
            for (String alias : ticon.aliases()) {
                writer.writeString(alias);
            }
        }

        return writer.toByteArray();
    }

    @NotNull
    private TiconCatalog decodeCatalog(@NotNull PacketReader reader) {
        int revision = reader.readVarInt();
        String font = reader.readString();

        int categoryCount = reader.readCount(TiconProtocol.MAX_CATEGORIES, "카테고리");
        List<TiconCategory> categories = new ArrayList<>(categoryCount);
        for (int index = 0; index < categoryCount; index++) {
            String id = reader.readString();
            String displayName = reader.readString();
            String iconTiconId = reader.readString();
            int order = reader.readVarInt();
            categories.add(new TiconCategory(id, displayName, iconTiconId, order));
        }

        int ticonCount = reader.readCount(TiconProtocol.MAX_TICONS, "티콘");
        List<Ticon> ticons = new ArrayList<>(ticonCount);
        for (int index = 0; index < ticonCount; index++) {
            String id = reader.readString();
            String displayName = reader.readString();
            String character = reader.readString();
            int categoryIndex = reader.readVarInt();
            String texture = reader.readString();
            boolean locked = reader.readBoolean();

            if (!TiconProtocol.ID_PATTERN.matcher(id).matches()) {
                throw new PacketReader.MalformedPacketException("티콘 id 형식이 잘못되었습니다: '" + id + "'");
            }
            if (!TiconProtocol.RESOURCE_PATH_PATTERN.matcher(texture).matches()) {
                throw new PacketReader.MalformedPacketException("티콘 '" + id + "' 의 텍스처 경로 형식이 잘못되었습니다.");
            }
            if (categoryIndex < 0 || categoryIndex >= categories.size()) {
                throw new PacketReader.MalformedPacketException(
                        "티콘 '" + id + "' 의 카테고리 인덱스가 범위를 벗어났습니다: " + categoryIndex);
            }

            int aliasCount = reader.readCount(TiconProtocol.MAX_ALIASES, "별칭");
            List<String> aliases = new ArrayList<>(aliasCount);
            for (int aliasIndex = 0; aliasIndex < aliasCount; aliasIndex++) {
                aliases.add(reader.readString());
            }

            ticons.add(new Ticon(id, displayName, character, categories.get(categoryIndex).id(), aliases, texture, locked));
        }

        return new TiconCatalog(revision, font, categories, ticons);
    }

    private int indexOfCategory(@NotNull List<TiconCategory> categories, @NotNull String categoryId) {
        for (int index = 0; index < categories.size(); index++) {
            if (categories.get(index).id().equals(categoryId)) {
                return index;
            }
        }
        throw new IllegalStateException("카탈로그에 없는 카테고리를 참조했습니다: " + categoryId);
    }
}