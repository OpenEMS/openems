package io.openems.edge.levl.controller.common;

public class Efficiency {
	
	/**
	 * Applies an efficiency to a power/energy outside of the battery. 
	 * negative values for Charge; positive for Discharge
	 * 
	 * @param value power/energy to which the efficiency should be applied
	 * @param efficiencyPercent efficiency which should be applied
	 * @return the power/energy inside the battery after applying the efficiency
	 */
	public static long apply(long value, double efficiencyPercent) {		
		if (value <= 0) { // charge
			return Math.round(value * efficiencyPercent / 100);
		}
		
		// discharge
		return Math.round(value / (efficiencyPercent / 100));
	}
	
	/**
	 * Unapplies an efficiency to a power/energy inside of the battery. 
	 * negative values for Charge; positive for Discharge
	 * 
	 * @param value power/energy to which the efficiency should be applied
	 * @param efficiencyPercent efficiency which should be applied
	 * @return the power/energy outside the battery after unapplying the efficiency
	 */
	public static long unapply(long value, double efficiencyPercent) {		
		if (value <= 0) { // charge
			return Math.round(value / (efficiencyPercent / 100));
		}
		
		// discharge
		return Math.round(value * efficiencyPercent / 100);
	}
	
}