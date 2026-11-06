package com.breakinblocks.bblink.core.internal;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sqlite.SQLiteConfig;

import javax.annotation.Nullable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayDeque;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * Relays in-game messages between servers running on the same machine through a shared SQLite database.
 * <p>
 * The database is an append-only log of messages. Every server appends its own messages and reads everyone
 * else's in id order, so each message is shown exactly once on every other server:
 * <ul>
 * <li>Ids come from {@code AUTOINCREMENT}, so they are never reused (even after pruning), and SQLite allows a
 * single writer at a time, so ids become visible in increasing order. A reader that has seen id {@code n} will
 * never later find a new row with an id below {@code n}.</li>
 * <li>Each reader only moves its cursor forward. It starts at the newest row when the server starts (history is
 * not replayed) and keeps its place across database reconnects.</li>
 * <li>Rows are tagged with a random per-run instance id rather than the server name, so a server never shows its
 * own messages back, even if two servers are misconfigured with the same name.</li>
 * </ul>
 * All database access happens on a single background thread, so the game thread never waits on disk.
 */
final class CrossServerLink {
    private static final Logger LOGGER = LoggerFactory.getLogger(CrossServerLink.class);
    private static final long POLL_INTERVAL_MILLIS = 250;
    private static final long HEARTBEAT_INTERVAL_MILLIS = 10_000;
    private static final long PRUNE_INTERVAL_MILLIS = 60_000;
    private static final long MESSAGE_RETENTION_MILLIS = TimeUnit.HOURS.toMillis(1);
    private static final long SERVER_RETENTION_MILLIS = TimeUnit.DAYS.toMillis(30);
    /**
     * A server that heartbeat more recently than this is considered running.
     */
    private static final long SERVER_ALIVE_MILLIS = 3 * HEARTBEAT_INTERVAL_MILLIS;
    /**
     * Messages that could not be written for this long are dropped rather than delivered late.
     */
    private static final long MAX_PENDING_AGE_MILLIS = 60_000;
    private static final int MAX_PENDING = 100;
    private static final int POLL_BATCH_SIZE = 200;
    private static final int BUSY_TIMEOUT_MILLIS = 5_000;
    private static final long CLOSE_TIMEOUT_MILLIS = 5_000;

    private final Path databaseFile;
    private final String instanceId = UUID.randomUUID().toString();
    private final String serverName;
    private final String serverKey;
    private final Consumer<String> broadcast;
    private final ScheduledExecutorService executor;

    // Only touched on the executor thread.
    @Nullable
    private Connection connection;
    private long lastSeenId = -1;
    private long nextHeartbeat = 0;
    private long nextPrune = 0;
    private boolean checkedNameConflict = false;
    private boolean failing = false;
    private final ArrayDeque<Pending> pending = new ArrayDeque<>();

    private volatile Set<String> knownServerKeys;

    private CrossServerLink(Path databaseFile, String serverName, Consumer<String> broadcast) {
        this.databaseFile = databaseFile.toAbsolutePath();
        this.serverName = serverName;
        this.serverKey = serverKey(serverName);
        this.broadcast = broadcast;
        this.knownServerKeys = Collections.singleton(serverKey);
        this.executor = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread thread = new Thread(r, "BBLink Cross-Server Link");
            thread.setDaemon(true);
            return thread;
        });
    }

    /**
     * Open the link and start receiving messages from other servers.
     *
     * @param broadcast Called (from a background thread) with each message from another server.
     */
    static CrossServerLink start(Path databaseFile, String serverName, Consumer<String> broadcast) {
        CrossServerLink link = new CrossServerLink(databaseFile, serverName, broadcast);
        LOGGER.info("Linking with other servers through " + link.databaseFile + " as '" + serverName + "'.");
        link.executor.scheduleWithFixedDelay(link::tick, 0, POLL_INTERVAL_MILLIS, TimeUnit.MILLISECONDS);
        return link;
    }

    /**
     * Normalised form of a server name used for matching (case and whitespace insensitive).
     */
    static String serverKey(String serverName) {
        return serverName.replaceAll("\\s+", "").toLowerCase(Locale.ROOT);
    }

    /**
     * @return Keys (see {@link #serverKey}) of all servers that have used this database recently, including this one.
     */
    Set<String> getKnownServerKeys() {
        return knownServerKeys;
    }

    /**
     * Send a message to all other linked servers. Never blocks.
     *
     * @param message Minecraft formatted message.
     */
    void publish(String message) {
        Pending entry = new Pending(message, System.currentTimeMillis());
        try {
            executor.execute(() -> {
                while (pending.size() >= MAX_PENDING) {
                    pending.poll();
                }
                pending.add(entry);
                tick();
            });
        } catch (RejectedExecutionException e) {
            LOGGER.debug("Cross-server link closed, dropping message: " + message);
        }
    }

    /**
     * Deliver any queued messages, then disconnect. Waits briefly for queued messages to be written.
     */
    void close() {
        try {
            executor.execute(() -> {
                tick();
                disconnect();
            });
        } catch (RejectedExecutionException ignored) {
            return;
        }
        executor.shutdown();
        try {
            if (!executor.awaitTermination(CLOSE_TIMEOUT_MILLIS, TimeUnit.MILLISECONDS)) {
                LOGGER.warn("Timed out waiting for the cross-server link to close.");
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    private void tick() {
        try {
            Connection c = connect();
            writePending(c);
            readNew(c);
            long now = System.currentTimeMillis();
            if (now >= nextHeartbeat) {
                heartbeat(c, now);
                nextHeartbeat = now + HEARTBEAT_INTERVAL_MILLIS;
            }
            if (now >= nextPrune) {
                prune(c, now);
                nextPrune = now + PRUNE_INTERVAL_MILLIS;
            }
            if (failing) {
                failing = false;
                LOGGER.info("Cross-server link database is working again.");
            }
        } catch (SQLException | RuntimeException e) {
            if (!failing) {
                failing = true;
                LOGGER.warn("Cross-server link database error (will keep retrying): " + databaseFile, e);
            } else {
                LOGGER.debug("Cross-server link database error", e);
            }
            disconnect();
        }
    }

    private Connection connect() throws SQLException {
        if (connection != null) return connection;

        try {
            Path parent = databaseFile.getParent();
            if (parent != null) Files.createDirectories(parent);
        } catch (Exception e) {
            throw new SQLException("Could not create directory for " + databaseFile, e);
        }

        SQLiteConfig config = new SQLiteConfig();
        // WAL lets the servers read while another is writing.
        config.setJournalMode(SQLiteConfig.JournalMode.WAL);
        config.setSynchronous(SQLiteConfig.SynchronousMode.NORMAL);
        config.setBusyTimeout(BUSY_TIMEOUT_MILLIS);
        Connection c = config.createConnection("jdbc:sqlite:" + databaseFile);
        try (Statement s = c.createStatement()) {
            s.executeUpdate("CREATE TABLE IF NOT EXISTS messages ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT, "
                    + "origin TEXT NOT NULL, "
                    + "server_name TEXT NOT NULL, "
                    + "created_at INTEGER NOT NULL, "
                    + "content TEXT NOT NULL)");
            s.executeUpdate("CREATE INDEX IF NOT EXISTS messages_created_at ON messages (created_at)");
            s.executeUpdate("CREATE TABLE IF NOT EXISTS servers ("
                    + "server_key TEXT PRIMARY KEY, "
                    + "server_name TEXT NOT NULL, "
                    + "instance TEXT NOT NULL, "
                    + "last_seen INTEGER NOT NULL)");
            if (lastSeenId < 0) {
                // First connection: start after whatever is already there so history isn't replayed.
                try (ResultSet rs = s.executeQuery("SELECT COALESCE(MAX(id), 0) FROM messages")) {
                    lastSeenId = rs.next() ? rs.getLong(1) : 0;
                }
            }
        } catch (SQLException e) {
            try {
                c.close();
            } catch (SQLException ignored) {
            }
            throw e;
        }
        connection = c;
        return c;
    }

    private void disconnect() {
        if (connection == null) return;
        try {
            connection.close();
        } catch (SQLException e) {
            LOGGER.debug("Failed to close cross-server link database", e);
        }
        connection = null;
    }

    private void writePending(Connection c) throws SQLException {
        if (pending.isEmpty()) return;
        long now = System.currentTimeMillis();
        try (PreparedStatement insert = c.prepareStatement(
                "INSERT INTO messages (origin, server_name, created_at, content) VALUES (?, ?, ?, ?)")) {
            for (Pending entry = pending.peek(); entry != null; entry = pending.peek()) {
                if (now - entry.createdAt > MAX_PENDING_AGE_MILLIS) {
                    pending.poll();
                    LOGGER.debug("Dropping stale cross-server message: " + entry.message);
                    continue;
                }
                insert.setString(1, instanceId);
                insert.setString(2, serverName);
                insert.setLong(3, entry.createdAt);
                insert.setString(4, entry.message);
                insert.executeUpdate();
                // Only dequeue once written, so a failure retries the same message instead of losing it.
                pending.poll();
            }
        }
    }

    private void readNew(Connection c) throws SQLException {
        try (PreparedStatement select = c.prepareStatement(
                "SELECT id, origin, content FROM messages WHERE id > ? ORDER BY id LIMIT " + POLL_BATCH_SIZE)) {
            int count;
            do {
                count = 0;
                select.setLong(1, lastSeenId);
                try (ResultSet rs = select.executeQuery()) {
                    while (rs.next()) {
                        count++;
                        long id = rs.getLong(1);
                        String origin = rs.getString(2);
                        String content = rs.getString(3);
                        // Advance before broadcasting: if broadcasting throws, the message is skipped, never repeated.
                        lastSeenId = id;
                        if (instanceId.equals(origin)) continue;
                        try {
                            broadcast.accept(content);
                        } catch (RuntimeException e) {
                            LOGGER.warn("Failed to show cross-server message in game: " + content, e);
                        }
                    }
                }
            } while (count == POLL_BATCH_SIZE);
        }
    }

    private void heartbeat(Connection c, long now) throws SQLException {
        if (!checkedNameConflict) {
            checkedNameConflict = true;
            try (PreparedStatement select = c.prepareStatement(
                    "SELECT instance, last_seen FROM servers WHERE server_key = ?")) {
                select.setString(1, serverKey);
                try (ResultSet rs = select.executeQuery()) {
                    if (rs.next() && !instanceId.equals(rs.getString(1)) && now - rs.getLong(2) < SERVER_ALIVE_MILLIS) {
                        LOGGER.warn("Another running server is already linked with serverName '" + serverName
                                + "'. Give each server a unique serverName in config/bblink.toml, otherwise their"
                                + " messages can't be told apart and Discord commands will target both.");
                    }
                }
            }
        }

        try (PreparedStatement upsert = c.prepareStatement(
                "INSERT INTO servers (server_key, server_name, instance, last_seen) VALUES (?, ?, ?, ?) "
                        + "ON CONFLICT (server_key) DO UPDATE SET "
                        + "server_name = excluded.server_name, instance = excluded.instance, last_seen = excluded.last_seen")) {
            upsert.setString(1, serverKey);
            upsert.setString(2, serverName);
            upsert.setString(3, instanceId);
            upsert.setLong(4, now);
            upsert.executeUpdate();
        }

        Set<String> keys = new HashSet<>();
        keys.add(serverKey);
        try (Statement s = c.createStatement();
             ResultSet rs = s.executeQuery("SELECT server_key FROM servers")) {
            while (rs.next()) {
                keys.add(rs.getString(1));
            }
        }
        knownServerKeys = Collections.unmodifiableSet(keys);
    }

    private void prune(Connection c, long now) throws SQLException {
        try (PreparedStatement deleteMessages = c.prepareStatement("DELETE FROM messages WHERE created_at < ?");
             PreparedStatement deleteServers = c.prepareStatement("DELETE FROM servers WHERE last_seen < ?")) {
            deleteMessages.setLong(1, now - MESSAGE_RETENTION_MILLIS);
            deleteMessages.executeUpdate();
            deleteServers.setLong(1, now - SERVER_RETENTION_MILLIS);
            deleteServers.executeUpdate();
        }
    }

    private static final class Pending {
        final String message;
        final long createdAt;

        Pending(String message, long createdAt) {
            this.message = message;
            this.createdAt = createdAt;
        }
    }
}
