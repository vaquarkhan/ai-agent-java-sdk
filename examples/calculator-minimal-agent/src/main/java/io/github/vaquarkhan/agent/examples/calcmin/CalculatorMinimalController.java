package io.github.vaquarkhan.agent.examples.calcmin;

import io.github.vaquarkhan.agent.AiAgent;
import io.github.vaquarkhan.agent.execution.AgentExecutionContext;
import io.github.vaquarkhan.agent.model.AgentResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author Vaquar Khan
 */
@RestController
public class CalculatorMinimalController {

    private final AiAgent aiAgent;

    public CalculatorMinimalController(AiAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @GetMapping(value = "/api/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public AskResponse ask(
            @RequestParam(value = "q", defaultValue = DEFAULT_Q) String question) {
        AgentResponse r =
                aiAgent.execute(question, AgentExecutionContext.standalone("calc-min"));
        return new AskResponse(r.content(), r.iterationCount());
    }

    static final String DEFAULT_Q = "What is the square root of 1764?";

    public record AskResponse(String content, int iterations) {}
}
