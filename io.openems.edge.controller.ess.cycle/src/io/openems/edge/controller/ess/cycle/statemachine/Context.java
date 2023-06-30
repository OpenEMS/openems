package io.openems.edge.controller.ess.cycle.statemachine;

import java.time.Duration;
import java.time.LocalDateTime;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.statemachine.AbstractContext;
import io.openems.edge.controller.ess.cycle.Config;
import io.openems.edge.controller.ess.cycle.ControllerEssCycle;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

public class Context extends AbstractContext<ControllerEssCycle> {

	private final Logger log = LoggerFactory.getLogger(Context.class);

	protected final Config config;
	protected final ManagedSymmetricEss ess;
	protected final int maxChargePower;
	protected final int maxDischargePower;
	protected final ComponentManager componentManager;
	protected final State previousState;
	private final LocalDateTime lastStateChange;

	public Context(ControllerEssCycle parent, Config config, ComponentManager componentManager, ManagedSymmetricEss ess,
			int maxChargePower, int maxDischargePower, State previousState, LocalDateTime lastStateChange) {
		super(parent);
		this.config = config;
		this.componentManager = componentManager;
		this.ess = ess;
		this.maxChargePower = maxChargePower;
		this.maxDischargePower = maxDischargePower;
		this.previousState = previousState;
		this.lastStateChange = lastStateChange;
	}

	/**
	 * Helper for a state change. If awaiting hysteresis time exceeded switches from
	 * currentState to NextState.
	 *
	 * @param currentState Used to output better log.
	 * @param nextState    state which will be switched to.
	 * @return boolean true if change state is allowed
	 */
	protected boolean waitForChangeState(State currentState, State nextState) {
		if (LocalDateTime.now(this.componentManager.getClock()).minus(Duration.ofMinutes(this.config.standbyTime()))
				.isAfter(this.lastStateChange)) {
			return true;
		}
		this.logInfo(this.log, "Awaiting hysteresis for changing from [" + currentState + "] to [" + nextState + "]");
		return false;
	}

	protected int getChargePower() {
		// get max charge/discharge power
		var power = Math.max(this.maxChargePower, this.config.power() * -1);
		if (this.ess instanceof HybridEss) {
			// modify target power by DC production for HybridEss
			var ess = (HybridEss) this.ess;
			power -= ess.getActivePower().orElse(0) - ess.getDcDischargePower().orElse(0);
		}
		return power;
	}

	protected int getDischargePower() {
		// TODO consider DC power for HybridEss
		return Math.min(this.maxDischargePower, this.config.power());
	}

}