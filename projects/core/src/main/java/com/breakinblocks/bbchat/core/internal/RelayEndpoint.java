package com.breakinblocks.bbchat.core.internal;

import javax.annotation.Nullable;

public interface RelayEndpoint {
    void cleanup();

    void onStarted();

    void onStopped();

    void onChat(String name, String text);

    void onLogin(String name);

    void onLogout(String name);

    void onAchievement(String name, String title, String description);

    void onDeath(String message, String target, @Nullable String source);
}
