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
import io.openems.edge.ess.api.SymmetricEss;

public class Warning implements IState {

	private final Logger log = LoggerFactory.getLogger(Warning.class);
	
	private ComponentManager componentManager;
	private Config config;
	private LocalDateTime startTime = null;
	
	public Warning(ComponentManager componentManager, Config config) {
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
		SymmetricEss ess = null;
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
			return new Critical(this.componentManager, this.config);
		}

		if (minCellVoltage > this.config.warningCellVoltage()) {
			return new Normal(this.componentManager, this.config);
		}

		if (this.startTime.plusSeconds(this.config.warningCellVoltageTime()).isBefore(LocalDateTime.now())) {
			this.resetStartTime();
			return new Charge(this.componentManager, this.config);
		}

		return this;
		
	}

	@Override
	public void act() {
		// There is nothing to do
	}
	
	private void resetStartTime() {
		this.startTime = null;

	}
}
