package io.openems.edge.controller.symmetric.limitactivepower;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Symmetric.LimitActivePower", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class SymmetricLimitActivePowerImpl extends AbstractOpenemsComponent
		implements SymmetricLimitActivePower, Controller, OpenemsComponent {

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	/**
	 * The configured Max Charge ActivePower.
	 *
	 * <p>
	 * Value is zero or negative
	 */
	private int maxChargePower = 0;

	/**
	 * The configured Max Discharge ActivePower.
	 *
	 * <p>
	 * Value is zero or positive
	 */
	private int maxDischargePower = 0;

	public SymmetricLimitActivePowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				SymmetricLimitActivePower.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
		this.maxChargePower = config.maxChargePower() * -1;
		this.maxDischargePower = config.maxDischargePower();
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

		if (this.config.validatePowerConstraints()) {

			// adjust value so that it fits into Min/MaxActivePower
			var maxPower = ess.getPower().getMaxPower(ess, Phase.ALL, Pwr.ACTIVE);
			var minPower = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
			var calculatedMaxDischargePower = fitIntoMinMax(minPower, maxPower, this.maxDischargePower);
			var calculatedMaxChargePower = fitIntoMinMax(minPower, maxPower, this.maxChargePower);

			// set result
			ess.addPowerConstraintAndValidate("SymmetricLimitActivePower", Phase.ALL, Pwr.ACTIVE,
					Relationship.GREATER_OR_EQUALS, calculatedMaxChargePower);
			ess.addPowerConstraintAndValidate("SymmetricLimitActivePower", Phase.ALL, Pwr.ACTIVE,
					Relationship.LESS_OR_EQUALS, calculatedMaxDischargePower);
		} else {

			ess.addPowerConstraint("SymmetricLimitActivePower", Phase.ALL, Pwr.ACTIVE, Relationship.GREATER_OR_EQUALS,
					this.maxChargePower);
			ess.addPowerConstraint("SymmetricLimitActivePower", Phase.ALL, Pwr.ACTIVE, Relationship.LESS_OR_EQUALS,
					this.maxDischargePower);
		}
	}

	private static int fitIntoMinMax(int min, int max, int value) {
		if (value > max) {
			value = max;
		}
		if (value < min) {
			value = min;
		}
		return value;
	}
}
