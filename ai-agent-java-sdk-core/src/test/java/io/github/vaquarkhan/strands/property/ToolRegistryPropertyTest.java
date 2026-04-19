package io.github.vaquarkhan.strands.property;

/**
 * @author Vaquar Khan
 */
import io.github.vaquarkhan.strands.config.AiAgentProperties;
import io.github.vaquarkhan.strands.support.TestToolCallback;
import io.github.vaquarkhan.strands.tool.ToolRegistry;
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
                new AiAgentProperties.Security());
        var result = registry.executeTool("boom", "{}");
        Assertions.assertFalse(result.success());
        Assertions.assertTrue(result.output().contains("error"));
    }

    @net.jqwik.api.Provide
    net.jqwik.api.Arbitrary<String> messages() {
        return Arbitraries.strings().ascii().ofMinLength(1).ofMaxLength(30);
    }
}
