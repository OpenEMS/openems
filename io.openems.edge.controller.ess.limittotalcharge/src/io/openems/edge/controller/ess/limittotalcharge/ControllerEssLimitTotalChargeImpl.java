package io.openems.edge.controller.ess.limittotalcharge;

import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.controller.ess.limittotalcharge.EnergyScheduler.buildEnergyScheduleHandler;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static org.osgi.service.component.annotations.ConfigurationPolicy.REQUIRE;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
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
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.LimitTotalCharge", //
		immediate = true, //
		configurationPolicy = REQUIRE //
)
public class ControllerEssLimitTotalChargeImpl extends AbstractOpenemsComponent
		implements ControllerEssLimitTotalCharge, EnergySchedulable, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerEssLimitTotalChargeImpl.class);

	@Reference
	private ComponentManager componentManager;

	/**
	 * Length of hysteresis in minutes. States are not changed quicker than this.
	 */
	private static final int HYSTERESIS = 5;
	private Instant lastStateChange = Instant.MIN;

	private EnergyScheduleHandler energyScheduleHandler;
	private String essId;
	private HybridEssMode hybridEssMode;
	private int maxSoc = 0;
	private int forceDischargeSoc = 0;
	private Optional<Integer> forceDischargePower = Optional.empty();
	private State state = State.NORMAL;

	public ControllerEssLimitTotalChargeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssLimitTotalCharge.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.energyScheduleHandler = buildEnergyScheduleHandler(this, //
				() -> new EnergyScheduler.Config(this.isEnabled() //
						? this.maxSoc //
						: null));

		this.essId = config.ess_id();
		this.hybridEssMode = config.hybridEssMode();
		this.maxSoc = config.maxSoc();
		this.forceDischargeSoc = config.forceDischargeSoc();

		// Set the MaxSoc channel.
		this._setMaxSoc(this.maxSoc);

		// Parse Force-Discharge-Power
		int forceDischargePower;
		try {
			forceDischargePower = config.forceDischargePower();
		} catch (Exception e) {
			// happens if optional configuration parameter is not given.
			forceDischargePower = 0;
		}
		if (forceDischargePower > 0) {
			// apply configured force discharge power if it is was set and it is greater than 0
			this.forceDischargePower = Optional.of(config.forceDischargePower());
		} else {
			this.forceDischargePower = Optional.empty();
		}

		// Force-Discharge-SoC must be greater than Max-SoC.
		if (this.forceDischargeSoc <= this.maxSoc) {
			this.forceDischargeSoc = this.maxSoc + 1;
			this.logWarn(this.log,
					"Force-Discharge-SoC [" + config.forceDischargeSoc() + "] is invalid in combination with Max-SoC ["
							+ config.maxSoc() + "]. Setting it to [" + this.forceDischargeSoc + "]");
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

		// initialize force Discharge
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

				if (soc >= this.forceDischargeSoc) {
					stateChanged = this.changeState(State.FORCE_DISCHARGE_SOC);
					break;
				}
				if (soc >= this.maxSoc) {
					stateChanged = this.changeState(State.MAX_SOC);
					break;
				}
				break;

			case MAX_SOC:
				/*
				 * Max-SoC State
				 */
				// Deny further charging: set Constraint for ActivePower >= 0.
				calculatedPower = getAcPower(ess, this.hybridEssMode, 0);

				if (soc >= this.forceDischargeSoc) {
					stateChanged = this.changeState(State.FORCE_DISCHARGE_SOC);
					break;
				}
				if (soc < this.maxSoc) {
					stateChanged = this.changeState(State.NORMAL);
					break;
				}
				break;

			case FORCE_DISCHARGE_SOC:
				/*
				 * Force-Discharge-SoC State
				 */
				// Force discharge: set Constraint for ActivePower.
				if (this.forceDischargePower.isPresent()) {
					calculatedPower = getAcPower(ess, this.hybridEssMode, //
							this.forceDischargePower.get()); // positive for discharging
				} else {
					var maxDischarge = ess.getPower().getMaxPower(ess, ALL, ACTIVE);
					// For Hybrid-ESS in TARGET_DC, neutral is local PV: zero battery discharge.
					var neutral = getAcPower(ess, this.hybridEssMode, 0);
					// Use one fifth of the remaining discharge headroom, then restore the PV offset.
					calculatedPower = neutral + Math.max(0, maxDischarge - neutral) / 5;
				}

				if (soc < this.forceDischargeSoc) {
					stateChanged = this.changeState(State.MAX_SOC);
					break;
				}
				break;
			}

		} while (stateChanged); // execute again if the state changed

		// adjust value so that it fits into Min/MaxActivePower
		if (calculatedPower != null) {
			calculatedPower = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, ALL, ACTIVE, calculatedPower);
		}

		// Apply Force-Discharge if it was set
		ess.setActivePowerGreaterOrEquals(calculatedPower);

		// store current state in StateMachine channel
		this.channel(ControllerEssLimitTotalCharge.ChannelId.STATE_MACHINE).setNextValue(this.state);
	}

	/**
	 * Gets the required AC power set-point for AC- or Hybrid-ESS.
	 *
	 * @param ess           the {@link ManagedSymmetricEss}; checked for
	 *                      {@link HybridEss}
	 * @param hybridEssMode the {@link HybridEssMode}
	 * @param power         the configured target power
	 * @return the AC power set-point
	 */
	protected static int getAcPower(ManagedSymmetricEss ess, HybridEssMode hybridEssMode, int power) {
		return switch (hybridEssMode) {
		case TARGET_AC -> power;

		case TARGET_DC -> //
			switch (ess) {
			case HybridEss he -> {
				var pv = ess.getActivePower().orElse(0) - he.getDcDischargePower().orElse(0);
				pv = pv > 0 ? pv : 0; // avoid negative numbers
				yield pv + power; // Charge or Discharge
			}
			default -> power;
			};
		};
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

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}
}
