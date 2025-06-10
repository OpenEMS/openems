package io.openems.common.function;

/**
 * This interface is similar to the java.util interface
 * {@link ThrowingBiConsumer}. Difference is, that it allows to pass to the
 * apply() method one more parameter.
 *
 * @param <T> the apply methods first argument type
 * @param <U> the apply methods second argument type
 * @param <S> the apply methods third argument type
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ThrowingTriConsumer<T, U, S, E extends Exception> {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t the first function argument
	 * @param u the second function argument
	 * @param s the third function argument
	 * @throws E on error
	 */
	public void accept(T t, U u, S s) throws E;

}
