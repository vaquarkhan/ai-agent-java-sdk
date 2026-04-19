package io.github.vaquarkhan.strands.hook;

/**
 * Result of evaluating a tool call against one or more policies.
 *
 * @author Vaquar Khan
 */
public record ToolCallPolicyDecision(boolean allowed, String arguments, String denialOutput) {

    private static final String DEFAULT_DENIAL_OUTPUT = "{\"error\":\"tool_call_denied\"}";

    public static ToolCallPolicyDecision allow(String arguments) {
        return new ToolCallPolicyDecision(true, arguments, null);
    }

    public static ToolCallPolicyDecision deny(String denialOutput) {
        String output = (denialOutput == null || denialOutput.isBlank()) ? DEFAULT_DENIAL_OUTPUT : denialOutput;
        return new ToolCallPolicyDecision(false, null, output);
    }
}
