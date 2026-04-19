package io.github.vaquarkhan.strands.approval;

import io.github.vaquarkhan.strands.execution.AgentExecutionContext;

/**
 * Human-in-the-loop approval manager that can pause tool execution until a decision is made.
 *
 * @author Vaquar Khan
 */
@FunctionalInterface
public interface ApprovalManager {

    /**
     * Waits for an approval decision for the requested tool call.
     *
     * @param iteration current loop iteration
     * @param toolName requested tool name
     * @param arguments requested tool arguments
     * @param context execution context
     * @return approval decision
     */
    ApprovalDecision awaitApproval(int iteration, String toolName, String arguments, AgentExecutionContext context);
}
