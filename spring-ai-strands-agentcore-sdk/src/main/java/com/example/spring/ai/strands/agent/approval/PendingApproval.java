package com.example.spring.ai.strands.agent.approval;

import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
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
        StrandsExecutionContext context,
        Instant createdAt) {}
