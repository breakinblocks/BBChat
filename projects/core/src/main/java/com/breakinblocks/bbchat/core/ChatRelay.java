package com.breakinblocks.bbchat.common;

import com.google.common.collect.ImmutableSet;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Activity;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.channel.Channel;
import net.dv8tion.jda.api.entities.channel.middleman.GuildMessageChannel;
import net.dv8tion.jda.api.entities.emoji.CustomEmoji;
import net.dv8tion.jda.api.entities.emoji.RichCustomEmoji;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.events.session.ReadyEvent;
import net.dv8tion.jda.api.events.session.SessionRecreateEvent;
import net.dv8tion.jda.api.events.session.SessionResumeEvent;
import net.dv8tion.jda.api.hooks.AnnotatedEventManager;
import net.dv8tion.jda.api.hooks.SubscribeEvent;
import net.dv8tion.jda.api.requests.GatewayIntent;
import net.dv8tion.jda.api.utils.MemberCachePolicy;
import net.dv8tion.jda.api.utils.cache.CacheFlag;
import net.dv8tion.jda.internal.utils.PermissionUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;
import javax.security.auth.login.LoginException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.CharBuffer;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.breakinblocks.bbchat.common.TextUtils.Formatting.BOLD;
import static com.breakinblocks.bbchat.common.TextUtils.Formatting.ITALIC;
import static com.breakinblocks.bbchat.common.TextUtils.Formatting.RESET;

public final class ChatRelay implements IRelay {
    private static final Logger LOGGER = LogManager.getLogger();
    private static final String FORMAT_CHAT = BOLD + "[%s]" + RESET + " %s";
    private static final String FORMAT_LOGIN = BOLD + "%s" + RESET + " joined the server";
    private static final String FORMAT_LOGOUT = BOLD + "%s" + RESET + " left the server";
    private static final String FORMAT_ACHIEVEMENT = BOLD + "%s" + RESET + " got " + BOLD + "%s" + RESET + " " + ITALIC + "%s" + RESET;
    private static final Pattern REGEX_EMOTE = Pattern.compile(":([A-Za-z0-9_]{2,32}):");
    private static final int MAX_DISCORD_MESSAGE_LENGTH = 2000;
    private static final int MAX_COMMAND_FILE_SIZE = 128 * 1024; // 128 KB should be plenty
    private static final int MAX_MESSAGE_QUEUE_SIZE = 100;
    private static final long LOGIN_ACHIEVEMENT_DELAY_MILLIS = 5 * 1000;
    private static final long SHUTDOWN_MESSAGES_TIMEOUT_MILLIS = 5 * 1000;
    private static final long SHUTDOWN_TIMEOUT_MILLIS = 10 * 1000;
    private final JDA jda;
    private final long guildId;
    private final long channelId;
    private final long staffRoleId;
    private final Set<String> commandPrefixes;
    private final Set<String> anyCommands;
    private final ConcurrentLinkedQueue<String> messageQueue = new ConcurrentLinkedQueue<>();
    private final Set<CompletableFuture<Message>> messageFutures = ConcurrentHashMap.newKeySet();
    private final Consumer<String> broadcastMessage;
    private final Supplier<PlayerCountInfo> playerCount;
    private final CommandHandler commandHandler;
    private final Map<String, Long> lastLogin = new HashMap<>();

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
                .create(
                        botToken,
                        GatewayIntent.GUILD_EMOJIS_AND_STICKERS,
                        GatewayIntent.GUILD_MESSAGES,
                        GatewayIntent.MESSAGE_CONTENT
                )
                .disableCache(
                        CacheFlag.ACTIVITY,
                        CacheFlag.VOICE_STATE,
                        CacheFlag.CLIENT_STATUS,
                        CacheFlag.ROLE_TAGS,
                        CacheFlag.FORUM_TAGS,
                        CacheFlag.ONLINE_STATUS,
                        CacheFlag.SCHEDULED_EVENTS
                )
                .enableCache(
                        CacheFlag.EMOJI,
                        CacheFlag.STICKER,
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
    ) {
        long guildIdL = parseULongOrZero(guildId, "guildId");
        long channelIdL = parseULongOrZero(channelId, "channelId");
        long staffRoleIdL = parseULongOrZero(staffRoleId, "staffRoleId");
        ProxyRelay proxyRelay = new ProxyRelay();
        Thread createThread = new Thread(() -> {
            try {
                int retries = 0;
                long delay = 5;
                // Cap out retry delay at 1 hour.
                long maxDelay = 60 * 60;
                while (true) {
                    try {
                        LOGGER.info("Logging in to Discord...");
                        ChatRelay chatRelay = new ChatRelay(botToken, guildIdL, channelIdL, staffRoleIdL, commandPrefix, anyCommands, broadcastMessage, playerCount, commandHandler);
                        if (proxyRelay.isServerRunning())
                            chatRelay.onStarted();
                        proxyRelay.setRelay(chatRelay);
                        break;
                    } catch (RuntimeException e) {
                        LOGGER.warn("Failed to connect to Discord.", e);
                    }
                    LOGGER.info("Retrying in " + delay + "s");
                    Thread.sleep(delay * 1000L);
                    delay = Math.min(delay * 2, maxDelay);
                }
                LOGGER.info("Connected to Discord!");
            } catch (InterruptedException e) {
                LOGGER.warn("Interrupted before connecting to Discord.", e);
            } catch (LoginException e) {
                LOGGER.warn("Failed to login ;-;. Check your bot token.", e);
            }
        });
        createThread.setName("BBChat Relay Creation Thread");
        createThread.setDaemon(true);
        createThread.start();

        return proxyRelay;
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
    public void checkConfig(ReadyEvent event) {
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) {
            LOGGER.warn("Could not find guild with id '" + guildId +"', make sure the guild id is correct and that the bot has been invited to the guild.");
        }

        Channel channel = jda.getChannelById(Channel.class, channelId);
        GuildMessageChannel guildMessageChannel;
        if (channel == null) {
            guildMessageChannel = null;
            LOGGER.warn("Could not find channel with id '" + channelId + "', make sure the channel id is correct and that the bot can view the channel (check the users list in the channel).");
        } else {
            if (!(channel instanceof GuildMessageChannel)) {
                guildMessageChannel = null;
                LOGGER.warn("Channel '" + channel.getName() + "' is not a guild message channel (it might be a non-guild-message channel like a forum channel but not a thread inside it).");
            } else {
                guildMessageChannel = (GuildMessageChannel) channel;
                if (!guildMessageChannel.canTalk()) {
                    LOGGER.warn("Bot does not have permission to talk in Channel '" + guildMessageChannel.getName() + "'. Make sure the bot has permissions to send messages in the channel.");
                }
            }
        }

        Role staffRole = jda.getRoleById(staffRoleId);
        if (staffRole == null) {
            LOGGER.warn("Could not find staff role with id: " + staffRoleId);
        }

        if (guild != null && guildMessageChannel != null) {
            Guild guildMessageChannelGuild = guildMessageChannel.getGuild();
            if (guild.getIdLong() != guildMessageChannelGuild.getIdLong()) {
                LOGGER.warn("Channel '" + guildMessageChannel.getName() + "' belongs to the guild '" + guildMessageChannelGuild.getName() + "'. Expected guild '" + guild.getName() + "'.");
            }
        }

        if (guild != null && staffRole != null) {
            Guild staffRoleGuild = staffRole.getGuild();
            if (guild.getIdLong() != staffRoleGuild.getIdLong()) {
                LOGGER.warn("Staff role '" + staffRole.getName() + "' belongs to the guild '" + staffRoleGuild.getName() + "'. Expected guild '" + guild.getName() + "'.");
            }
        }
    }

    @SubscribeEvent
    public void relayDiscordMessageToMinecraft(MessageReceivedEvent event) {
        if (!event.isFromGuild()) return;
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
                    .map(attachment -> attachment.getProxy().download())
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
        Guild guild = jda.getGuildById(guildId);
        if (guild == null) return;
        GuildMessageChannel channel = guild.getChannelById(GuildMessageChannel.class, channelId);
        if (channel == null) return;
        if (!channel.canTalk()) return;
        for (CharSequence message = messageQueue.poll(); message != null; message = messageQueue.poll()) {
            CharSequence replaced = replaceEmotes(message);
            CharSequence truncated = replaced.subSequence(0, Math.min(MAX_DISCORD_MESSAGE_LENGTH, replaced.length()));
            CompletableFuture<Message> messageFuture = channel.sendMessage(truncated).submit();
            messageFutures.add(messageFuture);
            messageFuture.whenComplete((m, t) -> messageFutures.remove(messageFuture));
        }
    }

    private CharSequence replaceEmotes(CharSequence message) {
        Matcher matcher = REGEX_EMOTE.matcher(message);
        StringBuffer buff = new StringBuffer();
        while (matcher.find()) {
            Optional<RichCustomEmoji> emote = jda.getEmojisByName(matcher.group(1), false).stream().findFirst();
            matcher.appendReplacement(buff, emote.map(CustomEmoji::getAsMention).orElseGet(matcher::group));
        }
        matcher.appendTail(buff);
        return buff;
    }

    private void sendToDiscord(String message) {
        while (messageQueue.size() >= MAX_MESSAGE_QUEUE_SIZE) {
            messageQueue.poll();
        }
        messageQueue.add(message);
        sendQueueToDiscord();
    }

    private void convertAndSendToDiscord(String text) {
        sendToDiscord(TextUtils.convertToDiscord(text));
    }

    private void updatePlayerCount(boolean minusOne) {
        PlayerCountInfo info = playerCount.get();
        int current = info.getCurrent() + (minusOne ? -1 : 0);
        jda.getPresence().setActivity(Activity.of(Activity.ActivityType.PLAYING, "with " + current + "/" + info.getMax() + " players"));
    }

    @SubscribeEvent
    public void sendQueueOnConnect(ReadyEvent event) {
        sendQueueToDiscord();
    }

    @SubscribeEvent
    public void sendQueueOnSessionResume(SessionResumeEvent event) {
        sendQueueToDiscord();
    }

    @SubscribeEvent
    public void sendQueueOnSessionRecreate(SessionRecreateEvent event) {
        sendQueueToDiscord();
    }

    @Override
    public void cleanup() {
        jda.shutdown();

        try {
            if (!jda.awaitShutdown(Duration.ofMillis(SHUTDOWN_TIMEOUT_MILLIS))) {
                jda.shutdownNow();
                jda.awaitShutdown();
            }
        } catch (InterruptedException ignored) {
            jda.shutdownNow();
        }
    }

    @Override
    public void onStarted() {
        sendToDiscord("**Server Started**");
        updatePlayerCount(false);
    }

    @Override
    public void onStopped() {
        sendToDiscord("**Server Stopped**");

        try {
            // Wait for the remaining messages to send with a timeout.
            CompletableFuture.allOf(messageFutures.toArray(new CompletableFuture[0]))
                    .get(SHUTDOWN_MESSAGES_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS);
        } catch (InterruptedException | ExecutionException | TimeoutException ignored) {
        }
    }

    @Override
    public void onChat(String name, String text) {
        convertAndSendToDiscord(String.format(FORMAT_CHAT, name, text));
    }

    @Override
    public void onLogin(String name) {
        lastLogin.put(name, System.currentTimeMillis());
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
        long now = System.currentTimeMillis();
        if (now < lastLogin.getOrDefault(name, now) + LOGIN_ACHIEVEMENT_DELAY_MILLIS)
            return;
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
