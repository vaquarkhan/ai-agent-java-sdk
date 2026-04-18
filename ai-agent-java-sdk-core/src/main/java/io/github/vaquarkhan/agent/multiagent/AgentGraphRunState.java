package io.github.vaquarkhan.agent.multiagent;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Mutable run state for an agent graph execution.
 *
 * @author Vaquar Khan
 */
public final class AgentGraphRunState {

    private final Map<String, String> nodeOutputs = new LinkedHashMap<>();

    public void putOutput(String nodeName, String output) {
        nodeOutputs.put(Objects.requireNonNull(nodeName, "nodeName"), output);
    }

    public String getOutput(String nodeName) {
        return nodeOutputs.get(nodeName);
    }

    public Map<String, String> outputs() {
        return Collections.unmodifiableMap(nodeOutputs);
    }
}

