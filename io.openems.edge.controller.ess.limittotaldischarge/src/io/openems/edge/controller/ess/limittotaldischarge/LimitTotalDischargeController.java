package io.openems.edge.controller.ess.limittotaldischarge;

import java.util.Optional;

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

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.ConstraintType;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Controller.Ess.AvoidTotalChargeDischarge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LimitTotalDischargeController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(LimitTotalDischargeController.class);

	private int minSoc = 0;
	private int forceChargeSoc = 0;
	private State state = State.NORMAL;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	public enum State implements OptionsEnum {
		NORMAL(0, "Normal"), MIN_SOC(1, "Min-SoC"), FORCE_CHARGE_SOC(2, "Force-Charge-SoC");

		private final int value;
		private final String option;

		private State(int value, String option) {
			this.value = value;
			this.option = option;
		}

		@Override
		public int getValue() {
			return value;
		}

		@Override
		public String getOption() {
			return option;
		}
	}

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		STATE_MACHINE(new Doc().level(Level.INFO).text("Current State of State-Machine").options(State.values())); //

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public LimitTotalDischargeController() {
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.service_pid(), config.id(), config.enabled());
		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(cm, config.service_pid(), "ess", config.ess_id())) {
			return;
		}

		this.minSoc = config.minSoc();
		this.forceChargeSoc = config.forceChargeSoc();

		// Force-Charge-SoC must be smaller than Min-SoC
		if (this.forceChargeSoc >= this.minSoc) {
			this.forceChargeSoc = this.minSoc - 1;
			this.logWarn(this.log,
					"Force-Charge-SoC [" + config.forceChargeSoc() + "] is invalid in combination with Min-SoC ["
							+ config.minSoc() + "]. Setting it to [" + this.forceChargeSoc + "]");
		}
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() {
		// store current state in StateMachine channel
		this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);
		
		Optional<Integer> socOpt = this.ess.getSoc().value().asOptional();

		// Set to normal state and return if SoC is not available
		if (!socOpt.isPresent()) {
			this.state = State.NORMAL;
			return;
		}
		int soc = socOpt.get();

		switch (this.state) {
		case NORMAL:
			/*
			 * Normal State
			 */
			if (soc <= this.minSoc) {
				this.state = State.MIN_SOC;
				return;
			}
			// no constraints in normal operation mode
			break;
		case MIN_SOC:
			/*
			 * Min-SoC State
			 */
			if (soc <= this.forceChargeSoc) {
				this.state = State.FORCE_CHARGE_SOC;
				return;
			}
			if (soc > this.minSoc) {
				this.state = State.NORMAL;
				return;
			}
			// Deny further discharging: set Constraint for ActivePower <= 0
			this.ess.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.LEQ, 0);
			break;
		case FORCE_CHARGE_SOC:
			/*
			 * Force-Charge-SoC State
			 */
			if (soc > this.minSoc) {
				this.state = State.MIN_SOC;
				return;
			}
			// Force charge: set Constraint for ActivePower <= MAX_CHARGE / 5
			int chargePower = this.ess.getPower().getMinActivePower() / 5;
			this.ess.addPowerConstraint(ConstraintType.CYCLE, Phase.ALL, Pwr.ACTIVE, Relationship.LEQ, chargePower);
			break;
		}
	}
}
