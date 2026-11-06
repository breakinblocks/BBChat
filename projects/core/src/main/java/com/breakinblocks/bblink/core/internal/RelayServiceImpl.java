package com.breakinblocks.bblink.core.internal;

import com.breakinblocks.bblink.core.api.MinecraftService;
import com.breakinblocks.bblink.core.api.NameSanitizer;
import com.breakinblocks.bblink.core.api.RelayService;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Set;
import java.util.function.Supplier;

public class RelayServiceImpl implements RelayService {
    private static final Logger LOGGER = LoggerFactory.getLogger(RelayServiceImpl.class);
    static final String SHARED_DATABASE_FILE_NAME = "bblink-shared.db";
    // In-game formats for messages shown on other linked servers. %1$s is the server tag.
    private static final String GRAY = "§7";
    private static final String YELLOW = "§e";
    private static final String GREEN = "§a";
    private static final String RESET = "§r";
    private static final String MC_TAG = GRAY + "[%s]" + RESET + " ";
    private static final String MC_CHAT = "<%s> %s";
    private static final String MC_LOGIN = YELLOW + "%s joined the server";
    private static final String MC_LOGOUT = YELLOW + "%s left the server";
    private static final String MC_ACHIEVEMENT = "%s has made the advancement " + GREEN + "[%s]";
    private static final String MC_STARTED = GRAY + "Server started";
    private static final String MC_STOPPED = GRAY + "Server stopped";

    private RelayEndpoint endpoint = DummyRelay.INSTANCE;
    @Nullable
    private CrossServerLink link;
    private BBLinkConfig config;
    private MinecraftService minecraft;
    private String serverName = "";

    @Override
    public void setMinecraftService(MinecraftService minecraftService) {
        minecraft = minecraftService;
    }

    @Override
    public void onStarting() {
        if (config == null) {
            config = new BBLinkConfig();
        }

        serverName = NameSanitizer.cleanName(config.serverName, "");
        String databasePath = config.sharedDatabasePath.trim();
        if (!databasePath.isEmpty()) {
            if (serverName.isEmpty()) {
                serverName = "Server";
                LOGGER.warn("sharedDatabasePath is set but serverName is blank. Using '" + serverName
                        + "'. Set a unique serverName for each linked server in config/bblink.toml.");
            }
            try {
                link = CrossServerLink.start(resolveDatabaseFile(databasePath), serverName, minecraft::onMessage);
            } catch (RuntimeException e) {
                LOGGER.error("Failed to start the cross-server link, chat won't be relayed to other servers.", e);
                link = null;
            }
        }

        CrossServerLink currentLink = link;
        Supplier<Set<String>> knownServerKeys = currentLink == null
                ? Collections::emptySet
                : currentLink::getKnownServerKeys;

        endpoint = ChatRelay.create(
                config.botToken,
                config.guildId,
                config.channelId,
                config.staffRoleId,
                config.commandPrefix,
                config.anyCommands,
                minecraft::onMessage,
                minecraft::getPlayerCountInfo,
                minecraft,
                serverName,
                knownServerKeys
        );
    }

    @Override
    public void onStarted() {
        endpoint.onStarted();
        publishEvent(MC_STARTED);
    }

    @Override
    public void onStopping() {
    }

    @Override
    public void onStopped() {
        endpoint.onStopped();
        endpoint.cleanup();
        endpoint = DummyRelay.INSTANCE;

        if (link != null) {
            publishEvent(MC_STOPPED);
            link.close();
            link = null;
        }
    }

    @Override
    public void onChat(String name, String text) {
        name = clean(name);
        text = NameSanitizer.stripFormatting(text);
        endpoint.onChat(name, text);
        publish(String.format(MC_CHAT, name, text));
    }

    @Override
    public void onLogin(String name) {
        name = clean(name);
        endpoint.onLogin(name);
        publishEvent(String.format(MC_LOGIN, name));
    }

    @Override
    public void onLogout(String name) {
        name = clean(name);
        endpoint.onLogout(name);
        publishEvent(String.format(MC_LOGOUT, name));
    }

    @Override
    public void onAchievement(String name, String title, String description) {
        name = clean(name);
        endpoint.onAchievement(name, title, description);
        publishEvent(String.format(MC_ACHIEVEMENT, name, NameSanitizer.stripFormatting(title)));
    }

    @Override
    public void onDeath(String message, String target, @Nullable String source) {
        message = NameSanitizer.stripFormatting(message);
        endpoint.onDeath(message, target, source);
        publishEvent(message);
    }

    /**
     * Accept a folder as well as a file, since "where should the shared database live" is easy to answer with one.
     * All linked servers must end up with the same file, which they do as long as they're given the same path.
     */
    static Path resolveDatabaseFile(String configured) {
        Path path = Paths.get(configured);
        boolean looksLikeFolder = configured.endsWith("/") || configured.endsWith("\\");
        if (looksLikeFolder || Files.isDirectory(path)) {
            return path.resolve(SHARED_DATABASE_FILE_NAME);
        }
        return path;
    }

    /**
     * Names should already be clean when they get here; this guards platforms that pass raw names.
     */
    private static String clean(String name) {
        return NameSanitizer.cleanName(name, name);
    }

    private void publishEvent(String message) {
        if (config != null && Boolean.TRUE.equals(config.crossServerEvents)) {
            publish(message);
        }
    }

    private void publish(String message) {
        if (link == null) return;
        link.publish(String.format(MC_TAG, serverName) + message);
    }
}
