package io.openems.common.utils;

import java.util.Objects;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Supplier;

import io.openems.common.function.ThrowingBiConsumer;
import io.openems.common.function.ThrowingConsumer;
import io.openems.common.function.ThrowingRunnable;

public final class FunctionUtils {

	/**
	 * Helper method to create empty {@link Runnable} or {@link ThrowingRunnable}.
	 * 
	 * <p>
	 * Usage:
	 * 
	 * <pre>
	 * final Runnable runnable = FunctionUtils::doNothing;
	 * 
	 * final ThrowingRunnable&lt;Exception&gt; runnable = FunctionUtils::doNothing;
	 * </pre>
	 */
	public static void doNothing() {

	}

	/**
	 * Helper method to create empty {@link Consumer} or {@link ThrowingConsumer}.
	 * 
	 * <p>
	 * Usage:
	 * 
	 * <pre>
	 * final Consumer&lt;String&gt; consumer = FunctionUtils::doNothing;
	 * 
	 * final ThrowingConsumer&lt;String, Exception&gt; consumer = FunctionUtils::doNothing;
	 * </pre>
	 * 
	 * @param <T> the type of the first input parameter
	 * @param t   the first input parameter
	 */
	public static <T> void doNothing(T t) {

	}

	/**
	 * Helper method to create empty {@link BiConsumer} or
	 * {@link ThrowingBiConsumer}.
	 * 
	 * <p>
	 * Usage:
	 * 
	 * <pre>
	 * final BiConsumer&lt;String&gt; consumer = FunctionUtils::doNothing;
	 * 
	 * final ThrowingBiConsumer&lt;String, Exception&gt; consumer = FunctionUtils::doNothing;
	 * </pre>
	 * 
	 * @param <T> the type of the first input parameter
	 * @param <U> the type of the second input parameter
	 * @param t   the first input parameter
	 * @param e   the second input parameter
	 */
	public static <T, U> void doNothing(T t, U e) {

	}

	/**
	 * Helper method to create a {@link Supplier}.
	 * 
	 * <p>
	 * Instead of an explicit type:
	 * 
	 * <pre>
	 * final Supplier&lt;String&gt; provideString = () -> {
	 * 	return "";
	 * };
	 * </pre>
	 * 
	 * <p>
	 * ... an implicit type based on the return value can be used:
	 * 
	 * <pre>
	 * final var provideString = supplier(() -> {
	 * 	return "";
	 * });
	 * </pre>
	 * 
	 * @param <T>      the type of results supplied by the created supplier
	 * @param supplier the created {@link Supplier}
	 * @return the created {@link Supplier}
	 */
	public static <T> Supplier<T> supplier(Supplier<T> supplier) {
		return supplier;
	}

	/**
	 * Returns a {@link Supplier} that lazily initializes and caches the value from
	 * the provided supplier. The value is computed and retrieved only once.
	 * Subsequent calls to {@link Supplier#get()} will return the cached value,
	 * avoiding recomputation.
	 * <p>
	 * This implementation is not thread-safe. If multiple threads invoke
	 * {@link Supplier#get()} concurrently, it may lead to inconsistent behavior,
	 * such as multiple invocations of the supplier or the value being computed
	 * multiple times.
	 * </p>
	 *
	 * <p>
	 * Example usage:
	 * </p>
	 * 
	 * <pre>
	 * var lazyStringSupplier = lazySingletion(() -> {
	 * 	System.out.println("Computing the value...");
	 * 	return "Hello, World!";
	 * });
	 * 
	 * System.out.println(lazyStringSupplier.get()); // Computes and prints the value
	 * System.out.println(lazyStringSupplier.get()); // Prints the cached value, no recomputation
	 * </pre>
	 *
	 * @param supplier The original supplier that provides the value to be lazily
	 *                 computed.
	 * @param <T>      The type of the value that the supplier produces.
	 * @return A {@link Supplier} that returns the cached value after the first
	 *         computation.
	 * @throws NullPointerException if the provided supplier is {@code null}.
	 *
	 * @see Supplier
	 */
	public static <T> Supplier<T> lazySingleton(Supplier<T> supplier) {
		Objects.requireNonNull(supplier);
		return new Supplier<T>() {

			private boolean isInitialized = false;
			private T value;

			@Override
			public T get() {
				if (!this.isInitialized) {
					this.value = supplier.get();
					this.isInitialized = true;
				}
				return this.value;
			}
		};
	}

	private FunctionUtils() {
	}

}
