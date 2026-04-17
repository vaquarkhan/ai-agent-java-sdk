# AI Agent Java SDK

Single Maven reactor: build everything from the repository root (no git submodules).

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
