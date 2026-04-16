# Calculator-only minimal agent

Java / Spring AI analogue of the smallest Strands Python sample:

```python
from strands import Agent
from strands_tools import calculator
agent = Agent(tools=[calculator])
agent("What is the square root of 1764?")
```

([Strands docs](https://strandsagents.com/docs/user-guide/quickstart/python/) - “Quick Start” calculator snippet.)

This project registers **one** `ToolCallback` (`calculator`) and a **scripted** `LoopModelClient` that invokes `isqrt` for `1764`, then returns a short answer - **no cloud LLM required**.

## Run

```bash
mvn spring-boot:run
curl -s "http://localhost:8089/api/ask"
```

## Author

Vaquar Khan
