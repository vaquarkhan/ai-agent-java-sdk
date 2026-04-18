package io.github.vaquarkhan.agent.tool;

import io.github.vaquarkhan.agent.tool.DynamicMcpToolConnector.McpClientFactory;
import io.github.vaquarkhan.agent.tool.DynamicMcpToolConnector.McpConnectionConfig;
import io.github.vaquarkhan.agent.tool.DynamicMcpToolConnector.TransportType;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaquar Khan
 */
class DynamicMcpToolConnectorTest {

    @Test
    void connectAndListConnections() {
        RecordingFactory factory = new RecordingFactory();
        DynamicMcpToolConnector connector = new DynamicMcpToolConnector(factory);

        McpConnectionConfig config = McpConnectionConfig.stdio("python", List.of("server.py"));
        connector.connect("mcp-1", config);

        Set<String> connections = connector.listConnections();
        assertEquals(1, connections.size());
        assertTrue(connections.contains("mcp-1"));
        assertEquals(1, factory.connectCalls.size());
    }

    @Test
    void disconnectRemovesConnection() {
        RecordingFactory factory = new RecordingFactory();
        DynamicMcpToolConnector connector = new DynamicMcpToolConnector(factory);

        connector.connect("mcp-1", McpConnectionConfig.stdio("cmd", List.of()));
        connector.disconnect("mcp-1");

        assertTrue(connector.listConnections().isEmpty());
        assertEquals(1, factory.disconnectCalls.size());
        assertEquals("mcp-1", factory.disconnectCalls.get(0));
    }

    @Test
    void disconnectNonExistentConnectionIsNoOp() {
        RecordingFactory factory = new RecordingFactory();
        DynamicMcpToolConnector connector = new DynamicMcpToolConnector(factory);

        connector.disconnect("non-existent");
        assertTrue(factory.disconnectCalls.isEmpty());
    }

    @Test
    void connectWithSameIdReplacesPreviousConnection() {
        RecordingFactory factory = new RecordingFactory();
        DynamicMcpToolConnector connector = new DynamicMcpToolConnector(factory);

        McpConnectionConfig config1 = McpConnectionConfig.stdio("cmd1", List.of());
        McpConnectionConfig config2 = McpConnectionConfig.sse("http://localhost:8080", Map.of());

        connector.connect("mcp-1", config1);
        connector.connect("mcp-1", config2);

        assertEquals(1, connector.listConnections().size());
        // Should have disconnected the first, then connected twice
        assertEquals(1, factory.disconnectCalls.size());
        assertEquals(2, factory.connectCalls.size());

        McpConnectionConfig current = connector.getConnectionConfig("mcp-1");
        assertNotNull(current);
        assertEquals(TransportType.SSE, current.transportType());
    }

    @Test
    void multipleConnectionsTracked() {
        RecordingFactory factory = new RecordingFactory();
        DynamicMcpToolConnector connector = new DynamicMcpToolConnector(factory);

        connector.connect("mcp-1", McpConnectionConfig.stdio("cmd1", List.of()));
        connector.connect("mcp-2", McpConnectionConfig.sse("http://localhost", Map.of()));
        connector.connect("mcp-3", McpConnectionConfig.stdio("cmd3", List.of("arg")));

        assertEquals(3, connector.listConnections().size());
        assertTrue(connector.listConnections().containsAll(Set.of("mcp-1", "mcp-2", "mcp-3")));
    }

    @Test
    void getConnectionConfigReturnsNullForUnknown() {
        DynamicMcpToolConnector connector = new DynamicMcpToolConnector(new RecordingFactory());
        assertNull(connector.getConnectionConfig("unknown"));
    }

    @Test
    void connectWithNullIdThrows() {
        DynamicMcpToolConnector connector = new DynamicMcpToolConnector(new RecordingFactory());
        assertThrows(IllegalArgumentException.class,
                () -> connector.connect(null, McpConnectionConfig.stdio("cmd", List.of())));
    }

    @Test
    void connectWithBlankIdThrows() {
        DynamicMcpToolConnector connector = new DynamicMcpToolConnector(new RecordingFactory());
        assertThrows(IllegalArgumentException.class,
                () -> connector.connect("  ", McpConnectionConfig.stdio("cmd", List.of())));
    }

    @Test
    void connectWithNullConfigThrows() {
        DynamicMcpToolConnector connector = new DynamicMcpToolConnector(new RecordingFactory());
        assertThrows(IllegalArgumentException.class,
                () -> connector.connect("mcp-1", null));
    }

    @Test
    void stdioConfigFactory() {
        McpConnectionConfig config = McpConnectionConfig.stdio("python", List.of("server.py", "--port", "8080"));
        assertEquals(TransportType.STDIO, config.transportType());
        assertEquals("python", config.command());
        assertNull(config.url());
        assertEquals(3, config.args().size());
        assertTrue(config.headers().isEmpty());
    }

    @Test
    void sseConfigFactory() {
        McpConnectionConfig config = McpConnectionConfig.sse("http://localhost:8080",
                Map.of("Authorization", "Bearer token"));
        assertEquals(TransportType.SSE, config.transportType());
        assertNull(config.command());
        assertEquals("http://localhost:8080", config.url());
        assertTrue(config.args().isEmpty());
        assertEquals(1, config.headers().size());
    }

    @Test
    void listConnectionsIsUnmodifiable() {
        DynamicMcpToolConnector connector = new DynamicMcpToolConnector(new RecordingFactory());
        connector.connect("mcp-1", McpConnectionConfig.stdio("cmd", List.of()));

        Set<String> connections = connector.listConnections();
        assertThrows(UnsupportedOperationException.class, () -> connections.add("mcp-2"));
    }

    /**
     * Recording factory that tracks connect/disconnect calls.
     */
    static class RecordingFactory implements McpClientFactory {
        final List<String> connectCalls = new ArrayList<>();
        final List<String> disconnectCalls = new ArrayList<>();

        @Override
        public void connect(String connectionId, McpConnectionConfig config) {
            connectCalls.add(connectionId);
        }

        @Override
        public void disconnect(String connectionId) {
            disconnectCalls.add(connectionId);
        }
    }
}
