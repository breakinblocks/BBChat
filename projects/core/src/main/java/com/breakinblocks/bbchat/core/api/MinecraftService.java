package com.breakinblocks.bbchat.core.api;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Events that the relay fires that the game should handle.
 * To be implemented by the game as a service and supplied to {@link RelayService#setMinecraftService(MinecraftService)}.
 */
public interface MinecraftService extends MinecraftServiceCommandHandler {
    String getBotToken();

    String getGuildId();

    String getChannelId();

    String getStaffRoleId();

    String getCommandPrefix();

    Collection<String> getAnyCommands();

    /**
     * Get info about the player count.
     *
     * @return Current and maximum players.
     */
    PlayerCountInfo getPlayerCountInfo();

    /**
     * Send a message to all players.
     *
     * @param message Message to send
     */
    void onMessage(String message);
}
