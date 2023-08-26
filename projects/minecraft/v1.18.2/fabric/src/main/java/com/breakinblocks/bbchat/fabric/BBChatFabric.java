package com.breakinblocks.bbchat.fabric;

import com.breakinblocks.bbchat.fabric.common.BBChatFabricEvents;
import com.breakinblocks.bbchat.fabric.common.FabricMinecraftService;
import com.breakinblocks.bbchat.vanilla.BBChat;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;

public class BBChatFabric extends BBChat implements ModInitializer {
    public BBChatFabric() {
        super(new FabricMinecraftService());
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            setServer(server);
            relayServerStarting();
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> relayServerStarted());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> relayServerStopping());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> relayServerStopped());
        BBChatFabricEvents.CHAT_MESSAGE_SENT.register(BBChat::relayChat);
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> relayLogin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> relayLogout(handler.getPlayer()));
        BBChatFabricEvents.ADVANCEMENT_GRANTED.register(BBChat::relayAchievement);
        BBChatFabricEvents.LIVING_ENTITY_DEATH.register(this::relayDeath);
    }
}
