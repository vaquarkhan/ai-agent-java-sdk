package com.example.spring.ai.strands.agent.hook;

/**
 * Functional interface for handling {@link StrandsHookEvent} instances.
 *
 * <p>Hooks are registered with a {@link HookRegistry} and dispatched at specific
 * points during agent execution (before/after model calls, tool calls, and invocations).
 *
 * @author Vaquar Khan
 */
@FunctionalInterface
public interface StrandsHook {

    /**
     * Handle the given hook event.
     *
     * @param event the event dispatched by the execution loop or agent
     */
    void handle(StrandsHookEvent event);
}
