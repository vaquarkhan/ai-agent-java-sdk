package com.example.spring.ai.strands.examples.calcmin;

import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.model.StrandsAgentResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vaquar Khan
 */
@RestController
public class CalculatorMinimalController {

    private final StrandsAgent strandsAgent;

    public CalculatorMinimalController(StrandsAgent strandsAgent) {
        this.strandsAgent = strandsAgent;
    }

    @GetMapping(value = "/api/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public AskResponse ask(
            @RequestParam(value = "q", defaultValue = DEFAULT_Q) String question) {
        StrandsAgentResponse r =
                strandsAgent.execute(question, StrandsExecutionContext.standalone("calc-min"));
        return new AskResponse(r.content(), r.iterationCount());
    }

    static final String DEFAULT_Q = "What is the square root of 1764?";

    public record AskResponse(String content, int iterations) {}
}
