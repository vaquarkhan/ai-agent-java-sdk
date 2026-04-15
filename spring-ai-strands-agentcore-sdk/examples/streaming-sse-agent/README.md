# Streaming + SSE

High-value pattern for production chat: **`StrandsAgent.executeStreaming`** exposed as **Server-Sent Events** (`text/event-stream`).

Strands Python exposes streaming via async generators / `stream_async`; on Spring Boot this maps naturally to **`Flux<String>`** and an MVC controller with `produces = TEXT_EVENT_STREAM_VALUE`.

This sample uses **no tools** (`tool-discovery.enabled: false`) and a **scripted** `LoopModelClient#stream` that emits several `StreamEvent.Token` chunks — runs without LLM credentials.

## Run

```bash
mvn spring-boot:run
curl -N "http://localhost:8090/api/stream"
```

## Author

Vaquar Khan
