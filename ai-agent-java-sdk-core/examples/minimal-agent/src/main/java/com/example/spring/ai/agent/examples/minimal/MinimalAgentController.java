package com.example.spring.ai.agent.examples.minimal;

import com.example.spring.ai.agent.AiAgent;
import com.example.spring.ai.agent.execution.AgentExecutionContext;
import com.example.spring.ai.agent.model.AgentResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Single endpoint that calls {@link AiAgent#execute} and returns the response.
 *
 * @author Vaquar Khan
 */
@RestController
public class MinimalAgentController {

    private final AiAgent aiAgent;

    public MinimalAgentController(AiAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @GetMapping(value = "/api/ask", produces = MediaType.APPLICATION_JSON_VALUE)
    public AskResponse ask(
            @RequestParam(value = "q", defaultValue = "What is Spring AI?") String question) {
        AgentResponse response =
                aiAgent.execute(question, AgentExecutionContext.standalone("minimal"));
        return new AskResponse(response.content(), response.iterationCount());
    }

    public record AskResponse(String content, int iterations) {}
}
