# sz-sdk-java-auto

The **Senzing Automatic Core SDK for Java** extends the
[Senzing Core SDK for Java](https://github.com/senzing-garage/sz-sdk-java)
with automatic handling features that make the SDK well-suited for
long-running, multi-threaded, server-side applications.

> If you are beginning your journey with [Senzing], please start with the
> [Senzing Quick Start guides].
>
> You are in the [Senzing Garage] where projects are "tinkered" on.
> Although this GitHub repository may help you understand an approach to
> using Senzing, it's not considered to be "production ready" and is not
> considered to be part of the Senzing product. It may not even be
> appropriate for your application of Senzing.

## Features

`SzAutoCoreEnvironment` is a drop-in replacement for `SzCoreEnvironment`
that adds the following capabilities:

- **Automatic basic retry** for any Senzing Core SDK method that fails
  with an `SzRetryableException`. The method is retried up to a
  configurable maximum, with an increasing delay between attempts.

- **Automatic configuration refresh** keeps the active configuration in
  sync with the current default configuration. Three modes are
  available:
  - **Disabled** — no automatic refresh.
  - **Reactive** — on-demand refresh when a method annotated with
    `@SzConfigRetryable` fails; if the configuration actually changed,
    the method is automatically retried.
  - **Proactive** — a background thread periodically compares the
    active and default configuration IDs and reinitializes when they
    differ. Reactive refresh is also performed in this mode.

- **Isolated thread pool** that confines all Senzing Core SDK operations
  to a fixed set of worker threads, allowing the SDK's thread-local
  resources to be reused across operations while preventing excessive
  memory growth from too many threads.

- **`ExecutorService`-like interface** (`submitTask(...)`) for running
  user code on the same thread pool as the SDK operations, avoiding
  unnecessary context switching when multiple SDK calls are made from
  the same task.

## Requirements

- **Java 17** or later.
- A working Senzing installation with native libraries (required at
  runtime by the underlying Senzing Core SDK).

## Maven Dependency

`sz-sdk-java-auto` is published to Maven Central:

```xml
<dependency>
  <groupId>com.senzing</groupId>
  <artifactId>sz-sdk-auto</artifactId>
  <version>1.0.0</version>
</dependency>
```

This artifact transitively depends on the Senzing Core SDK for Java
(`com.senzing:sz-sdk`), so you do not need to declare it separately.

## Usage

### Creating an `SzAutoCoreEnvironment`

Use `SzAutoCoreEnvironment.newAutoBuilder()` (or
`new SzAutoCoreEnvironment.Builder()`) to configure and create an
instance. The builder inherits all of the standard `SzCoreEnvironment`
configuration (settings, instance name, verbose logging, etc.) and adds
three additional options.

```java
import java.time.Duration;
import com.senzing.sdk.SzEngine;
import com.senzing.sdk.core.auto.SzAutoCoreEnvironment;

String settings = "{ \"PIPELINE\": { ... }, \"SQL\": { ... } }";

try (SzAutoCoreEnvironment env = SzAutoCoreEnvironment.newAutoBuilder()
        .instanceName("my-app")
        .settings(settings)
        .verboseLogging(false)
        .maxBasicRetries(2)                         // default is 2
        .configRefreshPeriod(Duration.ofMinutes(1)) // proactive refresh
        .concurrency(SzAutoCoreEnvironment.RECOMMENDED_CONCURRENCY)
        .build())
{
    SzEngine engine = env.getEngine();
    // ... use the engine, configManager, diagnostic, product, etc.
}
```

Only **one** active `SzCoreEnvironment` instance (including any
`SzAutoCoreEnvironment`) may exist in a JVM process at a time. The
existing instance must be destroyed before a new one can be created.

### Configuration Refresh

The `configRefreshPeriod(Duration)` builder method controls the refresh
mode:

| Value | Mode | Behavior |
|-------|------|----------|
| `null` (`DISABLED_CONFIG_REFRESH`) | Disabled | No automatic refresh; `@SzConfigRetryable` methods are not retried. |
| `Duration.ZERO` (`REACTIVE_CONFIG_REFRESH`) | Reactive | Refresh on-demand when a `@SzConfigRetryable` method fails; retry if the configuration actually changed. |
| Positive `Duration` | Proactive | Background thread refreshes every N seconds. Reactive refresh also occurs on failures. |

**Note:** Configuration refresh cannot be combined with an explicit
`configId(Long)`. The builder will throw `IllegalStateException` if
both are set.

### Thread Pool

The `concurrency(Integer)` builder method controls the internal thread
pool used to execute SDK operations:

| Value | Behavior |
|-------|----------|
| `null` (`DISABLED_CONCURRENCY`) | Thread pool disabled; SDK operations run in the calling thread. |
| `0` (`RECOMMENDED_CONCURRENCY`) | Pool sized to `Runtime.availableProcessors()`. |
| Positive integer | Pool sized to the specified value. |

When the thread pool is enabled, you can submit your own tasks to run
on the same pool — useful when a task makes multiple SDK calls and you
want to avoid context switching between them:

```java
Future<String> result = env.submitTask(() -> {
    SzEngine engine = env.getEngine();
    return engine.getEntityByRecordId("CUSTOMERS", "1001", flags);
});
```

### Basic Retry

`maxBasicRetries(int)` sets the maximum number of times any SDK method
will be retried when it fails with `SzRetryableException`. The default
is `2`. Each subsequent retry uses an increasing delay to allow the
underlying condition to resolve.

## Building from Source

### Prerequisites

1. **Java 17** or later.
2. **Apache Maven**.
3. **Senzing native libraries** installed locally (required for running
   the test suite).
4. The `sz-sdk-java` submodule, which provides shared test
   infrastructure and the parent `SzCoreEnvironment` class:

   ```sh
   git submodule update --init --recursive
   ```

### Environment Variables

The tests need to know where the Senzing installation lives:

- `SENZING_PATH` — path to the Senzing installation.
- `SENZING_DEV_LIBRARY_PATH` — path to the Senzing development
  libraries.

### Common Commands

```sh
# Full clean build, including tests
mvn clean install

# Compile and package without running tests
mvn clean install -DskipTests

# Run the test suite (requires Senzing native libraries)
mvn test

# Run a single test class
mvn test -Dtest=EngineBasicsTest

# Run a single test method
mvn test -Dtest=EngineBasicsTest#someMethod

# Run with code coverage (JaCoCo)
mvn test -Pjacoco

# Run Checkstyle
mvn validate -Pcheckstyle

# Run SpotBugs (with FindSecBugs)
mvn validate -Pspotbugs

# Release build with GPG signing
mvn clean install -Prelease
```

## Documentation

API documentation (Javadoc) is published alongside each release on
Maven Central. The Javadoc for `sz-sdk-java-auto` links against the
Javadoc for the underlying [Senzing Core SDK for Java].

## Contributing

See [CONTRIBUTING.md](CONTRIBUTING.md) for license-agreement details
and the pull request workflow.

## License

Licensed under the [Apache License, Version 2.0](LICENSE).

[Senzing]: https://senzing.com
[Senzing Quick Start guides]: https://docs.senzing.com/quickstart/
[Senzing Garage]: https://github.com/senzing-garage
[Senzing Core SDK for Java]: https://github.com/senzing-garage/sz-sdk-java
