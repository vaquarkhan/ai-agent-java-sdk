# Examples (Strands Python → Java / Spring AI)

| Example | Why it matters | Run (default port) |
|---------|----------------|--------------------|
| [python-quickstart-agent](python-quickstart-agent/) | Full **multi-tool** quickstart (`calculator`, `current_time`, `letter_counter`) like the [Python doc](https://strandsagents.com/docs/user-guide/quickstart/python/). | `8088` |
| [calculator-minimal-agent](calculator-minimal-agent/) | **Smallest** useful agent — single calculator tool (Python one-liner `Agent(tools=[calculator])`). | `8089` |
| [streaming-sse-agent](streaming-sse-agent/) | **`executeStreaming`** + **SSE** — standard pattern for chat UIs and long responses. | `8090` |
| [tool-discovery-filter-agent](tool-discovery-filter-agent/) | **`tool-discovery`** include/exclude globs — essential when many `ToolCallbackProvider` beans exist (MCP, AgentCore, yours). | `8091` |

All samples use a **scripted** `LoopModelClient` where noted so you can run them **without** cloud LLM keys; swap in a `ChatClient`-backed client for real model-driven behavior.

From the repository root:

```bash
mvn -DskipTests install
cd spring-ai-strands-agentcore-sdk/examples/<example-dir>
mvn spring-boot:run
```
