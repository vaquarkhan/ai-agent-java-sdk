package com.example.spring.ai.strands.agent.execution;

import com.example.spring.ai.strands.agent.model.ReasoningTrace;

/**
 * @author Vaquar Khan
 */

public class StrandsExecutionException extends RuntimeException {

    private final int iterationNumber;
    private final ReasoningTrace partialTrace;

    public StrandsExecutionException(String message, int iterationNumber, ReasoningTrace partialTrace) {
        super(message);
        this.iterationNumber = iterationNumber;
        this.partialTrace = partialTrace;
    }

    public StrandsExecutionException(String message, Throwable cause, int iterationNumber, ReasoningTrace partialTrace) {
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
