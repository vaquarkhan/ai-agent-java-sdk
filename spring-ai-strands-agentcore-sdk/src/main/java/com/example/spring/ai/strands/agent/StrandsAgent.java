package com.example.spring.ai.strands.agent;

import com.example.spring.ai.strands.agent.api.Advisor;
import com.example.spring.ai.strands.agent.approval.ApprovalManager;
import com.example.spring.ai.strands.agent.config.StrandsAgentProperties;
import com.example.spring.ai.strands.agent.config.StrandsAgentPropertiesValidator;
import com.example.spring.ai.strands.agent.execution.ExecutionMessage;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionContext;
import com.example.spring.ai.strands.agent.execution.StrandsExecutionLoop;
import com.example.spring.ai.strands.agent.execution.StrandsLoopResult;
import com.example.spring.ai.strands.agent.hook.HookRegistry;
import com.example.spring.ai.strands.agent.hook.StrandsHookEvent;
import com.example.spring.ai.strands.agent.model.StrandsAgentResponse;
import com.example.spring.ai.strands.agent.observability.StrandsObservability;
import com.example.spring.ai.strands.agent.plugin.StrandsPlugin;
import com.example.spring.ai.strands.agent.session.SessionManager;
import com.example.spring.ai.strands.agent.tool.ToolRegistry;
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
public class StrandsAgent {

    private final StrandsExecutionLoop executionLoop;
    private final ToolRegistry toolRegistry;
    private final StrandsAgentProperties properties;
    private final List<Advisor> advisors;
    private final StrandsObservability observability;
    private HookRegistry hookRegistry;
    private ApprovalManager approvalManager;
    private SessionManager sessionManager;
    private List<StrandsPlugin> plugins;

    public StrandsAgent(
            StrandsExecutionLoop executionLoop,
            ToolRegistry toolRegistry,
            StrandsAgentProperties properties,
            List<Advisor> advisors,
            StrandsObservability observability) {
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

    public void setPlugins(List<StrandsPlugin> plugins) {
        this.plugins = plugins;
    }

    public List<StrandsPlugin> getPlugins() {
        return plugins;
    }

    /**
     * Initialize all registered plugins. Called after construction when plugins are set.
     */
    public void initPlugins() {
        if (plugins == null || plugins.isEmpty()) {
            return;
        }
        for (StrandsPlugin plugin : plugins) {
            plugin.init(this);
        }
    }

    public StrandsAgentResponse execute(String userPrompt, StrandsExecutionContext context) {
        StrandsAgentPropertiesValidator.validateOrThrow(properties);

        // Dispatch before-invocation hook
        dispatchHook(new StrandsHookEvent.BeforeInvocation(userPrompt, context));

        // Load session messages if session manager is configured
        List<ExecutionMessage> sessionMessages = loadSessionMessages(context.getSessionId());

        List<ExecutionMessage> messages = new ArrayList<>(sessionMessages);
        messages.addAll(baseMessages(userPrompt));
        messages = applyAdvisors(messages, context);

        StrandsLoopResult result =
                executionLoop.run(resolveSystemPrompt(), messages, toolRegistry, observability, context);

        StrandsAgentResponse response = new StrandsAgentResponse(
                result.content(),
                result.reasoningTrace(),
                result.terminationReason(),
                result.iterationCount(),
                result.totalDuration());

        // Save session messages after execution
        saveSessionMessages(context.getSessionId(), messages, response);

        // Dispatch after-invocation hook
        dispatchHook(new StrandsHookEvent.AfterInvocation(response));

        return response;
    }

    public Flux<String> executeStreaming(String userPrompt, StrandsExecutionContext context) {
        StrandsAgentPropertiesValidator.validateOrThrow(properties);
        List<ExecutionMessage> messages = applyAdvisors(baseMessages(userPrompt), context);
        return executionLoop.runStreaming(resolveSystemPrompt(), messages, toolRegistry, observability, context);
    }

    private List<ExecutionMessage> baseMessages(String userPrompt) {
        return new ArrayList<>(List.of(new ExecutionMessage("user", userPrompt)));
    }

    private List<ExecutionMessage> applyAdvisors(List<ExecutionMessage> messages, StrandsExecutionContext context) {
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

    private void dispatchHook(StrandsHookEvent event) {
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

    private void saveSessionMessages(String sessionId, List<ExecutionMessage> messages, StrandsAgentResponse response) {
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
