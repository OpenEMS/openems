package io.openems.edge.meter.api;

/**
 * A VirtualMeter is a meter that does not exist physically.
 */
public interface VirtualMeter extends ElectricityMeter {

	/**
	 * Whether or not to add the values of this meter to Sum.
	 *
	 * <p>
	 * Use case: there are two production meters which should be joined to one
	 * virtual meter. In this case you would not want this VirtualMeter to be added
	 * to the total sum of Production power ("_sum/ActiveProductionPower").
	 *
	 * @return true if it should be added to sum.
	 */
	public boolean addToSum();

}
