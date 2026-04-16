package com.example.spring.ai.strands.examples.quickstart;

import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.model.StrandsAgentResponse;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * HTTP entry point for the quickstart demo.
 *
 * @author Vaquar Khan
 */
@RestController
public class QuickstartController {

    private final StrandsAgent strandsAgent;

    public QuickstartController(StrandsAgent strandsAgent) {
        this.strandsAgent = strandsAgent;
    }

    /**
     * Default message mirrors the Strands quickstart multi-part user prompt.
     */
    @GetMapping(value = "/api/quickstart", produces = MediaType.APPLICATION_JSON_VALUE)
    public QuickstartHttpResponse quickstart(
            @RequestParam(value = "message", defaultValue = DEFAULT_PROMPT) String message) {
        StrandsAgentResponse response =
                strandsAgent.execute(message, StrandsExecutionContext.standalone("quickstart"));
        return new QuickstartHttpResponse(
                response.content(),
                response.terminationReason().name(),
                response.iterationCount());
    }

    static final String DEFAULT_PROMPT =
            """
            I have 4 requests:
            1. What is the time right now?
            2. Calculate 3111696 / 74088
            3. Tell me how many letter R's are in "strawberry"
            4. Summarize the tool results briefly.
            """;

    public record QuickstartHttpResponse(String content, String terminationReason, int iterationCount) {}
}
