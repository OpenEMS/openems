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
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;

import io.openems.edge.common.type.TypeUtils;

import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.controller.symmetric.thresholdpeakshaver.ControllerEssThresholdPeakshaver;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.ChargeDischargeLimiter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssChargeDischargeLimiterImpl extends AbstractOpenemsComponent
		implements ControllerEssChargeDischargeLimiter, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerEssChargeDischargeLimiterImpl.class);

	private Config config;

	/**
	 * Length of hysteresis in minutes. States are not changed quicker than this.
	 * 
	 */
	private static final int HYSTERESIS = 3; // seconds
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

	private int taperStartSoc = 0;
	private int taperPercent = 3; // decrease charge power during the last X percent before hitting the max. Soc
	private int fullChargePower = 0;

	@Reference
	private ComponentManager componentManager;

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

		// this.ess = this.componentManager.getComponent(config.ess_id());
		this.minSoc = this.config.minSoc(); // min SoC
		this.maxSoc = this.config.maxSoc();
		this.forceChargeSoc = this.config.forceChargeSoc(); // if battery need balancing we charge to this value
		this.forceChargePower = this.config.forceChargePower(); // if battery need balancing we charge to this value
		this.energyBetweenBalancingCycles = this.config.energyBetweenBalancingCycles() * 1000; // convert kWh to Wh
		this.balancingHysteresisTime = this.config.balancingHysteresis();
		this.debugMode = this.config.debugMode();

		this.taperStartSoc = this.config.maxSoc() - taperPercent;

		this.log.info("Number of Peakshaving controllers found: ");

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}


	    initializeEssProperties();

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
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
	
	
	private void initializeEssProperties() {
	    // Überprüfen, ob ESS bereit und initialisiert ist
	    if (this.ess != null && this.ess.getAllowedChargePower().isDefined()) {
	        
	        this.slowChargePower = this.ess.getAllowedChargePower().get() / 20;
	        this.slowDisChargePower = this.ess.getAllowedDischargePower().get() / 20;
	        this.fullChargePower = this.ess.getAllowedChargePower().get();

	        this.logDebug(this.log, "Initialized slow charge and discharge power.");
	    } else {
	        this.logDebug(this.log, "Waiting for ESS initialization to set slow charge and discharge power.");
	    }
	}	

	@Override
	public void run() throws OpenemsNamedException {


	    if (this.ess == null || !this.ess.getAllowedChargePower().isDefined()) {
	        initializeEssProperties();
	        return;
	    }		

		Integer currentSoc = ess.getSoc().get();
		Integer currentActivePower = this.getEssChargePower().get(); // no matter if AC or DC charging
		Integer calculatedPower = 0; // No constraints

		// Remember: Negative values for Charge; positive for Discharge
		this.logDebug(this.log, "Number of Peakshaving controllers found: " + this.ctrlEssThresholdPeakshavers.size());

		switch (this.state) {
		case UNDEFINED:
			if (this.ess == null) {
				this.logDebug(this.log, "ERROR. ESS " + config.ess_id() + " unavailable ");
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
			if (shouldBalance()) {
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
			if (currentSoc >= taperStartSoc && currentSoc < maxSoc && currentActivePower < 0) {
				// Calculate tapering factor to gradually reduce power as SOC approaches max
				double taperFactor = Math.pow((double) (maxSoc - currentSoc) / (maxSoc - taperStartSoc), 2); // quadratic
																												// taper
				calculatedPower = (int) (fullChargePower * taperFactor);
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
			calculatedPower = this.slowDisChargePower != null ? this.slowDisChargePower : calculatedPower;

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
			if (!shouldBalance()) {
				this.changeState(State.NORMAL);
			} else if (currentSoc >= this.forceChargeSoc) {
				this.changeState(State.BALANCING_ACTIVE);
			} else {
				calculatedPower = this.forceChargePower * -1;
			}
			break;
		case BALANCING_WANTED:
			// State can be used to check things. Don´t know what, yet ;o)
			this.changeState(State.FORCE_CHARGE_ACTIVE);
			break;
		case BALANCING_ACTIVE:

			if (currentSoc == null) {
				this.logDebug(this.log, "SOC not available.");
				break;
			}
			// Start Timer
			if (balancingStartTime.equals(Instant.MIN)) {
				// Start the balancing timer
				balancingStartTime = Instant.now(this.componentManager.getClock());
				this.log.info("Balancing started at " + balancingStartTime);
				// ToDo: channel for balancing remaining time needed for UI modal
			}

			// Check if balancing duration has passed
			if (isBalancingDurationExceeded() || !this.shouldBalance()) {
				// Balancing time is over, transition to NORMAL state
				this.changeState(State.NORMAL);
				this.resetBalancingTimers();

			} else {
				this.balancingRemainingTime = (int) (this.balancingHysteresisTime - this.balancingTime);
				this.logDebug(this.log, "Balancing active since " + this.balancingStartTime + "|Remaining: "
						+ this.balancingRemainingTime + "s \n");

				//
				this._setBalancingRemainingSeconds(this.balancingRemainingTime);

				if (currentSoc < this.forceChargeSoc) {
					// SOC dropped below forceChargeSoc: reset balancing
					this.logDebug(this.log, "SOC dropped below " + this.forceChargeSoc + "%. Restarting balancing.");
					this.changeState(State.BALANCING_WANTED);
					this.resetBalancingTimers();
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
		this.applyActivePower(calculatedPower);
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

	private void resetBalancingTimers() {
		this.logDebug(this.log, "\nBalancing finished. Going back to normal operation. Resetting counters ");
		this.balancingStartTime = Instant.MIN; // Reset balancing start time
		this.balancingRemainingTime = 0; // Reset remaining balancing time
		this.resetChargedEnergy = true; // Flag to reset charged energy
		this.cumulatedchargedEnergy = 0; // Optional: reset cumulative charge energy here

	}

	private boolean isBalancingDurationExceeded() {
		// calculate balancing time so far to seconds
		this.balancingTime = Duration.between(balancingStartTime, Instant.now(this.componentManager.getClock()))
				.getSeconds();

		return this.balancingTime >= this.balancingHysteresisTime;
	}

	/**
	 * Calculates if the battery needs too be balanced. This depends on charged
	 * energy since the last balancing procedure. If charged energy exceeds
	 * configured energy method returns true.
	 * 
	 * Balancing is blocked if a Peakshaver is active. "Active" means that it is in
	 * operating state - not neccessaryly active discharging the battery
	 * 
	 * @return if battery should be balanced
	 */
	private boolean shouldBalance() {
		// this.logDebug(this.log, "\nCharged " + this.getActiveChargeEnergy().get() + "
		// since last balancing cycle");
		if (this.getChargedEnergy().get() == null) {
			return false;
		}

		// are there any peakshavers active
		if (this.isPeakshavingActive() == true) {
			this.logDebug(this.log, "Balancing is deactivated due to active peakshaving");
			return false;
		}

		// balancing is not desired
		if (config.energyBetweenBalancingCycles() == 0) {
			return false;
		}

		if (this.state == State.BALANCING_ACTIVE) {
			return false;
		}

		if (this.getChargedEnergy().get() > this.energyBetweenBalancingCycles) {
			return true;
		}
		return false;
	}

	/**
	 * Applies constraints to ess. Even FORCE_CHARGE is a constraint as the ESS is
	 * allowed to charge more than this controller´s desired value.
	 * 
	 * 
	 * @param calculatedPower
	 */
	void applyActivePower(Integer calculatedPower) {
		if (calculatedPower == null) {
			return; // No constraints to set
		}

		if (this.ess == null) {
			this.logDebug(this.log, "ERROR. No Ess to apply constraints to");
			return;
		}

		try {
			switch (this.state) {
			case MAX_SOC_REACHED, ABOVE_MAX_SOC -> // Block further charging
				ess.setActivePowerGreaterOrEquals(calculatedPower);

			case MIN_SOC_REACHED, BELOW_MIN_SOC -> // Block further discharging
				ess.setActivePowerLessOrEquals(calculatedPower);

			case FORCE_CHARGE_ACTIVE, BALANCING_ACTIVE -> {
				// Fit calculated power within min/max limits and apply
				calculatedPower = ess.getPower().fitValueIntoMinMaxPower(this.id(), ess, Phase.ALL, Pwr.ACTIVE,
						calculatedPower);
				ess.setActivePowerLessOrEquals(calculatedPower);
			}
			case NORMAL -> {
				if (calculatedPower < 0) { // ramp down charge power
					ess.setActivePowerGreaterOrEquals(calculatedPower);
				}
			}
			case BALANCING_WANTED -> //do nothing
			{}
			case UNDEFINED -> //do nothing
			{}

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

	/**
	 * Positive values (discharging) are ignored.
	 *
	 * @param nextState the target state
	 * @return whether the state was changed
	 */
	private void calculateChargedEnergy() {

		Long currentEssActiveChargeEnergy = 0L;

		currentEssActiveChargeEnergy = this.getEssChargedEnergy().get(); // Cumulative counter of ESS device. No matter if DC
																			// or AC coupled

		// Ess Active Charge Energy directly from ESS (cumulative)
		Integer storedChargedEnergy = this.getChargedEnergy().get(); // Stored charged energy from this controller's
																		// channel

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
		this.logDebug(this.log, "Writing charged energy");
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

	private boolean isPeakshavingActive() {
		// ToDo: Checkk other peakshaving controller
		// check all ess' connected peakshavers if any of them is active
		for (ControllerEssThresholdPeakshaver peakshaver : this.ctrlEssThresholdPeakshavers) {

			if (peakshaver.getStateMachine().toString() == "PEAKSHAVING_ACTIVE") {
				this.logDebug(this.log, "Peakshaving Controller " + peakshaver.alias() + " is active");
				return true;
			}
		}

		return false;
	}

	private Value<Long> getEssChargedEnergy() {
		if (this.ess instanceof HybridEss hss) {
			return hss.getDcChargeEnergy(); // DC Power for hybrid systems. negative values for Charge; positive for
											// Discharge
		} else {
			return this.ess.getActiveChargeEnergy();
		}
	}

	private Value<Integer> getEssChargePower() {
		if (this.ess instanceof HybridEss hss) {
			return hss.getDcDischargePower(); // DC Power for hybrid systems. negative values for Charge; positive for
												// Discharge
		} else {
			return this.ess.getActivePower(); //
		}
	}

}
