package io.openems.common.function;

/**
 * This interface is similar to the java.util interface {@link Runnable}.
 * Difference is, that it allows the run() method to throw a defined
 * {@link Exception}.
 * 
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ThrowingRunnable<E extends Exception> {

	public void run() throws E;

}
