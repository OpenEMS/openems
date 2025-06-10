package io.openems.common.function;

/**
 * This interface is similar to the java.util interface
 * {@link ThrowingBiFunction}. Difference is, that it allows to pass to the
 * apply() method one more parameter.
 *
 * @param <T> the apply methods first argument type
 * @param <U> the apply methods second argument type
 * @param <S> the apply methods third argument type
 * @param <R> the type of the result of the function
 * @param <E> the exception type
 */
@FunctionalInterface
public interface ThrowingTriFunction<T, U, S, R, E extends Exception> {

	/**
	 * Applies this function to the given arguments.
	 *
	 * @param t the first function argument
	 * @param u the second function argument
	 * @param s the third function argument
	 * @return the function result
	 * @throws E on error
	 */
	public R apply(T t, U u, S s) throws E;

}
