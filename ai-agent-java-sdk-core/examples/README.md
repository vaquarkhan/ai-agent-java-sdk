# Examples (AWS Strands Python → AI Agent Java SDK)

| Example | Why it matters | Run (default port) |
|---------|----------------|--------------------|
| [quickstart-agent](quickstart-agent/) | Full **multi-tool** quickstart (`calculator`, `current_time`, `letter_counter`) like the [Strands quickstart](https://strandsagents.com/docs/user-guide/quickstart/python/). | `8088` |
| [calculator-minimal-agent](calculator-minimal-agent/) | **Smallest** useful agent - single calculator tool (Python one-liner `Agent(tools=[calculator])`). | `8089` |
| [streaming-sse-agent](streaming-sse-agent/) | **`executeStreaming`** + **SSE** - standard pattern for chat UIs and long responses. | `8090` |
| [tool-discovery-filter-agent](tool-discovery-filter-agent/) | **`tool-discovery`** include/exclude globs - essential when many `ToolCallbackProvider` beans exist (MCP, AgentCore, yours). | `8091` |
| [minimal-agent](minimal-agent/) | **Simplest** possible agent - no tools, just model Q&A via properties. | `8092` |
| [streaming-agent](streaming-agent/) | **`executeStreaming`** + **SSE** - token-by-token streaming without tools. | `8093` |
| [tool-agent](tool-agent/) | **Custom tools** (weather lookup, unit converter) with tool discovery and multi-turn execution. | `8094` |

All samples use a **scripted** `LoopModelClient` where noted so you can run them **without** cloud LLM keys; swap in a `ChatClient`-backed client for real model-driven behavior.

From the repository root:

```bash
mvn -DskipTests install
cd ai-agent-java-sdk-core/examples/<example-dir>
mvn spring-boot:run
```
