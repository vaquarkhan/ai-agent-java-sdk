package io.github.vaquarkhan.agent.examples.toolagent;

import io.github.vaquarkhan.agent.AiAgent;
import io.github.vaquarkhan.agent.execution.AgentExecutionContext;
import io.github.vaquarkhan.agent.model.AgentResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for the tool-agent demo. Demonstrates tool discovery and execution.
 *
 * @author Vaquar Khan
 */
@RestController
public class ToolAgentController {

    private final AiAgent aiAgent;

    public ToolAgentController(AiAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @GetMapping(value = "/api/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public ToolAgentResponse ask(
            @RequestParam(value = "q", defaultValue = "What is the weather in Seattle in Fahrenheit?") String question) {
        AgentResponse response =
                aiAgent.execute(question, AgentExecutionContext.standalone("tool-agent"));
        return new ToolAgentResponse(
                response.content(),
                response.terminationReason().name(),
                response.iterationCount());
    }

    public record ToolAgentResponse(String content, String terminationReason, int iterationCount) {}
}
