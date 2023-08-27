package com.breakinblocks.bbchat.quilt;

import com.breakinblocks.bbchat.quilt.common.BBChatQuiltEvents;
import com.breakinblocks.bbchat.quilt.common.QuiltMinecraftService;
import com.breakinblocks.bbchat.vanilla.BBChat;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents;
import org.quiltmc.qsl.networking.api.ServerPlayConnectionEvents;

public class BBChatQuilt extends BBChat implements ModInitializer {
    public BBChatQuilt() {
        super(new QuiltMinecraftService());
    }

    @Override
    public void onInitialize(ModContainer mod) {
        ServerLifecycleEvents.STARTING.register(server -> {
            setServer(server);
            relayServerStarting();
        });
        ServerLifecycleEvents.READY.register(server -> relayServerStarted());
        ServerLifecycleEvents.STOPPING.register(server -> relayServerStopping());
        ServerLifecycleEvents.STOPPED.register(server -> relayServerStopped());
        BBChatQuiltEvents.CHAT_MESSAGE_SENT.register(BBChat::relayChat);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> relayLogin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> relayLogout(handler.getPlayer()));
        BBChatQuiltEvents.ADVANCEMENT_GRANTED.register(BBChat::relayAchievement);
        BBChatQuiltEvents.LIVING_ENTITY_DEATH.register(this::relayDeath);
    }
}
