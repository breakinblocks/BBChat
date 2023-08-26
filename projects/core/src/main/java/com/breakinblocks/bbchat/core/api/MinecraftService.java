package com.breakinblocks.bbchat.core.api;

/**
 * Events that the relay fires that the game should handle.
 * To be implemented by the game as a service and supplied to {@link RelayService#setMinecraftService(MinecraftService)}.
 */
public interface MinecraftService extends MinecraftServiceCommandHandler {
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
