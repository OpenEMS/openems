package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Limit  extends BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(Limit.class);

	int warningLowCellVoltage;
	int criticalLowCellVoltage;
	int criticalHighCellVoltage;
	int warningSoC;
	int lowTemperature;
	int highTemperature;
	long unusedTime;

	public Limit(//
			ManagedSymmetricEss ess, //
			int warningLowCellVoltage, //
			int criticalLowCellVoltage, //
			int criticalHighCellVoltage, //
			int warningSoC, //
			int lowTemperature, //
			int highTemperature, //
			long unusedTime
	) {
		super(ess);
		this.warningLowCellVoltage = warningLowCellVoltage;
		this.criticalLowCellVoltage = criticalLowCellVoltage;
		this.criticalHighCellVoltage = criticalHighCellVoltage;
		this.warningSoC = warningSoC;
		this.lowTemperature = lowTemperature;
		this.highTemperature = highTemperature;
		this.unusedTime = unusedTime;
	}
	@Override
	public State getState() {
		return State.LIMIT;
	}

	@Override
	public State getNextState() {

		// According to the state machine the next states can be:
		// NORMAL: ess is under normal operation conditions
		// FORCE_CHARGE: minimal cell voltage has been fallen under critical value
		// UNDEFINED: at least one value is not available
		// FULL_CHARGE: system has done nothing within the configured time
		
		if (isNextStateUndefined()) {
			return State.UNDEFINED;
		}

		if (getEssMinCellVoltage() < criticalLowCellVoltage) {
			return State.FORCE_CHARGE;
		}
		
		if ( //
			getEssMinCellVoltage() > warningLowCellVoltage && //
			getEssMaxCellVoltage() < criticalHighCellVoltage && //
			getEssMinCellTemperature() > lowTemperature && //
			getEssMaxCellTemperature() < highTemperature && // 
			getEssSoC() > warningSoC // && unused time
		) {
			return State.NORMAL;
		}
		
		//TODO unused time
		
		return State.LIMIT;
	}

	@Override
	public void act() {
		log.info("act");
		// Deny further discharging or charging
		
		if (getEssMinCellTemperature() <= lowTemperature || getEssMaxCellTemperature() >= highTemperature) {
			denyCharge();
			denyDischarge();
		}

		if (getEssMinCellVoltage() <= warningLowCellVoltage) {
			denyDischarge();
		}
		
		if (getEssMaxCellVoltage() >= criticalHighCellVoltage) {
			denyCharge();
		}
		
		if (getEssSoC() <= warningSoC) {
			denyDischarge();
		}
	}

}
