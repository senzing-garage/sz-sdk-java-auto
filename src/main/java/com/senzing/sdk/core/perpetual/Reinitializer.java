package com.senzing.sdk.core.perpetual;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;

import com.senzing.sdk.SzException;

/**
 * Background thread to refresh the configuration on the 
 * {@link SzPerpetualCoreEnvironment}.
 */
class Reinitializer extends Thread {
    /**
     * Constant for converting between nanoseconds and milliseconds.
     */
    private static final long ONE_MILLION = 1000000L;

    /**
     * Constant for converting between seconds and milliseconds.
     */
    private static final long ONE_THOUSAND = 1000L;

    /**
     * The maximum number of errors before giving up on monitoring the active
     * configuration for changes.
     */
    private static final int MAX_ERROR_COUNT = 5;

    /**
     * The {@link SzPerpetualCoreEnvironment} to monitor and reinitialize.
     */
    private SzPerpetualCoreEnvironment env;
    
    /**
     * Flag indicating if the thread should complete or continue monitoring.
     */
    private boolean complete;

    /**
     * Constructs with the {@link SzPerpetualCoreEnvironment}.
     *
     * @param env The {@link SzPerpetualCoreEnvironment} to use.
     */
    Reinitializer(SzPerpetualCoreEnvironment env) {
        this.env        = env;
        this.complete   = false;
    }

    /**
     * Signals that this thread should complete execution.
     */
    synchronized void complete() {
        if (this.complete) {
            return;
        }
        this.complete = true;
        this.notifyAll();
    }

    /**
     * Checks if this thread has received the completion signal.
     *
     * @return <tt>true</tt> if the completion signal has been received, 
     *         otherwise <tt>false</tt>.
     */
    synchronized boolean isComplete() {
        return this.complete;
    }

    /**
     * The run method implemented to periodically check if the active
     * configuration ID differs from the default configuration ID 
     * and if so, reinitializes.
     */
    public void run() {
        try {
            int errorCount = 0;
            // loop until completed
            while (!this.isComplete()) {
                // check if we have reached the maximum error count
                if (errorCount > MAX_ERROR_COUNT) {
                    System.err.println(
                        "Giving up on monitoring active configuration after "
                        + errorCount + " failures");
                    return;
                }

                // get the refresh period
                Duration duration = this.env.getConfigRefreshPeriod();

                // check if zero or null (we should not really get here since 
                // this thread should not be started if the delay is zero)
                if (duration == null || duration.isZero()) {
                    this.complete();
                    continue;
                }

                // convert to milliseconds
                long delay = duration.getSeconds() * ONE_THOUSAND 
                    + (duration.getNano() / ONE_MILLION);

                try {
                    synchronized (this) {
                        // sleep for the delay period
                        this.wait(delay);
                    }
                    
                    // check if destroyed
                    if (this.env.isDestroyed()) {
                        this.complete();
                        continue;
                    }

                    // ensure the config is current
                    this.env.ensureConfigCurrent();

                } catch (InterruptedException | SzException e) {
                    errorCount++;
                    continue;
                }

                // reset the error count if we successfully reach this point
                errorCount = 0;
            }

        } catch (Exception e) {
            System.err.println(
                "Giving up on monitoring active configuration due to exception:");
            System.err.println(e.getMessage());
            System.err.println(formatStackTrace(e.getStackTrace()));

        } finally {
            this.complete();
        }
    }

    /**
     * Formats an array of {@link StackTraceElement} instances using {@link
     * #formatStackTrace(StackTraceElement)} with a single element per line.
     * 
     * @param stackTrace The array of {@link StackTraceElement} instances to format.
     * 
     * @return The formatted {@link String} describing the array of {@link 
     *         StackTraceElement} instances or <code>null</code> if the specified
     *         array is <code>null</code>.
     */
    private static String formatStackTrace(StackTraceElement[] stackTrace) {
        if (stackTrace == null) {
            return null;
        }
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        for (StackTraceElement elem : stackTrace) {
            pw.println(formatStackTrace(elem));
        }
        return sw.toString();
    }

    /**
     * Formats a single {@link StackTraceElement} in the same format as they would appear
     * in an exception stack trace.
     * 
     * @param elem The {@link StackTraceElement} to format.
     * 
     * @return The formatted {@link String} describing the {@link StackTraceElement}.
     */
    private static String formatStackTrace(StackTraceElement elem) {
        StringBuilder sb = new StringBuilder();
        sb.append("        at ");
        
        // handle a null element
        if (elem == null) {
        sb.append("[unknown: null]");
        return sb.toString();
        }

        String moduleName = elem.getModuleName();
        if (moduleName != null && moduleName.length() > 0) {
            sb.append(moduleName).append("/");
        }
        sb.append(elem.getClassName());
        sb.append(".");
        sb.append(elem.getMethodName());
        sb.append("(");
        String fileName = elem.getFileName();
        sb.append((fileName == null) ? "[unknown file]" : fileName);
        sb.append(":");
        int lineNumber = elem.getLineNumber();
        if (lineNumber < 0) {
            sb.append("[unknown line]");
        } else {
            sb.append(lineNumber);
        }
        sb.append(")");

        return sb.toString();
    }

}
