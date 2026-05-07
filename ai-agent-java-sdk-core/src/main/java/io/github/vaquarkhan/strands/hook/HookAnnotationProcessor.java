package io.github.vaquarkhan.strands.hook;

import java.lang.reflect.Method;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Scans a bean (or plugin) for methods annotated with {@link OnHook} and
 * registers them with a {@link HookRegistry}.
 *
 * <p>Methods must accept exactly one parameter assignable from {@link AgentHookEvent}.
 * Methods with incorrect signatures are skipped with a warning.
 *
 * @author Vaquar Khan
 */
public class HookAnnotationProcessor {

    private static final Logger log = LoggerFactory.getLogger(HookAnnotationProcessor.class);

    private final HookRegistry hookRegistry;

    /**
     * Creates a processor that registers discovered hooks into the given registry.
     *
     * @param hookRegistry the registry to register hooks with
     */
    public HookAnnotationProcessor(HookRegistry hookRegistry) {
        this.hookRegistry = hookRegistry;
    }

    /**
     * Scans the given object for {@link OnHook}-annotated methods and registers
     * them as hooks in the registry.
     *
     * @param bean the object to scan
     * @return the number of hooks registered
     */
    public int process(Object bean) {
        if (bean == null) {
            return 0;
        }
        int registered = 0;
        Method[] methods = bean.getClass().getDeclaredMethods();
        for (Method method : methods) {
            OnHook annotation = method.getAnnotation(OnHook.class);
            if (annotation == null) {
                continue;
            }
            Class<? extends AgentHookEvent> eventType = annotation.value();

            // Validate method signature: must have exactly one parameter assignable from AgentHookEvent
            Class<?>[] paramTypes = method.getParameterTypes();
            if (paramTypes.length != 1 || !AgentHookEvent.class.isAssignableFrom(paramTypes[0])) {
                log.warn("Skipping @OnHook method '{}' on {}: must accept exactly one AgentHookEvent parameter",
                        method.getName(), bean.getClass().getSimpleName());
                continue;
            }

            method.setAccessible(true);
            final Method hookMethod = method;
            hookRegistry.register(eventType, event -> {
                try {
                    hookMethod.invoke(bean, event);
                } catch (Exception e) {
                    log.warn("Error invoking @OnHook method '{}': {}", hookMethod.getName(), e.getMessage(), e);
                }
            });
            registered++;
            log.debug("Registered @OnHook method '{}' for event {}", method.getName(), eventType.getSimpleName());
        }
        return registered;
    }
}
