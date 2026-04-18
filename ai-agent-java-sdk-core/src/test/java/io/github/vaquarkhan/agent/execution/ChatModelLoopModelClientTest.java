package io.github.vaquarkhan.agent.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.tool.ToolCallback;

/**
 * Tests for {@link ChatModelLoopModelClient}, verifying correct message conversion,
 * tool call detection, and that internal tool execution is disabled.
 *
 * @author Vaquar Khan
 */
class ChatModelLoopModelClientTest {

    private ChatModel chatModel;
    private ChatModelLoopModelClient client;

    @BeforeEach
    void setUp() {
        chatModel = mock(ChatModel.class);
        client = new ChatModelLoopModelClient(chatModel);
    }

    @Test
    void finalAnswerMappedCorrectly() {
        AssistantMessage output = new AssistantMessage("Hello, world!");
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(output)));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        ModelTurnResponse response = client.generate(
                List.of(new ExecutionMessage("user", "Hi")), List.of());

        assertFalse(response.hasToolCall());
        assertEquals("Hello, world!", response.content());
    }

    @Test
    void singleToolCallMappedCorrectly() {
        AssistantMessage output = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(new AssistantMessage.ToolCall("tc-1", "function", "calculator", "{\"x\":2}")))
                .build();
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(output)));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        ModelTurnResponse response = client.generate(
                List.of(new ExecutionMessage("user", "2+2")), List.of());

        assertTrue(response.hasToolCall());
        assertFalse(response.hasMultipleToolCalls());
        assertEquals("calculator", response.toolCallRequest().toolName());
        assertEquals("{\"x\":2}", response.toolCallRequest().arguments());
    }

    @Test
    void multipleToolCallsMappedCorrectly() {
        AssistantMessage output = AssistantMessage.builder()
                .content("")
                .toolCalls(List.of(
                        new AssistantMessage.ToolCall("tc-1", "function", "add", "{\"a\":1,\"b\":2}"),
                        new AssistantMessage.ToolCall("tc-2", "function", "multiply", "{\"a\":3,\"b\":4}")))
                .build();
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(output)));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        ModelTurnResponse response = client.generate(
                List.of(new ExecutionMessage("user", "compute")), List.of());

        assertTrue(response.hasToolCall());
        assertTrue(response.hasMultipleToolCalls());
        List<ToolCallRequest> requests = response.allToolCallRequests();
        assertEquals(2, requests.size());
        assertEquals("add", requests.get(0).toolName());
        assertEquals("{\"a\":1,\"b\":2}", requests.get(0).arguments());
        assertEquals("multiply", requests.get(1).toolName());
        assertEquals("{\"a\":3,\"b\":4}", requests.get(1).arguments());
    }

    @Test
    void messagesConvertedToCorrectSpringAiTypes() {
        List<ExecutionMessage> messages = List.of(
                new ExecutionMessage("system", "You are helpful"),
                new ExecutionMessage("user", "Hello"),
                new ExecutionMessage("assistant", "Hi there"),
                new ExecutionMessage("tool", "result data"));

        List<Message> converted = client.convertMessages(messages);

        assertEquals(4, converted.size());
        assertTrue(converted.get(0) instanceof SystemMessage);
        assertTrue(converted.get(1) instanceof UserMessage);
        assertTrue(converted.get(2) instanceof AssistantMessage);
        assertTrue(converted.get(3) instanceof ToolResponseMessage);

        assertEquals("You are helpful", converted.get(0).getText());
        assertEquals("Hello", converted.get(1).getText());
        assertEquals("Hi there", converted.get(2).getText());
    }

    @Test
    void internalToolExecutionDisabled() {
        AssistantMessage output = new AssistantMessage("ok");
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(output)));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        ToolCallback mockTool = mock(ToolCallback.class);
        client.generate(List.of(new ExecutionMessage("user", "test")), List.of(mockTool));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());

        Prompt captured = promptCaptor.getValue();
        assertNotNull(captured.getOptions());
        assertTrue(captured.getOptions() instanceof ToolCallingChatOptions);
        ToolCallingChatOptions options = (ToolCallingChatOptions) captured.getOptions();
        assertEquals(false, options.getInternalToolExecutionEnabled());
    }

    @Test
    void toolCallbacksPassedToPromptOptions() {
        AssistantMessage output = new AssistantMessage("ok");
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(output)));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        ToolCallback tool1 = mock(ToolCallback.class);
        ToolCallback tool2 = mock(ToolCallback.class);
        client.generate(List.of(new ExecutionMessage("user", "test")), List.of(tool1, tool2));

        ArgumentCaptor<Prompt> promptCaptor = ArgumentCaptor.forClass(Prompt.class);
        verify(chatModel).call(promptCaptor.capture());

        ToolCallingChatOptions options = (ToolCallingChatOptions) promptCaptor.getValue().getOptions();
        assertEquals(2, options.getToolCallbacks().size());
    }

    @Test
    void nullTextInOutputReturnedAsEmptyString() {
        AssistantMessage output = new AssistantMessage(null);
        ChatResponse chatResponse = new ChatResponse(List.of(new Generation(output)));
        when(chatModel.call(any(Prompt.class))).thenReturn(chatResponse);

        ModelTurnResponse response = client.generate(
                List.of(new ExecutionMessage("user", "test")), List.of());

        assertFalse(response.hasToolCall());
        assertEquals("", response.content());
    }
}
