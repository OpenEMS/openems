package io.openems.edge.controller.ess.emergencycapacityreserve;

import org.osgi.service.cm.ConfigurationAdmin;
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
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.filter.RampFilter;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.Context;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine;
import io.openems.edge.controller.ess.emergencycapacityreserve.statemachine.StateMachine.State;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.EmergencyCapacityReserve", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class EmergencyCapacityReserveImpl extends AbstractOpenemsComponent
		implements EmergencyCapacityReserve, Controller, OpenemsComponent {

	@Reference
	protected ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Sum sum;

	@Reference
	private ManagedSymmetricEss ess;

	private Config config;

	/**
	 * Minimum reserve SoC value in [%]
	 */
	private static final int reservSocMinValue = 5;

	/**
	 * Maximum reserve SoC value in [%]
	 */
	private static final int reservSocMaxValue = 100;

	private final StateMachine stateMachine = new StateMachine(State.NO_LIMIT);

	private final RampFilter rampFilter = new RampFilter();

	private final Logger log = LoggerFactory.getLogger(EmergencyCapacityReserveImpl.class);

	public EmergencyCapacityReserveImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				EmergencyCapacityReserve.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Override
	protected void modified(ComponentContext context, String id, String alias, boolean enabled) {
		super.modified(context, id, alias, enabled);
		this.updateConfig(config);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		Context context = this.handleStateMachine();

		if(this.config.isReserveSocEnabled()) {
			Integer activerPowerConstraint = rampFilter.getFilteredValueAsInteger(context.getTargetPower(),
					context.getRampPower());

			if (context.maxApparentPower != activerPowerConstraint) {
				// Set constraint only if we did not reach the max apparent power
				ess.setActivePowerLessOrEquals(activerPowerConstraint);
			}

			// set debug channels
			this._setDebugSetActivePowerLessOrEquals(activerPowerConstraint);
			this._setDebugTargetPower(context.getTargetPower());
			this._setDebugRampPower(context.getRampPower());
		}

	}

	/**
	 * Update {@link Config} for the controller.
	 * 
	 * @param config to update
	 */
	private void updateConfig(Config config) {
		this.config = config;

		boolean enableWarning = false;
		if (this.config.reserveSoc() < reservSocMinValue || this.config.reserveSoc() > reservSocMaxValue) {
			enableWarning = true;
		}

		this._setRangeOfReserveSocOutsideAllowedValue(enableWarning);

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
	}

	/**
	 * Handle different {@link State} of the {@link StateMachine}.
	 * 
	 * @return created {@link Context}
	 */
	private Context handleStateMachine() {
		this._setStateMachine(this.stateMachine.getCurrentState());

		// check SoC is defined
		Value<Integer> soc = ess.getSoc();
		Value<Integer> maxApparentPower = ess.getMaxApparentPower();
		if (!soc.isDefined() || !maxApparentPower.isDefined()) {
			this.stateMachine.forceNextState(State.NO_LIMIT);
		}

		Context context = new Context(this, this.sum, maxApparentPower.get(), soc.get(), this.config.reserveSoc());
		try {
			this.stateMachine.run(context);

			this.channel(Controller.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(Controller.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}

		return context;
	}

}
