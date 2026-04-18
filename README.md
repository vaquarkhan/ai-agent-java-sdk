# AI Agent Java SDK


###  Spring AI & `ai-agent-java-sdk`: A Complementary Enterprise Architecture

Spring AI provides an exceptional, generalized application framework for Java developers, offering unified APIs across major model providers like OpenAI, Anthropic, and Amazon Bedrock. It excels at bringing standard Spring Boot paradigms-like dependency injection and POJO mapping-into the AI ecosystem. 

However, as organizations transition to autonomous, multi-agent workflows in highly regulated production environments, they often require strict cloud-native optimizations and rigorous security guardrails. Our SDK (`ai-agent-java-sdk`) does not replace Spring AI; rather, it acts as a hyper-specialized enterprise extension. It natively bridges Java applications with powerful AWS infrastructure (like Amazon Bedrock AgentCore) and introduces zero-trust governance layers like MCP-Bastion and AIV-Integrity-Gate.

### ⚙️ Seamless Integration: Powered Internally by Spring AI AgentCore

It is important to note that this SDK does not seek to replace the foundational AgentCore integration. Instead, it works seamlessly with it. Internally, the `ai-agent-java-sdk` is built directly on top of the **Spring AI AgentCore** library, utilizing it for deep alignment and integration. 

By importing the `spring-ai-agentcore-bom` dependency, our SDK natively inherits and utilizes all core Amazon Bedrock AgentCore capabilities-such as fully managed serverless agent runtimes, secure code interpreters, and browser automation tools. What makes this SDK unique is that it wraps these native Spring AI AgentCore components in a model-driven execution loop, adding strict, testable enterprise governance, session management, and robust security layers.

###  Side-by-Side Comparison

| Feature Category | Spring AI (Core Framework) | `ai-agent-java-sdk` (Enterprise Extension) |
| :--- | :--- | :--- |
| **Primary Architecture** | General-purpose framework designed for model portability across diverse LLM providers. | Specialized, cloud-native extension focused on enterprise governance and high-performance execution. |
| **Agent Orchestration** | Relies on developer-defined, prescriptive patterns like Chain, Routing, and Orchestrator-Workers. | Leverages a model-driven approach where the LLM autonomously handles planning, chaining, and tool usage. |
| **Cloud Integration** | Abstracted, generic vector store and model integrations. | Natively optimized for Amazon Bedrock AgentCore, enabling serverless agent runtimes, secure code interpreters, and browser automation. |
| **MCP Security Governance** | Standard MCP tool execution; developers must build their own security layers. | Includes **MCP-Bastion**, providing <5ms local protection against prompt injections, PII leakage, infinite loops, and token budget overruns. |
| **Generated Code Integrity** | Maps structured model outputs directly into POJOs. | Includes **AIV-Integrity-Gate**, which actively validates generated imports against `pom.xml` and enforces strict YAML architectural design rules to prevent supply-chain risks. |

###  Inspired by AWS Strands Agents: The Model-Driven Advantage

This SDK brings the powerful architectural philosophy of the **AWS Strands Agents** framework to the Java ecosystem. 

Many traditional AI frameworks rely heavily on **workflow-driven orchestration**. In these setups, developers must hardcode explicit, prescriptive paths (e.g., Step A -> Step B -> Step C) using complex routing logic or chains. While this offers deterministic control, it often becomes brittle as tasks grow in complexity.

Our SDK adopts the **model-driven approach** championed by Strands. Instead of defining a rigid flowchart, you simply provide the agent with a goal (system prompt) and a set of available tools. The agent loop relies entirely on the LLM's advanced reasoning capabilities to autonomously figure out the optimal sequence of actions, observe results, and dynamically adapt. 

**Key Benefits of the Model-Driven Approach:**
*   **Drastically Reduced Boilerplate:** Build complex, multi-step agents in just a few lines of code without having to wire together complicated execution graphs.
*   **Resilience & Flexibility:** The model can re-plan on the fly. If a specific tool fails or returns unexpected data, the LLM can autonomously try an alternative strategy rather than crashing a hardcoded pipeline.
*   **Seamless Multi-Agent Collaboration:** Enables easy orchestration of sub-agents simply by registering them as tools. This allows the master agent to reason about when to delegate tasks to specialized workflows dynamically based on the evolving context.

###  When to use what:
Use **Spring AI** as your foundational layer to easily connect your enterprise data and build standard RAG or conversational applications. 

Add the **`ai-agent-java-sdk`** toolkit when you need to deploy autonomous, model-driven agents into mission-critical environments where strict token budgeting, PII redaction, automated dependency validation, and dynamic, on-the-fly task planning are absolute requirements.

## Build

```bash
mvn clean install
```

## Layout

| Path | Role |
|------|------|
| `pom.xml` | Parent BOM: Spring Boot, Spring AI, and [Spring AI AgentCore](https://github.com/spring-ai-community/spring-ai-agentcore) alignment (`org.springaicommunity:spring-ai-agentcore-bom`) |
| `ai-agent-java-sdk-core/` | Core library (`AiAgent`, execution loop, tools, observability) |
| `ai-agent-java-sdk-dynamodb-session-manager/` | Optional DynamoDB `SessionManager` |
| `examples/*` | Runnable Spring Boot sample applications |

Add optional AgentCore modules in your app using the same BOM version property `spring-ai-agentcore.version` from the parent POM (for example `spring-ai-agentcore-runtime-starter`).

## Documentation

See [ai-agent-java-sdk-core/README.md](ai-agent-java-sdk-core/README.md) and [examples/README.md](examples/README.md).

📚 Getting Started & Tutorials
Ready to see it in action? Check out our step-by-step documentation to start building model-driven, enterprise-grade AI agents today:

(https://github.com/vaquarkhan/ai-agent-java-sdk/blob/main/ai-agent-java-sdk-core/docs/tutorial.md)
