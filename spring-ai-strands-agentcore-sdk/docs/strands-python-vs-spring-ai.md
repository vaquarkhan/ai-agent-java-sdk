# Strands in Python vs Strands-style on Java / Spring AI

_Author: Vaquar Khan_

The [Strands Agents SDK](https://strandsagents.com/) ships first-class support for **Python** and **TypeScript**. This repository’s **spring-ai-strands-agentcore-sdk** implements the same **model-driven** pattern on the JVM: model plus tools in a loop until a final answer, integrated with **Spring Boot** and **Spring AI**.

The table below is a **side-by-side** map from the Python (and TS) mental model to the Java types and configuration used here.

| Topic | Strands (Python) | Strands in Spring AI (this SDK) |
|--------|------------------|----------------------------------|
| **Language / runtime** | Python 3.x, `pip` packages | Java 17+, Maven/Gradle, Spring Boot |
| **Agent entry point** | Strands `Agent` (and related APIs in the Python SDK) | `StrandsAgent` |
| **Execution loop** | Strands agent loop (model ↔ tools) | `StrandsExecutionLoop` |
| **Model invocation** | Bedrock / provider clients in Python | `LoopModelClient` (typically implemented with Spring AI `ChatClient` or your provider client) |
| **System prompt** | Constructor / builder / config in code | `strands.agent.system-prompt` or `system-prompt-resource` |
| **Tools** | Python functions, tool decorators, Strands tool ecosystem | `ToolCallback` implementations exposed via `ToolCallbackProvider` beans |
| **Tool registration** | Register with the Strands agent object | `ToolBridge.discoverTools` → `ToolRegistry` from all `ToolCallbackProvider` beans; optional glob include/exclude |
| **MCP (external tools)** | Python MCP clients and Strands integrations | Spring AI MCP client starter → MCP tools as `ToolCallbackProvider`; same discovery path as other tools |
| **Session / user context** | Python context objects / runtime | `StrandsExecutionContext`; `from(AgentCoreContext)` when using AgentCore |
| **Prompt enrichment (memory, RAG)** | Hooks or middleware in the Python stack | `Advisor` beans applied before the loop |
| **Streaming** | Async generators / Python streaming patterns | `executeStreaming` → `reactor.core.publisher.Flux<String>` |
| **Configuration** | Code + environment variables | `application.yml` / `application.properties` under `strands.agent.*` |
| **Observability** | Strands traces / OpenTelemetry in Python examples | `StrandsObservability`, Micrometer metrics, optional observations |
| **Multi-agent / A2A / graphs** | Strands samples and ecosystem (Python/TS) | Not in this JAR - use **spring-ai-a2a**, orchestration, or custom workflows that call **`StrandsAgent`** (see below) |
| **Agent HTTP/runtime, browser/code tools** | Strands + host app in Python | Not reimplemented here - use **spring-ai-agentcore** and related starters; tools surface as **`ToolCallbackProvider`** beans this SDK discovers (see below) |

## Composition: AgentCore and the rest of Spring AI (not duplicated in this JAR)

**spring-ai-strands-agentcore-sdk** only implements the **model-driven loop** (model ↔ tools until completion). That is deliberate: **Spring AI already provides** the surrounding agent platform pieces. You **compose** this module with them instead of expecting every Python Strands feature inside one artifact.

| Concern | Where it lives in Spring AI | How it works with this SDK |
|--------|-----------------------------|----------------------------|
| **HTTP/runtime for agents**, sessions, AgentCore-style invocation | [**spring-ai-agentcore**](https://github.com/spring-ai-community/spring-ai-agentcore) | Your app exposes endpoints or uses `@AgentCoreInvocation` (and related patterns) from AgentCore; **`StrandsAgent`** is a bean you call from that runtime. |
| **Browser tools, code interpreter, and similar** | AgentCore and companion starters that register **`ToolCallbackProvider`** beans | Those tools are **not** copied into this JAR. They appear as normal Spring AI tools; **`ToolBridge`** discovers them alongside your own `ToolCallbackProvider` beans. Use **`tool-discovery`** globs if you need to restrict which tools the Strands loop sees. |
| **Multi-agent patterns** (swarms, graphs, workflow-style routing) | [**spring-ai-a2a**](https://github.com/spring-ai-community/spring-ai-a2a) (Agent-to-Agent), or **your** orchestration layer | This module does **not** implement swarm/graph orchestration. Use A2A or custom code that invokes one or more **`StrandsAgent`** instances as **building blocks**, similar to composing Python Strands with external workflow engines. |
| **MCP** | **spring-ai-mcp** (client/server) | MCP-backed tools become `ToolCallbackProvider` beans; same discovery path as above. |

In short: **Python Strands** = core loop + a large ecosystem; **this SDK** = the **loop** on the JVM; **AgentCore, A2A, MCP, and your app** supply HTTP, multi-agent coordination, and specialized tools.

## Related reading

- Module [README.md](../README.md) - overview, Maven, and configuration
- [Tutorial](tutorial.md) - step-by-step usage
- [Developer guide](developer-guide.md) - architecture and property reference
- [Strands Agents SDK](https://strandsagents.com/) - Python / TypeScript reference
- [Spring AI documentation](https://docs.spring.io/spring-ai/reference/)
