package io.github.vaquarkhan.strands;

import io.github.vaquarkhan.strands.api.Advisor;
import io.github.vaquarkhan.strands.approval.ApprovalManager;
import io.github.vaquarkhan.strands.config.AiAgentProperties;
import io.github.vaquarkhan.strands.config.AiAgentPropertiesValidator;
import io.github.vaquarkhan.strands.execution.ExecutionMessage;
import io.github.vaquarkhan.strands.execution.AgentExecutionContext;
import io.github.vaquarkhan.strands.execution.AgentExecutionLoop;
import io.github.vaquarkhan.strands.execution.AgentLoopResult;
import io.github.vaquarkhan.strands.hook.HookRegistry;
import io.github.vaquarkhan.strands.hook.AgentHookEvent;
import io.github.vaquarkhan.strands.model.AgentResponse;
import io.github.vaquarkhan.strands.observability.AgentObservability;
import io.github.vaquarkhan.strands.plugin.AgentPlugin;
import io.github.vaquarkhan.strands.session.SessionManager;
import io.github.vaquarkhan.strands.tool.ToolRegistry;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.springframework.core.io.Resource;
import reactor.core.publisher.Flux;

/**
 * Model-driven agent facade: runs the execution loop with configured tools, advisors,
 * hooks, session management, and plugin support.
 *
 * @author Vaquar Khan
 */
public class AiAgent {

    private final AgentExecutionLoop executionLoop;
    private final ToolRegistry toolRegistry;
    private final AiAgentProperties properties;
    private final List<Advisor> advisors;
    private final AgentObservability observability;
    private HookRegistry hookRegistry;
    private ApprovalManager approvalManager;
    private SessionManager sessionManager;
    private List<AgentPlugin> plugins;

    public AiAgent(
            AgentExecutionLoop executionLoop,
            ToolRegistry toolRegistry,
            AiAgentProperties properties,
            List<Advisor> advisors,
            AgentObservability observability) {
        this.executionLoop = executionLoop;
        this.toolRegistry = toolRegistry;
        this.properties = properties;
        this.advisors = advisors == null ? List.of() : advisors;
        this.observability = observability;
    }

    public void setHookRegistry(HookRegistry hookRegistry) {
        this.hookRegistry = hookRegistry;
    }

    public HookRegistry getHookRegistry() {
        return hookRegistry;
    }

    public void setApprovalManager(ApprovalManager approvalManager) {
        this.approvalManager = approvalManager;
        this.executionLoop.setApprovalManager(approvalManager);
    }

    public ApprovalManager getApprovalManager() {
        return approvalManager;
    }

    public void setSessionManager(SessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    public SessionManager getSessionManager() {
        return sessionManager;
    }

    public void setPlugins(List<AgentPlugin> plugins) {
        this.plugins = plugins;
    }

    public List<AgentPlugin> getPlugins() {
        return plugins;
    }

    /**
     * Initialize all registered plugins. Called after construction when plugins are set.
     */
    public void initPlugins() {
        if (plugins == null || plugins.isEmpty()) {
            return;
        }
        for (AgentPlugin plugin : plugins) {
            plugin.init(this);
        }
    }

    public AgentResponse execute(String userPrompt, AgentExecutionContext context) {
        AiAgentPropertiesValidator.validateOrThrow(properties);

        // Dispatch before-invocation hook
        dispatchHook(new AgentHookEvent.BeforeInvocation(userPrompt, context));

        // Load session messages if session manager is configured
        List<ExecutionMessage> sessionMessages = loadSessionMessages(context.getSessionId());

        List<ExecutionMessage> messages = new ArrayList<>(sessionMessages);
        messages.addAll(baseMessages(userPrompt));
        messages = applyAdvisors(messages, context);

        AgentLoopResult result =
                executionLoop.run(resolveSystemPrompt(), messages, toolRegistry, observability, context);

        AgentResponse response = new AgentResponse(
                result.content(),
                result.reasoningTrace(),
                result.terminationReason(),
                result.iterationCount(),
                result.totalDuration());

        // Save session messages after execution
        saveSessionMessages(context.getSessionId(), messages, response);

        // Dispatch after-invocation hook
        dispatchHook(new AgentHookEvent.AfterInvocation(response));

        return response;
    }

    public Flux<String> executeStreaming(String userPrompt, AgentExecutionContext context) {
        AiAgentPropertiesValidator.validateOrThrow(properties);
        List<ExecutionMessage> messages = applyAdvisors(baseMessages(userPrompt), context);
        return executionLoop.runStreaming(resolveSystemPrompt(), messages, toolRegistry, observability, context);
    }

    private List<ExecutionMessage> baseMessages(String userPrompt) {
        return new ArrayList<>(List.of(new ExecutionMessage("user", userPrompt)));
    }

    private List<ExecutionMessage> applyAdvisors(List<ExecutionMessage> messages, AgentExecutionContext context) {
        List<ExecutionMessage> enriched = messages;
        for (Advisor advisor : advisors) {
            enriched = advisor.apply(enriched, context);
        }
        return enriched;
    }

    private String resolveSystemPrompt() {
        if (properties.getSystemPrompt() != null && !properties.getSystemPrompt().isBlank()) {
            return properties.getSystemPrompt();
        }
        Resource resource = properties.getSystemPromptResource();
        if (resource == null || !resource.exists()) {
            return "";
        }
        try {
            return new String(resource.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read system prompt resource", e);
        }
    }

    private void dispatchHook(AgentHookEvent event) {
        if (hookRegistry != null) {
            hookRegistry.dispatch(event);
        }
    }

    private List<ExecutionMessage> loadSessionMessages(String sessionId) {
        if (sessionManager == null || sessionId == null) {
            return List.of();
        }
        if (sessionManager.exists(sessionId)) {
            return sessionManager.load(sessionId);
        }
        return List.of();
    }

    private void saveSessionMessages(String sessionId, List<ExecutionMessage> messages, AgentResponse response) {
        if (sessionManager == null || sessionId == null) {
            return;
        }
        List<ExecutionMessage> toSave = new ArrayList<>(messages);
        if (response.content() != null && !response.content().isBlank()) {
            toSave.add(new ExecutionMessage("assistant", response.content()));
        }
        sessionManager.save(sessionId, toSave);
    }
}
