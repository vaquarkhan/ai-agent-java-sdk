package com.example.spring.ai.strands.examples.toolagent;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Tool-rich agent example: demonstrates custom tools (weather lookup, unit converter)
 * with tool discovery and execution in the reasoning loop.
 *
 * @author Vaquar Khan
 */
@SpringBootApplication
public class ToolAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(ToolAgentApplication.class, args);
    }
}
