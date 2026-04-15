package com.example.spring.ai.strands.agent.api;

import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import java.util.List;

/**
 * @author Vaquar Khan
 */
@FunctionalInterface
public interface Advisor {

    List<ExecutionMessage> apply(List<ExecutionMessage> messages, StrandsExecutionContext context);
}
