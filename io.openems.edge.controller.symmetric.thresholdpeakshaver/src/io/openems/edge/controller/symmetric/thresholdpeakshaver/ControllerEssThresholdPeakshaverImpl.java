package io.openems.edge.controller.symmetric.thresholdpeakshaver;

import java.time.Duration;
import java.time.Instant;

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
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.HybridEss;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;

import io.openems.edge.common.sum.GridMode;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.ThresholdPeakshaving", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssThresholdPeakshaverImpl extends AbstractOpenemsComponent
		implements ControllerEssThresholdPeakshaver, Controller, OpenemsComponent, TimedataProvider {

	public static final double DEFAULT_MAX_ADJUSTMENT_RATE = 0.2; // what´s the use? copied from peakshaving controller

	private final Logger log = LoggerFactory.getLogger(ControllerEssThresholdPeakshaverImpl.class);

	@Reference
	private ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ManagedSymmetricEss ess;

	@Reference
	private ElectricityMeter meter;

	private Config config;

	private int essCheckPowerHyteresesPercent = 20; // check if real ess power differs from target peakshaving power +
													// hysteresis

	private State state = State.UNDEFINED;
	private PeakshavingState peakshavingState = PeakshavingState.UNDEFINED;
	private static final int HYSTERESIS_TIME = 5; // seconds
	private Instant lastStateChangeTime = Instant.MIN;

	private Instant peakshavingStartTime = Instant.MIN;

	private Integer maxEssPower = 0;
	private AverageCalculator gridPowerAverageCalculator = new AverageCalculator(3);

	private final CalculateEnergyFromPower calculatePeakShavingEnergy = new CalculateEnergyFromPower(this,
			ControllerEssThresholdPeakshaver.ChannelId.PEAK_SHAVING_ENERGY);
	
	private final CalculateEnergyFromPower calculateRechargedEnergy = new CalculateEnergyFromPower(this,
			ControllerEssThresholdPeakshaver.ChannelId.RECHARGED_ENERGY);	

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	private volatile Timedata timedata = null;

	public ControllerEssThresholdPeakshaverImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssThresholdPeakshaver.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.config = config;

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}

		// Log warnings or handle errors if components are not available
		if (this.ess == null) {
			this.logError(this.log, "ESS component is not available.");
		} 
		
		if (this.meter == null) {
			this.logError(this.log, "Meter component is not available.");
		}

		

	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {

		// Calculate 'real' grid-power (without current ESS charge/discharge)
		var gridPower = meter.getActivePower().getOrError() /* current buy-from/sell-to grid */
				+ ess.getActivePower().getOrError() /* current charge/discharge Ess */;

		/*
		 * A 3 point average is used to start controller´s timer.
		 */
		gridPowerAverageCalculator.addValue(meter.getActivePower().getOrError());

		// Save grid power without peakshaving
		this._setGridPowerWithoutPeakShaving(gridPower);

		int calculatedPower = 0;

		// Integer essRealPower = this.ess.getActivePower().get();
		Integer essRealPower = this.getEssChargePower().get();
		Integer essSoc = this.ess.getSoc().get();

		switch (this.state) {
		case UNDEFINED:
			if (this.checkEnvironment() == false) {
				this.changeState(State.ERROR);
				break;
			}

			this.changeState(State.STANDBY);

			break;
		case ERROR:
			if (this.checkEnvironment() == true) {
				this.changeState(State.STANDBY);
				break;
			}
			break;
		case STANDBY:
			if (this.checkEnvironment() == false) {
				this.changeState(State.ERROR);
				this.changePeakshavingState(PeakshavingState.ERROR);
				break;
			}
			// Stub: check something
			if (gridPower >= this.config.peakShavingThresholdPower()) { // Activate Peakshaving
				// Peakshaving starts above threshold
				// Remember: In peak shaving mode the battery can be charged
				this.changeState(State.PEAKSHAVING_ACTIVE);
			}
			this.changePeakshavingState(PeakshavingState.DISABLED);
			break;
		case PEAKSHAVING_ACTIVE:
			if (this.checkEnvironment() == false) {
				this.changeState(State.ERROR);
				break;
			}

			// If grid power average is above threshold update timer
			if (gridPowerAverageCalculator.getAverage() > this.config.peakShavingThresholdPower()) {
				this.peakshavingStartTime = Instant.now(this.componentManager.getClock()); // Start timer
			}

			if (gridPower >= this.config.peakShavingPower()) {
				/*
				 * Peak-Shaving
				 */
				calculatedPower = gridPower -= this.config.peakShavingPower();
				// if peakshaving is active, save "shaved" power

				this.logDebug(this.log, "Peakshaver: Battery Discharging");

				if (essRealPower == 0 && calculatedPower > 0) { // assume ESS is 'empty'
					this.changePeakshavingState(PeakshavingState.DISCHARGING_FAILS);
					this._setPeakShavingPower(0); //
				}

				if (calculatedPower > this.maxEssPower) {
					this.changePeakshavingState(PeakshavingState.PEAKSHAVING_POWER_TOO_LOW); // target power is above
																								// max. ESS power
				} else {
					if (calculatedPower > essRealPower * (1 + this.essCheckPowerHyteresesPercent / 100)) {
						/*
						 * target Power is more than 10% above current ess Power
						 */
						this.changePeakshavingState(PeakshavingState.PEAKSHAVING_TARGET_NOT_REACHED);
					} else {
						this.changePeakshavingState(PeakshavingState.ACTIVE); // 'normal' peak shaving state
					}

				}
				if (essRealPower > 0) {
					this.calculatePeakShavingEnergy.update(essRealPower); // Energy used for peakshaving	
				} else {
					this.calculatePeakShavingEnergy.update(0); // Energy used for peakshaving	
				}
				
				this.calculateRechargedEnergy.update(0); // Energy used for re-charging				

			} else if (gridPower <= this.config.rechargePower()) {
				/*
				 * Recharge
				 */
				calculatedPower = gridPower -= this.config.rechargePower();
				this._setPeakShavingPower(0); // feed channel

				if (calculatedPower < 0 && essRealPower == 0) {
					this.changePeakshavingState(PeakshavingState.CHARGING_FINISHED); // Assume ESS is fully charged
				} else {
					this.logDebug(this.log, "Peakshaver: Battery Charging");
					this.changePeakshavingState(PeakshavingState.CHARGING);
				}
				this.calculatePeakShavingEnergy.update(0); // Energy used for peakshaing. 0 while re-charging
				if (essRealPower < 0) {  // only charge power
					this.logDebug(this.log, "ReChargeEnergyCalculator: " + (essRealPower * -1) + "[W] / " + this.getRechargedEnergy() + "[Wh]");
					this.calculateRechargedEnergy.update(essRealPower *-1); // Energy used for re-charging	
				} else {
					this.calculateRechargedEnergy.update(0);
				}
				

			} else {
				/*
				 * Do nothing
				 */
				calculatedPower = 0; // only 0 if gridPower is above limit or below recharge limit
				this.changePeakshavingState(PeakshavingState.IDLE);
				this.calculatePeakShavingEnergy.update(null); // Energy used for peakshaing. NULL while not active
				this.calculateRechargedEnergy.update(null); // Energy used for re-charging				

			}

			this.applyPower(ess, calculatedPower);

			// Only leave if grid power is below threshold an hysteresis has passed
			if (this.peakShavingHysteresisActive() == false) {
				// Peakshaving starts above threshold
				// Remember: In peak shaving mode the battery can be charged
				this.changeState(State.STANDBY);

			}

			// feed channels
			if (calculatedPower > 0) {

				this._setPeakShavingTargetPower(calculatedPower); // feed the channel
				this._setPeakShavingPower(Math.min(calculatedPower, essRealPower)); //

			} else {
				this._setPeakShavingTargetPower(0); // feed the channel
				this._setPeakShavingPower(0); //
			}

			break;
		default:
			// ToDo
			break;

		}
		// save current state / channels
		if (this.state != State.PEAKSHAVING_ACTIVE) {
			this._setPeakShavingTargetPower(0); // feed the channel
			this._setPeakShavingPower(0); //
		}
		
		this._setEssPower(essRealPower);
		this._setEssSoc(essSoc);

		this.logDebug(this.log, "\n PeakShaver Current State " + this.state.getName() + "\n PeakShaving State "
				+ this.peakshavingState.getName() + "\n max ESS power " + this.maxEssPower + "VA" + "\n Current SoC "
				+ this.ess.getSoc().get() + "%" + "\n Current ESS ActivePower " + essRealPower + "W"
				+ "\n Grid power (without ESS) " + this.getGridPowerWithoutPeakShaving() + "\n Grid powerAverage "
				+ this.gridPowerAverageCalculator.getAverage() + "W" + "\n Balancing Target power "
				+ this.getPeakShavingTargetPower() + "W" + "\n Shaved power " + this.getPeakShavedPower() + "W"

		);

	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	/*
	 * checks - Grid Mode - meter not null - ess not null
	 */
	private boolean checkEnvironment() {
		if (ess.getGridMode() != GridMode.ON_GRID) {
			return false;
		}
		if (this.meter == null || this.ess == null) {
			return false;
		}
		
		this.maxEssPower = this.ess.getMaxApparentPower().get();		

		return true;
	}

	private boolean peakShavingHysteresisActive() {
		long peakShavingDuration = Duration
				.between(this.peakshavingStartTime, Instant.now(this.componentManager.getClock())).getSeconds();
		if (peakShavingDuration > this.config.hysteresisTime()) {
			return false;
		} else {
			this.logDebug(this.log, "Hysteresis is active. Seconds passed: " + peakShavingDuration + "s");
			return true;
		}

	}

	/**
	 * Applies the power on the ESS.
	 *
	 * @param ess         {@link ManagedSymmetricEss} where the power needs to be
	 *                    set
	 * @param activePower the active power
	 * @throws OpenemsNamedException on error
	 */
	private void applyPower(ManagedSymmetricEss ess, Integer activePower) throws OpenemsNamedException {
		if (activePower != null) {
			this.logDebug(this.log, "PeakShaver applyPower: " + activePower + "[W]");
			if (activePower == 0) {
				this.logDebug(this.log, "PeakShaver applyPower with setActivePowerEquals: " + activePower + "[W]");
				ess.setActivePowerEquals(activePower);
			} else {
				this.logDebug(this.log,
						"PeakShaver applyPower with setActivePowerEqualsWithPid: " + activePower + "[W]");
				ess.setActivePowerEqualsWithPid(activePower);
			}

			ess.setReactivePowerEquals(0);
			this._setCalculatedPower(activePower); // save value to channel

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

			return false;
		}
		if (Duration.between(//
				this.lastStateChangeTime, //
				Instant.now(this.componentManager.getClock()) //
		).toSeconds() >= HYSTERESIS_TIME) {
			this.state = nextState;
			this.lastStateChangeTime = Instant.now(this.componentManager.getClock());
			this._setStateMachine(this.state); // save to channel
			return true;
		} else {

			return false;
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

	/**
	 * Changes the state if hysteresis time passed, to avoid too quick changes.
	 *
	 * @param nextState the target state
	 * @return whether the state was changed
	 */
	private boolean changePeakshavingState(PeakshavingState nextState) {
		if (this.peakshavingState == nextState) {

			return false;
		}

		// Placeholder for hysteresis - necessary?
		this.peakshavingState = nextState;
		this._setPeakShavingStateMachine(nextState); // save to channel

		return true;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
