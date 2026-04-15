package com.example.spring.ai.strands.agent.execution;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.support.AgentCoreContext;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StrandsExecutionContextTest {

    @Test
    void headersAreImmutable() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "secret");
        StrandsExecutionContext context = new StrandsExecutionContext("s1", "u1", headers);
        assertThrows(UnsupportedOperationException.class, () -> context.getHeaders().put("x", "y"));
    }

    @Test
    void toStringDoesNotExposeHeaders() {
        StrandsExecutionContext context = new StrandsExecutionContext("s1", "u1", Map.of("Authorization", "secret"));
        assertTrue(context.toString().contains("s1"));
        assertTrue(!context.toString().contains("secret"));
    }

    @Test
    void equalsAndHashCodeExcludeHeaders() {
        StrandsExecutionContext left = new StrandsExecutionContext("s1", "u1", Map.of("a", "1"));
        StrandsExecutionContext right = new StrandsExecutionContext("s1", "u1", Map.of("a", "2"));
        assertEquals(left, right);
        assertEquals(left.hashCode(), right.hashCode());
    }

    @Test
    void factoryMethodsWork() {
        AgentCoreContext agentCoreContext = new AgentCoreContext() {
            @Override
            public String sessionId() {
                return "sid";
            }

            @Override
            public String userId() {
                return "uid";
            }

            @Override
            public Map<String, String> headers() {
                return Map.of("h", "v");
            }
        };
        StrandsExecutionContext fromRuntime = StrandsExecutionContext.from(agentCoreContext);
        StrandsExecutionContext standalone = StrandsExecutionContext.standalone("standalone");
        assertEquals("sid", fromRuntime.getSessionId());
        assertEquals("standalone", standalone.getSessionId());
    }
}
