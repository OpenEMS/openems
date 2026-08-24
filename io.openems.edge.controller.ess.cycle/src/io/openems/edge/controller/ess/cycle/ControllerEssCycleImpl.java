package io.openems.edge.controller.ess.cycle;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicBoolean;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.cycle.statemachine.Context;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine;
import io.openems.edge.controller.ess.cycle.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Cycle", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences("ess")
public class ControllerEssCycleImpl extends AbstractOpenemsComponent
		implements ControllerEssCycle, Controller, OpenemsComponent {

	private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
	private final AtomicBoolean processFinished = new AtomicBoolean(false);

	private final Logger log = LoggerFactory.getLogger(ControllerEssCycleImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private LocalDateTime parsedStartTime;
	private LocalDateTime lastStateChangeTime;
	private Config config;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.ess_id})(enabled=true))")
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
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.applyConfig(config);
	}

	private void applyConfig(Config config) {
		this.config = config;
		this.parsedStartTime = DateUtils.parseLocalDateTimeOrNull(this.config.startTime(), DATE_TIME_FORMATTER);
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

		// store current state in StateMachine channel
		this._setStateMachine(this.stateMachine.getCurrentState());

		switch (this.config.mode()) {
		case MANUAL_ON -> {
			var context = new Context(this, //
					this.config, //
					this.componentManager.getClock(), //
					this.ess, //
					this.parsedStartTime);
			if (!this.ess.getSoc().isDefined()) {
				this.stateMachine.forceNextState(State.UNDEFINED);
			}
			this.stateMachine.run(context);
		}

		case MANUAL_OFF -> {
			// Do nothing
		}
		}
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

	public AtomicBoolean getProcessFinished() {
		return this.processFinished;
	}

}
