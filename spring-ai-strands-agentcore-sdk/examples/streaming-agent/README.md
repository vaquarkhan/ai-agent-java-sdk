# Streaming Agent Example

Demonstrates `StrandsAgent.executeStreaming()` returning `Flux<String>` as Server-Sent Events.

Uses a scripted streaming `LoopModelClient` so you can run it without cloud API keys.

## Run

```bash
mvn spring-boot:run
```

## Try it

```bash
curl -N "http://localhost:8093/api/stream?q=Tell+me+about+Spring+AI"
```

## What it demonstrates

- `executeStreaming()` returning `Flux<String>` for token-by-token delivery
- SSE endpoint using `MediaType.TEXT_EVENT_STREAM_VALUE`
- Scripted streaming model client (swap for a real LLM in production)
