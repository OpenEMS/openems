package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import java.util.Optional;

import org.osgi.service.metatype.annotations.AttributeDefinition;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.Config;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;

public class Undefined implements IState {

	private final Logger log = LoggerFactory.getLogger(Undefined.class);
	private ManagedSymmetricEss ess;

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
		this.ess = ess;
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
		// According to the state machine the next states can be normal, critical,
		// warning or undefined

		Optional<Integer> minCellVoltageOpt = ess.getMinCellVoltage().value().asOptional();
		if (!minCellVoltageOpt.isPresent()) {
			return State.UNDEFINED;
		}

		int minCellVoltage = minCellVoltageOpt.get();

		if (minCellVoltage < this.config.criticalCellVoltage()) {
			return State.LIMIT;
		}

		if (minCellVoltage < this.config.warningCellVoltage()) {
			return State.LIMIT;
		}

		return return State.NORMAL;;
	}

	@Override
	public void act() {
		// nothing to do
	}
}
