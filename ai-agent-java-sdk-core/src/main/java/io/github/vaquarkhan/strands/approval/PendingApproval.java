package io.github.vaquarkhan.strands.approval;

import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
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
