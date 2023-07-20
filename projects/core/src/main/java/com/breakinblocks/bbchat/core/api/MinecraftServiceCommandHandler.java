package com.breakinblocks.bbchat.core.api;

import java.util.function.Consumer;

/**
 * Temporary measure to get things to build during porting.
 * TODO: Remove.
 */
@FunctionalInterface
public interface MinecraftServiceCommandHandler {
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
