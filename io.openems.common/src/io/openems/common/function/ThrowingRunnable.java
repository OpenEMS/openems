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

	/**
	 * When an object implementing interface <code>Runnable</code> is used to create
	 * a thread, starting the thread causes the object's <code>run</code> method to
	 * be called in that separately executing thread.
	 *
	 * <p>
	 * The general contract of the method <code>run</code> is that it may take any
	 * action whatsoever.
	 *
	 * @see java.lang.Thread#run()
	 * @throws E on error
	 */
	public void run() throws E;

}
