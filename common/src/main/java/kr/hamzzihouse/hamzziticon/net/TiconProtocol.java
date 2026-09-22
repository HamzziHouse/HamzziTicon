package kr.hamzzihouse.hamzziticon.net;

import lombok.experimental.UtilityClass;

import java.util.regex.Pattern;

@UtilityClass
public class TiconProtocol {

    public final String CHANNEL = "hamzziticon:sync";

    public final byte VERSION = 1;

    public final byte C2S_HELLO = 0x00;

    public final byte S2C_CATALOG = 0x00;

    public final byte S2C_UP_TO_DATE = 0x01;

    public final byte S2C_DISABLED = 0x02;

    public final int NO_REVISION = -1;

    public final int MAX_STRING_LENGTH = 256;
    public final int MAX_REASON_LENGTH = 64;
    public final Pattern ID_PATTERN = Pattern.compile("[a-z0-9_]{2,32}");
    public final Pattern RESOURCE_PATH_PATTERN = Pattern.compile("[a-z0-9/._-]*");
    public final Pattern FONT_PATTERN = Pattern.compile("[a-z0-9_.-]+:[a-z0-9/._-]+");
    public final int MAX_CATEGORIES = 64;
    public final int MAX_TICONS = 4096;
    public final int MAX_ALIASES = 16;
}