package io.github.vaquarkhan.strands.execution;

import io.github.vaquarkhan.strands.execution.stream.StreamEvent;
import java.util.List;
import java.util.Optional;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;

/**
 * Resolves {@link ChatModel} from an {@link ObjectProvider} on first use. Used when no
 * {@code ChatModel} bean was visible yet at auto-configuration bean-creation time (ordering /
 * conditional registration edge cases); after the context is fully wired, the delegate becomes
 * {@link ChatModelLoopModelClient} or {@link NoopLoopModelClient}.
 *
 * @author Vaquar Khan
 */
public final class DeferredChatModelLoopModelClient implements LoopModelClient {

    private final ObjectProvider<ChatModel> chatModels;
    private volatile LoopModelClient delegate;

    public DeferredChatModelLoopModelClient(ObjectProvider<ChatModel> chatModels) {
        this.chatModels = chatModels;
    }

    private LoopModelClient delegate() {
        LoopModelClient d = delegate;
        if (d != null) {
            return d;
        }
        synchronized (this) {
            if (delegate == null) {
                Optional<ChatModel> first = chatModels.stream().findFirst();
                delegate = first.<LoopModelClient>map(ChatModelLoopModelClient::new).orElseGet(NoopLoopModelClient::new);
            }
            return delegate;
        }
    }

    @Override
    public ModelTurnResponse generate(List<ExecutionMessage> messages, List<ToolCallback> tools) {
        return delegate().generate(messages, tools);
    }

    @Override
    public Flux<StreamEvent> stream(List<ExecutionMessage> messages, List<ToolCallback> tools) {
        return delegate().stream(messages, tools);
    }
}
