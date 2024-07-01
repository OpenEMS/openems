package io.openems.edge.common.type;

public record Tuple<A, B>(A a, B b) {

	/**
	 * Factory for a {@link Tuple}.
	 * 
	 * @param <A> Type of a
	 * @param <B> Type of b
	 * @param a   value a
	 * @param b   value b
	 * @return a new Tuple
	 */
	public static <A, B> Tuple<A, B> of(A a, B b) {
		return new Tuple<>(a, b);
	}

}
