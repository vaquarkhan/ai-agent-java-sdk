package com.example.spring.ai.agent.hook;

/**
 * Functional interface for handling {@link AgentHookEvent} instances.
 *
 * <p>Hooks are registered with a {@link HookRegistry} and dispatched at specific
 * points during agent execution (before/after model calls, tool calls, and invocations).
 *
 * @author Vaquar Khan
 */
@FunctionalInterface
public interface AgentHook {

    /**
     * Handle the given hook event.
     *
     * @param event the event dispatched by the execution loop or agent
     */
    void handle(AgentHookEvent event);
}
