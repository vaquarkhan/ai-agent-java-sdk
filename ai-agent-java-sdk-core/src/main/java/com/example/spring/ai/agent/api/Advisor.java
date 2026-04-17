package com.example.spring.ai.agent.api;

import com.example.spring.ai.agent.execution.ExecutionMessage;
import com.example.spring.ai.agent.execution.AgentExecutionContext;
import java.util.List;

/**
 * @author Vaquar Khan
 */
@FunctionalInterface
public interface Advisor {

    List<ExecutionMessage> apply(List<ExecutionMessage> messages, AgentExecutionContext context);
}
