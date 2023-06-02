package io.openems.edge.controller.asymmetric.fixreactivepower;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Asymmetric.FixReactivePower", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class AsymmetricFixReactivePowerImpl extends AbstractOpenemsComponent
		implements AsymmetricFixReactivePower, Controller, OpenemsComponent {

	@Reference
	private ComponentManager componentManager;

	private Config config;

	public AsymmetricFixReactivePowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				AsymmetricFixReactivePower.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedAsymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

		this.addConstraint(ess, Phase.L1, this.config.powerL1());
		this.addConstraint(ess, Phase.L2, this.config.powerL2());
		this.addConstraint(ess, Phase.L3, this.config.powerL3());
	}

	private void addConstraint(ManagedAsymmetricEss ess, Phase phase, int power) throws OpenemsException {
		// adjust value so that it fits into Min/MaxActivePower
		var calculatedPower = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, phase, Pwr.REACTIVE, power);

		/*
		 * set result
		 */
		ess.addPowerConstraintAndValidate("AymmetricFixReactivePower " + phase, phase, Pwr.REACTIVE,
				Relationship.EQUALS, calculatedPower);
	}
}
