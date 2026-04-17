package com.example.spring.ai.agent.plugin;

import com.example.spring.ai.agent.AiAgent;
import com.example.spring.ai.agent.api.Advisor;
import com.example.spring.ai.agent.execution.ExecutionMessage;
import com.example.spring.ai.agent.execution.AgentExecutionContext;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Built-in plugin that loads "skills" - reusable prompt + tool combinations.
 *
 * <p>A {@link Skill} bundles a prompt fragment with a list of required tools.
 * This plugin prepends skill prompt fragments to the conversation as system
 * messages via an {@link Advisor}.
 *
 * @author Vaquar Khan
 */
public class SkillsPlugin implements AgentPlugin {

    private static final Logger log = LoggerFactory.getLogger(SkillsPlugin.class);

    private final List<Skill> skills;

    /**
     * Creates a skills plugin with the given list of skills.
     *
     * @param skills the skills to load
     */
    public SkillsPlugin(List<Skill> skills) {
        this.skills = skills != null ? List.copyOf(skills) : List.of();
    }

    @Override
    public void init(AiAgent agent) {
        if (skills.isEmpty()) {
            log.debug("No skills configured for SkillsPlugin");
            return;
        }
        log.info("SkillsPlugin initializing with {} skill(s)", skills.size());
    }

    /**
     * Returns an {@link Advisor} that prepends skill prompt fragments as system messages.
     *
     * @return the skills advisor
     */
    public Advisor createAdvisor() {
        return (messages, context) -> {
            if (skills.isEmpty()) {
                return messages;
            }
            List<ExecutionMessage> enriched = new ArrayList<>();
            for (Skill skill : skills) {
                if (skill.promptFragment() != null && !skill.promptFragment().isBlank()) {
                    enriched.add(new ExecutionMessage("system", skill.promptFragment()));
                }
            }
            enriched.addAll(messages);
            return enriched;
        };
    }

    /**
     * Returns the list of loaded skills.
     *
     * @return unmodifiable list of skills
     */
    public List<Skill> getSkills() {
        return skills;
    }

    @Override
    public String name() {
        return "SkillsPlugin";
    }
}
