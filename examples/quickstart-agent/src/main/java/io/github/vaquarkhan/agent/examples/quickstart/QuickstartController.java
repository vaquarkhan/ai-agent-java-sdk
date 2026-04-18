package io.github.vaquarkhan.agent.examples.quickstart;

import io.github.vaquarkhan.agent.AiAgent;
import io.github.vaquarkhan.agent.execution.AgentExecutionContext;
import io.github.vaquarkhan.agent.model.AgentResponse;
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

    private final AiAgent aiAgent;

    public QuickstartController(AiAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    /**
     * Default message mirrors the AWS Strands Python quickstart multi-part user prompt.
     */
    @GetMapping(value = "/api/quickstart", produces = MediaType.APPLICATION_JSON_VALUE)
    public QuickstartHttpResponse quickstart(
            @RequestParam(value = "message", defaultValue = DEFAULT_PROMPT) String message) {
        AgentResponse response =
                aiAgent.execute(message, AgentExecutionContext.standalone("quickstart"));
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
