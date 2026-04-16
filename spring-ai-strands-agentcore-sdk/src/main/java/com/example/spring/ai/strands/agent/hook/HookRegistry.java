package com.example.spring.ai.strands.agent.hook;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Registry that holds {@link StrandsHook} instances keyed by event type and dispatches
 * events to all matching hooks.
 *
 * <p>Hook exceptions are logged as warnings and do not interrupt the execution loop.
 *
 * @author Vaquar Khan
 */
public class HookRegistry {

    private static final Logger log = LoggerFactory.getLogger(HookRegistry.class);

    private final ConcurrentHashMap<Class<? extends StrandsHookEvent>, CopyOnWriteArrayList<StrandsHook>> hooks =
            new ConcurrentHashMap<>();

    /**
     * Register a hook for a specific event type.
     *
     * @param eventType the event class to listen for
     * @param hook      the hook to invoke when the event is dispatched
     */
    public void register(Class<? extends StrandsHookEvent> eventType, StrandsHook hook) {
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
    public void dispatch(StrandsHookEvent event) {
        List<StrandsHook> registered = hooks.get(event.getClass());
        if (registered == null || registered.isEmpty()) {
            return;
        }
        for (StrandsHook hook : registered) {
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
    public int hookCount(Class<? extends StrandsHookEvent> eventType) {
        List<StrandsHook> registered = hooks.get(eventType);
        return registered == null ? 0 : registered.size();
    }
}
