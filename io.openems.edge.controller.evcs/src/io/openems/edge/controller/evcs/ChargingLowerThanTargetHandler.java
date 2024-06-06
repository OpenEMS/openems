package io.openems.edge.controller.evcs;

import java.time.Clock;
import java.time.LocalDateTime;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.evcs.api.ManagedEvcs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ChargingLowerThanTargetHandler {

	/**
	 * The set charge limit is compared with the actual charge power. If for '3'
	 * times the evcs is not using all of the reserved charge power, the maxPower is
	 * reduced to the actually used charge power so that there is power left to be
	 * distributed to other EVCS.
	 */
	private static final int MAXIMUM_OUT_OF_RANGE_TRIES = 3;
	private int outOfRangeCounter = 0;
	private static final double CHARGING_TARGET_MAX_DIFFERENCE_PERCENT = 0.15; // 15%
	private static final int CHECK_CHARGING_TARGET_DIFFERENCE_TIME = 45; // sec

	private final Clock clock;

	private LocalDateTime lastChargingCheck = LocalDateTime.now();
	private Integer maximumChargePower = null; // W

	private final Logger log = LoggerFactory.getLogger(ChargingLowerThanTargetHandler.class);

	public ChargingLowerThanTargetHandler(Clock clock) {
		this.clock = clock;
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
		this.logDebug("Checking if charging power is lower than target");

		if (this.lastChargingCheck.plusSeconds(CHECK_CHARGING_TARGET_DIFFERENCE_TIME)
				.isBefore(LocalDateTime.now(this.clock))) {
			this.logDebug("Time since last check: " + CHECK_CHARGING_TARGET_DIFFERENCE_TIME + " seconds");
			if (this.isChargingLowerThanTarget(evcs)) {
				this.outOfRangeCounter++;
				this.logDebug("Out of range counter: " + this.outOfRangeCounter);
				if (this.outOfRangeCounter >= MAXIMUM_OUT_OF_RANGE_TRIES) {
					this.logDebug("Charging power is lower than target for " + MAXIMUM_OUT_OF_RANGE_TRIES + " times");
					return true;
				}
			} else {
				this.outOfRangeCounter = 0;
				this.logDebug("Resetting out of range counter to 0");
			}
			this.lastChargingCheck = LocalDateTime.now();
			this.logDebug("Updated last charging check time to " + this.lastChargingCheck);
		}
		return false;
	}

	/**
	 * Check if the difference between the requested charging target and the real
	 * charging power is higher than the CHARGING_TARGET_MAX_DIFFERENCE. If it
	 * returns true, it is setting the maximumPower.
	 *
	 * @param evcs EVCS
	 * @return true if the difference is too high
	 * @throws InvalidValueException invalidValueException
	 */
	protected boolean isChargingLowerThanTarget(ManagedEvcs evcs) throws InvalidValueException {
		this.logDebug("Checking if the charging power difference exceeds the maximum allowed percentage");

		int chargePower = evcs.getChargePower().orElse(0);
		int chargePowerTarget = evcs.getSetChargePowerLimit().orElse(evcs.getMaximumHardwarePower().getOrError());

		this.logDebug("Charge power: " + chargePower + ", Charge power target: " + chargePowerTarget);

		if (chargePowerTarget - chargePower > chargePowerTarget * CHARGING_TARGET_MAX_DIFFERENCE_PERCENT) {
			this.maximumChargePower = this.calculateMaximumPower(chargePower);
			this.lastChargingCheck = LocalDateTime.now();
			this.logDebug("Charging power difference is too high, setting maximum charge power to "
					+ this.maximumChargePower);
			return true;
		}
		this.maximumChargePower = null;
		this.logDebug("Charging power difference is within acceptable range");
		return false;
	}

	/**
	 * Returns the calculated maximum charge power depending on the current charge
	 * power and the current maximum charge power.
	 *
	 * @param currentChargePower current charge power
	 * @return the current charge power or one of the past charge power values
	 */
	private Integer calculateMaximumPower(int currentChargePower) {
		this.logDebug("Calculating maximum charge power");

		if (this.maximumChargePower == null) {
			this.logDebug("Maximum charge power is null, setting to current charge power: " + currentChargePower);
			return currentChargePower;
		}
		Integer calculatedPower = currentChargePower > this.maximumChargePower ? currentChargePower
				: this.maximumChargePower;
		this.logDebug("Calculated maximum charge power: " + calculatedPower);
		return calculatedPower;
	}

	/**
	 * Maximum charge power of the EV depending on the last
	 * {@link #isChargingLowerThanTarget(ManagedEvcs)} try's.
	 *
	 * @return The maximum charge power of the EV.
	 */
	protected Integer getMaximumChargePower() {
		this.logDebug("Getting maximum charge power: " + this.maximumChargePower);
		return this.maximumChargePower;
	}

	private void logDebug(String message) {
		if (log.isDebugEnabled()) {
			log.debug(message);
		}
	}
}
