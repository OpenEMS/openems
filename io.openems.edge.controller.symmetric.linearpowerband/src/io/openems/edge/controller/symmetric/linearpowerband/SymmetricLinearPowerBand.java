package io.openems.edge.controller.symmetric.linearpowerband;

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
@Component(name = "Controller.Symmetric.LinearPowerBand", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class SymmetricLinearPowerBand extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(SymmetricLinearPowerBand.class);

	@Reference
	protected ConfigurationAdmin cm;

	private int currentPower = 0;
	private int minPower = 0;
	private int maxPower = 0;
	private int adjustPower = 0;
	private State state = State.DOWNWARDS;

	enum State {
		DOWNWARDS, UPWARDS
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "ess", config.ess_id())) {
			return;
		}

		this.minPower = config.minPower();
		this.maxPower = config.maxPower();
		this.adjustPower = config.adjustPower();
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		switch (this.state) {
		case DOWNWARDS:
			// adjust Power
			this.currentPower -= this.adjustPower;
			if (this.currentPower < this.minPower) {
				// switch to discharge
				this.state = State.UPWARDS;
			}
			break;
		case UPWARDS:
			// adjust Power
			this.currentPower += this.adjustPower;
			if (this.currentPower > this.maxPower) {
				// switch to discharge
				this.state = State.DOWNWARDS;
			}
			break;
		}

		// adjust value so that it fits into Min/MaxActivePower
		int calculatedPower = ess.getPower().fitValueIntoMinMaxActivePower(ess, Phase.ALL, Pwr.ACTIVE, this.currentPower);

		/*
		 * set result
		 */
		try {
			this.ess.addPowerConstraintAndValidate("SymmetricLinearPowerBand", Phase.ALL, Pwr.ACTIVE,
					Relationship.EQUALS, calculatedPower); //
		} catch (PowerException e) {
			this.logError(this.log, e.getMessage());
		}
	}
}
