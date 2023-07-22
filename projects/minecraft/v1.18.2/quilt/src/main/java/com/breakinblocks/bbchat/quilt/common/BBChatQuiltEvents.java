package com.breakinblocks.bbchat.quilt.common;

import net.minecraft.advancements.Advancement;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.LivingEntity;
import org.quiltmc.qsl.base.api.event.Event;
import org.quiltmc.qsl.base.api.event.EventAwareListener;

public final class BBChatQuiltEvents {
    private BBChatQuiltEvents() {
    }

    public static final Event<ChatMessageSent> CHAT_MESSAGE_SENT = Event.create(ChatMessageSent.class, callbacks -> (player, message) -> {
        for (var callback : callbacks) {
            callback.chatMessageSent(player, message);
        }
    });

    /**
     * Functional interface to be implemented on callbacks for {@link #CHAT_MESSAGE_SENT}.
     *
     * @see #CHAT_MESSAGE_SENT
     */
    @FunctionalInterface
    public interface ChatMessageSent extends EventAwareListener {
        /**
         * Called when a player sends a chat message.
         *
         * @param player  that sent the {@code message}
         * @param message that was send by the {@code player}
         */
        void chatMessageSent(ServerPlayer player, String message);
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

    public static final Event<LivingEntityDeath> LIVING_ENTITY_DEATH = Event.create(LivingEntityDeath.class, callbacks -> (livingEntity, damageSource) -> {
        for (var callback : callbacks) {
            callback.livingEntityDeath(livingEntity, damageSource);
        }
    });

    /**
     * Functional interface to be implemented on callbacks for {@link #LIVING_ENTITY_DEATH}.
     *
     * @see #LIVING_ENTITY_DEATH
     */
    @FunctionalInterface
    public interface LivingEntityDeath extends EventAwareListener {
        /**
         * Called when a living entity dies.
         *
         * @param livingEntity that died.
         * @param damageSource that that killed the {@code livingEntity}.
         */
        void livingEntityDeath(LivingEntity livingEntity, DamageSource damageSource);
    }
}
