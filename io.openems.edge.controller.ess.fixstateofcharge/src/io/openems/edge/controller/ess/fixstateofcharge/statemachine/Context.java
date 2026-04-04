package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.controller.ess.fixstateofcharge.api.AbstractFixStateOfCharge;
import io.openems.edge.controller.ess.fixstateofcharge.api.ConfigProperties;
import io.openems.edge.controller.ess.fixstateofcharge.statemachine.StateMachine.State;

public class Context extends AbstractContext<AbstractFixStateOfCharge> {

	protected final ConfigProperties config;

	/*
	 * Input values (not null)
	 */
	protected final int maxApparentPower;
	protected final int soc;
	protected final int targetSoc;

	// ComponentManager Clock
	protected final Clock clock;

	private Float targetPower;
	private Float lastTargetPower;

	private float rampPower;
	private ZonedDateTime targetDateTime;

	public Context(AbstractFixStateOfCharge parent, ConfigProperties config, int maxApparentPower, int soc,
			int targetSoc, ZonedDateTime targetDateTime, Clock clock) {
		super(parent);
		this.config = config;
		this.maxApparentPower = maxApparentPower;
		this.soc = soc;
		this.targetSoc = targetSoc;
		this.targetDateTime = targetDateTime;
		this.clock = clock;
	}

	public Float getTargetPower() {
		return this.targetPower;
	}

	protected void setTargetPower(Float targetPower) {
		this.targetPower = targetPower;
		this.lastTargetPower = targetPower;
		this.getParent().setLastTargetPower(targetPower);
	}

	protected void setTargetPower(Integer targetPower) {
		this.setTargetPower(targetPower == null ? null : targetPower.floatValue());
	}

	/**
	 * Get the last target power set in this or previous ticks. Fallback to parent's
	 * persisted value for state transitions.
	 * 
	 * @return last target power set in this or previous ticks
	 */
	public Float getLastTargetPower() {
		if (this.lastTargetPower != null) {
			return this.lastTargetPower;
		}
		// Fallback to parent's persisted power value for state transitions
		return this.getParent().getLastTargetPower();
	}

	public float getRampPower() {
		return this.rampPower;
	}

	protected void setRampPower(Double rampPower) {
		this.rampPower = rampPower == null ? null : rampPower.floatValue();
	}

	public ZonedDateTime getTargetTime() {
		return this.targetDateTime;
	}

	/**
	 * Check whether the configured target time has already been passed.
	 * 
	 * @return target time already passed
	 */
	public boolean passedTargetTime() {
		if (this.targetDateTime == null) {
			return false;
		}
		var time = ZonedDateTime.now(this.clock);
		if (time.isAfter(this.targetDateTime)) {
			return true;
		}
		return false;
	}

	/**
	 * Check if SoC is above, below or at target.
	 * 
	 * @param soc       current state of charge
	 * @param targetSoc target state of charge
	 * @return above, below or at target soc {@link State}
	 */
	public static State getSocState(int soc, int targetSoc) {

		if (soc > targetSoc + AbstractFixStateOfCharge.DEFAULT_TARGET_SOC_BOUNDARIES) {
			return State.ABOVE_TARGET_SOC;
		}
		if (soc > targetSoc) {
			return State.WITHIN_UPPER_TARGET_SOC_BOUNDARIES;
		}
		if (soc < targetSoc - AbstractFixStateOfCharge.DEFAULT_TARGET_SOC_BOUNDARIES) {
			return State.BELOW_TARGET_SOC;
		}
		if (soc < targetSoc) {
			return State.WITHIN_LOWER_TARGET_SOC_BOUNDARIES;
		}
		return State.AT_TARGET_SOC;
	}

	protected Integer calculateTargetPower() {
		return AbstractFixStateOfCharge.calculateTargetPower(this.soc, this.targetSoc,
				this.getParent().getEss().getCapacity().orElse(0), this.clock,
				this.getTargetTime().minus(this.config.getTargetTimeBuffer(), ChronoUnit.MINUTES));
	}

	/**
	 * Get ESS capacity for estimation logic with fallback.
	 *
	 * @return capacity in Wh
	 * @throws InvalidValueException if capacity is not available or invalid
	 */
	protected int getEssCapacityForEstimationWh() throws InvalidValueException {
		return this.getParent().getEss().getCapacity().getOrError();
	}

	/**
	 * Calculates power used for required-time estimation.
	 *
	 * @param capacityWh ESS capacity in Wh
	 * @return estimation power in watts
	 */
	protected int getTimeEstimationPowerW(int capacityWh) {
		return Math.round(Math.min(this.maxApparentPower * AbstractFixStateOfCharge.DEFAULT_POWER_FACTOR,
				capacityWh * (1f / 3f)));
	}

	/**
	 * Calculate the limited power for the boundaries.
	 * 
	 * <p>
	 * Calculate the minimum from the maximum apparent power and the capacity,
	 * adjusting both with a factor.
	 * 
	 * @return limited power for boundaries
	 */
	protected int getBoundariesPower() throws InvalidValueException {
		var capacity = this.getEssCapacityForEstimationWh();
		return Math.round(Math.min(this.maxApparentPower * AbstractFixStateOfCharge.BOUNDARIES_POWER_FACTOR,
				capacity * (1f / 6f)));
	}

	/**
	 * Check if the target time should be considered.
	 * 
	 * @return is target time specified and valid
	 */
	protected boolean considerTargetTime() {
		return this.config.isTargetTimeSpecified() && this.targetDateTime != null;
	}
}