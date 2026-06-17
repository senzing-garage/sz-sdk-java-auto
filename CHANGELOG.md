# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
[markdownlint](https://dlaa.me/markdownlint/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

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
