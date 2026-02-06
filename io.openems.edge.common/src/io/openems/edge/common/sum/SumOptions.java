package io.openems.edge.common.sum;

public interface SumOptions {

	/**
	 * Whether or not to add the values of this meter to Sum.
	 *
	 * <p>
	 * Use case: there are two production meters which should be joined to one
	 * virtual meter. In this case you would not want the meters to be added to the
	 * total sum of Production power ("_sum/ActiveProductionPower").
	 *
	 * @return true if it should be added to sum.
	 */
	default boolean addToSum() {
		return false;
	}

}
