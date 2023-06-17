package com.breakinblocks.bbchat.core;

import java.util.function.Consumer;

@FunctionalInterface
public interface MinecraftService {
    /**
     * Execute a command on the server.
     *
     * @param isStaff     If the user has a staff role
     * @param name        Identifying name for the user
     * @param displayName Display name of the user
     * @param fullCommand Command to execute
     * @param response    For sending command output
     */
    void handleCommand(boolean isStaff, String name, String displayName, String fullCommand, Consumer<String> response);
}
