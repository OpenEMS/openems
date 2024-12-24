package io.openems.common.utils;

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

	private FunctionUtils() {
	}

}
