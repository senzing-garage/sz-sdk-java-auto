package com.senzing.sdk.core.auto;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;
import java.util.concurrent.Future;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.TestInstance.Lifecycle;

/**
 * Regression test for the deadlock that occurred when a task submitted via
 * {@link SzAutoCoreEnvironment#submitTask(java.util.concurrent.Callable)}
 * itself invoked an SDK operation.
 *
 * <p>
 * Every SDK operation routes through {@code doExecute()}, which submitted
 * a task to the fixed-size thread pool and blocked on its {@link Future}.
 * A user task already running on a pool thread therefore consumed a
 * thread while waiting on another task of the same pool. Once every
 * thread was doing so the nested tasks could never be scheduled, and
 * the pool was wedged with no timeout and no error. Worse, the wedged
 * operations hold read locks, so {@code destroy()} then blocked forever
 * acquiring the write lock -- the environment could not even be shut down.
 *
 * <p>
 * This test deliberately asserts the <em>mechanism</em> rather than the
 * symptom. Reproducing the deadlock would hang the build rather than fail it,
 * since neither the pool nor {@code destroy()} can recover. Instead it submits
 * a single task to a two-thread pool, which cannot deadlock because a thread is
 * always free, and verifies that the nested SDK dispatch runs on the
 * <em>same</em> thread as the task that triggered it.
 *
 * <p>
 * That is deterministic in both directions. With the fix the nested call
 * executes inline, so the threads are identical. Without it the calling thread
 * is blocked waiting, so the pool is obliged to run the nested task on its
 * other thread and the threads necessarily differ.
 */
@TestInstance(Lifecycle.PER_CLASS)
@Execution(ExecutionMode.SAME_THREAD)
public class PoolReentrancyTest extends AbstractAutoCoreTest
{
    /**
     * The size of the thread pool under test. This must be greater than one so
     * that a nested task has somewhere to run if the fix regresses, letting the
     * test fail rather than hang.
     */
    private static final int CONCURRENCY = 2;

    /**
     * How long to allow for the task to complete. The work takes milliseconds;
     * this is generous purely to tolerate a loaded CI runner, and exists so a
     * regression can never block the build indefinitely.
     */
    private static final long TIMEOUT_SECONDS = 30L;

    @BeforeAll
    public void initializeEnvironment()
    {
        this.beginTests();
        this.initializeTestEnvironment();
    }

    @AfterAll
    public void teardownEnvironment()
    {
        try {
            this.teardownTestEnvironment();
        } finally {
            this.endTests();
        }
    }

    /**
     * Verifies that an SDK dispatch made from within a submitted task runs on
     * the calling pool thread rather than being nested onto another.
     */
    @Test
    public void testNestedDispatchRunsOnTheCallingPoolThread()
    {
        this.performTest(() -> {
            SzAutoCoreEnvironment env = null;
            try {
                env = SzAutoCoreEnvironment
                    .newAutoBuilder()
                    .settings(this.getRepoSettings())
                    .instanceName(this.getInstanceName("PoolReentrancy"))
                    .verboseLogging(false)
                    .concurrency(CONCURRENCY)
                    .build();

                final SzAutoCoreEnvironment environment = env;

                // capture the pool thread running the task, and the thread
                // on which a nested SDK dispatch from it actually executes
                Future<Thread[]> future = environment.submitTask(() -> {
                    Thread outer = Thread.currentThread();

                    Thread inner = environment.execute(
                        () -> Thread.currentThread());

                    return new Thread[] { outer, inner };
                });

                Thread[] threads = future.get(TIMEOUT_SECONDS, SECONDS);

                assertSame(
                    threads[0],
                    threads[1],
                    "A nested SDK dispatch ran on a different pool thread "
                    + "than the task that made it. The calling thread "
                    + "blocks waiting on that nested task, so once "
                    + "every pool thread does this the pool deadlocks "
                    + "and destroy() can no longer acquire its write "
                    + "lock.");
            } catch (Exception e) {
                fail("Unexpectedly failed dispatching from a pool thread", e);
            } finally {
                if (env != null) {
                    env.destroy();
                }
            }
        });
    }
}
