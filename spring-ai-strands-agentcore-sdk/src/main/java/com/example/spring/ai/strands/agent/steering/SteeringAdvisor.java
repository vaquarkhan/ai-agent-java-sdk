package com.example.spring.ai.strands.agent.steering;

import com.example.spring.ai.strands.agent.api.Advisor;
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * An {@link Advisor} that evaluates {@link SteeringRule}s against the user prompt
 * and prepends matching rule instructions as system messages.
 *
 * <p>Condition matching is case-insensitive keyword containment: if the user
 * prompt contains the rule's condition string, the rule's instruction is applied.
 *
 * @author Vaquar Khan
 */
public class SteeringAdvisor implements Advisor {

    private static final Logger log = LoggerFactory.getLogger(SteeringAdvisor.class);

    private final List<SteeringRule> rules;

    /**
     * Creates a steering advisor with the given rules.
     *
     * @param rules the steering rules to evaluate
     */
    public SteeringAdvisor(List<SteeringRule> rules) {
        this.rules = rules != null ? List.copyOf(rules) : List.of();
    }

    @Override
    public List<ExecutionMessage> apply(List<ExecutionMessage> messages, StrandsExecutionContext context) {
        if (rules.isEmpty() || messages == null || messages.isEmpty()) {
            return messages;
        }

        // Find the last user message to evaluate conditions against
        String userPrompt = findLastUserPrompt(messages);
        if (userPrompt == null) {
            return messages;
        }

        List<ExecutionMessage> steeringMessages = new ArrayList<>();
        String lowerPrompt = userPrompt.toLowerCase(Locale.ROOT);

        for (SteeringRule rule : rules) {
            if (rule.condition() == null || rule.instruction() == null) {
                continue;
            }
            String lowerCondition = rule.condition().toLowerCase(Locale.ROOT);
            if (lowerPrompt.contains(lowerCondition)) {
                steeringMessages.add(new ExecutionMessage("system", rule.instruction()));
                log.debug("Steering rule '{}' matched, prepending instruction", rule.name());
            }
        }

        if (steeringMessages.isEmpty()) {
            return messages;
        }

        List<ExecutionMessage> result = new ArrayList<>(steeringMessages);
        result.addAll(messages);
        return result;
    }

    private String findLastUserPrompt(List<ExecutionMessage> messages) {
        for (int i = messages.size() - 1; i >= 0; i--) {
            ExecutionMessage msg = messages.get(i);
            if ("user".equals(msg.role())) {
                return msg.content();
            }
        }
        return null;
    }

    /**
     * Returns the configured steering rules.
     *
     * @return unmodifiable list of rules
     */
    public List<SteeringRule> getRules() {
        return rules;
    }
}
