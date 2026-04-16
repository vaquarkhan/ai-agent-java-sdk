# Minimal Agent Example

The simplest possible Strands agent: no tools, just model-driven Q&A configured entirely via `application.yml`.

Uses a scripted `LoopModelClient` so you can run it without cloud API keys.

## Run

```bash
mvn spring-boot:run
```

## Try it

```bash
curl "http://localhost:8092/api/ask?q=What+is+Spring+AI?"
```

## What it demonstrates

- Property-driven agent configuration (`strands.agent.*`)
- Single synchronous endpoint calling `StrandsAgent.execute()`
- Scripted model client (swap for a real LLM in production)
