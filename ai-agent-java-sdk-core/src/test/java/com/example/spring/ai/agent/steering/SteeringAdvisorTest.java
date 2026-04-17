package com.example.spring.ai.agent.steering;

import com.example.spring.ai.agent.execution.ExecutionMessage;
import com.example.spring.ai.agent.execution.AgentExecutionContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Vaquar Khan
 */
class SteeringAdvisorTest {

    @Test
    void matchingRulePrependsInstruction() {
        SteeringRule rule = new SteeringRule("code-rule", "code", "Always include code examples.");
        SteeringAdvisor advisor = new SteeringAdvisor(List.of(rule));

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "Write some code for me"));

        List<ExecutionMessage> result = advisor.apply(messages, context());

        assertEquals(2, result.size());
        assertEquals("system", result.get(0).role());
        assertEquals("Always include code examples.", result.get(0).content());
        assertEquals("user", result.get(1).role());
    }

    @Test
    void nonMatchingRuleIsSkipped() {
        SteeringRule rule = new SteeringRule("code-rule", "code", "Always include code examples.");
        SteeringAdvisor advisor = new SteeringAdvisor(List.of(rule));

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "Tell me about the weather"));

        List<ExecutionMessage> result = advisor.apply(messages, context());

        assertEquals(1, result.size());
        assertEquals("user", result.get(0).role());
    }

    @Test
    void multipleMatchingRulesAllApplied() {
        SteeringRule rule1 = new SteeringRule("code-rule", "code", "Include code examples.");
        SteeringRule rule2 = new SteeringRule("java-rule", "java", "Use Java syntax.");
        SteeringAdvisor advisor = new SteeringAdvisor(List.of(rule1, rule2));

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "Write Java code"));

        List<ExecutionMessage> result = advisor.apply(messages, context());

        assertEquals(3, result.size());
        assertEquals("Include code examples.", result.get(0).content());
        assertEquals("Use Java syntax.", result.get(1).content());
        assertEquals("Write Java code", result.get(2).content());
    }

    @Test
    void emptyRulesListIsNoOp() {
        SteeringAdvisor advisor = new SteeringAdvisor(List.of());

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "hello"));

        List<ExecutionMessage> result = advisor.apply(messages, context());

        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).content());
    }

    @Test
    void nullRulesListIsNoOp() {
        SteeringAdvisor advisor = new SteeringAdvisor(null);

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "hello"));

        List<ExecutionMessage> result = advisor.apply(messages, context());

        assertEquals(1, result.size());
    }

    @Test
    void conditionMatchIsCaseInsensitive() {
        SteeringRule rule = new SteeringRule("code-rule", "CODE", "Include code.");
        SteeringAdvisor advisor = new SteeringAdvisor(List.of(rule));

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "write some code please"));

        List<ExecutionMessage> result = advisor.apply(messages, context());

        assertEquals(2, result.size());
        assertEquals("Include code.", result.get(0).content());
    }

    @Test
    void emptyMessagesReturnedAsIs() {
        SteeringRule rule = new SteeringRule("rule", "test", "instruction");
        SteeringAdvisor advisor = new SteeringAdvisor(List.of(rule));

        List<ExecutionMessage> result = advisor.apply(List.of(), context());
        assertEquals(0, result.size());
    }

    @Test
    void nullMessagesReturnedAsNull() {
        SteeringRule rule = new SteeringRule("rule", "test", "instruction");
        SteeringAdvisor advisor = new SteeringAdvisor(List.of(rule));

        List<ExecutionMessage> result = advisor.apply(null, context());
        assertEquals(null, result);
    }

    @Test
    void matchesAgainstLastUserMessage() {
        SteeringRule rule = new SteeringRule("rule", "help", "Be helpful.");
        SteeringAdvisor advisor = new SteeringAdvisor(List.of(rule));

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "first message"),
                new ExecutionMessage("assistant", "response"),
                new ExecutionMessage("user", "I need help"));

        List<ExecutionMessage> result = advisor.apply(messages, context());

        assertEquals(4, result.size());
        assertEquals("Be helpful.", result.get(0).content());
    }

    @Test
    void noUserMessageMeansNoMatching() {
        SteeringRule rule = new SteeringRule("rule", "test", "instruction");
        SteeringAdvisor advisor = new SteeringAdvisor(List.of(rule));

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("system", "system message"),
                new ExecutionMessage("assistant", "test response"));

        List<ExecutionMessage> result = advisor.apply(messages, context());
        assertEquals(2, result.size());
    }

    @Test
    void getRulesReturnsConfiguredRules() {
        SteeringRule rule1 = new SteeringRule("r1", "c1", "i1");
        SteeringRule rule2 = new SteeringRule("r2", "c2", "i2");
        SteeringAdvisor advisor = new SteeringAdvisor(List.of(rule1, rule2));

        assertEquals(2, advisor.getRules().size());
    }

    @Test
    void ruleWithNullConditionIsSkipped() {
        SteeringRule rule = new SteeringRule("rule", null, "instruction");
        SteeringAdvisor advisor = new SteeringAdvisor(List.of(rule));

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "hello"));

        List<ExecutionMessage> result = advisor.apply(messages, context());
        assertEquals(1, result.size());
    }

    private AgentExecutionContext context() {
        return AgentExecutionContext.standalone("test-session");
    }
}
