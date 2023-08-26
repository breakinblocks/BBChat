package com.breakinblocks.bbchat.core.internal;

import com.breakinblocks.bbchat.core.api.MinecraftService;
import com.breakinblocks.bbchat.core.api.RelayService;
import org.jetbrains.annotations.Nullable;

public class RelayServiceImpl implements RelayService {
    private RelayEndpoint endpoint = DummyRelay.INSTANCE;
    private BBChatConfig config;
    private MinecraftService minecraft;

    @Override
    public void setMinecraftService(MinecraftService minecraftService) {
        minecraft = minecraftService;
    }

    @Override
    public void onStarting() {
        if (config == null) {
            config = new BBChatConfig();
        }

        endpoint = ChatRelay.create(
                config.botToken,
                config.guildId,
                config.channelId,
                config.staffRoleId,
                config.commandPrefix,
                config.anyCommands,
                minecraft::onMessage,
                minecraft::getPlayerCountInfo,
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
        endpoint.cleanup();
        endpoint = DummyRelay.INSTANCE;
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
