package io.openems.edge.controller.chp.cost;

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
		name = "Controller.CHP.Cost.Optimization", //
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
	private State state = State.UNDEFINED;
	private Integer gridPowerWithoutChp = 0;
	private Integer applyPowerTarget = null;
	private Integer lastApplyPowerTarget = null;	
	private Double currentPrice = null;	
	private Integer currentEnergyCost = 0;	

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private TimeOfUseTariff timeOfUseTariff;

	@Reference
	private ElectricityMeter gridMeter;

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

		// update filter for 'chp manager device'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "chp", config.chp_id())) {
			return;
		}

		// update filter for 'meter'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
	}

	@Override
	public void handleEvent(Event event) {

		// super.handleEvent(event);

		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_CONTROLLERS:
			int i = 0;
			break;
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_CONTROLLERS:
			i = 0;
			break;

		}
	}

	@Override
	public void run() throws OpenemsNamedException {

		// this.log.info("chpActivePower: " + (this.chp != null ?
		// this.chp.getActivePower().get() : null));

		Integer gridActivePower = this.gridMeter.getActivePower().get();
		Integer chpActivePower = this.chp.getGeneratorActivePower().get();
		// Integer chpActivePower = 0;
		Integer currentEnergyCost = this.getCurrentCost(gridPowerWithoutChp);
		
		if (!checkOperationalValues() || currentEnergyCost == null ) {
			this.logWarn(this.log, "Controller not ready");
			this.changeState(State.ERROR);
			return;
		}		
		this.gridPowerWithoutChp = gridActivePower + chpActivePower;
		
		this._setEnergyCosts(currentEnergyCost); // save value in channel

		switch (this.state) {
		case ERROR:
			if (checkOperationalValues()) {
				this.changeState(State.NORMAL);
				return;
			}
			break;
		case UNDEFINED:
			if (checkOperationalValues()) {
				this.changeState(State.NORMAL);
				return;
			}			
			break;
		case NORMAL:

			if (currentEnergyCost > this.config.maxCost()) {
				// ready to start

				// start chp
				this.changeState(State.CHP_ACTIVE);

				// Start Timer
				this.lastStartHysteresisTime = Instant.now(this.componentManager.getClock());
			} else {
				
				// Only turn off if there was a change in target power
				if (this.lastApplyPowerTarget != this.applyPowerTarget) {
					this.chp.applyPower(this.applyPowerTarget);	
				} else {
					// do nothing
				}
				
			}

			break;
		case CHP_ACTIVE:
			// check hysteresis
			// check target power
			// check costs
			//this._setAwaitingStartHysteresis(false); // 
			
			if (currentEnergyCost > this.config.maxCost()) {
				this.applyPowerTarget = this.calculateChpPowerTarget();
				this.chp.applyPower(this.applyPowerTarget);
				this._setAwaitingStartHysteresis(false);

			} else { // costs are below threshold -> can stop chp?
				// check last START time
				if (isHysteresisActive(this.lastStartHysteresisTime, this.config.startHyteresis())) {
					this.applyPowerTarget = this.calculateChpPowerTarget();
					this.chp.applyPower(this.applyPowerTarget);					
					this._setAwaitingStartHysteresis(true);
				} else {
					// stop chp
					this.applyPowerTarget = null;
					this.chp.applyPower(this.applyPowerTarget);
					this.changeState(State.CHP_INACTIVE);
					// Start Timer
					this.lastStopHysteresisTime = Instant.now(this.componentManager.getClock());
					this._setAwaitingStartHysteresis(false);
				}
			}
			
			this.lastApplyPowerTarget = this.applyPowerTarget;
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
	
	private boolean isHysteresisActive (Instant hysteresisTime, int configuredHysteresis) {
		if (Duration.between(//
				hysteresisTime, //
				Instant.now(this.componentManager.getClock()) //
		).toSeconds() >= configuredHysteresis) {
			return false;
		} else {
			return true;
		}
	}
	

	private int calculateChpPowerTarget() {
		int chpTargetPower = 0;
		
		if (this.gridPowerWithoutChp != null) {
			
			chpTargetPower = (int) Math.round( this.gridPowerWithoutChp * this.config.maxCost() / (double) this.currentEnergyCost) ;
			
			// Apply limits
			chpTargetPower = Math.min(this.config.maxActivePower(), chpTargetPower);
			chpTargetPower = Math.min(chpTargetPower, this.gridPowerWithoutChp);
		}

		this._setActivePowerTarget(chpTargetPower); // feed channel
		return chpTargetPower;
	}

	private Integer getCurrentCost(Integer gridPowerWithoutChp) {

		if (!this.timeOfUseTariff.getPrices().isEmpty()) {
			this.currentPrice = this.timeOfUseTariff.getPrices().getFirst() / 10; // Price in €/MWh. Divided to ct/kWh
		} else {
			return null;
		}

		if (gridPowerWithoutChp != null && this.currentPrice != null && gridPowerWithoutChp > 0) {
			this.currentEnergyCost = (int) Math.round((this.currentPrice * (gridPowerWithoutChp / 1000))); // Ct
		}


		return currentEnergyCost;
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
		
		
		if (isHysteresisActive(this.lastTransistionChangeTime, STATE_TRANSITION_HYSTERESIS)){
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
		this.logDebug(this.log, "" + "gridMeter: " + this.gridMeter + "chp: " + this.chp + "gridActivePower: "
				+ (this.gridMeter != null ? this.gridMeter.getActivePower().get() : null));

		if (this.chp == null || this.gridMeter == null || this.gridMeter.getMeterType() != MeterType.GRID
				|| this.timeOfUseTariff == null || this.gridMeter.getActivePower().get() == null || this.chp.getGeneratorActivePower().get() == null) {
			this.log.warn("Controller not ready");
			this.changeState(State.ERROR);
			return false;
		}
		return true;

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
		return "Current Energy costs: " + this.getEnergyCosts().asString() + " Ct/kWh"+
				" State: " + this.state +
				" TargetPower: " + this.getActivePowerTarget().asString() 
				
				
				;
	}	
}
