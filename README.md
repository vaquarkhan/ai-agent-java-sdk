# AI Agent Java SDK

> We don’t replace Spring AI or AgentCore; we give a strict, testable execution loop and governance for tool-calling agents on the JVM.

The root reactor builds **only** the SDK modules. Sample apps under `examples/` are optional and use a [separate aggregator POM](examples/pom.xml).

## Build (SDK)

```bash
mvn clean install
```

## Build (examples, optional)

Install the SDK first, then either build all examples or one module:

```bash
mvn clean install
mvn -f examples/pom.xml -DskipTests package
# or: cd examples/quickstart-agent && mvn package
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
