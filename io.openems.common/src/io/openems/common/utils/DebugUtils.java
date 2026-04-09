package io.openems.common.utils;

import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Supplier;

import org.slf4j.Logger;

import com.google.common.base.Stopwatch;

import io.openems.common.function.ThrowingRunnable;
import io.openems.common.function.ThrowingSupplier;

public final class DebugUtils {

	/**
	 * Measures the execution time of a {@link Runnable} and prints it to the
	 * console.
	 *
	 * @param <E>      the type of the exception
	 * @param runnable the {@link Runnable} to measure
	 */
	public static <E extends Exception> void measure(ThrowingRunnable<E> runnable) throws E {
		measure(getNameOfCallingMethod(), runnable);
	}

	/**
	 * Measures the execution time of a {@link Runnable} and prints it to the
	 * console.
	 *
	 * @param <E>      the type of the exception
	 * @param name     the name to print
	 * @param runnable the {@link Runnable} to measure
	 */
	public static <E extends Exception> void measure(String name, ThrowingRunnable<E> runnable) throws E {
		measure(name, () -> {
			runnable.run();
			return null;
		});
	}

	/**
	 * Measures the execution time of a {@link Supplier} and prints it to the
	 * console.
	 * 
	 * @param <T>      the type of the result
	 * @param <E>      the type of the exception
	 * @param supplier the {@link Supplier} to measure
	 * @return the result of the {@link Supplier}
	 */
	public static <T, E extends Exception> T measure(ThrowingSupplier<T, E> supplier) throws E {
		return measure(getNameOfCallingMethod(), supplier);
	}

	/**
	 * Measures the execution time of a {@link Supplier} and prints it to the
	 * console.
	 * 
	 * @param <T>      the type of the result
	 * @param <E>      the type of the exception
	 * @param name     the name to print
	 * @param supplier the {@link Supplier} to measure
	 * @return the result of the {@link Supplier}
	 */
	public static <T, E extends Exception> T measure(String name, ThrowingSupplier<T, E> supplier) throws E {
		final var start = System.currentTimeMillis();
		try {
			return supplier.get();
		} finally {
			final var end = System.currentTimeMillis();
			final var time = end - start;
			System.out.println("###################\n" + name + " took " + time + "ms\n###################");
		}
	}

	/**
	 * Measures the execution time of a {@link Runnable} and logs it using the given
	 * {@link Logger} with INFO level.
	 *
	 * @param <E>      the type of the exception
	 * @param log      the logger to use
	 * @param name     the name to print
	 * @param runnable the {@link Runnable} to measure
	 */
	public static <E extends Exception> void measure(Logger log, String name, ThrowingRunnable<E> runnable) throws E {
		measure(log, name, () -> {
			runnable.run();
			return null;
		});
	}

	/**
	 * Measures the execution time of a {@link ThrowingSupplier} and logs it using
	 * the given {@link Logger} with INFO level.
	 *
	 * @param <T>      the type of the result
	 * @param <E>      the type of the exception
	 * @param log      the logger to use
	 * @param name     the name to print
	 * @param supplier the {@link ThrowingSupplier} to measure
	 * @return the result of the {@link ThrowingSupplier}
	 */
	public static <T, E extends Exception> T measure(Logger log, String name, ThrowingSupplier<T, E> supplier)
			throws E {
		return measure(name, supplier, log::info);
	}

	/**
	 * Fully generic logger-based measurement. Caller can provide any logging
	 * function (e.g. log::info, log::debug).
	 *
	 * @param <T>      the type of the result
	 * @param <E>      the type of the exception
	 * @param name     the name to print
	 * @param supplier the {@link ThrowingSupplier} to measure
	 * @param logFn    function to log the resulting message
	 * @return the result of the {@link ThrowingSupplier}
	 */
	public static <T, E extends Exception> T measure(String name, ThrowingSupplier<T, E> supplier,
			Consumer<String> logFn) throws E {

		final var sw = Stopwatch.createStarted();
		try {
			return supplier.get();
		} finally {
			final var time = sw.elapsed(TimeUnit.MILLISECONDS);
			logFn.accept("[" + name + "] took " + time + " ms");
		}
	}

	private static String getNameOfCallingMethod() {
		final var stack = Thread.currentThread().getStackTrace();
		if (stack.length < 4) {
			return "unknown";
		}
		return stack[3].getMethodName();
	}

	private DebugUtils() {
	}
}
