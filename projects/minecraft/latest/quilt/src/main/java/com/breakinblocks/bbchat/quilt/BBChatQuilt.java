package com.breakinblocks.bbchat.quilt;

import com.breakinblocks.bbchat.quilt.common.BBChatInitializer;
import com.breakinblocks.bbchat.quilt.common.BBChatQuiltEvents;
import com.breakinblocks.bbchat.quilt.common.QuiltMinecraftService;
import com.breakinblocks.bbchat.vanilla.BBChat;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import org.quiltmc.loader.api.ModContainer;

import java.util.EnumSet;
import java.util.Objects;

public class BBChatQuilt extends BBChat implements BBChatInitializer {
    private ChatType chatTypeChat;

    public BBChatQuilt() {
        super(new QuiltMinecraftService());
    }

    @Override
    public void onInitialize(ModContainer mod) {
        BBChatQuiltEvents.SERVER_STARTING = server -> {
            setServer(server);
            relayServerStarting();
        };
        BBChatQuiltEvents.SERVER_STARTED = BBChat::relayServerStarted;
        BBChatQuiltEvents.SERVER_STOPPING = BBChat::relayServerStopping;
        BBChatQuiltEvents.SERVER_STOPPED = BBChat::relayServerStopped;
        BBChatQuiltEvents.CHAT_MESSAGE_SENT = BBChat::relayChat;
        BBChatQuiltEvents.PLAYER_LOGIN = BBChat::relayLogin;
        BBChatQuiltEvents.PLAYER_LOGOUT = BBChat::relayLogout;
        BBChatQuiltEvents.ADVANCEMENT_GRANTED = BBChat::relayAchievement;
        BBChatQuiltEvents.LIVING_ENTITY_DEATH = this::relayDeath;
    }
}
