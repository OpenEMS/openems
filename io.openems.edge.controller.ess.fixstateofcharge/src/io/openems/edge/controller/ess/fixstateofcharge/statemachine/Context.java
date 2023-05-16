package io.openems.edge.controller.ess.fixstateofcharge.statemachine;

import java.time.Clock;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.common.sum.Sum;
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
	private LocalDateTime targetDateTime;

	public Context(AbstractFixStateOfCharge parent, ConfigProperties config, Sum sum, int maxApparentPower, int soc,
			int targetSoc, LocalDateTime targetDateTime, Clock clock) {
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
	}

	protected void setTargetPower(Integer targetPower) {
		this.setTargetPower(this.targetPower = targetPower == null ? null : targetPower.floatValue());
	}

	public Float getLastTargetPower() {
		return this.lastTargetPower;
	}

	public float getRampPower() {
		return this.rampPower;
	}

	protected void setRampPower(Double rampPower) {
		this.rampPower = rampPower == null ? null : rampPower.floatValue();
	}

	protected void setRampPower(float rampPower) {
		this.rampPower = rampPower;
	}

	protected void setRampPower(int rampPower) {
		this.rampPower = rampPower;
	}

	public LocalDateTime getTargetTime() {
		return this.targetDateTime;
	}

	/**
	 * Check whether the configured target time has already been passed.
	 * 
	 * @return target time already passed
	 */
	public boolean passedTargetTime() {
		var time = LocalDateTime.now(this.clock);
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
	 * Calculate the limited power for the boundaries.
	 * 
	 * <p>
	 * Calculate the minimum from the maximum apparent power and the capacity,
	 * adjusting both with a factor.
	 * 
	 * @return limited power for boundaries
	 */
	protected int getBoundariesPower() {
		var capacity = this.getParent().getEss().getCapacity().orElse(8_800);
		return Math.round(Math.min(this.maxApparentPower * AbstractFixStateOfCharge.BOUNDARIES_POWER_FACTOR,
				capacity * (1f / 6f)));
	}
}