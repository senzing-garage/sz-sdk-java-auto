# sz-sdk-java-auto build & test commands

Maven project (Java 17). Tests require a working Senzing installation with
native libraries.

## Common commands

```bash
mvn clean install                 # full clean build
mvn test                          # run all tests (needs native libs)
mvn test -Dtest=ClassName         # single test class
mvn test -Dtest=ClassName#method  # single test method
mvn -Pcheckstyle validate         # checkstyle (must pass before a PR)
```

## Required environment for tests

Tests load the Senzing native libraries through a generated wrapper script
(`target/java-wrapper/bin/java-wrapper.bat`, produced by
`GenerateTestJVMScript` during the `process-test-classes` phase and used as the
surefire JVM). Two environment variables must be set:

- `SENZING_PATH` — path to the Senzing installation.
- `SENZING_DEV_LIBRARY_PATH` — path to the development libraries.

The `sz-sdk-java` git submodule must be initialized
(`git submodule update --init --recursive`) — tests reuse test utilities from
it, and they are added as test sources via `build-helper-maven-plugin`.

## Profiles

- `jacoco` — coverage report (`mvn -Pjacoco test`, output in
  `target/site/jacoco/`).
- `checkstyle` — checkstyle against the shared
  `.java-coding-standards/checkstyle/senzing-checkstyle.xml`.
- `spotbugs` — SpotBugs + FindSecBugs static analysis
  (`mvn -Pspotbugs validate`).
- `release` — enables GPG signing for Maven Central deployment
  (`mvn clean install -Prelease`).

## Coding standards

Java formatting is governed by the `.java-coding-standards` submodule. Reformat
with:

```bash
python3 .java-coding-standards/tooling/scripts/format_file.py src/main/java src/test/java
```

See the `building/java-formatting-standards` shared FAQ for day-to-day usage.
