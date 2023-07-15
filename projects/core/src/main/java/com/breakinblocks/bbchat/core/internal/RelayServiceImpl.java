package com.breakinblocks.bbchat.core.internal;

import com.breakinblocks.bbchat.core.api.MinecraftService;
import com.breakinblocks.bbchat.core.api.RelayService;
import org.jetbrains.annotations.Nullable;

public class RelayServiceImpl implements RelayService {
    private RelayEndpoint endpoint = DummyRelay.INSTANCE;

    @Override
    public void onStarting() {
        MinecraftService minecraft = MinecraftService.INSTANCE;
        endpoint = ChatRelay.create(
                minecraft.getBotToken(),
                minecraft.getGuildId(),
                minecraft.getChannelId(),
                minecraft.getStaffRoleId(),
                minecraft.getCommandPrefix(),
                minecraft.getAnyCommands(),
                MinecraftService.INSTANCE::onMessage,
                MinecraftService.INSTANCE::getPlayerCountInfo,
                minecraft
        );
    }

    @Override
    public void onStarted() {
        endpoint.onStarted();
    }

    @Override
    public void onStopping() {
    }

    @Override
    public void onStopped() {
        endpoint.onStopped();
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
