package io.github.vaquarkhan.strands.plugin;

import java.util.List;

/**
 * A reusable skill that combines a prompt fragment with required tools.
 *
 * <p>Skills are loaded by {@link SkillsPlugin} and their prompt fragments
 * are prepended to the system prompt via an Advisor.
 *
 * @param name          the skill name
 * @param description   a human-readable description
 * @param promptFragment the prompt text to prepend to the system prompt
 * @param requiredTools  list of tool names this skill depends on
 *
 * @author Vaquar Khan
 */
public record Skill(String name, String description, String promptFragment, List<String> requiredTools) {

    public Skill {
        requiredTools = requiredTools != null ? List.copyOf(requiredTools) : List.of();
    }
}
