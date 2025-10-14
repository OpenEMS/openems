package io.openems.edge.controller.chp.costoptimization;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.stream.Stream;

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
	private Instant lastTransistionChangeTime = Instant.MIN;
	private Instant lastStopHysteresisTime = Instant.MIN;
	private Instant lastStartHysteresisTime = Instant.MIN;
	private Instant lastPreparationHysteresisTime = Instant.MIN;
	private State state = State.UNDEFINED;
	private Integer gridPowerWithoutChp = 0;
	private Integer applyPowerTarget = 0;
	// private Integer lastApplyPowerTarget = null;
	private Double currentPrice = null;
	private Double currentEnergyCost = 0.0;
	
	private final Clock clock = Clock.systemDefaultZone(); // oder injizieren
	private int offset = 180; 
	

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
		if (!checkOperationalValues()) {
			this.logWarn(this.log, "Controller not ready");
			this.changeState(State.ERROR);
			return;
		}
		Integer gridActivePower = this.gridMeter.getActivePower().get();
		Integer chpActivePower = this.chp.getGeneratorActivePower().get();

		this.gridPowerWithoutChp = gridActivePower + chpActivePower;
		Double currentEnergyCostsWithoutChp = this.getCurrentCost(gridPowerWithoutChp);
		Double currentEnergyCosts = this.getCurrentCost(gridActivePower);
		
		Double futureEnergyCostsWithoutChp = this.getFutureCost(gridPowerWithoutChp);
		//Double futureEnergyCosts = this.getFutureCost(gridActivePower);		

		this._setEnergyCostsWithoutChp(currentEnergyCostsWithoutChp); // save value in channel
		this._setEnergyCosts(currentEnergyCosts); // save value in channel
		this._setChpActivePower(chpActivePower); // save value in channel
		this._setAwaitingDeviceHysteresis(this.chp.getAwaitingHysteresis().get());

		switch (this.state) {
		case ERROR:
			if (checkOperationalValues()) {
				this.changeState(State.NORMAL);
				return;
			}
			this.chp.applyPreparation(false);
			break;
		case UNDEFINED:
			if (checkOperationalValues()) {
				this.changeState(State.NORMAL);
				return;
			}
			this.chp.applyPreparation(false);
			break;
		case NORMAL:

			if (this.config.mode() == Mode.MANUAL_ON) {
				this.changeState(State.CHP_ACTIVE);
				this.lastStartHysteresisTime = Instant.now(this.componentManager.getClock());
				break;
			}

			if (this.config.mode() == Mode.MANUAL_OFF) {
				this.chp.applyPower(null); // off
				this.lastStopHysteresisTime = Instant.now(this.componentManager.getClock());
				this.changeState(State.CHP_INACTIVE);	
				this.chp.applyPreparation(false);				
				break;
			}
			
			if (!checkOperationalValues()) {
				this.changeState(State.ERROR);
				break;
				
			}

			if (futureEnergyCostsWithoutChp > this.config.maxCost()) {
				this.changeState(State.CHP_PREPARING);
				this.lastPreparationHysteresisTime = Instant.now(this.componentManager.getClock());
				break;
			}
			
			if (currentEnergyCostsWithoutChp > this.config.maxCost()) {
				// ready to start

				// start chp
				this.changeState(State.CHP_ACTIVE);

				// Start Timer
				this.lastStartHysteresisTime = Instant.now(this.componentManager.getClock());
			} else {

				// Only turn off if there was a change in target power
				// if (this.lastApplyPowerTarget != this.applyPowerTarget) {
				// this.chp.applyPower(this.applyPowerTarget);
				// } else {
				// do nothing
				// }
				this._setActivePowerTarget(this.applyPowerTarget);
				this.chp.applyPower(this.applyPowerTarget);
			}
			this.chp.applyPreparation(false); // no preparation in normal mode
			break;
		case CHP_PREPARING: // energy cost will exceed maximum. 
							// prepare CHPs, i.e. lower temperatures produced by other heating systems

			if (this.config.mode() == Mode.MANUAL_ON) {
				this.changeState(State.CHP_ACTIVE);
				this.lastStartHysteresisTime = Instant.now(this.componentManager.getClock());
				break;
			}

			if (this.config.mode() == Mode.MANUAL_OFF) {
				this.chp.applyPower(null); // off
				this.lastStopHysteresisTime = Instant.now(this.componentManager.getClock());
				this.chp.applyPreparation(false);
				this.changeState(State.CHP_INACTIVE);
				break;
			}

			if (futureEnergyCostsWithoutChp < this.config.maxCost()) { // can we  go back to normal?
				if (isHysteresisActive(this.lastPreparationHysteresisTime, this.config.preparationHyteresis())) { // back to normal if hysteresis is over
					
					// prepare something...
					this.chp.applyPreparation(true);
					this._setAwaitingPreparationHysteresis(true);					
					
				} else {
					this.changeState(State.NORMAL);
					this.chp.applyPreparation(false);
					this._setAwaitingPreparationHysteresis(false);
				}
				
				
			}
			
			if (currentEnergyCostsWithoutChp > this.config.maxCost()) {
				// ready to start

				// start chp
				this.changeState(State.CHP_ACTIVE);
								

				// Start Timer
				this.lastStartHysteresisTime = Instant.now(this.componentManager.getClock());
			} else {

				// Only turn off if there was a change in target power
				// if (this.lastApplyPowerTarget != this.applyPowerTarget) {
				// this.chp.applyPower(this.applyPowerTarget);
				// } else {
				// do nothing
				// }
				this._setActivePowerTarget(this.applyPowerTarget);
				this.chp.applyPower(this.applyPowerTarget);
			}

			break;			
		case CHP_ACTIVE:
			// check hysteresis
			// check target power
			// check costs
			// this._setAwaitingStartHysteresis(false); //

			if (this.config.mode() == Mode.MANUAL_ON) {
				this.chp.applyPower(this.config.maxActivePower()); // full power
				break;
			}

			if (this.config.mode() == Mode.MANUAL_OFF) {
				// stop chp
				this.applyPowerTarget = 0;
				this.chp.applyPower(this.applyPowerTarget);
				this.changeState(State.CHP_INACTIVE);
				// Start Timer
				this.lastStopHysteresisTime = Instant.now(this.componentManager.getClock());
				this._setAwaitingStartHysteresis(false);
				this._setAwaitingPreparationHysteresis(false);
				break;
			}
			
			// Automatic mode
			this._setAwaitingPreparationHysteresis(false); // Preparation Hysteresis has to be stopped anyway
			this.chp.applyPreparation(false);			

			if (currentEnergyCostsWithoutChp > this.config.maxCost()) {
				this.applyPowerTarget = this.calculateChpPowerTarget(currentEnergyCosts);
				this.chp.applyPower(this.applyPowerTarget);
				this._setAwaitingStartHysteresis(false);
			} else { // costs are below threshold or not in automatic mode -> can stop chp?
				// check last START time
				if (isHysteresisActive(this.lastStartHysteresisTime, this.config.startHyteresis())) {
					this.applyPowerTarget = this.calculateChpPowerTarget(currentEnergyCosts);
					this.chp.applyPower(this.applyPowerTarget);
					this._setAwaitingStartHysteresis(true);
				} else {
					// stop chp
					this.applyPowerTarget = 0;
					this.chp.applyPower(this.applyPowerTarget);
					this.changeState(State.CHP_INACTIVE);
					// Start Timer
					this.lastStopHysteresisTime = Instant.now(this.componentManager.getClock());
					this._setAwaitingStartHysteresis(false);
				}
			}

			// this.lastApplyPowerTarget = this.applyPowerTarget;
			break;
		case CHP_INACTIVE: // stopped

			if (isHysteresisActive(this.lastStopHysteresisTime, this.config.stopHyteresis())) {
				this._setAwaitingStopHysteresis(true);
			} else {
				this._setAwaitingStopHysteresis(false);
				this.changeState(State.NORMAL);
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

	/**
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
	 */
	private int calculateChpPowerTarget(Double currentEnergyCost) {
		
	    if (currentEnergyCost <= 0.0) {
	        this._setActivePowerTarget(0);
	        return 0;
	    }
		int chpTargetPower = 0;

		if (this.gridPowerWithoutChp != null) {

			chpTargetPower = (int) Math
					.round(this.gridPowerWithoutChp * this.config.maxCost() / currentEnergyCost);

			// Apply limits
			chpTargetPower = Math.min(this.config.maxActivePower(), chpTargetPower);
			chpTargetPower = Math.min(chpTargetPower, this.gridPowerWithoutChp); // sell to grid is not allowed
		}

		this._setActivePowerTarget(chpTargetPower); // feed channel
		return chpTargetPower;
	}

	private Double getCurrentCost(Integer power) {
		
		if (!this.timeOfUseTariff.getPrices().isEmpty() && power > 0) {
			this.currentPrice = this.timeOfUseTariff.getPrices().getFirst(); // Price in €/MWh.
		} else {
			return 0.0;
		}

		return Math.round((this.currentPrice * power / 1_000_000.0) * 1000.0) / 1000.0;
	}


	private Double getFutureCost(Integer power) {
	
		
		if (!this.timeOfUseTariff.getPrices().isEmpty() && power != null && power > 0) {
			//this.futurePrice = this.timeOfUseTariff.getPrices().getAt(t); // Price in €/MWh.
			
			Double[] arrFuturePrices = this.timeOfUseTariff.getPrices().asArray();
			double avgEurPerMWh = avgFirst3(arrFuturePrices);

			double eurPerHour = Math.round((avgEurPerMWh * (power / 1_000_000.0)) * 1000.0) / 1000.0;	
			return eurPerHour;

		} else {
			return 0.0;
		}
	
	}	
	
	private static double avgFirst3(Double[] a) {
	    if (a == null || a.length == 0) return 0.0;
	    int n = Math.min(3, a.length);
	    double sum = 0.0;
	    int cnt = 0;
	    for (int i = 0; i < n; i++) {
	        if (a[i] != null) {
	            sum += a[i];
	            cnt++;
	        }
	    }
	    if (cnt == 0) return 0.0;
	    double avg = sum / cnt;                 // €/MWh
	    return Math.round(avg * 10.0) / 10.0;   // auf 1 Nachkommastelle
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

		if (isHysteresisActive(this.lastTransistionChangeTime, STATE_TRANSITION_HYSTERESIS)) {
			this._setAwaitingTransitionHysteresis(true);
			return false;
		} else {
			this.state = nextState;
			this.lastTransistionChangeTime = Instant.now(this.componentManager.getClock());
			this._setAwaitingTransitionHysteresis(false);
			this._setStateMachine(nextState);
			this.logDebug(this.log, "Change state " + this.state + "->" + nextState);

			return true;
		}
	}

	private boolean checkOperationalValues() {
		this.logDebug(this.log,
				"" + " gridMeter: " + this.gridMeter + " chp: " + this.chp + " gridActivePower: "
						+ (this.gridMeter != null ? this.gridMeter.getActivePower().get() : null)
						+ (this.chp != null ? this.chp.getGeneratorActivePower().get() : null)

		);

		if (this.chp == null) {
			this.log.warn("Controller not ready. No CHP available");
			this.changeState(State.ERROR);
			return false;
		}
		
		if (this.gridMeter == null) {
			this.log.warn("Controller not ready. GridMeter is NULL");
			this.changeState(State.ERROR);
			return false;
		}		
		
		if (this.gridMeter.getMeterType() != MeterType.GRID) {
			this.log.warn("Controller not ready. Metertype is not GRID");
			this.changeState(State.ERROR);
			return false;

		}

		if (this.gridMeter.getActivePower().get() == null) {
			this.log.warn("Controller not ready. No value for ActivePower from GridMeter");
			this.changeState(State.ERROR);
			return false;
		}

		if (this.timeOfUseTariff == null) {
			this.log.warn("Controller not ready. No prices available because TimeOfUse Controller is NULL");
			this.changeState(State.ERROR);
			return false;
		}

		if (this.chp.getGeneratorActivePower().get() == null) {
			this.log.warn("Controller not ready. No value for ActivePower from generator(s)");
			this.changeState(State.ERROR);
			return false;
		}
		
		if (this.timeOfUseTariff.getPrices().isEmpty()) {
			this.log.warn("Controller not ready. No prices available");
			this.changeState(State.ERROR);
			return false;
		}

		return true;

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
			this.logInfo(this.log, message);
		}
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
