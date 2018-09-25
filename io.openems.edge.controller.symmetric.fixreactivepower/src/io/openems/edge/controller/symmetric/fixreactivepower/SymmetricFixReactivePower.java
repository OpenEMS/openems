package io.openems.edge.controller.symmetric.fixreactivepower;

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
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Symmetric.FixReactivePower", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SymmetricFixReactivePower extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(SymmetricFixReactivePower.class);

	@Reference
	protected ConfigurationAdmin cm;

	/**
	 * the configured Charge ActivePower
	 * 
	 * negative values for Charge; positive for Discharge
	 */
	private int power = 0;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "ess", config.ess_id())) {
			return;
		}

		this.power = config.power();
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		// adjust value so that it fits into Min/MaxActivePower
		int calculatedPower = ess.getPower().fitValueIntoMinMaxActivePower(ess, Phase.ALL, Pwr.REACTIVE, this.power);

		/*
		 * set result
		 */
		try {
			this.ess.addPowerConstraintAndValidate("SymmetricFixReactivePower", Phase.ALL, Pwr.REACTIVE,
					Relationship.EQUALS, calculatedPower);
		} catch (PowerException e) {
			this.logError(this.log, e.getMessage());
		}
	}
}
