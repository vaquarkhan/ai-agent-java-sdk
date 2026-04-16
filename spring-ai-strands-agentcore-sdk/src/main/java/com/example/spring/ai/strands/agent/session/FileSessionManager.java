package com.example.spring.ai.strands.agent.session;

import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Session manager that persists conversation messages as JSON files.
 *
 * <p>Each session is stored as a separate file named {@code {sessionId}.json}
 * in the configured directory. Thread safety is ensured via file locking.
 *
 * @author Vaquar Khan
 */
public class FileSessionManager implements SessionManager {

    private static final Logger log = LoggerFactory.getLogger(FileSessionManager.class);
    private static final TypeReference<List<ExecutionMessage>> MESSAGE_LIST_TYPE = new TypeReference<>() {};

    private final Path directory;
    private final ObjectMapper objectMapper;

    /**
     * Creates a file-based session manager.
     *
     * @param directory    the directory to store session files in
     * @param objectMapper the Jackson ObjectMapper for serialization
     */
    public FileSessionManager(Path directory, ObjectMapper objectMapper) {
        this.directory = directory;
        this.objectMapper = objectMapper;
        try {
            Files.createDirectories(directory);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to create session directory: " + directory, e);
        }
    }

    @Override
    public void save(String sessionId, List<ExecutionMessage> messages) {
        Path file = sessionFile(sessionId);
        Path lockFile = lockFile(sessionId);
        try {
            Files.createDirectories(directory);
            // Use file locking for thread safety
            try (FileChannel channel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock lock = channel.lock()) {
                objectMapper.writeValue(file.toFile(), messages);
            }
        } catch (IOException e) {
            log.error("Failed to save session {}: {}", sessionId, e.getMessage(), e);
            throw new RuntimeException("Failed to save session: " + sessionId, e);
        }
    }

    @Override
    public List<ExecutionMessage> load(String sessionId) {
        Path file = sessionFile(sessionId);
        if (!Files.exists(file)) {
            return List.of();
        }
        Path lockFile = lockFile(sessionId);
        try {
            try (FileChannel channel = FileChannel.open(lockFile,
                    StandardOpenOption.CREATE, StandardOpenOption.WRITE);
                 FileLock lock = channel.lock()) {
                return objectMapper.readValue(file.toFile(), MESSAGE_LIST_TYPE);
            }
        } catch (IOException e) {
            log.error("Failed to load session {}: {}", sessionId, e.getMessage(), e);
            return List.of();
        }
    }

    @Override
    public void delete(String sessionId) {
        Path file = sessionFile(sessionId);
        Path lockFile = lockFile(sessionId);
        try {
            Files.deleteIfExists(file);
            Files.deleteIfExists(lockFile);
        } catch (IOException e) {
            log.warn("Failed to delete session {}: {}", sessionId, e.getMessage());
        }
    }

    @Override
    public boolean exists(String sessionId) {
        return Files.exists(sessionFile(sessionId));
    }

    private Path sessionFile(String sessionId) {
        return directory.resolve(sessionId + ".json");
    }

    private Path lockFile(String sessionId) {
        return directory.resolve(sessionId + ".lock");
    }
}
