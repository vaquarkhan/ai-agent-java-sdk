package io.github.vaquarkhan.strands.hook;

import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import io.github.vaquarkhan.strands.execution.ModelTurnResponse;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import io.github.vaquarkhan.strands.execution.AgentLoopResult;
import io.github.vaquarkhan.strands.model.AgentResponse;
import io.github.vaquarkhan.strands.model.ToolExecutionResult;
import java.util.List;

/**
 * Sealed hierarchy of hook events dispatched during agent execution.
 *
 * <p>Mirrors the Python Strands SDK event model: before/after invocation,
 * before/after model call, and before/after tool call.
 *
 * @author Vaquar Khan
 */
public sealed interface AgentHookEvent
        permits AgentHookEvent.BeforeInvocation,
                AgentHookEvent.BeforeModelCall,
                AgentHookEvent.AfterModelCall,
                AgentHookEvent.BeforeToolCall,
                AgentHookEvent.AfterToolCall,
                AgentHookEvent.AfterInvocation {

    /**
     * Dispatched before the agent execution loop begins.
     */
    record BeforeInvocation(String userPrompt, AgentExecutionContext context) implements AgentHookEvent {
    }

    /**
     * Dispatched before each model call inside the execution loop.
     */
    record BeforeModelCall(int iteration, List<ExecutionMessage> messages) implements AgentHookEvent {
    }

    /**
     * Dispatched after each model call returns a response.
     */
    record AfterModelCall(int iteration, ModelTurnResponse response) implements AgentHookEvent {
    }

    /**
     * Dispatched before a tool is executed.
     */
    record BeforeToolCall(int iteration, String toolName, String arguments) implements AgentHookEvent {
    }

    /**
     * Dispatched after a tool execution completes.
     */
    record AfterToolCall(int iteration, String toolName, ToolExecutionResult result) implements AgentHookEvent {
    }

    /**
     * Dispatched after the agent invocation completes (at the agent level).
     */
    record AfterInvocation(AgentResponse response) implements AgentHookEvent {
    }
}
