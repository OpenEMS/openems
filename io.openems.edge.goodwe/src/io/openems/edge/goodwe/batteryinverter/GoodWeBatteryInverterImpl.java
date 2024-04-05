package io.openems.edge.goodwe.batteryinverter;

import static io.openems.edge.common.channel.ChannelUtils.setWriteValueIfNotRead;

import java.util.Objects;

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
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.event.EdgeEventConstants;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.sum.Sum;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.goodwe.common.AbstractGoodWe;
import io.openems.edge.goodwe.common.ApplyPowerHandler;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.enums.AppModeIndex;
import io.openems.edge.goodwe.common.enums.BatteryProtocol;
import io.openems.edge.goodwe.common.enums.ControlMode;
import io.openems.edge.goodwe.common.enums.EnableCurve;
import io.openems.edge.goodwe.common.enums.EnableDisable;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.BatteryInverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
)
@EventTopics({ //
		EdgeEventConstants.TOPIC_CYCLE_BEFORE_PROCESS_IMAGE, //
})
public class GoodWeBatteryInverterImpl extends AbstractGoodWe
		implements GoodWeBatteryInverter, GoodWe, HybridManagedSymmetricBatteryInverter,
		ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, ModbusComponent, OpenemsComponent, EventHandler {

	// Fenecon Home Battery Static module min voltage, used to calculate battery
	// module number per tower
	// TODO get from Battery
	private static final int MODULE_MIN_VOLTAGE = 42;

	private final Logger log = LoggerFactory.getLogger(GoodWeBatteryInverterImpl.class);
	private final ApplyPowerHandler applyPowerHandler = new ApplyPowerHandler();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	private ConfigurationAdmin cm;

	@Reference
	private Power power;

	@Reference
	private Sum sum;

	@Override
	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	/**
	 * We don't want to hold an actual Reference to the {@link Battery} Component,
	 * so we keep the latest data here. Updated in {@link #run(Battery, int, int)}.
	 */
	private BatteryData latestBatteryData = new BatteryData(null, null);

	protected static record BatteryData(Integer chargeMaxCurrent, Integer voltage) {
	}

	private Config config;

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
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.config = config;
		this.applyConfigIfNotSet(config, true);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
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
	}

	@Override
	public void handleEvent(Event event) {
		if (!this.isEnabled()) {
			return;
		}
		super.handleEvent(event);
	}

	/**
	 * Apply the configuration on if the values are not already set.
	 *
	 * <p>
	 * Feed In Power Setting consist of: Installed inverter country, feeding method:
	 * whether according to the power factor or power and frequency. In addition, it
	 * consist backup power availability.
	 *
	 * @param config         Configuration parameters.
	 * @param onConfigUpdate true when called on activate()/modified(), i.e. not in
	 *                       run()
	 * 
	 * @throws OpenemsNamedException on error
	 */
	private void applyConfigIfNotSet(Config config, boolean onConfigUpdate) throws OpenemsNamedException {

		// TODO: Set later. SELECT_WORK_MODE mapped after reading the dsp version
		// (0x00) 'General Mode: Self use' instead of (0x01) 'Off-grid Mode', (0x02)
		// 'Backup Mode' or (0x03) 'Economic Mode'.
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.SELECT_WORK_MODE), AppModeIndex.SELF_USE);

		// country setting
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.SAFETY_COUNTRY_CODE), config.safetyCountry());

		// Backup Power on / off
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.BACK_UP_ENABLE), config.backupEnable().booleanValue);

		// Should be updated according to backup power
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.AUTO_START_BACKUP), config.backupEnable().booleanValue);

		// Feed-in limitation on / off
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.FEED_POWER_ENABLE), config.feedPowerEnable().booleanValue);

		// Feed-in limitation
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.FEED_POWER_PARA_SET), config.feedPowerPara());

		// Set to feed in power settings to default
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.QU_CURVE), EnableCurve.DISABLE);
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.ENABLE_CURVE_PU), EnableCurve.DISABLE);

		if (onConfigUpdate) {
			// Mppt Shadow enable / disable
			setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.MPPT_FOR_SHADOW_ENABLE), false);

			// Feed-in settings
			var setFeedInPowerSettings = config.setfeedInPowerSettings();
			switch (setFeedInPowerSettings) {
			case UNDEFINED:
				break;
			case QU_ENABLE_CURVE:
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.LOCK_IN_POWER_QU), 200);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.LOCK_OUT_POWER_QU), 50);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.V1_VOLTAGE), 214);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.V1_VALUE), 436);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.V2_VOLTAGE), 223);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.V2_VALUE), 0);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.V3_VOLTAGE), 237);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.V3_VALUE), 0);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.V4_VOLTAGE), 247);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.V4_VALUE), -526);
				break;
			case PU_ENABLE_CURVE:
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.A_POINT_POWER), 2000);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.A_POINT_COS_PHI), 0);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.B_POINT_POWER), 2000);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.B_POINT_COS_PHI), 0);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.C_POINT_POWER), 2000);
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.C_POINT_COS_PHI), 0);
				break;
			case LAGGING_0_80:
			case LAGGING_0_81:
			case LAGGING_0_82:
			case LAGGING_0_83:
			case LAGGING_0_84:
			case LAGGING_0_85:
			case LAGGING_0_86:
			case LAGGING_0_87:
			case LAGGING_0_88:
			case LAGGING_0_89:
			case LAGGING_0_90:
			case LAGGING_0_91:
			case LAGGING_0_92:
			case LAGGING_0_93:
			case LAGGING_0_94:
			case LAGGING_0_95:
			case LAGGING_0_96:
			case LAGGING_0_97:
			case LAGGING_0_98:
			case LAGGING_0_99:
			case LEADING_0_80:
			case LEADING_0_81:
			case LEADING_0_82:
			case LEADING_0_83:
			case LEADING_0_84:
			case LEADING_0_85:
			case LEADING_0_86:
			case LEADING_0_87:
			case LEADING_0_88:
			case LEADING_0_89:
			case LEADING_0_90:
			case LEADING_0_91:
			case LEADING_0_92:
			case LEADING_0_93:
			case LEADING_0_94:
			case LEADING_0_95:
			case LEADING_0_96:
			case LEADING_0_97:
			case LEADING_0_98:
			case LEADING_0_99:
			case LEADING_1:
				if (setFeedInPowerSettings.fixedPowerFactor == null) {
					throw new IllegalArgumentException(
							"Feed-In-Power-Setting [" + setFeedInPowerSettings + "] has no fixed power factor");
				}
				setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.FIXED_POWER_FACTOR),
						config.setfeedInPowerSettings().fixedPowerFactor);
				break;
			}
		}

		// Ripple Control Receiver on / off
		setWriteValueIfNotRead(this.channel(GoodWe.ChannelId.RIPPLE_CONTROL_RECEIVER_ENABLE),
				config.rcrEnable().booleanValue);
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
		var setChargeMaxCurrent = this.getGoodweType().maxDcCurrent;
		var setDischargeMaxCurrent = this.getGoodweType().maxDcCurrent;
		var setChargeMaxVoltage = battery.getChargeMaxVoltage().orElse(0);
		var setDischargeMinVoltage = battery.getDischargeMinVoltage().orElse(0);
		Integer setSocUnderMin = 0; // [0-100]; 0 MinSoc = 100 DoD
		Integer setOfflineSocUnderMin = 0; // [0-100]; 0 MinSoc = 100 DoD

		if (battery.isStarted() && battery instanceof BatteryFeneconHome homeBattery) {

			setBatteryStrings = homeBattery.getNumberOfModulesPerTower().orElse(setBatteryStrings);

			/*
			 * Check combination of GoodWe inverter and FENECON Home battery to avoid
			 * invalid combinations for FENECON Home Systems
			 */
			if (this.getGoodweType().isInvalidBattery.test(homeBattery.getBatteryHardwareType())) {
				this._setImpossibleFeneconHomeCombination(true);

				// Set zero limits to avoid charging and discharging
				setChargeMaxCurrent = 0;
				setDischargeMaxCurrent = 0;
			} else {
				this._setImpossibleFeneconHomeCombination(false);
			}
		}

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
		return calculateSurplusPower(this.latestBatteryData, productionPower, this.getGoodweType().maxDcCurrent);

	}

	protected static Integer calculateSurplusPower(BatteryData battery, Integer productionPower, int maxDcCurrent) {
		// Is DC Charge Current available?
		if (battery.chargeMaxCurrent == null || battery.chargeMaxCurrent >= maxDcCurrent) {
			return null;
		}

		// Is DC PV Production available?
		if (productionPower == null || productionPower <= 0) {
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
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// GoodWe is always started. This has no effect.
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower) throws OpenemsNamedException {

		// ApplyConfig
		this.applyConfigIfNotSet(this.config, false);

		// Calculate ActivePower, Energy and Max-AC-Power.
		this.updatePowerAndEnergyChannels();
		this.calculateMaxAcPower(this.getMaxApparentPower().orElse(0));

		this.latestBatteryData = new BatteryData(battery.getChargeMaxCurrent().get(), battery.getVoltage().get());

		// Apply Power Set-Point
		this.applyPowerHandler.apply(this, setActivePower, this.config.controlMode(), this.sum.getGridActivePower(),
				this.getActivePower(), this.getMaxAcImport(), this.getMaxAcExport(), this.power.isPidEnabled());

		// Set Battery Limits
		this.setBatteryLimits(battery);

		// Set General Values
		this.setGeneralValues();
	}

	@Override
	public BatteryInverterConstraint[] getStaticConstraints() throws OpenemsNamedException {
		return new BatteryInverterConstraint[] { //
				new BatteryInverterConstraint("Max AC Import", Phase.ALL, Pwr.ACTIVE, //
						Relationship.GREATER_OR_EQUALS, this.getMaxAcImport().orElse(0)), //
				new BatteryInverterConstraint("Max AC Export", Phase.ALL, Pwr.ACTIVE, //
						Relationship.LESS_OR_EQUALS, this.getMaxAcExport().orElse(0)) //
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
}
