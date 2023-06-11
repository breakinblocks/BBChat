package com.breakinblocks.bbchat.core;

import javax.annotation.Nullable;

public class DummyRelay implements Relay {
    public static final DummyRelay INSTANCE = new DummyRelay();

    private DummyRelay() {

    }

    @Override
    public void cleanup() {

    }

    @Override
    public void onStarted() {

    }

    @Override
    public void onStopped() {

    }

    @Override
    public void onChat(String name, String text) {

    }

    @Override
    public void onLogin(String name) {

    }

    @Override
    public void onLogout(String name) {

    }

    @Override
    public void onAchievement(String name, String title, String description) {

    }

    @Override
    public void onDeath(String message, String target, @Nullable String source) {

    }
}
