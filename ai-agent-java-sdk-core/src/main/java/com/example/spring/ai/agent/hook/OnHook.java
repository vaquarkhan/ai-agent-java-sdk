package com.example.spring.ai.agent.hook;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a method as a hook handler for a specific {@link AgentHookEvent} type.
 *
 * <p>Annotated methods must accept a single parameter of type {@link AgentHookEvent}
 * (or a specific subtype). They are discovered by {@link HookAnnotationProcessor}
 * during plugin initialization or auto-configuration.
 *
 * <p>Example usage:
 * <pre>{@code
 * @OnHook(AgentHookEvent.BeforeModelCall.class)
 * public void onBeforeModel(AgentHookEvent event) {
 *     // handle event
 * }
 * }</pre>
 *
 * @author Vaquar Khan
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OnHook {

    /**
     * The hook event type this method handles.
     *
     * @return the event class
     */
    Class<? extends AgentHookEvent> value();
}
