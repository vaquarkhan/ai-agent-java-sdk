package io.github.vaquarkhan.agent.hook;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry that holds {@link AgentHook} instances keyed by event type and dispatches
 * events to all matching hooks.
 *
 * <p>Hook exceptions are logged as warnings and do not interrupt the execution loop.
 *
 * @author Vaquar Khan
 */
public class HookRegistry {

    private static final Logger log = LoggerFactory.getLogger(HookRegistry.class);

    private final ConcurrentHashMap<Class<? extends AgentHookEvent>, CopyOnWriteArrayList<AgentHook>> hooks =
            new ConcurrentHashMap<>();
    private final CopyOnWriteArrayList<ToolCallPolicy> toolCallPolicies = new CopyOnWriteArrayList<>();

    /**
     * Register a hook for a specific event type.
     *
     * @param eventType the event class to listen for
     * @param hook      the hook to invoke when the event is dispatched
     */
    public void register(Class<? extends AgentHookEvent> eventType, AgentHook hook) {
        hooks.computeIfAbsent(eventType, k -> new CopyOnWriteArrayList<>()).add(hook);
    }

    /**
     * Dispatch an event to all hooks registered for its type.
     *
     * <p>If a hook throws an exception, it is logged as a warning and the remaining
     * hooks continue to execute.
     *
     * @param event the event to dispatch
     */
    public void dispatch(AgentHookEvent event) {
        List<AgentHook> registered = hooks.get(event.getClass());
        if (registered == null || registered.isEmpty()) {
            return;
        }
        for (AgentHook hook : registered) {
            try {
                hook.handle(event);
            } catch (Exception e) {
                log.warn("Hook threw exception for event {}: {}", event.getClass().getSimpleName(), e.getMessage(), e);
            }
        }
    }

    /**
     * Returns the number of hooks registered for a given event type.
     *
     * @param eventType the event class
     * @return the count of registered hooks
     */
    public int hookCount(Class<? extends AgentHookEvent> eventType) {
        List<AgentHook> registered = hooks.get(eventType);
        return registered == null ? 0 : registered.size();
    }

    /**
     * Register a tool-call policy that can approve/deny/modify arguments before execution.
     *
     * @param policy policy to apply for each requested tool call
     */
    public void registerToolCallPolicy(ToolCallPolicy policy) {
        if (policy != null) {
            toolCallPolicies.add(policy);
        }
    }

    /**
     * Applies all registered tool-call policies in registration order.
     *
     * <p>If any policy denies execution, evaluation stops and the call is denied.
     * If policies rewrite arguments, the latest rewritten value is used.
     *
     * @param iteration loop iteration
     * @param toolName tool name
     * @param arguments current tool arguments
     * @return final decision after applying policies
     */
    public ToolCallPolicyDecision evaluateToolCall(int iteration, String toolName, String arguments) {
        String currentArguments = arguments;
        for (ToolCallPolicy policy : toolCallPolicies) {
            try {
                ToolCallPolicyDecision decision = policy.evaluate(iteration, toolName, currentArguments);
                if (decision == null) {
                    continue;
                }
                if (!decision.allowed()) {
                    return ToolCallPolicyDecision.deny(decision.denialOutput());
                }
                if (decision.arguments() != null) {
                    currentArguments = decision.arguments();
                }
            } catch (Exception e) {
                log.warn("Tool call policy threw exception for tool {}: {}", toolName, e.getMessage(), e);
            }
        }
        return ToolCallPolicyDecision.allow(currentArguments);
    }
}
