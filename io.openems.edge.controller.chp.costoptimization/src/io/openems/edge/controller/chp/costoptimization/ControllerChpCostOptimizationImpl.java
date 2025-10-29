package io.openems.edge.controller.chp.costoptimization;

import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
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
import io.openems.common.exceptions.OpenemsException;
import io.openems.common.types.MeterType;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.timeofusetariff.api.TimeOfUseTariff;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import io.openems.edge.generator.api.ManagedSymmetricGenerator;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.CHP.CostOptimization", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS, //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS //
})
public class ControllerChpCostOptimizationImpl extends AbstractOpenemsComponent
		implements ControllerChpCostOptimization, Controller, OpenemsComponent, EventHandler {

	private Config config = null;
	private final Logger log = LoggerFactory.getLogger(ControllerChpCostOptimizationImpl.class);
	private static final int STATE_TRANSITION_HYSTERESIS = 10; // seconds ToDo: increase this value
	private static final int TARGET_NOT_REACHED_GRACE_SECONDS = 300; // check if CHPs produce target power timeout
	private static final int IDLE_GRACE_SECONDS = 300; // check if CHPs produce target power timeout
	private static final int REDUCED_POWER_GRACE_SECONDS = 3600; // check if CHPs produce target power timeout

	private Instant lastTransitionChangeTime = Instant.MIN;
	private Instant lastRunHysteresisTime = Instant.MIN; // CHPs are allowed to stop after that hysteresis
	private Instant lastStartHysteresisTime = Instant.MIN;
	private Instant lastPreparationHysteresisTime = Instant.MIN;
	private Instant targetNotReachedTime = Instant.MIN;
	private Instant lastIdleHysteresisTime = Instant.MIN;
	private Instant lastReducedPowerHysteresisTime = Instant.MIN;
	private State state = State.UNDEFINED;
	private Integer gridPowerWithoutChp = 0;
	private Integer applyPowerTarget = 0;
	private Double currentPrice = 0.0;
	private Double futurePrice = 0.0;

	private boolean targetNotReachedStartFlag = false;
	private boolean idleStartFlag = false;
	private boolean wasTemperatureNearMax = false;

	private boolean temperatureAboveThreshold = false;
	private boolean temperatureBelowMin = false;
	private boolean temperatureAboveMax = false;
	private boolean temperatureNearMax = false;
	

	private boolean operationalValuesOk = false;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private TimeOfUseTariff timeOfUseTariff;

	@Reference(name = "gridMeter", service = ElectricityMeter.class, policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL, bind = "bindGridMeter", unbind = "unbindGridMeter")
	private volatile ElectricityMeter gridMeter;

	@Reference
	private ManagedSymmetricGenerator chp;

	public ControllerChpCostOptimizationImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				ElectricityMeter.ChannelId.values(), Controller.ChannelId.values(), //
				ManagedSymmetricGenerator.ChannelId.values(), ControllerChpCostOptimization.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsException {
		this.config = config;
		super.activate(context, config.id(), config.alias(), config.enabled());

		boolean updated = false;
		updated |= OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "chp", config.chp_id());
		updated |= OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "gridMeter", config.meter_id());
		if (updated)
			return; // DS reconfig kommt gleich nochmal

		var props = context.getProperties();
		log.info("[CHP CostOpt] gridMeter.target={}", props.get("gridMeter.target"));
		log.info("[CHP CostOpt] chp.target={}", props.get("chp.target"));
	}

	@Override
	public void handleEvent(Event event) {

		// super.handleEvent(event);

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
			// nothing to do
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			// nothing to do
			break;

		}
	}

	@Override
	public void run() throws OpenemsNamedException {
		this.updateOperationalValues();
		if (this.operationalValuesOk == false) {
			this.logWarn(this.log, "Controller not ready");
			this.changeState(State.ERROR);
			return;
		}
		Integer gridActivePower = this.gridMeter.getActivePower().get();
		Integer chpActivePower = this.chp.getGeneratorActivePower().get();

		this.gridPowerWithoutChp = gridActivePower + chpActivePower;
		Double currentEnergyCostsWithoutChp = this.getCurrentCost(gridPowerWithoutChp);
		Double currentEnergyCosts = this.getCurrentCost(gridActivePower);
		
		this.currentPrice = this.getCurrentPrice();
		this.futurePrice = this.getFuturePrice();

		Double futureEnergyCostsWithoutChp = this.getFutureCost(gridPowerWithoutChp);
		// Double futureEnergyCosts = this.getFutureCost(gridActivePower);

		this._setEnergyCostsWithoutChp(currentEnergyCostsWithoutChp); // save value in channel
		this._setEnergyCosts(currentEnergyCosts); // save value in channel
		this._setChpActivePower(chpActivePower); // save value in channel
		this._setAwaitingDeviceHysteresis(Boolean.TRUE.equals(this.chp.getAwaitingHysteresis().get()));
		this._setCurrentEnergyPrice(this.currentPrice);
		this.checkTemperatureLimits();

		// check hysteresis timer
		if (isHysteresisActive(this.lastStartHysteresisTime, this.config.startHyteresis())) {
			this._setAwaitingStartHysteresis(true);
			this.logDebug(this.log, "Start hysteresis (before next start is allowed) "
					+ getRemainingHysteresisTime(this.lastStartHysteresisTime, this.config.startHyteresis()));
		} else {
			this._setAwaitingStartHysteresis(false);
		}

		if (isHysteresisActive(this.lastRunHysteresisTime, this.config.runHyteresis())) {
			this._setAwaitingRunHysteresis(true);
			this.logDebug(this.log, "Run hysteresis (before stop is allowed) "
					+ getRemainingHysteresisTime(this.lastRunHysteresisTime, this.config.runHyteresis()));
		} else {
			this._setAwaitingRunHysteresis(false);
		}

		if (isHysteresisActive(this.lastPreparationHysteresisTime, this.config.preparationHyteresis())) {
			this._setAwaitingPreparationHysteresis(true);
			this.logDebug(this.log, "Preparation " + getRemainingHysteresisTime(this.lastPreparationHysteresisTime,
					this.config.preparationHyteresis()));

		} else {
			this._setAwaitingPreparationHysteresis(false);
		}

		if (isHysteresisActive(this.lastReducedPowerHysteresisTime, REDUCED_POWER_GRACE_SECONDS)) {
			this._setAwaitingReducedPowerHysteresis(true);
			this.logDebug(this.log, "Reduced Power " + getRemainingHysteresisTime(this.lastReducedPowerHysteresisTime,
					REDUCED_POWER_GRACE_SECONDS));

		} else {
			this._setAwaitingReducedPowerHysteresis(false);
		}
		
		// just for logging
		if (isHysteresisActive(this.lastTransitionChangeTime, STATE_TRANSITION_HYSTERESIS)) {
			this.logDebug(log, "TransitionHysteresis "
					+ getRemainingHysteresisTime(this.lastTransitionChangeTime, STATE_TRANSITION_HYSTERESIS));
			this._setAwaitingTransitionHysteresis(true);

		} else {
			this._setAwaitingTransitionHysteresis(false);
		}

		logDebug(this.log, "Calculated power " + this.applyPowerTarget + "W");

		switch (this.state) {
		case ERROR:
			if (this.operationalValuesOk == true) {
				this.changeState(State.NORMAL);
				this.logDebug(this.log, "Controller ready. Transition to state: NORMAL\n");
				return;
			}
			this.logDebug(this.log, "ERROR in Controller\n");
			this.chp.applyPreparation(false);
			this.setChpOff();
			break;
		case UNDEFINED:
			if (!this.chpReadyForOperation()) {
				this.changeState(State.CHP_NOT_READY);
				return;
			}
			if (this.operationalValuesOk == true) {
				this.logDebug(this.log, "Controller ready. Transition to state: NORMAL\n");
				this.changeState(State.NORMAL);
				return;
			}
			this.chp.applyPreparation(false);
			this.setChpOff();
			break;
		case IDLE:
			if (this.operationalValuesOk == false) {
				this.changeState(State.ERROR);
				break;
			}

			// activate controller if grid consumption is above configured value
			if (this.gridPowerWithoutChp < this.config.minGridPower()) {
				this.lastIdleHysteresisTime = Instant.now(this.componentManager.getClock()); // start timer
				logDebug(this.log, "Grid Consumption " + this.gridPowerWithoutChp + "W below min "
						+ this.config.minGridPower() + "W. Reset Timer Waiting in State IDLE");

				this.chp.applyPreparation(false);
				this.setChpOff();
				break;
			}

			if (!isHysteresisActive(this.lastIdleHysteresisTime,
					ControllerChpCostOptimizationImpl.IDLE_GRACE_SECONDS)) {
				this.changeState(State.NORMAL);
				logDebug(this.log, "Grid Consumption " + this.gridPowerWithoutChp + "W above min "
						+ this.config.minGridPower() + "W. Timer over Transition to State NORMAL");

			} else {
				logDebug(this.log, "Grid Consumption " + this.gridPowerWithoutChp + "W above min "
						+ this.config.minGridPower() + "W. Timer active Waiting in State IDLE");
				this.chp.applyPreparation(false);
				this.setChpOff();
			}
			break;

		case OVER_TEMPERATURE:
			if (!this.temperatureAboveMax) {
				this.logDebug(this.log, "Temperature low enough. Transition to state: NORMAL\n");
				this.changeState(State.NORMAL);
				break;
			}
			this.chp.applyPreparation(true); // apply Preparation without transition
			this.setChpOff();
			break;
		case CHP_NOT_READY:
			if (this.chpReadyForOperation()) {
				this.logDebug(this.log, "At least one CHP is ready. Transition to state: NORMAL \n");
				this.changeState(State.NORMAL);
				break;
			}
			this.logError(this.log, "No CHP ready. Either hardware fault or all CHPs blocked \n");
			this.chp.applyPreparation(false);
			this.setChpOff();
			break;
		case NORMAL:

			if (this.config.mode() == Mode.MANUAL_ON) {
				if (this.changeState(State.CHP_ACTIVE)) {
					this.targetNotReachedStartFlag = false;
					this.targetNotReachedTime = Instant.now(this.componentManager.getClock());
					this._setTargetNotReached(false);
					// keine Timer/Hysteresen setzen – dein Wunsch
				}
				this.chp.applyPower(this.config.maxActivePower());
				break;
			}

			if (this.config.mode() == Mode.MANUAL_OFF) {

				if (this.changeState(State.CHP_INACTIVE)) {
					this.chp.applyPower(null);
				}
				break;
			}

			if (this.operationalValuesOk == false) {
				this.changeState(State.ERROR);
				break;
			}

			// takes care of hardware locks
			if (!this.chpReadyForOperation()) {
				this.changeState(State.CHP_NOT_READY);
				return;
			}

			// check power
			if (this.gridPowerWithoutChp < this.config.minGridPower()) {
				this.changeState(State.IDLE);
				break;
			}

			// check buffer tank temperature
			if (this.temperatureBelowMin) { // start CHPs
				// start chp
				if (this.changeState(State.CHP_ACTIVE)) {
					this.targetNotReachedStartFlag = false;
					this.targetNotReachedTime = Instant.now(this.componentManager.getClock());
					this._setTargetNotReached(false);
					this.logDebug(this.log,
							"Temperature " + this.chp.getAverageBufferTankTemperature().asString()
									+ " below configured value " + this.config.minBufferTankTemperature()
									+ "°C. Transition to state: CHP_ACTIVE\n");
					// Start Timer
					this.lastRunHysteresisTime = Instant.now(this.componentManager.getClock());
					break;
				}
				break;
			}

			// Overtemperature. Do not start
			// check buffer tank temperature MAX
			if (this.temperatureAboveMax) {
				// stop chp. set timer
				if (this.changeState(State.OVER_TEMPERATURE)) {
					this.logDebug(this.log,
							"Temperature " + this.chp.getAverageBufferTankTemperature().asString()
									+ " above configured value " + this.config.maxBufferTankTemperature()
									+ "°C. Transition to state: OVER_TEMPERATURE\n");
					this.lastStartHysteresisTime = Instant.now(this.componentManager.getClock());
					break;
				}
				break;

			}

			// Costs are high. Start CHP but check hysteresis and temperatures
			if (this.currentPrice > this.config.priceThreshold()) {
				this.logDebug(this.log, "Current price " + this.currentPrice + "€/MWh above configured value "
						+ this.config.priceThreshold());
				if (this.temperatureAboveThreshold) {
					// do NOT start chp
					if (this.changeState(State.CHP_PREPARING)) {

						this.logDebug(this.log, "Temperature " + this.chp.getAverageBufferTankTemperature().asString()
								+ " above configured THRESHOLD value " + this.config.thresholdBufferTankTemperature()
								+ "°C. CHP won´t be started. Transition to state PREPARATION without hysteresis\n");

						break;
					} else {
						// Wechsel blockiert -> eindeutig in NORMAL bleiben
						this.chp.applyPreparation(false);
						this.setChpOff();
					}
					break;
				} else {
					this.chp.applyPreparation(false);
					this.setChpOff();
				}

				if (!isHysteresisActive(this.lastStartHysteresisTime, this.config.startHyteresis())) {
					if (this.changeState(State.CHP_ACTIVE)) {
						this.logDebug(this.log,
								"Current price " + this.currentPrice + "€/MWh above configured value "
										+ this.config.priceThreshold() + "€/MWh Transition to state: CHP_ACTIVE\n");
						// Start Timer: do not STOP for...
						this.lastRunHysteresisTime = Instant.now(this.componentManager.getClock());
						break;
					}
				} else {
					this.logDebug(this.log,
							"Current price " + this.currentPrice + "€/MWh above configured value "
									+ this.config.priceThreshold() + "€/MWh Cannot start due to hysteresis ");
					this.chp.applyPreparation(false);
					this.setChpOff();
				}

			}

			if (this.futurePrice > this.config.priceThreshold()) {
				if (this.changeState(State.CHP_PREPARING)) {
					this.logDebug(this.log,
							" Future price " + this.futurePrice + "€/MWh above configured value "
									+ this.config.priceThreshold() + "€/MWh Transition to state: PREPARATION\n");
					this.lastPreparationHysteresisTime = Instant.now(this.componentManager.getClock());
				} else {
					// Wechsel blockiert -> eindeutig in NORMAL bleiben
					this.chp.applyPreparation(false);
					this.setChpOff();
				}
				break;
			} else {
				this.chp.applyPreparation(false);
				this.setChpOff();
			}
			break;
		case CHP_PREPARING:
			// Future energy price will exceed maxPrice → pre-cool buffer tank
			// Preparation runs for a defined hysteresis duration, then returns to NORMAL.

			if (this.config.mode() == Mode.MANUAL_ON) {
				if (this.changeState(State.CHP_ACTIVE)) {
					this.targetNotReachedStartFlag = false;
					this.targetNotReachedTime = Instant.now(this.componentManager.getClock());
					this._setTargetNotReached(false);
					// keine Timer/Hysteresen setzen – dein Wunsch
				}
				this.chp.applyPower(this.config.maxActivePower());
				break;
			}

			if (this.config.mode() == Mode.MANUAL_OFF) {
				// stop chp
				this.applyPowerTarget = 0;

				if (this.changeState(State.CHP_INACTIVE)) {
					// Start Timer
					this.chp.applyPower(null);
				}
				break;
			}

			this.logDebug(this.log,
					"Current/Future price " + this.currentPrice + "/" + this.futurePrice);
			if ((this.futurePrice * 2) < this.config.priceThreshold()) {
				if (this.changeState(State.NORMAL)) {
					this.chp.applyPreparation(false);
					// Start Timer
					this.logDebug(this.log,
							"Future price below half of configured maximum. Transition to state: NORMAL");
				}
				break;

			}

			if (this.temperatureAboveThreshold) {
				// do NOT start chp
				this.logDebug(this.log,
						"Temperature " + this.chp.getAverageBufferTankTemperature().asString()
								+ " above configured THRESHOLD value " + this.config.thresholdBufferTankTemperature()
								+ "°C. CHP won´t be started. Waiting...\n");
				this.chp.applyPreparation(true);
				break;
			}

			if (this.temperatureBelowMin) { // Temp too low. Go to normal and decide if we can turn on
				if (this.changeState(State.NORMAL)) {
					this.logDebug(this.log,
							"Temperature " + this.chp.getAverageBufferTankTemperature().asString() + " too low. value "
									+ this.config.minBufferTankTemperature()
									+ "°C. Trasition to state NORMAL to make further descisions\n");
					break;
				}
				break;
			}

			if (this.getAwaitingPreparationHysteresis().get()) {
				// prepare something...
				this.chp.applyPreparation(true);

			} else { // hyteresis over -> go back to normal
				if (this.changeState(State.NORMAL)) {
					this.chp.applyPreparation(false); // sofort clearen
				}
			}
			break;

		case CHP_ACTIVE:
			this.applyPowerTarget = this.calculateChpPowerTarget(); // target power calculation

			this.chp.applyPreparation(false);
			this._setAwaitingPreparationHysteresis(false); // Preparation Hysteresis has to be stopped anyway

			// check if power has to be reduced due to a temperature near max
			this.applyPowerTarget = this.applyNearMaxReduction(this.applyPowerTarget);
			this._setActivePowerTarget(applyPowerTarget); // feed channel

			if (this.config.mode() == Mode.MANUAL_ON) {
				this.chp.applyPower(this.config.maxActivePower()); // full power
				break;
			}

			if (this.config.mode() == Mode.MANUAL_OFF) {
				// stop chp
				this.setChpOff();
				this.changeState(State.CHP_INACTIVE);
				break;
			}

			// Automatic mode

			if (this.temperatureAboveMax) {
				if (this.changeState(State.OVER_TEMPERATURE)) {
					this.lastStartHysteresisTime = Instant.now(this.componentManager.getClock());
					break;
				}
				break;
			}
			


			

			// Consumption too low. Stop immediately
			if (this.gridPowerWithoutChp < config.minGridPower()) {
				if (this.changeState(State.IDLE)) {
					this.lastStartHysteresisTime = Instant.now(this.componentManager.getClock());
					this.logDebug(this.log, "Grid consumption too low. Transition to state: IDLE");
					this.chp.applyPreparation(false);
					this.setChpOff();
					break;
				}
				break;
			}

			// Check if even half of target power is active
			if ((this.getChpActivePower().get() * 2) < this.applyPowerTarget) {
				if (!targetNotReachedStartFlag) {
					this.targetNotReachedTime = Instant.now(this.componentManager.getClock()); // start timer
				}
				if (!isHysteresisActive(this.targetNotReachedTime,
						ControllerChpCostOptimizationImpl.TARGET_NOT_REACHED_GRACE_SECONDS)) {
					this._setTargetNotReached(true);
				}
				this.targetNotReachedStartFlag = true;
			} else {
				this.targetNotReachedStartFlag = false;
				this._setTargetNotReached(false);
			}

			if (this.currentPrice > this.config.priceThreshold()) {
				this.chp.applyPower(this.applyPowerTarget);
				this.chp.applyPreparation(false);
				this._setAwaitingPreparationHysteresis(false); // Preparation Hysteresis has to be stopped anyway

			} else { // price are below threshold or not in automatic mode -> can stop chp?
				// check last START time
				if (!this.getAwaitingRunHysteresis().get()) {

					this.logDebug(this.log, "Current price " + this.currentPrice + "€/MWh ≤ "
							+ this.config.priceThreshold() + "€/MWh. Run hysteresis over. ");

					if (this.temperatureAboveThreshold) {
						// stop chp
						if (this.changeState(State.CHP_INACTIVE)) {
							this.logDebug(this.log, "Current price " + this.currentPrice + "€/MWh ≤ "
									+ this.config.priceThreshold()
									+ "€/MWh. Run hysteresis over AND Temperature above threshold → stop CHP → CHP_INACTIVE");
							// Start Timer
							this.lastStartHysteresisTime = Instant.now(this.componentManager.getClock()); // wait until
																											// next
																											// start
						}
					} else {
						this.chp.applyPower(this.applyPowerTarget);
						this.chp.applyPreparation(false);
						this._setAwaitingPreparationHysteresis(false); // Preparation Hysteresis has to be stopped
																		// anyway
						this.logDebug(this.log,
								"RunHysteresis over but temperature is not above threshold. Keep on running ");
					}
				} else {
					this.chp.applyPower(this.applyPowerTarget);
					this.chp.applyPreparation(false);
					this._setAwaitingPreparationHysteresis(false); // Preparation Hysteresis has to be stopped anyway
					this.logDebug(this.log, "Price " + this.currentPrice + "€/MWh below configured value "
							+ this.config.priceThreshold() + "€/MWh. RunHysteresis active. Keep on running ");
				}
			}

			break;
		case CHP_INACTIVE: // stopped, transition after timer is over

			if (this.getAwaitingRunHysteresis().get()) {
				this.logDebug(this.log, " CHP stopped. Wait for Run hysteresis before transitioning");
				this.setChpOff();
				this.chp.applyPreparation(false);
			} else {
				if (this.changeState(State.NORMAL)) {
					this.logDebug(this.log, " CHP stopped. Hystereses over. Transition to state: NORMAL \n");
				}
			}
			break;
		default: // ToDo. Is there a default case?
			break;
		}

	}

	private boolean isHysteresisActive(Instant hysteresisTime, int configuredHysteresis) {
		if (Duration.between(//
				hysteresisTime, //
				Instant.now(this.componentManager.getClock()) //
		).toSeconds() >= configuredHysteresis) {
			return false;
		} else {
			return true;
		}
	}

	private String getRemainingHysteresisTime(Instant lastEventTime, int hysteresisSeconds) {
		Instant now = Instant.now(this.componentManager.getClock());
		long elapsed = Duration.between(lastEventTime, now).getSeconds();
		long remaining = hysteresisSeconds - elapsed;

		if (remaining <= 0) {
			return "0:00";
		}

		return "Time to go: " + remaining + "s";
	}

	/**
	 * 2025 10 27 costs is no longer used. We´re taking a look on prices
	 * 
	 * Calculates the optimal target power for the CHP unit to ensure that energy
	 * costs do not exceed the configured maximum (maxCost). The calculation is
	 * based on the current grid power without CHP and the current energy cost. The
	 * result is limited by the maximum allowed CHP power and by the actual grid
	 * power demand (to avoid unwanted feed-in to the grid).
	 * 
	 * Formula: chpTargetPower = gridPowerWithoutChp * maxCost / currentEnergyCost
	 * 
	 * Limits: - Does not exceed the maximum CHP power (maxActivePower). - Does not
	 * exceed the current grid power demand (prevents export to the grid).
	 * 
	 * Returns the calculated power target in watts and sets it to the corresponding
	 * channel.
	 
	private int calculateChpPowerTarget(Double currentEnergyCost) {

		if (currentEnergyCost <= 0.0) {
			this._setActivePowerTarget(0);
			return 0;
		}
		int chpTargetPower = 0;

		if (this.gridPowerWithoutChp != null) {

			chpTargetPower = (int) Math.round(this.gridPowerWithoutChp * this.config.priceThreshold() / currentEnergyCost);
			logDebug(this.log, "TargetPower Calculation: GridPowerWithoutChp * ConfigMaxCost / CurrentEnergyCosts " + this.gridPowerWithoutChp + "W * " + this.config.priceThreshold() + "€/h / " + currentEnergyCost + "€/h" );

			// Apply limits
			chpTargetPower = Math.min(this.config.maxActivePower(), chpTargetPower);
			chpTargetPower = Math.min(chpTargetPower, this.gridPowerWithoutChp); // sell to grid is not allowed
		}

		this._setActivePowerTarget(chpTargetPower); // feed channel
		return chpTargetPower;
	}
	*/
	
	private int calculateChpPowerTarget() {

		int chpTargetPower = 0;

		if (this.gridPowerWithoutChp != null) {

			// Apply limits
			chpTargetPower = Math.min(this.config.maxActivePower(), this.gridPowerWithoutChp); // also avoids sell to grid 

		}

		
		return chpTargetPower;
	}	

	private Double getCurrentCost(Integer power) {

		Double currentPrice = 0.0;
		if (!this.timeOfUseTariff.getPrices().isEmpty() && power != null && power > 0) {
			currentPrice = this.timeOfUseTariff.getPrices().getFirst(); // Price in €/MWh.
		} else {
			return 0.0;
		}
		// this.logDebug(this.log, " CurrentPrice " + currentPrice + "€/MWh\n");
		return Math.round((currentPrice * power / 1_000_000.0) * 1000.0) / 1000.0;
	}
	
	private Double getCurrentPrice() {

		Double currentPrice = 0.0;
		if (!this.timeOfUseTariff.getPrices().isEmpty()) {
			currentPrice = this.timeOfUseTariff.getPrices().getFirst(); // Price in €/MWh.
		} else {
			return 0.0;
		}
		// this.logDebug(this.log, " CurrentPrice " + currentPrice + "€/MWh\n");
		return currentPrice;
	}	

	private Double getFuturePrice() {
		var from = ZonedDateTime.now(this.componentManager.getClock());
		int qMin = (from.getMinute() / 15) * 15;
		from = from.withMinute(qMin).withSecond(0).withNano(0);

		var to = from.plusSeconds(this.config.preparationHyteresis()); // 3600s = 1h

		if (!this.timeOfUseTariff.getPrices().isEmpty()) {
			// this.futurePrice = this.timeOfUseTariff.getPrices().get

			// Double[] arrFuturePrices = this.timeOfUseTariff.getPrices().asArray();

			var avgEurPerMWh = this.timeOfUseTariff.getPrices().getBetween(from, to).mapToDouble(Double::doubleValue)
					.average().orElse(0.0);

			return avgEurPerMWh;

		} else {
			return 0.0;
		}

	}	
	
	private Double getFutureCost(Integer power) {
		var from = ZonedDateTime.now(this.componentManager.getClock());
		int qMin = (from.getMinute() / 15) * 15;
		from = from.withMinute(qMin).withSecond(0).withNano(0);

		var to = from.plusSeconds(this.config.preparationHyteresis()); // 3600s = 1h

		if (!this.timeOfUseTariff.getPrices().isEmpty() && power != null && power > 0) {
			// this.futurePrice = this.timeOfUseTariff.getPrices().get

			// Double[] arrFuturePrices = this.timeOfUseTariff.getPrices().asArray();

			var avgEurPerMWh = this.timeOfUseTariff.getPrices().getBetween(from, to).mapToDouble(Double::doubleValue)
					.average().orElse(0.0);

			double eurPerHour = Math.round((avgEurPerMWh * (power / 1_000_000.0)) * 1000.0) / 1000.0;
			return eurPerHour;

		} else {
			return 0.0;
		}

	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
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

		if (isHysteresisActive(this.lastTransitionChangeTime, STATE_TRANSITION_HYSTERESIS)) {
			this._setAwaitingTransitionHysteresis(true);
			return false;
		}

		State oldState = this.state;
		this.state = nextState;

		this.lastTransitionChangeTime = Instant.now(this.componentManager.getClock());
		this._setAwaitingTransitionHysteresis(true);
		this._setStateMachine(nextState);
		this.logDebug(this.log, "Change state " + oldState + "->" + nextState);

		return true;

	}

	private void updateOperationalValues() {
		// this.logDebug(this.log,
		// "" + " gridMeter: " + this.gridMeter + " chp: " + this.chp + "
		// gridActivePower: "
		// + (this.gridMeter != null ? this.gridMeter.getActivePower().get() : null)
		// + (this.chp != null ? this.chp.getGeneratorActivePower().get() : null)
		//
		// );

		if (this.chp == null) {
			this.log.warn("Controller not ready. No CHP available");
			this.changeState(State.ERROR);
			this.operationalValuesOk = false;
			return;
		}

		if (this.chp.getGeneratorActivePower().get() == null) {
			this.log.warn("Controller not ready. CHP power not available");
			this.changeState(State.ERROR);
			this.operationalValuesOk = false;
			return;
		}

		if (this.gridMeter == null) {
			this.log.warn("Controller not ready. GridMeter is NULL");
			this.changeState(State.ERROR);
			this.operationalValuesOk = false;
			return;
		}

		if (this.gridMeter.getMeterType() != MeterType.GRID) {
			this.log.warn("Controller not ready. Metertype is not GRID");
			this.changeState(State.ERROR);
			this.operationalValuesOk = false;
			return;
		}

		if (this.gridMeter.getActivePower().get() == null) {
			this.log.warn("Controller not ready. No value for ActivePower from GridMeter");
			this.changeState(State.ERROR);
			this.operationalValuesOk = false;
			return;
		}

		if (this.timeOfUseTariff == null) {
			this.log.warn("Controller not ready. No prices available because TimeOfUse Controller is NULL");
			this.changeState(State.ERROR);
			this.operationalValuesOk = false;
			return;
		}

		if (this.chp.getGeneratorActivePower().get() == null) {
			this.log.warn("Controller not ready. No value for ActivePower from generator(s)");
			this.changeState(State.ERROR);
			this.operationalValuesOk = false;
			return;
		}

		if (this.timeOfUseTariff.getPrices().isEmpty()) {
			this.log.warn("Controller not ready. No prices available");
			this.changeState(State.ERROR);
			this.operationalValuesOk = false;
			return;
		}

		this.operationalValuesOk = true;
		return;

	}

	private boolean chpReadyForOperation() {
		Boolean b = this.chp.getReadyForOperation().get();
		return Boolean.TRUE.equals(b); // null-safe
	}

	void bindGridMeter(ElectricityMeter m) {
		this.gridMeter = m;
		log.info("[CHP CostOpt] gridMeter bound: id={}, type={}", m.id(), m.getMeterType());
	}

	void unbindGridMeter(ElectricityMeter m) {
		if (this.gridMeter == m) {
			this.gridMeter = null;
			log.info("[CHP CostOpt] gridMeter unbound: id={}", m.id());
		}
	}

	/**
	 * Uses Info Log for further debug features.
	 */
	@Override
	protected void logDebug(Logger log, String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, "[CtrlChpCostOptimization]State: " + this.state + " " + message);
		}
	}

	private void setChpOff() {
		this.logDebug(this.log, "Setting flags and target power to OFF \n");

		// Null-sicher arbeiten
		if (this.chp != null) {
			this.chp.applyPower(null);
		}

		// Zielwerte und Flags konsistent zurücksetzen
		this.applyPowerTarget = 0;
		this._setActivePowerTarget(0);
		this.targetNotReachedStartFlag = false;
		this._setTargetNotReached(false);
	}

	private void checkTemperatureLimits() {
		var bufferTemperature = this.chp.getAverageBufferTankTemperature().get(); // Dezi Degrees
		if (bufferTemperature == null) {
			this.logError(this.log, "Buffer Temperature not available");
			return;
		}

		bufferTemperature = (int) Math.round(bufferTemperature / 10.0);

		// check if temperature gets close to max -> reduce power
		if (bufferTemperature >= this.config.reducePowerThresholdTemperature()) {
			this._setTemperatureNearMax(true);
			this.temperatureNearMax = true;
		} else {
			this._setTemperatureNearMax(false);
			this.temperatureNearMax = false;
		}
		
		if (bufferTemperature <= this.config.minBufferTankTemperature()) {
			this._setTemperatureBelowMin(true);
			this._setTemperatureAboveMax(false);
			this._setTemperatureAboveThreshold(false);

			this.temperatureBelowMin = true;
			this.temperatureAboveMax = false;
			this.temperatureAboveThreshold = false;
			return;
		}

		if (bufferTemperature >= this.config.maxBufferTankTemperature()) {
			this._setTemperatureBelowMin(false);
			this._setTemperatureAboveMax(true);
			this._setTemperatureAboveThreshold(true);

			this.temperatureBelowMin = false;
			this.temperatureAboveMax = true;
			this.temperatureAboveThreshold = true;
			return;
		}

		if (bufferTemperature >= this.config.thresholdBufferTankTemperature()
				&& bufferTemperature < this.config.maxBufferTankTemperature()) {
			this._setTemperatureBelowMin(false);
			this._setTemperatureAboveMax(false);
			this._setTemperatureAboveThreshold(true);

			this.temperatureBelowMin = false;
			this.temperatureAboveMax = false;
			this.temperatureAboveThreshold = true;
			return;
		}

		this._setTemperatureBelowMin(false);
		this._setTemperatureAboveMax(false);
		this._setTemperatureAboveThreshold(false);

		this.temperatureBelowMin = false;
		this.temperatureAboveMax = false;
		this.temperatureAboveThreshold = false;

	}
	
	/**
	 * Handles the "temperature near max" logic including hysteresis,
	 * and optionally halves the target power if the buffer tank temperature
	 * is within the configured near-max range.
	 *
	 * Behavior:
	 *  - Starts the reduced-power hysteresis on the rising edge (false → true).
	 *  - Restarts it once the timer expires while still being in near-max range.
	 *  - Ends it once temperature is below the near-max range and the hysteresis has expired.
	 *  - Applies power reduction (target ÷ 2) while hysteresis is active.
	 *
	 * @param target current CHP target power in watts
	 * @return possibly reduced target power in watts
	 */
	private int applyNearMaxReduction(int target) {
	    boolean awaitingReduced = Boolean.TRUE.equals(this.getAwaitingReducedPowerHysteresis().get());
	    final boolean near = this.temperatureNearMax;
	    final boolean hystActive = isHysteresisActive(this.lastReducedPowerHysteresisTime, REDUCED_POWER_GRACE_SECONDS);

	    // 1) Rising edge: temperatureNearMax changed from false → true
	    if (near && !this.wasTemperatureNearMax) {
	        this.lastReducedPowerHysteresisTime = Instant.now(this.componentManager.getClock());
	        this._setAwaitingReducedPowerHysteresis(true);
	        awaitingReduced = true;
	    }

	    // 2) Timer expired but still near-max → restart hysteresis
	    if (near && !hystActive) {
	        this.lastReducedPowerHysteresisTime = Instant.now(this.componentManager.getClock());
	        this._setAwaitingReducedPowerHysteresis(true);
	        awaitingReduced = true;
	    }

	    // 3) Temperature back to normal and hysteresis expired → stop reduction
	    if (!near && !hystActive) {
	        this._setAwaitingReducedPowerHysteresis(false);
	        awaitingReduced = false;
	    }

	    // 4) Apply reduction while hysteresis is active
	    if (awaitingReduced) {
	        target = Math.floorDiv(target, 2);
	        this.logDebug(this.log, "Temperature near max -> reducing power");
	    }

	    this.wasTemperatureNearMax = near;
	    return target;
	}
	

	@Override
	public String debugLog() {
		// return null;
		return "Current Energy costs: " + this.getEnergyCosts().asString() + " €/h"
				+ "Current Energy costs without CHP: " + this.getEnergyCostsWithoutChp().asString() + " €/h"
				+ " State: " + this.state + " TargetPower: " + this.getActivePowerTarget().asString()
				+ " ChpActivePower: " + this.getChpActivePower().asString()

		;
	}
}
