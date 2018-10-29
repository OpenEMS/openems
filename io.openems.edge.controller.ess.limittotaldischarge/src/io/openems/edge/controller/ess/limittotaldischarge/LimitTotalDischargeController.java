package io.openems.edge.controller.ess.limittotaldischarge;

import java.time.LocalDateTime;
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

import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.channel.doc.OptionsEnum;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.PowerException;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;

@Designate(ocd = Config.class, factory = true)
@Component( //
		name = "Controller.Ess.AvoidTotalChargeDischarge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LimitTotalDischargeController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(LimitTotalDischargeController.class);

	/**
	 * Length of hysteresis in seconds. States are not changed quicker than this.
	 */
	private final int hysteresis = 5 * 60;
	private LocalDateTime lastStateChange = LocalDateTime.MIN;

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
		STATE_MACHINE(new Doc().level(Level.INFO).text("Current State of State-Machine").options(State.values())), //
		AWAITING_HYSTERESIS(
				new Doc().level(Level.INFO).text("Would change State, but hystesis is active").options(State.values())); //

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
		// Set to normal state and return if SoC is not available
		Optional<Integer> socOpt = this.ess.getSoc().value().asOptional();
		if (!socOpt.isPresent()) {
			this.state = State.NORMAL;
			return;
		}
		int soc = socOpt.get();

		// initialize force Charge
		Optional<Integer> calculatedPower = Optional.empty();

		State nextState = this.state;
		switch (this.state) {
		case NORMAL:
			/*
			 * Normal State
			 */
			// no constraints in normal operation mode

			if (soc <= this.forceChargeSoc) {
				nextState = State.FORCE_CHARGE_SOC;
				break;
			} else if (soc <= this.minSoc) {
				nextState = State.MIN_SOC;
				break;
			}
			break;

		case MIN_SOC:
			/*
			 * Min-SoC State
			 */
			// Deny further discharging: set Constraint for ActivePower <= 0
			calculatedPower = Optional.of(0);

			if (soc <= this.forceChargeSoc) {
				nextState = State.FORCE_CHARGE_SOC;
				break;
			}
			if (soc > this.minSoc) {
				nextState = State.NORMAL;
				break;
			}
			break;

		case FORCE_CHARGE_SOC:
			/*
			 * Force-Charge-SoC State
			 */
			// Force charge: set Constraint for ActivePower
			int maxCharge = this.ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
            calculatedPower = Optional.of(maxCharge / 5);

			if (soc > this.forceChargeSoc) {
				nextState = State.MIN_SOC;
				break;
			}
			break;
		}

		// Apply Force-Charge if it was set
		if (calculatedPower.isPresent()) {
			try {
				this.ess.addPowerConstraintAndValidate("LimitTotalDischarge", Phase.ALL, Pwr.ACTIVE,
						Relationship.LESS_OR_EQUALS, calculatedPower.get());
			} catch (PowerException e) {
				this.logError(this.log, e.getMessage());
			}
		}

		/*
		 * Do we have a new State? Change State only if hysteresis time passed, to avoid
		 * too quick changes
		 */
		if (this.state != nextState) {
			if (this.lastStateChange.plusSeconds(this.hysteresis).isBefore(LocalDateTime.now())) {
				this.state = nextState;
				this.lastStateChange = LocalDateTime.now();
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
			} else {
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(true);
			}
		} else {
			this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
		}

		// store current state in StateMachine channel
		this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);
	}
}
