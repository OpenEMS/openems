package io.openems.backend.metadata.odoo;

import static java.util.stream.Collectors.joining;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;

import io.openems.common.function.ThrowingRunnable;
import io.openems.common.function.ThrowingSupplier;
import io.openems.common.types.DebugMode;
import io.openems.common.utils.ThreadPoolUtils;

public class DebugExecutor implements ExecutorService {

	private static final Function<String, AtomicInteger> ATOMIC_INTEGER_PROVIDER = //
			key -> new AtomicInteger(0);

	private static final String UNDEFINED_TASK_ID = "UNDEFINED";

	private final ThreadPoolExecutor executor;
	private final ConcurrentHashMap<String, AtomicInteger> activeTasks = new ConcurrentHashMap<>(100);
	private final ConcurrentHashMap<String, AtomicInteger> taskCounter = new ConcurrentHashMap<>(100);

	public DebugExecutor(ThreadPoolExecutor executor) {
		super();
		this.executor = executor;
	}

	@Override
	public void execute(Runnable command) {
		this.submit(UNDEFINED_TASK_ID, command::run);
	}

	@Override
	public void shutdown() {
		this.executor.shutdown();
	}

	@Override
	public List<Runnable> shutdownNow() {
		return this.executor.shutdownNow();
	}

	@Override
	public boolean isShutdown() {
		return this.executor.isShutdown();
	}

	@Override
	public boolean isTerminated() {
		return this.executor.isTerminated();
	}

	@Override
	public boolean awaitTermination(long timeout, TimeUnit unit) throws InterruptedException {
		return this.executor.awaitTermination(timeout, unit);
	}

	/**
	 * Execute a {@link ThrowingRunnable} and tracks the id of the task.
	 *
	 * @param <E>     the type of the exception
	 * @param id      the identifier for this type of command
	 * @param command the {@link ThrowingRunnable}
	 * @return a future
	 */
	public <E extends Exception> CompletableFuture<Void> submit(String id, ThrowingRunnable<E> command) {
		return this.submit(id, () -> {
			command.run();
			return null;
		});
	}

	/**
	 * Execute a {@link ThrowingSupplier} and tracks the id of the task.
	 * 
	 * @param <T>     the result
	 * @param <E>     the type of the exception
	 * @param id      the identifier for this type of command
	 * @param command the {@link ThrowingSupplier}
	 * @return a future with the result
	 */
	public <T, E extends Exception> CompletableFuture<T> submit(String id, ThrowingSupplier<T, E> command) {
		this.activeTasks.computeIfAbsent(id, ATOMIC_INTEGER_PROVIDER).incrementAndGet();
		this.taskCounter.computeIfAbsent(id, ATOMIC_INTEGER_PROVIDER).incrementAndGet();
		return CompletableFuture.supplyAsync(() -> {
			try {
				return command.get();
			} catch (Exception e) {
				throw new CompletionException(e);
			} finally {
				this.activeTasks.computeIfPresent(id, (t, u) -> u.decrementAndGet() <= 0 ? null : u);
			}
		}, this.executor);
	}

	@Override
	public <T> Future<T> submit(Callable<T> task) {
		return this.submit(UNDEFINED_TASK_ID, task::call);
	}

	@Override
	public <T> Future<T> submit(Runnable task, T result) {
		return this.submit(UNDEFINED_TASK_ID, () -> {
			task.run();
			return result;
		});
	}

	@Override
	public Future<Void> submit(Runnable task) {
		return this.submit(UNDEFINED_TASK_ID, task::run);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks) throws InterruptedException {
		return this.executor.invokeAll(tasks);
	}

	@Override
	public <T> List<Future<T>> invokeAll(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException {
		return this.executor.invokeAll(tasks, timeout, unit);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
		return this.executor.invokeAny(tasks);
	}

	@Override
	public <T> T invokeAny(Collection<? extends Callable<T>> tasks, long timeout, TimeUnit unit)
			throws InterruptedException, ExecutionException, TimeoutException {
		return this.executor.invokeAny(tasks, timeout, unit);
	}

	/**
	 * Gets a debug log.
	 * 
	 * @param debugMode the {@link DebugMode}
	 * @return the log as a {@link String}
	 */
	public String debugLog(DebugMode debugMode) {
		var b = new StringBuilder(ThreadPoolUtils.debugLog(this.executor));

		if (debugMode == DebugMode.DETAILED) {
			b.append(", Active-Tasks: ");

			b.append(this.activeTasks.entrySet().stream() //
					.map(t -> t.getKey() + ":" + t.getValue().get()) //
					.collect(joining(", ")));

			b.append(", Sum-Tasks: ");

			b.append(this.taskCounter.entrySet().stream() //
					.map(t -> t.getKey() + ":" + t.getValue().get()) //
					.collect(joining(", ")));
		}

		return b.toString();
	}

	/**
	 * Gets debug metrics of this executor.
	 * 
	 * @return the metrics
	 */
	public Map<String, Long> debugMetrics() {
		return ThreadPoolUtils.debugMetrics(this.executor);
	}

}
