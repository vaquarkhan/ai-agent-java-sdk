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
| `MAVEN_GPG_PRIVATE_KEY` | Armored GPG private key (recommended name) |
| `GPG_PRIVATE_KEY` | **Alias** — same value if you already use this name elsewhere |
| `MAVEN_GPG_PASSPHRASE` | Key passphrase (omit or leave empty only if your key has no passphrase) |
| `GPG_PASSPHRASE` | **Alias** for the passphrase |

The workflow imports the key with **`gpg --import`** (no third-party action). If **`crazy-max/ghaction-import-gpg`** was failing with **`gpg_private_key`**, the usual cause was a missing or wrongly named repository secret pull.

## Workflow

- [`.github/workflows/maven-publish.yml`](../.github/workflows/maven-publish.yml) — dispatch **Publish to Maven Central** or push tag `v*.*.*`.

## Release version

Use a **release** `<version>` in the root `pom.xml` (e.g. `0.1.0`) before publishing. Bump, commit, tag, push, run the workflow.

## Troubleshooting (GitHub Actions)

**`Input required and not supplied: gpg_private_key`** (older workflows used `ghaction-import-gpg`; current workflow uses **`gpg --import`** instead.)

If the job still fails at **Verify required secrets** or **Import GPG key**:

- Add **`MAVEN_GPG_PRIVATE_KEY`** *or* **`GPG_PRIVATE_KEY`** (exact names) under **Settings → Secrets and variables → Actions** on the **same repository** that runs the workflow (not only on your fork profile).
- Use the **armored** export (must contain `BEGIN PGP PRIVATE KEY BLOCK`): `gpg --armor --export-secret-keys KEY_ID`
- Set **`MAVEN_GPG_PASSPHRASE`** or **`GPG_PASSPHRASE`** if your key has a passphrase (both can be empty if the key truly has none).

**Deploy fails with Central / 401**

- Add **`CENTRAL_PORTAL_USERNAME`** and **`CENTRAL_PORTAL_PASSWORD`** from the [Central Portal user token](https://central.sonatype.org/publish/generate-portal-token/).

## Local dry run

```bash
mvn -B clean verify -Pcentral-publish -Dgpg.skip=true
```

On PowerShell quote the flag: `-Pcentral-publish "-Dgpg.skip=true"`.
