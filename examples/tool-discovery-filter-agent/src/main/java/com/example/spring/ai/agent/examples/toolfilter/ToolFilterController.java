package com.example.spring.ai.agent.examples.toolfilter;

import com.example.spring.ai.agent.AiAgent;
import com.example.spring.ai.agent.execution.AgentExecutionContext;
import com.example.spring.ai.agent.model.AgentResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vaquar Khan
 */
@RestController
public class ToolFilterController {

    private final AiAgent aiAgent;

    public ToolFilterController(AiAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @GetMapping(value = "/api/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public AgentResponse run() {
        return aiAgent.execute(
                "Run the filtered tool demo.", AgentExecutionContext.standalone("tool-filter"));
    }
}
