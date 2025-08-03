package io.openems.edge.predictor.api.mlcore.datastructures;

import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;
import java.util.function.Function;

public interface DataStructure<I> {

	/**
	 * Returns an unmodifiable view of the index list of this data structure.
	 *
	 * @return an unmodifiable list representing the index
	 */
	public List<I> getIndex();

	/**
	 * Removes all rows containing {@code null} or {@code NaN} values.
	 */
	public void dropNa();

	/**
	 * Sorts the data structure by its index using the specified comparator.
	 *
	 * @param comparator comparator to order the indices
	 */
	public void sortByIndex(Comparator<I> comparator);

	/**
	 * Returns the minimum value in this data structure, if present.
	 *
	 * @return an {@code OptionalDouble} containing the minimum value, or empty if
	 *         there are no values
	 */
	public OptionalDouble min();

	/**
	 * Returns the maximum value in this data structure, if present.
	 *
	 * @return an {@code OptionalDouble} containing the maximum value, or empty if
	 *         there are no values
	 */
	public OptionalDouble max();

	/**
	 * Applies the given function to each value in the data structure.
	 * 
	 * <p>
	 * {@code null} values are not passed to the function.
	 *
	 * @param function the function to apply to each non-null value
	 */
	public void apply(Function<Double, Double> function);

	/**
	 * Returns a deep copy of this data structure. Modifications to the copy do not
	 * affect the original.
	 *
	 * @return an independent copy
	 */
	public DataStructure<I> copy();
}
