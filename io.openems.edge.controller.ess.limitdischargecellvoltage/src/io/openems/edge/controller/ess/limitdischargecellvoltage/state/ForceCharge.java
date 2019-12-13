package io.openems.edge.controller.ess.limitdischargecellvoltage.state;

import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.controller.ess.limitdischargecellvoltage.Config;
import io.openems.edge.controller.ess.limitdischargecellvoltage.IState;
import io.openems.edge.controller.ess.limitdischargecellvoltage.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

public class ForceCharge implements IState {

	private final Logger log = LoggerFactory.getLogger(ForceCharge.class);
	private ComponentManager componentManager;
	private Config config;
	private LocalDateTime startTime = null;

	public ForceCharge(ComponentManager componentManager, Config config) {
		this.componentManager = componentManager;
		this.config = config;
	}

	@Override
	public State getState() {
		return State.CHARGE;
	}

	@Override
	public IState getNextStateObject() {
		// According to the state machine the next states can be normal, charge or
		// undefined
		SymmetricEss ess = null;

		try {
			ess = this.componentManager.getComponent(this.config.ess_id());
		} catch (OpenemsNamedException e) {
			this.log.error(e.getMessage());
			this.resetStartTime();
			return new Undefined(this.componentManager, this.config);
		}

		if (ess == null) {
			this.resetStartTime();
			return new Undefined(this.componentManager, this.config);
		}

		if (this.startTime == null) {
			this.startTime = LocalDateTime.now();
		}

		if (this.startTime.plusSeconds(this.config.chargingTime()).isBefore(LocalDateTime.now())) {
			this.resetStartTime();
			return new Normal(this.componentManager, this.config);
		}

		return this;
	}

	private void resetStartTime() {
		this.startTime = null;

	}

	@Override
	public void act() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());
		int maxCharge = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
		int calculatedPower = maxCharge / 100 * this.config.chargePowerPercent();
		ess.getSetActivePowerLessOrEquals().setNextWriteValue(calculatedPower);
	}
}
