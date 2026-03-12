package io.openems.edge.goodwe.batteryinverter;

import static io.openems.common.utils.FunctionUtils.doNothing;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_2;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_1;
import static io.openems.edge.bridge.modbus.api.ElementToChannelConverter.SCALE_FACTOR_MINUS_2;
import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;
import static io.openems.edge.common.type.Phase.SingleOrAllPhase.ALL;
import static io.openems.edge.ess.power.api.Pwr.ACTIVE;
import static io.openems.edge.ess.power.api.Relationship.GREATER_OR_EQUALS;
import static io.openems.edge.ess.power.api.Relationship.LESS_OR_EQUALS;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
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
import io.openems.edge.bridge.modbus.api.ChannelMetaInfoReadAndWrite;
import io.openems.edge.bridge.modbus.api.ElementToChannelConverter;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.api.element.DummyRegisterElement;
import io.openems.edge.bridge.modbus.api.element.SignedWordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedDoublewordElement;
import io.openems.edge.bridge.modbus.api.element.UnsignedWordElement;
import io.openems.edge.bridge.modbus.api.task.FC16WriteRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC3ReadRegistersTask;
import io.openems.edge.bridge.modbus.api.task.FC6WriteRegisterTask;
import io.openems.edge.bridge.modbus.api.task.Task;
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
import io.openems.edge.common.taskmanager.Priority;
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
import io.openems.edge.goodwe.common.GoodWePowerSetting;
import io.openems.edge.goodwe.common.enums.AppModeIndex;
import io.openems.edge.goodwe.common.enums.BatteryProtocol;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EnableCurve;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.goodwe.common.enums.FeedInPowerSettings.FixedPowerFactor;
import io.openems.edge.goodwe.common.enums.GoodWeType;
import io.openems.edge.goodwe.common.enums.GridCode;
import io.openems.edge.goodwe.common.enums.InternalSocProtection;
import io.openems.edge.goodwe.common.enums.SafetyCountry;
import io.openems.edge.goodwe.update.GoodWeBatteryInverterUpdateParams;
import io.openems.edge.goodwe.update.GoodWeBatteryInverterUpdateable;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.BatteryInverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE)
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

	private List<Task> safetyParameterSettingsTasks = Collections.emptyList();

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
				GoodWeBatteryInverter.ChannelId.values(), //
				GoodWePowerSetting.ChannelId.values() //
		);
		// GoodWe is always started
		this._setStartStop(StartStop.START);

		SymmetricBatteryInverter.calculateApparentPowerFromActiveAndReactivePower(this);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		this.serialNumberStorage.createAndAddOnChangeListener(this.channel(GoodWe.ChannelId.SERIAL_NUMBER));

		this.updateServiceBinder.updateBundleContext(context.getBundleContext());

		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfigIfNotSet(config, true);
		this.addPowerSettingTasks();
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		this.config = config;
		this.updateServiceBinder.updateBundleContext(context.getBundleContext());
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfigIfNotSet(config, true);
		this.addPowerSettingTasks();
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
		SafetyCountry safetyCountryCode = config.gridCode() == GridCode.VDE_4110 //
				? SafetyCountry.GERMANY_VDE_4110 //
				: config.safetyCountry();
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.SAFETY_COUNTRY_CODE), safetyCountryCode);

		// Backup Power on / off
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.BACK_UP_ENABLE), config.backupEnable().booleanValue);

		// Should be updated according to back up power
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.AUTO_START_BACKUP), config.backupEnable().booleanValue);

		// Power settings
		this.setPowerSettings();

		// Feed-in limitation
		if (config.feedPowerPara() != -1) {
			// Moves set value to Meta app.
			// This config is deprecated and needs to be removed in the future.
			this.migrateFeedPowerParaConfigValue(config.feedPowerPara(), config.feedPowerEnable().booleanValue);
		}

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
		case FENECON_50K, FENECON_100K -> {
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

	private void handlePfChannels(GoodWeType goodWeType) throws OpenemsNamedException {
		EnumWriteChannel pfUnderfrequencyChannel = this
				.channel((GoodWePowerSetting.ChannelId.V2_APM_ENABLE_PF_UNDERFREQUENZY_CURVE));
		pfUnderfrequencyChannel.setNextWriteValue(EnableCurve.DISABLE);
		EnumWriteChannel pfOverfrequencyChannel = this
				.channel(GoodWePowerSetting.ChannelId.V2_APM_ENABLE_PF_OVERFREQUENZY_CURVE);
		pfOverfrequencyChannel.setNextWriteValue(EnableCurve.DISABLE);
	}

	private void handleFeedInSetting(boolean feedPowerEnable, int feedPowerPara, GoodWeType goodweType)
			throws IllegalArgumentException, OpenemsNamedException {

		// TODO: Add individual handling related to each GoodWeType
		switch (goodweType) {
		case FENECON_50K, FENECON_100K -> {
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
					+ " MinSoc " //
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
				preprocessAmpereValue47900(battery.getDischargeMaxCurrent(), setDischargeMaxCurrent));
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
		this.handleMaxAcPower(this.getMaxApparentPower().orElse(0), battery);

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
		var gridSellHardLimit = this.meta.getGridSellHardLimit();
		if (gridSellHardLimit < maxApparentPower) {
			enableFeedInLimit = true;
			gridFeedInLimit = gridSellHardLimit;
		}

		// Limit from Ripple Control Receiver (Minimum of both limits)
		if (this.rcr != null && this.rcr.isEnabled()) {
			enableFeedInLimit = true;
			gridFeedInLimit = Math.min(gridFeedInLimit, this.rcr.getDynamicGridFeedInLimit(maxApparentPower));
		}

		this.handleFeedInSetting(enableFeedInLimit, gridFeedInLimit, this.getGoodweType());
	}

	private void setPowerSettings() throws OpenemsNamedException {
		switch (this.config.gridCode()) {
		case VDE_4105 -> this.setPowerSettingsFor4105();
		case VDE_4110 -> this.setPowerSettingsFor4110();
		case UNDEFINED -> doNothing();
		}
	}

	private void setPowerSettingsFor4105() throws OpenemsNamedException {
		var setFeedInPowerSettings = this.config.setfeedInPowerSettings();
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
		if (this.getGoodweType() == GoodWeType.FENECON_50K) {
			this.handlePfChannels(this.getGoodweType());
		}
	}

	private void setPowerSettingsFor4110() {
		var handler = new PowerSettingHandler(this);
		handler.handlePowerSetting(this.config);
	}

	/**
	 * Get the power settings tasks of the inverter.
	 *
	 * <p>
	 * A lot of individual power settings can be configured for each inverter. These
	 * power settings are mapped here.
	 *
	 * @return a list of {@link Task}
	 */
	private List<Task> getDefaultPowerSettingsTasks() {

		return List.of(//
				new FC3ReadRegistersTask(45400, Priority.LOW, //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1, new UnsignedWordElement(45400), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1_TIME, new UnsignedWordElement(45401)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1, new UnsignedWordElement(45402), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1_TIME, new UnsignedWordElement(45403)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2, new UnsignedWordElement(45404), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2_TIME, new UnsignedWordElement(45405)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2, new UnsignedWordElement(45406), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2_TIME, new UnsignedWordElement(45407)), //
						m(GoodWe.ChannelId.GRID_VOLT_QUALITY, new UnsignedWordElement(45408), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1, new UnsignedWordElement(45409), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1_TIME, new UnsignedWordElement(45410)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1, new UnsignedWordElement(45411), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1_TIME, new UnsignedWordElement(45412)), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2, new UnsignedWordElement(45413), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2_TIME, new UnsignedWordElement(45414)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2, new UnsignedWordElement(45415), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2_TIME, new UnsignedWordElement(45416)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH, new UnsignedWordElement(45417), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW, new UnsignedWordElement(45418), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH, new UnsignedWordElement(45419), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW, new UnsignedWordElement(45420), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_RECOVER_TIME, new UnsignedWordElement(45421)), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_HIGH, new UnsignedWordElement(45422),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_LOW, new UnsignedWordElement(45423), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_HIGH, new UnsignedWordElement(45424),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_LOW, new UnsignedWordElement(45425), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_TIME, new UnsignedWordElement(45426)), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_TIME, new UnsignedWordElement(45427)), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_GENERATE, new UnsignedWordElement(45428),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_RECONNECT, new UnsignedWordElement(45429),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_REDUCTION, new UnsignedWordElement(45430),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_PROTECT, new UnsignedWordElement(45431)), //

						// Cos Phi Curve
						m(GoodWe.ChannelId.ENABLE_POWER_SLOPE_COS_PHI_P, new UnsignedWordElement(45432)), //
						m(GoodWe.ChannelId.ENABLE_CURVE_COS_PHI_P, new UnsignedWordElement(45433)), //
						m(GoodWe.ChannelId.A_POINT_POWER, new SignedWordElement(45434)), //
						m(GoodWe.ChannelId.A_POINT_COS_PHI, new SignedWordElement(45435), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.B_POINT_POWER, new SignedWordElement(45436)), //
						m(GoodWe.ChannelId.B_POINT_COS_PHI, new SignedWordElement(45437), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_POINT_POWER, new SignedWordElement(45438)), //
						m(GoodWe.ChannelId.C_POINT_COS_PHI, new SignedWordElement(45439)),
						m(GoodWe.ChannelId.LOCK_IN_VOLTAGE, new UnsignedWordElement(45440), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LOCK_OUT_VOLTAGE, new UnsignedWordElement(45441), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER, new SignedWordElement(45442)), //

						// Power and frequency curve (PF)
						m(GoodWe.ChannelId.ENABLE_PF_CURVE, new UnsignedWordElement(45443)), //
						m(GoodWe.ChannelId.FFROZEN_DCH, new UnsignedWordElement(45444), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FFROZEN_CH, new UnsignedWordElement(45445), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_DCH, new UnsignedWordElement(45446), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_CH, new UnsignedWordElement(45447), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.OF_RECOVERY_WAITING_TIME, new UnsignedWordElement(45448),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_FREQURNCY1, new UnsignedWordElement(45449), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_FREQUENCY2, new UnsignedWordElement(45450), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.OF_RECOVERY_SLOPE, new UnsignedWordElement(45451), //
								new ChannelMetaInfoReadAndWrite(45451, 45452)), //
						m(GoodWe.ChannelId.CFP_SETTINGS, new UnsignedWordElement(45452), //
								new ChannelMetaInfoReadAndWrite(45452, 45451)), //
						m(GoodWe.ChannelId.CFP_OF_SLOPE_PERCENT, new UnsignedWordElement(45453), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CFP_UF_SLOPE_PERCENT, new UnsignedWordElement(45454), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CFP_OF_RECOVER_POWER_PERCENT, new UnsignedWordElement(45455)), //

						// QU Curve
						m(GoodWe.ChannelId.ENABLE_QU_CURVE, new UnsignedWordElement(45456)), //
						m(GoodWe.ChannelId.LOCK_IN_POWER_QU, new SignedWordElement(45457)), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER_QU, new SignedWordElement(45458)), //
						m(GoodWe.ChannelId.V1_VOLTAGE, new UnsignedWordElement(45459), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V1_VALUE, new UnsignedWordElement(45460)), //
						m(GoodWe.ChannelId.V2_VOLTAGE, new UnsignedWordElement(45461), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE, new UnsignedWordElement(45462)), //
						m(GoodWe.ChannelId.V3_VOLTAGE, new UnsignedWordElement(45463), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE, new UnsignedWordElement(45464)), //
						m(GoodWe.ChannelId.V4_VOLTAGE, new UnsignedWordElement(45465), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE, new SignedWordElement(45466)), //
						m(GoodWe.ChannelId.K_VALUE, new UnsignedWordElement(45467)), //
						m(GoodWe.ChannelId.TIME_CONSTANT, new UnsignedWordElement(45468)), //
						m(GoodWe.ChannelId.MISCELLANEA, new UnsignedWordElement(45469)), //
						new DummyRegisterElement(45470, 45471), //

						// PU Curve
						m(GoodWe.ChannelId.ENABLE_PU_CURVE, new UnsignedWordElement(45472)), //
						m(GoodWe.ChannelId.POWER_CHANGE_RATE, new UnsignedWordElement(45473)), // [0, 1200] s
						m(GoodWe.ChannelId.V1_VOLTAGE_PU, new UnsignedWordElement(45474), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V1_VALUE_PU, new SignedWordElement(45475), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VOLTAGE_PU, new UnsignedWordElement(45476), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE_PU, new SignedWordElement(45477), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VOLTAGE_PU, new UnsignedWordElement(45478), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE_PU, new SignedWordElement(45479), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VOLTAGE_PU, new UnsignedWordElement(45480), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE_PU, new SignedWordElement(45481), SCALE_FACTOR_MINUS_1), //

						// Fix Pf (80=Pf 0.8, 20= -0.8Pf)
						m(GoodWe.ChannelId.FIXED_POWER_FACTOR, new UnsignedWordElement(45482)), //
						// Set the percentage of rated power of the inverter
						m(GoodWe.ChannelId.FIXED_REACTIVE_POWER, new SignedWordElement(45483), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.FIXED_ACTIVE_POWER, new UnsignedWordElement(45484), SCALE_FACTOR_MINUS_1), //
						new DummyRegisterElement(45485, 45487), //
						m(GoodWe.ChannelId.AUTO_TEST_ENABLE, new UnsignedWordElement(45488)), //
						m(GoodWe.ChannelId.AUTO_TEST_STEP, new UnsignedWordElement(45489)), //
						m(GoodWe.ChannelId.UW_ITALY_FREQ_MODE, new UnsignedWordElement(45490)), //
						// this must be turned off to do Meter test . "1" means Off
						m(GoodWe.ChannelId.ALL_POWER_CURVE_DISABLE, new UnsignedWordElement(45491)), //
						m(GoodWe.ChannelId.R_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45492)), //
						m(GoodWe.ChannelId.S_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45493)), //
						m(GoodWe.ChannelId.T_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45494)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3, new UnsignedWordElement(45495), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3_TIME, new UnsignedWordElement(45496)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3, new UnsignedWordElement(45497), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3_TIME, new UnsignedWordElement(45498)), //
						m(GoodWe.ChannelId.ZVRT_CONFIG, new UnsignedWordElement(45499)), //
						m(GoodWe.ChannelId.LVRT_START_VOLT, new UnsignedWordElement(45500), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_END_VOLT, new UnsignedWordElement(45501), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_START_TRIP_TIME, new UnsignedWordElement(45502)), //
						m(GoodWe.ChannelId.LVRT_END_TRIP_TIME, new UnsignedWordElement(45503)), //
						m(GoodWe.ChannelId.LVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45504), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_VOLT, new UnsignedWordElement(45505), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_END_VOLT, new UnsignedWordElement(45506), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_TRIP_TIME, new UnsignedWordElement(45507)), //
						m(GoodWe.ChannelId.HVRT_END_TRIP_TIME, new UnsignedWordElement(45508)), //
						m(GoodWe.ChannelId.HVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45509), SCALE_FACTOR_MINUS_1), //

						// Additional settings for PF/PU/UF
						m(GoodWe.ChannelId.PF_TIME_CONSTANT, new UnsignedWordElement(45510)), //
						m(GoodWe.ChannelId.POWER_FREQ_TIME_CONSTANT, new UnsignedWordElement(45511)), //
						// Additional settings for P(U) Curve
						m(GoodWe.ChannelId.PU_TIME_CONSTANT, new UnsignedWordElement(45512)), //
						m(GoodWe.ChannelId.D_POINT_POWER, new SignedWordElement(45513)), //
						m(GoodWe.ChannelId.D_POINT_COS_PHI, new SignedWordElement(45514)), //
						// Additional settings for UF Curve
						m(GoodWe.ChannelId.UF_RECOVERY_WAITING_TIME, new UnsignedWordElement(45515),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.UF_RECOVER_SLOPE, new UnsignedWordElement(45516)), //
						m(GoodWe.ChannelId.CFP_UF_RECOVER_POWER_PERCENT, new UnsignedWordElement(45517)), //
						m(GoodWe.ChannelId.POWER_CHARGE_LIMIT, new UnsignedWordElement(45518), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_CHARGE_LIMIT_RECONNECT, new UnsignedWordElement(45519),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_UF_CHARGE_STOP, new UnsignedWordElement(45520), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_OF_DISCHARGE_STOP, new UnsignedWordElement(45521),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_TWOSSTEPF_FLG, new UnsignedWordElement(45522))), //
				new FC16WriteRegistersTask(45400, //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1, new UnsignedWordElement(45400), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S1_TIME, new UnsignedWordElement(45401)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1, new UnsignedWordElement(45402), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S1_TIME, new UnsignedWordElement(45403)), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2, new UnsignedWordElement(45404), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S2_TIME, new UnsignedWordElement(45405)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2, new UnsignedWordElement(45406), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S2_TIME, new UnsignedWordElement(45407)), //
						m(GoodWe.ChannelId.GRID_VOLT_QUALITY, new UnsignedWordElement(45408), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1, new UnsignedWordElement(45409), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S1_TIME, new UnsignedWordElement(45410)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1, new UnsignedWordElement(45411), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S1_TIME, new UnsignedWordElement(45412)), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2, new UnsignedWordElement(45413), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH_S2_TIME, new UnsignedWordElement(45414)), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2, new UnsignedWordElement(45415), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW_S2_TIME, new UnsignedWordElement(45416)), //
						// Connect voltage
						m(GoodWe.ChannelId.GRID_VOLT_HIGH, new UnsignedWordElement(45417), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW, new UnsignedWordElement(45418), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_HIGH, new UnsignedWordElement(45419), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_LOW, new UnsignedWordElement(45420), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_RECOVER_TIME, new UnsignedWordElement(45421)), //
						// Reconnect voltage
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_HIGH, new UnsignedWordElement(45422),
								SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_LOW, new UnsignedWordElement(45423), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_HIGH, new UnsignedWordElement(45424),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_LOW, new UnsignedWordElement(45425), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_VOLT_RECOVER_TIME, new UnsignedWordElement(45426)), //
						m(GoodWe.ChannelId.GRID_FREQ_RECOVER_TIME, new UnsignedWordElement(45427)), //
						// Power rate limit
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_GENERATE, new UnsignedWordElement(45428),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_RECONNECT, new UnsignedWordElement(45429),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_RATE_LIMIT_REDUCTION, new UnsignedWordElement(45430),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.GRID_PROTECT, new UnsignedWordElement(45431)), //
						m(GoodWe.ChannelId.ENABLE_POWER_SLOPE_COS_PHI_P, new UnsignedWordElement(45432)), //

						// Cos Phi Curve
						m(GoodWe.ChannelId.ENABLE_CURVE_COS_PHI_P, new UnsignedWordElement(45433)), //
						m(GoodWe.ChannelId.A_POINT_POWER, new SignedWordElement(45434)), //
						m(GoodWe.ChannelId.A_POINT_COS_PHI, new SignedWordElement(45435)), //
						m(GoodWe.ChannelId.B_POINT_POWER, new SignedWordElement(45436)), //
						m(GoodWe.ChannelId.B_POINT_COS_PHI, new SignedWordElement(45437)), //
						m(GoodWe.ChannelId.C_POINT_POWER, new SignedWordElement(45438)), //
						m(GoodWe.ChannelId.C_POINT_COS_PHI, new SignedWordElement(45439)), //
						// [600, 3000]
						m(GoodWe.ChannelId.LOCK_IN_VOLTAGE, new UnsignedWordElement(45440), SCALE_FACTOR_MINUS_1), //
						// [600, 3000]
						m(GoodWe.ChannelId.LOCK_OUT_VOLTAGE, new UnsignedWordElement(45441), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER, new SignedWordElement(45442)), //

						// Power and frequency curve (PF)
						m(GoodWe.ChannelId.ENABLE_PF_CURVE, new UnsignedWordElement(45443)), //
						// GW is not supporting Coils (POWER_FREQUENCY_RESPONSE_MODE will be set by
						// default to slope (bit1: response mode, 1: fstop, 0: slope))

						m(GoodWe.ChannelId.FFROZEN_DCH, new UnsignedWordElement(45444), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FFROZEN_CH, new UnsignedWordElement(45445), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_DCH, new UnsignedWordElement(45446), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.FSTOP_CH, new UnsignedWordElement(45447), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_WAITING_TIME, new UnsignedWordElement(45448)), //
						m(GoodWe.ChannelId.RECOVERY_FREQURNCY1, new UnsignedWordElement(45449), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.RECOVERY_FREQUENCY2, new UnsignedWordElement(45450), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CFP_SETTINGS, new UnsignedWordElement(45451), //
								new ChannelMetaInfoReadAndWrite(45452, 45451)), //
						m(GoodWe.ChannelId.OF_RECOVERY_SLOPE, new UnsignedWordElement(45452), //
								new ChannelMetaInfoReadAndWrite(45451, 45452)), //
						m(GoodWe.ChannelId.CFP_OF_SLOPE_PERCENT, new UnsignedWordElement(45453), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CFP_UF_SLOPE_PERCENT, new UnsignedWordElement(45454), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.CFP_OF_RECOVER_POWER_PERCENT, new UnsignedWordElement(45455)), //

						// QU Curve
						m(GoodWe.ChannelId.ENABLE_QU_CURVE, new UnsignedWordElement(45456)), //
						m(GoodWe.ChannelId.LOCK_IN_POWER_QU, new SignedWordElement(45457)), //
						m(GoodWe.ChannelId.LOCK_OUT_POWER_QU, new SignedWordElement(45458)), //
						m(GoodWe.ChannelId.V1_VOLTAGE, new UnsignedWordElement(45459), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V1_VALUE, new UnsignedWordElement(45460)), //
						m(GoodWe.ChannelId.V2_VOLTAGE, new UnsignedWordElement(45461), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE, new UnsignedWordElement(45462)), //
						m(GoodWe.ChannelId.V3_VOLTAGE, new UnsignedWordElement(45463), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE, new UnsignedWordElement(45464)), //
						m(GoodWe.ChannelId.V4_VOLTAGE, new UnsignedWordElement(45465), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE, new SignedWordElement(45466)), //
						m(GoodWe.ChannelId.K_VALUE, new UnsignedWordElement(45467)), //
						m(GoodWe.ChannelId.TIME_CONSTANT, new UnsignedWordElement(45468)), //
						m(GoodWe.ChannelId.MISCELLANEA, new UnsignedWordElement(45469)), //

						new DummyRegisterElement(45470, 45471), //

						// PU Curve
						m(GoodWe.ChannelId.ENABLE_PU_CURVE, new UnsignedWordElement(45472)),
						m(GoodWe.ChannelId.POWER_CHANGE_RATE, new UnsignedWordElement(45473), SCALE_FACTOR_MINUS_2), // General
						m(GoodWe.ChannelId.V1_VOLTAGE_PU, new UnsignedWordElement(45474), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V1_VALUE_PU, new SignedWordElement(45475)), //
						m(GoodWe.ChannelId.V2_VOLTAGE_PU, new UnsignedWordElement(45476), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V2_VALUE_PU, new SignedWordElement(45477)), //
						m(GoodWe.ChannelId.V3_VOLTAGE_PU, new UnsignedWordElement(45478), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V3_VALUE_PU, new SignedWordElement(45479)), //
						m(GoodWe.ChannelId.V4_VOLTAGE_PU, new UnsignedWordElement(45480), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.V4_VALUE_PU, new SignedWordElement(45481)), //
						// 80=Pf 0.8, 20= -0.8Pf
						m(GoodWe.ChannelId.FIXED_POWER_FACTOR, new UnsignedWordElement(45482)), // [0,20]||[80,100]
						// Set the percentage of rated power of the inverter
						m(GoodWe.ChannelId.FIXED_REACTIVE_POWER, new SignedWordElement(45483)),
						m(GoodWe.ChannelId.FIXED_ACTIVE_POWER, new UnsignedWordElement(45484)),
						new DummyRegisterElement(45485, 45490), //
						// This must be turned off to do Meter test . "1" means Off
						m(GoodWe.ChannelId.ALL_POWER_CURVE_DISABLE, new UnsignedWordElement(45491)), //
						// if it is 1-phase inverter, then use only R phase. Unbalance output function
						// must be turned on to set different values for R/S/T phases
						m(GoodWe.ChannelId.R_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45492)), //
						m(GoodWe.ChannelId.S_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45493)), //
						m(GoodWe.ChannelId.T_PHASE_FIXED_ACTIVE_POWER, new UnsignedWordElement(45494)), //
						// only for countries where it needs 3-stage grid voltage
						// protection, Eg. Czech Republic
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3, new UnsignedWordElement(45495), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_HIGH_S3_TIME, new UnsignedWordElement(45496)), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3, new UnsignedWordElement(45497), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.GRID_VOLT_LOW_S3_TIME, new UnsignedWordElement(45498)), //

						// For ZVRT, LVRT, HVRT
						m(GoodWe.ChannelId.ZVRT_CONFIG, new UnsignedWordElement(45499)), //
						m(GoodWe.ChannelId.LVRT_START_VOLT, new UnsignedWordElement(45500), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_END_VOLT, new UnsignedWordElement(45501), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.LVRT_START_TRIP_TIME, new UnsignedWordElement(45502)), //
						m(GoodWe.ChannelId.LVRT_END_TRIP_TIME, new UnsignedWordElement(45503)), //
						m(GoodWe.ChannelId.LVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45504), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_VOLT, new UnsignedWordElement(45505), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_END_VOLT, new UnsignedWordElement(45506), SCALE_FACTOR_MINUS_1), //
						m(GoodWe.ChannelId.HVRT_START_TRIP_TIME, new UnsignedWordElement(45507)), //
						m(GoodWe.ChannelId.HVRT_END_TRIP_TIME, new UnsignedWordElement(45508)), //
						m(GoodWe.ChannelId.HVRT_TRIP_LIMIT_VOLT, new UnsignedWordElement(45509), SCALE_FACTOR_MINUS_1), //

						// Additional settings for PF/PU/UF
						m(GoodWe.ChannelId.PF_TIME_CONSTANT, new UnsignedWordElement(45510)), //
						m(GoodWe.ChannelId.POWER_FREQ_TIME_CONSTANT, new UnsignedWordElement(45511)), //
						// Additional settings for P(U) Curve
						m(GoodWe.ChannelId.PU_TIME_CONSTANT, new UnsignedWordElement(45512)), //
						m(GoodWe.ChannelId.D_POINT_POWER, new SignedWordElement(45513)), //
						m(GoodWe.ChannelId.D_POINT_COS_PHI, new SignedWordElement(45514)), //
						// Additional settings for UF Curve
						m(GoodWe.ChannelId.UF_RECOVERY_WAITING_TIME, new UnsignedWordElement(45515),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.UF_RECOVER_SLOPE, new UnsignedWordElement(45516)), //
						m(GoodWe.ChannelId.CFP_UF_RECOVER_POWER_PERCENT, new UnsignedWordElement(45517)), //
						m(GoodWe.ChannelId.POWER_CHARGE_LIMIT, new UnsignedWordElement(45518), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.POWER_CHARGE_LIMIT_RECONNECT, new UnsignedWordElement(45519),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_UF_CHARGE_STOP, new UnsignedWordElement(45520), SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_OF_DISCHARGE_STOP, new UnsignedWordElement(45521),
								SCALE_FACTOR_MINUS_2), //
						m(GoodWe.ChannelId.C_EXT_TWOSSTEPF_FLG, new UnsignedWordElement(45522))));
	}

	/**
	 * Gets the power settings tasks of the inverter that is using new registers
	 * especially for VDE-AR-N-4110.
	 *
	 * <p>
	 * A lot of individual power settings can be configured for each inverter. These
	 * power settings are mapped here. Known models using this protocol version are
	 * ET50 & ET100.
	 *
	 * @return a list of {@link Task}
	 */
	private List<Task> getPowerSettingsV2Tasks() {

		return Arrays.asList(

				// ── Read Task R1: 45409 – 45513 ──────────────────────────────────────
				new FC3ReadRegistersTask(45409, Priority.HIGH, //
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_1_VALUE, new UnsignedWordElement(45409),
								SCALE_FACTOR_1),
						new DummyRegisterElement(45410), //
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_1_VALUE, new UnsignedWordElement(45411),
								SCALE_FACTOR_1),
						new DummyRegisterElement(45412), //
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_2_VALUE, new UnsignedWordElement(45413),
								SCALE_FACTOR_1),
						new DummyRegisterElement(45414), //
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_2_VALUE, new UnsignedWordElement(45415),
								SCALE_FACTOR_1),
						new DummyRegisterElement(45416, 45418),
						m(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_UPPER_FREQUENCY, new UnsignedWordElement(45419),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_LOWER_FREQUENCY, new UnsignedWordElement(45420),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_OBSERVATION_TIME, new UnsignedWordElement(45421)),
						new DummyRegisterElement(45422, 45423),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_UPPER_FREQUENCY,
								new UnsignedWordElement(45424), SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_LOWER_FREQUENCY,
								new UnsignedWordElement(45425), SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_OBSERVATION_TIME,
								new UnsignedWordElement(45426)),
						new DummyRegisterElement(45427),
						m(GoodWePowerSetting.ChannelId.V2_CP_SOFT_RAMP_UP_GRADIENT, new UnsignedWordElement(45428)),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_GRADIENT, new UnsignedWordElement(45429)),
						new DummyRegisterElement(45430, 45432),
						m(GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_CURVE_COS_PHI_P, new UnsignedWordElement(45433)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_A_POINT_POWER, new SignedWordElement(45434)),
						new DummyRegisterElement(45435),
						m(GoodWePowerSetting.ChannelId.V2_RPM_B_POINT_POWER, new SignedWordElement(45436)),
						new DummyRegisterElement(45437),
						m(GoodWePowerSetting.ChannelId.V2_RPM_C_POINT_POWER, new SignedWordElement(45438)),
						new DummyRegisterElement(45439, 45443),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_START, new UnsignedWordElement(45444),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_THRESHOLD,
								new UnsignedWordElement(45445), SCALE_FACTOR_1),
						new DummyRegisterElement(45446, 45455),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_ENABLE_QU_CURVE, new UnsignedWordElement(45456)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_LOCK_IN_POWER, new SignedWordElement(45457)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_LOCK_OUT_POWER, new SignedWordElement(45458)),
						new DummyRegisterElement(45459),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V1_VALUE, new SignedWordElement(45460)),
						new DummyRegisterElement(45461),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V2_VALUE, new SignedWordElement(45462)),
						new DummyRegisterElement(45463),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V3_VALUE, new SignedWordElement(45464)),
						new DummyRegisterElement(45465),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V4_VALUE, new SignedWordElement(45466)),
						new DummyRegisterElement(45467, 45471),
						m(GoodWePowerSetting.ChannelId.V2_APM_ENABLE_PU_CURVE, new UnsignedWordElement(45472)),
						m(GoodWePowerSetting.ChannelId.V2_APM_GENERAL_POWER_GRADIENT, new UnsignedWordElement(45473)),
						new DummyRegisterElement(45474),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V1_VALUE, new SignedWordElement(45475)),
						new DummyRegisterElement(45476),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V2_VALUE, new SignedWordElement(45477)),
						new DummyRegisterElement(45478),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V3_VALUE, new SignedWordElement(45479)),
						new DummyRegisterElement(45480),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V4_VALUE, new SignedWordElement(45481)),
						new DummyRegisterElement(45482),
						m(GoodWePowerSetting.ChannelId.V2_RPM_FIXED_Q_VALUE, new SignedWordElement(45483)),
						m(GoodWePowerSetting.ChannelId.V2_APM_GENERAL_OUTPUT_ACTIVE_POWER,
								new SignedWordElement(45484)),
						new DummyRegisterElement(45485, 45512),
						m(GoodWePowerSetting.ChannelId.V2_RPM_D_POINT_POWER, new SignedWordElement(45513))),

				// ── Read Task R2: 45526 – 45624 ──────────────────────────────────────
				new FC3ReadRegistersTask(45526, Priority.HIGH,
						m(GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_QP_CURVE, new UnsignedWordElement(45526)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P1_POWER, new SignedWordElement(45527)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P1_REACTIVE_POWER, new SignedWordElement(45528)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P2_POWER, new SignedWordElement(45529)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P2_REACTIVE_POWER, new SignedWordElement(45530)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P3_POWER, new SignedWordElement(45531)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P3_REACTIVE_POWER, new SignedWordElement(45532)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P4_POWER, new SignedWordElement(45533)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P4_REACTIVE_POWER, new SignedWordElement(45534)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P5_POWER, new SignedWordElement(45535)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P5_REACTIVE_POWER, new SignedWordElement(45536)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P6_POWER, new SignedWordElement(45537)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P6_REACTIVE_POWER, new SignedWordElement(45538)),
						new DummyRegisterElement(45539, 45541),
						m(GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_FIXED_Q, new UnsignedWordElement(45542)),
						new DummyRegisterElement(45543, 45570),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_CURVE_MODE, new UnsignedWordElement(45571)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_OVEREXCITED_SLOPE, new SignedWordElement(45572)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_UNDEREXCITED_SLOPE, new SignedWordElement(45573)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_VOLTAGE_DEAD_BAND, new UnsignedWordElement(45574)),
						new DummyRegisterElement(45575, 45621),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_CURVE_MODE, new UnsignedWordElement(45622)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_OVEREXCITED_SLOPE, new SignedWordElement(45623)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_UNDEREXCITED_SLOPE, new SignedWordElement(45624)),
						new DummyRegisterElement(45625, 45646)),

				// ── Read Task R3: 45647 – 45699 ──────────────────────────────────────
				new FC3ReadRegistersTask(45647, Priority.HIGH,
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_3_VALUE, new UnsignedWordElement(45647),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_3_VALUE, new UnsignedWordElement(45648),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_4_VALUE, new UnsignedWordElement(45649),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_4_VALUE, new UnsignedWordElement(45650),
								SCALE_FACTOR_1),
						new DummyRegisterElement(45651),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_1_VALUE, new UnsignedWordElement(45652)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_1_TRIP_TIME,
								new UnsignedDoublewordElement(45653)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_1_VALUE, new UnsignedWordElement(45655)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_1_TRIP_TIME,
								new UnsignedDoublewordElement(45656)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_2_VALUE, new UnsignedWordElement(45658)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_2_TRIP_TIME,
								new UnsignedDoublewordElement(45659)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_2_VALUE, new UnsignedWordElement(45661)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_2_TRIP_TIME,
								new UnsignedDoublewordElement(45662)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_3_VALUE, new UnsignedWordElement(45664)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_3_TRIP_TIME,
								new UnsignedDoublewordElement(45665)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_3_VALUE, new UnsignedWordElement(45667)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_3_TRIP_TIME,
								new UnsignedDoublewordElement(45668)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_4_VALUE, new UnsignedWordElement(45670)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_4_TRIP_TIME,
								new UnsignedDoublewordElement(45671)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_4_VALUE, new UnsignedWordElement(45673),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_4_TRIP_TIME,
								new UnsignedDoublewordElement(45674)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_1_TRIP_TIME,
								new UnsignedDoublewordElement(45676)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_1_TRIP_TIME,
								new UnsignedDoublewordElement(45678)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_2_TRIP_TIME,
								new UnsignedDoublewordElement(45680)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_2_TRIP_TIME,
								new UnsignedDoublewordElement(45682)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_3_TRIP_TIME,
								new UnsignedDoublewordElement(45684)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_3_TRIP_TIME,
								new UnsignedDoublewordElement(45686)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_4_TRIP_TIME,
								new UnsignedDoublewordElement(45688)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_4_TRIP_TIME,
								new UnsignedDoublewordElement(45690)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_TEN_MIN_OVERVOLT_STAGE_VALUE,
								new UnsignedWordElement(45692)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_TEN_MIN_STAGE_TRIP_TIME,
								new UnsignedDoublewordElement(45693)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_TIME_CONSTANT, new UnsignedWordElement(45695),
								SCALE_FACTOR_2),
						new DummyRegisterElement(45696),
						m(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_TIME_CONSTANT, new UnsignedWordElement(45697),
								SCALE_FACTOR_2),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_TIME_CONSTANT, new UnsignedWordElement(45698),
								SCALE_FACTOR_2),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_PT1_TIME_CONSTANT_PT1_MODE,
								new UnsignedWordElement(45699), SCALE_FACTOR_2)),

				// ── Read Task R4: 45701 – 45781 ──────────────────────────────────────
				new FC3ReadRegistersTask(45701, Priority.HIGH,
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V1_VOLTAGE, new UnsignedWordElement(45701)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V2_VOLTAGE, new UnsignedWordElement(45702)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V3_VOLTAGE, new UnsignedWordElement(45703)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V4_VOLTAGE, new UnsignedWordElement(45704)),
						new DummyRegisterElement(45705, 45708),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_EXTENDED_FUNCTIONS, new UnsignedWordElement(45709)),
						new DummyRegisterElement(45710, 45713),
						m(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_LOWER_VOLTAGE, new UnsignedWordElement(45714)),
						m(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_UPPER_VOLTAGE, new UnsignedWordElement(45715)),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_LOWER_VOLTAGE,
								new UnsignedWordElement(45716)),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_UPPER_VOLTAGE,
								new UnsignedWordElement(45717)),
						new DummyRegisterElement(45718, 45719),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V1_VOLTAGE, new UnsignedWordElement(45720)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V2_VOLTAGE, new UnsignedWordElement(45721)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V3_VOLTAGE, new UnsignedWordElement(45722)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V4_VOLTAGE, new UnsignedWordElement(45723)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_OUTPUT_RESPONSE_MODE, new UnsignedWordElement(45724)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_PT1_TIME_CONSTANT_GRADIENT_MODE,
								new UnsignedWordElement(45725)),
						new DummyRegisterElement(45726, 45732),
						m(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_LOCK_IN_VOLTAGE, new UnsignedWordElement(45733)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_LOCK_OUT_VOLTAGE, new UnsignedWordElement(45734)),
						new DummyRegisterElement(45735, 45736),
						m(GoodWePowerSetting.ChannelId.V2_RPM_A_POINT_COS_PHI, new SignedWordElement(45737)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_B_POINT_COS_PHI, new SignedWordElement(45738)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_C_POINT_COS_PHI, new SignedWordElement(45739)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_D_POINT_COS_PHI, new SignedWordElement(45740)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_E_POINT_POWER, new SignedWordElement(45741)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_E_POINT_COS_PHI, new SignedWordElement(45742)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_EXTENDED_FUNCTIONS,
								new UnsignedWordElement(45743)),
						new DummyRegisterElement(45744, 45753),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_DELAY_TIME,
								new UnsignedWordElement(45754), SCALE_FACTOR_2),
						new DummyRegisterElement(45755),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_SLOPE, new UnsignedWordElement(45756)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_FSTOP_ENABLE,
								new UnsignedWordElement(45757)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_HYSTERESIS_POINT,
								new UnsignedWordElement(45758), SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_DELAY_WAITING_TIME,
								new UnsignedWordElement(45759), SCALE_FACTOR_2),
						new DummyRegisterElement(45760),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_HYSTERESIS_SLOPE,
								new UnsignedWordElement(45761)),
						new DummyRegisterElement(45762, 45777),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_DELAY_TIME,
								new UnsignedWordElement(45778), SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(45779),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_SLOPE, new UnsignedWordElement(45780)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_FSTOP_ENABLE,
								new UnsignedWordElement(45781))),

				// ── Read Task R5: 45782 – 45867 ──────────────────────────────────────
				new FC3ReadRegistersTask(45782, Priority.HIGH,
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_POINT,
								new UnsignedWordElement(45782), SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_DELAY_WAITING_TIME,
								new UnsignedWordElement(45783), SCALE_FACTOR_2),
						new DummyRegisterElement(45784),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_SLOPE,
								new UnsignedWordElement(45785)),
						new DummyRegisterElement(45786, 45799),
						m(GoodWePowerSetting.ChannelId.V2_CP_SOFT_RAMP_UP_GRADIENT_ENABLE,
								new UnsignedWordElement(45800)),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_GRADIENT_ENABLE,
								new UnsignedWordElement(45801)),
						new DummyRegisterElement(45802, 45824),
						m(GoodWePowerSetting.ChannelId.V2_VRT_CURRENT_DISTRIBUTION_MODE,
								new UnsignedWordElement(45825)),
						m(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_MODE,
								new UnsignedWordElement(45826)),
						m(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_SPEED,
								new UnsignedWordElement(45827)),
						m(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_MODE_END,
								new UnsignedWordElement(45828)),
						m(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_SPEED,
								new UnsignedWordElement(45829)),
						new DummyRegisterElement(45830, 45833),
						m(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_SLOPE,
								new UnsignedDoublewordElement(45834)),
						m(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_SLOPE,
								new UnsignedDoublewordElement(45836)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_ENABLE, new UnsignedWordElement(45838)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_ENTER_THRESHOLD, new UnsignedWordElement(45839)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_EXIT_ENDPOINT, new UnsignedWordElement(45840)),
						new DummyRegisterElement(45841, 45845),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_K1_SLOPE, new UnsignedWordElement(45846)),
						new DummyRegisterElement(45847, 45851),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_ZERO_CURRENT_MODE_ENABLE,
								new UnsignedWordElement(45852)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD,
								new UnsignedWordElement(45853)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV1_VOLTAGE, new UnsignedWordElement(45854)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV1_TIME, new UnsignedWordElement(45855),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV2_VOLTAGE, new UnsignedWordElement(45856)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV2_TIME, new UnsignedWordElement(45857),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV3_VOLTAGE, new UnsignedWordElement(45858)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV3_TIME, new UnsignedWordElement(45859),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV4_VOLTAGE, new UnsignedWordElement(45860)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV4_TIME, new UnsignedWordElement(45861),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV5_VOLTAGE, new UnsignedWordElement(45862)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV5_TIME, new UnsignedWordElement(45863),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV6_VOLTAGE, new UnsignedWordElement(45864)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV6_TIME, new UnsignedWordElement(45865),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV7_VOLTAGE, new UnsignedWordElement(45866)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV7_TIME, new UnsignedWordElement(45867),
								SCALE_FACTOR_1),
						new DummyRegisterElement(45868, 45870)),

				// ── Read Task R6: 45871 – 45918 ──────────────────────────────────────
				new FC3ReadRegistersTask(45871, Priority.HIGH,
						m(GoodWePowerSetting.ChannelId.V2_HVRT_ENABLE, new UnsignedWordElement(45871)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_ENTER_HIGH_CROSSING, new UnsignedWordElement(45872)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_EXIT_HIGH_CROSSING, new UnsignedWordElement(45873)),
						new DummyRegisterElement(45874, 45878),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_K2_SLOPE, new UnsignedWordElement(45879)),
						new DummyRegisterElement(45880, 45884),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_ZERO_CURRENT_MODE_ENABLE,
								new UnsignedWordElement(45885)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD,
								new UnsignedWordElement(45886)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV1_VOLTAGE, new UnsignedWordElement(45887)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV1_TIME, new UnsignedWordElement(45888),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV2_VOLTAGE, new UnsignedWordElement(45889)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV2_TIME, new UnsignedWordElement(45890),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV3_VOLTAGE, new UnsignedWordElement(45891)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV3_TIME, new UnsignedWordElement(45892),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV4_VOLTAGE, new UnsignedWordElement(45893)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV4_TIME, new UnsignedWordElement(45894),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV5_VOLTAGE, new UnsignedWordElement(45895)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV5_TIME, new UnsignedWordElement(45896),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV6_VOLTAGE, new UnsignedWordElement(45897)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV6_TIME, new UnsignedWordElement(45898),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV7_VOLTAGE, new UnsignedWordElement(45899)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV7_TIME, new UnsignedWordElement(45900),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_ENABLE, new UnsignedWordElement(45901)),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF1_FREQUENCY, new UnsignedWordElement(45902),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF1_TIME, new UnsignedWordElement(45903)),
						new DummyRegisterElement(45904),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF2_FREQUENCY, new UnsignedWordElement(45905),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF2_TIME, new UnsignedWordElement(45906)),
						new DummyRegisterElement(45907),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF3_FREQUENCY, new UnsignedWordElement(45908),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF3_TIME, new UnsignedWordElement(45909)),
						new DummyRegisterElement(45910),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF1_FREQUENCY, new UnsignedWordElement(45911),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF1_TIME, new UnsignedWordElement(45912)),
						new DummyRegisterElement(45913),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF2_FREQUENCY, new UnsignedWordElement(45914),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF2_TIME, new UnsignedWordElement(45915)),
						new DummyRegisterElement(45916),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF3_FREQUENCY, new UnsignedWordElement(45917),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF3_TIME, new UnsignedWordElement(45918),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3)),

				new FC16WriteRegistersTask(45409,
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_1_VALUE, new UnsignedWordElement(45409),
								SCALE_FACTOR_1),
						new DummyRegisterElement(45410),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_1_VALUE, new UnsignedWordElement(45411),
								SCALE_FACTOR_1),
						new DummyRegisterElement(45412),
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_2_VALUE, new UnsignedWordElement(45413),
								SCALE_FACTOR_1),
						new DummyRegisterElement(45414),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_2_VALUE, new UnsignedWordElement(45415),
								SCALE_FACTOR_1),
						new DummyRegisterElement(45416, 45418),
						m(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_UPPER_FREQUENCY, new UnsignedWordElement(45419),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_LOWER_FREQUENCY, new UnsignedWordElement(45420),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_OBSERVATION_TIME, new UnsignedWordElement(45421)),
						new DummyRegisterElement(45422, 45423),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_UPPER_FREQUENCY,
								new UnsignedWordElement(45424), SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_LOWER_FREQUENCY,
								new UnsignedWordElement(45425), SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_OBSERVATION_TIME,
								new UnsignedWordElement(45426)),
						new DummyRegisterElement(45427),
						m(GoodWePowerSetting.ChannelId.V2_CP_SOFT_RAMP_UP_GRADIENT, new UnsignedWordElement(45428)),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_GRADIENT, new UnsignedWordElement(45429)),
						new DummyRegisterElement(45430, 45432),
						m(GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_CURVE_COS_PHI_P, new UnsignedWordElement(45433)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_A_POINT_POWER, new SignedWordElement(45434)),
						new DummyRegisterElement(45435),
						m(GoodWePowerSetting.ChannelId.V2_RPM_B_POINT_POWER, new SignedWordElement(45436)),
						new DummyRegisterElement(45437),
						m(GoodWePowerSetting.ChannelId.V2_RPM_C_POINT_POWER, new SignedWordElement(45438))),

				new FC16WriteRegistersTask(45444,
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_START, new UnsignedWordElement(45444),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_THRESHOLD,
								new UnsignedWordElement(45445), SCALE_FACTOR_1)),

				new FC16WriteRegistersTask(45456,
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_ENABLE_QU_CURVE, new UnsignedWordElement(45456)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_LOCK_IN_POWER, new SignedWordElement(45457)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_LOCK_OUT_POWER, new SignedWordElement(45458)),
						new DummyRegisterElement(45459),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V1_VALUE, new SignedWordElement(45460)),
						new DummyRegisterElement(45461),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V2_VALUE, new SignedWordElement(45462)),
						new DummyRegisterElement(45463),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V3_VALUE, new SignedWordElement(45464)),
						new DummyRegisterElement(45465),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V4_VALUE, new SignedWordElement(45466))),

				new FC16WriteRegistersTask(45472,
						m(GoodWePowerSetting.ChannelId.V2_APM_ENABLE_PU_CURVE, new UnsignedWordElement(45472)),
						m(GoodWePowerSetting.ChannelId.V2_APM_GENERAL_POWER_GRADIENT, new UnsignedWordElement(45473)),
						new DummyRegisterElement(45474),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V1_VALUE, new SignedWordElement(45475)),
						new DummyRegisterElement(45476),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V2_VALUE, new SignedWordElement(45477)),
						new DummyRegisterElement(45478),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V3_VALUE, new SignedWordElement(45479)),
						new DummyRegisterElement(45480),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V4_VALUE, new SignedWordElement(45481)),
						new DummyRegisterElement(45482),
						m(GoodWePowerSetting.ChannelId.V2_RPM_FIXED_Q_VALUE, new SignedWordElement(45483)),
						m(GoodWePowerSetting.ChannelId.V2_APM_GENERAL_OUTPUT_ACTIVE_POWER,
								new SignedWordElement(45484))),

				new FC6WriteRegisterTask(45513,
						m(GoodWePowerSetting.ChannelId.V2_RPM_D_POINT_POWER, new SignedWordElement(45513))),

				new FC16WriteRegistersTask(45526,
						m(GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_QP_CURVE, new UnsignedWordElement(45526)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P1_POWER, new SignedWordElement(45527)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P1_REACTIVE_POWER, new SignedWordElement(45528)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P2_POWER, new SignedWordElement(45529)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P2_REACTIVE_POWER, new SignedWordElement(45530)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P3_POWER, new SignedWordElement(45531)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P3_REACTIVE_POWER, new SignedWordElement(45532)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P4_POWER, new SignedWordElement(45533)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P4_REACTIVE_POWER, new SignedWordElement(45534)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P5_POWER, new SignedWordElement(45535)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P5_REACTIVE_POWER, new SignedWordElement(45536)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P6_POWER, new SignedWordElement(45537)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_P6_REACTIVE_POWER, new SignedWordElement(45538)),
						new DummyRegisterElement(45539, 45541),
						m(GoodWePowerSetting.ChannelId.V2_RPM_ENABLE_FIXED_Q, new UnsignedWordElement(45542))),

				new FC16WriteRegistersTask(45571,
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_CURVE_MODE, new UnsignedWordElement(45571)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_OVEREXCITED_SLOPE, new SignedWordElement(45572)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_UNDEREXCITED_SLOPE, new SignedWordElement(45573)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_VOLTAGE_DEAD_BAND, new UnsignedWordElement(45574))),

				new FC16WriteRegistersTask(45622,
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_CURVE_MODE, new UnsignedWordElement(45622)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_OVEREXCITED_SLOPE, new SignedWordElement(45623)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_UNDEREXCITED_SLOPE, new SignedWordElement(45624))),

				new FC16WriteRegistersTask(45647,
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_3_VALUE, new UnsignedWordElement(45647),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_3_VALUE, new UnsignedWordElement(45648),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_4_VALUE, new UnsignedWordElement(45649),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_4_VALUE, new UnsignedWordElement(45650),
								SCALE_FACTOR_1),
						new DummyRegisterElement(45651),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_1_VALUE, new UnsignedWordElement(45652)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_1_TRIP_TIME,
								new UnsignedDoublewordElement(45653)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_1_VALUE, new UnsignedWordElement(45655)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_1_TRIP_TIME,
								new UnsignedDoublewordElement(45656)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_2_VALUE, new UnsignedWordElement(45658)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_2_TRIP_TIME,
								new UnsignedDoublewordElement(45659)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_2_VALUE, new UnsignedWordElement(45661)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_2_TRIP_TIME,
								new UnsignedDoublewordElement(45662)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_3_VALUE, new UnsignedWordElement(45664)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_3_TRIP_TIME,
								new UnsignedDoublewordElement(45665)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_3_VALUE, new UnsignedWordElement(45667)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_3_TRIP_TIME,
								new UnsignedDoublewordElement(45668)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_4_VALUE, new UnsignedWordElement(45670)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_UNDER_VOLT_STAGE_4_TRIP_TIME,
								new UnsignedDoublewordElement(45671)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_4_VALUE, new UnsignedWordElement(45673),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_VPP_OVER_VOLT_STAGE_4_TRIP_TIME,
								new UnsignedDoublewordElement(45674)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_1_TRIP_TIME,
								new UnsignedDoublewordElement(45676)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_1_TRIP_TIME,
								new UnsignedDoublewordElement(45678)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_2_TRIP_TIME,
								new UnsignedDoublewordElement(45680)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_2_TRIP_TIME,
								new UnsignedDoublewordElement(45682)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_3_TRIP_TIME,
								new UnsignedDoublewordElement(45684)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_3_TRIP_TIME,
								new UnsignedDoublewordElement(45686)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_UNDER_FREQ_STAGE_4_TRIP_TIME,
								new UnsignedDoublewordElement(45688)),
						m(GoodWePowerSetting.ChannelId.V2_FPP_OVER_FREQ_STAGE_4_TRIP_TIME,
								new UnsignedDoublewordElement(45690)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_TEN_MIN_OVERVOLT_STAGE_VALUE,
								new UnsignedWordElement(45692)),
						m(GoodWePowerSetting.ChannelId.V2_VPP_TEN_MIN_STAGE_TRIP_TIME,
								new UnsignedDoublewordElement(45693)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_TIME_CONSTANT, new UnsignedWordElement(45695),
								SCALE_FACTOR_2),
						new DummyRegisterElement(45696),
						m(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_TIME_CONSTANT, new UnsignedWordElement(45697),
								SCALE_FACTOR_2),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QP_TIME_CONSTANT, new UnsignedWordElement(45698),
								SCALE_FACTOR_2),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_PT1_TIME_CONSTANT_PT1_MODE,
								new UnsignedWordElement(45699), SCALE_FACTOR_2)),

				new FC16WriteRegistersTask(45701,
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V1_VOLTAGE, new UnsignedWordElement(45701)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V2_VOLTAGE, new UnsignedWordElement(45702)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V3_VOLTAGE, new UnsignedWordElement(45703)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_V4_VOLTAGE, new UnsignedWordElement(45704))),

				new FC6WriteRegisterTask(45709,
						m(GoodWePowerSetting.ChannelId.V2_RPM_QU_EXTENDED_FUNCTIONS, new UnsignedWordElement(45709))),

				new FC16WriteRegistersTask(45714,
						m(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_LOWER_VOLTAGE, new UnsignedWordElement(45714)),
						m(GoodWePowerSetting.ChannelId.V2_CP_RAMP_UP_UPPER_VOLTAGE, new UnsignedWordElement(45715)),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_LOWER_VOLTAGE,
								new UnsignedWordElement(45716)),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_UPPER_VOLTAGE,
								new UnsignedWordElement(45717)),
						new DummyRegisterElement(45718, 45719),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V1_VOLTAGE, new UnsignedWordElement(45720)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V2_VOLTAGE, new UnsignedWordElement(45721)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V3_VOLTAGE, new UnsignedWordElement(45722)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_V4_VOLTAGE, new UnsignedWordElement(45723)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_OUTPUT_RESPONSE_MODE, new UnsignedWordElement(45724)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PU_PT1_TIME_CONSTANT_GRADIENT_MODE,
								new UnsignedWordElement(45725))),

				new FC16WriteRegistersTask(45733,
						m(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_LOCK_IN_VOLTAGE, new UnsignedWordElement(45733)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_LOCK_OUT_VOLTAGE, new UnsignedWordElement(45734)),
						new DummyRegisterElement(45735, 45736),
						m(GoodWePowerSetting.ChannelId.V2_RPM_A_POINT_COS_PHI, new SignedWordElement(45737)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_B_POINT_COS_PHI, new SignedWordElement(45738)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_C_POINT_COS_PHI, new SignedWordElement(45739)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_D_POINT_COS_PHI, new SignedWordElement(45740)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_E_POINT_POWER, new SignedWordElement(45741)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_E_POINT_COS_PHI, new SignedWordElement(45742)),
						m(GoodWePowerSetting.ChannelId.V2_RPM_COSPHIP_EXTENDED_FUNCTIONS,
								new UnsignedWordElement(45743))),

				new FC16WriteRegistersTask(45754,
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_DELAY_TIME,
								new UnsignedWordElement(45754), SCALE_FACTOR_2),
						new DummyRegisterElement(45755),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_SLOPE, new UnsignedWordElement(45756)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_FSTOP_ENABLE,
								new UnsignedWordElement(45757)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_HYSTERESIS_POINT,
								new UnsignedWordElement(45758), SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_DELAY_WAITING_TIME,
								new UnsignedWordElement(45759), SCALE_FACTOR_2),
						new DummyRegisterElement(45760),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_OVERFREQUENCY_HYSTERESIS_SLOPE,
								new UnsignedWordElement(45761))),

				new FC16WriteRegistersTask(45778,
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_DELAY_TIME,
								new UnsignedWordElement(45778), SCALE_FACTOR_MINUS_1),
						new DummyRegisterElement(45779),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_SLOPE, new UnsignedWordElement(45780)),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_FSTOP_ENABLE,
								new UnsignedWordElement(45781))),

				new FC16WriteRegistersTask(45782,
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_POINT,
								new UnsignedWordElement(45782), SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_DELAY_WAITING_TIME,
								new UnsignedWordElement(45783), SCALE_FACTOR_2),
						new DummyRegisterElement(45784),
						m(GoodWePowerSetting.ChannelId.V2_APM_PF_UNDERFREQUENCY_HYSTERESIS_SLOPE,
								new UnsignedWordElement(45785))),

				new FC16WriteRegistersTask(45800,
						m(GoodWePowerSetting.ChannelId.V2_CP_SOFT_RAMP_UP_GRADIENT_ENABLE,
								new UnsignedWordElement(45800)),
						m(GoodWePowerSetting.ChannelId.V2_CP_RECONNECTION_GRADIENT_ENABLE,
								new UnsignedWordElement(45801))),

				new FC16WriteRegistersTask(45825,
						m(GoodWePowerSetting.ChannelId.V2_VRT_CURRENT_DISTRIBUTION_MODE,
								new UnsignedWordElement(45825)),
						m(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_MODE,
								new UnsignedWordElement(45826)),
						m(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_SPEED,
								new UnsignedWordElement(45827)),
						m(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_MODE_END,
								new UnsignedWordElement(45828)),
						m(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_SPEED,
								new UnsignedWordElement(45829))),

				new FC16WriteRegistersTask(45834,
						m(GoodWePowerSetting.ChannelId.V2_VRT_ACTIVE_POWER_RECOVERY_SLOPE,
								new UnsignedDoublewordElement(45834)),
						m(GoodWePowerSetting.ChannelId.V2_VRT_REACTIVE_POWER_RECOVERY_SLOPE,
								new UnsignedDoublewordElement(45836)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_ENABLE, new UnsignedWordElement(45838)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_ENTER_THRESHOLD, new UnsignedWordElement(45839)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_EXIT_ENDPOINT, new UnsignedWordElement(45840))),

				new FC6WriteRegisterTask(45846,
						m(GoodWePowerSetting.ChannelId.V2_LVRT_K1_SLOPE, new UnsignedWordElement(45846))),

				new FC16WriteRegistersTask(45852,
						m(GoodWePowerSetting.ChannelId.V2_LVRT_ZERO_CURRENT_MODE_ENABLE,
								new UnsignedWordElement(45852)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD,
								new UnsignedWordElement(45853)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV1_VOLTAGE, new UnsignedWordElement(45854)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV1_TIME, new UnsignedWordElement(45855),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV2_VOLTAGE, new UnsignedWordElement(45856)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV2_TIME, new UnsignedWordElement(45857),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV3_VOLTAGE, new UnsignedWordElement(45858)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV3_TIME, new UnsignedWordElement(45859),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV4_VOLTAGE, new UnsignedWordElement(45860)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV4_TIME, new UnsignedWordElement(45861),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV5_VOLTAGE, new UnsignedWordElement(45862)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV5_TIME, new UnsignedWordElement(45863),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV6_VOLTAGE, new UnsignedWordElement(45864)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV6_TIME, new UnsignedWordElement(45865),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV7_VOLTAGE, new UnsignedWordElement(45866)),
						m(GoodWePowerSetting.ChannelId.V2_LVRT_UV7_TIME, new UnsignedWordElement(45867),
								SCALE_FACTOR_1)),

				new FC16WriteRegistersTask(45871,
						m(GoodWePowerSetting.ChannelId.V2_HVRT_ENABLE, new UnsignedWordElement(45871)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_ENTER_HIGH_CROSSING, new UnsignedWordElement(45872)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_EXIT_HIGH_CROSSING, new UnsignedWordElement(45873))),

				new FC6WriteRegisterTask(45879,
						m(GoodWePowerSetting.ChannelId.V2_HVRT_K2_SLOPE, new UnsignedWordElement(45879))),

				new FC16WriteRegistersTask(45885,
						m(GoodWePowerSetting.ChannelId.V2_HVRT_ZERO_CURRENT_MODE_ENABLE,
								new UnsignedWordElement(45885)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_ZERO_CURRENT_MODE_ENTRY_THRESHOLD,
								new UnsignedWordElement(45886)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV1_VOLTAGE, new UnsignedWordElement(45887)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV1_TIME, new UnsignedWordElement(45888),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV2_VOLTAGE, new UnsignedWordElement(45889)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV2_TIME, new UnsignedWordElement(45890),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV3_VOLTAGE, new UnsignedWordElement(45891)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV3_TIME, new UnsignedWordElement(45892),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV4_VOLTAGE, new UnsignedWordElement(45893)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV4_TIME, new UnsignedWordElement(45894),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV5_VOLTAGE, new UnsignedWordElement(45895)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV5_TIME, new UnsignedWordElement(45896),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV6_VOLTAGE, new UnsignedWordElement(45897)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV6_TIME, new UnsignedWordElement(45898),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV7_VOLTAGE, new UnsignedWordElement(45899)),
						m(GoodWePowerSetting.ChannelId.V2_HVRT_OV7_TIME, new UnsignedWordElement(45900),
								SCALE_FACTOR_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_ENABLE, new UnsignedWordElement(45901)),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF1_FREQUENCY, new UnsignedWordElement(45902),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF1_TIME, new UnsignedWordElement(45903)),
						new DummyRegisterElement(45904),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF2_FREQUENCY, new UnsignedWordElement(45905),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF2_TIME, new UnsignedWordElement(45906)),
						new DummyRegisterElement(45907),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF3_FREQUENCY, new UnsignedWordElement(45908),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_UF3_TIME, new UnsignedWordElement(45909)),
						new DummyRegisterElement(45910),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF1_FREQUENCY, new UnsignedWordElement(45911),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF1_TIME, new UnsignedWordElement(45912)),
						new DummyRegisterElement(45913),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF2_FREQUENCY, new UnsignedWordElement(45914),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF2_TIME, new UnsignedWordElement(45915)),
						new DummyRegisterElement(45916),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF3_FREQUENCY, new UnsignedWordElement(45917),
								SCALE_FACTOR_MINUS_1),
						m(GoodWePowerSetting.ChannelId.V2_FRT_OF3_TIME, new UnsignedWordElement(45918),
								ElementToChannelConverter.SCALE_FACTOR_MINUS_3)));
	}

	private void addPowerSettingTasks() {
		var protocol = this.getModbusProtocol();
		removeTasks(//
				protocol, //
				this.safetyParameterSettingsTasks //
		);
		this.safetyParameterSettingsTasks = switch (this.config.gridCode()) {
		case VDE_4105 -> this.getDefaultPowerSettingsTasks();
		case VDE_4110 -> this.getPowerSettingsV2Tasks();
		case UNDEFINED -> Collections.emptyList();
		};
		protocol.addTasks(this.safetyParameterSettingsTasks);
	}
}
