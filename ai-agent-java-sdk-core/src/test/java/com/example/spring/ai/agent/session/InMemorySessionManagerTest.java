package com.example.spring.ai.agent.session;

import com.example.spring.ai.agent.execution.ExecutionMessage;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaquar Khan
 */
class InMemorySessionManagerTest {

    @Test
    void saveAndLoad() {
        InMemorySessionManager manager = new InMemorySessionManager();

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "Hello"),
                new ExecutionMessage("assistant", "Hi"));

        manager.save("session-1", messages);
        List<ExecutionMessage> loaded = manager.load("session-1");

        assertEquals(2, loaded.size());
        assertEquals("Hello", loaded.get(0).content());
        assertEquals("Hi", loaded.get(1).content());
    }

    @Test
    void deleteRemovesSession() {
        InMemorySessionManager manager = new InMemorySessionManager();

        manager.save("session-1", List.of(new ExecutionMessage("user", "test")));
        assertTrue(manager.exists("session-1"));

        manager.delete("session-1");
        assertFalse(manager.exists("session-1"));
        assertTrue(manager.load("session-1").isEmpty());
    }

    @Test
    void existsReturnsFalseForUnknownSession() {
        InMemorySessionManager manager = new InMemorySessionManager();
        assertFalse(manager.exists("unknown"));
    }

    @Test
    void existsReturnsTrueForSavedSession() {
        InMemorySessionManager manager = new InMemorySessionManager();
        manager.save("session-1", List.of(new ExecutionMessage("user", "hi")));
        assertTrue(manager.exists("session-1"));
    }

    @Test
    void loadNonExistentSessionReturnsEmptyList() {
        InMemorySessionManager manager = new InMemorySessionManager();
        List<ExecutionMessage> loaded = manager.load("non-existent");
        assertTrue(loaded.isEmpty());
    }

    @Test
    void saveOverwritesPreviousSession() {
        InMemorySessionManager manager = new InMemorySessionManager();

        manager.save("session-1", List.of(new ExecutionMessage("user", "first")));
        manager.save("session-1", List.of(new ExecutionMessage("user", "second")));

        List<ExecutionMessage> loaded = manager.load("session-1");
        assertEquals(1, loaded.size());
        assertEquals("second", loaded.get(0).content());
    }

    @Test
    void loadReturnsCopyNotReference() {
        InMemorySessionManager manager = new InMemorySessionManager();

        List<ExecutionMessage> original = new ArrayList<>();
        original.add(new ExecutionMessage("user", "hello"));
        manager.save("session-1", original);

        List<ExecutionMessage> loaded = manager.load("session-1");
        loaded.add(new ExecutionMessage("user", "extra"));

        // Original stored data should not be affected
        assertEquals(1, manager.load("session-1").size());
    }

    @Test
    void concurrentAccessFromMultipleThreads() throws InterruptedException {
        InMemorySessionManager manager = new InMemorySessionManager();
        int threadCount = 10;
        int operationsPerThread = 100;
        CountDownLatch latch = new CountDownLatch(threadCount);
        AtomicInteger errors = new AtomicInteger(0);

        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        for (int t = 0; t < threadCount; t++) {
            final int threadId = t;
            executor.submit(() -> {
                try {
                    for (int i = 0; i < operationsPerThread; i++) {
                        String sessionId = "session-" + threadId + "-" + i;
                        List<ExecutionMessage> messages = List.of(
                                new ExecutionMessage("user", "msg-" + threadId + "-" + i));
                        manager.save(sessionId, messages);
                        List<ExecutionMessage> loaded = manager.load(sessionId);
                        if (loaded.isEmpty()) {
                            errors.incrementAndGet();
                        }
                        manager.exists(sessionId);
                        manager.delete(sessionId);
                    }
                } catch (Exception e) {
                    errors.incrementAndGet();
                } finally {
                    latch.countDown();
                }
            });
        }

        assertTrue(latch.await(30, TimeUnit.SECONDS));
        executor.shutdown();
        assertEquals(0, errors.get(), "Concurrent operations should not produce errors");
    }

    @Test
    void multipleSessions() {
        InMemorySessionManager manager = new InMemorySessionManager();

        manager.save("s1", List.of(new ExecutionMessage("user", "msg1")));
        manager.save("s2", List.of(new ExecutionMessage("user", "msg2")));
        manager.save("s3", List.of(new ExecutionMessage("user", "msg3")));

        assertEquals("msg1", manager.load("s1").get(0).content());
        assertEquals("msg2", manager.load("s2").get(0).content());
        assertEquals("msg3", manager.load("s3").get(0).content());
    }
}
