package io.github.vaquarkhan.strands.multiagent;

import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Minimal DAG-based multi-agent orchestration.
 *
 * <p>This is intentionally small: it runs nodes in a topological order and passes prior outputs
 * via {@link AgentGraphRunState}. It is a foundation for higher-level "swarm" routing and
 * parallelism strategies.
 *
 * @author Vaquar Khan
 */
public final class AgentGraph {

    private final Map<String, AgentGraphNode> nodes = new HashMap<>();
    private final Map<String, Set<String>> edges = new HashMap<>(); // from -> to

    public AgentGraph addNode(AgentGraphNode node) {
        Objects.requireNonNull(node, "node");
        if (nodes.putIfAbsent(node.name(), node) != null) {
            throw new IllegalArgumentException("Duplicate node name: " + node.name());
        }
        edges.computeIfAbsent(node.name(), k -> new HashSet<>());
        return this;
    }

    public AgentGraph addEdge(String fromNode, String toNode) {
        Objects.requireNonNull(fromNode, "fromNode");
        Objects.requireNonNull(toNode, "toNode");
        if (!nodes.containsKey(fromNode) || !nodes.containsKey(toNode)) {
            throw new IllegalArgumentException("Both nodes must exist before adding an edge.");
        }
        edges.computeIfAbsent(fromNode, k -> new HashSet<>()).add(toNode);
        return this;
    }

    public AgentGraphRunState run(String initialInput, AgentExecutionContext context) {
        Objects.requireNonNull(initialInput, "initialInput");
        Objects.requireNonNull(context, "context");

        List<String> order = topologicalOrder();
        AgentGraphRunState state = new AgentGraphRunState();

        for (String nodeName : order) {
            AgentGraphNode node = nodes.get(nodeName);
            String prompt = node.buildPrompt(initialInput, state);
            String output = node.agent().execute(prompt, context).content();
            state.putOutput(nodeName, output);
        }

        return state;
    }

    private List<String> topologicalOrder() {
        // Kahn's algorithm
        Map<String, Integer> indegree = new HashMap<>();
        for (String n : nodes.keySet()) {
            indegree.put(n, 0);
        }
        for (Map.Entry<String, Set<String>> e : edges.entrySet()) {
            for (String to : e.getValue()) {
                indegree.put(to, indegree.getOrDefault(to, 0) + 1);
            }
        }

        ArrayDeque<String> q = new ArrayDeque<>();
        for (Map.Entry<String, Integer> e : indegree.entrySet()) {
            if (e.getValue() == 0) {
                q.add(e.getKey());
            }
        }

        ArrayList<String> order = new ArrayList<>(nodes.size());
        while (!q.isEmpty()) {
            String n = q.removeFirst();
            order.add(n);
            for (String to : edges.getOrDefault(n, Set.of())) {
                int d = indegree.computeIfPresent(to, (k, v) -> v - 1);
                if (d == 0) {
                    q.add(to);
                }
            }
        }

        if (order.size() != nodes.size()) {
            throw new IllegalStateException("AgentGraph contains a cycle or disconnected edge references.");
        }
        return order;
    }
}

