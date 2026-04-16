package com.example.spring.ai.strands.agent.multiagent;

import com.example.spring.ai.strands.agent.StrandsAgent;
import java.util.Objects;
import java.util.function.BiFunction;

/**
 * A named node in an agent graph.
 *
 * <p>The node's prompt is produced by a function that can reference:
 * <ul>
 *   <li>the initial user input</li>
 *   <li>the accumulated outputs of prior nodes</li>
 * </ul>
 *
 * @author Vaquar Khan
 */
public final class AgentGraphNode {

    private final String name;
    private final StrandsAgent agent;
    private final BiFunction<String, AgentGraphRunState, String> promptFn;

    public AgentGraphNode(String name, StrandsAgent agent, BiFunction<String, AgentGraphRunState, String> promptFn) {
        this.name = Objects.requireNonNull(name, "name");
        this.agent = Objects.requireNonNull(agent, "agent");
        this.promptFn = Objects.requireNonNull(promptFn, "promptFn");
    }

    public String name() {
        return name;
    }

    public StrandsAgent agent() {
        return agent;
    }

    public String buildPrompt(String initialInput, AgentGraphRunState state) {
        return promptFn.apply(initialInput, state);
    }
}

