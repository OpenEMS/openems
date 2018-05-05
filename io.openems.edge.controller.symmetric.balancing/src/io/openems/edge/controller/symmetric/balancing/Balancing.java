package io.openems.edge.controller.symmetric.balancing;

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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.InvalidValueException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.Ess;
import io.openems.edge.ess.power.PowerException;
import io.openems.edge.ess.power.symmetric.PEqualLimitation;
import io.openems.edge.ess.power.symmetric.SymmetricPower;
import io.openems.edge.ess.symmetric.api.SymmetricEss;
import io.openems.edge.meter.symmetric.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.Balancing", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class Balancing extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(Balancing.class);

	@Reference
	protected ConfigurationAdmin cm;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "ess", config.ess_id())) {
			return;
		}
		// update filter for 'meter'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "meter", config.meter_id())) {
			return;
		}
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter meter;

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Calculates required charge/discharge power
	 * 
	 * @throws InvalidValueException
	 */
	private int calculateRequiredPower() throws InvalidValueException {
		return this.meter.getActivePower().getActiveValue() /* current buy-from/sell-to grid */
				+ this.ess.getActivePower().getActiveValue() /* current charge/discharge Ess */;
	}

	@Override
	public void run() {
		int requiredPower;
		try {
			/*
			 * Check that we are On-Grid
			 */
			Enum<?> gridMode = this.ess.getGridMode().getActiveValueOptionEnum();
			if (gridMode != Ess.GridMode.ON_GRID) {
				return;
			}

			/*
			 * Calculates required charge/discharge power
			 */
			requiredPower = this.calculateRequiredPower();

		} catch (InvalidValueException e) {
			logError(this.log, e.getMessage());
			return;
		}

		SymmetricPower power = ess.getPower();
		if (requiredPower > 0) {
			/*
			 * Discharge
			 */
			// fit into max possible discharge power
			int maxDischargePower = power.getMaxP().orElse(0);
			if (requiredPower > maxDischargePower) {
				requiredPower = maxDischargePower;
			}

		} else {
			/*
			 * Charge
			 */
			// fit into max possible discharge power
			int maxChargePower = power.getMinP().orElse(0);
			if (requiredPower < maxChargePower) {
				requiredPower = maxChargePower;
			}
		}

		/*
		 * set result
		 */
		try {
			power.applyLimitation(new PEqualLimitation(power).setP(requiredPower));
		} catch (PowerException e) {
			logError(this.log, "Unable to set Power: " + e.getMessage());
		}
	}
}
