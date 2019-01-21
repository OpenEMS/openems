package io.openems.edge.controller.symmetric.randompower;

import java.util.concurrent.ThreadLocalRandom;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.RandomPower", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SymmetricRandomPower extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(SymmetricRandomPower.class);

	@Reference
	protected ComponentManager componentManager;

	private Config config;

	public SymmetricRandomPower() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled());
		this.config = config;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.config.ess_id());

		int randomPower = ThreadLocalRandom.current().nextInt(this.config.minPower(), this.config.maxPower() + 1);

		// adjust value so that it fits into Min/MaxActivePower
		randomPower = ess.getPower().fitValueIntoMinMaxActivePower(ess, Phase.ALL, Pwr.ACTIVE, randomPower);

		/*
		 * set result
		 */
		try {
			ess.addPowerConstraintAndValidate("SymmetricRandomPower", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
					randomPower); //
		} catch (PowerException e) {
			this.logError(this.log, e.getMessage());
		}
	}
}
