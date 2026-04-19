package io.github.vaquarkhan.examples.toolfilter;

import io.github.vaquarkhan.strands.AiAgent;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import io.github.vaquarkhan.strands.model.AgentResponse;
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
