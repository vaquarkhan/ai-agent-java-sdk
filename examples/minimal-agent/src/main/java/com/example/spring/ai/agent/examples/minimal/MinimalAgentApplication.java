package com.example.spring.ai.agent.examples.minimal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Simplest possible AI Agent Java SDK sample: no tools, just model-driven Q and A.
 * All configuration is in application.yml; the scripted model client returns a canned answer.
 *
 * @author Vaquar Khan
 */
@SpringBootApplication
public class MinimalAgentApplication {

    public static void main(String[] args) {
        SpringApplication.run(MinimalAgentApplication.class, args);
    }
}
