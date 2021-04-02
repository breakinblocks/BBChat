package com.breakinblocks.bbchat.common;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.ChannelType;
import net.dv8tion.jda.api.entities.Emote;
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
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.breakinblocks.bbchat.common.TextUtils.Formatting.BOLD;
import static com.breakinblocks.bbchat.common.TextUtils.Formatting.ITALIC;
import static com.breakinblocks.bbchat.common.TextUtils.Formatting.RESET;

public class ChatRelay implements IRelay {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FORMAT_CHAT = BOLD + "[%s]" + RESET + " %s";
    private static final String FORMAT_LOGIN = BOLD + "%s" + RESET + " joined the server";
    private static final String FORMAT_LOGOUT = BOLD + "%s" + RESET + " left the server";
    private static final String FORMAT_ACHIEVEMENT = BOLD + "%s" + RESET + " got " + BOLD + "%s" + RESET + " " + ITALIC + "%s" + RESET;
    private static final Pattern REGEX_EMOTE = Pattern.compile(":([A-Za-z0-9_]{2,32}):");
    private static final int MAX_COMMAND_FILE_SIZE = 128 * 1024; // 128 KB should be plenty
    private final JDA jda;
    private final long guildId;
    private final long channelId;
    private final long staffRoleId;
    private final Set<String> commandPrefixes;
    private final Set<String> anyCommands;
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();
    private final Consumer<String> broadcastMessage;
    private final Supplier<PlayerCountInfo> playerCount;
    private final CommandHandler commandHandler;

    private ChatRelay(
            String botToken,
            long guildId,
            long channelId,
            long staffRoleId,
            String commandPrefix,
            Collection<String> anyCommands,
            Consumer<String> broadcastMessage,
            Supplier<PlayerCountInfo> playerCount,
            CommandHandler commandHandler
    ) throws LoginException {
        jda = JDABuilder
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
        this.guildId = guildId;
        this.channelId = channelId;
        this.staffRoleId = staffRoleId;
        commandPrefixes = ImmutableSet.<String>builder()
                .add(commandPrefix)
                .add("<@!" + jda.getSelfUser().getId() + ">")
                .add("<@" + jda.getSelfUser().getId() + ">")
                .build();
        this.anyCommands = ImmutableSet.copyOf(anyCommands);
        this.broadcastMessage = broadcastMessage;
        this.playerCount = playerCount;
        this.commandHandler = commandHandler;
    }

    public static IRelay create(
            String botToken,
            String guildId,
            String channelId,
            String staffRoleId,
            String commandPrefix,
            Collection<String> anyCommands,
            Consumer<String> broadcastMessage,
            Supplier<PlayerCountInfo> playerCount,
            CommandHandler commandHandler
    ) throws LoginException {
        long guildIdL = parseULongOrZero(guildId, "guildId");
        long channelIdL = parseULongOrZero(channelId, "channelId");
        long staffRoleIdL = parseULongOrZero(staffRoleId, "staffRoleId");
        return new ChatRelay(botToken, guildIdL, channelIdL, staffRoleIdL, commandPrefix, anyCommands, broadcastMessage, playerCount, commandHandler);
    }

    private static long parseULongOrZero(String input, String desc) {
        try {
            long value = Long.parseUnsignedLong(input);
            if (value == 0)
                LOGGER.warn(desc + " is zero");
            return value;
        } catch (NumberFormatException ignored) {
            LOGGER.warn(desc + " failed to parse as unsigned long");
        }
        return 0L;
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
        final String commandPortion = rawMessage.substring(prefix.length()).trim();
        // If it's empty without the prefix, try get the command from the first attached txt file instead
        if (commandPortion.isEmpty()) {
            message.getAttachments().stream()
                    .filter(attachment -> attachment.getSize() <= MAX_COMMAND_FILE_SIZE)
                    .filter(attachment -> "txt".equals(attachment.getFileExtension()))
                    .findFirst()
                    .map(Message.Attachment::retrieveInputStream)
                    .ifPresent(future -> future
                            .thenApply(stream -> {
                                try (BufferedReader reader = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                                    CharBuffer buffer = CharBuffer.allocate(MAX_COMMAND_FILE_SIZE);
                                    int len = reader.read(buffer);
                                    buffer.rewind();
                                    return buffer.subSequence(0, len).toString();
                                } catch (IOException e) {
                                    LOGGER.error("Failed to read attachment", e);
                                }
                                return "";
                            })
                            .thenAccept(command -> handlePotentialCommand(member, command.trim()))
                    );
        } else {
            handlePotentialCommand(member, commandPortion);
        }
    }

    private void handlePotentialCommand(Member member, String fullCommand) {
        final boolean isStaff = member.getRoles().stream().anyMatch(role -> role.getIdLong() == staffRoleId);
        final String commandRoot = fullCommand.split(" ", 2)[0];
        // Check that it is a staff member or the command is allowed for anyone
        if (isStaff || anyCommands.contains(commandRoot)) {
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
        for (CharSequence message = messageQueue.poll(); message != null; message = messageQueue.poll()) {
            replaceEmotes(message);
            channel.sendMessage(message).submit();
        }
    }

    private CharSequence replaceEmotes(CharSequence message) {
        Matcher matcher = REGEX_EMOTE.matcher(message);
        StringBuffer buff = new StringBuffer();
        while (matcher.find()) {
            Optional<Emote> emote = jda.getEmotesByName(matcher.group(1), false).stream().findFirst();
            matcher.appendReplacement(buff, emote.map(Emote::getAsMention).orElseGet(matcher::group));
        }
        matcher.appendTail(buff);
        return buff;
    }

    private void sendToDiscord(String message) {
        messageQueue.add(message);
        sendQueueToDiscord();
    }

    private void convertAndSendToDiscord(String text) {
        sendToDiscord(TextUtils.convertToDiscord(text));
    }

    private void updatePlayerCount(boolean minusOne) {
        PlayerCountInfo info = playerCount.get();
        int current = info.getCurrent() + (minusOne ? -1 : 0);
        jda.getPresence().setActivity(Activity.of(Activity.ActivityType.DEFAULT, "with " + current + "/" + info.getMax() + " players"));
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
        jda.shutdown();
    }

    @Override
    public void onStarted() {
        sendToDiscord("**Server Started**");
        updatePlayerCount(false);
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
        updatePlayerCount(false);
    }

    @Override
    public void onLogout(String name) {
        convertAndSendToDiscord(String.format(FORMAT_LOGOUT, name));
        updatePlayerCount(true);
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
