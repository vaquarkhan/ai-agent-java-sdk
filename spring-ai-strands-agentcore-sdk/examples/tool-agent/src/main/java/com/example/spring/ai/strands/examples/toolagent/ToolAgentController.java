package com.example.spring.ai.strands.examples.toolagent;

import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.model.StrandsAgentResponse;
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

    private final StrandsAgent strandsAgent;

    public ToolAgentController(StrandsAgent strandsAgent) {
        this.strandsAgent = strandsAgent;
    }

    @GetMapping(value = "/api/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public ToolAgentResponse ask(
            @RequestParam(value = "q", defaultValue = "What is the weather in Seattle in Fahrenheit?") String question) {
        StrandsAgentResponse response =
                strandsAgent.execute(question, StrandsExecutionContext.standalone("tool-agent"));
        return new ToolAgentResponse(
                response.content(),
                response.terminationReason().name(),
                response.iterationCount());
    }

    public record ToolAgentResponse(String content, String terminationReason, int iterationCount) {}
}
