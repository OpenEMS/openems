package io.openems.edge.controller.ess.chargedischargelimiter;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

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
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.controller.api.Controller;

import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.timedata.api.utils.CalculateEnergyFromPower;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.ChargeDischargeLimiter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerChargeDischargeLimiterImpl extends AbstractOpenemsComponent
		implements ControllerChargeDischargeLimiter, TimedataProvider, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(ControllerChargeDischargeLimiterImpl.class);

	private final CalculateEnergyFromPower calculateChargeEnergy = new CalculateEnergyFromPower(this,
			ControllerChargeDischargeLimiter.ChannelId.ACTIVE_CHARGE_ENERGY);

	private Config config;
	
	/**
	 * Length of hysteresis in minutes. States are not changed quicker than this.
	 */
	private static final int HYSTERESIS = 5;
	private Instant lastStateChange = Instant.MIN;

	private String essId;
	private int minSoc = 0;
	private int maxSoc = 0;
	private int forceChargeSoc = 0;
	private int forceChargePower = 0;	
	

	@Reference
	private ComponentManager componentManager;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;


	public ControllerChargeDischargeLimiterImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerChargeDischargeLimiter.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());

		this.config = config;
		try {
			ManagedSymmetricEss ess = this.componentManager.getComponent(config.ess_id());
		} catch (OpenemsNamedException e) {
			
			e.printStackTrace();
		} 
	}
	
	
	
	/*
	 * 
	 * 
	 * this.minSoc = config.minSoc(); // min SoC this.maxSoc = config.minSoc(); //
	 * max. Soc this.forceChargeSoc = config.forceChargeSoc(); // if battery need
	 * balancing we charge to this value this.forceChargePower =
	 * config.forceChargePower(); // if battery need balancing we charge to this
	 * value
	 * 
	 * ManagedSymmetricEss ess = this.componentManager.getComponent(this.essId);
	 * 
	 */

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void run() throws OpenemsNamedException {
// method stub		

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
	public Timedata getTimedata() {
		return this.timedata;
	}

	private void calculateEnergy() {
	    // Calculate Energy
	    var activePower = this.ess.getActivePower();

	    if (activePower == null) {
	        // Not available
	        this.calculateChargeEnergy.update(null);
	    } else if (activePower < 0) {
	        this.calculateChargeEnergy.update(activePower * -1);
	    } 

	}


}
