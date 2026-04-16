package com.example.spring.ai.strands.examples.minimal;

import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.model.StrandsAgentResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single endpoint that calls {@link StrandsAgent#execute} and returns the response.
 *
 * @author Vaquar Khan
 */
@RestController
public class MinimalAgentController {

    private final StrandsAgent strandsAgent;

    public MinimalAgentController(StrandsAgent strandsAgent) {
        this.strandsAgent = strandsAgent;
    }

    @GetMapping(value = "/api/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public AskResponse ask(
            @RequestParam(value = "q", defaultValue = "What is Spring AI?") String question) {
        StrandsAgentResponse response =
                strandsAgent.execute(question, StrandsExecutionContext.standalone("minimal"));
        return new AskResponse(response.content(), response.iterationCount());
    }

    public record AskResponse(String content, int iterations) {}
}
