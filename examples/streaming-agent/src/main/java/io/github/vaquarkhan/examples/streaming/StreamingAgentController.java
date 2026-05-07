package io.github.vaquarkhan.examples.streaming;

import io.github.vaquarkhan.strands.AiAgent;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * SSE endpoint that streams tokens from {@link AiAgent#executeStreaming}.
 *
 * @author Vaquar Khan
 */
@RestController
public class StreamingAgentController {

    private final AiAgent aiAgent;

    public StreamingAgentController(AiAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @GetMapping(value = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestParam(value = "q", defaultValue = "Tell me about Spring AI") String question) {
        return aiAgent.executeStreaming(question, AgentExecutionContext.standalone("streaming"));
    }
}
