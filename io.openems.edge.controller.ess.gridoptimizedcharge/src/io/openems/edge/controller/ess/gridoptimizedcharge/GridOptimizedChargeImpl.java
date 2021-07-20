package io.openems.edge.controller.ess.gridoptimizedcharge;

import org.osgi.service.cm.ConfigurationAdmin;
import org.osgi.service.component.ComponentContext;
import org.osgi.service.component.annotations.Activate;
import org.osgi.service.component.annotations.Component;
import org.osgi.service.component.annotations.ConfigurationPolicy;
import org.osgi.service.component.annotations.Deactivate;
import org.osgi.service.component.annotations.Modified;
import org.osgi.service.component.annotations.Reference;
import org.osgi.service.component.annotations.ReferenceCardinality;
import org.osgi.service.component.annotations.ReferencePolicy;
import org.osgi.service.component.annotations.ReferencePolicyOption;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventConstants;
import org.osgi.service.event.EventHandler;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.ComponentManagerProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.GridOptimizedCharge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE, //
		property = EventConstants.EVENT_TOPIC + "=" + EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE //
)
public class GridOptimizedChargeImpl extends AbstractOpenemsComponent implements GridOptimizedCharge, EventHandler,
		Controller, OpenemsComponent, TimedataProvider, ComponentManagerProvider {

	private final Logger log = LoggerFactory.getLogger(GridOptimizedChargeImpl.class);

	protected Config config = null;

	/**
	 * Delay Charge logic.
	 */
	private DelayCharge delayCharge;

	/**
	 * Sell to grid logic.
	 */
	private SellToGridLimit sellToGridLimit;

	/*
	 * Time counter for the important states
	 */
	private final CalculateActiveTime calculateDelayChargeTime = new CalculateActiveTime(this,
			GridOptimizedCharge.ChannelId.DELAY_CHARGE_TIME);
	private final CalculateActiveTime calculateSellToGridTime = new CalculateActiveTime(this,
			GridOptimizedCharge.ChannelId.SELL_TO_GRID_LIMIT_TIME);
	private final CalculateActiveTime calculateNoLimitationTime = new CalculateActiveTime(this,
			GridOptimizedCharge.ChannelId.NO_LIMITATION_TIME);
	
	@Reference
	protected Sum sum;

	@Reference
	protected PredictorManager predictorManager;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	protected ManagedSymmetricEss ess;

	@Reference
	protected SymmetricMeter meter;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	public GridOptimizedChargeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				GridOptimizedCharge.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	private void updateConfig(Config config) {
		this.config = config;
		this.delayCharge = new DelayCharge(this);
		this.sellToGridLimit = new SellToGridLimit(this);

		// update filter for 'ess'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "ess", config.ess_id())) {
			return;
		}

		// update filter for 'meter'
		if (OpenemsComponent.updateReferenceFilter(this.cm, this.servicePid(), "meter", config.meter_id())) {
			return;
		}
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE:

			// Updates the time channels.
			this.calculateTime();
			break;
		}
	}

	/**
	 * Counts up the time of each state when it is active.
	 */
	private void calculateTime() {
		boolean sellToGridLimitIsActive = false;
		boolean delayChargeLimitIsActive = false;
		boolean noLimitIsActive = false;

		SellToGridLimitState sellToGridLimitState = this.getSellToGridLimitState();
		DelayChargeState delayChargeState = this.getDelayChargeState();
		int sellToGridLimit = this.getSellToGridLimitMinimumChargeLimit().orElse(0);

		if (sellToGridLimitState.equals(SellToGridLimitState.ACTIVE_LIMIT_FIXED)) {
			sellToGridLimitIsActive = true;
		} else if (delayChargeState.equals(DelayChargeState.ACTIVE_LIMIT)) {
			delayChargeLimitIsActive = true;
		} else if (sellToGridLimitState.equals(SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT) && sellToGridLimit > 0) {
			sellToGridLimitIsActive = true;
		} else {
			noLimitIsActive = true;
		}

		this.calculateSellToGridTime.update(sellToGridLimitIsActive);
		this.calculateDelayChargeTime.update(delayChargeLimitIsActive);
		this.calculateNoLimitationTime.update(noLimitIsActive);
	}

	@Override
	public void run() throws OpenemsNamedException {

		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		GridMode gridMode = this.ess.getGridMode();
		if (gridMode.isUndefined()) {
			this.logWarn(this.log, "Grid-Mode is [UNDEFINED]");
		}
		switch (gridMode) {
		case ON_GRID:
		case UNDEFINED:
			break;
		case OFF_GRID:
			this._setSellToGridLimitState(SellToGridLimitState.UNDEFINED);
			this._setDelayChargeState(DelayChargeState.UNDEFINED);
			return;
		}

		Integer sellToGridLimitMinChargePower = null;
		Integer delayChargeMaxChargePower = null;

		/*
		 * Run the logic of the different modes, depending on the configuration
		 */
		switch (this.config.mode()) {
		case OFF:
			this.delayCharge.setDelayChargeStateAndLimit(DelayChargeState.DISABLED, null);
			this.sellToGridLimit.setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState.DISABLED, null);
			return;
		case AUTOMATIC:
			sellToGridLimitMinChargePower = this.sellToGridLimit.getSellToGridLimit();
			delayChargeMaxChargePower = this.delayCharge.getPredictiveDelayChargeMaxCharge();
			break;
		case MANUAL:
			sellToGridLimitMinChargePower = this.sellToGridLimit.getSellToGridLimit();
			delayChargeMaxChargePower = this.delayCharge.getManualDelayChargeMaxCharge();
			break;
		}

		// Prioritize both limits to get valid constraints for the ess & apply these.
		this.applyCalculatedPowerLimits(sellToGridLimitMinChargePower, delayChargeMaxChargePower);
	}

	/**
	 * Apply the calculated power limits.
	 * 
	 * @param sellToGridLimitMinChargePower minimum charge power
	 * @param delayChargeMaxChargePower     maximum charge power
	 * @throws OpenemsNamedException on error
	 */
	private void applyCalculatedPowerLimits(Integer sellToGridLimitMinChargePower, Integer delayChargeMaxChargePower)
			throws OpenemsNamedException {

		boolean delayChargeIsDefined = delayChargeMaxChargePower != null;
		boolean sellToGridLimitIsDefined = sellToGridLimitMinChargePower != null;

		Integer rawDelayChargeMaxChargePower = delayChargeMaxChargePower;

		if (delayChargeIsDefined) {
			// Calculate AC-Setpoint depending on the DC production
			delayChargeMaxChargePower = this.calculateDelayChargeAcLimit(delayChargeMaxChargePower);
		}

		/*
		 * Set sellToGridLimit if its lower than delayChargeLimit as fix value.
		 * 
		 * <p> e.g. sellToGridLimit [-10kW | -3kW] & delayCharge [-2kW | 10kW] - it will
		 * take -3 kW to reduce the grid power with the minimum power and avoid to
		 * charge more than needed.
		 */
		if (sellToGridLimitIsDefined && delayChargeIsDefined) {
			if (sellToGridLimitMinChargePower <= delayChargeMaxChargePower) {

				this.ess.setActivePowerEquals(sellToGridLimitMinChargePower);
				this.delayCharge.setDelayChargeStateAndLimit(DelayChargeState.NO_CHARGE_LIMIT, null);
				this.sellToGridLimit.setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState.ACTIVE_LIMIT_FIXED,
						sellToGridLimitMinChargePower);
				this.logDebug("Applying both constraints not possible - Set active power according to SellToGridLimit: "
						+ sellToGridLimitMinChargePower);
				return;
			}
		}

		// Apply maximum charge power
		if (delayChargeIsDefined) {
			this.delayCharge.applyCalculatedLimit(rawDelayChargeMaxChargePower, delayChargeMaxChargePower);
		}

		// Apply minimum charge power
		if (sellToGridLimitIsDefined) {
			this.sellToGridLimit.applyCalculatedMinimumChargePower(sellToGridLimitMinChargePower);
		}
	}

	/**
	 * Calculating the AC limit.
	 * 
	 * <p>
	 * Calculating the maximum charge power in AC systems and the maximum discharge
	 * power in DC systems as inverter setpoint.
	 * 
	 * @param delayChargeMaxChargePower maximum charge power of the battery
	 * @return Maximum power to is allowed to charged(AC) or discharged(DC)
	 */
	private int calculateDelayChargeAcLimit(int delayChargeMaxChargePower) {

		// Calculate AC-Setpoint depending on the DC production
		int productionDcPower = this.sum.getProductionDcActualPower().orElse(0);
		delayChargeMaxChargePower = productionDcPower - delayChargeMaxChargePower;

		return delayChargeMaxChargePower;
	}

	protected void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}

	@Override
	public ComponentManager getComponentManager() {
		return this.componentManager;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}
}
