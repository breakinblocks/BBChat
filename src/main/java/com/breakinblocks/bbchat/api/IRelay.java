package com.breakinblocks.bbchat.api;

public interface IRelay {
    void cleanup();

    void onStarted();

    void onStopped();

    void onChat(String name, String text);

    void onLogin(String name);

    void onLogout(String name);

    void onAchievement(String name, String title, String description);
}
