package io.openems.edge.controller.symmetric.selltogridlimit;

import static org.osgi.service.component.annotations.ReferenceCardinality.MANDATORY;
import static org.osgi.service.component.annotations.ReferencePolicy.STATIC;
import static org.osgi.service.component.annotations.ReferencePolicyOption.GREEDY;

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
import io.openems.common.referencetarget.GenerateTargetsFromReferences;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.SellToGridLimit", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@GenerateTargetsFromReferences({ "ess", "meter" })
public class ControllerEssSellToGridLimitImpl extends AbstractOpenemsComponent
		implements ControllerEssSellToGridLimit, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerEssSellToGridLimitImpl.class);

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.ess_id})(enabled=true))")
	private ManagedSymmetricEss ess;

	@Reference(policy = STATIC, policyOption = GREEDY, cardinality = MANDATORY, //
			target = "(&(id=${config.meter_id})(enabled=true))")
	private ElectricityMeter meter;

	private Config config = null;

	public ControllerEssSellToGridLimitImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssSellToGridLimit.ChannelId.values());
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
		// Check that we are On-Grid (and warn on undefined Grid-Mode)
		if (!this.ess.isOnGridOrUndefined(m -> this.logWarn(this.log, m))) {
			return;
		}

		// Get the grid power and ess power
		int gridPower = this.meter.getActivePower().getOrError(); /* current buy-from/sell-to grid */

		// Checking if the grid power is above the maximum feed-in
		if (gridPower * -1 > this.config.maximumSellToGridPower()) {

			// Calculate actual limit for Ess
			var essPowerLimit = gridPower + this.ess.getActivePower().getOrError()
					+ this.config.maximumSellToGridPower();

			// Apply limit
			this.ess.setActivePowerLessOrEquals(essPowerLimit);
		}
	}
}
