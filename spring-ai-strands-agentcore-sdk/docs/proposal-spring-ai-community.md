# [Proposal] spring-ai-strands-agentcore-sdk - Strands-style model-driven agent execution for Spring AI

## Project Name

spring-ai-strands-agentcore-sdk

## Project Description

spring-ai-strands-agentcore-sdk is a Spring Boot module that brings the [Strands Agents SDK](https://github.com/strands-agents/sdk-python) model-driven execution pattern to Java. Instead of hardcoding agent workflows as DAGs or state machines, the foundation model autonomously controls planning, tool selection, and multi-step execution in a loop - the same approach used by the Python and TypeScript Strands SDKs.

The module composes with the existing [spring-ai-agentcore](https://github.com/spring-ai-community/spring-ai-agentcore) ecosystem. It does not duplicate runtime infrastructure, memory, browser, code interpreter, or A2A support. It adds the execution engine layer that drives the model-tool reasoning loop.

## Problem it solves

The Strands Agents SDK is one of the most popular open-source agent frameworks, but it only supports Python and TypeScript. Enterprise teams running Spring Boot backends need the same model-driven execution pattern without maintaining a separate Python service for every agent.

Spring AI provides ChatClient and tool calling, but no built-in model-driven execution loop with:

- Iterative model-tool cycles where the model decides when to stop
- Per-iteration observability (reasoning traces, metrics, tracing spans)
- Hook system for before/after events at each stage of the loop
- Plugin system for composable agent extensions
- Conversation management (sliding window, token counting)
- Session persistence across invocations
- Security hardening (tool argument limits, timeouts, rate limiting, output sanitization)
- Parallel tool execution when the model requests multiple tools in one turn
- Steering rules for conditional prompt injection
- Skills (reusable prompt + tool combinations)
- Directory-based tool loading with hot-reload
- Dynamic MCP server connections at runtime

This library provides all of the above as a single module that works alongside spring-ai-agentcore, spring-ai-a2a, and spring-ai-mcp.

## Working proof of concept

The library is fully implemented and tested. It includes:

**Core execution engine:**
- `StrandsAgent` - main entry point with `execute()` (sync) and `executeStreaming()` (`Flux<String>`)
- `StrandsExecutionLoop` - iterative model-tool reasoning loop with max iterations, retry logic, and conversation management
- `LoopModelClient` - interface for model abstraction (wraps Spring AI ChatClient)
- `ModelTurnResponse` - supports single tool calls, parallel tool calls, and final answers

**Tool system:**
- `ToolBridge` - discovers all `ToolCallbackProvider` beans from the Spring context
- `ToolRegistry` - executes tools with argument size limits, timeouts, rate limiting, and safe error handling
- `DirectoryToolLoader` - watches a directory for JSON tool definitions with hot-reload via WatchService
- `DynamicMcpToolConnector` - connects to MCP servers at runtime (stdio/SSE)
- Parallel tool execution via `CompletableFuture`

**Hook and plugin system:**
- `HookRegistry` with 6 event types: BeforeInvocation, BeforeModelCall, AfterModelCall, BeforeToolCall, AfterToolCall, AfterInvocation
- `@OnHook` annotation for declarative hook registration
- `HookAnnotationProcessor` for scanning beans
- `StrandsPlugin` interface with `PluginScanner` for auto-discovery of hooks and tool providers
- `SkillsPlugin` for reusable prompt + tool combinations
- `SteeringAdvisor` for conditional prompt injection based on keyword matching

**State management:**
- `SlidingWindowConversationManager` - keeps last N messages
- `TokenCountConversationManager` - fits messages within token budget
- `InMemorySessionManager` - ConcurrentHashMap-backed (development)
- `FileSessionManager` - JSON file persistence with file locking (production)
- `SessionManager` interface for custom backends (DynamoDB, Redis, etc.)

**Observability:**
- `StrandsObservability` with Micrometer metrics and Observation API tracing spans
- `ReasoningTrace` capturing every iteration with tool names, inputs, outputs, and timing
- Structured logging at DEBUG and INFO levels
- Output sanitization (AWS keys, JWTs, emails) and trace truncation

**Security:**
- Tool argument size limits (`max-tool-argument-bytes`)
- Per-tool execution timeouts (`tool-timeout-seconds`)
- Tool rate limiting per loop (`tool-rate-limit`)
- Tool name validation (`[a-zA-Z0-9_-]`)
- SSRF prevention (system-prompt-resource rejects http/https URLs)
- Actuator property hiding (`@JsonIgnore` on sensitive fields)
- Immutable `StrandsExecutionContext` with headers excluded from toString/equals/hashCode

**Configuration:**
- `StrandsAgentProperties` under `strands.agent.*` with Jakarta Validation
- `StrandsAgentAutoConfiguration` with `@ConditionalOnProperty` and `@ConditionalOnMissingBean`

**Testing:**
- 191 tests (unit, property-based with jqwik, integration, security)
- Property-based tests for configuration round-trip, execution loop correctness, tool discovery, streaming behavior
- Security tests for session isolation, prompt injection, header leakage, SSRF prevention

**Documentation:**
- README with feature parity table vs Python Strands SDK
- Developer guide with full configuration reference
- Tutorial (21 parts) covering every feature with working code examples
- Python-to-Java migration guide with side-by-side comparisons
- 7 working example applications

## Existing Repository URL with POC implementation

https://github.com/vaquarkhan/spring-ai-strands-agentcore-sdk

## Integration with Spring AI

spring-ai-strands-agentcore-sdk integrates directly with Spring AI's existing APIs without modifying or replacing any core components:

**ToolCallbackProvider interface** - `ToolBridge` discovers all `ToolCallbackProvider` beans from the Spring context, including those from spring-ai-agentcore (browser, code interpreter), spring-ai-mcp, and custom application tools. No special registration needed.

**ChatClient** - The `LoopModelClient` interface is implemented by wrapping Spring AI's `ChatClient`. Any model provider supported by Spring AI (Bedrock, OpenAI, Ollama, etc.) works through this abstraction.

**Advisor pattern** - The `Advisor` interface follows the same enrichment pattern as Spring AI advisors. Memory advisors from spring-ai-agentcore-memory compose directly with the Strands execution loop.

**spring-ai-agentcore composition** - The `StrandsAgent` bean is callable from `@AgentCoreInvocation`-annotated methods. Session context propagates from `AgentCoreContext` to `StrandsExecutionContext`. SSE streaming works through the existing runtime starter.

**spring-ai-a2a composition** - The `StrandsAgent` can serve as the executor behind A2A protocol endpoints for multi-agent orchestration.

**spring-ai-mcp composition** - MCP tools registered as `ToolCallbackProvider` beans are automatically discovered by `ToolBridge` and available in the Strands execution loop.

## Existing Documentation

- [README](https://github.com/vaquarkhan/spring-ai-strands-agentcore-sdk/blob/main/spring-ai-strands-agentcore-sdk/README.md)
- [Tutorial](https://github.com/vaquarkhan/spring-ai-strands-agentcore-sdk/blob/main/spring-ai-strands-agentcore-sdk/docs/tutorial.md)
- [Developer Guide](https://github.com/vaquarkhan/spring-ai-strands-agentcore-sdk/blob/main/spring-ai-strands-agentcore-sdk/docs/developer-guide.md)
- [Python vs Java Comparison](https://github.com/vaquarkhan/spring-ai-strands-agentcore-sdk/blob/main/spring-ai-strands-agentcore-sdk/docs/strands-python-vs-spring-ai.md)

## Development Team

Vaquar Khan (@vaquarkhan)

## Project Requirements

- [x] Has working proof of concept that demonstrates integration with Spring AI
- [x] Includes unit and integration tests
- [x] Uses or will use Apache 2 license
- [x] All development will occur in a public repository
- [x] Agrees to follow the Spring AI code of conduct
- [x] Will provide clear contribution guidelines
- [x] Will follow semantic versioning (MAJOR.MINOR.PATCH)

## Preferred Packaging Method

Using GitHub's process with io.github.spring-ai-community as the groupId

## Commercial Relationship

- [ ] This project has commercial ownership/control (single-vendor)

## Additional Information

This module is designed as a new module within the spring-ai-agentcore ecosystem, following the same BOM and modularity patterns. It can be added to the existing [spring-ai-agentcore](https://github.com/spring-ai-community/spring-ai-agentcore) repository as `spring-ai-strands-agentcore-sdk` alongside the existing runtime-starter, memory, browser, code-interpreter, and artifact-store modules.

The Strands Agents SDK is widely adopted in the Python ecosystem and closely associated with AWS. Bringing this pattern to Spring AI fills a gap for enterprise Java teams that want model-driven agent execution without maintaining Python services.
