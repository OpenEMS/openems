package io.openems.common.utils;

import java.util.function.Supplier;

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
