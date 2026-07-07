package io.openems.common.types;

import java.io.Serial;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * A result that is either a successful execution with the value or a failure
 * with the error-value.
 *
 * <p>
 * {@code Result}-type is created for interoperability between normal Java-code
 * that throws exception and more functional code. You do not need
 * {@code Result}-type most of the time in Java-code, where you can directly
 * throw exceptions. But there are situations, where more functional-style is
 * used. In such situations pure-functions are expected that throw no
 * exceptions. Handling exception in such situations can be cumbersome and
 * require a lot of boilerplate code. {@code Result}-type helps with exception
 * handling and allows to write idiomatic functional code that can interact with
 * methods that throw exceptions.
 *
 * <p>
 * Result can be either success or failure, but not both at the same time.
 * Enclosed successful value or error value can be of different types,
 * represented by generic type-parameters.
 *
 * <p>
 * The {@link Result#ok(Object)} and {@link Result#error(Exception)} methods
 * create a new Result-value, that is respectively either a successful result or
 * an error.
 *
 * {@snippet lang = "java" :
 * Result<String> suc = Result.ok("Hello, World!");
 * }
 *
 * <p>
 * The above line declares successful result value.
 *
 * {@snippet lang = "java" :
 * Result<String> err = Result.error(new Exception("Something went wrong"));
 * }
 *
 * <p>
 * The above line declares error-value.
 *
 * <p>
 * The {@link Result.Ok} and {@link Result.Error} records are subtypes of the
 * {@code Result}-type and allow to use pattern matching to distinguish between
 * a successful result and an error
 * {@snippet lang = "java" :
 * Result<String> result = loadFile("test.txt");
 * switch (result) {
 * case Result.Success<String>(String value) -> System.out.println(value);
 * case Result.Error<String>(Exception error) -> throw new IOException("%s: error".formatted(error.getMessage()));
 * }
 * }
 * 
 * <p>
 * In the above example we expect that the {@code loadFile} method returns the
 * {@code Result}-type instead of throwing an exception. This allows us to use
 * more functional-style, by using this method in the lambda-expression that is
 * not expected to throw any exceptions. Handling exception in such situations
 * can be cumbersome and require a lot of boilerplate code.
 *
 * @param <T> type of successful result value
 */
public sealed interface Result<T> {
	/**
	 * Creates a successful result with the given value.
	 *
	 * @param value successful result value
	 * @param <T>   Result value type
	 * @return a successful result with the given value
	 */
	public static <T> Result<T> ok(T value) {
		return new Ok<>(value);
	}

	/**
	 * Creates a error result with the given exception.
	 *
	 * @param exception exception that caused the error
	 * @param <T>       Result value type
	 * @return a error result with the given exception
	 */
	public static <T> Result<T> error(Exception exception) {
		return new Error<>(exception);
	}

	/**
	 * Creates a successful result if the Optional is present, otherwise an error
	 * result with the provided exception.
	 *
	 * @param optional the Optional to convert
	 * @param error    supplier that provides the exception if Optional is empty
	 * @param <T>      Result value type
	 * @return a successful result if Optional is present, otherwise an error result
	 */
	public static <T> Result<T> of(Optional<T> optional, Supplier<Exception> error) {
		return optional.map(Result::ok).orElseGet(() -> Result.error(error.get()));
	}

	/**
	 * Checks if the result is successful.
	 *
	 * @return true if the result is successful, false otherwise
	 */
	public boolean isOk();

	/**
	 * Checks if the result is an error.
	 *
	 * @return true if the result is an error, false otherwise
	 */
	public boolean isError();

	/**
	 * Returns the value or throws an exception if error.
	 *
	 * @return the successful value
	 * @throws ResultThrowException if the result is an error
	 */
	public T getOrThrow();

	/**
	 * Transforms the successful value with the given function.
	 *
	 * @param mapper the function to transform the value
	 * @param <N>    the type of the transformed value
	 * @return a new Result with the transformed value, or the error if present
	 */
	public <N> Result<N> map(Function<T, N> mapper);

	/**
	 * Returns the value or a default value if error.
	 *
	 * @param defaultValue the value to return if result is an error
	 * @return the successful value or the default value
	 */
	public T getOrElse(T defaultValue);

	/**
	 * Executes the consumer if the result is successful.
	 *
	 * @param consumer the consumer to execute
	 */
	public void ifSuccess(Consumer<T> consumer);

	/**
	 * Returns the value as an Optional.
	 *
	 * @return an Optional containing the value if successful, empty otherwise
	 */
	public Optional<T> get();

	/**
	 * Throws the exception if the result is an error.
	 *
	 * @throws ResultThrowException if the result is an error
	 */
	public void throwError();

	/**
	 * Chains with another Result if this result is successful.
	 *
	 * @param other the Result to chain with
	 * @return the other Result if current is successful, otherwise this error
	 *         Result
	 */
	public Result<T> andThen(Result<T> other);

	/**
	 * Chains with another Result via Supplier.
	 *
	 * @param other the Supplier that provides the Result to chain with
	 * @return the Result from the supplier if current is successful, otherwise this
	 *         error Result
	 */
	public Result<T> andThen(Supplier<Result<T>> other);

	record Ok<T>(T value) implements Result<T> {

		@Override
		public boolean isOk() {
			return true;
		}

		@Override
		public boolean isError() {
			return false;
		}

		@Override
		public T getOrThrow() {
			return this.value;
		}

		@Override
		public <N> Result<N> map(Function<T, N> mapper) {
			return Result.ok(mapper.apply(this.value));
		}

		@Override
		public T getOrElse(T defaultValue) {
			return this.value;
		}

		@Override
		public void ifSuccess(Consumer<T> consumer) {
			consumer.accept(this.value);
		}

		@Override
		public Optional<T> get() {
			return Optional.of(this.value);
		}

		@Override
		public void throwError() {
		}

		@Override
		public Result<T> andThen(Result<T> other) {
			return other;
		}

		@Override
		public Result<T> andThen(Supplier<Result<T>> other) {
			return other.get();
		}
	}

	record Error<T>(Exception exception) implements Result<T> {
		@Override
		public boolean isOk() {
			return false;
		}

		@Override
		public boolean isError() {
			return true;
		}

		@Override
		public T getOrThrow() {
			throw new ResultThrowException(this.exception);
		}

		@Override
		public <N> Result<N> map(Function<T, N> mapper) {
			return Result.error(this.exception);
		}

		@Override
		public T getOrElse(T defaultValue) {
			return defaultValue;
		}

		@Override
		public void ifSuccess(Consumer<T> consumer) {
		}

		@Override
		public Optional<T> get() {
			return Optional.empty();
		}

		@Override
		public void throwError() {
			throw new ResultThrowException(this.exception);
		}

		@Override
		public Result<T> andThen(Result<T> other) {
			return this;
		}

		@Override
		public Result<T> andThen(Supplier<Result<T>> other) {
			return this;
		}
	}

	public static class ResultThrowException extends RuntimeException {
		@Serial
		private static final long serialVersionUID = 1L;

		public ResultThrowException(Throwable cause) {
			super(cause.getMessage(), cause);
		}
	}
}
