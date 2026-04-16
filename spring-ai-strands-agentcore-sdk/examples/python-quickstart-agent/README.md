# Python quickstart (ported to Java / Spring AI)

This example mirrors the official **Strands Python quickstart**: an agent with **community-style tools** (`calculator`, `current_time`) and a custom **`letter_counter`**, then a multi-part user message.

- Python reference: [Python Quickstart | Strands Agents SDK](https://strandsagents.com/docs/user-guide/quickstart/python/)
- Source sketch (conceptually): `pip install strands-agents strands-agents-tools`, then `Agent(tools=[calculator, current_time, letter_counter])` and the long user prompt with four requests.

## What this Java project does

| Python (Strands) | This example |
|------------------|--------------|
| `from strands import Agent, tool` | `StrandsAgent` + `StrandsExecutionLoop` (from **spring-ai-strands-agentcore-sdk**) |
| `from strands_tools import calculator, current_time` | `QuickstartToolCallbacks.calculator()` and `.currentTime()` (same roles, Java implementations) |
| `@tool def letter_counter(...)` | `QuickstartToolCallbacks.letterCounter()` |
| `agent(message)` | `GET /api/quickstart` → `strandsAgent.execute(...)` |

### Scripted model (no API keys)

By default, **`PythonQuickstartConfiguration`** registers a **`@Primary` `LoopModelClient`** that **scripts** the three tool calls in order, then returns a short final line. That lets you run the app **without** Amazon Bedrock or OpenAI credentials while still exercising **real tool execution** through `ToolRegistry`.

For a **real** foundation model, replace that bean with a `LoopModelClient` implementation that delegates to Spring AI **`ChatClient`** (or your provider) and maps responses to `ModelTurnResponse` / `StreamEvent`s.

## Run

From the repository root (after installing the SDK module locally if needed):

```bash
cd spring-ai-strands-agentcore-sdk/examples/python-quickstart-agent
mvn spring-boot:run
```

Then:

```text
curl -s "http://localhost:8088/api/quickstart"
```

Optional custom message:

```text
curl -s --get "http://localhost:8088/api/quickstart" --data-urlencode "message=What is 144 divided by 12?"
```

(With the scripted model, custom prompts still follow the same fixed tool sequence when prior tool outputs are 0-2; swap in a real `LoopModelClient` for LLM-driven behavior.)

## Author

Vaquar Khan
