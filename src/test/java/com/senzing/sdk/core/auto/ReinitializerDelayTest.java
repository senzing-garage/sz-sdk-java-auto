package com.senzing.sdk.core.auto;

import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import java.time.Duration;
import java.util.List;
import static org.junit.jupiter.api.TestInstance.Lifecycle;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression test for the refresh period that silently disabled proactive
 * configuration refresh.
 *
 * <p>
 * {@link Reinitializer} converted the configured {@link Duration} to
 * milliseconds with two flooring operations -- whole seconds, plus integer
 * division of the nanoseconds by one million. A positive {@link Duration} of
 * under one millisecond therefore truncated to zero, and it passed the
 * {@code isZero()} guard because it genuinely is not zero. The resulting
 * {@code wait(0)} has no timeout, so the thread parked until notified and
 * the configuration was never refreshed again for the life of the environment.
 */
@TestInstance(Lifecycle.PER_CLASS)
public class ReinitializerDelayTest
{
    /**
     * Provides durations paired with the number of milliseconds the
     * reinitializer should wait for them.
     *
     * @return The {@link List} of {@link Arguments} for the parameters.
     */
    public List<Arguments> getDelayParameters()
    {
        return List.of(
            // sub-millisecond durations must never floor to zero
            Arguments.of(Duration.ofNanos(1L), 1L),
            Arguments.of(Duration.ofNanos(500000L), 1L),
            Arguments.of(Duration.ofNanos(999999L), 1L),

            // exact and above-millisecond durations convert directly
            Arguments.of(Duration.ofMillis(1L), 1L),
            Arguments.of(Duration.ofMillis(1500L), 1500L),
            Arguments.of(Duration.ofSeconds(1L), 1000L),
            Arguments.of(Duration.ofSeconds(30L), 30000L),

            // the seconds and nanoseconds terms are combined
            Arguments.of(Duration.ofSeconds(2L).plusMillis(250L), 2250L),
            Arguments.of(Duration.ofSeconds(1L).plusNanos(999999L), 1000L));
    }

    @ParameterizedTest
    @MethodSource("getDelayParameters")
    public void testDelayMillis(Duration duration, long expected)
    {
        long actual = Reinitializer.delayMillis(duration);

        assertEquals(expected, actual,
                     "The delay for " + duration + " is not as expected.");

        assertTrue(
            actual > 0L,
            "The delay for " + duration + " must be positive, since "
            + "wait(0) would block until notified rather than time " + "out: "
            + actual);
    }
}
