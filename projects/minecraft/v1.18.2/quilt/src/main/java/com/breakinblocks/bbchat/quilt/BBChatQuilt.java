package com.breakinblocks.bbchat.quilt;

import com.breakinblocks.bbchat.quilt.common.BBChatQuiltEvents;
import com.breakinblocks.bbchat.quilt.common.QuiltMinecraftService;
import com.breakinblocks.bbchat.vanilla.BBChat;
import com.breakinblocks.bbchat.vanilla.common.BBChatConfig;
import fuzs.forgeconfigapiport.api.config.v2.ForgeConfigRegistry;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.chat.ChatType;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.fml.config.ModConfig;
import org.quiltmc.loader.api.ModContainer;
import org.quiltmc.qsl.base.api.entrypoint.ModInitializer;
import org.quiltmc.qsl.chat.api.QuiltChatEvents;
import org.quiltmc.qsl.chat.api.QuiltMessageType;
import org.quiltmc.qsl.chat.api.types.ChatS2CMessage;
import org.quiltmc.qsl.entity.event.api.LivingEntityDeathCallback;
import org.quiltmc.qsl.lifecycle.api.event.ServerLifecycleEvents;
import org.quiltmc.qsl.networking.api.ServerPlayConnectionEvents;

import java.util.EnumSet;
import java.util.Objects;

public class BBChatQuilt extends BBChat implements ModInitializer {
    private ChatType chatTypeChat;

    public BBChatQuilt() {
        super(new QuiltMinecraftService());
    }

    @Override
    public void onInitialize(ModContainer mod) {
        ForgeConfigRegistry.INSTANCE.register(BBChat.MOD_ID, ModConfig.Type.COMMON, BBChatConfig.commonSpec);
        ServerLifecycleEvents.STARTING.register(server -> {
            setServer(server);
            Registry<ChatType> registry = server.registryAccess().registryOrThrow(Registries.CHAT_TYPE);
            chatTypeChat = registry.getOrThrow(ChatType.CHAT);
            relayServerStarting();
        });
        ServerLifecycleEvents.READY.register(server -> relayServerStarted());
        ServerLifecycleEvents.STOPPING.register(server -> relayServerStopping());
        ServerLifecycleEvents.STOPPED.register(server -> relayServerStopped());
        QuiltChatEvents.AFTER_PROCESS.register(EnumSet.of(QuiltMessageType.CHAT, QuiltMessageType.SERVER, QuiltMessageType.OUTBOUND), rawMessage -> {
            Player rawPlayer = rawMessage.getPlayer();
            if (!(rawPlayer instanceof ServerPlayer) || !(rawMessage instanceof ChatS2CMessage)) {
                return;
            }

            ServerPlayer player = (ServerPlayer) rawPlayer;
            ChatS2CMessage message = (ChatS2CMessage) rawMessage;
            Component decoratedContent = Objects.requireNonNullElseGet(message.getUnsignedContent(), () -> Component.literal(message.getBody().content()));
            relayChat(player, decoratedContent);
        });
        ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> relayLogin(handler.getPlayer()));
        ServerPlayConnectionEvents.DISCONNECT.register((handler, server) -> relayLogout(handler.getPlayer()));
        BBChatQuiltEvents.ADVANCEMENT_GRANTED.register(BBChat::relayAchievement);
        LivingEntityDeathCallback.EVENT.register(this::relayDeath);
    }
}
