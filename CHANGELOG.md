# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
[markdownlint](https://dlaa.me/markdownlint/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.1.0] - 2026-10-23

### Fixed in version 1.1.0

- Fixed a thread pool deadlock. A nested SDK dispatch made from inside a pool
  thread submitted a second task to the same fixed pool and then waited on it,
  so a pool saturated with outer tasks could never make progress. Pool threads
  are now stamped with their owning environment, and a dispatch already running
  on that environment's own pool thread executes inline instead of
  re-submitting.
- Fixed proactive configuration refresh being silently disabled by a
  sub-millisecond `configRefreshPeriod`. The period truncated to a zero delay,
  and `wait(0)` blocks until notified rather than returning immediately, so the
  background refresh never ran for exactly the periods that requested it most
  often. The delay is now floored at one millisecond.
- Added the missing `@Test` annotation to three `submitTask` failure tests that
  were therefore never executed.

### Changes in version 1.1.0

- Raised the minimum required `com.senzing:sz-sdk` version from `4.3.0` to
  `4.4.0`. The native JNI layer changed between those releases from
  int-return-with-out-parameter to direct-return-with-throws, and the project
  does not build against `4.3.0`. **Consumers running Senzing 4.3.x must remain
  on `1.0.1`.**
- Adopted the shared [`java-coding-standards`](https://github.com/senzing-garage/java-coding-standards)
  at release `0.7.1`, pinned as the `.java-coding-standards` submodule, and
  wired up the checkstyle profile, the formatter, the VSCode integration and
  the editor hooks from that pin. Reformatting only; no behavioral change.
- `ConcurrentRetryEngineReadTest` now resolves the installed Senzing build
  version from `szBuildVersion.json` and skips the search-defaults comparison on
  builds that predate deterministic cross-thread name scoring, where a
  thread-local name culture cache could score a borderline name pair
  differently on each pool thread. The check compares by semantic version
  rather than raw build number, because build numbers are not ordered across
  release lines, and it deliberately fails open so an unreadable version file
  runs the test rather than suppressing it.
- Upgraded `com.senzing:senzing-commons` (test scope) from `4.0.1` to `4.0.2`.
- Upgraded `org.junit.jupiter:junit-jupiter` (test scope) from `6.1.0` to
  `6.1.3`.
- Upgraded `org.apache.commons:commons-configuration2` (test scope) from
  `2.15.0` to `2.15.1`.
- Upgraded `org.slf4j:slf4j-api` (test scope) from `2.0.18` to `2.0.20`.
- Upgraded `org.slf4j:slf4j-simple` (test scope) from `2.0.18` to `2.0.20`.
- Upgraded `org.xerial:sqlite-jdbc` (test scope) from `3.53.1.0` to `3.53.4.0`.
- Upgraded `com.github.spotbugs:spotbugs-maven-plugin` (build) from `4.9.8.3`
  to `4.10.4.1`.
- Upgraded `org.apache.maven.plugins:maven-compiler-plugin` (build) from
  `3.15.0` to `3.16.0`.
- Upgraded `org.apache.maven.plugins:maven-surefire-plugin` (build) from
  `3.5.5` to `3.6.0`.
- Upgraded `org.codehaus.mojo:build-helper-maven-plugin` (build) from `3.6.1`
  to `3.6.2`.
- Upgraded `org.codehaus.mojo:exec-maven-plugin` (build) from `3.6.3` to
  `3.6.4`.
- Upgraded `org.jacoco:jacoco-maven-plugin` (build) from `0.8.14` to `0.8.15`.
- Upgraded `org.sonatype.central:central-publishing-maven-plugin` (build) from
  `0.10.0` to `0.11.0`.

## [1.0.1] - 2026-06-17

### Changes in version 1.0.1

- Upgraded `com.senzing:senzing-commons` (test scope) from `4.0.0` to `4.0.1`.
- Upgraded `org.junit.jupiter:junit-jupiter` (test scope) from `6.0.3` to `6.1.0`.
- Upgraded `org.slf4j:slf4j-api` (test scope) from `2.0.17` to `2.0.18`.
- Upgraded `org.slf4j:slf4j-simple` (test scope) from `2.0.17` to `2.0.18`.
- Upgraded `org.xerial:sqlite-jdbc` (test scope) from `3.53.0.0` to `3.53.1.0`.
- Upgraded `org.apache.maven.plugins:maven-enforcer-plugin` (build) from `3.6.2`
  to `3.6.3`.

## [1.0.0] - 2026-05-14

### Changes in version 1.0.0

- Initial stable release.
- Raised the minimum required `com.senzing:sz-sdk` version from `4.1.0`
  to `4.3.0`, which relocates `GenerateTestJVMScript` from
  `com.senzing.test` (test scope) to `com.senzing.sdk.core` (main
  scope); the test-wrapper generation in `pom.xml` was updated to match.
- Upgraded `com.senzing:senzing-commons` (test scope) from `4.0.0-beta.3.0`
  to `4.0.0`.
- Upgraded `org.xerial:sqlite-jdbc` (test scope) from `3.51.3.0` to
  `3.53.0.0`.

## [0.5.1] - 2026-02-25

### Fixed in version 0.5.1

- Fixed `StackOverflowError` caused by infinite recursion between `execute()`
  and `ensureConfigCurrent()` when config refresh is enabled and a persistent
  failure occurs during a `@SzConfigRetryable` method.

## [0.5.0] - 2025-12-03

### Changes in version 0.5.0

- Initial stable pre-release version.
- Adds `SzAutoEnvironment` interface so `SzAutoCoreEnvironment` functionality
  can be proxied.
