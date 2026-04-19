package io.github.vaquarkhan.strands.examples.quickstart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Quickstart agent with community-style tools ({@code calculator}, {@code current_time})
 * and a custom {@code letter_counter}.
 *
 * @author Vaquar Khan
 */
@SpringBootApplication
public class QuickstartApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuickstartApplication.class, args);
    }
}
