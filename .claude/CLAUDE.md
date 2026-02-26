# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

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
