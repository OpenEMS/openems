package io.openems.edge.controller.ess.gridoptimizedcharge;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.OptionalDouble;
import java.util.concurrent.CompletableFuture;

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
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.AbstractOpenemsComponent;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.ComponentManagerProvider;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.filter.RampFilter;
import io.openems.edge.common.meta.GridFeedInLimitationType;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.controller.api.Controller;
import io.openems.edge.energy.api.EnergySchedulable;
import io.openems.edge.energy.api.handler.EnergyScheduleHandler;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.predictor.api.manager.PredictorManager;
import io.openems.edge.timedata.api.Timedata;
import io.openems.edge.timedata.api.TimedataProvider;
import io.openems.edge.timedata.api.utils.CalculateActiveTime;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "Controller.Ess.GridOptimizedCharge", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
public class ControllerEssGridOptimizedChargeImpl extends AbstractOpenemsComponent
		implements ControllerEssGridOptimizedCharge, EnergySchedulable, Controller, OpenemsComponent, TimedataProvider,
		ComponentManagerProvider {

	/**
	 * Buffer in watt taken into account in the calculation of the first and last
	 * time when production is lower or higher than consumption.
	 */
	protected static final int DEFAULT_POWER_BUFFER = 100;

	protected final RampFilter rampFilter = new RampFilter();
	protected int maximumSellToGridPower = 0;

	private final Logger log = LoggerFactory.getLogger(ControllerEssGridOptimizedChargeImpl.class);

	/*
	 * Time counter for the important states
	 */
	private final CalculateActiveTime calculateDelayChargeTime = new CalculateActiveTime(this,
			ControllerEssGridOptimizedCharge.ChannelId.DELAY_CHARGE_TIME);
	private final CalculateActiveTime calculateSellToGridTime = new CalculateActiveTime(this,
			ControllerEssGridOptimizedCharge.ChannelId.SELL_TO_GRID_LIMIT_TIME);
	private final CalculateActiveTime calculateAvoidLowChargingTime = new CalculateActiveTime(this,
			ControllerEssGridOptimizedCharge.ChannelId.AVOID_LOW_CHARGING_TIME);
	private final CalculateActiveTime calculateNoLimitationTime = new CalculateActiveTime(this,
			ControllerEssGridOptimizedCharge.ChannelId.NO_LIMITATION_TIME);

	protected Config config = null;
	private EnergyScheduleHandler energyScheduleHandler;

	/** Delay Charge logic. */
	private DelayCharge delayCharge;
	/** Sell to grid logic. */
	private SellToGridLimit sellToGridLimit;
	/** Keeps the current day to detect changes in day. */
	private LocalDate currentDay = LocalDate.MIN;

	@Reference
	protected Sum sum;

	@Reference
	protected ComponentManager componentManager;

	@Reference
	protected PredictorManager predictorManager;

	@Reference
	protected ManagedSymmetricEss ess;

	@Reference
	protected ElectricityMeter meter;

	@Reference
	private ConfigurationAdmin cm;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected Meta meta;

	public ControllerEssGridOptimizedChargeImpl() {
		super(//
				OpenemsComponent.ChannelId.values(), //
				Controller.ChannelId.values(), //
				ControllerEssGridOptimizedCharge.ChannelId.values() //
		);
	}

	@Activate
	private void activate(ComponentContext context, Config config) {
		super.activate(context, config.id(), config.alias(), config.enabled());
		this.energyScheduleHandler = EnergyScheduler.buildEnergyScheduleHandler(this, //
				() -> this.config.enabled() //
						? switch (this.config.mode()) {
						case AUTOMATIC -> new EnergyScheduler.Config.Automatic();
						case MANUAL ->
							new EnergyScheduler.Config.Manual(DelayCharge.parseTime(this.config.manualTargetTime()));
						case OFF -> null;
						} //
						: null);
		this.updateConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		super.modified(context, config.id(), config.alias(), config.enabled());
		this.updateConfig(config);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	private void updateConfig(Config config) {
		this.config = config;
		if (config.maximumSellToGridPower() != -1) {
			this.migrateFeedPowerParaConfigValue(config.maximumSellToGridPower(), config.sellToGridLimitEnabled());
		}
		this.delayCharge = new DelayCharge(this);
		this.updateMaximumSellToGridPower();
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
	public void run() throws OpenemsNamedException {
		this.updateMaximumSellToGridPower();
		if (!this.ess.isManaged() && this.config.mode() != Mode.OFF) {
			this._setConfiguredEssIsNotManaged(true);
			return;
		}
		this._setConfiguredEssIsNotManaged(false);

		if (!this.sum.getProductionActivePower().isDefined()) {
			this._setNoValidProductionPredictionChannel(true);
			return;
		}
		this._setNoValidProductionPredictionChannel(false);

		// Updates the time channels.
		this.calculateTime();

		/*
		 * Check that we are On-Grid (and warn on undefined Grid-Mode)
		 */
		var gridMode = this.ess.getGridMode();
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

		this.resetChannelsAtMidnight();

		// Check if the logic already started or should start
		if (!this.getStartEpochSeconds().isDefined()) {

			var clock = this.componentManager.getClock();
			IntegerReadChannel productionChannel = this.sum.getProductionActivePowerChannel();

			// Check start if production not already reached the maximum sell to grid power
			if (productionChannel.value().orElse(0) < this.maximumSellToGridPower) {

				/*
				 * Calculate the average with the last 100 values of production and consumption
				 */
				var productionAvgOpt = this.getChannelAverageOfPastSeconds(100, productionChannel);
				var consumptionAvgOpt = this.getChannelAverageOfPastSeconds(100,
						this.sum.getConsumptionActivePowerChannel());

				var production = productionAvgOpt.isPresent() ? productionAvgOpt.getAsDouble() : 0;
				var consumption = consumptionAvgOpt.isPresent() ? consumptionAvgOpt.getAsDouble() : 0;

				// Initiate the start time if the production is higher than the consumption
				if ((production <= 100) || (production <= consumption + DEFAULT_POWER_BUFFER)) {
					// No restriction required so far, as not enough is produced
					this.delayCharge.setDelayChargeStateAndLimit(DelayChargeState.NOT_STARTED, null);
					this.sellToGridLimit.setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState.NOT_STARTED, null);
					return;
				}
			}
			this._setStartEpochSeconds(LocalTime.now(clock), clock);
		}

		Integer sellToGridLimitMinChargePower = null;
		Integer delayChargeMaxChargePower = null;

		/*
		 * Run the logic of the different modes, depending on the configuration
		 */
		switch (this.config.mode()) {
		case OFF:
			this.delayCharge.setDelayChargeStateAndLimit(DelayChargeState.DISABLED, null);
			sellToGridLimitMinChargePower = this.sellToGridLimit.getSellToGridLimit();
			break;
		case AUTOMATIC:
			sellToGridLimitMinChargePower = this.sellToGridLimit.getSellToGridLimit();
			delayChargeMaxChargePower = this.delayCharge.getPredictiveDelayChargeMaxCharge();
			break;
		case MANUAL:
			sellToGridLimitMinChargePower = this.sellToGridLimit.getSellToGridLimit();
			delayChargeMaxChargePower = this.delayCharge.getManualDelayChargeMaxCharge();
			break;
		}

		this.predictChargeStart();

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

		var delayChargeIsDefined = delayChargeMaxChargePower != null;
		var sellToGridLimitIsDefined = sellToGridLimitMinChargePower != null;

		var rawDelayChargeMaxChargePower = delayChargeMaxChargePower;

		if (delayChargeIsDefined) {
			// Calculate AC-Setpoint depending on the DC production
			delayChargeMaxChargePower = this.calculateDelayChargeAcLimit(delayChargeMaxChargePower);
		}

		/*
		 * Set sellToGridLimit if it's lower than delayChargeLimit as fix value.
		 *
		 * <p> e.g. sellToGridLimit [-10kW | -3kW] & delayCharge [-2kW | 10kW] - it will
		 * take -3 kW to reduce the grid power with the minimum power and avoid to
		 * charge more than needed.
		 */
		if (sellToGridLimitIsDefined && delayChargeIsDefined) {
			if (sellToGridLimitMinChargePower <= delayChargeMaxChargePower) {

				this.ess.setActivePowerEquals(sellToGridLimitMinChargePower);
				this.sellToGridLimit.setSellToGridLimitChannelsAndLastLimit(SellToGridLimitState.ACTIVE_LIMIT_FIXED,
						sellToGridLimitMinChargePower);
				this.logDebug("Applying both constraints not possible - Set active power according to SellToGridLimit: "
						+ sellToGridLimitMinChargePower);

				this.delayCharge.setDelayChargeStateAndLimit(DelayChargeState.NO_CHARGE_LIMIT, null);

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
	 * @return Maximum allowed power for charging (AC) or discharging (DC)
	 */
	private int calculateDelayChargeAcLimit(int delayChargeMaxChargePower) {

		// Calculate AC-Setpoint depending on the DC production
		int productionDcPower = this.sum.getProductionDcActualPower().orElse(0);
		return productionDcPower - delayChargeMaxChargePower;
	}

	/**
	 * Counts up the time of each state when it is active.
	 */
	private void calculateTime() {
		var sellToGridLimitIsActive = false;
		var delayChargeLimitIsActive = false;
		var noLimitIsActive = false;
		var avoidLowChargingIsActive = false;

		var sellToGridLimitState = this.getSellToGridLimitState();
		var delayChargeState = this.getDelayChargeState();
		int sellToGridLimit = this.getSellToGridLimitMinimumChargeLimit().orElse(0);

		if (sellToGridLimitState.equals(SellToGridLimitState.ACTIVE_LIMIT_FIXED)) {
			sellToGridLimitIsActive = true;
		} else if (delayChargeState.equals(DelayChargeState.ACTIVE_LIMIT)) {
			delayChargeLimitIsActive = true;
		} else if (delayChargeState.equals(DelayChargeState.AVOID_LOW_CHARGING)) {
			avoidLowChargingIsActive = true;
		} else if (sellToGridLimitState.equals(SellToGridLimitState.ACTIVE_LIMIT_CONSTRAINT) && sellToGridLimit > 0) {
			sellToGridLimitIsActive = true;
		} else {
			noLimitIsActive = true;
		}

		this.calculateSellToGridTime.update(sellToGridLimitIsActive);
		this.calculateDelayChargeTime.update(delayChargeLimitIsActive);
		this.calculateAvoidLowChargingTime.update(avoidLowChargingIsActive);
		this.calculateNoLimitationTime.update(noLimitIsActive);
	}

	/**
	 * Resets the predicted target minutes at midnight.
	 */
	private void resetChannelsAtMidnight() {

		var today = LocalDate.now(this.componentManager.getClock());
		if (!this.currentDay.equals(today)) {

			/*
			 * Target minutes
			 */
			this._setPredictedTargetMinute(null);
			this._setPredictedTargetMinuteAdjusted(null);
			this._setStartEpochSeconds(null);
			this._setPredictedChargeStartEpochSeconds(null);

			this.currentDay = today;
		}
	}

	/**
	 * Calculates the average of the past channel values.
	 *
	 * @param consideredSeconds Seconds that should be taken into account for the
	 *                          past channels
	 * @param channel           Channel whose values are calculated
	 * @return Average of the past channel values
	 */
	private OptionalDouble getChannelAverageOfPastSeconds(int consideredSeconds, IntegerReadChannel channel) {

		// Get the past channel values
		var pastValues = channel.getPastValues()
				.tailMap(LocalDateTime.now(this.componentManager.getClock()).minusSeconds(consideredSeconds), true)
				.values();

		// Make sure we have at least one value
		if (pastValues.isEmpty()) {
			pastValues = new ArrayList<>();
			pastValues.add(channel.value());
		}

		return pastValues.stream().filter(Value::isDefined) //
				.mapToInt(Value::get) //
				.average();
	}

	/**
	 * Predicted charge start time.
	 * 
	 * <p>
	 * Predicted charge start time as epoch seconds and set the channel.
	 * 
	 * @throws OpenemsException on error
	 */
	private void predictChargeStart() throws OpenemsException {
		var targetTime = this.getTargetMinute().orElse(DelayCharge.DEFAULT_TARGET_TIME.get(ChronoField.MINUTE_OF_DAY));
		var capacity = this.ess.getCapacity().getOrError();
		var soc = this.ess.getSoc().getOrError();

		// Predict ChargeStart
		Long epochChargeStartTime = DelayCharge.getPredictedChargeStart(targetTime, capacity, soc,
				this.componentManager.getClock());
		if (epochChargeStartTime == null) {
			this._setPredictedChargeStartEpochSeconds(null);
			return;
		}

		/*
		 * Set ChargeStart only until we have charged (Start time would increase)
		 */
		var currentVal = this.getPredictedChargeStartEpochSeconds().asOptional();

		// ChargeStart not set
		if (currentVal.isEmpty()) {
			this._setPredictedChargeStartEpochSeconds(epochChargeStartTime);
			return;
		}

		// ChargeStart time is earlier because the remaining capacity increased
		if (currentVal.get() < epochChargeStartTime) {
			this._setPredictedChargeStartEpochSeconds(epochChargeStartTime);
			return;
		}

		// ChargeStart already set
		this._setPredictedChargeStartEpochSeconds(currentVal.get());
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

	@Override
	public EnergyScheduleHandler getEnergyScheduleHandler() {
		return this.energyScheduleHandler;
	}

	private void updateMaximumSellToGridPower() {
		// TODO: add RippleControlReceiver logic in near future
		if (this.meta.getGridFeedInLimitationType().asEnum() == GridFeedInLimitationType.DYNAMIC_LIMITATION) {
			this.maximumSellToGridPower = this.meta.getMaximumGridFeedInLimit();
		}
	}

	/**
	 * Shifts the feed-in power limitation setting from the controller to the Meta
	 * App. In the future, no feed-in limitation should be configured directly in
	 * the controller. To avoid inconsistencies caused by having this value defined
	 * in different Controllers (e.g. GridOptimizeCharge and
	 * GoodWeBatteryInverterImpl, also potentially with different values), the
	 * setting will be managed globally within the Meta App. Simply removing the
	 * config parameter would lead to issues for existing systems, as the value
	 * would be lost. Therefore, a gradual migration is necessary. For this, the
	 * current value is extracted from the "maxSellToGridPower" variable and
	 * transferred to the Meta App. As a placeholder, the value -1 will be written
	 * into the inverter's config. In a future version, this migration logic can
	 * then be removed entirely.
	 *
	 * @param maximumGridFeedInLimit          the maximum grid feed in limit.
	 * @param isMaximumGridFeedInLimitEnabled is maximum grid feed in limit enabled?
	 */
	private void migrateFeedPowerParaConfigValue(int maximumGridFeedInLimit, boolean isMaximumGridFeedInLimitEnabled) {
		try {
			var config = this.cm.getConfiguration(Meta.SINGLETON_SERVICE_PID, "?");
			var properties = config.getProperties();
			var gridFeedInLimitationType = isMaximumGridFeedInLimitEnabled ? GridFeedInLimitationType.DYNAMIC_LIMITATION
					: GridFeedInLimitationType.NO_LIMITATION;

			if (properties.get("maximumGridFeedInLimit") != null
					|| properties.get("gridFeedInLimitationType") != null) {
				// Already migrated, do nothing
				this.log.warn(
						"Value is already migrated to Meta App, skipping migration. Set the value in the Meta Component instead.");
				this.resetMaximumSellToGridPower();
				return;
			}

			properties.put("maximumGridFeedInLimit", maximumGridFeedInLimit);
			properties.put("gridFeedInLimitationType", gridFeedInLimitationType.name());
			config.updateIfDifferent(properties);

			this.resetMaximumSellToGridPower();
		} catch (IOException e) {
			this.log.warn("Unable to update {} config", this.id(), e);
		}
	}

	private void resetMaximumSellToGridPower() throws IOException {
		// Async update to avoid modifying the current Component during activation or
		// modified
		CompletableFuture.runAsync(() -> {
			try {
				var config = this.cm.getConfiguration(this.servicePid(), "?");
				var properties = config.getProperties();
				properties.put("maximumSellToGridPower", -1);
				config.updateIfDifferent(properties);
			} catch (IOException e) {
				this.log.warn("Unable to reset maximumSellToGridPower in GridOptimizedCharge", e);
			}
		});
	}
}
