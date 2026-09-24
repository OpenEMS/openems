package io.openems.edge.simulator.controller.ess.csvactivepower;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.PowerConstraint;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.simulator.datasource.api.SimulatorDatasource;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Simulator.Controller.Ess.CsvActivePower", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssCsvActivePowerImpl extends AbstractOpenemsComponent
		implements ControllerEssCsvActivePower, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerEssCsvActivePowerImpl.class);

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SimulatorDatasource datasource;

	private Config config = null;
	private int currentPower = 0;

	public ControllerEssCsvActivePowerImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssCsvActivePower.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		if (this.applyConfig(context, config)) {
			return;
		}
	}

	@Modified
	private void modified(ComponentContext context, Config config) {
		super.modified(context, config.id(), config.alias(), config.enabled());
		if (this.applyConfig(context, config)) {
			return;
		}
	}

	private boolean applyConfig(ComponentContext context, Config config) {
		this.config = config;

		// Update ESS reference filter
		var essUpdated = OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id());

		// Update Datasource reference filter
		var datasourceUpdated = OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "datasource",
				config.datasource_id());

		return essUpdated || datasourceUpdated;
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		if (!this.isEnabled()) {
			return;
		}

		try {
			// Get current power value from CSV datasource
			var powerChannelAddress = new ChannelAddress("", this.config.powerColumnName());

			var powerValue = this.datasource.getValue(OpenemsType.DOUBLE, powerChannelAddress);

			if (powerValue != null) {
				// Convert object to double, then to integer for power constraint
				var powerDouble = ((Number) powerValue).doubleValue();
				var targetPower = (int) Math.round(powerDouble);

				// Apply scale factor (e.g., multiply by number of inverters)
				targetPower = targetPower * this.config.scaleFactor();

				// Apply ramp rate limiting
				var rampRate = this.config.rampRate();
				var powerDiff = targetPower - this.currentPower;

				if (Math.abs(powerDiff) > rampRate) {
					// Limit change to rampRate per cycle
					if (powerDiff > 0) {
						this.currentPower += rampRate;
					} else {
						this.currentPower -= rampRate;
					}
				} else {
					// Within ramp rate, apply target directly
					this.currentPower = targetPower;
				}

				// Apply Active-Power Set-Point to ESS
				PowerConstraint.apply(this.ess, this.id(), //
						this.config.phase(), Pwr.ACTIVE, this.config.relationship(), this.currentPower);

				// Update the current power channel for monitoring
				this.channel(ControllerEssCsvActivePower.ChannelId.CURRENT_POWER).setNextValue(this.currentPower);
			} else {
				this.log.warn("Power value from CSV is null - column '{}' not found or no data",
						this.config.powerColumnName());
			}

		} catch (Exception e) {
			this.log.error("Error reading power value from CSV: {}", e.getMessage(), e);
		}
	}
}
