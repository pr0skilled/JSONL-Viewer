# JSONL Viewer

A fast viewer for **JSON Lines** files (`.jsonl`, `.ndjson`, `.jsonlines`, `.ldjson`)
for any IntelliJ-based IDE — built for files that are far too large for the normal
editor to open.

Opening a JSONL file adds a **Records** tab that shows the file as a virtualized,
scrollable list of one-line record previews. Selecting a record pretty-prints it
below with JSON syntax highlighting and independently foldable nested objects and
arrays.

## Features

- **Handles huge files** — the file is streamed and indexed by record offset; it is
  never loaded into the editor's document or PSI, so multi-hundred-megabyte files
  open instantly without hitting the IDE's file-size limits.
- **On-demand pretty-printing** — each record is formatted only when selected;
  nested structures fold independently (right-click → *Collapse All* / *Expand All*).
- **Full-text search** — search across the entire file; the list filters to the
  matching records with the match highlighted.
- **Read-only** — never modifies your files; the raw text stays available on a
  second tab.

Works in any IntelliJ-based IDE (IntelliJ IDEA, Rider, PyCharm, and others),
build 242 and newer.

## Installation

From a built distribution:

1. Build the plugin (see below) — the artifact lands in `build/distributions/`.
2. In your IDE: **Settings → Plugins → ⚙ → Install Plugin from Disk…** and pick the
   generated `.zip`.

To try it without installing, run the sandbox IDE:

```bash
./gradlew runIde
```

Open [`examples/sample.jsonl`](examples/sample.jsonl) to see the Records tab in action.

## Building

Requirements:

- JDK 21 for the compilation toolchain (resolved automatically by the IntelliJ
  Platform Gradle plugin).
- The Gradle daemon itself must run on a JDK **≤ 21** (the Kotlin DSL compiler in
  Gradle 8.x does not support newer JDKs). `gradle.properties` pins a local JDK
  path for this; override `org.gradle.java.home` for your own machine or CI.

Common tasks:

```bash
./gradlew build         # compile + test + assemble
./gradlew test          # run the unit tests
./gradlew runIde        # launch a sandbox IDE with the plugin loaded
./gradlew verifyPlugin  # JetBrains Plugin Verifier against pinned IDE builds
```

## Publishing

Marketplace upload (token from <https://plugins.jetbrains.com/author/me/tokens>):

```bash
INTELLIJ_PUBLISH_TOKEN=... ./gradlew publishPlugin
```

Plugin signing is wired but commented out in `build.gradle.kts`; enable it and supply
the certificate/key environment variables to publish a signed build.

## Project layout

```
src/main/kotlin/io/jsonlviewer/   plugin sources
src/main/resources/META-INF/      plugin.xml descriptor
src/test/kotlin/io/jsonlviewer/   unit tests
examples/                         sample .jsonl file
```

## License

[MIT](LICENSE) © 2026 pr0skilled
