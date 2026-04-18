package io.github.vaquarkhan.agent.api;

import io.github.vaquarkhan.agent.execution.ExecutionMessage;
import io.github.vaquarkhan.agent.execution.AgentExecutionContext;
import java.util.List;

/**
 * @author Vaquar Khan
 */
@FunctionalInterface
public interface Advisor {

    List<ExecutionMessage> apply(List<ExecutionMessage> messages, AgentExecutionContext context);
}
