package com.example.spring.ai.agent.execution;

import com.example.spring.ai.agent.model.ReasoningTrace;

/**
 * @author Vaquar Khan
 */

public class AgentExecutionException extends RuntimeException {

    private final int iterationNumber;
    private final ReasoningTrace partialTrace;

    public AgentExecutionException(String message, int iterationNumber, ReasoningTrace partialTrace) {
        super(message);
        this.iterationNumber = iterationNumber;
        this.partialTrace = partialTrace;
    }

    public AgentExecutionException(String message, Throwable cause, int iterationNumber, ReasoningTrace partialTrace) {
        super(message, cause);
        this.iterationNumber = iterationNumber;
        this.partialTrace = partialTrace;
    }

    public int getIterationNumber() {
        return iterationNumber;
    }

    public ReasoningTrace getPartialTrace() {
        return partialTrace;
    }
}
