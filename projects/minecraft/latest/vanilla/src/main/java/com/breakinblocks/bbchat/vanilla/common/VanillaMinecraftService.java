package com.breakinblocks.bbchat.vanilla.common;

import com.breakinblocks.bbchat.core.MinecraftService;

import java.util.Collection;
import java.util.stream.Collectors;

public abstract class VanillaMinecraftService implements MinecraftService {
    @Override
    public String getBotToken() {
        return BBChatConfig.COMMON.botToken.get();
    }

    @Override
    public String getChannelId() {
        return BBChatConfig.COMMON.channelId.get();
    }

    @Override
    public String getStaffRoleId() {
        return BBChatConfig.COMMON.staffRoleId.get();
    }

    @Override
    public String getCommandPrefix() {
        return BBChatConfig.COMMON.commandPrefix.get();
    }

    @Override
    public Collection<String> getAnyCommands() {
        return BBChatConfig.COMMON.anyCommands.get().stream().map(String::toString).collect(Collectors.toList());
    }
}
