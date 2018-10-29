package io.openems.edge.controller.asymmetric.balancingcosphi;

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

import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedAsymmetricEss;
import io.openems.edge.ess.power.api.Constraint;
import io.openems.edge.ess.power.api.LinearCoefficient;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.AsymmetricMeter;

@Designate(ocd = Config.class, factory = true)
@Component(name = "Controller.Asymmetric.BalancingCosPhi", immediate = true, configurationPolicy = ConfigurationPolicy.REQUIRE)
public class CosPhi extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	public final static CosPhiDirection DEFAULT_DIRECTION = CosPhiDirection.CAPACITIVE;
	public final static double DEFAULT_COS_PHI = 1d;

	private final Logger log = LoggerFactory.getLogger(CosPhi.class);

	@Reference
	protected ConfigurationAdmin cm;

	private CosPhiDirection direction = DEFAULT_DIRECTION;
	private double cosPhi = DEFAULT_COS_PHI;

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

		this.direction = config.direction();
		this.cosPhi = Math.abs(config.cosPhi());
	}

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedAsymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private AsymmetricMeter meter;

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		this.addConstraint(Phase.L1, this.meter.getActivePowerL1(), this.meter.getReactivePowerL1(),
				this.ess.getActivePowerL1(), this.ess.getReactivePowerL1());
		this.addConstraint(Phase.L2, this.meter.getActivePowerL2(), this.meter.getReactivePowerL2(),
				this.ess.getActivePowerL2(), this.ess.getReactivePowerL2());
		this.addConstraint(Phase.L3, this.meter.getActivePowerL3(), this.meter.getReactivePowerL3(),
				this.ess.getActivePowerL3(), this.ess.getReactivePowerL3());
	}

	private void addConstraint(Phase phase, Channel<Integer> meterActivePower, Channel<Integer> meterReactivePower,
			Channel<Integer> essActivePower, Channel<Integer> essReactivePower) {
		// Calculate the startpoint of the cosPhi line in relation to the ess zero power
		long pNull = meterActivePower.value().orElse(0) + essActivePower.value().orElse(0);
		long qNull = meterReactivePower.value().orElse(0) + essReactivePower.value().orElse(0);
		double m = Math.tan(Math.acos(Math.abs(cosPhi)));
		if (this.direction == CosPhiDirection.INDUCTIVE) {
			m *= -1;
		}
		System.out.println("Steigung [" + m + "] pNull [" + pNull + "] qNull [" + qNull + "]");

		double staticValueOfEquation = m * pNull * qNull;

		try {
			Power power = this.ess.getPower();
			Constraint c = new Constraint(ess.id() + phase + ": CosPhi [" + cosPhi + "]", new LinearCoefficient[] { //
					new LinearCoefficient(power.getCoefficient(this.ess, phase, Pwr.ACTIVE), m), //
					new LinearCoefficient(power.getCoefficient(this.ess, phase, Pwr.REACTIVE), 1) //
			}, Relationship.EQUALS, staticValueOfEquation);
			System.out.println("Add CosPhi: " + c);
			power.addConstraintAndValidate(c);
		} catch (PowerException e) {
			this.logError(this.log, e.getMessage());
		}
	}
}
