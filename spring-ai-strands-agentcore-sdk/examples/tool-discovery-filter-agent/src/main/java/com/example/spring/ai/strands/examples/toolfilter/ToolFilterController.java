package com.example.spring.ai.strands.examples.toolfilter;

import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.model.StrandsAgentResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vaquar Khan
 */
@RestController
public class ToolFilterController {

    private final StrandsAgent strandsAgent;

    public ToolFilterController(StrandsAgent strandsAgent) {
        this.strandsAgent = strandsAgent;
    }

    @GetMapping(value = "/api/run", produces = MediaType.APPLICATION_JSON_VALUE)
    public StrandsAgentResponse run() {
        return strandsAgent.execute(
                "Run the filtered tool demo.", StrandsExecutionContext.standalone("tool-filter"));
    }
}
