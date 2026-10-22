# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Git Policy

Claude may edit files, create branches, commit, and push changes directly using
clear, detailed commit messages. Prefer working on a feature branch rather than
committing directly to `main`. Do not merge pull requests without confirmation.

## Project Overview

This is the **Senzing Java Automatic Core SDK** (`sz-sdk-java-auto`), which extends the Senzing Java Core SDK with automatic handling features for long-running multi-threaded server applications. The key enhancements include:

- **Automatic retry logic** for methods that fail with `SzRetryableException`
- **Automatic configuration refresh** to keep the active configuration in sync with the default configuration
- **Thread pool isolation** to optimize resource reuse and prevent excessive memory allocation
- **ExecutorService-like interface** for executing SDK operations with minimal context switching

This is a "garage project" (experimental/non-production) maintained by Senzing.

## Build Commands

### Basic Commands
```bash
# Full clean build
mvn clean install

# Run all tests (requires Senzing native libraries)
mvn test

# Run a single test class
mvn test -Dtest=ClassName

# Run a specific test method
mvn test -Dtest=ClassName#methodName
```

### Quality & Analysis
```bash
# Run tests with code coverage report
mvn test -Pjacoco

# Run checkstyle validation
mvn validate -Pcheckstyle

# Run static analysis with SpotBugs
mvn validate -Pspotbugs
```

### Release
```bash
# Build with GPG signing enabled (for releases)
mvn clean install -Prelease
```

## Architecture

### Core Components

1. **SzAutoCoreEnvironment** (`src/main/java/com/senzing/sdk/core/auto/SzAutoCoreEnvironment.java`)
   - Main class that extends `SzCoreEnvironment` from the sz-sdk-java submodule
   - Implements Builder pattern via nested `Builder` class and `AbstractBuilder<E, B>` for extensibility
   - Manages three key features via configuration:
     - **Basic retry**: Configurable via `maxBasicRetries` (default: 2)
     - **Configuration refresh**: Three modes via `RefreshMode` enum (DISABLED, REACTIVE, PROACTIVE)
     - **Thread pool**: Configurable via `concurrency` (null=disabled, 0=CPU cores, N=specific count)

2. **Reinitializer** (`src/main/java/com/senzing/sdk/core/auto/Reinitializer.java`)
   - Background thread for proactive configuration refresh
   - Only started when `RefreshMode.PROACTIVE` is enabled
   - Periodically checks if `activeConfigId != defaultConfigId` and reinitializes if needed
   - Gracefully shuts down when environment is destroyed

3. **RetryHandler** (inner class in `SzAutoCoreEnvironment.java`)
   - Dynamic proxy `InvocationHandler` that wraps SDK interface instances
   - Detects methods annotated with `@SzConfigRetryable`
   - Sets thread-local `CONFIG_RETRY_FLAG` to enable retry-after-refresh logic
   - Recursively proxies return values that are SDK interfaces

### Key Design Patterns

- **Proxy Pattern**: SDK interfaces are wrapped with dynamic proxies to intercept method calls
- **Thread-Local State**: Uses `ThreadLocal<Boolean>` flags to track retry state across call stack
- **Read-Write Locks**: Uses `ReentrantReadWriteLock` to coordinate operations during refresh/destroy
- **Builder Pattern**: Fluent builder API with type-safe generics for extensibility

### Configuration Refresh Modes

1. **DISABLED** (`configRefreshPeriod = null`)
   - No automatic refresh, methods not retried after config changes

2. **REACTIVE** (`configRefreshPeriod = Duration.ZERO`)
   - On-demand refresh when `@SzConfigRetryable` method fails
   - Method automatically retried if config was updated

3. **PROACTIVE** (`configRefreshPeriod = Duration.ofSeconds(N)`)
   - Background thread refreshes every N seconds
   - ALSO performs reactive refresh on failures

### Thread Pool Usage

When concurrency > 0, all SDK operations execute in a fixed thread pool:
- Reuses threads to leverage Senzing's thread-local resource caching
- Prevents excessive memory usage from too many threads
- Provides `submitTask(Callable)` and `submitTask(Runnable)` methods for user tasks

## Test Structure

### Test Organization

Tests are organized in `src/test/java/com/senzing/sdk/core/auto/`:
- Basic tests (e.g., `EngineBasicsTest.java`) - Test core functionality
- Retry tests (e.g., `BasicRetryTest.java`, `ConfigRetryTest.java`) - Test retry mechanisms
- Concurrent retry tests (e.g., `ConcurrentRetryEngineBasicsTest.java`) - Test retry under concurrency
- `AbstractAutoCoreTest.java` - Base class for all tests, extends `AbstractCoreTest` from sz-sdk-java

### Test Infrastructure

- **Native Library Setup**: Tests use `GenerateTestJVMScript` (from sz-sdk-java) to create a wrapper script that loads native libraries
- **Environment Variables Required**:
  - `SENZING_PATH` - Path to Senzing installation
  - `SENZING_DEV_LIBRARY_PATH` - Path to development libraries
- **Test Sources**: Build includes tests from `sz-sdk-java/src/test/java` via `build-helper-maven-plugin`
- **Parallel Execution**: JUnit configured for concurrent class-level execution (see surefire plugin config)

## Important Constraints

### Native Library Requirements
- This project wraps native Senzing libraries via JNI
- Must have Senzing native libraries installed and properly configured
- The `java-wrapper.bat` script (generated during build) sets up native library paths

### Single Active Environment
- Only **one active** `SzCoreEnvironment` instance (including `SzAutoCoreEnvironment`) can exist per JVM process
- Attempting to create a second instance throws `IllegalStateException`
- Must call `destroy()` before creating a new instance

### Configuration Refresh Restrictions
- Cannot specify both an explicit `configId` AND enable configuration refresh
- Builder will throw `IllegalStateException` if both are provided

### Submodule Dependency
- `sz-sdk-java` submodule must be initialized: `git submodule update --init --recursive`
- Tests depend on test utilities from the submodule
- Main code extends `SzCoreEnvironment` from the submodule

## Maven Configuration Notes

### Key Plugins

1. **exec-maven-plugin**: Generates java-wrapper.bat script during `process-test-classes` phase
2. **build-helper-maven-plugin**: Adds test sources from sz-sdk-java submodule and demo sources
3. **maven-compiler-plugin**: Excludes core SDK tests from compilation (only run auto-specific tests)
4. **maven-surefire-plugin**: Uses generated java-wrapper.bat as JVM, configures parallel execution

### Profiles

- `release`: Enables GPG signing for Maven Central deployment
- `jacoco`: Enables code coverage reporting
- `checkstyle`: Enables checkstyle validation with suppressions
- `spotbugs`: Enables static analysis including FindSecBugs
- `java-17` / `java-18+`: Handle Javadoc tag compatibility across Java versions

## Java Coding Standards

**IMPORTANT — apply when generating or modifying Java code:** All Java code
(new and existing) in this repository must conform to the formatting rules in
`.java-coding-standards/docs/java-coding-standards.md`. Apply these rules
**from the start** — do not write code first and reformat afterward. When in
doubt about a specific case (parameter alignment, method continuation,
ternary tier, javadoc reflow), read the full standards document or search
the FAQ:
`mcp__sz-sdk-java-auto-faq__search_faqs(query="java formatting")`.

### Quick reference

- **80-character line limit** (enforced by checkstyle via `-Pcheckstyle`).
  Lines beyond 80 chars must be wrapped.
- **Allman braces** for class/interface/enum/method/constructor definitions
  (opening `{` on its own line, left-aligned with the declaration).
- **Same-line braces** for control flow: `if`/`else`/`for`/`while`/`do`/
  `try`/`catch`/`finally`/`switch`/`synchronized`, lambdas, array
  initializers, static init blocks.
- **Multi-line conditions**: when an `if`/`catch`/etc. condition wraps to
  multiple lines, the opening brace goes on its own line (Allman) to
  visually separate condition from body.
- **Method parameters** (priority order): single line if it fits; otherwise
  paren-aligned with types/names aligned in columns; otherwise next-line
  double-indented.
- **`throws` clauses** go on their own line, single-indented.
- **Continuation indentation**: +4 per wrap level (cumulating to
  8 spaces of displacement for the typical double-wrap; see the
  full standards doc for the per-level rule).
- **Operators on continuation lines**: break **before** `+`, `&&`, `||`, `?`,
  `:`, `.` (the operator starts the continuation line).
- **Short-circuit `if`**: `if (cond) statement;` on one line is preferred
  (Tier 1) when it fits; otherwise add braces.
- **Javadoc**: reflow prose and `@param`/`@return`/`@throws` to fill lines
  near 80 chars; do not leave 1-3 orphan words on a line.
- **CSOFF/CSON**: only for deliberately aligned multi-line output
  (column-formatted diagnostics, ASCII art, SQL DDL with aligned clauses)
  — never a general escape hatch.

### Verification

Run checkstyle: `mvn -Pcheckstyle validate` (must report `BUILD SUCCESS`
before opening a PR).

### Bulk formatting

`.java-coding-standards/tooling/scripts/format_file.py` is the
single end-user entry point — a thin wrapper that invokes the
tree-sitter-based AST formatter at `format_java.py` in-process.
Accepts one or more file or directory targets (directories are
recursively scanned for `.java` files):

```bash
# Format a single file in place.
python3 .java-coding-standards/tooling/scripts/format_file.py path/to/File.java

# Format every .java file under src/main/java/ in place.
python3 .java-coding-standards/tooling/scripts/format_file.py src/main/java
```

The same script is wired to the VSCode `Format Java file to
Senzing standards` task, the `emeraldwalk.runonsave` extension
(format-on-save), and the Claude Code `PostToolUse` hook — so
every save runs the canonical formatter. Same input → same
output, regardless of caller. The `building/java-formatting-
standards` FAQ summarizes day-to-day usage.

## FAQ MCP Server

This project ships a local FAQ MCP server registered in `.mcp.json` under
the name `sz-sdk-java-auto-faq`. It serves both:

- **Shared FAQs** from the standards-repo submodule
  (`.java-coding-standards/docs/faqs/`) — coding standards, javadoc reflow
  rules, system-stubs/ResourceLock test pattern, FAQ-authoring conventions.
- **Project-local FAQs** from `.claude/faqs/<category>/<topic>.md` —
  project-specific architecture, conventions, build/release notes,
  troubleshooting.

The server merges both into one BM25-ranked search index. Tool surface:

- `mcp__sz-sdk-java-auto-faq__get_faq_categories`
- `mcp__sz-sdk-java-auto-faq__search_faqs(query=...)`
- `mcp__sz-sdk-java-auto-faq__get_faq(title=...)`

**Use it BEFORE making design assumptions or troubleshooting.** Specifically:

- Before changing build, test, or release configuration (`pom.xml`,
  surefire, checkstyle, jacoco, spotbugs, release process), call
  `search_faqs` for relevant topics.
- Before modifying public APIs, search for any documented invariants or
  rationale.
- When a build, test, or dependency issue surfaces, search the
  `troubleshooting` category first.
- When unsure what is documented, call `get_faq_categories` to enumerate
  what's available.

**After resolving a non-obvious issue**, ask the user whether to capture
the solution as a new FAQ. Project-specific lessons go in
`.claude/faqs/<category>/<topic>.md`. Lessons about the standards
themselves go via PR to the standards repo. Restart the session so the
server re-indexes.

FAQs are pulled on demand, so detail is cheap there. Keep CLAUDE.md lean
and push operational/troubleshooting depth into FAQ files.

## Testing Configuration

Tests use JUnit Jupiter with parallel execution enabled (configured in
`pom.xml` surefire plugin):

- Classes run concurrently.
- Methods within a class run in same thread (default).
- Dynamic parallelism factor.

### System Stubs, ExecutionMode, and ResourceLock

Tests that **stub environment variables** or **capture stdout / stderr**
must follow the project's `system-stubs` + `@Execution(SAME_THREAD)` +
`@ResourceLock` pattern to avoid build-log noise and inter-class capture
races. Before writing such a test, search the FAQ:
`mcp__sz-sdk-java-auto-faq__search_faqs(query="system stubs")`.

Headline rules:

- Use `system-stubs-jupiter` **programmatically at the method level**
  (`new EnvironmentVariables(...).execute(...)`, `new SystemOut().execute(...)`,
  `new SystemErr().execute(...)`) — never the `@ExtendWith` annotation form.
- Tag the test (or the class) with `@Execution(ExecutionMode.SAME_THREAD)`
  — `System.setOut` / `setErr` are JVM-wide, so concurrent redirects race.
- Add `@ResourceLock(Resources.SYSTEM_OUT)` and/or
  `@ResourceLock(Resources.SYSTEM_ERR)` for cross-class mutual exclusion.
  When both are present, **always declare `SYSTEM_OUT` first, `SYSTEM_ERR`
  second** to avoid deadlock.
- If the production code starts a background thread in its constructor,
  place the `new ...()` call **inside** the `stub.execute(...)` lambda so
  the redirect is active before the thread starts.

Full pattern, examples, and the JVM-warning suppression details are in the
shared `testing/system-stubs-and-output-capture` FAQ.
