package com.example.spring.ai.strands.agent.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.Collections;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.DefaultToolDefinition;
import org.springframework.ai.tool.definition.ToolDefinition;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

/**
 * Watches a directory for {@code .json} tool definition files and maintains
 * a live map of shell-command-based tools.
 *
 * <p>Each JSON file defines a tool with the following structure:
 * <pre>{@code
 * {
 *   "name": "my_tool",
 *   "description": "Does something useful",
 *   "command": "echo hello"
 * }
 * }</pre>
 *
 * <p>When files are added, removed, or modified, the internal tool map is updated.
 * This is a simplified Java equivalent of the Python Strands SDK
 * {@code load_tools_from_directory} function.
 *
 * @author Vaquar Khan
 */
public class DirectoryToolLoader implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(DirectoryToolLoader.class);

    private final Path directory;
    private final ObjectMapper objectMapper;
    private final ConcurrentHashMap<String, ToolCallback> tools = new ConcurrentHashMap<>();
    private final AtomicBoolean watching = new AtomicBoolean(false);
    private volatile Thread watchThread;

    /**
     * Creates a directory tool loader for the specified directory.
     *
     * @param directory    the directory to watch for .json tool definitions
     * @param objectMapper the Jackson ObjectMapper for parsing JSON files
     */
    public DirectoryToolLoader(Path directory, ObjectMapper objectMapper) {
        this.directory = directory;
        this.objectMapper = objectMapper;
    }

    /**
     * Performs an initial scan of the directory and loads all valid .json tool definitions.
     *
     * @return this loader for chaining
     */
    public DirectoryToolLoader loadInitial() {
        if (!Files.isDirectory(directory)) {
            log.warn("Tool directory does not exist: {}", directory);
            return this;
        }
        try (DirectoryStream<Path> stream = Files.newDirectoryStream(directory, "*.json")) {
            for (Path file : stream) {
                loadToolFromFile(file);
            }
        } catch (IOException e) {
            log.warn("Failed to scan tool directory {}: {}", directory, e.getMessage());
        }
        return this;
    }

    /**
     * Starts watching the directory for changes in a background daemon thread.
     * Call {@link #close()} to stop watching.
     */
    public void startWatching() {
        if (!Files.isDirectory(directory)) {
            log.warn("Cannot watch non-existent directory: {}", directory);
            return;
        }
        if (watching.compareAndSet(false, true)) {
            watchThread = new Thread(this::watchLoop, "strands-tool-dir-watcher");
            watchThread.setDaemon(true);
            watchThread.start();
        }
    }

    /**
     * Returns an unmodifiable view of the currently loaded tools.
     *
     * @return map of tool name to ToolCallback
     */
    public Map<String, ToolCallback> getTools() {
        return Collections.unmodifiableMap(tools);
    }

    @Override
    public void close() {
        watching.set(false);
        if (watchThread != null) {
            watchThread.interrupt();
        }
    }

    private void watchLoop() {
        try (WatchService watchService = directory.getFileSystem().newWatchService()) {
            directory.register(watchService, ENTRY_CREATE, ENTRY_MODIFY, ENTRY_DELETE);
            while (watching.get() && !Thread.currentThread().isInterrupted()) {
                WatchKey key = watchService.take();
                for (WatchEvent<?> event : key.pollEvents()) {
                    WatchEvent.Kind<?> kind = event.kind();
                    @SuppressWarnings("unchecked")
                    WatchEvent<Path> pathEvent = (WatchEvent<Path>) event;
                    Path fileName = pathEvent.context();
                    if (fileName == null || !fileName.toString().endsWith(".json")) {
                        continue;
                    }
                    Path fullPath = directory.resolve(fileName);
                    if (kind == ENTRY_DELETE) {
                        removeToolFromFile(fileName.toString());
                    } else {
                        loadToolFromFile(fullPath);
                    }
                }
                if (!key.reset()) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (IOException e) {
            log.warn("Watch service error for {}: {}", directory, e.getMessage());
        }
    }

    void loadToolFromFile(Path file) {
        try {
            ToolJsonDefinition def = objectMapper.readValue(file.toFile(), ToolJsonDefinition.class);
            if (def.name == null || def.name.isBlank() || def.command == null || def.command.isBlank()) {
                log.warn("Skipping invalid tool definition (missing name or command): {}", file);
                return;
            }
            String description = def.description != null ? def.description : def.name;
            String command = def.command;
            ToolCallback callback = new ShellCommandToolCallback(def.name, description, command);
            tools.put(def.name, callback);
            log.debug("Loaded tool '{}' from {}", def.name, file);
        } catch (IOException e) {
            log.warn("Skipping invalid JSON file {}: {}", file, e.getMessage());
        }
    }

    private void removeToolFromFile(String fileName) {
        // Remove any tool whose source file matches
        String baseName = fileName.replace(".json", "");
        tools.entrySet().removeIf(entry -> entry.getKey().equals(baseName));
        log.debug("Removed tool for deleted file: {}", fileName);
    }

    /**
     * JSON structure for a tool definition file.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    static class ToolJsonDefinition {
        public String name;
        public String description;
        public String command;
    }

    /**
     * A ToolCallback that executes a shell command.
     */
    static class ShellCommandToolCallback implements ToolCallback {

        private final ToolDefinition definition;
        private final String command;

        ShellCommandToolCallback(String name, String description, String command) {
            this.definition = DefaultToolDefinition.builder()
                    .name(name)
                    .description(description)
                    .inputSchema("{\"type\":\"object\"}")
                    .build();
            this.command = command;
        }

        @Override
        public ToolDefinition getToolDefinition() {
            return definition;
        }

        @Override
        public String call(String arguments) {
            try {
                ProcessBuilder pb = new ProcessBuilder("bash", "-c", command);
                pb.redirectErrorStream(true);
                Process process = pb.start();
                String output = new String(process.getInputStream().readAllBytes());
                int exitCode = process.waitFor();
                if (exitCode != 0) {
                    return "{\"error\":\"command_failed\",\"exitCode\":" + exitCode + ",\"output\":\"" + output.trim() + "\"}";
                }
                return output.trim();
            } catch (Exception e) {
                return "{\"error\":\"command_execution_failed\",\"message\":\"" + e.getMessage() + "\"}";
            }
        }

        String getCommand() {
            return command;
        }
    }
}
