package com.example.spring.ai.agent.examples.streaming;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Demonstrates {@code AiAgent#executeStreaming} behind HTTP SSE - common for chat UIs and AgentCore-style runtimes.
 *
 * @author Vaquar Khan
 */
@SpringBootApplication
public class StreamingSseApplication {

    public static void main(String[] args) {
        SpringApplication.run(StreamingSseApplication.class, args);
    }
}
