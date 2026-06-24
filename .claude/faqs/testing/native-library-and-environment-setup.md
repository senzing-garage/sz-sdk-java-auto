# Native-library test setup & single-active-environment constraint

Tests in this project exercise the real Senzing Core SDK, which is backed by
native libraries loaded via JNI. There are two things to know before writing or
running tests.

## 1. Native-library JVM wrapper

The build does not run tests on a plain `java` — it generates a wrapper script
that sets up the native library paths first:

- `GenerateTestJVMScript` (from the `sz-sdk-java` submodule, main scope as of
  `sz-sdk` 4.3.0) runs in the `process-test-classes` phase and writes
  `target/java-wrapper/bin/java-wrapper.bat`.
- `maven-surefire-plugin` is configured to use that wrapper as its `<jvm>`.
- Generation reads `SENZING_PATH` and `SENZING_DEV_LIBRARY_PATH` (see the
  `building/sz-sdk-java-auto-build-commands` FAQ). Without them, tests cannot
  locate the native libraries.

`AbstractAutoCoreTest` (extends `AbstractCoreTest` from the submodule) is the
base class for the project's tests.

## 2. One active environment per JVM

Only **one active** `SzCoreEnvironment` (including `SzAutoCoreEnvironment`) may
exist per JVM process. Constructing a second while one is active throws
`IllegalStateException`; you must `destroy()` the first one first. Tests must
tear down their environment (and let the background `Reinitializer` shut down)
before the next environment is created.

Surefire runs **classes** concurrently (methods same-thread, dynamic factor) —
so cross-class isolation matters. Tests that stub environment variables or
capture `System.out` / `System.err` must follow the `system-stubs` +
`@Execution(SAME_THREAD)` + `@ResourceLock` pattern; see the shared
`testing/system-stubs-and-output-capture` FAQ. When a production object starts
a background thread in its constructor (e.g. PROACTIVE mode's `Reinitializer`),
construct it **inside** the `stub.execute(...)` lambda so the stream redirect is
active before the thread starts.
