package io.github.vaquarkhan.strands.hook;

/**
 * Policy hook that can allow, deny, or rewrite tool-call arguments before execution.
 *
 * <p>This enables human-in-the-loop approval workflows and guardrails that actively
 * control execution instead of only observing events.
 *
 * @author Vaquar Khan
 */
@FunctionalInterface
public interface ToolCallPolicy {

    /**
     * Evaluates a tool call candidate.
     *
     * @param iteration current loop iteration number
     * @param toolName requested tool name
     * @param arguments requested tool arguments (JSON string)
     * @return decision indicating allow/deny and optional argument override
     */
    ToolCallPolicyDecision evaluate(int iteration, String toolName, String arguments);
}
