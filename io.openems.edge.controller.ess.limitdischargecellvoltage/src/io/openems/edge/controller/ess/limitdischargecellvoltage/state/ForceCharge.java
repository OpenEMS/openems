package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class ForceCharge  extends BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(ForceCharge.class);
	private int chargePowerPercent;
	private int chargingTime;
	private LocalDateTime startTime = null;

	public ForceCharge(ManagedSymmetricEss ess, int chargePowerPercent, int chargingTime) {
		super(ess);
		this.chargePowerPercent = chargePowerPercent;
		this.chargingTime = chargingTime;
	}

	@Override
	public State getState() {
		return State.FORCE_CHARGE;
	}

	@Override
	public State getNextState() {
		// According to the state machine the next states can be CHECK, FORCE_CHARGE or
		// UNDEFINED
		
		if (isNextStateUndefined()) {
			this.resetStartTime();
			return State.UNDEFINED;
		}

		if (this.startTime == null) {
			this.startTime = LocalDateTime.now();
		}

		if (this.startTime.plusSeconds(chargingTime).isBefore(LocalDateTime.now())) {
			this.resetStartTime();
			return State.CHECK;
		}

		return State.FORCE_CHARGE;
	}

	private void resetStartTime() {
		this.startTime = null;

	}

	@Override
	public void act() throws OpenemsNamedException {
		log.info("act");
		chargeEssWithPercentOfMaxPower(chargePowerPercent);
	}
}
