package com.breakinblocks.bbchat.quilt.common;

import net.minecraft.advancements.Advancement;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;

public final class BBChatQuiltEvents {
    private BBChatQuiltEvents() {
    }

    public static ServerStarting SERVER_STARTING;

    /**
     * Functional interface to be implemented on callbacks for {@link #SERVER_STARTING}.
     *
     * @see #SERVER_STARTING
     */
    @FunctionalInterface
    public interface ServerStarting {
        /**
         * Called when the server is starting.
         *
         * @param server Server instance.
         */
        void serverStarting(MinecraftServer server);
    }

    public static ServerStarted SERVER_STARTED;

    /**
     * Functional interface to be implemented on callbacks for {@link #SERVER_STARTED}.
     *
     * @see #SERVER_STARTED
     */
    @FunctionalInterface
    public interface ServerStarted {
        /**
         * Called when the server has started.
         */
        void serverStarted();
    }

    public static ServerStopping SERVER_STOPPING;

    /**
     * Functional interface to be implemented on callbacks for {@link #SERVER_STOPPING}.
     *
     * @see #SERVER_STOPPING
     */
    @FunctionalInterface
    public interface ServerStopping {
        /**
         * Called when the server is stopping.
         */
        void serverStopping();
    }

    public static ServerStopped SERVER_STOPPED;

    /**
     * Functional interface to be implemented on callbacks for {@link #SERVER_STOPPED}.
     *
     * @see #SERVER_STOPPED
     */
    @FunctionalInterface
    public interface ServerStopped {
        /**
         * Called when the server has stopped.
         */
        void serverStopped();
    }

    public static ChatMessageSent CHAT_MESSAGE_SENT;

    /**
     * Functional interface to be implemented on callbacks for {@link #CHAT_MESSAGE_SENT}.
     *
     * @see #CHAT_MESSAGE_SENT
     */
    @FunctionalInterface
    public interface ChatMessageSent {
        /**
         * Called when a player sends a chat message.
         *
         * @param player  that sent the {@code message}
         * @param message that was send by the {@code player}
         */
        void chatMessageSent(ServerPlayer player, String message);
    }

    public static PlayerLogin PLAYER_LOGIN;

    /**
     * Functional interface to be implemented on callbacks for {@link #PLAYER_LOGIN}.
     *
     * @see #PLAYER_LOGIN
     */
    @FunctionalInterface
    public interface PlayerLogin {
        /**
         * Called when a player logs in.
         *
         * @param player that logged in.
         */
        void playerLogin(Player player);
    }

    public static PlayerLogout PLAYER_LOGOUT;

    /**
     * Functional interface to be implemented on callbacks for {@link #PLAYER_LOGOUT}.
     *
     * @see #PLAYER_LOGOUT
     */
    @FunctionalInterface
    public interface PlayerLogout {
        /**
         * Called when a player logs out.
         *
         * @param player that logged out.
         */
        void playerLogout(Player player);
    }

    public static AdvancementGranted ADVANCEMENT_GRANTED;

    /**
     * Functional interface to be implemented on callbacks for {@link #ADVANCEMENT_GRANTED}.
     *
     * @see #ADVANCEMENT_GRANTED
     */
    @FunctionalInterface
    public interface AdvancementGranted {
        /**
         * Called when a player is granted an advancement.
         *
         * @param player      that was granted the {@code advancement}
         * @param advancement that was granted to the {@code player}
         */
        void advancementGranted(ServerPlayer player, Advancement advancement);
    }

    public static LivingEntityDeath LIVING_ENTITY_DEATH;

    /**
     * Functional interface to be implemented on callbacks for {@link #LIVING_ENTITY_DEATH}.
     *
     * @see #LIVING_ENTITY_DEATH
     */
    @FunctionalInterface
    public interface LivingEntityDeath {
        /**
         * Called when a living entity dies.
         *
         * @param livingEntity that died.
         * @param damageSource that that killed the {@code livingEntity}.
         */
        void livingEntityDeath(LivingEntity livingEntity, DamageSource damageSource);
    }
}
