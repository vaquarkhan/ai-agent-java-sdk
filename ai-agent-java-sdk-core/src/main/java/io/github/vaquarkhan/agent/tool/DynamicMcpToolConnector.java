package io.github.vaquarkhan.agent.tool;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Facade for connecting to MCP servers at runtime (not just at startup).
 *
 * <p>This connector manages MCP connection lifecycle and delegates actual
 * MCP client creation to the Spring AI MCP infrastructure. Discovered tools
 * are registered into the provided {@link ToolRegistry} via a callback.
 *
 * <p>This mirrors the Python Strands SDK dynamic MCP client capability,
 * adapted for the Spring ecosystem.
 *
 * @author Vaquar Khan
 */
public class DynamicMcpToolConnector {

    private static final Logger log = LoggerFactory.getLogger(DynamicMcpToolConnector.class);

    private final ConcurrentHashMap<String, McpConnectionConfig> connections = new ConcurrentHashMap<>();
    private final McpClientFactory clientFactory;

    /**
     * Creates a connector with the given MCP client factory.
     *
     * @param clientFactory factory for creating MCP client connections
     */
    public DynamicMcpToolConnector(McpClientFactory clientFactory) {
        this.clientFactory = clientFactory;
    }

    /**
     * Connect to an MCP server with the given configuration.
     *
     * <p>If a connection with the same ID already exists, it is disconnected first
     * and replaced with the new connection.
     *
     * @param connectionId unique identifier for this connection
     * @param config       the MCP connection configuration
     */
    public void connect(String connectionId, McpConnectionConfig config) {
        if (connectionId == null || connectionId.isBlank()) {
            throw new IllegalArgumentException("connectionId must not be null or blank");
        }
        if (config == null) {
            throw new IllegalArgumentException("config must not be null");
        }
        if (connections.containsKey(connectionId)) {
            log.info("Replacing existing MCP connection: {}", connectionId);
            disconnect(connectionId);
        }
        connections.put(connectionId, config);
        clientFactory.connect(connectionId, config);
        log.info("Connected to MCP server: {} (transport={})", connectionId, config.transportType());
    }

    /**
     * Disconnect from an MCP server.
     *
     * @param connectionId the connection identifier to disconnect
     */
    public void disconnect(String connectionId) {
        McpConnectionConfig removed = connections.remove(connectionId);
        if (removed != null) {
            clientFactory.disconnect(connectionId);
            log.info("Disconnected MCP server: {}", connectionId);
        }
    }

    /**
     * Returns the set of active connection identifiers.
     *
     * @return unmodifiable set of connection IDs
     */
    public Set<String> listConnections() {
        return Collections.unmodifiableSet(connections.keySet());
    }

    /**
     * Returns the configuration for a specific connection.
     *
     * @param connectionId the connection identifier
     * @return the configuration, or null if not connected
     */
    public McpConnectionConfig getConnectionConfig(String connectionId) {
        return connections.get(connectionId);
    }

    /**
     * Configuration for an MCP server connection.
     *
     * @param transportType the transport type (stdio or sse)
     * @param command       the command to execute (for stdio transport)
     * @param url           the URL to connect to (for sse transport)
     * @param args          additional arguments
     * @param headers       HTTP headers (for sse transport)
     */
    public record McpConnectionConfig(
            TransportType transportType,
            String command,
            String url,
            List<String> args,
            Map<String, String> headers) {

        public McpConnectionConfig {
            args = args != null ? List.copyOf(args) : List.of();
            headers = headers != null ? Map.copyOf(headers) : Map.of();
        }

        /**
         * Creates a stdio transport configuration.
         */
        public static McpConnectionConfig stdio(String command, List<String> args) {
            return new McpConnectionConfig(TransportType.STDIO, command, null, args, Map.of());
        }

        /**
         * Creates an SSE transport configuration.
         */
        public static McpConnectionConfig sse(String url, Map<String, String> headers) {
            return new McpConnectionConfig(TransportType.SSE, null, url, List.of(), headers);
        }
    }

    /**
     * MCP transport types.
     */
    public enum TransportType {
        STDIO, SSE
    }

    /**
     * Factory interface for creating and managing MCP client connections.
     * Implementations delegate to Spring AI MCP infrastructure.
     */
    public interface McpClientFactory {

        /**
         * Create and start an MCP client connection.
         *
         * @param connectionId the connection identifier
         * @param config       the connection configuration
         */
        void connect(String connectionId, McpConnectionConfig config);

        /**
         * Stop and clean up an MCP client connection.
         *
         * @param connectionId the connection identifier
         */
        void disconnect(String connectionId);
    }
}
