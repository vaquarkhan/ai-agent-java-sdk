package com.example.spring.ai.strands.agent.approval;

import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

/**
 * In-memory approval manager that pauses the loop until another thread approves or denies
 * the pending tool request.
 *
 * <p>Useful for simple human approval workflows in local deployments or tests.
 *
 * @author Vaquar Khan
 */
public class ManualApprovalManager implements ApprovalManager {

    private final Duration timeout;
    private final Map<String, PendingApproval> pendingApprovals = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<ApprovalDecision>> futures = new ConcurrentHashMap<>();

    public ManualApprovalManager(Duration timeout) {
        this.timeout = timeout == null ? Duration.ofMinutes(5) : timeout;
    }

    @Override
    public ApprovalDecision awaitApproval(int iteration, String toolName, String arguments, StrandsExecutionContext context) {
        String requestId = UUID.randomUUID().toString();
        PendingApproval pending = new PendingApproval(
                requestId, iteration, toolName, arguments, context, Instant.now());
        CompletableFuture<ApprovalDecision> future = new CompletableFuture<>();
        pendingApprovals.put(requestId, pending);
        futures.put(requestId, future);
        try {
            return future.get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
        } catch (Exception e) {
            return ApprovalDecision.deny("{\"error\":\"approval_timeout\"}");
        } finally {
            pendingApprovals.remove(requestId);
            futures.remove(requestId);
        }
    }

    public Collection<PendingApproval> getPendingApprovals() {
        return pendingApprovals.values();
    }

    public boolean approve(String requestId, String arguments) {
        CompletableFuture<ApprovalDecision> future = futures.get(requestId);
        return future != null && future.complete(ApprovalDecision.approve(arguments));
    }

    public boolean deny(String requestId, String denialOutput) {
        CompletableFuture<ApprovalDecision> future = futures.get(requestId);
        return future != null && future.complete(ApprovalDecision.deny(denialOutput));
    }
}
