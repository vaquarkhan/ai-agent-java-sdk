package com.example.spring.ai.strands.agent;

import com.example.spring.ai.strands.agent.api.Advisor;
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.config.StrandsAgentPropertiesValidator;
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionLoop;
import com.example.spring.ai.strands.agent.execution.StrandsLoopResult;
import com.example.spring.ai.strands.agent.model.StrandsAgentResponse;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;

/**
 * Model-driven agent facade: runs the execution loop with configured tools and advisors.
 *
 * @author Vaquar Khan
 */

public class StrandsAgent {

    private final StrandsExecutionLoop executionLoop;
    private final ToolRegistry toolRegistry;
    private final StrandsAgentProperties properties;
    private final List<Advisor> advisors;
    private final StrandsObservability observability;

    public StrandsAgent(
            StrandsExecutionLoop executionLoop,
            ToolRegistry toolRegistry,
            StrandsAgentProperties properties,
            List<Advisor> advisors,
            StrandsObservability observability) {
        this.executionLoop = executionLoop;
        this.toolRegistry = toolRegistry;
        this.properties = properties;
        this.advisors = advisors == null ? List.of() : advisors;
        this.observability = observability;
    }

    public StrandsAgentResponse execute(String userPrompt, StrandsExecutionContext context) {
        StrandsAgentPropertiesValidator.validateOrThrow(properties);
        List<ExecutionMessage> messages = applyAdvisors(baseMessages(userPrompt), context);
        StrandsLoopResult result =
                executionLoop.run(resolveSystemPrompt(), messages, toolRegistry, observability, context);
        return new StrandsAgentResponse(
                result.content(),
                result.reasoningTrace(),
                result.terminationReason(),
                result.iterationCount(),
                result.totalDuration());
    }

    public Flux<String> executeStreaming(String userPrompt, StrandsExecutionContext context) {
        StrandsAgentPropertiesValidator.validateOrThrow(properties);
        List<ExecutionMessage> messages = applyAdvisors(baseMessages(userPrompt), context);
        return executionLoop.runStreaming(resolveSystemPrompt(), messages, toolRegistry, observability, context);
    }

    private List<ExecutionMessage> baseMessages(String userPrompt) {
        return new ArrayList<>(List.of(new ExecutionMessage("user", userPrompt)));
    }

    private List<ExecutionMessage> applyAdvisors(List<ExecutionMessage> messages, StrandsExecutionContext context) {
        List<ExecutionMessage> enriched = messages;
        for (Advisor advisor : advisors) {
            enriched = advisor.apply(enriched, context);
        }
        return enriched;
    }

    private String resolveSystemPrompt() {
        if (properties.getSystemPrompt() != null && !properties.getSystemPrompt().isBlank()) {
            return properties.getSystemPrompt();
        }
        Resource resource = properties.getSystemPromptResource();
        if (resource == null || !resource.exists()) {
            return "";
        }
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read system prompt resource", e);
        }
    }
}
