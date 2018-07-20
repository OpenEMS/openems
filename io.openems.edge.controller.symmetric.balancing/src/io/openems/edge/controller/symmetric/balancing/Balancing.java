package io.openems.edge.controller.symmetric.balancing;

import org.apache.commons.math3.optim.linear.Relationship;
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
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.meter.api.SymmetricMeter;

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
	private ManagedSymmetricEss ess;

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
	private int calculateRequiredPower() throws InvalidValueException, NullPointerException {
		return this.meter.getActivePower().value().get() /* current buy-from/sell-to grid */
				+ this.ess.getActivePower().value().get() /* current charge/discharge Ess */;
	}

	@Override
	public void run() {
		int requiredPower;
		try {
			/*
			 * Check that we are On-Grid
			 */
			Enum<?> gridMode = this.ess.getGridMode().value().asEnum();
			if (gridMode != SymmetricEss.GridMode.ON_GRID) {
				return;
			}

			/*
			 * Calculates required charge/discharge power
			 */
			requiredPower = this.calculateRequiredPower();

		} catch (InvalidValueException | NullPointerException e) {
			logError(this.log,
					"Error while calculating required power. " + e.getClass().getSimpleName() + ": " + e.getMessage());
			return;
		}

		Power power = ess.getPower();
		if (requiredPower > 0) {
			/*
			 * Discharge
			 */
			// fit into max possible discharge power
			int maxDischargePower = power.getMaxActivePower();
			if (requiredPower > maxDischargePower) {
				requiredPower = maxDischargePower;
			}

		} else {
			/*
			 * Charge
			 */
			// fit into max possible discharge power
			int maxChargePower = power.getMinActivePower();
			if (requiredPower < maxChargePower) {
				requiredPower = maxChargePower;
			}
		}

		/*
		 * set result
		 */
		try {
			this.ess.addPowerConstraintAndValidate(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.EQ,
					requiredPower);
			this.ess.addPowerConstraintAndValidate(ConstraintType.CYCLE, Phase.ALL, Pwr.REACTIVE, Relationship.EQ, 0);
		} catch (PowerException e) {
			logError(this.log, "Unable to set Power: " + e.getMessage());
		}
	}
}
