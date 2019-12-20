package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Normal  extends BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(Normal.class);

	int warningLowCellVoltage;
	int criticalHighCellVoltage;
	int warningSoC;
	int lowTemperature;
	int highTemperature;
	long unusedTime;

	public Normal(//
			ManagedSymmetricEss ess, //
			int warningLowCellVoltage, //
			int criticalHighCellVoltage, //
			int warningSoC, //
			int lowTemperature, //
			int highTemperature, //
			long unusedTime
	) {
		super(ess);
		this.warningLowCellVoltage = warningLowCellVoltage;
		this.criticalHighCellVoltage = criticalHighCellVoltage;
		this.warningSoC = warningSoC;
		this.lowTemperature = lowTemperature;
		this.highTemperature = highTemperature;
		this.unusedTime = unusedTime;
	}

	@Override
	public State getState() {
		return State.NORMAL;
	}

	@Override
	public State getNextState() {
		// According to the state machine the next states can be: 
		// NORMAL: Ess is still under normal operation conditions
		// UNDEFINED: at least one important value (soc, cell voltages/temperatures) is not available
		// LIMIT: one important values has reached its limit
		// FULL_CHARGE: ess was not used for defined time
		if (isNextStateUndefined()) {
			return State.UNDEFINED;
		}

		if (getEssMinCellVoltage() < warningLowCellVoltage) {
			return State.LIMIT;
		}

		if (getEssMaxCellVoltage() > criticalHighCellVoltage) {
			return State.LIMIT;
		}
		
		if (getEssMinCellTemperature() < lowTemperature) {
			return State.LIMIT;
		}
		
		if (getEssMaxCellTemperature() > highTemperature) {
			return State.LIMIT;
		}
		
		if (getEssSoC() < warningSoC) {
			return State.LIMIT;
		}

		//TODO unused time
		
		return State.NORMAL;
	}

	@Override
	public void act() {
		log.info("act");
		// nothing to do
	}
}
