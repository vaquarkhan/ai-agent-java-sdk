package io.github.vaquarkhan.strands.conversation;

import static org.assertj.core.api.Assertions.assertThat;

import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import io.github.vaquarkhan.strands.execution.LoopModelClient;
import io.github.vaquarkhan.strands.execution.ModelTurnResponse;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import io.github.vaquarkhan.strands.execution.stream.StreamEvent;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.ai.tool.ToolCallback;
import reactor.core.publisher.Flux;

class SummarizingConversationManagerTest {

    @Test
    void summarizesOldMessagesAndKeepsTail() {
        CapturingLoopModelClient model = new CapturingLoopModelClient("hello-summary");
        SummarizingConversationManager mgr = new SummarizingConversationManager(model, 2);

        List<ExecutionMessage> managed = mgr.manage(List.of(
                new ExecutionMessage("user", "u1"),
                new ExecutionMessage("assistant", "a1"),
                new ExecutionMessage("user", "u2"),
                new ExecutionMessage("assistant", "a2")
        ), new AgentExecutionContext(null, null, Map.of()));

        assertThat(managed).hasSize(3);
        assertThat(managed.get(0).role()).isEqualTo("assistant");
        assertThat(managed.get(0).content()).contains("Summary: hello-summary");
        assertThat(managed.get(1).content()).isEqualTo("u2");
        assertThat(managed.get(2).content()).isEqualTo("a2");

        assertThat(model.lastMessages).isNotNull();
        assertThat(model.lastMessages.get(0).role()).isEqualTo("system");
        assertThat(model.lastMessages.get(1).role()).isEqualTo("user");
        assertThat(model.lastMessages.get(1).content()).contains("user: u1").contains("assistant: a1");
    }

    private static final class CapturingLoopModelClient implements LoopModelClient {
        private final String response;
        private List<ExecutionMessage> lastMessages;

        private CapturingLoopModelClient(String response) {
            this.response = response;
        }

        @Override
        public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
            this.lastMessages = messages;
            return ModelTurnResponse.finalAnswer(response);
        }

        @Override
        public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
            this.lastMessages = messages;
            return Flux.empty();
        }
    }
}

