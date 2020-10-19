package io.openems.edge.controller.evcs;

import java.time.LocalDateTime;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.evcs.api.ManagedEvcs;

public class ChargingLowerThanTargetHandler {

	/**
	 * The set charge limit is compared with the actual charge power. If for '3'
	 * times the evcs is not using all of the reserved charge power, the maxPower is
	 * reduced to the actually used charge power so that there is power left to be
	 * distributed to other EVCS.
	 */
	private static final int MAXIMUM_OUT_OF_RANGE_TRIES = 3;
	private int outOfRangeCounter = 0;
	private static final double CHARGING_TARGET_MAX_DIFFERENCE_PERCENT = 0.10; // 10%
	private static final int CHECK_CHARGING_TARGET_DIFFERENCE_TIME = 30; // sec
	private LocalDateTime lastChargingCheck = LocalDateTime.now();

	private final EvcsControllerImpl parent;

	public ChargingLowerThanTargetHandler(EvcsControllerImpl parent) {
		this.parent = parent;
	}

	/**
	 * Check if the difference between the requested charging target and the real
	 * charging power is higher than the CHARGING_TARGET_MAX_DIFFERENCE for at least
	 * MAXIMUM_OUT_OF_RANGE_TRIES.
	 * 
	 * @param evcs EVCS
	 * @return true if the difference is too high
	 * @throws InvalidValueException invalidValueException
	 */
	protected boolean isLower(ManagedEvcs evcs) throws InvalidValueException {
		if (this.lastChargingCheck.plusSeconds(CHECK_CHARGING_TARGET_DIFFERENCE_TIME)
				.isBefore(LocalDateTime.now(this.parent.componentManager.getClock()))) {
			if (this.isChargingLowerThanTarget(evcs)) {

				this.outOfRangeCounter++;
				if (this.outOfRangeCounter >= MAXIMUM_OUT_OF_RANGE_TRIES) {
					return true;
				}
			} else {
				this.outOfRangeCounter = 0;
			}
			this.lastChargingCheck = LocalDateTime.now();
		}
		return false;
	}

	/**
	 * Check if the difference between the requested charging target and the real
	 * charging power is higher than the CHARGING_TARGET_MAX_DIFFERENCE.
	 * 
	 * @param evcs EVCS
	 * @return true if the difference is too high
	 * @throws InvalidValueException invalidValueException
	 */
	protected boolean isChargingLowerThanTarget(ManagedEvcs evcs) throws InvalidValueException {
		int chargingPower = evcs.getChargePower().orElse(0);
		int chargingPowerTarget = evcs.getSetChargePowerLimit().orElse(evcs.getMaximumHardwarePower().getOrError());
		if (chargingPowerTarget - chargingPower > chargingPowerTarget * CHARGING_TARGET_MAX_DIFFERENCE_PERCENT) {
			this.lastChargingCheck = LocalDateTime.now();
			return true;
		}
		return false;
	}

}
