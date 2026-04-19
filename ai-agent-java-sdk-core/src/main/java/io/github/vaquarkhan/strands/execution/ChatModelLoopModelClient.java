package io.github.vaquarkhan.strands.execution;

import io.github.vaquarkhan.strands.execution.stream.StreamEvent;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.DefaultToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

/**
 * Bridge between Spring AI's {@link ChatModel} and this SDK's {@link LoopModelClient} interface.
 *
 * <p>This class calls {@code ChatModel.call(Prompt)} directly (not {@code ChatClient}) so that
 * the {@link AgentExecutionLoop} retains full control over the tool execution cycle. Tool
 * execution inside the model is explicitly disabled via
 * {@code internalToolExecutionEnabled(false)} on the chat options.
 *
 * @author Vaquar Khan
 */
public class ChatModelLoopModelClient implements LoopModelClient {

    private static final Logger log = LoggerFactory.getLogger(ChatModelLoopModelClient.class);

    private final ChatModel chatModel;

    public ChatModelLoopModelClient(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    @Override
    public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
        List<Message> springMessages = convertMessages(messages);
        Prompt prompt = buildPrompt(springMessages, tools);

        log.debug("Calling ChatModel with {} messages and {} tool callbacks", springMessages.size(), tools.size());
        ChatResponse response = chatModel.call(prompt);

        return mapResponse(response);
    }

    @Override
    public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
        List<Message> springMessages = convertMessages(messages);
        Prompt prompt = buildPrompt(springMessages, tools);

        log.debug("Streaming ChatModel with {} messages and {} tool callbacks", springMessages.size(), tools.size());

        return chatModel.stream(prompt)
                .filter(chunk -> chunk.getResult() != null && chunk.getResult().getOutput() != null)
                .concatMap(chunk -> {
                    AssistantMessage output = chunk.getResult().getOutput();

                    // Check for tool calls in the streaming chunk
                    if (output.hasToolCalls()) {
                        List<StreamEvent> events = new ArrayList<>();
                        for (AssistantMessage.ToolCall toolCall : output.getToolCalls()) {
                            events.add(new StreamEvent.ToolCallBoundary(
                                    new ToolCallRequest(toolCall.name(), toolCall.arguments())));
                        }
                        return Flux.fromIterable(events);
                    }

                    // Emit text tokens
                    String text = output.getText();
                    if (text != null && !text.isEmpty()) {
                        return Flux.just(new StreamEvent.Token(text));
                    }

                    return Flux.empty();
                })
                .concatWith(Flux.defer(() -> Flux.just(new StreamEvent.Complete(""))));
    }

    /**
     * Converts execution messages to Spring AI message types.
     */
    List<Message> convertMessages(List<ExecutionMessage> messages) {
        List<Message> result = new ArrayList<>(messages.size());
        for (ExecutionMessage msg : messages) {
            result.add(toSpringMessage(msg));
        }
        return result;
    }

    private Message toSpringMessage(ExecutionMessage msg) {
        return switch (msg.role()) {
            case "system" -> new SystemMessage(msg.content());
            case "user" -> new UserMessage(msg.content());
            case "assistant" -> new AssistantMessage(msg.content());
            case "tool" -> ToolResponseMessage.builder()
                    .responses(List.of(
                            new ToolResponseMessage.ToolResponse("", "", msg.content())))
                    .build();
            default -> throw new IllegalArgumentException("Unknown message role: " + msg.role());
        };
    }

    /**
     * Builds a Spring AI {@link Prompt} with tool callbacks and internal tool execution disabled.
     */
    Prompt buildPrompt(List<Message> messages, List<ToolCallback> tools) {
        ToolCallingChatOptions options = DefaultToolCallingChatOptions.builder()
                .toolCallbacks(tools)
                .internalToolExecutionEnabled(false)
                .build();
        return new Prompt(messages, options);
    }

    /**
     * Maps a Spring AI {@link ChatResponse} to a {@link ModelTurnResponse}.
     */
    ModelTurnResponse mapResponse(ChatResponse response) {
        AssistantMessage output = response.getResult().getOutput();

        if (output.hasToolCalls()) {
            List<AssistantMessage.ToolCall> toolCalls = output.getToolCalls();

            if (toolCalls.size() == 1) {
                AssistantMessage.ToolCall tc = toolCalls.get(0);
                log.debug("Model requested single tool call: {}", tc.name());
                return ModelTurnResponse.toolCall(tc.name(), tc.arguments());
            }

            log.debug("Model requested {} parallel tool calls", toolCalls.size());
            List<ToolCallRequest> requests = new ArrayList<>(toolCalls.size());
            for (AssistantMessage.ToolCall tc : toolCalls) {
                requests.add(new ToolCallRequest(tc.name(), tc.arguments()));
            }
            return ModelTurnResponse.multiToolCall(requests);
        }

        String content = output.getText() != null ? output.getText() : "";
        log.debug("Model returned final answer ({} chars)", content.length());
        return ModelTurnResponse.finalAnswer(content);
    }
}
