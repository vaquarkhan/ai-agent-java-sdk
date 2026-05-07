package io.github.vaquarkhan.strands.steering;

/**
 * A steering rule that conditionally injects instructions into the conversation.
 *
 * <p>The condition is a simple keyword match against the user prompt. When the
 * user prompt contains the condition keyword (case-insensitive), the instruction
 * is prepended as a system message.
 *
 * @param name        the rule name (for logging and identification)
 * @param condition   the keyword to match against the user prompt (case-insensitive)
 * @param instruction the instruction to prepend when the condition matches
 *
 * @author Vaquar Khan
 */
public record SteeringRule(String name, String condition, String instruction) {
}
