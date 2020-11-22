package com.breakinblocks.bbchat.api;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.events.ReconnectedEvent;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.util.Collection;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import static com.breakinblocks.bbchat.api.TextUtils.Formatting.*;

public class ChatRelay implements IRelay {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FORMAT_CHAT = BOLD + "[%s]" + RESET + " %s";
    private static final String FORMAT_LOGIN = BOLD + "%s" + RESET + " joined the server";
    private static final String FORMAT_LOGOUT = BOLD + "%s" + RESET + " left the server";
    private static final String FORMAT_ACHIEVEMENT = BOLD + "%s" + RESET + " got " + BOLD + "%s" + RESET + " " + ITALIC + "%s" + RESET;
    private final JDA jda;
    private final long guildId;
    private final long channelId;
    private final long staffRoleId;
    private final Set<String> commandPrefixes;
    private final Set<String> anyCommands;
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();
    private final Consumer<String> broadcastMessage;
    private final CommandHandler commandHandler;

    public ChatRelay(
            String botToken,
            long serverId,
            long channelId,
            long staffRoleId,
            String commandPrefix,
            Collection<String> anyCommands,
            Consumer<String> broadcastMessage,
            CommandHandler commandHandler
    ) throws LoginException {
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
        this.staffRoleId = staffRoleId;
        this.commandPrefixes = ImmutableSet.of(commandPrefix, "<@!" + this.jda.getSelfUser().getId() + "> ");
        this.anyCommands = ImmutableSet.copyOf(anyCommands);
        this.broadcastMessage = broadcastMessage;
        this.commandHandler = commandHandler;
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
        handlePotentialCommand(member, event.getMessage());
    }

    private void handlePotentialCommand(Member member, Message message) {
        final String rawMessage = message.getContentRaw();
        final String prefix = commandPrefixes.stream().filter(rawMessage::startsWith).findAny().orElse(null);
        // Starts with the prefix or mentions the relay
        if (prefix == null) return;
        final String fullCommand = rawMessage.substring(prefix.length()).trim();
        final boolean isStaff = member.getRoles().stream().anyMatch(role -> role.getIdLong() == staffRoleId);
        final String commandRoot = fullCommand.split(" ", 2)[0];
        // Check that it is a staff member or the command is allowed for anyone
        if (isStaff || this.anyCommands.contains(commandRoot)) {
            // Run command
            final String name = member.getUser().getAsTag() + " (" + member.getId() + ")";
            final String displayName = member.getEffectiveName();
            final String logName = member.getNickname() == null ? name : name + "/" + displayName;
            LOGGER.info(logName + " is running the command `" + fullCommand + "`");
            commandHandler.handleCommand(isStaff, name, displayName, fullCommand, this::convertAndSendToDiscord);
        }
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

    private void convertAndSendToDiscord(String text) {
        sendToDiscord(TextUtils.convertToDiscord(text));
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
        convertAndSendToDiscord(String.format(FORMAT_CHAT, name, text));
    }

    @Override
    public void onLogin(String name) {
        convertAndSendToDiscord(String.format(FORMAT_LOGIN, name));
    }

    @Override
    public void onLogout(String name) {
        convertAndSendToDiscord(String.format(FORMAT_LOGOUT, name));
    }

    @Override
    public void onAchievement(String name, String title, String description) {
        convertAndSendToDiscord(String.format(FORMAT_ACHIEVEMENT, name, title, description));
    }

    @Override
    public void onDeath(String message, String target, @Nullable String source) {
        message = TextUtils.replaceWord(message, target, BOLD + target + RESET);
        if (source != null) {
            message = TextUtils.replaceWord(message, source, BOLD + source + RESET);
        }
        convertAndSendToDiscord(message);
    }
}
