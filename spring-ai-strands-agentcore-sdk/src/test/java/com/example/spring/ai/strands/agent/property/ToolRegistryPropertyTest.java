package com.example.spring.ai.strands.agent.property;

/**
 * @author Vaquar Khan
 */
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.support.TestToolCallback;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import java.util.Map;
import net.jqwik.api.Arbitraries;
import net.jqwik.api.ForAll;
import net.jqwik.api.Property;
import org.junit.jupiter.api.Assertions;

class ToolRegistryPropertyTest {

    @Property(tries = 100)
    void toolErrorContained(@ForAll("messages") String errorMessage) {
        ToolRegistry registry = new ToolRegistry(
                Map.of(
                        "boom",
                        new TestToolCallback("boom", "d", args -> {
                            throw new RuntimeException(errorMessage);
                        })),
                new StrandsAgentProperties.Security());
        var result = registry.executeTool("boom", "{}");
        Assertions.assertFalse(result.success());
        Assertions.assertTrue(result.output().contains("error"));
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<String> messages() {
        return Arbitraries.strings().ascii().ofMinLength(1).ofMaxLength(30);
    }
}
