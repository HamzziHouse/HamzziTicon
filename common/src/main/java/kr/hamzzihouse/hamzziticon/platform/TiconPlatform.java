package kr.hamzzihouse.hamzziticon.platform;

import org.jetbrains.annotations.NotNull;

import java.nio.file.Path;

public interface TiconPlatform {

    @NotNull
    String loaderName();

    @NotNull
    String modVersion();

    @NotNull
    Path configDirectory();

    boolean canSendToServer();

    void sendToServer(byte @NotNull [] payload);

    void runOnClientThread(@NotNull Runnable task);
}