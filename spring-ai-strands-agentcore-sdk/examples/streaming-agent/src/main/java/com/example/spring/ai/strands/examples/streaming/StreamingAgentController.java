package com.example.spring.ai.strands.examples.streaming;

import com.example.spring.ai.strands.agent.StrandsAgent;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

/**
 * SSE endpoint that streams tokens from {@link StrandsAgent#executeStreaming}.
 *
 * @author Vaquar Khan
 */
@RestController
public class StreamingAgentController {

    private final StrandsAgent strandsAgent;

    public StreamingAgentController(StrandsAgent strandsAgent) {
        this.strandsAgent = strandsAgent;
    }

    @GetMapping(value = "/api/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> stream(
            @RequestParam(value = "q", defaultValue = "Tell me about Spring AI") String question) {
        return strandsAgent.executeStreaming(question, StrandsExecutionContext.standalone("streaming"));
    }
}
