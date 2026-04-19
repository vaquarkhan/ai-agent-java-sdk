package io.github.vaquarkhan.strands.examples.calcmin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal agent with a single calculator tool - Java analogue of the Strands Python snippet:
 *
 * <pre>
 * from strands import Agent
 * from strands_tools import calculator
 * agent = Agent(tools=[calculator])
 * agent("What is the square root of 1764?")
 * </pre>
 *
 * @author Vaquar Khan
 */
@SpringBootApplication
public class CalculatorMinimalApplication {

    public static void main(String[] args) {
        SpringApplication.run(CalculatorMinimalApplication.class, args);
    }
}
