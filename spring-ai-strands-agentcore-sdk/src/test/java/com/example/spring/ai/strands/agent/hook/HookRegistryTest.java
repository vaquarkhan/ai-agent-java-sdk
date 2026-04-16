package com.example.spring.ai.strands.agent.hook;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.ModelTurnResponse;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.model.StrandsAgentResponse;
import com.example.spring.ai.strands.agent.model.TerminationReason;
import com.example.spring.ai.strands.agent.model.ToolExecutionResult;
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
        registry.register(StrandsHookEvent.BeforeInvocation.class, event -> {
            StrandsHookEvent.BeforeInvocation bi = (StrandsHookEvent.BeforeInvocation) event;
            captured.set(bi.userPrompt());
        });
        registry.dispatch(new StrandsHookEvent.BeforeInvocation("hello", StrandsExecutionContext.standalone("s")));
        assertEquals("hello", captured.get());
    }

    @Test
    void multipleHooksForSameEventAllCalled() {
        HookRegistry registry = new HookRegistry();
        List<String> calls = new ArrayList<>();
        registry.register(StrandsHookEvent.BeforeInvocation.class, event -> calls.add("hook1"));
        registry.register(StrandsHookEvent.BeforeInvocation.class, event -> calls.add("hook2"));
        registry.register(StrandsHookEvent.BeforeInvocation.class, event -> calls.add("hook3"));
        registry.dispatch(new StrandsHookEvent.BeforeInvocation("hi", StrandsExecutionContext.standalone("s")));
        assertEquals(List.of("hook1", "hook2", "hook3"), calls);
    }

    @Test
    void dispatchWithNoRegisteredHooksDoesNotThrow() {
        HookRegistry registry = new HookRegistry();
        assertDoesNotThrow(() ->
                registry.dispatch(new StrandsHookEvent.BeforeInvocation("hi", StrandsExecutionContext.standalone("s"))));
    }

    @Test
    void hookExceptionDoesNotBreakOtherHooks() {
        HookRegistry registry = new HookRegistry();
        AtomicBoolean secondCalled = new AtomicBoolean(false);
        registry.register(StrandsHookEvent.BeforeInvocation.class, event -> {
            throw new RuntimeException("hook failure");
        });
        registry.register(StrandsHookEvent.BeforeInvocation.class, event -> secondCalled.set(true));
        registry.dispatch(new StrandsHookEvent.BeforeInvocation("hi", StrandsExecutionContext.standalone("s")));
        assertTrue(secondCalled.get());
    }

    @Test
    void hookCountReturnsCorrectCount() {
        HookRegistry registry = new HookRegistry();
        assertEquals(0, registry.hookCount(StrandsHookEvent.BeforeInvocation.class));
        registry.register(StrandsHookEvent.BeforeInvocation.class, event -> {});
        assertEquals(1, registry.hookCount(StrandsHookEvent.BeforeInvocation.class));
        registry.register(StrandsHookEvent.BeforeInvocation.class, event -> {});
        assertEquals(2, registry.hookCount(StrandsHookEvent.BeforeInvocation.class));
        assertEquals(0, registry.hookCount(StrandsHookEvent.AfterInvocation.class));
    }

    @Test
    void allSixEventTypesCanBeDispatched() {
        HookRegistry registry = new HookRegistry();
        List<String> dispatched = new ArrayList<>();

        registry.register(StrandsHookEvent.BeforeInvocation.class, e -> dispatched.add("BeforeInvocation"));
        registry.register(StrandsHookEvent.BeforeModelCall.class, e -> dispatched.add("BeforeModelCall"));
        registry.register(StrandsHookEvent.AfterModelCall.class, e -> dispatched.add("AfterModelCall"));
        registry.register(StrandsHookEvent.BeforeToolCall.class, e -> dispatched.add("BeforeToolCall"));
        registry.register(StrandsHookEvent.AfterToolCall.class, e -> dispatched.add("AfterToolCall"));
        registry.register(StrandsHookEvent.AfterInvocation.class, e -> dispatched.add("AfterInvocation"));

        registry.dispatch(new StrandsHookEvent.BeforeInvocation("hi", StrandsExecutionContext.standalone("s")));
        registry.dispatch(new StrandsHookEvent.BeforeModelCall(1, List.of()));
        registry.dispatch(new StrandsHookEvent.AfterModelCall(1, ModelTurnResponse.finalAnswer("ok")));
        registry.dispatch(new StrandsHookEvent.BeforeToolCall(1, "calc", "{}"));
        registry.dispatch(new StrandsHookEvent.AfterToolCall(1, "calc",
                new ToolExecutionResult("calc", true, "4", Duration.ZERO)));
        registry.dispatch(new StrandsHookEvent.AfterInvocation(
                new StrandsAgentResponse("ok", null, TerminationReason.MODEL_COMPLETION, 1, Duration.ZERO)));

        assertEquals(List.of("BeforeInvocation", "BeforeModelCall", "AfterModelCall",
                "BeforeToolCall", "AfterToolCall", "AfterInvocation"), dispatched);
    }
}
