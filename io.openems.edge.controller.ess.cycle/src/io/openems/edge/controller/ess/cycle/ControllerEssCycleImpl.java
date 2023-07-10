package io.openems.edge.controller.ess.cycle;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.cycle.statemachine.Context;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Cycle", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssCycleImpl extends AbstractOpenemsComponent
		implements ControllerEssCycle, Controller, OpenemsComponent {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

	private final Logger log = LoggerFactory.getLogger(ControllerEssCycleImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private LocalDateTime lastStateChangeTime;
	private LocalDateTime parsedStartTime;
	private State setNextState;
	private Config config;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	public ControllerEssCycleImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssCycle.ChannelId.values() //
		);
		this._setCompletedCycles(0);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (this.applyConfig(context, config)) {
			return;
		}
	}

	private boolean applyConfig(ComponentContext context, Config config) {
		this.config = config;
		this.parsedStartTime = DateUtils.parseLocalDateTimeOrNull(this.config.startTime(), DATE_TIME_FORMATTER);
		return OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id());
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (this.parsedStartTime == null) {
			this.logError(this.log, "Start time could not be parsed");
			return;
		}
		switch (this.config.mode()) {
		case MANUAL_ON:
			// get max charge/discharge power
			var allowedDischargePower = this.ess.getPower().getMaxPower(this.ess, Phase.ALL, Pwr.ACTIVE);
			var allowedChargePower = this.ess.getPower().getMinPower(this.ess, Phase.ALL, Pwr.ACTIVE);

			// Prepare Context
			var context = new Context(this, //
					this.config, //
					this.componentManager.getClock(), //
					this.ess, //
					allowedChargePower, //
					allowedDischargePower, //
					this.parsedStartTime);

			// store current state in StateMachine channel
			var currentState = this.getCurrentState();
			this._setStateMachine(currentState);

			try {
				this.stateMachine.run(context);
				this._setRunFailed(false);
			} catch (OpenemsNamedException e) {
				this.logError(this.log, "StateMachine failed: " + e.getMessage());
				this._setRunFailed(true);
			}
			break;
		case MANUAL_OFF:
			// Do nothing
			break;
		}
	}

	/**
	 * Gets the current {@link State} state of the {@link StateMachine}.
	 *
	 * @return the {@link State}
	 */
	public State getCurrentState() {
		return this.stateMachine.getCurrentState();
	}

	/**
	 * Sets the next {@link StateMachine} {@link State}.
	 * 
	 * @param state as a next {@link StateMachine} {@link State} to be set.
	 */
	public void setNextState(State nextState) {
		this.setNextState = nextState;
	}

	/**
	 * Gets the next {@link State} state of the {@link StateMachine}.
	 * 
	 * @return next {@link StateMachine} {@link State}
	 */
	public State getNextState() {
		return this.setNextState;
	}

	/**
	 * Gets the time when {@link StateMachine} {@link State} changed.
	 * 
	 * @return {@link LocalDateTime} last state changed time.
	 */
	public LocalDateTime getLastStateChangeTime() {
		return this.lastStateChangeTime;
	}

	/**
	 * Sets the time when {@link StateMachine} {@link State} changed.
	 *
	 * @param time {@link LocalDateTime} last state changed time.
	 */
	public void setLastStateChangeTime(LocalDateTime time) {
		this.lastStateChangeTime = time;
	}
}
