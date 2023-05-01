package io.openems.edge.controller.ess.standby;

import java.time.LocalDate;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.utils.DateUtils;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.standby.statemachine.Context;
import io.openems.edge.controller.ess.standby.statemachine.StateMachine;
import io.openems.edge.controller.ess.standby.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.Standby", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class StandbyControllerImpl extends AbstractOpenemsComponent
		implements StandbyController, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(StandbyControllerImpl.class);

	public StandbyControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				StandbyController.ChannelId.values() //
		);
	}

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected Sum sum;

	/**
	 * Manages the {@link State}s of the StateMachine.
	 */
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private Config config;
	private LocalDate configuredStartDate;
	private LocalDate configuredEndDate;

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.config = config;
		// TODO error handling if input is invalid
		// TODO switch format to {@link DateTimeFormatter#ISO_LOCAL_DATE}
		this.configuredStartDate = DateUtils.parseLocalDateOrError(config.startDate(), DateUtils.DMY_FORMATTER);
		this.configuredEndDate = DateUtils.parseLocalDateOrError(config.endDate(), DateUtils.DMY_FORMATTER);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		var gridMode = ess.getGridMode();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		switch (gridMode) {
		case ON_GRID:
		case UNDEFINED:
			break;
		case OFF_GRID:
			return;
		}

		// Store the current State
		this.channel(StandbyController.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Prepare Context
		var context = new Context(this, ess, this.sum, this.configuredStartDate, this.configuredEndDate,
				this.config.dayOfWeek(), this.componentManager.getClock());

		// Call the StateMachine
		this.stateMachine.run(context);
	}

}
