package io.openems.edge.goodwe.batteryinverter;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;
import static io.openems.edge.ess.power.api.Relationship.LESS_OR_EQUALS;

import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

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
import org.osgi.service.event.EventHandler;
import org.osgi.service.event.propertytypes.EventTopics;
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.fenecon.home.BatteryFeneconHome;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.ComponentManager;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.meta.GridFeedInLimitationType;
import io.openems.edge.common.meta.Meta;
import io.openems.edge.common.serialnumber.SerialNumberStorage;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.common.update.Updateable;
import io.openems.edge.controller.ess.ripplecontrolreceiver.ControllerEssRippleControlReceiver;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.goodwe.batteryinverter.statemachine.Context;
import io.openems.edge.goodwe.batteryinverter.statemachine.StateMachine;
import io.openems.edge.goodwe.batteryinverter.statemachine.StateMachine.State;
import io.openems.edge.goodwe.common.AbstractGoodWe;
import io.openems.edge.goodwe.common.ApplyPowerHandler;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.enums.AppModeIndex;
import io.openems.edge.goodwe.common.enums.BatteryProtocol;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EnableCurve;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings.FixedPowerFactor;
import io.openems.edge.goodwe.common.enums.GoodWeType;
import io.openems.edge.goodwe.common.enums.InternalSocProtection;
import io.openems.edge.goodwe.update.GoodWeBatteryInverterUpdateParams;
import io.openems.edge.goodwe.update.GoodWeBatteryInverterUpdateable;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.BatteryInverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE, //
})
public class GoodWeBatteryInverterImpl extends AbstractGoodWe implements GoodWeBatteryInverter, GoodWe,
		HybridManagedSymmetricBatteryInverter, ManagedSymmetricBatteryInverter, SymmetricBatteryInverter,
		ModbusComponent, OpenemsComponent, EventHandler, StartStoppable {

	// Fenecon Home Battery Static module min voltage, used to calculate battery
	// module number per tower
	// TODO get from Battery
	private static final int MODULE_MIN_VOLTAGE = 42;

	private final AtomicReference<StartStop> startStopTarget = new AtomicReference<>(StartStop.UNDEFINED);
	private final Logger log = LoggerFactory.getLogger(GoodWeBatteryInverterImpl.class);
	private final StateMachine stateMachine = new StateMachine(State.UNDEFINED);

	private final ServiceBinder<GoodWeBatteryInverterUpdateParams, GoodWeBatteryInverterUpdateable> updateServiceBinder = new ServiceBinder<>(
			Updateable.class, updateParams -> {
				final var bridge = this.getBridgeModbus();
				if (bridge == null) {
					return null;
				}
				return new GoodWeBatteryInverterUpdateable(bridge, updateParams, this.getGoodweTypeChannel(),
						this.channel(GoodWe.ChannelId.DSP_FM_VERSION_MASTER),
						this.channel(GoodWe.ChannelId.DSP_BETA_VERSION), this.channel(GoodWe.ChannelId.ARM_FM_VERSION),
						this.channel(GoodWe.ChannelId.ARM_BETA_VERSION));
			}, GoodWeBatteryInverterUpdateable::deactivate);

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private ComponentManager componentManager;

	@Reference
	private Power power;

	@Reference
	private Sum sum;

	@Reference
	private SerialNumberStorage serialNumberStorage;

	@Reference
	private Meta meta;

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	protected volatile ControllerEssRippleControlReceiver rcr;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
		this.updateServiceBinder.updateConfiguration();
	}

	@Reference(//
			policy = ReferencePolicy.DYNAMIC, //
			policyOption = ReferencePolicyOption.GREEDY, //
			cardinality = ReferenceCardinality.MULTIPLE //
	)
	private void bindUpdateParams(GoodWeBatteryInverterUpdateParams updateParams) {
		this.updateServiceBinder.bindService(updateParams);
	}

	@SuppressWarnings("unused")
	private void unbindUpdateParams(GoodWeBatteryInverterUpdateParams updateParams) {
		this.updateServiceBinder.unbindService(updateParams);
	}

	/**
	 * We don't want to hold an actual Reference to the {@link Battery} Component,
	 * so we keep the latest data here. Updated in {@link #run(Battery, int, int)}.
	 */
	private BatteryData latestBatteryData = new BatteryData(null, null);

	protected record BatteryData(Integer chargeMaxCurrent, Integer voltage) {
	}

	private Config config = null;

	public GoodWeBatteryInverterImpl() throws OpenemsNamedException {
		super(//
				SymmetricBatteryInverter.ChannelId.ACTIVE_POWER, //
				SymmetricBatteryInverter.ChannelId.REACTIVE_POWER, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_POWER, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_CHARGE_ENERGY, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_ENERGY, //
				OpenemsComponent.ChannelId.values(), //
				ModbusComponent.ChannelId.values(), //
				StartStoppable.ChannelId.values(), //
				SymmetricBatteryInverter.ChannelId.values(), //
				ManagedSymmetricBatteryInverter.ChannelId.values(), //
				HybridManagedSymmetricBatteryInverter.ChannelId.values(), //
				GoodWe.ChannelId.values(), //
				GoodWeBatteryInverter.ChannelId.values() //
		);
		// GoodWe is always started
		this._setStartStop(StartStop.START);

		SymmetricBatteryInverter.calculateApparentPowerFromActiveAndReactivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.serialNumberStorage.createAndAddOnChangeListener(this.channel(GoodWe.ChannelId.SERIAL_NUMBER));

		this.updateServiceBinder.updateBundleContext(context.getBundleContext());

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		this.applyConfigIfNotSet(config, true);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.updateServiceBinder.updateBundleContext(context.getBundleContext());
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		this.applyConfigIfNotSet(config, true);
	}

	@Override
	@Deactivate
	protected void deactivate() {
		super.deactivate();
		this.updateServiceBinder.deactivate();
	}

	@Override
	public void handleEvent(Event event) {
		if (this.config == null || !this.isEnabled()) {
			return;
		}
		super.handleEvent(event);
		switch (event.getTopic()) {
		case EdgeEventConstants.TOPIC_CYCLE_AFTER_PROCESS_IMAGE -> this.handleStateMachine();
		}
	}

	/**
	 * Handles the State-Machine.
	 */
	private void handleStateMachine() {
		// Store the current State
		this.channel(GoodWeBatteryInverter.ChannelId.STATE_MACHINE).setNextValue(this.stateMachine.getCurrentState());

		// Initialize 'Start-Stop' Channel
		this._setStartStop(StartStop.UNDEFINED);

		var context = new Context(this, this.componentManager.getClock());
		// Call the StateMachine
		try {

			this.stateMachine.run(context);

			this.channel(GoodWeBatteryInverter.ChannelId.RUN_FAILED).setNextValue(false);

		} catch (OpenemsNamedException e) {
			this.channel(GoodWeBatteryInverter.ChannelId.RUN_FAILED).setNextValue(true);
			this.logError(this.log, "StateMachine failed: " + e.getMessage());
		}
	}

	@Override
	public void setStartStop(StartStop value) {
		if (this.startStopTarget.getAndSet(value) != value) {
			// Set only if value changed
			this.stateMachine.forceNextState(State.UNDEFINED);
		}
	}

	@Override
	public StartStop getStartStopTarget() {
		return switch (this.config.startStop()) {
		case AUTO -> this.startStopTarget.get();
		case START -> StartStop.START;
		case STOP -> StartStop.STOP;
		};
	}

	/**
	 * Shifts the feed-in power limitation setting from the inverter to the Meta
	 * App.
	 *
	 * <p>
	 * In the future, no feed-in limitation should be configured directly in the
	 * inverter. To avoid inconsistencies caused by having this value defined in
	 * different Controllers (e.g. GridOptimizeCharge and GoodWeBatteryInverterImpl,
	 * also potentially with different values), the setting will be managed globally
	 * within the Meta App. Simply removing the config parameter would lead to
	 * issues for existing systems, as the value would be lost. Therefore, a gradual
	 * migration is necessary. For this, the current value is extracted from the
	 * "feedPowerPara" variable and transferred to the Meta App. As a placeholder,
	 * the value -1 will be written into the inverter's config. In a future version,
	 * this migration logic can then be removed entirely.
	 * </p>
	 *
	 * @param feedPowerPara                   the maximum grid feed in limit.
	 * @param isMaximumGridFeedInLimitEnabled is maximum grid feed in limit enabled?
	 */
	private void migrateFeedPowerParaConfigValue(int feedPowerPara, boolean isMaximumGridFeedInLimitEnabled) {
		try {
			var config = this.cm.getConfiguration(Meta.SINGLETON_SERVICE_PID, "?");
			var properties = config.getProperties();
			var gridFeedInLimitationType = isMaximumGridFeedInLimitEnabled ? GridFeedInLimitationType.DYNAMIC_LIMITATION
					: GridFeedInLimitationType.NO_LIMITATION;

			if (properties.get("maximumGridFeedInLimit") != null
					|| properties.get("gridFeedInLimitationType") != null) {
				// Already migrated, do nothing
				this.log.info(
						"Value is already migrated to Meta App, skipping migration. Set the value in the Meta Component instead.");
				this.resetMaximumSellToGridPower();
				return;
			}

			properties.put("maximumGridFeedInLimit", feedPowerPara);
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
				properties.put("feedPowerPara", -1);
				config.updateIfDifferent(properties);
			} catch (IOException e) {
				this.log.warn("Unable to reset maximumSellToGridPower in GoodWeBatteryInverter", e);
			}
		});
	}

	/**
	 * Apply the configuration on if the values are not already set.
	 *
	 * <p>
	 * Feed In Power Setting consist of: Installed inverter country, feeding method:
	 * whether according to the power factor or power and frequency. In addition, it
	 * consists backup power availability.
	 * </p>
	 *
	 * @param config         Configuration parameters.
	 * @param onConfigUpdate true when called on activate()/modified(), i.e. not in
	 *                       run()
	 *
	 * @throws OpenemsNamedException on error
	 */
	private void applyConfigIfNotSet(Config config, boolean onConfigUpdate) throws OpenemsNamedException {

		// Default Work-Mode
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.SELECT_WORK_MODE), AppModeIndex.SELF_USE);

		// Disable internal Battery/SoC Protection
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.STOP_SOC_PROTECT), InternalSocProtection.DISABLE);

		// country setting
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.SAFETY_COUNTRY_CODE), config.safetyCountry());

		// Backup Power on / off
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.BACK_UP_ENABLE), config.backupEnable().booleanValue);

		// Should be updated according to back up power
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.AUTO_START_BACKUP), config.backupEnable().booleanValue);

		// Feed-in limitation
		if (config.feedPowerPara() != -1) {
			// Moves set value to Meta app.
			// This config is deprecated and needs to be removed in the future.
			this.migrateFeedPowerParaConfigValue(config.feedPowerPara(), config.feedPowerEnable().booleanValue);
		}

		// Set feed in power settings
		var setFeedInPowerSettings = config.setfeedInPowerSettings();
		var quEnableDisable = EnableCurve.DISABLE;
		var puEnableDisable = EnableCurve.DISABLE;
		var cosPhiPEnableDisable = EnableCurve.DISABLE;
		var pfEnableDisable = EnableCurve.DISABLE;
		var fixedPowerFactor = FixedPowerFactor.LEADING_1_OR_NONE;
		var fixedPowerFactorEnable = EnableCurve.DISABLE;

		switch (setFeedInPowerSettings) {

		case UNDEFINED -> doNothing();
		case QU_ENABLE_CURVE -> {
			quEnableDisable = EnableCurve.ENABLE;

			/*
			 * Detailed Q(U) settings like V1_VOLTAGE & V1_VALUE are updated automatically
			 * by GoodWe while setting the country code.
			 */
		}
		case PU_ENABLE_CURVE -> {
			// Not part of the VDE-AR-N 4105 (GERMANY)
			puEnableDisable = EnableCurve.ENABLE;
		}
		case LAGGING_0_80, LAGGING_0_81, LAGGING_0_82, LAGGING_0_83, LAGGING_0_84, LAGGING_0_85, LAGGING_0_86,
				LAGGING_0_87, LAGGING_0_88, LAGGING_0_89, LAGGING_0_90, LAGGING_0_91, LAGGING_0_92, LAGGING_0_93,
				LAGGING_0_94, LAGGING_0_95, LAGGING_0_96, LAGGING_0_97, LAGGING_0_98, LAGGING_0_99, LEADING_0_80,
				LEADING_0_81, LEADING_0_82, LEADING_0_83, LEADING_0_84, LEADING_0_85, LEADING_0_86, LEADING_0_87,
				LEADING_0_88, LEADING_0_89, LEADING_0_90, LEADING_0_91, LEADING_0_92, LEADING_0_93, LEADING_0_94,
				LEADING_0_95, LEADING_0_96, LEADING_0_97, LEADING_0_98, LEADING_0_99, LEADING_1 -> {

			fixedPowerFactor = setFeedInPowerSettings.fixedPowerFactor;
			fixedPowerFactorEnable = EnableCurve.ENABLE;
		}
		case PF_ENABLE_CURVE -> {
			pfEnableDisable = EnableCurve.ENABLE;
			// TODO: Details settings
		}
		case COS_PHI_P_CURVE -> {
			cosPhiPEnableDisable = EnableCurve.ENABLE;
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.A_POINT_POWER), 100); // range -1000,1000: 10%
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.A_POINT_COS_PHI), 100); // -100,100: factor 1
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.B_POINT_POWER), 500); // -1000,1000: 50%
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.B_POINT_COS_PHI), 100); // -100,100: factor 1
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.C_POINT_POWER), 1000); // -1000,1000: 100%
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.C_POINT_COS_PHI), 90); // -100,100: factor 0,9
		}
		}
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.ENABLE_QU_CURVE), quEnableDisable);
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.ENABLE_CURVE_COS_PHI_P), cosPhiPEnableDisable);
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.ENABLE_PU_CURVE), puEnableDisable);
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.ENABLE_PF_CURVE), pfEnableDisable);
		this.handleFixedPowerFactor(this.getGoodweType(), fixedPowerFactorEnable, fixedPowerFactor);

		// Multi-functional Block for Ripple Control Receiver and NA protection on / off
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.DRED_REMOTE_SHUTDOWN_RCR_FUNCTIONS_ENABLE),
				config.rcrEnable().booleanValue || config.naProtectionEnable().booleanValue);

		// Try only once
		if (onConfigUpdate) { //
			// Mppt Shadow enable / disable
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.MPPT_FOR_SHADOW_ENABLE), false);
		}
	}

	private void handleFixedPowerFactor(GoodWeType goodweType, EnableCurve fixedPowerFactorEnable,
			FixedPowerFactor fixedPowerFactor) throws IllegalArgumentException, OpenemsNamedException {

		// TODO: Add individual handling related to each GoodWeType
		switch (goodweType) {
		case FENECON_50K -> {
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.ENABLE_FIXED_POWER_FACTOR_V2), fixedPowerFactorEnable);
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.FIXED_POWER_FACTOR_V2), fixedPowerFactor);
		}
		case FENECON_FHI_10_DAH, FENECON_FHI_20_DAH, FENECON_FHI_29_9_DAH, FENECON_GEN2_10K, FENECON_GEN2_15K,
				FENECON_GEN2_6K, GOODWE_10K_BT, GOODWE_10K_ET, GOODWE_5K_BT, GOODWE_5K_ET, GOODWE_8K_BT,
				GOODWE_8K_ET -> {
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.FIXED_POWER_FACTOR), fixedPowerFactor);
		}
		case UNDEFINED -> doNothing();
		}
	}

	private void handleFeedInSetting(boolean feedPowerEnable, int feedPowerPara, GoodWeType goodweType)
			throws IllegalArgumentException, OpenemsNamedException {

		// TODO: Add individual handling related to each GoodWeType
		switch (goodweType) {
		case FENECON_50K -> {
			// Feed-in limitation on / off
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.EXTENDED_FEED_POWER_ENABLE), feedPowerEnable);
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.FEED_POWER_ENABLE), feedPowerEnable);
			// Feed-in limitation
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.EXTENDED_FEED_POWER_PARA_SET), feedPowerPara);
		}
		case FENECON_FHI_10_DAH, FENECON_FHI_20_DAH, FENECON_FHI_29_9_DAH, FENECON_GEN2_10K, FENECON_GEN2_15K,
				FENECON_GEN2_6K, GOODWE_10K_BT, GOODWE_10K_ET, GOODWE_5K_BT, GOODWE_5K_ET, GOODWE_8K_BT,
				GOODWE_8K_ET -> {
			// Feed-in limitation on / off
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.FEED_POWER_ENABLE), feedPowerEnable);

			// Feed-in limitation
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.FEED_POWER_PARA_SET), feedPowerPara);
		}
		case UNDEFINED -> doNothing();
		}
	}

	/**
	 * Sets the Battery Limits.
	 *
	 * @param battery linked {@link Battery}.
	 * @throws OpenemsNamedException on error
	 */
	private void setBatteryLimits(Battery battery) throws OpenemsNamedException {

		/*
		 * Make sure PV-Master registers are correct, because they define the overall
		 * min/max limits.
		 */
		var bmsChargeMaxCurrent = this.getBmsChargeMaxCurrent();
		var bmsDischargeMaxCurrent = this.getBmsDischargeMaxCurrent();
		var bmsChargeMaxVoltage = this.getBmsChargeMaxVoltage();
		var bmsDischargeMinVoltage = this.getBmsDischargeMinVoltage();

		Channel<Integer> bmsSocUnderMinChannel = this.channel(GoodWe.ChannelId.BMS_SOC_UNDER_MIN);
		var bmsSocUnderMin = bmsSocUnderMinChannel.value();
		Channel<Integer> bmsOfflineSocUnderMinChannel = this.channel(GoodWe.ChannelId.BMS_OFFLINE_SOC_UNDER_MIN);
		var bmsOfflineSocUnderMin = bmsOfflineSocUnderMinChannel.value();

		var setBatteryStrings = TypeUtils.divide(battery.getDischargeMinVoltage().get(), MODULE_MIN_VOLTAGE);
		final int setChargeMaxCurrent;
		final int setDischargeMaxCurrent;
		var setChargeMaxVoltage = battery.getChargeMaxVoltage().orElse(210);
		var setDischargeMinVoltage = battery.getDischargeMinVoltage().orElse(210);
		Integer setSocUnderMin = 0; // [0-100]; 0 MinSoc = 100 DoD
		Integer setOfflineSocUnderMin = 0; // [0-100]; 0 MinSoc = 100 DoD

		if (battery.isStarted() && battery instanceof BatteryFeneconHome homeBattery) {

			setBatteryStrings = homeBattery.getNumberOfModulesPerTower().orElse(setBatteryStrings);

			final var batteryType = homeBattery.getBatteryHardwareType();

			/*
			 * Check combination of GoodWe inverter and FENECON Home battery to avoid
			 * invalid combinations for FENECON Home Systems
			 */
			if (this.getGoodweType().isInvalidBattery.test(batteryType)) {
				this._setImpossibleFeneconHomeCombination(true);

				// Set zero limits to avoid charging and discharging
				setChargeMaxCurrent = 0;
				setDischargeMaxCurrent = 0;
			} else {
				setChargeMaxCurrent = this.getGoodweType().maxDcCurrent.apply(batteryType);
				setDischargeMaxCurrent = this.getGoodweType().maxDcCurrent.apply(batteryType);
				this._setImpossibleFeneconHomeCombination(false);
			}
		} else {
			setChargeMaxCurrent = this.getGoodweType().maxDcCurrent.apply(null);
			setDischargeMaxCurrent = this.getGoodweType().maxDcCurrent.apply(null);
		}

		/*
		 * Check correct BMS register values. Goodwe recommends setting the values once
		 */
		if (bmsChargeMaxCurrent.isDefined() && !Objects.equals(bmsChargeMaxCurrent.get(), setChargeMaxCurrent)
				|| bmsDischargeMaxCurrent.isDefined()
						&& !Objects.equals(bmsDischargeMaxCurrent.get(), setDischargeMaxCurrent)
				|| bmsSocUnderMin.isDefined() && !Objects.equals(bmsSocUnderMin.get(), setSocUnderMin)
				|| bmsOfflineSocUnderMin.isDefined()
						&& !Objects.equals(bmsOfflineSocUnderMin.get(), setOfflineSocUnderMin)) {

			// Update is required
			this.logInfo(this.log, "Update for PV-Master BMS Registers is required." //
					+ " Voltages" //
					+ " [Discharge " + bmsDischargeMinVoltage.get() + " -> " + setDischargeMinVoltage + "]" //
					+ " [Charge " + bmsChargeMaxVoltage.get() + " -> " + setChargeMaxVoltage + "]" //
					+ " Currents " //
					+ " [Charge " + bmsChargeMaxCurrent.get() + " -> " + setChargeMaxCurrent + "]" //
					+ " [Discharge " + bmsDischargeMaxCurrent.get() + " -> " + setDischargeMaxCurrent + "]" //
					+ " MinSoc [" //
					+ " [On-Grid " + bmsSocUnderMin.get() + " -> " + setSocUnderMin + "] " //
					+ " [Off-Grid " + bmsOfflineSocUnderMin.get() + " -> " + setOfflineSocUnderMin + "]");

			// Registers 45352
			this.writeToChannel(GoodWe.ChannelId.BMS_CHARGE_MAX_VOLTAGE, setChargeMaxVoltage); // [150-600]
			this.writeToChannel(GoodWe.ChannelId.BMS_CHARGE_MAX_CURRENT, setChargeMaxCurrent); // [0-100]
			this.writeToChannel(GoodWe.ChannelId.BMS_DISCHARGE_MIN_VOLTAGE, setDischargeMinVoltage); // [150-600]
			this.writeToChannel(GoodWe.ChannelId.BMS_DISCHARGE_MAX_CURRENT, setDischargeMaxCurrent); // [0-100]
			this.writeToChannel(GoodWe.ChannelId.BMS_SOC_UNDER_MIN, setSocUnderMin);
			this.writeToChannel(GoodWe.ChannelId.BMS_OFFLINE_DISCHARGE_MIN_VOLTAGE, setDischargeMinVoltage); // [150-600]
			this.writeToChannel(GoodWe.ChannelId.BMS_OFFLINE_SOC_UNDER_MIN, setOfflineSocUnderMin);
		}

		/*
		 * Check correct BMS register value for voltage. Handled separately to avoid
		 * sending other values multiple times if the voltage registers are not written
		 * immediately
		 */
		if (doSetBmsVoltage(battery, bmsChargeMaxVoltage, setChargeMaxVoltage, bmsDischargeMinVoltage,
				setDischargeMinVoltage)) {
			// Update is required
			this.logInfo(this.log, "Update for BMS Registers." //
					+ " Voltages" //
					+ " [Discharge " + bmsDischargeMinVoltage.get() + " -> " + setDischargeMinVoltage + "]" //
					+ " [Charge " + bmsChargeMaxVoltage.get() + " -> " + setChargeMaxVoltage
					+ "]. This can take up to 10 minutes.");

			this.writeToChannel(GoodWe.ChannelId.BMS_CHARGE_MAX_VOLTAGE, setChargeMaxVoltage);
			this.writeToChannel(GoodWe.ChannelId.BMS_DISCHARGE_MIN_VOLTAGE, setDischargeMinVoltage);
		}

		/*
		 * Regularly write all WBMS Channels.
		 */
		this.writeToChannel(GoodWe.ChannelId.WBMS_VERSION, 1);
		this.writeToChannel(GoodWe.ChannelId.WBMS_STRINGS, setBatteryStrings); // numberOfModulesPerTower
		// TODO is writing WBMS_STRINGS still required with latest firmware?
		this.writeToChannel(GoodWe.ChannelId.WBMS_CHARGE_MAX_VOLTAGE, battery.getChargeMaxVoltage().orElse(0));
		this.writeToChannel(GoodWe.ChannelId.WBMS_CHARGE_MAX_CURRENT,

				preprocessAmpereValue47900(battery.getChargeMaxCurrent(), setChargeMaxCurrent));
		this.writeToChannel(GoodWe.ChannelId.WBMS_DISCHARGE_MIN_VOLTAGE, battery.getDischargeMinVoltage().orElse(0));
		this.writeToChannel(GoodWe.ChannelId.WBMS_DISCHARGE_MAX_CURRENT,
				preprocessAmpereValue47900(battery.getDischargeMaxCurrent(), setChargeMaxCurrent));
		this.writeToChannel(GoodWe.ChannelId.WBMS_VOLTAGE, battery.getVoltage().orElse(0));
		this.writeToChannel(GoodWe.ChannelId.WBMS_CURRENT, TypeUtils.abs(battery.getCurrent().orElse(0)));

		// Set SoC within [1;100] to avoid force-charge internally by PCS at 0 %
		this.writeToChannel(GoodWe.ChannelId.WBMS_SOC, TypeUtils.fitWithin(1, 100, battery.getSoc().orElse(1)));
		this.writeToChannel(GoodWe.ChannelId.WBMS_SOH, battery.getSoh().orElse(100));

		// Average Min/Max Cell Temperature; defaults to 0
		this.writeToChannel(GoodWe.ChannelId.WBMS_TEMPERATURE, //
				TypeUtils.orElse(//
						TypeUtils.averageRounded(//
								battery.getMaxCellTemperature().get(), battery.getMinCellTemperature().get()),
						0));

		this.writeToChannel(GoodWe.ChannelId.WBMS_WARNING_CODE, 0);
		this.writeToChannel(GoodWe.ChannelId.WBMS_ALARM_CODE, 0);
		this.writeToChannel(GoodWe.ChannelId.WBMS_STATUS, 0);
		this.writeToChannel(GoodWe.ChannelId.WBMS_DISABLE_TIMEOUT_DETECTION, 0);
	}

	protected static boolean doSetBmsVoltage(Battery battery, Value<Integer> bmsChargeMaxVoltage,
			Integer setChargeMaxVoltage, Value<Integer> bmsDischargeMinVoltage, Integer setDischargeMinVoltage) {
		if (!battery.getChargeMaxCurrent().isDefined() || !battery.getDischargeMaxCurrent().isDefined()
				|| !bmsChargeMaxVoltage.isDefined() || !bmsChargeMaxVoltage.isDefined()) {
			// Do not set channels if input data is missing/not yet available
			return false;
		}
		if (battery.getChargeMaxCurrent().get() == 0 || battery.getDischargeMaxCurrent().get() == 0) {
			// Exclude times with full or empty battery, due to inverter mis-behaviour
			return false;
		}
		if (Objects.equals(bmsChargeMaxVoltage.get(), setChargeMaxVoltage)
				&& Objects.equals(bmsDischargeMinVoltage.get(), setDischargeMinVoltage)) {
			// Values are already set
			return false;
		}
		return true;
	}

	/**
	 * Set general values.
	 *
	 * @throws IllegalArgumentException on error
	 * @throws OpenemsNamedException    on error
	 */
	private void setGeneralValues() throws IllegalArgumentException, OpenemsNamedException {

		// Set BatteryProtocols only once, as the WBMS Channels are reset afterwards
		if (!this.getBatteryProtocolArm().equals(BatteryProtocol.EMS_USE)) {
			this.writeToChannel(GoodWe.ChannelId.BATTERY_PROTOCOL_ARM, BatteryProtocol.EMS_USE); // EMS-Mode 287/11F
		}

		/*
		 * Set goodwe force charge and end SoC if not already set
		 */
		if (!Objects.equals(this.getSocStartToForceCharge().get(), 0)) {
			this.writeToChannel(GoodWe.ChannelId.SOC_START_TO_FORCE_CHARGE, 0);
		}
		if (!Objects.equals(this.getSocStopToForceCharge().get(), 0)) {
			this.writeToChannel(GoodWe.ChannelId.SOC_STOP_TO_FORCE_CHARGE, 0);
		}
	}

	protected static int preprocessAmpereValue47900(Value<Integer> v, int maxDcCurrent) {
		return TypeUtils.fitWithin(0, maxDcCurrent, v.orElse(0));
	}

	private void writeToChannel(GoodWe.ChannelId channelId, OptionsEnum value)
			throws IllegalArgumentException, OpenemsNamedException {
		EnumWriteChannel channel = this.channel(channelId);
		channel.setNextWriteValue(value);
	}

	private void writeToChannel(GoodWe.ChannelId channelId, Integer value)
			throws IllegalArgumentException, OpenemsNamedException {
		IntegerWriteChannel channel = this.channel(channelId);
		channel.setNextWriteValue(value);
	}

	@Override
	public int getPowerPrecision() {
		return 1;
	}

	@Override
	public Timedata getTimedata() {
		return this.timedata;
	}

	@Override
	public Integer getDcPvPower() {
		return this.calculatePvProduction();
	}

	@Override
	public Integer getSurplusPower() {
		var productionPower = this.calculatePvProduction();
		return calculateSurplusPower(this.latestBatteryData, productionPower,
				this.getGoodweType().maxDcCurrent.apply(null));

	}

	protected static Integer calculateSurplusPower(BatteryData battery, Integer productionPower, int maxDcCurrent) {
		if (battery.chargeMaxCurrent == null /* Charge Max Current is not available */
				|| battery.chargeMaxCurrent >= maxDcCurrent /* Charge Max Current is higher than inverter limit */
				|| battery.chargeMaxCurrent < 0 /* Battery is in Force-Discharge mode */

				|| battery.voltage == null /* Battery Voltage is not available */
				|| battery.voltage < 0 /* Battery Voltage is negative */

				|| productionPower == null /* Production Power is not available */
				|| productionPower <= 0 /* Production Power is zero or negative */
		) {
			return null;
		}

		// Reduce PV Production power by DC max charge power
		var surplusPower = productionPower //
				- TypeUtils.orElse(TypeUtils.multiply(battery.chargeMaxCurrent, battery.voltage), 0);

		if (surplusPower <= 0) {
			// PV Production is less than the maximum charge power -> no surplus power
			return null;
		}

		// Surplus power is always positive here
		return surplusPower;
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {

		// ApplyConfig
		this.applyConfigIfNotSet(this.config, false);

		// Calculate ActivePower, Energy and Max-AC-Power.
		this.updatePowerAndEnergyChannels(battery.getSoc().get(), battery.getCurrent().get());
		this.handleMaxAcPower(this.getMaxApparentPower().orElse(0));

		this.handleGridFeed(this.config, this.meta.getGridFeedInLimitationType());

		this.latestBatteryData = new BatteryData(battery.getChargeMaxCurrent().get(), battery.getVoltage().get());

		// Apply Power Set-Point
		ApplyPowerHandler.apply(this, setActivePower, this.config.controlMode(), this.sum.getGridActivePower(),
				this.getActivePower(), this.getMaxAcImport(), this.getMaxAcExport(), this.power.isPidEnabled());

		// Set Battery Limits
		this.setBatteryLimits(battery);

		// Set General Values
		this.setGeneralValues();
	}

	@Override
	public BatteryInverterConstraint[] getStaticConstraints() throws OpenemsNamedException {
		return new BatteryInverterConstraint[] { //
				new BatteryInverterConstraint("Max AC Import", ALL, ACTIVE, GREATER_OR_EQUALS,
						this.getMaxAcImport().orElse(0)), //
				new BatteryInverterConstraint("Max AC Export", ALL, ACTIVE, LESS_OR_EQUALS,
						this.getMaxAcExport().orElse(0)) //
		};
	}

	@Override
	public String debugLog() {
		return "AllowedAC:" + this.getMaxAcImport().asStringWithoutUnit() + ";" + this.getMaxAcExport().asString();
	}

	@Override
	public boolean isManaged() {
		return !this.config.controlMode().equals(ControlMode.INTERNAL);
	}

	@Override
	public boolean isOffGridPossible() {
		return this.config.backupEnable().equals(EnableDisable.ENABLE);
	}

	private void handleGridFeed(Config config, GridFeedInLimitationType limitType) throws OpenemsNamedException {

		if (!this.getMaxApparentPower().isDefined()) {
			return;
		}

		var maxApparentPower = this.getMaxApparentPower().get();
		var enableFeedInLimit = false;
		var gridFeedInLimit = maxApparentPower;

		// Limit from general Feed-In Limitation
		if (limitType == GridFeedInLimitationType.DYNAMIC_LIMITATION) {
			enableFeedInLimit = true;
			gridFeedInLimit = this.meta.getMaximumGridFeedInLimitValue().orElse(maxApparentPower);
		}

		// Limit from Ripple Control Receiver (Minimum of both limits)
		if (this.rcr != null && this.rcr.isEnabled()) {
			enableFeedInLimit = true;
			gridFeedInLimit = this.rcr.getDynamicGridFeedInLimit(maxApparentPower);
		}

		this.handleFeedInSetting(enableFeedInLimit, gridFeedInLimit, this.getGoodweType());
	}
}
