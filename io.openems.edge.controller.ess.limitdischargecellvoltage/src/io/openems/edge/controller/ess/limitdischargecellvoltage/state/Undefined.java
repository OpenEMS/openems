package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Undefined extends BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(Undefined.class);

	int warningLowCellVoltage;
	int criticalHighCellVoltage;
	int warningSoC;
	int lowTemperature;
	int highTemperature;

	public Undefined(//
			ManagedSymmetricEss ess, //
			int warningLowCellVoltage, //
			int criticalHighCellVoltage, //
			int warningSoC, //
			int lowTemperature, //
			int highTemperature//
	) {
		super(ess);
		this.warningLowCellVoltage = warningLowCellVoltage;
		this.criticalHighCellVoltage = criticalHighCellVoltage;
		this.warningSoC = warningSoC;
		this.lowTemperature = lowTemperature;
		this.highTemperature = highTemperature;
	}

	@Override
	public State getState() {
		return State.UNDEFINED;
	}

	@Override
	public State getNextState() {
		// According to the state machine the next states can be NORMAL or LIMIT
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
	
		return State.NORMAL;
	}

	@Override
	public void act() {
		log.info("Nothing to do!");
	}
}
