# AI Agent Java SDK (`ai-agent-java-sdk-core`)

**AI Agent Java SDK** provides **model-driven** agent execution for [Spring AI](https://spring.io/projects/spring-ai) and the [spring-ai-agentcore](https://github.com/spring-ai-community/spring-ai-agentcore) ecosystem. The design is **inspired by the mental model of the [AWS Strands Agents Python SDK](https://strandsagents.com/)** (and related Python/TypeScript materials): the foundation model plans, chooses tools, and iterates until it produces a final answer, instead of the application hard-coding workflows or DAGs. This project is **independent** and is **not** the official “Strands for Java” product.

---

## Background: the model-driven pattern (AWS Strands Python SDK)

The [AWS Strands Agents Python SDK](https://strandsagents.com/) ecosystem describes a **model-driven** loop that many teams also want on the JVM:

- You supply a **model**, a **system prompt**, and **tools**.
- The model **reasons** about the task, may **call tools**, receives **tool results**, and **repeats** until it returns a **final answer** (or a guardrail such as max iterations stops the loop).

That differs from frameworks where you encode fixed step sequences or state machines for every branch. In a model-driven design, the model adapts when inputs change, tools fail, or users ask unexpected questions-within the boundaries you set (prompt, tool set, limits, security).

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

1. **Implement that model-driven execution loop** in Java: model call → optional tool execution → feed results back → repeat until completion or limits.
2. **Reuse Spring AI** primitives: `ChatClient`, `ToolCallback`, `ToolCallbackProvider`, advisors, and optional MCP-related providers-rather than inventing a second tool stack.
3. **Compose with spring-ai-agentcore**: runtime invocation (`@AgentCoreInvocation`), memory advisors, browser and code-interpreter tools as `ToolCallbackProvider` beans, SSE streaming, and session context-without duplicating what those modules already do.

So: **the same loop semantics in spirit**; **Spring AI and AgentCore for wiring, tools, memory, and HTTP**.

---

## AWS Strands (Python) vs AI Agent Java SDK (this module)

Same **model-driven loop** idea; different **language runtime** and **integration surface**. When comparing the official [Strands Python SDK](https://strandsagents.com/) to this library, use the side-by-side table here:

**[Python (AWS Strands) vs Java (AI Agent Java SDK)](docs/python-strands-vs-ai-agent-java.md)**

---

## What this module provides

| Area | What you get |
|------|----------------|
| **Execution engine** | A **model-driven loop** (`AgentExecutionLoop`): the model decides tool use; the loop executes tools and returns results to the model until a final answer or termination. |
| **Agent API** | **`AiAgent`**: synchronous `execute(...)` and streaming `executeStreaming(...)` (`Flux<String>`). |
| **Tools** | **`ToolBridge`** discovers tools from all **`ToolCallbackProvider`** beans; **`ToolRegistry`** executes tools with size limits, timeouts, optional rate limits, and safe error surfaces for the model. |
| **Configuration** | **`AiAgentProperties`** under `ai.agent.*` with validation (required model fields, max iterations, tool discovery include/exclude globs, security-related limits). |
| **Context** | **`AgentExecutionContext`**: session and user identity, with headers excluded from `equals`/`hashCode`/`toString` to reduce accidental leakage in logs. |
| **Observability** | **`AgentObservability`**: reasoning trace structure, Micrometer metrics hooks, optional tracing observations, configurable sanitization and trace truncation for tool output in traces/logs. |
| **Streaming** | Token-style streaming path integrated with the loop (pause/resume around tool boundaries as designed for AgentCore SSE usage). |
| **Auto-configuration** | **`AiAgentAutoConfiguration`** registers beans when `ai.agent.enabled` is true (default), with hooks for overrides via `@ConditionalOnMissingBean`. |

---

## What stays in other libraries (not duplicated here)

To avoid overlap with existing **Spring AI** modules, this JAR **does not** reimplement the full agent platform-only the **model-driven tool loop** described above.

- **spring-ai-agentcore** - Agent HTTP/runtime, `@AgentCoreInvocation`, health, rate limiting, artifact store, authentication model, BOM alignment, and **tooling such as browser or code-interpreter** when exposed as **`ToolCallbackProvider`** beans from AgentCore starters. This SDK **consumes** those tools via `ToolBridge`; it does **not** embed AgentCore or duplicate its endpoints.
- **spring-ai-a2a** - Agent-to-Agent protocol and **multi-agent** patterns (swarm-, graph-, or workflow-style coordination). Use A2A, orchestration, or custom workflows that call **`AiAgent`** as a building block; those patterns are **out of scope** for this module alone.
- **spring-ai-mcp** - MCP client/server; remote tools still surface through Spring AI **`ToolCallbackProvider`** where configured.

You combine **this module** with AgentCore, A2A, MCP, and your application code when you need the full picture-similar to how teams compose the **Python Strands** core loop with a larger ecosystem.

---

## Mapping: Python Strands concepts → AI Agent Java SDK

| Concept (Python Strands) | In AI Agent Java SDK |
|------------------------|-------------|
| Agent loop (model ↔ tools until done) | `AgentExecutionLoop` |
| System prompt + user messages | `AiAgent` + `AiAgentProperties` (`system-prompt` / `system-prompt-resource`) |
| Tool registration and execution | `ToolBridge`, `ToolRegistry`, Spring AI `ToolCallback` / `ToolCallbackProvider` |
| Session / user for memory and runtime | `AgentExecutionContext` (aligned with AgentCore context in integrated apps) |
| Traces and metrics for the loop | `AgentObservability`, Micrometer, optional observations |
| Streaming UX | `executeStreaming` → `Flux<String>` for integration with SSE-style runtimes |

Advanced multi-agent patterns (swarm, graph, workflow) from Strands Python docs are **not implemented inside this JAR**. On the Spring AI stack they belong in **spring-ai-a2a**, application orchestration, or workflows that invoke **`AiAgent`**. AgentCore HTTP/runtime and browser/code-style tools come from **spring-ai-agentcore** (and starters) as **`ToolCallbackProvider`** beans-see [python-strands-vs-ai-agent-java.md](docs/python-strands-vs-ai-agent-java.md#composition-agentcore-and-the-rest-of-spring-ai-not-duplicated-in-this-jar).

---

## Requirements

- **Java 17+**
- **Spring Boot 3.x** (as used by spring-ai-agentcore)
- **Spring AI** (via BOM; this repo’s parent imports `spring-ai-bom`, e.g. `1.0.0-M6`-adjust to a GA version when you align with your stack)

---

## Maven dependency

Add the module (coordinates match your parent `groupId` / `version`):

```xml
<dependency>
    <groupId>com.example.spring.ai</groupId>
    <artifactId>ai-agent-java-sdk-core</artifactId>
    <version>0.1.0-SNAPSHOT</version>
</dependency>
```

Auto-configuration registers a **`NoopLoopModelClient`** if you do not define your own `LoopModelClient`. For real model calls you must provide a **`LoopModelClient` bean** (typically implemented with Spring AI `ChatClient` or your provider’s client) wired to your Bedrock, OpenAI, Ollama, or other runtime. The exact provider setup follows Spring AI and AgentCore documentation.

---

## Minimal configuration (`application.yml`)

```yaml
ai:
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

- `com.example.spring.ai.agent.AiAgent` - entry point
- `com.example.spring.ai.agent.config.AiAgentProperties` - `ai.agent.*` binding
- `com.example.spring.ai.agent.execution.AgentExecutionLoop` - core loop
- `com.example.spring.ai.agent.tool.ToolBridge` / `ToolRegistry` - tool discovery and execution
- `com.example.spring.ai.agent.observability.AgentObservability` - traces and metrics
- `com.example.spring.ai.agent.api.Advisor` - optional prompt enrichment (e.g. memory advisors from AgentCore)

---

## Benefits summary

- **Same mental model as the AWS Strands Python SDK (in spirit)**: model-led planning and tool use in a tight loop.
- **Native Spring Boot integration**: properties, validation, auto-configuration, Actuator-friendly metrics.
- **Interop with Spring AI tools**: one bridge from `ToolCallbackProvider` (including browser, code interpreter, MCP) into the loop.
- **Fits AgentCore**: session context, streaming, and advisors compose with existing starters instead of forking a separate agent framework.

---

## Further reading

- [Examples (quickstart and more)](examples/README.md)
- [Python Strands vs AI Agent Java SDK (side-by-side)](docs/python-strands-vs-ai-agent-java.md)
- [Tutorial (step-by-step)](docs/tutorial.md) - tools, MCP, streaming, production tuning
- [Developer guide (reference)](docs/developer-guide.md) - architecture, configuration matrix, observability
- [Spring AI documentation](https://docs.spring.io/spring-ai/reference/)
- [spring-ai-agentcore](https://github.com/spring-ai-community/spring-ai-agentcore)
- [AWS Strands Agents (Python / TypeScript)](https://strandsagents.com/) (reference for the original pattern and tooling ecosystem)

---

## Parity with the Python Strands SDK (through composition)

The Python Strands SDK is **much more than just the execution loop**. The Java/Spring AI approach intentionally splits the full experience across multiple packages:

- **Model providers:** Python ships **13+ providers** built-in. Java does not ship providers inside this module; it delegates to Spring AI model starters. That is why this project includes a bridge so the loop can work with Spring AI models (for example via `ChatModelLoopModelClient` and Spring AI’s `ChatModel` beans), but you still need Spring AI starters on the classpath.
- **Execution control:** Python supports loop control patterns (for example pause/interrupt for human approval) and steering through hook/controller-style APIs. In this Java module, hooks can **observe** and we also added **tool-call policies** (allow/deny/rewrite) so you can block or steer tool execution mid-loop. What’s still missing is the full Python-style `event.interrupt()` style “pause and wait for approval, then resume” contract.
- **Multi-agent orchestration:** Python Graph/Swarm/A2A orchestration is built into the SDK. This Java module focuses on single-agent execution; for Graph/Swarm/A2A style coordination you use Spring AI’s companion packages (for example `spring-ai-a2a`) and/or your own orchestration around `AiAgent`.
- **Tool ecosystem:** Python’s `strands-agents-tools` gives many ready-to-use tools (calculator, HTTP request, shell, file ops, etc.). Java does not include an equivalent pre-built tool bundle in this module. Instead, you provide tools through Spring AI/AgentCore as `ToolCallbackProvider` beans (and MCP tools also surface as those providers), and this SDK discovers them via `ToolBridge` / `ToolRegistry`.
- **Streaming and other SDK layers:** This module provides **streaming** via `AiAgent.executeStreaming` (`Flux<String>`). However, Python features like bidirectional streaming, evaluation SDK, structured output generation, and summarization-based conversation managers are not implemented in this Java module (for conversation we currently rely on window/token-count style managers rather than an LLM-based summarization pipeline).

**Practical meaning:** this repository delivers the **model-driven agent loop engine** for Java, while the “batteries included” experience (models, MCP runtime, multi-agent coordination, and many tools) comes from the broader Spring AI ecosystem you compose in.


For the detailed side-by-side comparison, see [python-strands-vs-ai-agent-java.md](docs/python-strands-vs-ai-agent-java.md).

---

## Author

**Vaquar Khan**

---

## License

Follow the license of the enclosing project / repository this artifact ships with.
