package com.breakinblocks.bbchat.core;

import javax.annotation.Nullable;

/**
 * Events that the game fires that the relay should handle.
 * To be implemented by the relay as a service.
 */
public interface RelayService {
    /**
     * For use by the game.
     */
    public static final RelayService INSTANCE = RelayUtils.loadSingleService(RelayService.class);

    /**
     * Server is starting.
     */
    void onStarting();

    /**
     * Server has started.
     */
    void onStarted();

    /**
     * Server is going to stop.
     */
    void onStopping();

    /**
     * Server has stopped.
     */
    void onStopped();

    /**
     * Player sent a chat message.
     *
     * @param name Player's name
     * @param text The message they sent
     */
    void onChat(String name, String text);

    /**
     * Player has logged into the server.
     *
     * @param name Player's name
     */
    void onLogin(String name);

    /**
     * Player has logged out of the server.
     *
     * @param name Player's name.
     */
    void onLogout(String name);

    /**
     * Player has gained an achievement or advancement that is displayable.
     *
     * @param name        Player's name.
     * @param title       Title of the achievement or advancement.
     * @param description Description of the achievement or advancement.
     */
    void onAchievement(String name, String title, String description);

    /**
     * Named entity has died.
     *
     * @param message Death message.
     * @param target  The name of the entity that died.
     * @param source  The name of the entity responsible for the death of the {@code target}.
     */
    void onDeath(String message, String target, @Nullable String source);
}
