package com.example.spring.ai.strands.agent.execution;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.hook.HookRegistry;
import com.example.spring.ai.strands.agent.hook.StrandsHookEvent;
import com.example.spring.ai.strands.agent.model.ToolExecutionResult;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.support.MockModelClient;
import com.example.spring.ai.strands.agent.support.TestToolCallback;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.micrometer.observation.ObservationRegistry;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ExecutionLoopHooksTest {

    @Test
    void beforeModelCallDispatchedWithCorrectIteration() {
        HookRegistry hookRegistry = new HookRegistry();
        List<Integer> iterations = new ArrayList<>();
        hookRegistry.register(StrandsHookEvent.BeforeModelCall.class, event -> {
            StrandsHookEvent.BeforeModelCall bmc = (StrandsHookEvent.BeforeModelCall) event;
            iterations.add(bmc.iteration());
        });

        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{}"))
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        ToolRegistry registry = toolRegistryWithCalc();
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        loop.setHookRegistry(hookRegistry);

        loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                registry, observability(), StrandsExecutionContext.standalone("s"));

        assertEquals(List.of(1, 2), iterations);
    }

    @Test
    void afterModelCallDispatchedWithResponse() {
        HookRegistry hookRegistry = new HookRegistry();
        List<ModelTurnResponse> responses = new ArrayList<>();
        hookRegistry.register(StrandsHookEvent.AfterModelCall.class, event -> {
            StrandsHookEvent.AfterModelCall amc = (StrandsHookEvent.AfterModelCall) event;
            responses.add(amc.response());
        });

        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.finalAnswer("the answer"));
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        loop.setHookRegistry(hookRegistry);

        loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                emptyRegistry(), observability(), StrandsExecutionContext.standalone("s"));

        assertEquals(1, responses.size());
        assertEquals("the answer", responses.get(0).content());
    }

    @Test
    void beforeToolCallDispatchedWithToolNameAndArguments() {
        HookRegistry hookRegistry = new HookRegistry();
        AtomicReference<String> capturedToolName = new AtomicReference<>();
        AtomicReference<String> capturedArgs = new AtomicReference<>();
        hookRegistry.register(StrandsHookEvent.BeforeToolCall.class, event -> {
            StrandsHookEvent.BeforeToolCall btc = (StrandsHookEvent.BeforeToolCall) event;
            capturedToolName.set(btc.toolName());
            capturedArgs.set(btc.arguments());
        });

        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{\"x\":2}"))
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        ToolRegistry registry = toolRegistryWithCalc();
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        loop.setHookRegistry(hookRegistry);

        loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                registry, observability(), StrandsExecutionContext.standalone("s"));

        assertEquals("calc", capturedToolName.get());
        assertEquals("{\"x\":2}", capturedArgs.get());
    }

    @Test
    void afterToolCallDispatchedWithResult() {
        HookRegistry hookRegistry = new HookRegistry();
        AtomicReference<ToolExecutionResult> capturedResult = new AtomicReference<>();
        hookRegistry.register(StrandsHookEvent.AfterToolCall.class, event -> {
            StrandsHookEvent.AfterToolCall atc = (StrandsHookEvent.AfterToolCall) event;
            capturedResult.set(atc.result());
        });

        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.toolCall("calc", "{}"))
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        ToolRegistry registry = toolRegistryWithCalc();
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        loop.setHookRegistry(hookRegistry);

        loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                registry, observability(), StrandsExecutionContext.standalone("s"));

        assertNotNull(capturedResult.get());
        assertEquals("calc", capturedResult.get().toolName());
        assertTrue(capturedResult.get().success());
        assertEquals("4", capturedResult.get().output());
    }

    @Test
    void hooksDispatchedForParallelToolCalls() {
        HookRegistry hookRegistry = new HookRegistry();
        List<String> beforeToolNames = new ArrayList<>();
        List<String> afterToolNames = new ArrayList<>();
        hookRegistry.register(StrandsHookEvent.BeforeToolCall.class, event -> {
            StrandsHookEvent.BeforeToolCall btc = (StrandsHookEvent.BeforeToolCall) event;
            beforeToolNames.add(btc.toolName());
        });
        hookRegistry.register(StrandsHookEvent.AfterToolCall.class, event -> {
            StrandsHookEvent.AfterToolCall atc = (StrandsHookEvent.AfterToolCall) event;
            afterToolNames.add(atc.toolName());
        });

        MockModelClient modelClient = new MockModelClient()
                .addResponse(ModelTurnResponse.multiToolCall(List.of(
                        new ToolCallRequest("add", "{\"a\":1}"),
                        new ToolCallRequest("mul", "{\"b\":2}"))))
                .addResponse(ModelTurnResponse.finalAnswer("done"));
        ToolRegistry registry = new ToolRegistry(Map.of(
                "add", new TestToolCallback("add", "addition", a -> "sum"),
                "mul", new TestToolCallback("mul", "multiply", a -> "product")),
                new StrandsAgentProperties.Security());
        StrandsExecutionLoop loop = new StrandsExecutionLoop(modelClient, 5, "agent");
        loop.setHookRegistry(hookRegistry);

        loop.run("sys", List.of(new ExecutionMessage("user", "hi")),
                registry, observability(), StrandsExecutionContext.standalone("s"));

        // One BeforeToolCall per tool in the parallel batch
        assertEquals(List.of("add", "mul"), beforeToolNames);
        // One AfterToolCall per tool in the parallel batch
        assertEquals(List.of("add", "mul"), afterToolNames);
    }

    private ToolRegistry toolRegistryWithCalc() {
        return new ToolRegistry(
                Map.of("calc", new TestToolCallback("calc", "calculator", a -> "4")),
                new StrandsAgentProperties.Security());
    }

    private ToolRegistry emptyRegistry() {
        return new ToolRegistry(Map.of(), new StrandsAgentProperties.Security());
    }

    private StrandsObservability observability() {
        return new StrandsObservability(new SimpleMeterRegistry(), ObservationRegistry.NOOP,
                new StrandsAgentProperties.Security());
    }
}
