package com.senzing.sdk.core.auto;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import java.io.File;
import java.nio.file.Files;
import java.time.Duration;
import javax.json.JsonObject;
import com.senzing.nativeapi.InstallLocations;
import com.senzing.util.JsonUtilities;
import com.senzing.util.SemanticVersion;
import static org.junit.jupiter.api.TestInstance.Lifecycle;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class ConcurrentRetryEngineReadTest extends EngineReadTest
{
    private static final Integer CONCURRENCY = 4;

    private static final Duration DURATION = Duration.ofMillis(500);

    /**
     * The name of the JSON file describing the installed Senzing build.
     */
    private static final String BUILD_VERSION_FILE = "szBuildVersion.json";

    /**
     * The JSON property holding the dotted Senzing build version.
     */
    private static final String BUILD_VERSION_KEY = "BUILD_VERSION";

    /**
     * The oldest Senzing build known to carry the GDEV-4791 fix.
     *
     * <p>
     * Before this build the name culture cache was thread-local, so two calls
     * dispatched to different threads of the Senzing thread pool could score
     * the same pair of names differently (a borderline
     * {@code GNR_FN} of 80 on one thread and 81 on another, both of which
     * bucket as {@code LIKELY}). That made byte-for-byte comparison
     * of two search results unreliable whenever the concurrency is
     * greater than one. The defect is only reachable through the
     * pooled dispatch added by this project, so it cannot be reproduced
     * -- or suppressed -- from the {@code sz-sdk-java} tests.
     * </p>
     *
     * <p>
     * This is the oldest build <em>verified</em> to pass, not necessarily the
     * first build containing the fix; narrow it if the exact build is known.
     * Comparison is by {@link SemanticVersion}, not by raw build number,
     * because build numbers are not ordered across release lines (for example
     * {@code 4.4.3.26276} is numerically greater than
     * {@code 4.5.0.26275} yet predates the fix).
     * </p>
     */
    private static final SemanticVersion MIN_BUILD_VERSION
        = new SemanticVersion("4.5.0.26294");

    @Override
    protected Integer getConcurrency()
    {
        return CONCURRENCY;
    }

    @Override
    protected Duration getConfigRefreshPeriod()
    {
        return DURATION;
    }

    /**
     * The {@link SemanticVersion} of the installed Senzing build, or
     * <code>null</code> if it could not be determined.
     *
     * <p>
     * Resolved once at class initialization rather than per
     * test invocation: this method source yields several hundred
     * cases, and the JVM guarantees the initializer runs exactly
     * once even though JUnit executes test classes concurrently.
     * </p>
     */
    private static final SemanticVersion INSTALLED_BUILD_VERSION
        = readInstalledBuildVersion();

    /**
     * Reads the build version of the Senzing installation
     * under test from its <code>szBuildVersion.json</code>.
     *
     * <p>
     * This deliberately fails open: if the version cannot be determined for any
     * reason it returns <code>null</code>, which runs the gated test rather
     * than silently suppressing it. A note is written to standard error so that
     * an unreadable version file is not mistaken for a fixed build.
     * </p>
     *
     * @return The {@link SemanticVersion} for the installed Senzing build, or
     *         <code>null</code> if it cannot be determined.
     */
    private static SemanticVersion readInstalledBuildVersion()
    {
        String reason = null;
        try {
            InstallLocations locations = InstallLocations.findLocations();
            File installDir = (locations == null)
                ? null : locations.getInstallDirectory();

            if (installDir == null) {
                reason = "no Senzing installation was found";
            } else {
                File versionFile = new File(installDir, BUILD_VERSION_FILE);

                if (!versionFile.exists()) {
                    reason = versionFile + " does not exist";
                } else {
                    String jsonText = Files.readString(versionFile.toPath());

                    JsonObject jsonObj = JsonUtilities.parseJsonObject(
                        jsonText);

                    String buildVersion = JsonUtilities.getString(
                        jsonObj, BUILD_VERSION_KEY);

                    if (buildVersion == null) {
                        reason = "no " + BUILD_VERSION_KEY
                            + " in " + versionFile;
                    } else {
                        return new SemanticVersion(buildVersion);
                    }
                }
            }
        } catch (Exception e) {
            reason = e.toString();
        }

        System.err.println(
            "WARNING: could not determine the installed Senzing build "
                + "version (" + reason + "); version-gated tests will run "
                + "rather than be skipped.");

        return null;
    }

    /**
     * Gets the {@link SemanticVersion} describing the build of the Senzing
     * installation under test, or <code>null</code> if it cannot be determined.
     *
     * @return The {@link SemanticVersion} for the installed Senzing build, or
     *         <code>null</code> if it cannot be determined.
     */
    protected static SemanticVersion getInstalledBuildVersion()
    {
        return INSTALLED_BUILD_VERSION;
    }

    /**
     * Overridden to skip this test against Senzing builds that predate the
     * GDEV-4791 fix, where the thread-local name culture cache makes the two
     * search results legitimately differ under pooled dispatch.
     *
     * <p>
     * The single-threaded {@link EngineReadTest} still runs
     * this test against every build, so the default-flag
     * contract itself remains covered everywhere.
     * </p>
     *
     * {@inheritDoc}
     */
    @Override
    @ParameterizedTest
    @MethodSource("getSearchDefaultParameters")
    public void testSearchByAttributesdDefaults(String attributes,
                                                String searchProfile)
    {
        SemanticVersion buildVersion = getInstalledBuildVersion();

        assumeTrue(
            (buildVersion == null)
            || (buildVersion.compareTo(MIN_BUILD_VERSION) >= 0),
            "Senzing " + buildVersion + " predates the GDEV-4791 fix "
            + "(first verified in " + MIN_BUILD_VERSION + "), so the "
            + "thread-local name culture cache can score a borderline "
            + "name pair differently on each pool thread.");

        super.testSearchByAttributesdDefaults(attributes, searchProfile);
    }
}
