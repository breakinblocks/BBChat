package com.breakinblocks.bbchat.common;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.StringTextComponent;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.ServerChatEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;

import javax.security.auth.login.LoginException;
import java.util.Objects;

public class ChatRelay {
    private final MinecraftServer server;
    private final JDA jda;
    private final long guildId;
    private final long channelId;

    public ChatRelay(MinecraftServer server, String botToken, long serverId, long channelId) throws LoginException {
        this.server = server;
        this.jda = JDABuilder
                .create(botToken,
                        GatewayIntent.GUILD_MESSAGES
                )
                .disableCache(
                        CacheFlag.ACTIVITY,
                        CacheFlag.VOICE_STATE,
                        CacheFlag.EMOTE,
                        CacheFlag.CLIENT_STATUS,
                        CacheFlag.MEMBER_OVERRIDES
                )
                .setMemberCachePolicy(MemberCachePolicy.NONE)
                .setEventManager(new AnnotatedEventManager())
                .addEventListeners(this)
                .build();
        this.guildId = serverId;
        this.channelId = channelId;
        MinecraftForge.EVENT_BUS.register(this);
    }

    public void cleanup() {
        MinecraftForge.EVENT_BUS.unregister(this);
        this.jda.shutdown();
    }

    @net.dv8tion.jda.api.hooks.SubscribeEvent
    public void relayDiscordMessageToMinecraft(MessageReceivedEvent event) {
        if (event.getChannelType() != ChannelType.TEXT) return;
        if (event.getGuild().getIdLong() != guildId) return;
        if (event.getChannel().getIdLong() != channelId) return;
        if (event.getAuthor().isBot()) return; // Should this just ignore self?
        Member member = Objects.requireNonNull(event.getMember()); // Should not be null since TextChannel
        String name = member.getEffectiveName();
        String text = event.getMessage().getContentDisplay();
        String message = String.format("[%s] %s", name, text);
        server.getPlayerList().sendMessage(new StringTextComponent(message), false);
    }

    @SubscribeEvent
    public void relayMinecraftMessageToDiscord(ServerChatEvent event) {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        String name = event.getPlayer().getName().getUnformattedComponentText();
        String text = event.getMessage();
        String message = String.format("**[%s]** %s", name, text);
        channel.sendMessage(message).submit();
    }
}
