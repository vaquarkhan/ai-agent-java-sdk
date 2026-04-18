package io.github.vaquarkhan.agent.security;

import java.util.regex.Pattern;

/**
 * Centralizes security-related constants used across the SDK.
 *
 * <p>Security logic in this project is intentionally distributed across the classes that enforce it:
 * <ul>
 *   <li>{@link io.github.vaquarkhan.agent.tool.ToolRegistry} - argument size limits,
 *       execution timeouts, rate limiting, and tool name validation</li>
 *   <li>{@link io.github.vaquarkhan.agent.config.AiAgentProperties} - SSRF prevention,
 *       actuator property hiding, and configuration validation</li>
 *   <li>{@link io.github.vaquarkhan.agent.observability.AgentObservability} - output
 *       sanitization (AWS keys, JWTs, emails) and trace truncation</li>
 * </ul>
 *
 * <p>This class provides shared constants so that validation rules remain consistent when referenced
 * from multiple locations (e.g., both {@code ToolBridge} and {@code ToolRegistry} use the same
 * safe tool name pattern).
 *
 * @author Vaquar Khan
 */
public final class SecurityConstants {

    private SecurityConstants() {}

    // -- Tool name validation --

    /** Only alphanumeric characters, underscores, and hyphens are allowed in tool names. */
    public static final Pattern SAFE_TOOL_NAME = Pattern.compile("[a-zA-Z0-9_-]+");

    // -- Tool execution error messages --

    public static final String UNKNOWN_TOOL_MESSAGE = "{\"error\":\"tool_not_found\"}";
    public static final String RATE_LIMIT_MESSAGE = "{\"error\":\"tool_rate_limit\"}";
    public static final String INVALID_TOOL_NAME_MESSAGE = "{\"error\":\"invalid_tool_name\"}";
    public static final String TOOL_ARGUMENT_TOO_LARGE_MESSAGE = "{\"error\":\"tool_argument_too_large\"}";
    public static final String TOOL_TIMEOUT_MESSAGE = "{\"error\":\"tool_timeout\"}";
    public static final String TOOL_EXECUTION_FAILED_MESSAGE = "{\"error\":\"tool_execution_failed\"}";

    // -- Sanitization patterns --

    /** Matches AWS access key IDs (AKIA followed by 16 alphanumeric characters). */
    public static final Pattern AWS_ACCESS_KEY_PATTERN = Pattern.compile("AKIA[0-9A-Z]{16}");

    /** Matches JWT tokens (three base64url segments separated by dots). */
    public static final Pattern JWT_PATTERN =
            Pattern.compile("[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+\\.[A-Za-z0-9-_]+");

    /** Matches email addresses. */
    public static final Pattern EMAIL_PATTERN =
            Pattern.compile("\\b[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}\\b");

    // -- Sanitization replacement strings --

    public static final String REDACTED_AWS_KEY = "[REDACTED_AWS_KEY]";
    public static final String REDACTED_JWT = "[REDACTED_JWT]";
    public static final String REDACTED_EMAIL = "[REDACTED_EMAIL]";
}
