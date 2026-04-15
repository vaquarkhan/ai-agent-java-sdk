# Tool discovery filters (include / exclude)

**High value for production:** Spring may register **many** `ToolCallbackProvider` beans (MCP, AgentCore browser tools, your own). The Strands SDK can **narrow** which tool names the model sees using **glob** patterns — **exclude wins over include** (deny-over-allow).

This example registers **four** tools:

| Tool name      | Intended outcome                         |
|----------------|------------------------------------------|
| `admin_secret` | Removed by `exclude-patterns: ["*_secret"]` |
| `public_echo`  | Kept (matches `public_*`)                |
| `demo_calc`    | Kept (matches `demo_*`)                 |
| `alpha_nav`    | Removed (does not match any `include-pattern`) |

With non-empty `include-patterns`, only tools that match **at least one** include **and** no exclude pattern are kept.

A **scripted** `LoopModelClient` calls `demo_calc` once (6×7) then summarizes.

## Run

```bash
mvn spring-boot:run
curl -s "http://localhost:8091/api/run"
```

## Author

Vaquar Khan
