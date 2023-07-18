package com.breakinblocks.bbchat.core.internal;

import org.jetbrains.annotations.Nullable;

public class ProxyRelay implements RelayEndpoint {
    private RelayEndpoint endpoint = DummyRelay.INSTANCE;
    private boolean isServerRunning = false;
    private final Object isServerRunningLock = new Object();

    void setEndpoint(RelayEndpoint endpoint) {
        synchronized (isServerRunningLock) {
            RelayEndpoint oldEndpoint = this.endpoint;
            this.endpoint = endpoint;
            oldEndpoint.cleanup();
            if (isServerRunning) {
                endpoint.onStarted();
            }
        }
    }

    @Override
    public void cleanup() {
        setEndpoint(DummyRelay.INSTANCE);
    }

    @Override
    public void onStarted() {
        synchronized (isServerRunningLock) {
            endpoint.onStarted();
            isServerRunning = true;
        }
    }

    @Override
    public void onStopped() {
        synchronized (isServerRunningLock) {
            isServerRunning = false;
            endpoint.onStopped();
        }
    }

    @Override
    public void onChat(String name, String text) {
        endpoint.onChat(name, text);
    }

    @Override
    public void onLogin(String name) {
        endpoint.onLogin(name);
    }

    @Override
    public void onLogout(String name) {
        endpoint.onLogout(name);
    }

    @Override
    public void onAchievement(String name, String title, String description) {
        endpoint.onAchievement(name, title, description);
    }

    @Override
    public void onDeath(String message, String target, @Nullable String source) {
        endpoint.onDeath(message, target, source);
    }
}
