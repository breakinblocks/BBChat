package com.breakinblocks.bbchat.fabric;

import com.breakinblocks.bbchat.fabric.common.BBChatFabricEvents;
import com.breakinblocks.bbchat.fabric.common.FabricMinecraftService;
import com.breakinblocks.bbchat.vanilla.BBChat;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.entity.event.v1.ServerLivingEntityEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;

public class BBChatFabric extends BBChat implements ModInitializer {
    private ChatType chatTypeChat;

    public BBChatFabric() {
        super(new FabricMinecraftService());
    }

    @Override
    public void onInitialize() {
        ServerLifecycleEvents.SERVER_STARTING.register(server -> {
            setServer(server);
            Registry<ChatType> registry = server.registryAccess().registryOrThrow(Registries.CHAT_TYPE);
            chatTypeChat = registry.getOrThrow(ChatType.CHAT);
            relayServerStarting();
        });
        ServerLifecycleEvents.SERVER_STARTED.register(server -> relayServerStarted());
        ServerLifecycleEvents.SERVER_STOPPING.register(server -> relayServerStopping());
        ServerLifecycleEvents.SERVER_STOPPED.register(server -> relayServerStopped());
        ServerMessageEvents.CHAT_MESSAGE.register((message, sender, params) -> {
            if (params.chatType() != chatTypeChat) {
                return;
            }

            relayChat(sender, message.decoratedContent());
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> relayLogin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> relayLogout(handler.getPlayer()));
        BBChatFabricEvents.ADVANCEMENT_GRANTED.register(BBChat::relayAchievement);
        ServerLivingEntityEvents.AFTER_DEATH.register(this::relayDeath);
    }
}
