package io.github.vaquarkhan.strands.model;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.strands.execution.AgentExecutionException;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DataModelsTest {

    @Test
    void recordsExposeStateAndEquality() {
        IterationRecord record =
                new IterationRecord(1, "calc", "{}", "4", IterationType.TOOL_CALL, Duration.ofMillis(10));
        ReasoningTrace trace =
                new ReasoningTrace("trace", "agent", "session", Instant.now(), List.of(record));
        ToolExecutionResult toolExecutionResult =
                new ToolExecutionResult("calc", true, "4", Duration.ofMillis(10));
        AgentResponse response =
                new AgentResponse("done", trace, TerminationReason.MODEL_COMPLETION, 1, Duration.ofMillis(12));

        assertEquals("calc", record.toolName());
        assertEquals(trace, response.reasoningTrace());
        assertTrue(toolExecutionResult.toString().contains("calc"));
    }

    @Test
    void executionExceptionCarriesIterationAndTrace() {
        ReasoningTrace trace = new ReasoningTrace("trace", "agent", "session", Instant.now(), List.of());
        AgentExecutionException exception =
                new AgentExecutionException("boom", new RuntimeException("x"), 3, trace);
        assertEquals(3, exception.getIterationNumber());
        assertEquals(trace, exception.getPartialTrace());
    }
}
