package io.openems.edge.controller.symmetric.peakshaving;

import java.util.Optional;

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

import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.SymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Controller.Symmetric.PeakShaving", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class PeakShaving extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(PeakShaving.class);

	@Reference
	protected ConfigurationAdmin cm;

	/*
	 * Peak-Shaving power
	 * 
	 * Grid purchase power above this value is considered a peak and shaved to this
	 * value.
	 */
	private int peakShavingPower;

	/**
	 * Recharge power
	 * 
	 * If grid purchase power is below this value battery is recharged.
	 */
	private int rechargePower;

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

		this.peakShavingPower = config.peakShavingPower();
		this.rechargePower = config.rechargePower();
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private SymmetricMeter meter;

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		Optional<Enum<?>> gridMode = this.ess.getGridMode().value().asEnumOptional();
		if (gridMode.orElse(SymmetricEss.GridMode.UNDEFINED) == SymmetricEss.GridMode.UNDEFINED) {
			this.logWarn(this.log, "Grid-Mode is [" + gridMode + "]");
		}
		if (gridMode.orElse(SymmetricEss.GridMode.UNDEFINED) != SymmetricEss.GridMode.ON_GRID) {
			return;
		}

		// Calculate 'real' grid-power (without current ESS charge/discharge)
		int gridPower = this.meter.getActivePower().value().orElse(0) /* current buy-from/sell-to grid */
				+ this.ess.getActivePower().value().orElse(0) /* current charge/discharge Ess */;

		int calculatedPower;
		if (gridPower >= this.peakShavingPower) {
			/*
			 * Peak-Shaving
			 */
			calculatedPower = gridPower -= this.peakShavingPower;

		} else if (gridPower <= this.rechargePower) {
			/*
			 * Recharge
			 */
			calculatedPower = gridPower -= this.rechargePower;

		} else {
			/*
			 * Do nothing
			 */
			calculatedPower = 0;
		}

		Power power = ess.getPower();
		calculatedPower = power.fitValueIntoMinMaxActivePower(ess, Phase.ALL, Pwr.ACTIVE, calculatedPower);

		/*
		 * set result
		 */
		try {
			this.ess.addPowerConstraintAndValidate("PeakShavingController", Phase.ALL, Pwr.ACTIVE, Relationship.EQUALS,
					calculatedPower); //
		} catch (PowerException e) {
			this.logError(this.log, e.getMessage());
		}
	}
}
