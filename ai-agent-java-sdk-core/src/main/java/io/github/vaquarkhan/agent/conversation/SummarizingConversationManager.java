package io.github.vaquarkhan.agent.conversation;

import io.github.vaquarkhan.agent.execution.ExecutionMessage;
import io.github.vaquarkhan.agent.execution.LoopModelClient;
import io.github.vaquarkhan.agent.execution.AgentExecutionContext;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Conversation manager that summarizes older messages into a compact "summary" message.
 *
 * <p>This is intentionally generic and provider-agnostic: summarization uses the configured {@link LoopModelClient}
 * with an empty tool set.
 *
 * @author Vaquar Khan
 */
public class SummarizingConversationManager implements ConversationManager {

    private final LoopModelClient modelClient;
    private final int keepLastMessages;
    private final String summarizationInstruction;

    public SummarizingConversationManager(LoopModelClient modelClient, int keepLastMessages) {
        this(modelClient, keepLastMessages,
                "Summarize the conversation so far for future context. "
                        + "Preserve user intent, constraints, important facts, and decisions. "
                        + "Write a concise summary.");
    }

    public SummarizingConversationManager(LoopModelClient modelClient, int keepLastMessages, String summarizationInstruction) {
        this.modelClient = Objects.requireNonNull(modelClient, "modelClient");
        if (keepLastMessages < 1) {
            throw new IllegalArgumentException("keepLastMessages must be >= 1");
        }
        this.keepLastMessages = keepLastMessages;
        this.summarizationInstruction = Objects.requireNonNull(summarizationInstruction, "summarizationInstruction");
    }

    @Override
    public List<ExecutionMessage> manage(List<ExecutionMessage> messages, AgentExecutionContext context) {
        Objects.requireNonNull(messages, "messages");
        Objects.requireNonNull(context, "context");

        if (messages.size() <= keepLastMessages) {
            return messages;
        }

        int split = Math.max(0, messages.size() - keepLastMessages);
        List<ExecutionMessage> toSummarize = messages.subList(0, split);
        List<ExecutionMessage> tail = messages.subList(split, messages.size());

        StringBuilder transcript = new StringBuilder();
        for (ExecutionMessage m : toSummarize) {
            transcript.append(m.role()).append(": ").append(m.content()).append("\n");
        }

        List<ExecutionMessage> summarizerPrompt = List.of(
                new ExecutionMessage("system", summarizationInstruction),
                new ExecutionMessage("user", transcript.toString())
        );

        String summary = modelClient.generate(summarizerPrompt, List.of()).content();
        ExecutionMessage summaryMessage = new ExecutionMessage("assistant", "Summary: " + summary);

        ArrayList<ExecutionMessage> managed = new ArrayList<>(1 + tail.size());
        managed.add(summaryMessage);
        managed.addAll(tail);
        return managed;
    }
}

