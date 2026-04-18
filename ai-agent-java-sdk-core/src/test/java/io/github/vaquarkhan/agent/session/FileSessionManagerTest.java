package io.github.vaquarkhan.agent.session;

import io.github.vaquarkhan.agent.execution.ExecutionMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaquar Khan
 */
class FileSessionManagerTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void saveAndLoadRoundTrip(@TempDir Path tempDir) {
        FileSessionManager manager = new FileSessionManager(tempDir, objectMapper);

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "Hello"),
                new ExecutionMessage("assistant", "Hi there"));

        manager.save("session-1", messages);
        List<ExecutionMessage> loaded = manager.load("session-1");

        assertEquals(2, loaded.size());
        assertEquals("user", loaded.get(0).role());
        assertEquals("Hello", loaded.get(0).content());
        assertEquals("assistant", loaded.get(1).role());
        assertEquals("Hi there", loaded.get(1).content());
    }

    @Test
    void deleteRemovesFile(@TempDir Path tempDir) {
        FileSessionManager manager = new FileSessionManager(tempDir, objectMapper);

        manager.save("session-1", List.of(new ExecutionMessage("user", "test")));
        assertTrue(manager.exists("session-1"));

        manager.delete("session-1");
        assertFalse(manager.exists("session-1"));
    }

    @Test
    void existsReturnsCorrectValue(@TempDir Path tempDir) {
        FileSessionManager manager = new FileSessionManager(tempDir, objectMapper);

        assertFalse(manager.exists("session-1"));

        manager.save("session-1", List.of(new ExecutionMessage("user", "test")));
        assertTrue(manager.exists("session-1"));
    }

    @Test
    void nonExistentSessionReturnsEmptyList(@TempDir Path tempDir) {
        FileSessionManager manager = new FileSessionManager(tempDir, objectMapper);

        List<ExecutionMessage> loaded = manager.load("non-existent");
        assertTrue(loaded.isEmpty());
    }

    @Test
    void saveOverwritesPreviousSession(@TempDir Path tempDir) {
        FileSessionManager manager = new FileSessionManager(tempDir, objectMapper);

        manager.save("session-1", List.of(new ExecutionMessage("user", "first")));
        manager.save("session-1", List.of(
                new ExecutionMessage("user", "second"),
                new ExecutionMessage("assistant", "response")));

        List<ExecutionMessage> loaded = manager.load("session-1");
        assertEquals(2, loaded.size());
        assertEquals("second", loaded.get(0).content());
    }

    @Test
    void multipleSessions(@TempDir Path tempDir) {
        FileSessionManager manager = new FileSessionManager(tempDir, objectMapper);

        manager.save("session-1", List.of(new ExecutionMessage("user", "msg1")));
        manager.save("session-2", List.of(new ExecutionMessage("user", "msg2")));

        assertEquals("msg1", manager.load("session-1").get(0).content());
        assertEquals("msg2", manager.load("session-2").get(0).content());
    }

    @Test
    void deleteNonExistentSessionIsNoOp(@TempDir Path tempDir) {
        FileSessionManager manager = new FileSessionManager(tempDir, objectMapper);
        // Should not throw
        manager.delete("non-existent");
        assertFalse(manager.exists("non-existent"));
    }

    @Test
    void emptyMessageListSavedAndLoaded(@TempDir Path tempDir) {
        FileSessionManager manager = new FileSessionManager(tempDir, objectMapper);

        manager.save("session-1", List.of());
        List<ExecutionMessage> loaded = manager.load("session-1");
        assertTrue(loaded.isEmpty());
        assertTrue(manager.exists("session-1"));
    }
}
