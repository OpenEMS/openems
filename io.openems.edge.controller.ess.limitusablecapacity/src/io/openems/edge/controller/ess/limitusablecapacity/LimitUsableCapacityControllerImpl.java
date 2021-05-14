package io.openems.edge.controller.ess.limitusablecapacity;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
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

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

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

		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

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

		// initialize allowedCharge
		Integer allowedCharge = null;

		// initialize allowedDischarge
		Integer allowedDischarge = null;

		// Set to normal state and return if SoC is not available
		Value<Integer> socOpt = this.ess.getSoc();
		if (!socOpt.isDefined()) {
			this.state = State.NO_LIMIT;

			// Update the channels before returning
			updateAllowedChargePower(allowedCharge);
			updateAllowedDischargePower(allowedDischarge);
			this.channel(LimitUsableCapacityController.ChannelId.STATE_MACHINE).setNextValue(this.state);

			return;
		}
		int soc = socOpt.get();

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
				allowedDischarge = (this.ess.getMaxApparentPower().getOrError() * 20) / 100;

				// Force charging
				this.ess.setActivePowerLessOrEquals(this.ess.getPower().fitValueIntoMinMaxPower(this.id(), this.ess,
						Phase.ALL, Pwr.ACTIVE, this.forceChargePower * -1));

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

		updateAllowedChargePower(allowedCharge);

		updateAllowedDischargePower(allowedDischarge);

		this.channel(LimitUsableCapacityController.ChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	/**
	 * Update the allowedChargePower for the Ess, also updates the Allowed charge
	 * power channel
	 * 
	 * @param allowedCharge
	 * @throws OpenemsException
	 */
	private void updateAllowedChargePower(Integer allowedCharge) throws OpenemsException {
		// Allowed Charge Power
		if (allowedCharge != null) {
			Constraint allowedChargeConstraint = this.ess.getPower().createSimpleConstraint(//
					this.ess.id() + ": LimitUsableCapacity", //
					this.ess, Phase.ALL, Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS, //
					0); //
			this.ess.getPower().addConstraintAndValidate(allowedChargeConstraint);
		}

		this.channel(LimitUsableCapacityController.ChannelId.ALLOWED_CHARGE_POWER).setNextValue(allowedCharge);
	}

	/**
	 * Update the allowedDishargePower for the Ess, also updates the Allowed
	 * Discharge power channel
	 * 
	 * @param allowedDischarge
	 * @throws OpenemsException
	 */
	private void updateAllowedDischargePower(Integer allowedDischarge) throws OpenemsException {
		// Allowed Discharge Power
		if (allowedDischarge != null) {
			Constraint allowedDischargeConstraint = this.ess.getPower().createSimpleConstraint(//
					this.ess.id() + ": LimitUsableCapacity", //
					this.ess, Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS, //
					0); //
			this.ess.getPower().addConstraintAndValidate(allowedDischargeConstraint);
		}
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
