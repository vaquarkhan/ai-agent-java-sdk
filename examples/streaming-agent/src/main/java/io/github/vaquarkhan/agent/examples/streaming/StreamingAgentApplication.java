package io.github.vaquarkhan.agent.examples.streaming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Streaming agent example: demonstrates {@code AiAgent.executeStreaming()} returning
 * {@code Flux<String>} exposed as Server-Sent Events.
 *
 * @author Vaquar Khan
 */
@SpringBootApplication
public class StreamingAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamingAgentApplication.class, args);
    }
}
