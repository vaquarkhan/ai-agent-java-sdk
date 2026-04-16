# Tool Agent Example

Demonstrates custom tools (weather lookup, unit converter) with tool discovery and execution
in the reasoning loop.

Uses a scripted `LoopModelClient` so you can run it without cloud API keys.

## Run

```bash
mvn spring-boot:run
```

## Try it

```bash
curl "http://localhost:8094/api/ask?q=What+is+the+weather+in+Seattle+in+Fahrenheit?"
```

## What it demonstrates

- Custom tool callbacks (weather lookup, unit converter)
- Tool discovery with include/exclude patterns
- Multi-turn tool execution in the reasoning loop
- Scripted model client (swap for a real LLM in production)
