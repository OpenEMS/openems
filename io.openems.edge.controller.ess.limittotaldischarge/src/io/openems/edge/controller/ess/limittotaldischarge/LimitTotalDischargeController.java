package io.openems.edge.controller.ess.limittotaldischarge;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.TemporalAmount;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Level;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.AvoidTotalChargeDischarge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class LimitTotalDischargeController extends AbstractOpenemsComponent implements Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(LimitTotalDischargeController.class);

	private final Clock clock;

	@Reference
	protected ComponentManager componentManager;

	/**
	 * Length of hysteresis in seconds. States are not changed quicker than this.
	 */
	private final TemporalAmount hysteresis = Duration.ofMinutes(5);
	private LocalDateTime lastStateChange = LocalDateTime.MIN;

	private String essId;
	private int minSoc = 0;
	private int forceChargeSoc = 0;
	private Optional<Integer> forceChargePower = Optional.empty();
	private State state = State.NORMAL;

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		STATE_MACHINE(new Doc() //
				.level(Level.INFO) //
				.text("Current State of State-Machine") //
				.options(State.values())), //
		AWAITING_HYSTERESIS(new Doc() //
				.level(Level.INFO) //
				.text("Would change State, but hystesis is active") //
				.options(State.values())); //

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
		this(Clock.systemDefaultZone());
	}

	protected LimitTotalDischargeController(Clock clock) {
		this.clock = clock;
		Utils.initializeChannels(this).forEach(channel -> this.addChannel(channel));
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.enabled());

		this.essId = config.ess_id();
		this.minSoc = config.minSoc();
		this.forceChargeSoc = config.forceChargeSoc();
		if (config.forceChargePower() > 0) {
			// apply configured force charge power if it is was set and it is greater than 0
			this.forceChargePower = Optional.of(config.forceChargePower());
		}

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
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.essId);

		// Set to normal state and return if SoC is not available
		Optional<Integer> socOpt = ess.getSoc().value().asOptional();
		if (!socOpt.isPresent()) {
			this.state = State.NORMAL;
			return;
		}
		int soc = socOpt.get();

		// initialize force Charge
		Integer calculatedPower = null;

		boolean stateChanged;
		do {
			stateChanged = false;

			switch (this.state) {
			case UNDEFINED:
			case NORMAL:
				/*
				 * Normal State
				 */
				// no constraints in normal operation mode
				calculatedPower = null;

				if (soc <= this.forceChargeSoc) {
					stateChanged = this.changeState(State.FORCE_CHARGE_SOC);
					break;
				} else if (soc <= this.minSoc) {
					stateChanged = this.changeState(State.MIN_SOC);
					break;
				}
				break;

			case MIN_SOC:
				/*
				 * Min-SoC State
				 */
				// Deny further discharging: set Constraint for ActivePower <= 0
				calculatedPower = 0;

				if (soc <= this.forceChargeSoc) {
					stateChanged = this.changeState(State.FORCE_CHARGE_SOC);
					break;
				}
				if (soc > this.minSoc) {
					stateChanged = this.changeState(State.NORMAL);
					break;
				}
				break;

			case FORCE_CHARGE_SOC:
				/*
				 * Force-Charge-SoC State
				 */
				// Force charge: set Constraint for ActivePower
				if (this.forceChargePower.isPresent()) {
					calculatedPower = this.forceChargePower.get() * -1; // convert to negative for charging
				} else {
					int maxCharge = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
					calculatedPower = maxCharge / 5;
				}

				if (soc > this.forceChargeSoc) {
					stateChanged = this.changeState(State.MIN_SOC);
					break;
				}
				break;
			}

		} while (stateChanged); // execute again if the state changed

		// adjust value so that it fits into Min/MaxActivePower
		if (calculatedPower != null) {
			calculatedPower = ess.getPower().fitValueIntoMinMaxActivePower(ess, Phase.ALL, Pwr.ACTIVE, calculatedPower);
		}

		// Apply Force-Charge if it was set
		try {
			ess.getSetActivePowerLessOrEquals().setNextWriteValue(calculatedPower);
		} catch (OpenemsException e) {
			this.logError(this.log, e.getMessage());
		}

		// store current state in StateMachine channel
		this.channel(ChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	/**
	 * Changes the state if hysteresis time passed, to avoid too quick changes.
	 * 
	 * @param nextState the target state
	 * @return whether the state was changed
	 */
	private boolean changeState(State nextState) {
		if (this.state != nextState) {
			if (this.lastStateChange.plus(this.hysteresis).isBefore(LocalDateTime.now(this.clock))) {
				this.state = nextState;
				this.lastStateChange = LocalDateTime.now(this.clock);
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
				return true;
			} else {
				this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(true);
				return false;
			}
		} else {
			this.channel(ChannelId.AWAITING_HYSTERESIS).setNextValue(false);
			return false;
		}
	}
}
