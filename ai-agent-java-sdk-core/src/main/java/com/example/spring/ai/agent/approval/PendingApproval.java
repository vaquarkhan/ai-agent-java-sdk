package com.example.spring.ai.agent.approval;

import com.example.spring.ai.agent.execution.AgentExecutionContext;
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
