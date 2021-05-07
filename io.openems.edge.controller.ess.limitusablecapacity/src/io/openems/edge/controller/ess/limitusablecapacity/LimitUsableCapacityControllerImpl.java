package io.openems.edge.controller.ess.limitusablecapacity;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.LimitUsableCapacity", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LimitUsableCapacityControllerImpl extends AbstractOpenemsComponent
		implements LimitUsableCapacityController, Controller, OpenemsComponent {

	@Reference
	protected ComponentManager componentManager;

	// Force charge power
	private int forceChargePower = 2000;

	private Config config;
	private State state = State.UNDEFINED;

	public LimitUsableCapacityControllerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				LimitUsableCapacityController.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) throws OpenemsException {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.config = config;

		/*
		 * Checking the Soc values in the configuration
		 * 
		 * forceChargeSoc < stopDischargeSoc < allowDischargeSoc < allowChargeSoc <
		 * stopChargeSoc
		 * 
		 */
		if (this.config.forceChargeSoc() > this.config.stopDischargeSoc()
				&& this.config.stopDischargeSoc() > this.config.allowDischargeSoc()
				&& this.config.allowDischargeSoc() > this.config.allowChargeSoc()
				&& this.config.allowChargeSoc() > this.config.stopChargeSoc()) {
			throw new OpenemsException("Please re-check the configuration, invalid values present in the Soc columns");
		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

		// Set to normal state and return if SoC is not available
		Value<Integer> socOpt = ess.getSoc();
		if (!socOpt.isDefined()) {
			this.state = State.NO_LIMIT;
			return;
		}
		int soc = socOpt.get();

		// initialize allowedCharge
		Integer allowedCharge = null;

		// initialize allowedDischarge
		Integer allowedDischarge = null;

		boolean stateChanged;
		do {
			stateChanged = false;
			switch (this.state) {
			case UNDEFINED:
			case NO_LIMIT:
				// no constraints in normal operation mode
				allowedDischarge = null;
				allowedCharge = null;

				if (soc <= this.config.stopDischargeSoc()) {
					stateChanged = this.changeState(State.STOP_DISCHARGE);
					break;
				}
				if (soc >= this.config.stopChargeSoc()) {
					stateChanged = this.changeState(State.STOP_CHARGE);
					break;
				}
				break;
			case STOP_DISCHARGE:
				allowedDischarge = 0;

				if (soc <= this.config.forceChargeSoc()) {
					stateChanged = this.changeState(State.FORCE_CHARGE);
					break;
				}
				if (soc > this.config.allowDischargeSoc()) {
					stateChanged = this.changeState(State.NO_LIMIT);
					break;
				}
				break;
			case FORCE_CHARGE:
				allowedDischarge = (ess.getMaxApparentPower().getOrError() * 20) / 100;

				// Force charging
				ess.setActivePowerLessOrEquals(ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL,
						Pwr.ACTIVE, this.forceChargePower * -1));

				if (soc >= this.config.stopDischargeSoc()) {
					stateChanged = this.changeState(State.STOP_DISCHARGE);
					break;
				}
				break;
			case STOP_CHARGE:
				allowedCharge = 0;

				if (soc <= this.config.allowChargeSoc()) {
					stateChanged = this.changeState(State.NO_LIMIT);
					break;
				}
				break;

			}

		} while (stateChanged);

		// Allowed Charge Power
		if (allowedCharge != null) {
			Constraint allowedChargeConstraint = ess.getPower().createSimpleConstraint(//
					ess.id() + ": LimitUsableCapacity", //
					ess, Phase.ALL, Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS, //
					0); //
			ess.getPower().addConstraintAndValidate(allowedChargeConstraint);
		}

		// Allowed Discharge Power
		if (allowedDischarge != null) {
			Constraint allowedDischargeConstraint = ess.getPower().createSimpleConstraint(//
					ess.id() + ": LimitUsableCapacity", //
					ess, Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, //
					0); //
			ess.getPower().addConstraintAndValidate(allowedDischargeConstraint);
		}

		this.channel(LimitUsableCapacityController.ChannelId.STATE_MACHINE).setNextValue(this.state);
		this.channel(LimitUsableCapacityController.ChannelId.ALLOWED_CHARGE_POWER).setNextValue(allowedCharge);
		this.channel(LimitUsableCapacityController.ChannelId.ALLOWED_DISCHARGE_POWER).setNextValue(allowedDischarge);
	}

	/**
	 * A flag to maintain change in the state.
	 * 
	 * @param nextState the target state
	 * @return Flag that the state is changed or not
	 */
	private boolean changeState(State nextState) {
		if (this.state != nextState) {
			this.state = nextState;
			return true;
		} else {
			return false;
		}
	}
}
