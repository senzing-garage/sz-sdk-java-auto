package com.senzing.sdk.core.auto;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestMethodOrder;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.senzing.sdk.SzEngine;
import com.senzing.sdk.SzException;
import com.senzing.sdk.SzRecordKey;

import uk.org.webcompere.systemstubs.stream.SystemErr;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import static org.junit.jupiter.api.TestInstance.Lifecycle;

import java.util.concurrent.Callable;

/**
 * Regression test for the {@link StackOverflowError} caused by infinite
 * recursion between {@link SzAutoCoreEnvironment#execute(Callable)} and
 * {@link SzAutoCoreEnvironment#ensureConfigCurrent()}.
 *
 * <p>
 * When config refresh is enabled and {@code CONFIG_RETRY_FLAG} is set (via
 * a {@code @SzConfigRetryable} proxy), a persistent failure caused
 * {@code execute()} to call {@code ensureConfigCurrent()}, which called
 * {@code getActiveConfigId()} / {@code getDefaultConfigId()} back through
 * the overridden {@code execute()}, which called
 * {@code ensureConfigCurrent()} again, ad&nbsp;infinitum.
 *
 * <p>
 * The fix uses the {@code ENSURING_CONFIG} thread-local guard to prevent
 * nested {@code execute()} calls from re-entering the config-retry path
 * while {@code ensureConfigCurrent()} is already in progress.
 */
@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
@TestMethodOrder(OrderAnnotation.class)
public class EnsureConfigRecursionTest extends AbstractAutoCoreTest
{
    /**
     * A mock environment that can simulate persistent failures at the
     * {@link #doExecute(Callable)} level to trigger the recursion scenario.
     */
    private static class MockEnvironment extends SzAutoCoreEnvironment {
        private static final ThreadLocal<Boolean> ALWAYS_FAIL = new ThreadLocal<>() {
            protected Boolean initialValue() {
                return Boolean.FALSE;
            }
        };

        public MockEnvironment(String instanceName, String settings) {
            super(SzAutoCoreEnvironment.newAutoBuilder()
                    .settings(settings).instanceName(instanceName)
                    .configRefreshPeriod(REACTIVE_CONFIG_REFRESH)
                    .concurrency(null));
        }

        public void setAlwaysFail(boolean fail) {
            ALWAYS_FAIL.set(fail);
        }

        @Override
        protected <T> T doExecute(Callable<T> task) throws Exception
        {
            if (Boolean.TRUE.equals(ALWAYS_FAIL.get())) {
                throw new SzException("Simulated persistent failure");
            }
            return super.doExecute(task);
        }
    }

    /**
     * The environment for this test.
     */
    private MockEnvironment env = null;

    @BeforeAll
    public void initializeEnvironment() {
        this.beginTests();
        this.initializeTestEnvironment();
        String settings     = this.getRepoSettings();
        String instanceName = this.getClass().getSimpleName();
        this.env = new MockEnvironment(instanceName, settings);
    }

    @AfterAll
    public void teardownEnvironment() {
        try {
            if (this.env != null) {
                this.env.destroy();
                this.env = null;
            }
            this.teardownTestEnvironment();
        } finally {
            this.endTests();
        }
    }

    /**
     * Tests that a persistent failure during a {@code @SzConfigRetryable}
     * method results in an {@link SzException} rather than a
     * {@link StackOverflowError} from infinite recursion between
     * {@code execute()} and {@code ensureConfigCurrent()}.
     */
    @Test
    @Order(10)
    public void testNoRecursionOnPersistentFailure() throws Exception {
        SzEngine engine = null;
        try {
            engine = this.env.getEngine();
        } catch (SzException e) {
            fail("Failed to get engine", e);
        }

        final SzEngine eng = engine;

        // capture stderr so the expected stack trace from execute()'s
        // e.printStackTrace() does not appear in the build output
        SystemErr systemErr = new SystemErr();
        systemErr.execute(() -> {
            // enable persistent failures so every doExecute() throws
            this.env.setAlwaysFail(true);
            try {
                // addRecord is @SzConfigRetryable -- the proxy sets
                // CONFIG_RETRY_FLAG=true, so execute() will attempt
                // config retry on failure.  With persistent failures,
                // the nested calls inside ensureConfigCurrent() also
                // fail.  Before the fix this caused infinite recursion.
                eng.addRecord(SzRecordKey.of("TEST", "REC1"),
                              "{ \"NAME_FULL\": \"Test\" }");

                fail("Expected SzException from persistent failure");

            } catch (SzException e) {
                // expected -- the ENSURING_CONFIG guard prevents recursion

            } catch (StackOverflowError e) {
                fail("Infinite recursion between execute() and "
                     + "ensureConfigCurrent() -- StackOverflowError thrown");

            } finally {
                this.env.setAlwaysFail(false);
            }
        });

        // verify the captured stderr contains the expected stack trace
        // from the config refresh failure (printed by execute())
        String errOutput = systemErr.getText();
        assertTrue(errOutput.contains("Simulated persistent failure"),
                   "Expected stderr to contain the simulated failure "
                   + "stack trace from ensureConfigCurrent(), got: "
                   + errOutput);
    }
}
