package io.github.vaquarkhan.strands.api;

import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import java.util.List;

/**
 * @author Vaquar Khan
 */
@FunctionalInterface
public interface Advisor {

    List<ExecutionMessage> apply(List<ExecutionMessage> messages, AgentExecutionContext context);
}
