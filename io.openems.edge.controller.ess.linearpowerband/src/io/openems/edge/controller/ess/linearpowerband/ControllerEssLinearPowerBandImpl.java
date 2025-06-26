package io.openems.edge.controller.ess.linearpowerband;

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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.LinearPowerBand", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssLinearPowerBandImpl extends AbstractOpenemsComponent
		implements ControllerEssLinearPowerBand, Controller, OpenemsComponent {

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	private Config config;
	private int currentPower = 0;
	private State state = State.DOWNWARDS;

	public ControllerEssLinearPowerBandImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssLinearPowerBand.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		switch (config.startDirection()) {
		case CHARGE:
			this.state = State.DOWNWARDS;
			break;
		case DISCHARGE:
			this.state = State.UPWARDS;
			break;
		}

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		// Store the current State
		this.channel(ControllerEssLinearPowerBand.ChannelId.STATE_MACHINE).setNextValue(this.state);

		// Run the State-Machine
		switch (this.state) {
		case UNDEFINED:
			this.state = State.DOWNWARDS;
			break;

		case DOWNWARDS:
			// adjust Power
			this.currentPower = Math.max(this.config.minPower(), this.currentPower - this.config.adjustPower());

			if (this.currentPower == this.config.minPower()) {
				// switch to discharge
				this.state = State.UPWARDS;
			}
			break;

		case UPWARDS:
			// adjust Power
			this.currentPower = Math.min(this.config.maxPower(), this.currentPower + this.config.adjustPower());

			if (this.currentPower == this.config.maxPower()) {
				// switch to discharge
				this.state = State.DOWNWARDS;
			}
			break;
		}

		// Keep target power for debugging
		this._setTargetPower(this.currentPower);

		// Apply Power Set-Point
		this.ess.setActivePowerEquals(this.currentPower);
	}
}
