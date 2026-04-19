package io.github.vaquarkhan.strands.examples.streaming;

import io.github.vaquarkhan.strands.AiAgent;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * @author Vaquar Khan
 */
@RestController
public class StreamingSseController {

    private final AiAgent aiAgent;

    public StreamingSseController(AiAgent aiAgent) {
        this.aiAgent = aiAgent;
    }

    @GetMapping(value = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(@RequestParam(value = "q", defaultValue = "Hello stream") String question) {
        return aiAgent.executeStreaming(question, AgentExecutionContext.standalone("sse-demo"));
    }
}
