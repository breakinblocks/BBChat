package com.breakinblocks.bbchat.core;

import org.jetbrains.annotations.Nullable;

public class ProxyRelay implements IRelay {
    private IRelay relay = DummyRelay.INSTANCE;
    private boolean isServerRunning = false;

    public void setRelay(IRelay relay) {
        this.relay = relay;
    }

    public boolean isServerRunning() {
        return isServerRunning;
    }

    @Override
    public void cleanup() {
        relay.cleanup();
    }

    @Override
    public void onStarted() {
        relay.onStarted();
        isServerRunning = true;
    }

    @Override
    public void onStopped() {
        isServerRunning = false;
        relay.onStopped();
    }

    @Override
    public void onChat(String name, String text) {
        relay.onChat(name, text);
    }

    @Override
    public void onLogin(String name) {
        relay.onLogin(name);
    }

    @Override
    public void onLogout(String name) {
        relay.onLogout(name);
    }

    @Override
    public void onAchievement(String name, String title, String description) {
        relay.onAchievement(name, title, description);
    }

    @Override
    public void onDeath(String message, String target, @Nullable String source) {
        relay.onDeath(message, target, source);
    }
}
