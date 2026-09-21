package io.openems.common.types;

import java.util.function.BiFunction;
import java.util.function.Function;

/**
 * An immutable container holding two values (a 2-tuple).
 *
 * <p>
 * This class is a generic, type-safe tuple implementation using Java records.
 * It provides functional-style operations such as mapping, combining, and
 * transforming its elements.
 *
 * <p>
 * Instances of this class are immutable and thread-safe.
 *
 * @param <A> the type of the first element (a)
 * @param <B> the type of the second element (b)
 */
public record Tuple2<A, B>(A a, B b) {

	/**
	 * Creates a new {@code Tuple} instance.
	 *
	 * @param <A> the type of the first value
	 * @param <B> the type of the second value
	 * @param a   the first value
	 * @param b   the second value
	 * @return a new {@code Tuple} containing the given values
	 */
	public static <A, B> Tuple2<A, B> of(A a, B b) {
		return new Tuple2<>(a, b);
	}

	/**
	 * Returns a new {@code Tuple} with the first element replaced.
	 *
	 * @param newA the new first value
	 * @return a new {@code Tuple} with the updated first value
	 */
	public Tuple2<A, B> withA(A newA) {
		return new Tuple2<>(newA, this.b);
	}

	/**
	 * Returns a new {@code Tuple} with the second element replaced.
	 *
	 * @param newB the new second value
	 * @return a new {@code Tuple} with the updated second value
	 */
	public Tuple2<A, B> withB(B newB) {
		return new Tuple2<>(this.a, newB);
	}

	/**
	 * Transforms the first element using the given mapping function.
	 *
	 * @param <R>    the resulting type of the first element
	 * @param mapper a function to transform the first element
	 * @return a new {@code Tuple} with the transformed first element
	 * @throws NullPointerException if {@code mapper} is {@code null}
	 */
	public <R> Tuple2<R, B> mapA(Function<? super A, ? extends R> mapper) {
		return new Tuple2<>(mapper.apply(this.a), this.b);
	}

	/**
	 * Transforms the second element using the given mapping function.
	 *
	 * @param <R>    the resulting type of the second element
	 * @param mapper a function to transform the second element
	 * @return a new {@code Tuple} with the transformed second element
	 * @throws NullPointerException if {@code mapper} is {@code null}
	 */
	public <R> Tuple2<A, R> mapB(Function<? super B, ? extends R> mapper) {
		return new Tuple2<>(this.a, mapper.apply(this.b));
	}

	/**
	 * Transforms both elements using the provided mapping functions.
	 *
	 * @param <R>  the resulting type of the first element
	 * @param <S>  the resulting type of the second element
	 * @param mapA a function to transform the first element
	 * @param mapB a function to transform the second element
	 * @return a new {@code Tuple} containing the transformed elements
	 * @throws NullPointerException if any mapper is {@code null}
	 */
	public <R, S> Tuple2<R, S> map(//
	                               Function<? super A, ? extends R> mapA, //
	                               Function<? super B, ? extends S> mapB //
	) {
		return new Tuple2<>(mapA.apply(this.a), mapB.apply(this.b));
	}

	/**
	 * Combines this tuple with another tuple using the provided combining
	 * functions.
	 *
	 * <p>
	 * The resulting tuple is constructed by applying the given functions to the
	 * corresponding elements of both tuples.
	 *
	 * @param <C>      the resulting type of the first element
	 * @param <D>      the resulting type of the second element
	 * @param other    the other tuple to combine with
	 * @param combineA a function to combine the first elements
	 * @param combineB a function to combine the second elements
	 * @return a new {@code Tuple} containing the combined values
	 * @throws NullPointerException if any argument is {@code null}
	 */
	public <C, D> Tuple2<C, D> combine(//
	                                   Tuple2<? extends A, ? extends B> other, //
	                                   BiFunction<? super A, ? super A, ? extends C> combineA, //
	                                   BiFunction<? super B, ? super B, ? extends D> combineB //
	) {
		return new Tuple2<>(combineA.apply(this.a, other.a), combineB.apply(this.b, other.b));
	}

	/**
	 * Returns a new {@code Tuple} with the elements swapped.
	 *
	 * @return a new {@code Tuple} where {@code a} becomes {@code b} and vice versa
	 */
	public Tuple2<B, A> swap() {
		return new Tuple2<>(this.b, this.a);
	}

	/**
	 * Applies a function to both elements of this tuple.
	 *
	 * @param <R> the result type
	 * @param fn  a function that consumes both elements
	 * @return the result of applying the function
	 * @throws NullPointerException if {@code fn} is {@code null}
	 */
	public <R> R apply(BiFunction<? super A, ? super B, ? extends R> fn) {
		return fn.apply(this.a, this.b);
	}

}