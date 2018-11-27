package io.openems.edge.controller.symmetric.balancing;

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

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.ess.power.api.Phase;
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
	protected ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SymmetricMeter meter;

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	/**
	 * Calculates required charge/discharge power.
	 * 
	 * @return the required power
	 */
	private int calculateRequiredPower() {
		return this.meter.getActivePower().value().orElse(0) /* current buy-from/sell-to grid */
				+ this.ess.getActivePower().value().orElse(0) /* current charge/discharge Ess */;
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
		/*
		 * Calculates required charge/discharge power
		 */
		int calculatedPower = this.calculateRequiredPower();

		// adjust value so that it fits into Min/MaxActivePower
		calculatedPower = ess.getPower().fitValueIntoMinMaxActivePower(ess, Phase.ALL, Pwr.ACTIVE, calculatedPower);

		/*
		 * set result
		 */
		try {
			this.ess.getSetActivePowerEquals().setNextWriteValue(calculatedPower);
			this.ess.getSetReactivePowerEquals().setNextWriteValue(0);
		} catch (OpenemsException e) {
			this.logError(this.log, e.getMessage());
		}
	}
}
