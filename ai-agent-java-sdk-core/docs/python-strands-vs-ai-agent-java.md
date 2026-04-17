# AWS Strands Agents (Python) vs AI Agent Java SDK

_Author: Vaquar Khan_

The [AWS Strands Agents Python SDK](https://github.com/strands-agents/sdk-python) ships first-class support for **Python** and **TypeScript**. This repository's **AI Agent Java SDK** (`ai-agent-java-sdk-core`) implements a **model-driven** pattern on the JVM **inspired by** that mental model: model plus tools in a loop until a final answer, integrated with **Spring Boot** and **Spring AI**. This is an **independent** Java library—not an official AWS Strands product.

---

## Core Concepts Side-by-Side

| Topic | Python Strands | Java / Spring AI (this SDK) |
|-------|---------------|----------------------------|
| **Language** | Python 3.10+, pip | Java 17+, Maven/Gradle, Spring Boot 3.x |
| **Agent entry point** | `Agent()` | `AiAgent` (auto-configured bean) |
| **Execution loop** | `event_loop_cycle` (recursive) | `AgentExecutionLoop.run()` (iterative) |
| **Model abstraction** | `Model` ABC with `BedrockModel`, `OpenAIModel`, `OllamaModel`, etc. | `LoopModelClient` interface (wraps Spring AI `ChatClient`) |
| **System prompt** | `Agent(system_prompt="...")` | `ai.agent.system-prompt` or `system-prompt-resource` |
| **Tools** | `@tool` decorator on Python functions | `ToolCallback` + `ToolCallbackProvider` beans |
| **Tool registry** | `ToolRegistry` class | `ToolBridge.discoverTools()` builds `ToolRegistry` |
| **Tool filtering** | Pass specific tools to `Agent(tools=[...])` | `tool-discovery.include-patterns` / `exclude-patterns` globs |
| **MCP tools** | `MCPClient` wrapping stdio/SSE servers | Spring AI MCP client starter auto-registers `ToolCallbackProvider` beans |
| **Dynamic MCP** | `mcp_client` tool connects at runtime | `DynamicMcpToolConnector` with `connect()`/`disconnect()` |
| **Directory tools** | `Agent(load_tools_from_directory="./tools")` | `DirectoryToolLoader` watches `.json` files with `WatchService` |
| **Parallel tools** | Model requests multiple `tool_use` blocks | `ModelTurnResponse.multiToolCall()` + `ToolRegistry.executeToolsParallel()` |
| **Session context** | Python context objects | `AgentExecutionContext` (session ID, user ID, headers) |
| **Prompt enrichment** | Hooks / middleware | `Advisor` beans applied before the loop |
| **Streaming** | `agent.stream_async()` async generator | `agent.executeStreaming()` returns `Flux<String>` |
| **Configuration** | Code + environment variables | `application.yml` under `ai.agent.*` |

---

## Extensibility Side-by-Side

| Topic | Python Strands | Java / Spring AI (this SDK) |
|-------|---------------|----------------------------|
| **Hook system** | `HookRegistry` with `BeforeInvocationEvent`, `BeforeModelCallEvent`, etc. | `HookRegistry` with `AgentHookEvent.BeforeInvocation`, `.BeforeModelCall`, etc. |
| **Hook annotation** | `@hook` decorator | `@OnHook(AgentHookEvent.BeforeModelCall.class)` annotation |
| **Hook processor** | Auto-discovery in Plugin | `HookAnnotationProcessor.process(bean)` |
| **Plugin system** | `Plugin` base class with `init_agent()` | `AgentPlugin` interface with `init(agent)` |
| **Plugin auto-discovery** | Scans for hooks and tools in plugin | `PluginScanner` scans for `@OnHook` methods and `ToolCallbackProvider` methods |
| **Skills** | Agent Skills Plugin | `SkillsPlugin` + `Skill` record |
| **Steering** | Steering System | `SteeringAdvisor` + `SteeringRule` record |
| **Callback handlers** | `callback_handler` function | `@OnHook` annotations or `HookRegistry.register()` |

---

## State and Session Side-by-Side

| Topic | Python Strands | Java / Spring AI (this SDK) |
|-------|---------------|----------------------------|
| **Agent state** | `AgentState` class | `ExecutionMessage` list + `AgentExecutionContext` |
| **Session manager** | `SessionManager` base, `FileSessionManager`, `DynamoDBSessionManager` | `SessionManager` interface, `InMemorySessionManager`, `FileSessionManager` |
| **Conversation manager** | Sliding window, summarization, token counting | `SlidingWindowConversationManager`, `TokenCountConversationManager` |
| **Retry logic** | Error handler with retry/backoff | `AgentExecutionLoop` with `setRetryEnabled()`, `setMaxRetries()`, `setBackoffMillis()` |

---

## Observability Side-by-Side

| Topic | Python Strands | Java / Spring AI (this SDK) |
|-------|---------------|----------------------------|
| **Tracing** | OpenTelemetry spans | Micrometer Observation spans |
| **Metrics** | OpenTelemetry metrics | Micrometer meters (`ai.agent.iteration.count`, `ai.agent.loop.duration`, etc.) |
| **Logging** | Python `logging` module | SLF4J (DEBUG per-iteration, INFO timing) |
| **Trace output** | In-memory traces on `AgentResult` | `ReasoningTrace` on `AgentResponse` |
| **Sanitization** | Not built-in | Built-in: AWS keys, JWTs, emails redacted in traces |
| **Truncation** | Not built-in | Built-in: `trace-max-output-length` config |

---

## Security Side-by-Side

| Topic | Python Strands | Java / Spring AI (this SDK) |
|-------|---------------|----------------------------|
| **Tool argument limits** | Not built-in | `security.max-tool-argument-bytes` (default 64KB) |
| **Tool timeouts** | Basic timeout support | `security.tool-timeout-seconds` (default 60s) |
| **Tool rate limiting** | Not built-in | `security.tool-rate-limit` per loop |
| **Tool name validation** | Not built-in | `[a-zA-Z0-9_-]` pattern enforced |
| **SSRF prevention** | Not built-in | `system-prompt-resource` rejects http/https URLs |
| **Actuator hiding** | Not applicable | `@JsonIgnore` on sensitive properties |
| **Session isolation** | Not built-in | Immutable `AgentExecutionContext`, file locking |

---

## Multi-Agent Patterns

Multi-agent orchestration (Swarm, Graph, A2A) is **not implemented inside this SDK**. In the Python Strands ecosystem, these patterns live in `strands.multiagent`. In the Spring AI ecosystem, they live in **spring-ai-a2a** and your application's orchestration layer.

Here is how each Python pattern maps to the Java stack:

### Swarm Pattern

In Python Strands, a Swarm is a collaborative agent team where agents hand off to each other autonomously:

```python
# Python Strands
from strands import Agent
from strands.multiagent import Swarm

researcher = Agent(name="researcher", system_prompt="You research topics...")
coder = Agent(name="coder", system_prompt="You write code...")
reviewer = Agent(name="reviewer", system_prompt="You review code...")

swarm = Swarm(
    [researcher, coder, reviewer],
    entry_point=researcher,
    max_handoffs=20,
    execution_timeout=900.0
)
result = swarm("Design and implement a REST API for a todo app")
```

In Java, you build this by composing multiple `AiAgent` instances with a coordinator:

```java
// Java / Spring AI - Swarm-style coordination
@Service
public class SwarmCoordinator {

    private final Map<String, AiAgent> agents;

    public SwarmCoordinator(
            @Qualifier("researcher") AiAgent researcher,
            @Qualifier("coder") AiAgent coder,
            @Qualifier("reviewer") AiAgent reviewer) {
        this.agents = Map.of(
            "researcher", researcher,
            "coder", coder,
            "reviewer", reviewer
        );
    }

    public String execute(String task) {
        // Start with researcher
        String currentAgent = "researcher";
        String currentInput = task;
        int handoffs = 0;
        int maxHandoffs = 20;

        while (handoffs < maxHandoffs) {
            AiAgent agent = agents.get(currentAgent);
            AgentResponse response = agent.execute(
                currentInput,
                AgentExecutionContext.standalone("swarm-" + handoffs)
            );

            // Check if the agent wants to hand off
            String content = response.content();
            if (content.contains("HANDOFF:")) {
                String nextAgent = parseHandoffTarget(content);
                currentInput = parseHandoffMessage(content);
                currentAgent = nextAgent;
                handoffs++;
            } else {
                return content; // Final answer
            }
        }
        return "Max handoffs reached";
    }
}
```

For production swarm patterns, use **spring-ai-a2a** where each agent is an A2A server and the coordinator discovers agents via Agent Cards.

### Graph Pattern (DAG)

In Python Strands, a Graph is a deterministic directed graph where agents execute in dependency order:

```python
# Python Strands
from strands import Agent
from strands.multiagent import GraphBuilder

researcher = Agent(name="researcher", system_prompt="You research topics...")
analyst = Agent(name="analyst", system_prompt="You analyze data...")
fact_checker = Agent(name="fact_checker", system_prompt="You verify facts...")
report_writer = Agent(name="report_writer", system_prompt="You write reports...")

builder = GraphBuilder()
builder.add_node(researcher, "research")
builder.add_node(analyst, "analysis")
builder.add_node(fact_checker, "fact_check")
builder.add_node(report_writer, "report")

builder.add_edge("research", "analysis")
builder.add_edge("research", "fact_check")
builder.add_edge("analysis", "report")
builder.add_edge("fact_check", "report")

graph = builder.build()
result = graph("Research the impact of AI on healthcare")
```

In Java, you build this with a simple pipeline that chains `AiAgent` calls:

```java
// Java / Spring AI - Graph-style pipeline
@Service
public class ResearchPipeline {

    private final AiAgent researcher;
    private final AiAgent analyst;
    private final AiAgent factChecker;
    private final AiAgent reportWriter;

    public String execute(String task) {
        // Step 1: Research
        String researchResult = researcher.execute(
            task,
            AgentExecutionContext.standalone("research")
        ).content();

        // Step 2: Analysis and fact-checking in parallel
        CompletableFuture<String> analysisFuture = CompletableFuture.supplyAsync(() ->
            analyst.execute(
                "Based on this research, analyze: " + researchResult,
                AgentExecutionContext.standalone("analysis")
            ).content()
        );

        CompletableFuture<String> factCheckFuture = CompletableFuture.supplyAsync(() ->
            factChecker.execute(
                "Verify the facts in: " + researchResult,
                AgentExecutionContext.standalone("fact-check")
            ).content()
        );

        String analysisResult = analysisFuture.join();
        String factCheckResult = factCheckFuture.join();

        // Step 3: Report writing
        return reportWriter.execute(
            "Write a report based on:\nAnalysis: " + analysisResult
                + "\nFact check: " + factCheckResult,
            AgentExecutionContext.standalone("report")
        ).content();
    }
}
```

### A2A Protocol (Remote Agents)

In Python Strands, remote agents communicate via A2A:

```python
# Python Strands
from strands.a2a_agent import A2AAgent
from strands.multiagent import GraphBuilder

ml_analyzer = A2AAgent(
    endpoint="http://ml-service:9000",
    name="ml_analyzer",
    timeout=600
)

builder = GraphBuilder()
builder.add_node(ml_analyzer, "ml")
graph = builder.build()
result = graph("Analyze customer feedback")
```

In Java, use **spring-ai-a2a** for the same pattern:

```java
// Java / Spring AI - A2A remote agent
// See https://github.com/spring-ai-community/spring-ai-a2a

A2ACardResolver resolver = new A2ACardResolver(
    new JdkA2AHttpClient(),
    "http://ml-service:9000",
    "/a2a/card",
    null
);

Client client = Client.builder(resolver.getAgentCard())
    .withTransport(JSONRPCTransport.class, new JSONRPCTransportConfig())
    .build();

client.sendMessage(new Message.Builder()
    .role(Message.Role.USER)
    .parts(List.of(new TextPart("Analyze customer feedback")))
    .build());
```

---

## Powerful Examples

### Example 1: Research Agent with Web Search and Code Execution

**Python Strands:**

```python
from strands import Agent
from strands_tools import tavily_search, python_repl, file_write

agent = Agent(
    system_prompt="""You are a research agent. When asked a question:
    1. Search the web for current information
    2. Analyze the data using Python
    3. Write a summary report to a file""",
    tools=[tavily_search, python_repl, file_write]
)

agent("Research the latest trends in renewable energy and create a data analysis report")
```

**Java / Spring AI:**

```java
// application.yml
// ai.agent.system-prompt: "You are a research agent..."
// spring.ai.mcp.client.stdio.servers.tavily.command: "uvx"
// spring.ai.mcp.client.stdio.servers.tavily.args: ["tavily-mcp-server"]

@Service
public class ResearchAgent {

    private final AiAgent agent;

    public String research(String topic) {
        // MCP tools (tavily) + custom tools (file_write) are auto-discovered
        return agent.execute(
            "Research " + topic + " and create a data analysis report",
            AgentExecutionContext.standalone("research-session")
        ).content();
    }
}
```

### Example 2: Code Review Agent with Hooks

**Python Strands:**

```python
from strands import Agent, tool
import logging

logger = logging.getLogger("code_review")

def callback_handler(**kwargs):
    if "current_tool_use" in kwargs:
        tool = kwargs["current_tool_use"]
        logger.info(f"Tool called: {tool.get('name')}")

@tool
def read_file(path: str) -> str:
    """Read a source code file."""
    with open(path) as f:
        return f.read()

@tool
def write_review(path: str, review: str) -> str:
    """Write a code review to a file."""
    with open(path, 'w') as f:
        f.write(review)
    return f"Review written to {path}"

agent = Agent(
    system_prompt="You are a senior code reviewer...",
    tools=[read_file, write_review],
    callback_handler=callback_handler
)

agent("Review the code in src/main.py and write feedback to review.md")
```

**Java / Spring AI:**

```java
// Hook for tracking tool calls
@Component
public class CodeReviewHooks {

    @OnHook(AgentHookEvent.BeforeToolCall.class)
    public void logToolCall(AgentHookEvent event) {
        var e = (AgentHookEvent.BeforeToolCall) event;
        System.out.println("Tool called: " + e.toolName());
    }

    @OnHook(AgentHookEvent.AfterInvocation.class)
    public void logCompletion(AgentHookEvent event) {
        var e = (AgentHookEvent.AfterInvocation) event;
        System.out.println("Review completed in "
            + e.response().iterationCount() + " iterations");
    }
}

// Tools
@Bean
public ToolCallbackProvider codeReviewTools() {
    return () -> new ToolCallback[] {
        new FileReadToolCallback(),
        new WriteReviewToolCallback()
    };
}

// Usage
AgentResponse response = agent.execute(
    "Review the code in src/main.py and write feedback to review.md",
    AgentExecutionContext.standalone("code-review")
);
```

### Example 3: Customer Support Agent with Session Memory and Steering

**Python Strands:**

```python
from strands import Agent
from strands_tools import http_request

agent = Agent(
    system_prompt="You are a customer support agent for an e-commerce platform...",
    tools=[http_request]
)

# Multi-turn conversation
agent("I ordered item #12345 last week")
agent("Can you check the shipping status?")
agent("I want to return it")
```

**Java / Spring AI:**

```java
// Steering rules for customer support
@Bean
public SteeringAdvisor steeringAdvisor() {
    return new SteeringAdvisor(List.of(
        new SteeringRule("refund", "return",
            "Follow the return policy. Ask for order number and reason."),
        new SteeringRule("escalation", "manager",
            "Offer to escalate. Log the request for review.")
    ));
}

// Session persistence for multi-turn
@Bean
public SessionManager sessionManager(ObjectMapper mapper) {
    return new FileSessionManager(Path.of("/var/sessions"), mapper);
}

// Controller
@PostMapping("/support")
public Map<String, String> support(@RequestBody SupportRequest req) {
    AgentResponse response = agent.execute(
        req.message(),
        AgentExecutionContext.standalone(req.sessionId())
    );
    return Map.of(
        "response", response.content(),
        "sessionId", req.sessionId()
    );
}

// First call: "I ordered item #12345 last week" - saved to session
// Second call: "Can you check the shipping status?" - session loaded, agent remembers
// Third call: "I want to return it" - steering rule "return" fires, adds policy instructions
```

### Example 4: Data Pipeline with Parallel Tool Execution

**Python Strands:**

```python
from strands import Agent
from strands_tools import use_aws, python_repl, file_write

agent = Agent(
    system_prompt="You are a data pipeline agent...",
    tools=[use_aws, python_repl, file_write]
)

agent("Fetch sales data from S3, analyze trends with pandas, and write a CSV report")
```

**Java / Spring AI:**

```java
// The model can request multiple tools in one turn.
// For example, it might ask for S3 data and DynamoDB data simultaneously.
// The SDK handles this via ToolRegistry.executeToolsParallel().

// In your LoopModelClient, when the model requests parallel tools:
if (modelResponse.hasMultipleToolUses()) {
    List<ToolCallRequest> requests = modelResponse.getToolUses().stream()
        .map(tu -> new ToolCallRequest(tu.getName(), tu.getArguments()))
        .toList();
    return ModelTurnResponse.multiToolCall(requests);
}

// The execution loop automatically detects multiToolCall responses
// and runs all tools concurrently via CompletableFuture.
```

### Example 5: Plugin-Based Agent with Skills

**Python Strands:**

```python
from strands import Agent
from strands.plugins import Plugin

class SecurityPlugin(Plugin):
    def init_agent(self, agent):
        # Add security-focused hooks and tools
        pass

agent = Agent(
    system_prompt="You are a security-aware assistant...",
    plugins=[SecurityPlugin()]
)
```

**Java / Spring AI:**

```java
// Security plugin with hooks and skills
@Component
public class SecurityPlugin implements AgentPlugin {

    @Override
    public void init(AiAgent agent) {
        // Plugin initialization
    }

    @OnHook(AgentHookEvent.BeforeToolCall.class)
    public void auditToolCall(AgentHookEvent event) {
        var e = (AgentHookEvent.BeforeToolCall) event;
        // Log every tool call for security audit
        SecurityAuditLog.record(e.toolName(), e.arguments());
    }
}

// Skills for the security domain
@Bean
public SkillsPlugin securitySkills() {
    return new SkillsPlugin(List.of(
        new Skill("input-validation", "Input validation",
            "Always validate and sanitize user inputs before processing.",
            List.of("validator")),
        new Skill("data-classification", "Data classification",
            "Classify data sensitivity before storing or transmitting.",
            List.of("classifier"))
    ));
}
```

---

## Composition: What Lives Where

This SDK only implements the **model-driven loop**. The rest of the agent platform comes from other Spring AI modules:

| Concern | Python Strands | Java / Spring AI |
|---------|---------------|-----------------|
| Core execution loop | `ai.agent` + `strands.event_loop` | **This SDK** (`AiAgent`, `AgentExecutionLoop`) |
| HTTP runtime, health, rate limiting | Host app (Flask, FastAPI, etc.) | **spring-ai-agentcore** (`@AgentCoreInvocation`, `/ping`) |
| Browser, code interpreter tools | `strands_tools` package | **spring-ai-agentcore** modules (as `ToolCallbackProvider` beans) |
| Multi-agent (Swarm, Graph) | `strands.multiagent` | **spring-ai-a2a** + your orchestration code |
| A2A protocol | `ai.agent.a2a_agent` | **spring-ai-a2a** library |
| MCP client/server | `strands.tools.mcp` | **spring-ai-mcp** (Spring AI native) |
| Bidirectional streaming (voice) | `strands.experimental.bidi` | Not applicable for Java backend services |

In short: **Python Strands** = core loop + large ecosystem in one package. **This SDK** = the loop on the JVM. **AgentCore, A2A, MCP, and your app** supply HTTP, multi-agent coordination, and specialized tools.

---

## Related Reading

- Module [README.md](../README.md) - overview, Maven, and configuration
- [Tutorial](tutorial.md) - step-by-step usage of every feature
- [Developer guide](developer-guide.md) - architecture and property reference
- [Strands Agents SDK (Python)](https://github.com/strands-agents/sdk-python) - Python source code
- [Strands Agents documentation](https://strandsagents.com/) - official docs
- [spring-ai-a2a](https://github.com/spring-ai-community/spring-ai-a2a) - Agent-to-Agent protocol for Java
- [spring-ai-agentcore](https://github.com/spring-ai-community/spring-ai-agentcore) - AgentCore runtime for Java
- [Spring AI documentation](https://docs.spring.io/spring-ai/reference/)
