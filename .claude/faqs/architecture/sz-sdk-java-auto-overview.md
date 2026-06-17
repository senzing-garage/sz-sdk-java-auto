# sz-sdk-java-auto architecture overview

`sz-sdk-java-auto` (Maven artifact `com.senzing:sz-sdk-auto`) extends the
Senzing Core SDK for Java (`com.senzing:sz-sdk`) with automatic handling
features for long-running, multi-threaded, server-side applications.
`SzAutoCoreEnvironment` is a drop-in replacement for `SzCoreEnvironment`.

## The four enhancements

1. **Automatic basic retry** — any Senzing Core SDK method that fails with an
   `SzRetryableException` is retried up to a configurable maximum
   (`maxBasicRetries`, default `DEFAULT_MAX_BASIC_RETRIES = 2`) with an
   increasing delay between attempts.
2. **Automatic configuration refresh** — keeps the active configuration ID in
   sync with the current default configuration ID. See the
   `conventions/configuration-refresh-modes` FAQ for the DISABLED / REACTIVE /
   PROACTIVE modes.
3. **Isolated thread pool** — confines all SDK operations to a fixed worker
   pool so Senzing's thread-local native resources are reused across
   operations, while preventing the excessive memory growth that comes from
   running Senzing work on too many threads. Controlled by `concurrency`
   (`null` = disabled / run on calling thread, `0` =
   `Runtime.availableProcessors()`, `N` = fixed count).
4. **`ExecutorService`-like interface** — `submitTask(Callable)` /
   `submitTask(Runnable)` run user code on the same pool as SDK operations to
   avoid unnecessary context switching when several SDK calls happen in one
   task.

## Key classes

- **`SzAutoCoreEnvironment`** (`src/main/java/com/senzing/sdk/core/auto/`)
  — the main class; extends `SzCoreEnvironment`. Built via the fluent nested
  `Builder` / `AbstractBuilder<E, B>` (generic for extensibility). Only **one
  active** environment may exist per JVM — call `destroy()` before creating
  another.
- **`Reinitializer`** — background thread used only in PROACTIVE mode;
  periodically reinitializes when `activeConfigId != defaultConfigId` and shuts
  down gracefully on `destroy()`.
- **`RetryHandler`** (inner class) — a dynamic-proxy `InvocationHandler` that
  wraps returned SDK interfaces, detects methods annotated with
  `@SzConfigRetryable`, sets the thread-local `CONFIG_RETRY_FLAG`, and
  recursively proxies SDK-interface return values.
- **`SzAutoEnvironment`** — interface so `SzAutoCoreEnvironment` functionality
  can itself be proxied.

## Design patterns

- **Proxy pattern** for intercepting SDK method calls.
- **Thread-local flags** (`CONFIG_RETRY_FLAG`, `RETRIED_FLAG`,
  `ENSURING_CONFIG`) to track retry/refresh state across the call stack and
  prevent re-entrant recursion during config refresh.
- **`ReentrantReadWriteLock`** to coordinate normal operations against
  refresh/destroy.
- **Builder pattern** with type-safe generics for subclassing.

> Garage project: experimental, not production-supported.
