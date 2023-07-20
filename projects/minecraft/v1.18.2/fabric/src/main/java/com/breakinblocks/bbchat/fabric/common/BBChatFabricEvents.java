package com.breakinblocks.bbchat.fabric.common;

import net.fabricmc.fabric.api.event.Event;
import net.fabricmc.fabric.api.event.EventFactory;
import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;

public class BBChatFabricEvents {
    public static final Event<AdvancementGranted> ADVANCEMENT_GRANTED = EventFactory.createArrayBacked(AdvancementGranted.class, callbacks -> (player, advancement) -> {
        for (var callback : callbacks) {
            callback.advancementGranted(player, advancement);
        }
    });

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
}
