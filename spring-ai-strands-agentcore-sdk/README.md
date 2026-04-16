# spring-ai-strands-agentcore-sdk

Strands-style **model-driven** agent execution for [Spring AI](https://spring.io/projects/spring-ai) and the [spring-ai-agentcore](https://github.com/spring-ai-community/spring-ai-agentcore) ecosystem. This module brings the same architectural idea as the open-source **Strands Agents SDK** (Python and TypeScript) to Java: the foundation model plans, chooses tools, and iterates until it produces a final answer, instead of the application hard-coding workflows or DAGs.

---

## What “Strands” means (the model-driven pattern)

The [Strands Agents SDK](https://strandsagents.com/) (Python and TypeScript, open source, used widely with Amazon Bedrock and other providers) is built around a **model-driven** loop:

- You supply a **model**, a **system prompt**, and **tools**.
- The model **reasons** about the task, may **call tools**, receives **tool results**, and **repeats** until it returns a **final answer** (or a guardrail such as max iterations stops the loop).

That differs from frameworks where you encode fixed step sequences or state machines for every branch. In a model-driven design, the model adapts when inputs change, tools fail, or users ask unexpected questions—within the boundaries you set (prompt, tool set, limits, security).

Public background on that philosophy and production use cases:

- [Strands Agents and the model-driven approach](https://aws.amazon.com/blogs/opensource/strands-agents-and-the-model-driven-approach/) (AWS Open Source Blog)
- [Strands Agents SDK: architectures and observability](https://aws.amazon.com/blogs/machine-learning/strands-agents-sdk-a-technical-deep-dive-into-agent-architectures-and-observability/) (AWS Machine Learning Blog)
- [Introducing Strands Agents](https://aws.amazon.com/blogs/opensource/introducing-strands-agents-an-open-source-ai-agents-sdk/) (AWS Open Source Blog)

Typical **benefits** teams cite for this pattern (in any language):

- **Less brittle orchestration**: fewer hand-maintained “if step 3 fails, go to step 7” graphs.
- **Faster iteration**: adjust prompts and tools rather than rewiring control flow for every new scenario.
- **Clear extension model**: new capabilities are often **new tools** plus prompt tuning, not new orchestration code paths.
- **Operational visibility**: multi-step loops benefit from **traces, metrics, and structured logs** so you can see what the model tried and what tools ran.

---

## Why a Java / Spring AI SDK?

The official Strands SDKs today target **Python** and **TypeScript**. Many enterprise systems that use **Spring Boot**, **JVM** libraries, and **Spring AI** (including Amazon Bedrock and other providers via Spring AI abstractions) need the **same execution model** without maintaining a parallel Python service for every agent.

This module exists to:

1. **Implement the Strands-style execution loop** in Java: model call → optional tool execution → feed results back → repeat until completion or limits.
2. **Reuse Spring AI** primitives: `ChatClient`, `ToolCallback`, `ToolCallbackProvider`, advisors, and optional MCP-related providers—rather than inventing a second tool stack.
3. **Compose with spring-ai-agentcore**: runtime invocation (`@AgentCoreInvocation`), memory advisors, browser and code-interpreter tools as `ToolCallbackProvider` beans, SSE streaming, and session context—without duplicating what those modules already do.

So: **Strands semantics on the loop**; **Spring AI and AgentCore for wiring, tools, memory, and HTTP**.

---

## Strands (Python SDK) vs Strands-style on Spring AI (this module)

Same **model-driven loop** idea; different **language runtime** and **integration surface**. When moving from the official [Strands Python SDK](https://strandsagents.com/) (or TypeScript) to Java, use the side-by-side table here:

**[Strands in Python vs Strands in Java / Spring AI](docs/strands-python-vs-spring-ai.md)**

---

## What this module provides

| Area | What you get |
|------|----------------|
| **Execution engine** | A **model-driven loop** (`StrandsExecutionLoop`): the model decides tool use; the loop executes tools and returns results to the model until a final answer or termination. |
| **Agent API** | **`StrandsAgent`**: synchronous `execute(...)` and streaming `executeStreaming(...)` (`Flux<String>`). |
| **Tools** | **`ToolBridge`** discovers tools from all **`ToolCallbackProvider`** beans; **`ToolRegistry`** executes tools with size limits, timeouts, optional rate limits, and safe error surfaces for the model. |
| **Configuration** | **`StrandsAgentProperties`** under `strands.agent.*` with validation (required model fields, max iterations, tool discovery include/exclude globs, security-related limits). |
| **Context** | **`StrandsExecutionContext`**: session and user identity, with headers excluded from `equals`/`hashCode`/`toString` to reduce accidental leakage in logs. |
| **Observability** | **`StrandsObservability`**: reasoning trace structure, Micrometer metrics hooks, optional tracing observations, configurable sanitization and trace truncation for tool output in traces/logs. |
| **Streaming** | Token-style streaming path integrated with the loop (pause/resume around tool boundaries as designed for AgentCore SSE usage). |
| **Auto-configuration** | **`StrandsAgentAutoConfiguration`** registers beans when `strands.agent.enabled` is true (default), with hooks for overrides via `@ConditionalOnMissingBean`. |

---

## What stays in other libraries (not duplicated here)

To avoid overlap with existing **Spring AI** modules, this JAR **does not** reimplement the full agent platform—only the **Strands-style tool loop**.

- **spring-ai-agentcore** — Agent HTTP/runtime, `@AgentCoreInvocation`, health, rate limiting, artifact store, authentication model, BOM alignment, and **tooling such as browser or code-interpreter** when exposed as **`ToolCallbackProvider`** beans from AgentCore starters. This SDK **consumes** those tools via `ToolBridge`; it does **not** embed AgentCore or duplicate its endpoints.
- **spring-ai-a2a** — Agent-to-Agent protocol and **multi-agent** patterns (swarm-, graph-, or workflow-style coordination). Use A2A, orchestration, or custom workflows that call **`StrandsAgent`** as a building block; those patterns are **out of scope** for this module alone.
- **spring-ai-mcp** — MCP client/server; remote tools still surface through Spring AI **`ToolCallbackProvider`** where configured.

You combine **this module** with AgentCore, A2A, MCP, and your application code when you need the full picture—similar to how **Python Strands** composes its core loop with a larger ecosystem.

---

## Mapping: Strands concepts (Python/TS) → this Java module

| Strands-style concept | In this SDK |
|------------------------|-------------|
| Agent loop (model ↔ tools until done) | `StrandsExecutionLoop` |
| System prompt + user messages | `StrandsAgent` + `StrandsAgentProperties` (`system-prompt` / `system-prompt-resource`) |
| Tool registration and execution | `ToolBridge`, `ToolRegistry`, Spring AI `ToolCallback` / `ToolCallbackProvider` |
| Session / user for memory and runtime | `StrandsExecutionContext` (aligned with AgentCore context in integrated apps) |
| Traces and metrics for the loop | `StrandsObservability`, Micrometer, optional observations |
| Streaming UX | `executeStreaming` → `Flux<String>` for integration with SSE-style runtimes |

Advanced multi-agent patterns (swarm, graph, workflow) from Strands Python docs are **not implemented inside this JAR**. On the Spring AI stack they belong in **spring-ai-a2a**, application orchestration, or workflows that invoke **`StrandsAgent`**. AgentCore HTTP/runtime and browser/code-style tools come from **spring-ai-agentcore** (and starters) as **`ToolCallbackProvider`** beans—see [strands-python-vs-spring-ai.md](docs/strands-python-vs-spring-ai.md#composition-agentcore-and-the-rest-of-spring-ai-not-duplicated-in-this-jar).

---

## Requirements

- **Java 17+**
- **Spring Boot 3.x** (as used by spring-ai-agentcore)
- **Spring AI** (via BOM; this repo’s parent imports `spring-ai-bom`, e.g. `1.0.0-M6`—adjust to a GA version when you align with your stack)

---

## Maven dependency

Add the module (coordinates match your parent `groupId` / `version`):

```xml
<dependency>
    <groupId>com.example.spring.ai</groupId>
    <artifactId>spring-ai-strands-agentcore-sdk</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Auto-configuration registers a **`NoopLoopModelClient`** if you do not define your own `LoopModelClient`. For real model calls you must provide a **`LoopModelClient` bean** (typically implemented with Spring AI `ChatClient` or your provider’s client) wired to your Bedrock, OpenAI, Ollama, or other runtime. The exact provider setup follows Spring AI and AgentCore documentation.

---

## Minimal configuration (`application.yml`)

```yaml
strands:
  agent:
    enabled: true
    model-provider: bedrock   # example: align with your Spring AI setup
    model-id: "anthropic.claude-3-5-sonnet-20240620-v1:0"
    system-prompt: "You are a helpful assistant. Use tools when they improve the answer."
    max-iterations: 25
    tool-discovery:
      enabled: true
      include-patterns: []
      exclude-patterns: []
    security:
      max-tool-argument-bytes: 65536
      tool-timeout-seconds: 60
      tool-rate-limit: 0
      sanitize-tool-output: false
      trace-max-output-length: 1024
      trace-include-tool-data: false
```

**Notes:**

- `system-prompt` and `system-prompt-resource` are **mutually exclusive**.
- `system-prompt-resource` must not be an `http://` or `https://` URL (classpath and file resources are intended).
- Sensitive fields are marked to avoid accidental exposure via configuration endpoints in line with security guidance.

---

## Primary Java types

- `com.example.spring.ai.strands.agent.StrandsAgent` – entry point
- `com.example.spring.ai.strands.agent.config.StrandsAgentProperties` – `strands.agent.*` binding
- `com.example.spring.ai.strands.agent.execution.StrandsExecutionLoop` – core loop
- `com.example.spring.ai.strands.agent.tool.ToolBridge` / `ToolRegistry` – tool discovery and execution
- `com.example.spring.ai.strands.agent.observability.StrandsObservability` – traces and metrics
- `com.example.spring.ai.strands.agent.api.Advisor` – optional prompt enrichment (e.g. memory advisors from AgentCore)

---

## Benefits summary

- **Same mental model as Strands (Python/TS)**: model-led planning and tool use in a tight loop.
- **Native Spring Boot integration**: properties, validation, auto-configuration, Actuator-friendly metrics.
- **Interop with Spring AI tools**: one bridge from `ToolCallbackProvider` (including browser, code interpreter, MCP) into the loop.
- **Fits AgentCore**: session context, streaming, and advisors compose with existing starters instead of forking a separate agent framework.

---

## Further reading

- [Examples (Python quickstart port)](examples/README.md)
- [Strands Python vs Spring AI (side-by-side)](docs/strands-python-vs-spring-ai.md)
- [Tutorial (step-by-step)](docs/tutorial.md) — tools, MCP, streaming, production tuning
- [Developer guide (reference)](docs/developer-guide.md) — architecture, configuration matrix, observability
- [Spring AI documentation](https://docs.spring.io/spring-ai/reference/)
- [spring-ai-agentcore](https://github.com/spring-ai-community/spring-ai-agentcore)
- [Strands Agents SDK](https://strandsagents.com/) (Python / TypeScript reference for the original pattern and tooling ecosystem)

---

## Feature Parity with Python Strands SDK

This module implements the core patterns from the [Python Strands Agents SDK](https://github.com/strands-agents/sdk-python), adapted for the Java / Spring AI ecosystem. The table below shows the current implementation status.

| Python Strands SDK Feature | Java / Spring AI Equivalent | Status |
|---|---|---|
| Model-driven execution loop | `StrandsExecutionLoop` | Implemented |
| Agent (model + tools + prompt) | `StrandsAgent` | Implemented |
| Tool registration and execution | `ToolBridge`, `ToolRegistry`, `ToolCallbackProvider` | Implemented |
| Conversation manager (sliding window) | `SlidingWindowConversationManager` | Implemented |
| Conversation manager (token count) | `TokenCountConversationManager` | Implemented |
| Session manager (in-memory) | `InMemorySessionManager` | Implemented |
| Session manager (file-based) | `FileSessionManager` | Implemented |
| Session manager (DynamoDB) | Extension point (requires AWS SDK) | Documented |
| Hook system (before/after events) | `HookRegistry`, `StrandsHook`, `StrandsHookEvent` | Implemented |
| Hook annotation (`@OnHook`) | `@OnHook` + `HookAnnotationProcessor` | Implemented |
| Plugin system | `StrandsPlugin`, `PluginScanner` | Implemented |
| Skills (reusable prompt + tool combos) | `SkillsPlugin`, `Skill` | Implemented |
| Steering rules | `SteeringRule`, `SteeringAdvisor` | Implemented |
| Tool loading from directory | `DirectoryToolLoader` | Implemented |
| Dynamic MCP client | `DynamicMcpToolConnector` | Implemented |
| Streaming (SSE) | `executeStreaming` / `Flux<String>` | Implemented |
| Observability (traces, metrics) | `StrandsObservability`, Micrometer | Implemented |
| Advisors (memory, RAG enrichment) | `Advisor` interface | Implemented |
| Multi-agent (swarm, graph, workflow) | Out of scope (use spring-ai-a2a) | N/A |

For a detailed side-by-side comparison, see [strands-python-vs-spring-ai.md](docs/strands-python-vs-spring-ai.md).

---

## Author

**Vaquar Khan**

---

## License

Follow the license of the enclosing project / repository this artifact ships with.
