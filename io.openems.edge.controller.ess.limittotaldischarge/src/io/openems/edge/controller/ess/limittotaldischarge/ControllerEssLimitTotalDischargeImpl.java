package io.openems.edge.controller.ess.limittotaldischarge;

import java.time.Duration;
import java.time.Instant;
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

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.LimitTotalDischarge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssLimitTotalDischargeImpl extends AbstractOpenemsComponent
		implements ControllerEssLimitTotalDischarge, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerEssLimitTotalDischargeImpl.class);

	@Reference
	protected ComponentManager componentManager;

	/**
	 * Length of hysteresis in minutes. States are not changed quicker than this.
	 */
	private static final int HYSTERESIS = 5;
	private Instant lastStateChange = Instant.MIN;

	private String essId;
	private int minSoc = 0;
	private int forceChargeSoc = 0;
	private Optional<Integer> forceChargePower = Optional.empty();
	private State state = State.NORMAL;

	public ControllerEssLimitTotalDischargeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssLimitTotalDischarge.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.essId = config.ess_id();
		this.minSoc = config.minSoc();
		this.forceChargeSoc = config.forceChargeSoc();

		// Set the MinSoc channel.
		this._setMinSoc(this.minSoc);

		// Parse Force-Charge-Power
		int forceChargePower;
		try {
			forceChargePower = config.forceChargePower();
		} catch (Exception e) {
			// happens if optional configuration parameter is not given.
			forceChargePower = 0;
		}
		if (forceChargePower > 0) {
			// apply configured force charge power if it is was set and it is greater than 0
			this.forceChargePower = Optional.of(config.forceChargePower());
		} else {
			this.forceChargePower = Optional.empty();
		}

		// Force-Charge-SoC must be smaller than Min-SoC
		if (this.forceChargeSoc >= this.minSoc) {
			this.forceChargeSoc = this.minSoc - 1;
			this.logWarn(this.log,
					"Force-Charge-SoC [" + config.forceChargeSoc() + "] is invalid in combination with Min-SoC ["
							+ config.minSoc() + "]. Setting it to [" + this.forceChargeSoc + "]");
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
		ManagedSymmetricEss ess = this.componentManager.getComponent(this.essId);

		// Set to normal state and return if SoC is not available
		var socOpt = ess.getSoc();
		if (!socOpt.isDefined()) {
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
				}
				if (soc <= this.minSoc) {
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
					var maxCharge = ess.getPower().getMinPower(ess, Phase.ALL, Pwr.ACTIVE);
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
			calculatedPower = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.ACTIVE,
					calculatedPower);
		}

		// Apply Force-Charge if it was set
		ess.setActivePowerLessOrEquals(calculatedPower);

		// store current state in StateMachine channel
		this.channel(ControllerEssLimitTotalDischarge.ChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	/**
	 * Changes the state if hysteresis time passed, to avoid too quick changes.
	 *
	 * @param nextState the target state
	 * @return whether the state was changed
	 */
	private boolean changeState(State nextState) {
		if (this.state == nextState) {
			this._setAwaitingHysteresisValue(false);
			return false;
		}
		if (Duration.between(//
				this.lastStateChange, //
				Instant.now(this.componentManager.getClock()) //
		).toMinutes() >= HYSTERESIS) {
			this.state = nextState;
			this.lastStateChange = Instant.now(this.componentManager.getClock());
			this._setAwaitingHysteresisValue(false);
			return true;
		} else {
			this._setAwaitingHysteresisValue(true);
			return false;
		}
	}
}
