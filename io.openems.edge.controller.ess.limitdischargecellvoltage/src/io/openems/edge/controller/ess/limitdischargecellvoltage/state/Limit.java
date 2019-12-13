package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import java.time.LocalDateTime;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.Config;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class Limit implements IState {

	private final Logger log = LoggerFactory.getLogger(Limit.class);

	private ComponentManager componentManager;
	private Config config;
	private LocalDateTime startTime = null;

	public Limit(ComponentManager componentManager, Config config) {
		this.componentManager = componentManager;
		this.config = config;
	}

	@Override
	public State getState() {
		return State.WARNING;
	}

	@Override
	public IState getNextStateObject() {

		// According to the state machine the next states can be normal, charge or
		// undefined
		ManagedSymmetricEss ess = null;
		try {
			ess = this.componentManager.getComponent(this.config.ess_id());
		} catch (OpenemsNamedException e) {
			log.error(e.getMessage());
			this.resetStartTime();
			return new Undefined(this.componentManager, this.config);
		}

		if (ess == null) {
			this.resetStartTime();
			return new Undefined(this.componentManager, this.config);
		}

		Optional<Integer> minCellVoltageOpt = ess.getMinCellVoltage().value().asOptional();
		if (!minCellVoltageOpt.isPresent()) {
			this.resetStartTime();
			return new Undefined(this.componentManager, this.config);
		}

		if (this.startTime == null) {
			this.startTime = LocalDateTime.now();
		}

		int minCellVoltage = minCellVoltageOpt.get();

		if (minCellVoltage < this.config.criticalCellVoltage()) {
			return new Limit(this.componentManager, this.config);
		}

		if (minCellVoltage > this.config.warningCellVoltage()) {
			return new Normal(this.componentManager, this.config);
		}

		if (this.startTime.plusSeconds(this.config.warningCellVoltageTime()).isBefore(LocalDateTime.now())) {
			this.resetStartTime();
			return new ForceCharge(this.componentManager, this.config);
		}
		
		return this;
	}

	@Override
	public void act() {
		// Deny further discharging
		ManagedSymmetricEss ess = null;
		try {
			ess = this.componentManager.getComponent(this.config.ess_id());
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());			
			return;
		}

		Integer calculatedPower = 0;
		calculatedPower = ess.getPower().fitValueIntoMinMaxPower("DischargeLimitCellVoltage.Warning", ess, Phase.ALL, Pwr.ACTIVE, calculatedPower);
		try {
			ess.getSetActivePowerLessOrEquals().setNextWriteValue(calculatedPower);
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
		}
	}

	private void resetStartTime() {
		this.startTime = null;
	}
}
