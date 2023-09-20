package com.breakinblocks.bbchat.quilt.common;

import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;
import org.quiltmc.qsl.base.api.event.Event;
import org.quiltmc.qsl.base.api.event.EventAwareListener;

public final class BBChatQuiltEvents {
    private BBChatQuiltEvents() {
    }

    public static final Event<AdvancementGranted> ADVANCEMENT_GRANTED = Event.create(AdvancementGranted.class, callbacks -> (player, advancement) -> {
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
    public interface AdvancementGranted extends EventAwareListener {
        /**
         * Called when a player is granted an advancement.
         *
         * @param player      that was granted the {@code advancement}
         * @param advancement that was granted to the {@code player}
         */
        void advancementGranted(ServerPlayer player, Advancement advancement);
    }
}
