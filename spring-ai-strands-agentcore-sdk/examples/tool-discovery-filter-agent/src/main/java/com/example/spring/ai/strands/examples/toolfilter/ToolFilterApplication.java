package com.example.spring.ai.strands.examples.toolfilter;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Shows {@code strands.agent.tool-discovery} include/exclude — critical for production agents that register many
 * {@link org.springframework.ai.tool.ToolCallbackProvider} beans but want the Strands loop to see only a subset.
 *
 * @author Vaquar Khan
 */
@SpringBootApplication
public class ToolFilterApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToolFilterApplication.class, args);
    }
}
