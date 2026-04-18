package io.github.vaquarkhan.agent.approval;

import io.github.vaquarkhan.agent.execution.AgentExecutionContext;
import java.time.Instant;

/**
 * Immutable description of a pending approval request.
 *
 * @author Vaquar Khan
 */
public record PendingApproval(
        String requestId,
        int iteration,
        String toolName,
        String arguments,
        AgentExecutionContext context,
        Instant createdAt) {}
