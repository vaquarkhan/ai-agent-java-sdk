package com.example.spring.ai.strands.agent.hook;

import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.execution.StrandsLoopResult;
import com.example.spring.ai.strands.agent.model.StrandsAgentResponse;
import com.example.spring.ai.strands.agent.model.ToolExecutionResult;
import java.util.List;

/**
 * Sealed hierarchy of hook events dispatched during agent execution.
 *
 * <p>Mirrors the Python Strands SDK event model: before/after invocation,
 * before/after model call, and before/after tool call.
 *
 * @author Vaquar Khan
 */
public sealed interface StrandsHookEvent
        permits StrandsHookEvent.BeforeInvocation,
                StrandsHookEvent.BeforeModelCall,
                StrandsHookEvent.AfterModelCall,
                StrandsHookEvent.BeforeToolCall,
                StrandsHookEvent.AfterToolCall,
                StrandsHookEvent.AfterInvocation {

    /**
     * Dispatched before the agent execution loop begins.
     */
    record BeforeInvocation(String userPrompt, StrandsExecutionContext context) implements StrandsHookEvent {
    }

    /**
     * Dispatched before each model call inside the execution loop.
     */
    record BeforeModelCall(int iteration, List<ExecutionMessage> messages) implements StrandsHookEvent {
    }

    /**
     * Dispatched after each model call returns a response.
     */
    record AfterModelCall(int iteration, ModelTurnResponse response) implements StrandsHookEvent {
    }

    /**
     * Dispatched before a tool is executed.
     */
    record BeforeToolCall(int iteration, String toolName, String arguments) implements StrandsHookEvent {
    }

    /**
     * Dispatched after a tool execution completes.
     */
    record AfterToolCall(int iteration, String toolName, ToolExecutionResult result) implements StrandsHookEvent {
    }

    /**
     * Dispatched after the agent invocation completes (at the agent level).
     */
    record AfterInvocation(StrandsAgentResponse response) implements StrandsHookEvent {
    }
}
