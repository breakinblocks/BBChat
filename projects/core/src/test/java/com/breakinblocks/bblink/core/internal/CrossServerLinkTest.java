package com.breakinblocks.bblink.core.internal;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.CountDownLatch;
import java.util.function.BooleanSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrossServerLinkTest {
    @TempDir
    Path tempDir;

    private static void waitFor(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 15_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) return;
            Thread.sleep(25);
        }
    }

    @Test
    void messagesReachOtherServersExactlyOnceAndNeverEcho() throws Exception {
        Path db = tempDir.resolve("shared.db");
        List<String> receivedA = new CopyOnWriteArrayList<>();
        List<String> receivedB = new CopyOnWriteArrayList<>();
        CrossServerLink a = CrossServerLink.start(db, "Survival", receivedA::add);
        CrossServerLink b = CrossServerLink.start(db, "Creative", receivedB::add);
        // Let both initialise their cursors before publishing.
        Thread.sleep(500);

        a.publish("from A");
        b.publish("from B");
        waitFor(() -> receivedA.size() >= 1 && receivedB.size() >= 1);
        Thread.sleep(1_000); // give any duplicate a chance to show up

        assertEquals(Collections.singletonList("from B"), receivedA);
        assertEquals(Collections.singletonList("from A"), receivedB);

        a.close();
        b.close();
    }

    @Test
    void concurrentPublishersDeliverEverythingInOrderWithoutDuplicates() throws Exception {
        Path db = tempDir.resolve("shared.db");
        int servers = 3;
        int perServer = 300;
        List<List<String>> received = new ArrayList<>();
        List<CrossServerLink> links = new ArrayList<>();
        for (int i = 0; i < servers; i++) {
            List<String> inbox = new CopyOnWriteArrayList<>();
            received.add(inbox);
            links.add(CrossServerLink.start(db, "S" + i, inbox::add));
        }
        Thread.sleep(500);

        CountDownLatch go = new CountDownLatch(1);
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < servers; i++) {
            int server = i;
            Thread t = new Thread(() -> {
                try {
                    go.await();
                } catch (InterruptedException e) {
                    return;
                }
                for (int n = 0; n < perServer; n++) {
                    links.get(server).publish(server + ":" + n);
                }
            });
            t.start();
            threads.add(t);
        }
        go.countDown();
        for (Thread t : threads) t.join();

        int expected = (servers - 1) * perServer;
        waitFor(() -> received.stream().allMatch(r -> r.size() >= expected));
        Thread.sleep(1_000);

        for (int i = 0; i < servers; i++) {
            List<String> inbox = received.get(i);
            assertEquals(expected, inbox.size(), "server " + i + " message count");
            assertEquals(expected, inbox.stream().distinct().count(), "server " + i + " has duplicates");
            for (String message : inbox) {
                assertTrue(!message.startsWith(i + ":"), "server " + i + " received its own message " + message);
            }
            // Each sender's messages arrive in the order they were sent.
            for (int sender = 0; sender < servers; sender++) {
                if (sender == i) continue;
                String prefix = sender + ":";
                int last = -1;
                for (String message : inbox) {
                    if (!message.startsWith(prefix)) continue;
                    int n = Integer.parseInt(message.substring(prefix.length()));
                    assertEquals(last + 1, n, "server " + i + " out of order from " + sender);
                    last = n;
                }
            }
        }

        for (CrossServerLink link : links) link.close();
    }

    @Test
    void historyIsNotReplayedOnRestart() throws Exception {
        Path db = tempDir.resolve("shared.db");
        List<String> first = new CopyOnWriteArrayList<>();
        CrossServerLink a = CrossServerLink.start(db, "Survival", m -> {
        });
        CrossServerLink b = CrossServerLink.start(db, "Creative", first::add);
        Thread.sleep(500);
        a.publish("before restart");
        waitFor(() -> first.size() >= 1);
        b.close();

        List<String> second = new CopyOnWriteArrayList<>();
        CrossServerLink restarted = CrossServerLink.start(db, "Creative", second::add);
        Thread.sleep(500);
        a.publish("after restart");
        waitFor(() -> second.size() >= 1);
        Thread.sleep(500);

        assertEquals(Collections.singletonList("before restart"), first);
        assertEquals(Collections.singletonList("after restart"), second);

        a.close();
        restarted.close();
    }

    @Test
    void knownServersAreShared() throws Exception {
        Path db = tempDir.resolve("shared.db");
        CrossServerLink a = CrossServerLink.start(db, "Survival", m -> {
        });
        CrossServerLink b = CrossServerLink.start(db, "Creative World", m -> {
        });
        Thread.sleep(500);
        // b's first heartbeat may have run before a's; a's next one is 10s out, so check b sees itself and a sees b
        // once both have heartbeat.
        waitFor(() -> a.getKnownServerKeys().contains("creativeworld") || b.getKnownServerKeys().contains("survival"));
        assertTrue(a.getKnownServerKeys().contains("creativeworld") || b.getKnownServerKeys().contains("survival"));
        assertTrue(b.getKnownServerKeys().contains("creativeworld"));
        a.close();
        b.close();
    }
}
