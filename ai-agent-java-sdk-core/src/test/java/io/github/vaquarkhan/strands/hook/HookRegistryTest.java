package io.github.vaquarkhan.strands.hook;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import io.github.vaquarkhan.strands.execution.ModelTurnResponse;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import io.github.vaquarkhan.strands.model.AgentResponse;
import io.github.vaquarkhan.strands.model.TerminationReason;
import io.github.vaquarkhan.strands.model.ToolExecutionResult;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

class HookRegistryTest {

    @Test
    void registerAndDispatchBeforeInvocation() {
        HookRegistry registry = new HookRegistry();
        AtomicReference<String> captured = new AtomicReference<>();
        registry.register(AgentHookEvent.BeforeInvocation.class, event -> {
            AgentHookEvent.BeforeInvocation bi = (AgentHookEvent.BeforeInvocation) event;
            captured.set(bi.userPrompt());
        });
        registry.dispatch(new AgentHookEvent.BeforeInvocation("hello", AgentExecutionContext.standalone("s")));
        assertEquals("hello", captured.get());
    }

    @Test
    void multipleHooksForSameEventAllCalled() {
        HookRegistry registry = new HookRegistry();
        List<String> calls = new ArrayList<>();
        registry.register(AgentHookEvent.BeforeInvocation.class, event -> calls.add("hook1"));
        registry.register(AgentHookEvent.BeforeInvocation.class, event -> calls.add("hook2"));
        registry.register(AgentHookEvent.BeforeInvocation.class, event -> calls.add("hook3"));
        registry.dispatch(new AgentHookEvent.BeforeInvocation("hi", AgentExecutionContext.standalone("s")));
        assertEquals(List.of("hook1", "hook2", "hook3"), calls);
    }

    @Test
    void dispatchWithNoRegisteredHooksDoesNotThrow() {
        HookRegistry registry = new HookRegistry();
        assertDoesNotThrow(() ->
                registry.dispatch(new AgentHookEvent.BeforeInvocation("hi", AgentExecutionContext.standalone("s"))));
    }

    @Test
    void hookExceptionDoesNotBreakOtherHooks() {
        HookRegistry registry = new HookRegistry();
        AtomicBoolean secondCalled = new AtomicBoolean(false);
        registry.register(AgentHookEvent.BeforeInvocation.class, event -> {
            throw new RuntimeException("hook failure");
        });
        registry.register(AgentHookEvent.BeforeInvocation.class, event -> secondCalled.set(true));
        registry.dispatch(new AgentHookEvent.BeforeInvocation("hi", AgentExecutionContext.standalone("s")));
        assertTrue(secondCalled.get());
    }

    @Test
    void hookCountReturnsCorrectCount() {
        HookRegistry registry = new HookRegistry();
        assertEquals(0, registry.hookCount(AgentHookEvent.BeforeInvocation.class));
        registry.register(AgentHookEvent.BeforeInvocation.class, event -> {});
        assertEquals(1, registry.hookCount(AgentHookEvent.BeforeInvocation.class));
        registry.register(AgentHookEvent.BeforeInvocation.class, event -> {});
        assertEquals(2, registry.hookCount(AgentHookEvent.BeforeInvocation.class));
        assertEquals(0, registry.hookCount(AgentHookEvent.AfterInvocation.class));
    }

    @Test
    void allSixEventTypesCanBeDispatched() {
        HookRegistry registry = new HookRegistry();
        List<String> dispatched = new ArrayList<>();

        registry.register(AgentHookEvent.BeforeInvocation.class, e -> dispatched.add("BeforeInvocation"));
        registry.register(AgentHookEvent.BeforeModelCall.class, e -> dispatched.add("BeforeModelCall"));
        registry.register(AgentHookEvent.AfterModelCall.class, e -> dispatched.add("AfterModelCall"));
        registry.register(AgentHookEvent.BeforeToolCall.class, e -> dispatched.add("BeforeToolCall"));
        registry.register(AgentHookEvent.AfterToolCall.class, e -> dispatched.add("AfterToolCall"));
        registry.register(AgentHookEvent.AfterInvocation.class, e -> dispatched.add("AfterInvocation"));

        registry.dispatch(new AgentHookEvent.BeforeInvocation("hi", AgentExecutionContext.standalone("s")));
        registry.dispatch(new AgentHookEvent.BeforeModelCall(1, List.of()));
        registry.dispatch(new AgentHookEvent.AfterModelCall(1, ModelTurnResponse.finalAnswer("ok")));
        registry.dispatch(new AgentHookEvent.BeforeToolCall(1, "calc", "{}"));
        registry.dispatch(new AgentHookEvent.AfterToolCall(1, "calc",
                new ToolExecutionResult("calc", true, "4", Duration.ZERO)));
        registry.dispatch(new AgentHookEvent.AfterInvocation(
                new AgentResponse("ok", null, TerminationReason.MODEL_COMPLETION, 1, Duration.ZERO)));

        assertEquals(List.of("BeforeInvocation", "BeforeModelCall", "AfterModelCall",
                "BeforeToolCall", "AfterToolCall", "AfterInvocation"), dispatched);
    }
}
