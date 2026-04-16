package com.example.spring.ai.strands.agent.plugin;

import com.example.spring.ai.strands.agent.api.Advisor;
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Vaquar Khan
 */
class SkillsPluginTest {

    @Test
    void loadSkillsFromList() {
        List<Skill> skills = List.of(
                new Skill("search", "Web search skill", "You can search the web.", List.of("web_search")),
                new Skill("calc", "Calculator skill", "You can do math.", List.of("calculator")));

        SkillsPlugin plugin = new SkillsPlugin(skills);

        assertEquals(2, plugin.getSkills().size());
        assertEquals("search", plugin.getSkills().get(0).name());
        assertEquals("calc", plugin.getSkills().get(1).name());
    }

    @Test
    void skillPromptFragmentsPrependedViaAdvisor() {
        List<Skill> skills = List.of(
                new Skill("search", "Search", "You can search the web.", List.of()),
                new Skill("calc", "Calc", "You can do math.", List.of()));

        SkillsPlugin plugin = new SkillsPlugin(skills);
        Advisor advisor = plugin.createAdvisor();

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "What is 2+2?"));

        List<ExecutionMessage> result = advisor.apply(messages, context());

        // Should have 2 system messages (skill fragments) + 1 user message
        assertEquals(3, result.size());
        assertEquals("system", result.get(0).role());
        assertEquals("You can search the web.", result.get(0).content());
        assertEquals("system", result.get(1).role());
        assertEquals("You can do math.", result.get(1).content());
        assertEquals("user", result.get(2).role());
    }

    @Test
    void emptySkillsListProducesNoOpAdvisor() {
        SkillsPlugin plugin = new SkillsPlugin(List.of());
        Advisor advisor = plugin.createAdvisor();

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "hello"));

        List<ExecutionMessage> result = advisor.apply(messages, context());
        assertEquals(1, result.size());
        assertEquals("hello", result.get(0).content());
    }

    @Test
    void nullSkillsListTreatedAsEmpty() {
        SkillsPlugin plugin = new SkillsPlugin(null);
        assertTrue(plugin.getSkills().isEmpty());
    }

    @Test
    void skillWithBlankPromptFragmentIsSkipped() {
        List<Skill> skills = List.of(
                new Skill("empty", "Empty", "", List.of()),
                new Skill("valid", "Valid", "Do something.", List.of()));

        SkillsPlugin plugin = new SkillsPlugin(skills);
        Advisor advisor = plugin.createAdvisor();

        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("user", "test"));

        List<ExecutionMessage> result = advisor.apply(messages, context());
        // Only the valid skill fragment + user message
        assertEquals(2, result.size());
        assertEquals("Do something.", result.get(0).content());
    }

    @Test
    void skillRecordAccessors() {
        Skill skill = new Skill("name", "desc", "fragment", List.of("tool1", "tool2"));
        assertEquals("name", skill.name());
        assertEquals("desc", skill.description());
        assertEquals("fragment", skill.promptFragment());
        assertEquals(2, skill.requiredTools().size());
    }

    @Test
    void skillRequiredToolsDefaultsToEmptyList() {
        Skill skill = new Skill("name", "desc", "fragment", null);
        assertNotNull(skill.requiredTools());
        assertTrue(skill.requiredTools().isEmpty());
    }

    @Test
    void pluginNameReturnsSkillsPlugin() {
        SkillsPlugin plugin = new SkillsPlugin(List.of());
        assertEquals("SkillsPlugin", plugin.name());
    }

    private StrandsExecutionContext context() {
        return StrandsExecutionContext.standalone("test-session");
    }
}
