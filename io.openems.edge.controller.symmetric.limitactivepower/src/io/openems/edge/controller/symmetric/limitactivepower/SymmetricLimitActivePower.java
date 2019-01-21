package io.openems.edge.controller.symmetric.limitactivepower;

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
@Component(name = "Controller.Symmetric.LimitActivePower", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SymmetricLimitActivePower extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(SymmetricLimitActivePower.class);

	@Reference
	protected ComponentManager componentManager;

	private String essId;

	/**
	 * the configured Max Charge ActivePower
	 * 
	 * value is zero or negative
	 */
	private int maxChargePower = 0;
	/**
	 * the configured Max Discharge ActivePower
	 * 
	 * value is zero or positive
	 */
	private int maxDischargePower = 0;

	public SymmetricLimitActivePower() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled());
		this.essId = config.ess_id();
		this.maxChargePower = config.maxChargePower() * -1;
		this.maxDischargePower = config.maxDischargePower();
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.essId);

		// adjust value so that it fits into Min/MaxActivePower
		int calculatedMaxDischargePower = ess.getPower().fitValueIntoMinMaxActivePower(ess, Phase.ALL, Pwr.ACTIVE,
				this.maxDischargePower);
		int calculatedMaxChargePower = ess.getPower().fitValueIntoMinMaxActivePower(ess, Phase.ALL, Pwr.ACTIVE,
				this.maxChargePower);

		/*
		 * set result
		 */
		try {
			ess.addPowerConstraintAndValidate("SymmetricLimitActivePower", Phase.ALL, Pwr.ACTIVE,
					Relationship.GREATER_OR_EQUALS, calculatedMaxChargePower);
			ess.addPowerConstraintAndValidate("SymmetricLimitActivePower", Phase.ALL, Pwr.ACTIVE,
					Relationship.LESS_OR_EQUALS, calculatedMaxDischargePower);
		} catch (PowerException e) {
			this.logError(this.log, e.getMessage());
		}
	}
}
