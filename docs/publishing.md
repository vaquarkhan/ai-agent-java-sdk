# Publishing to Maven Central

This repository publishes **`io.github.vaquarkhan`** artifacts from the **root** Maven reactor (`ai-agent-java-sdk`, `ai-agent-java-sdk-core`, `ai-agent-java-sdk-dynamodb-session-manager`). The `examples/` tree is **not** published.

Use the [Central Publisher Portal](https://central.sonatype.com/) and [`central-publishing-maven-plugin`](https://central.sonatype.org/publish/publish-portal-maven/) ([Maven Central artifact](https://central.sonatype.com/artifact/org.sonatype.central/central-publishing-maven-plugin)).

## Maintainer setup

1. Register namespace and components: [Publish guide — initial setup](https://central.sonatype.org/publish/publish-guide/#initial-setup)
2. Create a Portal user token: [Generate user token](https://central.sonatype.org/publish/generate-portal-token/)
3. GPG signing: [Requirements — GPG](https://central.sonatype.org/publish/requirements/#sign-files-with-gpgpgp)

## GitHub Actions secrets

| Secret | Purpose |
|--------|---------|
| `CENTRAL_PORTAL_USERNAME` | Portal token username |
| `CENTRAL_PORTAL_PASSWORD` | Portal token password |
| `MAVEN_GPG_PRIVATE_KEY` | Armored GPG private key |
| `MAVEN_GPG_PASSPHRASE` | Key passphrase |

## Workflow

- [`.github/workflows/maven-publish.yml`](../.github/workflows/maven-publish.yml) — dispatch **Publish to Maven Central** or push tag `v*.*.*`.

## Release version

Use a **release** `<version>` in the root `pom.xml` (e.g. `0.1.0`) before publishing. Bump, commit, tag, push, run the workflow.

## Local dry run

```bash
mvn -B clean verify -Pcentral-publish -Dgpg.skip=true
```

On PowerShell quote the flag: `-Pcentral-publish "-Dgpg.skip=true"`.
