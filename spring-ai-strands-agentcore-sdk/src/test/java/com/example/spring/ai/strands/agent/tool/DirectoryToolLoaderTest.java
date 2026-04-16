package com.example.spring.ai.strands.agent.tool;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.ai.tool.ToolCallback;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaquar Khan
 */
class DirectoryToolLoaderTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void loadToolsFromDirectory(@TempDir Path tempDir) throws IOException {
        // Create a valid tool definition file
        String json = """
                {
                    "name": "greet",
                    "description": "Greets the user",
                    "command": "echo hello"
                }
                """;
        Files.writeString(tempDir.resolve("greet.json"), json);

        DirectoryToolLoader loader = new DirectoryToolLoader(tempDir, objectMapper);
        loader.loadInitial();

        Map<String, ToolCallback> tools = loader.getTools();
        assertEquals(1, tools.size());
        assertTrue(tools.containsKey("greet"));
        assertNotNull(tools.get("greet").getToolDefinition());
        assertEquals("greet", tools.get("greet").getToolDefinition().name());
    }

    @Test
    void multipleToolFilesLoaded(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("tool1.json"),
                "{\"name\":\"tool1\",\"description\":\"Tool 1\",\"command\":\"echo 1\"}");
        Files.writeString(tempDir.resolve("tool2.json"),
                "{\"name\":\"tool2\",\"description\":\"Tool 2\",\"command\":\"echo 2\"}");

        DirectoryToolLoader loader = new DirectoryToolLoader(tempDir, objectMapper);
        loader.loadInitial();

        assertEquals(2, loader.getTools().size());
        assertTrue(loader.getTools().containsKey("tool1"));
        assertTrue(loader.getTools().containsKey("tool2"));
    }

    @Test
    void invalidJsonFilesAreSkipped(@TempDir Path tempDir) throws IOException {
        // Valid tool
        Files.writeString(tempDir.resolve("valid.json"),
                "{\"name\":\"valid\",\"description\":\"Valid\",\"command\":\"echo ok\"}");
        // Invalid JSON
        Files.writeString(tempDir.resolve("invalid.json"), "this is not json{{{");

        DirectoryToolLoader loader = new DirectoryToolLoader(tempDir, objectMapper);
        loader.loadInitial();

        assertEquals(1, loader.getTools().size());
        assertTrue(loader.getTools().containsKey("valid"));
    }

    @Test
    void emptyDirectoryProducesNoTools(@TempDir Path tempDir) {
        DirectoryToolLoader loader = new DirectoryToolLoader(tempDir, objectMapper);
        loader.loadInitial();

        assertTrue(loader.getTools().isEmpty());
    }

    @Test
    void nonExistentDirectoryProducesNoTools() {
        Path nonExistent = Path.of("non-existent-dir-" + System.nanoTime());
        DirectoryToolLoader loader = new DirectoryToolLoader(nonExistent, objectMapper);
        loader.loadInitial();

        assertTrue(loader.getTools().isEmpty());
    }

    @Test
    void toolDefinitionMissingNameIsSkipped(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("noname.json"),
                "{\"description\":\"No name\",\"command\":\"echo x\"}");

        DirectoryToolLoader loader = new DirectoryToolLoader(tempDir, objectMapper);
        loader.loadInitial();

        assertTrue(loader.getTools().isEmpty());
    }

    @Test
    void toolDefinitionMissingCommandIsSkipped(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("nocmd.json"),
                "{\"name\":\"nocmd\",\"description\":\"No command\"}");

        DirectoryToolLoader loader = new DirectoryToolLoader(tempDir, objectMapper);
        loader.loadInitial();

        assertTrue(loader.getTools().isEmpty());
    }

    @Test
    void nonJsonFilesAreIgnored(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("readme.txt"), "This is not a tool");
        Files.writeString(tempDir.resolve("tool.json"),
                "{\"name\":\"tool\",\"description\":\"A tool\",\"command\":\"echo hi\"}");

        DirectoryToolLoader loader = new DirectoryToolLoader(tempDir, objectMapper);
        loader.loadInitial();

        assertEquals(1, loader.getTools().size());
        assertTrue(loader.getTools().containsKey("tool"));
    }

    @Test
    void toolsMapIsUnmodifiable(@TempDir Path tempDir) throws IOException {
        Files.writeString(tempDir.resolve("tool.json"),
                "{\"name\":\"tool\",\"description\":\"A tool\",\"command\":\"echo hi\"}");

        DirectoryToolLoader loader = new DirectoryToolLoader(tempDir, objectMapper);
        loader.loadInitial();

        Map<String, ToolCallback> tools = loader.getTools();
        org.junit.jupiter.api.Assertions.assertThrows(UnsupportedOperationException.class,
                () -> tools.put("new", null));
    }
}
