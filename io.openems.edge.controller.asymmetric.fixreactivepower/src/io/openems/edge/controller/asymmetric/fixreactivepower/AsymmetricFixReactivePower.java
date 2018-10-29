package io.openems.edge.controller.asymmetric.fixreactivepower;

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
@Component(name = "Controller.Asymmetric.FixReactivePower", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class AsymmetricFixReactivePower extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(AsymmetricFixReactivePower.class);

	@Reference
	protected ConfigurationAdmin cm;

	/**
	 * the configured Charge ReactivePower
	 * 
	 * negative values for Charge; positive for Discharge
	 */
	private int powerL1 = 0;
	private int powerL2 = 0;
	private int powerL3 = 0;

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "ess", config.ess_id())) {
			return;
		}

		this.powerL1 = config.powerL1();
		this.powerL2 = config.powerL2();
		this.powerL3 = config.powerL3();
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		this.addConstraint(Phase.L1, this.powerL1);
		this.addConstraint(Phase.L2, this.powerL2);
		this.addConstraint(Phase.L3, this.powerL3);
	}

	private void addConstraint(Phase phase, int power) {
		// adjust value so that it fits into Min/MaxActivePower
		int calculatedPower = ess.getPower().fitValueIntoMinMaxActivePower(ess, phase, Pwr.REACTIVE, power);

		/*
		 * set result
		 */
		try {
			this.ess.addPowerConstraintAndValidate("AymmetricFixReactivePower " + phase, phase, Pwr.REACTIVE,
					Relationship.EQUALS, calculatedPower);
		} catch (PowerException e) {
			this.logError(this.log, e.getMessage());
		}
	}
}
