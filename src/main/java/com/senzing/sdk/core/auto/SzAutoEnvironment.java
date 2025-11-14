package com.senzing.sdk.core.auto;

import java.time.Duration;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;

import com.senzing.sdk.SzConfigRetryable;
import com.senzing.sdk.SzEnvironment;
import com.senzing.sdk.SzRetryableException;
import com.senzing.sdk.core.auto.SzAutoCoreEnvironment.RefreshMode;

/**
 * Provides an interface for the enhanced functionality provided by
 * {@link SzAutoCoreEnvironment} so that the functionality may be
 * proxied or otherwise implemented.
 */
public interface SzAutoEnvironment extends SzEnvironment {
    /**
     * Gets the concurrency with which this instance was initialized.
     * 
     * <p>
     * The value returned will be zero (0) if the thread pool has been
     * disabled, otherwise it will be the number of threads in the 
     * pool.
     * </p>
     * 
     * @return The number of threads in the thread pool for the internal
     *         {@link ExecutorService}, or zero (0) if threading is disabled.
     */
    int getConcurrency();

    /**
     * Gets the maximum number of basic retries that will be 
     * attempted when a Senzing Core SDK operation fails with an
     * {@link SzRetryableException}.
     * 
     * <p>
     * See {@link SzAutoCoreEnvironment.Initializer#getMaxBasicRetries()} for a
     * description of how the maximum is applied.
     * </p>
     * 
     * @return The maximum number of basic retries that will be 
     *         attempted when a Senzing Core SDK operation fails 
     *         with an {@link SzRetryableException}.
     */
    int getMaxBasicRetries();

    /**
     * Gets the total number of times the configuration was automatically
     * refreshed either periodically or due to an exception on a method
     * annotated with {@link SzConfigRetryable}.
     *  
     * <p>
     * <b>NOTE:</b> This does <b>NOT</b> include explicit calls to 
     * {@link #reinitialize(long)}.
     * </p>
     * 
     * @return The total number of times the configuration was automatically
     *         refreshed
     */
    int getConfigRefreshCount();

    /**
     * Gets the total number Senzing Core SDK method invocations 
     * that initially failed and were retried whether or not they
     * ultimately succeeded.
     *  
     * @return The total number Senzing Core SDK method invocations
     *         that initially failed and were retried.
     */
    int getRetriedCount();

    /**
     * Gets the total number Senzing Core SDK method invocations 
     * that initially failed, were retried at least once and
     * ultimately failed.
     * 
     * @return The total number Senzing Core SDK method invocations
     *         that initially failed, were retried and ultimately failed.
     */
    int getRetriedFailureCount();

    /**
     * Gets the {@link RefreshMode} describing how this instance will 
     * handle refreshing the configuration (or not refreshing it).  The
     * mode is set based on the value for the {@linkplain 
     * SzAutoCoreEnvironment.Builder#getConfigRefreshPeriod() configuration
     * refresh period} provided to the {@link SzAutoCoreEnvironment.Builder}
     * via {@link SzAutoCoreEnvironment.Builder#configRefreshPeriod(Duration)}.
     * 
     * @return The {@link RefreshMode} describing how this instance will
     *         handle refreshing the configuration (or not).
     * 
     * @see #getConfigRefreshPeriod()
     * @see SzAutoCoreEnvironment.Builder#configRefreshPeriod(Duration)
     * @see SzAutoCoreEnvironment.Builder#getConfigRefreshPeriod()
     * @see RefreshMode
     */
    RefreshMode getConfigRefreshMode();

    /**
     * Gets the {@link Duration} for the {@linkplain 
     * SzAutoCoreEnvironment.Builder#getConfigRefreshPeriod()
     * configuration refresh period}.
     * 
     * @return The {@link Duration} for the {@linkplain 
     *         SzAutoCoreEnvironment.Builder#getConfigRefreshPeriod() 
     *         configuration refresh period}, or <code>null</code> 
     *         if configuration refresh {@linkplain RefreshMode#DISABLED
     *         disabled}.
     * 
     * @see #getConfigRefreshMode()
     * @see SzAutoCoreEnvironment.Builder#configRefreshPeriod(Duration)
     * @see SzAutoCoreEnvironment.Builder#getConfigRefreshPeriod()
     * @see RefreshMode
     */
    Duration getConfigRefreshPeriod();

    /**
     * Performs the specified task using this instance's configured 
     * thread pool and internal {@link ExecutorService} via
     * {@link ExecutorService#submit(Callable)} or directly
     * executes the task in the calling thread if the thread pool 
     * has been disabled.
     * 
     * <p>
     * This returned {@link Future} will provide the result of the
     * task via {@link Future#get()} upon successful completion.
     * </p>
     * 
     * <p>
     * <b>NOTE:</b> Any Senzing Core SDK calls made using this 
     * {@link SzAutoEnvironment} instance will also be run in
     * the same thread with no additional context switching.
     * </p>
     * 
     * @param <T> The return type of the task.
     * @param task The {@link Callable} task to perform.
     * @return A {@link Future} representing the result of the
     *         result of the task.
     */
    <T> Future<T> submitTask(Callable<T> task);
    
    /**
     * Performs the specified task using this instance's configured 
     * thread pool and internal {@link ExecutorService} via 
     * {@link ExecutorService#submit(Runnable)} or directly
     * executes the task in the calling thread if the thread pool 
     * has been disabled.
     * 
     * <p>
     * This returned {@link Future} will provide a <code>null</code>
     * value via {@link Future#get()} upon successful completion.
     * </p>
     * 
     * <p>
     * <b>NOTE:</b> Any Senzing Core SDK calls made using this 
     * {@link SzAutoEnvironment} instance will also be run in 
     * the same thread with no additional context switching.
     * </p>
     * 
     * @param task The {@link Runnable} task to perform.
     * @return A {@link Future} representing the result of the
     *         result of the task that will provide the value
     *         <code>null</code> upon successful completion.
     */
    Future<?> submitTask(Runnable task);

    /**
     * Performs the specified task using this instance's configured 
     * thread pool and internal {@link ExecutorService} via 
     * {@link ExecutorService#submit(Runnable, Object)} or directly
     * executes the task in the calling thread if the thread pool 
     * has been disabled.
     * 
     * <p>
     * This returned {@link Future} will provide the specified result
     * value via {@link Future#get()} upon successful completion.
     * </p>
     * 
     * <p>
     * <b>NOTE:</b> Any Senzing Core SDK calls made using this 
     * {@link SzAutoEnvironment} instance will also be run in the
     * same thread with no additional context switching.
     * </p>
     * 
     * @param <T> The return type of the task.
     * @param task The {@link Callable} task to perform.
     * @param result The result to return from the returned 
     *               {@link Future}.
     * @return A {@link Future} representing the result of the
     *         result of the task.
     */
    <T> Future<T> submitTask(Runnable task, T result);    
}
