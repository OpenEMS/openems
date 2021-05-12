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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.exceptions.OpenemsException;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.sum.GridMode;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.meter.api.SymmetricMeter;
import io.openems.edge.predictor.api.manager.PredictorManager;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.GridOptimizedCharge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class GridOptimizedChargeImpl extends AbstractOpenemsComponent
		implements GridOptimizedCharge, Controller, OpenemsComponent {

	private final Logger log = LoggerFactory.getLogger(GridOptimizedChargeImpl.class);

	protected Config config = null;

	private DelayCharge delayCharge;

	private SellToGridLimit sellToGridLimit;

	@Reference
	protected Sum sum;

	@Reference
	protected PredictorManager predictorManager;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected ManagedSymmetricEss ess;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected SymmetricMeter meter;

	public GridOptimizedChargeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				GridOptimizedCharge.ChannelId.values() //
		);
	}

	@Activate
	void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Modified
	void modified(ComponentContext context, Config config) throws OpenemsNamedException {
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

				this.setActivePowerConstraint(sellToGridLimitMinChargePower, Relationship.EQUALS);
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

	private int calculateDelayChargeAcLimit(int delayChargeMaxChargePower) {

		// Calculate AC-Setpoint depending on the DC production
		int productionDcPower = this.sum.getProductionDcActualPower().orElse(0);
		delayChargeMaxChargePower = productionDcPower - delayChargeMaxChargePower;

		return delayChargeMaxChargePower;
	}

	/**
	 * Set active power constraint.
	 * 
	 * <p>
	 * Sets an active power constraint depending on the given power limit and
	 * relationship.
	 * 
	 * @param currentLimit limit that needs to be set
	 * @param relationship Relationship of the limit
	 * @return was the constraint successfully set
	 * 
	 */
	protected boolean setActivePowerConstraint(int currentLimit, Relationship relationship) {
		try {
			// Set the constraints and the according channels for testing
			switch (relationship) {
			case EQUALS:
				this.ess.setActivePowerEquals(currentLimit);
				break;
			case GREATER_OR_EQUALS:
				this.ess.setActivePowerGreaterOrEquals(currentLimit);
				break;
			case LESS_OR_EQUALS:
				this.ess.setActivePowerLessOrEquals(currentLimit);
				break;
			}
			return true;
		} catch (OpenemsNamedException e) {
			return false;
		}
	}

	/**
	 * Get the int value of the given Channel value.
	 * 
	 * <p>
	 * Set StateChannel's and throws OpenemsException if the value is not defined.
	 * 
	 * @param value        Channel value
	 * @param stateChannel state channel that should be set
	 * @return int value
	 * @throws OpenemsException on null value
	 */
	protected int getIntValueOrSetStateAndException(Value<Integer> value,
			io.openems.edge.common.channel.ChannelId stateChannel) throws OpenemsException {
		if (!value.isDefined()) {
			this.channel(stateChannel).setNextValue(true);
			this.logDebug(stateChannel.doc().getText());
			throw new OpenemsException(stateChannel.doc().getText());
		}
		this.channel(stateChannel).setNextValue(false);
		return value.get();
	}

	protected void logDebug(String message) {
		if (this.config.debugMode()) {
			this.logInfo(this.log, message);
		}
	}
}
