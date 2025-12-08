package io.openems.edge.controller.ess.chargedischargelimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

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

import io.openems.edge.common.modbusslave.ModbusSlave;
import io.openems.edge.common.modbusslave.ModbusSlaveNatureTable;
import io.openems.edge.common.modbusslave.ModbusSlaveTable;

import io.openems.common.channel.AccessMode;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.ChannelAddress;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.common.type.Phase.SingleOrAllPhase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.common.type.TypeUtils;

import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.controller.symmetric.thresholdpeakshaver.ControllerEssThresholdPeakshaver;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;

import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.ChargeDischargeLimiter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssChargeDischargeLimiterImpl extends AbstractOpenemsComponent
		implements ControllerEssChargeDischargeLimiter, Controller, OpenemsComponent, EnergySchedulable, ModbusSlave {

	private final Logger log = LoggerFactory.getLogger(ControllerEssChargeDischargeLimiterImpl.class);

	private Config config;

	/**
	 * Length of hysteresis in seconds. States are not changed quicker than this.
	 * 
	 */
	private static final int HYSTERESIS = 10; // seconds
	private Instant lastStateChangeTime = Instant.MIN;
	private Instant balancingStartTime = Instant.MIN;
	private long balancingTime = 0; // Time used for balancing so far
	private int balancingRemainingTime = 0; // Remaining time for balancing. Used in UI
	private Long lastEssActiveChargeEnergy = null;
	private Integer cumulatedchargedEnergy = 0;
	private boolean resetChargedEnergy = false;

	private int minSoc = 0;

	private int maxSoc = 0;
	private int forceChargeSoc = 0;
	private int forceChargePower = 0;
	private int energyBetweenBalancingCycles = 0;
	private int balancingHysteresisTime = 0;
	private State state = State.UNDEFINED;

	private boolean debugMode = false;
	private Integer slowChargePower = null;
	private Integer slowDisChargePower = null;
	private boolean autoDischarge = false;

	private int taperStartSoc = 0;
	private int taperPercent = 3; // decrease charge power during the last X percent before hitting the max. Soc
	private int fullChargePower = 0;

	@Reference
	private ComponentManager componentManager;

	private EnergyScheduleHandler energyScheduleHandler;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private ManagedSymmetricEss ess;

	@Reference(policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE, //
			target = "(enabled=true)")
	private List<ControllerEssThresholdPeakshaver> ctrlEssThresholdPeakshavers = new CopyOnWriteArrayList<>();

	@Reference
	private TimeOfUseTariff timeOfUseTariff;

	public ControllerEssChargeDischargeLimiterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssChargeDischargeLimiter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.config = config;

		this.updateConfig(config);

		this.energyScheduleHandler = io.openems.edge.controller.ess.chargedischargelimiter.EnergyScheduler
				.buildEnergyScheduleHandler(this,
						() -> this.config.enabled()
								? new io.openems.edge.controller.ess.chargedischargelimiter.EnergyScheduler.Config(
										this.config.minSoc(), this.config.maxSoc())
								: null);

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	protected void modified(ComponentContext context, String id, String alias, boolean enabled) {
		super.modified(context, id, alias, enabled);
		this.updateConfig(this.config);
		if (this.energyScheduleHandler != null) {
			this.energyScheduleHandler.triggerReschedule("ControllerEssChargeDischargeLimiterImpl::modified()");
		}
	}

	private void updateConfig(Config config) {
		this.config = config;
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

		this.minSoc = this.config.minSoc();
		this.maxSoc = this.config.maxSoc();
		this.autoDischarge = this.config.autoDischarge();
		this.forceChargeSoc = this.config.forceChargeSoc();
		this.forceChargePower = this.config.forceChargePower();
		this.energyBetweenBalancingCycles = this.config.energyBetweenBalancingCycles() * 1000; // kWh -> Wh
		this.balancingHysteresisTime = this.config.balancingHysteresis();
		this.debugMode = this.config.debugMode();

		this.taperStartSoc = this.config.maxSoc() - this.taperPercent;

	}

	/**
	 * The channel for cumulated chargedEnergy is not available at startup or even
	 * 0. So we have to get the latest value from timedata.
	 */
	private void initializeChargedEnergyFromTimedata() {

		// ControllerEssChargeDischargeLimiter.ChannelId.STATE_MACHINE;
		var timedata = this.getTimedata();

		if (timedata == null) {
			this.logDebug(this.log, "Timedata service is not available.");
			return; // Exit if timedata service is not available
		}

		this.logDebug(this.log, "Querying Timedata service for the latest energy value...");

		this.timedata
				.getLatestValue(new ChannelAddress(this.id(),
						ControllerEssChargeDischargeLimiter.ChannelId.CHARGED_ENERGY.id()))
				.thenAccept(chargedEnergy -> {
					if (chargedEnergy.isPresent()) {
						Integer value = TypeUtils.getAsType(OpenemsType.INTEGER, chargedEnergy.get());
						this.logDebug(this.log, "Fetched latest ChargedEnergy value: " + value);
						this._setChargedEnergy(value);
					} else {
						this.logDebug(this.log, "No current energy value found for ChargedEnergy channel");
						this._setChargedEnergy(0);
					}
				});
	}

	private Object getTimedata() {
		return this.timedata;
	}

	private void setEssProperties() {
		// Initial values
		this.slowChargePower = 500;
		this.slowDisChargePower = 500;
		this.fullChargePower = 500;

		if (this.ess == null || this.ess.getAllowedChargePower().get() == null) {
			this.changeState(State.UNDEFINED);
			this.logDebug(this.log,
					"Waiting for ESS initialization to set slow charge and discharge power. Setting minimum values");
			return;
		}

		// Charge power must be positive
		if (this.ess.getAllowedChargePower().get() > 0) {
			this.logDebug(this.log, "Unable to set slow charge / discharge power");
			return;
		}

		this.slowChargePower = this.ess.getAllowedChargePower().get() / 20;
		this.slowDisChargePower = this.ess.getAllowedDischargePower().get() / 20;
		this.fullChargePower = this.ess.getAllowedChargePower().get();

		this.logDebug(this.log, "Initialized slow charge and discharge power.");
		return;

	}

	@Override
	public void run() throws OpenemsNamedException {

		this.setEssProperties();

		if (this.ess == null) {
			this.changeState(State.ERROR);
			this.log.error("ESS is null in run method! Aborting execution.");
			return;
		}

		Integer currentSoc = this.ess.getSoc().get();
		Integer currentActivePower = this.getEssChargePower().get(); // no matter if AC or DC charging
		Integer calculatedPower = 0; // No constraints

		this.updateUsableSocAndCapacity(currentSoc);

		// Remember: Negative values for Charge; positive for Discharge
		this.logDebug(this.log, "Number of Peakshaving controllers found: " + this.ctrlEssThresholdPeakshavers.size());

		switch (this.state) {
		case UNDEFINED:
			if (this.ess == null) {
				this.logDebug(this.log, "ERROR. ESS " + this.config.ess_id() + " unavailable ");
				return;
			}
			// check if we can change to normal operation, i.e. if SOC and activePower
			// values are available
			if (currentSoc != null && currentActivePower != null) {
				this.changeState(State.NORMAL);
			}
			break;
		case NORMAL:
			//
			this._setBalancingRemainingSeconds(0); // no remaining time in normal operation
			// check if charge energy is below the next balancing cycle. Only balance if
			// this is desired
			if (this.shouldBalance()) {
				this.changeState(State.BALANCING_WANTED);
				break;
			} else if (currentSoc < this.minSoc) {
				this.changeState(State.BELOW_MIN_SOC);
				break;
			} else if (currentSoc > this.maxSoc) {
				this.changeState(State.ABOVE_MAX_SOC);
				break;
			} else if (currentSoc.equals(this.minSoc)) {
				this.changeState(State.MIN_SOC_REACHED);
				break;
			} else if (currentSoc.equals(this.maxSoc)) {
				this.changeState(State.MAX_SOC_REACHED);
				break;
			}

			// Tapering logic: Gradual power reduction as we approach maxSoc
			if (currentSoc >= this.taperStartSoc && currentSoc < this.maxSoc && currentActivePower < 0) {
				// Calculate tapering factor to gradually reduce power as SOC approaches max
				double taperFactor = Math.pow((double) (this.maxSoc - currentSoc) / (this.maxSoc - this.taperStartSoc), 2); // quadratic
				// taper
				calculatedPower = (int) (this.fullChargePower * taperFactor);
				this.logDebug(this.log, "Reducing charge power as SOC approaches max: " + calculatedPower + "W");
			}
			break;
		case ERROR:
			// log errors
			break;
		case MIN_SOC_REACHED:
			if (currentSoc > this.minSoc) {
				this.changeState(State.NORMAL);
				break;
			} else if (currentSoc < this.minSoc) {
				this.changeState(State.BELOW_MIN_SOC);
				break;
			}
			calculatedPower = 0;
			break;
		case MAX_SOC_REACHED:

			if (currentSoc < this.maxSoc) {
				this.changeState(State.NORMAL);
				break;
			} else if (currentSoc > this.maxSoc) {
				this.changeState(State.ABOVE_MAX_SOC);
				break;
			}

			calculatedPower = 0;
			break;
		case BELOW_MIN_SOC:
			// block discharging and slowly charge
			calculatedPower = this.slowChargePower != null ? this.slowChargePower : calculatedPower;
			if (currentSoc == this.minSoc) {
				this.changeState(State.MIN_SOC_REACHED);
			} else if (currentSoc > this.minSoc) {
				this.changeState(State.NORMAL);
			}
			break;
		case ABOVE_MAX_SOC:

			if (this.slowDisChargePower != null && this.autoDischarge) {
				calculatedPower = this.slowDisChargePower; // discharge slowly if autoDischarge is configured
			}

			if (currentSoc == this.maxSoc) {
				this.changeState(State.MAX_SOC_REACHED);
			} else if (currentSoc < this.maxSoc) {
				this.changeState(State.NORMAL);
			}
			break;
		case FORCE_CHARGE_ACTIVE:
			// force charge with forceChargePower
			// Charge battery with desired power
			// Check wether it has reached desired SOC
			if (!this.shouldBalance()) {
				this.changeState(State.NORMAL);
				break;
			}

			calculatedPower = this.forceChargePower * -1;

			if (currentSoc >= this.forceChargeSoc) {
				this.changeState(State.BALANCING_ACTIVE);
			}
			break;
		case BALANCING_WANTED:
			// State can be used to check things. Don´t know what, yet ;o)

			// Check again if balancing is necessary
			if (!this.shouldBalance()) {
				this.changeState(State.NORMAL);
				break;
			}

			if (!this.isWithinPriceLimit()) {
				this.changeState(State.PRICE_LIMIT);
				break;
			}

			this.changeState(State.FORCE_CHARGE_ACTIVE);

			break;
		case PRICE_LIMIT:

			// Check again if balancing is necessary
			if (!this.shouldBalance()) {
				this.changeState(State.NORMAL);
				break;
			}

			// no transition if price exceeds limit
			if (!this.isWithinPriceLimit()) {
				this.logDebug(this.log, "Balancing is desired but price exceeds limit \n");
				break;
			}

			this.changeState(State.FORCE_CHARGE_ACTIVE);
			break;
		case BALANCING_ACTIVE:

			if (currentSoc == null) {
				this.logDebug(this.log, "SOC not available.");
				break;
			}
			// Start Timer
			if (this.balancingStartTime.equals(Instant.MIN)) {
				// Start the balancing timer
				this.balancingStartTime = Instant.now(this.componentManager.getClock());
				this.log.info("Balancing started at " + this.balancingStartTime);
				// ToDo: channel for balancing remaining time needed for UI modal
			}

			if (!this.shouldBalance()) {
				this.resetBalancingTimers(false);
				this.changeState(State.NORMAL);
				break;
			}

			// Check if balancing duration has passed
			if (this.isBalancingDurationExceeded()) {
				// Balancing time is over, transition to NORMAL state
				this.resetBalancingTimers(true);
				this.changeState(State.NORMAL);

			} else {
				this.balancingRemainingTime = (int) (this.balancingHysteresisTime - this.balancingTime);
				this.logDebug(this.log, "Balancing active since " + this.balancingStartTime + "|Remaining: "
						+ this.balancingRemainingTime + "s \n");

				//
				this._setBalancingRemainingSeconds(this.balancingRemainingTime);

				if (currentSoc < (this.forceChargeSoc - 1)) {
					// SOC dropped below forceChargeSoc: reset balancing
					this.logDebug(this.log, "SOC dropped below " + this.forceChargeSoc + "%. Restarting balancing.");
					this.changeState(State.BALANCING_WANTED);
					this.resetBalancingTimers(false);
					break;
				} else if (currentSoc <= (this.forceChargeSoc + 1)) {
					// SOC slightly below target, maintain SOC with force charging
					calculatedPower = this.forceChargePower * -1; // Charging has a negative value
				} else {
					// SOC is sufficient, no charging or discharging needed
					calculatedPower = 0; // block further discharging if SOC is sufficient
				}

			}
			break;

		}
		this.applyActivePowerConstraint(calculatedPower);
		this.calculateChargedEnergy();

		this._setMaxSoc(this.maxSoc);
		this._setMinSoc(this.minSoc);
		this._setBalancingSoc(this.forceChargeSoc);

		// save current state channel
		this._setStateMachine(this.state);

		this.logDebug(this.log,
				"\nCurrent State " + this.state.getName() + "\n" + "Current SoC " + this.ess.getSoc().get() + "% \n"
						+ "Current ActivePower " + this.getEssChargePower().get() + "W \n" + "Calculated ActivePower "
						+ calculatedPower + "W \n" + "Energy charged since last balancing "
						+ this.getChargedEnergy().get() + "Wh \n");

	}

	private void resetBalancingTimers(boolean fullReset) {
		this.logDebug(this.log, "\nBalancing finished or aborted. Resetting timers ");
		this.balancingStartTime = Instant.MIN; // Reset balancing start time
		this.balancingRemainingTime = this.balancingHysteresisTime; // Reset remaining balancing time

		if (fullReset) {
			this.logDebug(this.log, "\nBalancing finished. Going back to normal operation. Energy counters ");
			this.resetChargedEnergy = true; // Flag to reset charged energy
			this.cumulatedchargedEnergy = 0; // Optional: reset cumulative charge energy here
		}

	}

	private boolean isBalancingDurationExceeded() {
		// calculate balancing time so far to seconds
		this.balancingTime = Duration.between(this.balancingStartTime, Instant.now(this.componentManager.getClock()))
				.getSeconds();

		return this.balancingTime >= this.balancingHysteresisTime;
	}

	private boolean isWithinPriceLimit() {
		Integer currentPrice = 0;

		if (this.config.maxPrice() == 0) {
			return true;
		}

		if (this.timeOfUseTariff == null) {
			this.log.warn("TimeOfUseTariff service is null.");
			return true; // Ignore check if no ToU controller is available and no price can be obtained
		}
		currentPrice = (int) Math.round(this.timeOfUseTariff.getPrices().getFirst() / 10); // Price in €/MWh. Divided to ct/kWh
		// balancing is not desired
		if (currentPrice > this.config.maxPrice()) {
			this.logDebug(this.log, "Balancing is deactivated due to high price. Configured limit: "
					+ this.config.maxPrice() + " Current price: " + currentPrice + "[ct/kWh]");
			return false;
		}
		return true;

	}

	/**
	 * Calculates if the battery needs too be balanced. This depends on charged
	 * energy since the last balancing procedure. If charged energy exceeds
	 * configured energy method returns true.
	 * 
	 * <p>Balancing is blocked if a Peakshaver is active. "Active" means that it is in
	 * operating state - not neccessaryly active discharging the battery
	 * 
	 * @return if battery should be balanced
	 */
	private boolean shouldBalance() {

		Integer chargedEnergy = this.getChargedEnergy().get();

		if (chargedEnergy == null) {
			this.logDebug(this.log, "ERROR: Cannot determine charged energy");
			return false;
		}

		// are there any peakshavers active
		if (this.isPeakshavingActive() == true) {
			this.logDebug(this.log, "Balancing is deactivated due to active peakshaving");
			return false;
		}

		// balancing is not desired
		if (this.config.energyBetweenBalancingCycles() == 0) {
			this.logDebug(this.log, "Balancing is deactivated due to config setting");
			return false;
		}

		if (this.state == State.BALANCING_ACTIVE) {
			this.logDebug(this.log, "Balancing already active");
			return true;
		}

		if (chargedEnergy > this.energyBetweenBalancingCycles) {
			this.logDebug(this.log, "Balancing necessary because charged energy between balancing cycles exceeded");
			return true;
		}

		this.logDebug(this.log, "No Balancing necessary");
		return false;
	}

	/**
	 * Applies constraints to ess. Even FORCE_CHARGE is a constraint as the ESS is
	 * allowed to charge more than this controller´s desired value.
	 * 
	 * 
	 * @param calculatedPower as constraint
	 */
	void applyActivePowerConstraint(Integer calculatedPower) {
		if (calculatedPower == null) {
			return; // No constraints to set
		}

		if (this.ess == null) {
			this.logDebug(this.log, "ERROR. No Ess to apply constraints to");
			return;
		}

		try {
			switch (this.state) {
			case MAX_SOC_REACHED, ABOVE_MAX_SOC -> { // Block further charging
				this.ess.setActivePowerGreaterOrEquals(calculatedPower);
				this.logDebug(this.log, "ApplyPowerMethod -> setActivePowerGreaterOrEquals " + calculatedPower);

			}

			case MIN_SOC_REACHED, BELOW_MIN_SOC -> { // Block further discharging
				this.ess.setActivePowerLessOrEquals(calculatedPower);
				this.logDebug(this.log, "ApplyPowerMethod -> setActivePowerLessOrEquals " + calculatedPower);

			}

			case FORCE_CHARGE_ACTIVE, BALANCING_ACTIVE -> {
				// Fit calculated power within min/max limits and apply
				calculatedPower = this.ess.getPower().fitValueIntoMinMaxPower(this.id(), this.ess, SingleOrAllPhase.ALL,
						Pwr.ACTIVE, calculatedPower);
				this.ess.setActivePowerLessOrEquals(calculatedPower);

			}
			case NORMAL -> {
				if (calculatedPower < 0) { // ramp down charge power
					this.ess.setActivePowerGreaterOrEquals(calculatedPower);
					this.logDebug(this.log, "ApplyPowerMethod -> setActivePowerGreaterOrEquals " + calculatedPower);
				}
			}
			case BALANCING_WANTED ->  { // do nothing 
				}
			case PRICE_LIMIT -> { // do nothing
				// Waiting for price to fall below maxPrice. No constraints applied now.
			}
			case UNDEFINED -> { // do nothing
				}

			default -> throw new IllegalArgumentException("Unexpected State: " + this.state);
			}
		} catch (OpenemsNamedException e) {
			// ToDo: Handle exceptions, add logging
			e.printStackTrace();
		}
	}

	/**
	 * Changes the state if hysteresis time passed, to avoid too quick changes.
	 *
	 * @param nextState the target state
	 * @return whether the state was changed
	 */
	private boolean changeState(State nextState) {
		this.logDebug(this.log, "Change state " + this.state + "->" + nextState);
		if (this.state == nextState) {
			this._setAwaitingHysteresisValue(false);
			return false;
		}
		if (Duration.between(//
				this.lastStateChangeTime, //
				Instant.now(this.componentManager.getClock()) //
		).toSeconds() >= HYSTERESIS) {
			this.state = nextState;
			this.lastStateChangeTime = Instant.now(this.componentManager.getClock());
			this._setAwaitingHysteresisValue(false);
			return true;
		} else {
			this._setAwaitingHysteresisValue(true);
			return false;
		}
	}

	private void calculateChargedEnergy() {

		Long currentEssActiveChargeEnergy = 0L;

		currentEssActiveChargeEnergy = this.getEssChargedEnergy().get(); // Cumulative counter of ESS device. No matter if DC or AC coupled

		// Ess Active Charge Energy directly from ESS (cumulative)
		Integer storedChargedEnergy = this.getChargedEnergy().get(); // Stored charged energy from this controller's channel

		// Early exit if any data is not available
		if (currentEssActiveChargeEnergy == null || storedChargedEnergy == null) {
			this.initializeChargedEnergyFromTimedata();
			return;
		}

		// If it's the first time or if the lastEssActiveChargeEnergy is null,
		// initialize it
		if (this.lastEssActiveChargeEnergy == null) {
			this.lastEssActiveChargeEnergy = currentEssActiveChargeEnergy;
			return;
		}

		// Calculate energy difference
		int energyDifference = (int) (currentEssActiveChargeEnergy - this.lastEssActiveChargeEnergy);

		// Update charged energy by adding the difference
		this.cumulatedchargedEnergy = storedChargedEnergy + energyDifference;

		// Update the last known cumulative energy
		this.lastEssActiveChargeEnergy = currentEssActiveChargeEnergy;

		// If reset is flagged (e.g., calibration completed), reset the charged energy
		if (this.resetChargedEnergy) {
			this.cumulatedchargedEnergy = 0;
			this.resetChargedEnergy = false;
		}
		this.logDebug(this.log, "Writing charged energy " + this.cumulatedchargedEnergy + "[Wh]");
		// Set the updated charged energy in the controller's channel
		this._setChargedEnergy(this.cumulatedchargedEnergy);

	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.debugMode) {
			this.logInfo(this.log, message);
		}
	}

	/**
	 * Calculation of useable Soc and Capacity.
	 * Channels are stored in this controller and not in the device as we cannot be sure that 
	 * they exist.
	 * 
	 * @param soc the current SoC
	 */
	private void updateUsableSocAndCapacity(Integer soc) {
		if (this.ess == null) {
			this.logDebug(this.log, "Cannot calculate usable SoC: ESS is null");
			return;
		}

		if (soc == null) {
			this.logDebug(this.log, "Cannot calculate usable SoC: SoC is null");
			return;
		}

		Integer totalCapacityWh = this.ess.getCapacity().get();

		if (totalCapacityWh == null) {
			this.logDebug(this.log, "Cannot calculate usable SoC: totalCapacityWh is null");
			return;
		}

		// Min/Max SoC-borders from config
		int minSocPercentage = this.minSoc;
		int maxSocPercentage = this.maxSoc;

		if (maxSocPercentage <= minSocPercentage) {
			this.logDebug(this.log, "Invalid SoC window: minSoc=" + minSocPercentage + ", maxSoc=" + maxSocPercentage);
			return;
		}

		// normalize
		int useableSoc = soc > maxSocPercentage ? 100
				: soc < minSocPercentage ? 0
						: (int) (((double) (soc - minSocPercentage) / (maxSocPercentage - minSocPercentage)) * 100);

		this.logDebug(this.log, "Normalized usable SoC: " + useableSoc + "% based on current SoC: " + soc);

		double usableCapacityRange = (maxSocPercentage - minSocPercentage) / 100.0;
		int totalUsableCapacityWh = (int) (totalCapacityWh * usableCapacityRange);

		int useableCapacityWh = (int) (totalUsableCapacityWh * (useableSoc / 100.0));

		// Clamp 0..totalUsableCapacityWh
		useableCapacityWh = Math.max(Math.min(useableCapacityWh, totalUsableCapacityWh), 0);

		// feed channels
		this._setUseableSoc(useableSoc);
		this._setUseableCapacity(useableCapacityWh);

		this.logDebug(this.log, "Capacity info: totalCapacity=" + totalCapacityWh + "Wh, usableRange="
				+ totalUsableCapacityWh + "Wh, currentUsableCapacity=" + useableCapacityWh + "Wh");
	}

	private boolean isPeakshavingActive() {
		// Check all connected peakshaving controllers to determine if any are active
		if (this.ctrlEssThresholdPeakshavers == null || this.ctrlEssThresholdPeakshavers.isEmpty()) {
			this.logDebug(this.log, "No peakshaving controllers connected.");
			return false;
		}

		for (ControllerEssThresholdPeakshaver peakshaver : this.ctrlEssThresholdPeakshavers) {
			if (peakshaver == null) {
				this.logDebug(this.log, "ERROR: A peakshaver instance is null.");
				continue; // Skip null entries without breaking the loop
			}

			String state = peakshaver.getStateMachine() != null ? peakshaver.getStateMachine().toString() : "null";
			this.logDebug(this.log, "Peakshaver state: " + state);

			if ("PEAKSHAVING_ACTIVE".equals(state)) { // Use .equals for string comparison
				this.logDebug(this.log, "Peakshaving Controller " + peakshaver.alias() + " is active.");
				return true;
			}
		}

		this.logDebug(this.log, "No active peakshaving controllers detected.");
		return false;
	}

	@Override
	public String getEssId() {
		if (this.ess == null) {
			return null;
		} else {
			return this.ess.id();
		}

	}

	private Value<Long> getEssChargedEnergy() {
		if (this.ess instanceof HybridEss hss) {
			return hss.getDcChargeEnergy();
		} else {
			return this.ess.getActiveChargeEnergy();
		}
	}

	private Value<Integer> getEssChargePower() {
		if (this.ess instanceof HybridEss hss) {
			return hss.getDcDischargePower(); // DC Power for hybrid systems. negative values for Charge; positive for Discharge
		} else {
			return this.ess.getActivePower();
		}
	}

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}

	@Override
	public ModbusSlaveTable getModbusSlaveTable(AccessMode accessMode) {
		return new ModbusSlaveTable(//
				OpenemsComponent.getModbusSlaveNatureTable(accessMode), //
				ModbusSlaveNatureTable.of(ControllerEssChargeDischargeLimiter.class, accessMode, 100) //
						.build());
	}

}
