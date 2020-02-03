package io.openems.edge.battery.soltaro.controller.state;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.battery.soltaro.SoltaroBattery;
import io.openems.edge.battery.soltaro.controller.IState;
import io.openems.edge.battery.soltaro.controller.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Normal extends BaseState implements IState {

	private final Logger log = LoggerFactory.getLogger(Normal.class);

	int warningLowCellVoltage;
	int criticalHighCellVoltage;
	int warningSoC;
	int lowTemperature;
	int highTemperature;
	long unusedTime;

	public Normal(//
			ManagedSymmetricEss ess, //
			SoltaroBattery bms, //
			int warningLowCellVoltage, //
			int criticalHighCellVoltage, //
			int warningSoC, //
			int lowTemperature, //
			int highTemperature, //
			long unusedTime) {
		super(ess, bms);
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
		// UNDEFINED: at least one important value (soc, cell voltages/temperatures) is
		// not available
		// LIMIT: one important values has reached its limit
		// FULL_CHARGE: ess was not used for defined time
		if (isNextStateUndefined()) {
			return State.UNDEFINED;
		}

		if (getBmsMinCellVoltage() < warningLowCellVoltage) {
			return State.LIMIT;
		}

		if (getBmsMaxCellVoltage() > criticalHighCellVoltage) {
			return State.LIMIT;
		}

		if (getBmsMinCellTemperature() < lowTemperature) {
			return State.LIMIT;
		}

		if (getBmsMaxCellTemperature() > highTemperature) {
			return State.LIMIT;
		}

		if (getBmsSoC() < warningSoC) {
			return State.LIMIT;
		}

		if (bmsNeedsFullCharge(unusedTime)) {
			return State.FULL_CHARGE;
		}

		return State.NORMAL;
	}

	@Override
	public void act() {
		log.info("act");
		// nothing to do
	}
}
