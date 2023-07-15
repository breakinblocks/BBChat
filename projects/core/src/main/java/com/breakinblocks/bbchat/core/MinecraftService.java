package com.breakinblocks.bbchat.core;

import java.util.Collection;
import java.util.function.Consumer;

/**
 * Events that the relay fires that the game should handle.
 * To be implemented by the game as a service.
 */
public interface MinecraftService {
    /**
     * For use by core.
     */
    MinecraftService INSTANCE = RelayUtils.loadSingleService(MinecraftService.class);

    String getBotToken();

    String getChannelId();

    String getStaffRoleId();

    String getCommandPrefix();

    Collection<String> getAnyCommands();

    /**
     * Send a message to all players.
     *
     * @param message Message to send
     */
    void onMessage(String message);

    /**
     * Execute a command on the server.
     *
     * @param isStaff     If the user has a staff role
     * @param name        Identifying name for the user
     * @param displayName Display name of the user
     * @param fullCommand Command to execute
     * @param response    For sending command output
     */
    void onCommand(boolean isStaff, String name, String displayName, String fullCommand, Consumer<String> response);
}
