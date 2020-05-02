package com.breakinblocks.bbchat.common;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.minecraftforge.common.MinecraftForge;

import javax.security.auth.login.LoginException;
import java.util.Objects;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

public class ChatRelay implements IRelay {
    private final Consumer<String> broadcastMessage;
    private final JDA jda;
    private final long guildId;
    private final long channelId;
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();

    public ChatRelay(Consumer<String> broadcastMessage, String botToken, long serverId, long channelId) throws LoginException {
        this.broadcastMessage = broadcastMessage;
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

    @SubscribeEvent
    public void relayDiscordMessageToMinecraft(MessageReceivedEvent event) {
        if (event.getChannelType() != ChannelType.TEXT) return;
        if (event.getGuild().getIdLong() != guildId) return;
        if (event.getChannel().getIdLong() != channelId) return;
        if (event.getAuthor().isBot()) return; // Should this just ignore self?
        Member member = Objects.requireNonNull(event.getMember()); // Should not be null since TextChannel
        String name = member.getEffectiveName();
        String text = event.getMessage().getContentDisplay();
        String message = String.format("[%s] %s", name, text);
        broadcastMessage.accept(message);
    }

    private void sendQueueToDiscord() {
        TextChannel channel = jda.getTextChannelById(channelId);
        if (channel == null) return;
        for (String message = messageQueue.poll(); message != null; message = messageQueue.poll()) {
            channel.sendMessage(message).submit();
        }
    }

    private void sendToDiscord(String message) {
        messageQueue.add(message);
        sendQueueToDiscord();
    }

    @SubscribeEvent
    public void sendQueueOnConnect(ReadyEvent event) {
        sendQueueToDiscord();
    }

    @SubscribeEvent
    public void sendQueueOnReconnect(ReconnectedEvent event) {
        sendQueueToDiscord();
    }

    @Override
    public void cleanup() {
        this.jda.shutdown();
    }

    @Override
    public void onStarted() {
        sendToDiscord("**Server Started**");
    }

    @Override
    public void onStopped() {
        sendToDiscord("**Server Stopped**");
    }

    @Override
    public void onChat(String name, String text) {
        sendToDiscord(String.format("**[%s]** %s", name, text));
    }

    @Override
    public void onLogin(String name) {
        sendToDiscord(String.format("**%s** joined the server", name));
    }

    @Override
    public void onLogout(String name) {
        sendToDiscord(String.format("**%s** left the server", name));
    }

    @Override
    public void onAchievement(String name, String title, String description) {
        sendToDiscord(String.format("**%s** got **%s** *%s*", name, title, description));
    }
}
