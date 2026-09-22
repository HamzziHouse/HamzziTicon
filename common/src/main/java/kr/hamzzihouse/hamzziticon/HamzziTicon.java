package kr.hamzzihouse.hamzziticon;

import lombok.experimental.UtilityClass;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@UtilityClass
public class HamzziTicon {

    public final String MOD_ID = "hamzziticon";

    public final String MOD_NAME = "HamzziTicon";

    private final Logger LOGGER = LoggerFactory.getLogger(MOD_NAME);

    @NotNull
    public Logger logger() {
        return LOGGER;
    }
}