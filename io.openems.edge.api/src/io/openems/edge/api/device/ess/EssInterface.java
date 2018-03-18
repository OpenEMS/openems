package io.openems.edge.api.device.ess;

import java.util.Optional;

import org.osgi.annotation.versioning.ProviderType;

import io.openems.edge.api.device.DeviceInterface;


@ProviderType
public interface EssInterface extends DeviceInterface{
	
	/**
	 * Get the current State of Charge in percent(0-100)
	 * @return the State of Charge or Optional.empty(), if no value available
	 */
	Optional<Short> getSoc();
	/**
	 * Get the energy the battery is containing in Wh
	 * @return the charged energy or Optional.empty(), if no value available
	 */
	Optional<Long> getEnergy();
	/**
	 * Get the energy, the battery can charge, if completely empty, in Wh 
	 * @return the capacity or Optional.empty(), if no value available 
	 */
	Optional<Long> getCapacity();
	/**
	 * Get the maximal apparentpower the storage can provide in VA
	 * @return the nominalpower or Optional.empty(), if no value available
	 */
	Optional<Long> getNominalPower();
	/**
	 * Get the allowed power to charge the battery at the moment in W
	 * @return the allowedcharge or Optional.empty(), if no value available
	 */
	Optional<Long> getAllowedCharge();
	/**
	 * Get the allowed power to discharge the battery at the moment in W
	 * @return the alloweddischarge or Optional.empty(), if no value available
	 */
	Optional<Long> getAllowedDischarge();
	/**
	 * Get the allowed apparent power the inverter can provide at the moment in VA
	 * @return the allowedapparent or Optional.empty(), if no value available
	 */
	Optional<Long> getAllowedApparent();
	/**
	 * Returns true, if the battery is fully loaded
	 * @return
	 */
	boolean isFull();
	/**
	 * Returns true, if the battery is empty
	 * @return
	 */
	boolean isEmpty();
	
}
