package io.openems.edge.goodwe.batteryinverter;

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
import org.osgi.service.metatype.annotations.Designate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OptionsEnum;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.batteryinverter.api.BatteryInverterConstraint;
import io.openems.edge.batteryinverter.api.HybridManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.ManagedSymmetricBatteryInverter;
import io.openems.edge.batteryinverter.api.SymmetricBatteryInverter;
import io.openems.edge.bridge.modbus.api.BridgeModbus;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.common.type.TypeUtils;
import io.openems.edge.ess.api.ApplyPowerContext;
import io.openems.edge.ess.power.api.Phase;
import io.openems.edge.ess.power.api.Power;
import io.openems.edge.ess.power.api.Pwr;
import io.openems.edge.ess.power.api.Relationship;
import io.openems.edge.goodwe.common.AbstractGoodWe;
import io.openems.edge.goodwe.common.ApplyPowerHandler;
import io.openems.edge.goodwe.common.GoodWe;
import io.openems.edge.goodwe.common.enums.EmsPowerMode;
import io.openems.edge.goodwe.common.enums.EnableCurve;
import io.openems.edge.goodwe.common.enums.FixedPowerFactor;
import io.openems.edge.timedata.api.Timedata;

@Designate(ocd = Config.class, factory = true)
@Component(//
		name = "GoodWe.BatteryInverter", //
		immediate = true, //
		configurationPolicy = ConfigurationPolicy.REQUIRE //
) //
public class GoodWeBatteryInverterImpl extends AbstractGoodWe
		implements GoodWeBatteryInverter, GoodWe, HybridManagedSymmetricBatteryInverter,
		ManagedSymmetricBatteryInverter, SymmetricBatteryInverter, OpenemsComponent {

	private static final int MAX_DC_CURRENT = 25; // [A]

	private final Logger log = LoggerFactory.getLogger(GoodWeBatteryInverterImpl.class);
	private final ApplyPowerHandler applyPowerHandler = new ApplyPowerHandler();

	@Reference(policy = ReferencePolicy.DYNAMIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.OPTIONAL)
	private volatile Timedata timedata = null;

	@Reference
	protected ConfigurationAdmin cm;

	@Reference
	private Power power;

	// Fenecon Home Battery Static module min voltage, used to calculate battery
	// module number per tower
	// TODO get from Battery
	private static final int MODULE_MIN_VOLTAGE = 42;

	/**
	 * Holds the latest known Charge-Max-Current. Updated in
	 * {@link #run(Battery, int, int)}.
	 */
	private Value<Integer> lastChargeMaxCurrent = null;

	private Config config;

	@Reference(policy = ReferencePolicy.STATIC, policyOption = ReferencePolicyOption.GREEDY, cardinality = ReferenceCardinality.MANDATORY)
	protected void setModbus(BridgeModbus modbus) {
		super.setModbus(modbus);
	}

	@Activate
	private void activate(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.activate(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(config);
	}

	@Modified
	private void modified(ComponentContext context, Config config) throws OpenemsNamedException {
		if (super.modified(context, config.id(), config.alias(), config.enabled(), config.modbusUnitId(), this.cm,
				"Modbus", config.modbus_id())) {
			return;
		}
		this.applyConfig(config);
	}

	@Deactivate
	protected void deactivate() {
		super.deactivate();
	}

	public GoodWeBatteryInverterImpl() throws OpenemsNamedException {
		super(//
				SymmetricBatteryInverter.ChannelId.ACTIVE_POWER, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_POWER, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_CHARGE_ENERGY, //
				SymmetricBatteryInverter.ChannelId.ACTIVE_DISCHARGE_ENERGY, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_CHARGE_ENERGY, //
				HybridManagedSymmetricBatteryInverter.ChannelId.DC_DISCHARGE_ENERGY, //
				OpenemsComponent.ChannelId.values(), //
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

	/**
	 * Apply the configuration on Activate and Modified.
	 * 
	 * <p>
	 * Feed In Power Setting consist of: Installed inverter country, feeding method:
	 * whether according to the power factor or power and frequency. In addition, it
	 * consist backup power availability.
	 * 
	 * @param config Configuration parameters.
	 * @throws OpenemsNamedException on error
	 */
	private void applyConfig(Config config) throws OpenemsNamedException {
		this.config = config;

		/**
		 * Should be always set to general mode
		 * <ul>
		 * <li>0x00: General Mode: Self use
		 * <li>0x01: Off-grid Mode
		 * <li>0x02: Backup Mode
		 * <li>0x03: Economic Mode
		 * </ul>
		 */
		this.writeToChannel(GoodWe.ChannelId.SELECT_WORK_MODE, 0);

		// country setting
		this.writeToChannel(GoodWe.ChannelId.SAFETY_COUNTRY_CODE, config.safetyCountry());

		// Backup Power on / off
		this.writeToChannel(GoodWe.ChannelId.BACK_UP_ENABLE, config.backupEnable());

		// Feed-in limitation on / off
		this.writeToChannel(GoodWe.ChannelId.FEED_POWER_ENABLE, config.feedPowerEnable());

		// Feed-in limitation
		this.writeToChannel(GoodWe.ChannelId.FEED_POWER_PARA, config.feedPowerPara());

		// Set to feed in power settings to default
		this.writeToChannel(GoodWe.ChannelId.ENABLE_CURVE_QU, EnableCurve.DISABLE);
		this.writeToChannel(GoodWe.ChannelId.ENABLE_CURVE_PU, EnableCurve.DISABLE);

		// Feed-in settings
		switch (config.setfeedInPowerSettings()) {
		case QU_ENABLE_CURVE:
			this.writeToChannel(GoodWe.ChannelId.LOCK_IN_POWER_QU, 200);
			this.writeToChannel(GoodWe.ChannelId.LOCK_OUT_POWER_QU, 50);
			this.writeToChannel(GoodWe.ChannelId.V1_VOLTAGE, 214);
			this.writeToChannel(GoodWe.ChannelId.V1_VALUE, 436);
			this.writeToChannel(GoodWe.ChannelId.V2_VOLTAGE, 223);
			this.writeToChannel(GoodWe.ChannelId.V2_VALUE, 0);
			this.writeToChannel(GoodWe.ChannelId.V3_VOLTAGE, 237);
			this.writeToChannel(GoodWe.ChannelId.V3_VALUE, 0);
			this.writeToChannel(GoodWe.ChannelId.V4_VOLTAGE, 247);
			this.writeToChannel(GoodWe.ChannelId.V4_VALUE, 65009);
			break;
		case PU_ENABLE_CURVE:
			this.writeToChannel(GoodWe.ChannelId.POINT_A_VALUE, 2000);
			this.writeToChannel(GoodWe.ChannelId.POINT_A_PF, 0);
			this.writeToChannel(GoodWe.ChannelId.POINT_B_VALUE, 2000);
			this.writeToChannel(GoodWe.ChannelId.POINT_B_PF, 0);
			this.writeToChannel(GoodWe.ChannelId.POINT_C_VALUE, 2000);
			this.writeToChannel(GoodWe.ChannelId.POINT_C_PF, 0);
			break;
		case LAGGING_0_80:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_80);
			break;
		case LAGGING_0_81:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_81);
			break;
		case LAGGING_0_82:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_82);
			break;
		case LAGGING_0_83:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_83);
			break;
		case LAGGING_0_84:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_84);
			break;
		case LAGGING_0_85:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_85);
			break;
		case LAGGING_0_86:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_86);
			break;
		case LAGGING_0_87:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_87);
			break;
		case LAGGING_0_88:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_88);
			break;
		case LAGGING_0_89:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_89);
			break;
		case LAGGING_0_90:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_90);
			break;
		case LAGGING_0_91:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_91);
			break;
		case LAGGING_0_92:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_92);
			break;
		case LAGGING_0_93:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_93);
			break;
		case LAGGING_0_94:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_94);
			break;
		case LAGGING_0_95:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_95);
			break;
		case LAGGING_0_96:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_96);
			break;
		case LAGGING_0_97:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_97);
			break;
		case LAGGING_0_98:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_98);
			break;
		case LAGGING_0_99:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LAGGING_0_99);
			break;
		case LEADING_0_80:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_80);
			break;
		case LEADING_0_81:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_81);
			break;
		case LEADING_0_82:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_82);
			break;
		case LEADING_0_83:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_83);
			break;
		case LEADING_0_84:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_84);
			break;
		case LEADING_0_85:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_85);
			break;
		case LEADING_0_86:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_86);
			break;
		case LEADING_0_87:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_87);
			break;
		case LEADING_0_88:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_88);
			break;
		case LEADING_0_89:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_89);
			break;
		case LEADING_0_90:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_90);
			break;
		case LEADING_0_91:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_91);
			break;
		case LEADING_0_92:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_92);
			break;
		case LEADING_0_93:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_93);
			break;
		case LEADING_0_94:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_94);
			break;
		case LEADING_0_95:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_95);
			break;
		case LEADING_0_96:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_96);
			break;
		case LEADING_0_97:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_97);
			break;
		case LEADING_0_98:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_98);
			break;
		case LEADING_0_99:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_0_99);
			break;
		case LEADING_1:
			this.writeToChannel(GoodWe.ChannelId.FIXED_POWER_FACTOR, FixedPowerFactor.LEADING_1);
			break;
		case UNDEFINED:
			break;
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
		Value<Integer> bmsChargeMaxCurrent = this.getBmsChargeMaxCurrent();
		Value<Integer> bmsDischargeMaxCurrent = this.getBmsDischargeMaxCurrent();
		Value<Integer> bmsChargeMaxVoltage = this.getBmsChargeMaxVoltage();
		Value<Integer> bmsDischargeMinVoltage = this.getBmsDischargeMinVoltage();

		Channel<Integer> bmsSocUnderMinChannel = this.channel(GoodWe.ChannelId.BMS_SOC_UNDER_MIN);
		Value<Integer> bmsSocUnderMin = bmsSocUnderMinChannel.value();
		Channel<Integer> bmsOfflineSocUnderMinChannel = this.channel(GoodWe.ChannelId.BMS_OFFLINE_SOC_UNDER_MIN);
		Value<Integer> bmsOfflineSocUnderMin = bmsOfflineSocUnderMinChannel.value();
		// TODO: this should not be required anymore with latest (beta) firmware
		// For example, the nominal voltage of the battery rack is 240V, then
		// BattStrings shall be 5. And max charge voltage range of the battery shall be
		// within in 5*50V~6*50V (250V~300V), and min discharge voltage of battery shall
		// be within 5*40V~5*48V (200V~240V). of course please make sure the battery
		// real acceptable charge/discharge limitation is also in this range.
//		Integer setChargeMaxVoltage = TypeUtils.fitWithin(//
//				TypeUtils.multiply(setBatteryStrings, 50), //
//				TypeUtils.multiply(TypeUtils.sum(setBatteryStrings, 1), 50), //
//				battery.getChargeMaxVoltage().get());

		Integer setBatteryStrings = TypeUtils.divide(battery.getDischargeMinVoltage().get(), MODULE_MIN_VOLTAGE);
		Integer setChargeMaxCurrent = MAX_DC_CURRENT;
		Integer setDischargeMaxCurrent = MAX_DC_CURRENT;
		Integer setChargeMaxVoltage = battery.getChargeMaxVoltage().orElse(0);
		Integer setDischargeMinVoltage = battery.getDischargeMinVoltage().orElse(0);
		Integer setSocUnderMin = 0; // [0-100]; 0 MinSoc = 100 DoD
		Integer setOfflineSocUnderMin = 0; // [0-100]; 0 MinSoc = 100 DoD
		if ((bmsChargeMaxCurrent.isDefined() && !Objects.equals(bmsChargeMaxCurrent.get(), setChargeMaxCurrent))
				|| (bmsDischargeMaxCurrent.isDefined()
						&& !Objects.equals(bmsDischargeMaxCurrent.get(), setDischargeMaxCurrent))
				|| (bmsSocUnderMin.isDefined() && !Objects.equals(bmsSocUnderMin.get(), setSocUnderMin))

		// TODO bmsOfflineSocUnderMin change is not applied!

//				|| (bmsOfflineSocUnderMin.isDefined()
//						&& !Objects.equals(bmsOfflineSocUnderMin.get(), setOfflineSocUnderMin))
		// || (bmsChargeMaxVoltage.isDefined() &&
		// !Objects.equals(bmsChargeMaxVoltage.get(), setChargeMaxVoltage))
//				|| (bmsDischargeMinVoltage.isDefined()
//						&& !Objects.equals(bmsDischargeMinVoltage.get(), setDischargeMinVoltage))
		// TODO: it is not clear to me, why ChargeMaxVoltage is set to 250 but
		// frequently gets reset to 210. This is with first beta firmware on fems888.
		// Disabling Voltage-Check because of this.
		) {
			// Update is required
			this.logInfo(this.log, "Update for PV-Master BMS Registers is required." //
					+ " Voltages" //
					+ " [Discharge" + bmsDischargeMinVoltage.get() + " -> " + setDischargeMinVoltage + "]" //
					+ " [Charge" + bmsChargeMaxVoltage + " -> " + setChargeMaxVoltage + "]" //
					+ " Currents " //
					+ " [Charge " + bmsChargeMaxCurrent.get() + " -> " + setChargeMaxCurrent + "]" //
					+ " [Discharge " + bmsDischargeMaxCurrent.get() + " -> " + setDischargeMaxCurrent + "]" //
					+ " MinSoc " //
					+ " [" + bmsSocUnderMin.get() + " -> " + setSocUnderMin + "] " //
					+ " [" + bmsOfflineSocUnderMin.get() + " -> " + setOfflineSocUnderMin + "]");

			this.writeToChannel(GoodWe.ChannelId.BATTERY_PROTOCOL_ARM, 287); // EMS-Mode

			// Registers 45350
			this.writeToChannel(GoodWe.ChannelId.BMS_LEAD_CAPACITY, 200); // TODO: calculate value
			this.writeToChannel(GoodWe.ChannelId.BMS_STRINGS, setBatteryStrings); // [4-12]
			// TODO is writing BMS_STRINGS and BMS_LEAD_CAPACITY still required with latest
			// firmware?
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
				TypeUtils.orElse(preprocessAmpereValue47900(battery.getChargeMaxCurrent()), 0));
		this.writeToChannel(GoodWe.ChannelId.WBMS_DISCHARGE_MIN_VOLTAGE, battery.getDischargeMinVoltage().orElse(0));
		this.writeToChannel(GoodWe.ChannelId.WBMS_DISCHARGE_MAX_CURRENT,
				TypeUtils.orElse(preprocessAmpereValue47900(battery.getDischargeMaxCurrent()), 0));
		this.writeToChannel(GoodWe.ChannelId.WBMS_VOLTAGE, battery.getVoltage().orElse(0));
		this.writeToChannel(GoodWe.ChannelId.WBMS_CURRENT, TypeUtils.abs(battery.getCurrent().orElse(0)));
		// Set SoC within [1;100] to avoid force-charge internally by PCS at 0 %
		this.writeToChannel(GoodWe.ChannelId.WBMS_SOC, TypeUtils.fitWithin(1, 100, battery.getSoc().orElse(1)));
		this.writeToChannel(GoodWe.ChannelId.WBMS_SOH, battery.getSoh().orElse(100));
		this.writeToChannel(GoodWe.ChannelId.WBMS_TEMPERATURE, TypeUtils.orElse(
				TypeUtils.sum(battery.getMaxCellTemperature().get(), battery.getMinCellTemperature().get()), 0));
		this.writeToChannel(GoodWe.ChannelId.WBMS_WARNING_CODE, 0);
		this.writeToChannel(GoodWe.ChannelId.WBMS_ALARM_CODE, 0);
		this.writeToChannel(GoodWe.ChannelId.WBMS_STATUS, 0);
		this.writeToChannel(GoodWe.ChannelId.WBMS_DISABLE_TIMEOUT_DETECTION, 0);
	}

	private static Integer preprocessAmpereValue47900(Value<Integer> v) {
		Integer value = v.get();
		value = TypeUtils.fitWithin(0, MAX_DC_CURRENT, value);
		return value;
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
		// TODO logic is insufficient
		if (this.lastChargeMaxCurrent == null || !this.lastChargeMaxCurrent.isDefined()
				|| this.lastChargeMaxCurrent.get() >= MAX_DC_CURRENT) {
			return null;
		}
		Integer productionPower = this.calculatePvProduction();
		return productionPower;
	}

	@Override
	public void setStartStop(StartStop value) throws OpenemsNamedException {
		// GoodWe is always started. This has no effect.
	}

	@Override
	public void run(Battery battery, int setActivePower, int setReactivePower, ApplyPowerContext context)
			throws OpenemsNamedException {
		// Calculate ActivePower and Energy values.
		this.updatePowerAndEnergyChannels();
		this.lastChargeMaxCurrent = battery.getChargeMaxCurrent();

		// Calculate and store Max-AC-Export and -Import for use in
		// getStaticConstraints()
		Integer maxDcChargePower = /* can be negative for force-discharge */
				TypeUtils.multiply(//
						/* Charge-Max-Current */ this.getBmsChargeMaxCurrent().get(), //
						/* Battery Voltage */ battery.getVoltage().get());
		int pvProduction = TypeUtils.min(0, this.calculatePvProduction());
		this._setMaxAcImport(TypeUtils.multiply(/* negate */ -1, //
				TypeUtils.subtract(maxDcChargePower,
						TypeUtils.min(maxDcChargePower /* avoid negative number for `subtract` */, pvProduction))));
		this._setMaxAcExport(TypeUtils.sum(//
				/* Max DC-Discharge-Power */ TypeUtils.multiply(//
						/* Discharge-Max-Current */ this.getBmsDischargeMaxCurrent().get(), //
						/* Battery Voltage */ battery.getVoltage().get()),
				/* PV Production */ pvProduction));

		if (this.config.blockWrites()) {
			return;
		}

		if (this.config.emsPowerMode() != EmsPowerMode.UNDEFINED && this.config.emsPowerSet() >= 0) {
			System.out.println("Static " + this.config.emsPowerMode() + "[" + this.config.emsPowerSet() + "]");
			IntegerWriteChannel emsPowerSetChannel = this.channel(GoodWe.ChannelId.EMS_POWER_SET);
			emsPowerSetChannel.setNextWriteValue(this.config.emsPowerSet());
			EnumWriteChannel emsPowerModeChannel = this.channel(GoodWe.ChannelId.EMS_POWER_MODE);
			emsPowerModeChannel.setNextWriteValue(this.config.emsPowerMode());

		} else {
			// Apply Power Set-Point
			this.applyPowerHandler.apply(this, false /* read-only mode is never true */, setActivePower, context);
		}

		// Set Battery Limits
		this.setBatteryLimits(battery);
	}

	@Override
	public BatteryInverterConstraint[] getStaticConstraints() throws OpenemsNamedException {
		if (this.config.emsPowerMode() != EmsPowerMode.UNDEFINED && this.config.emsPowerSet() >= 0) {
			// Manual EMS Settings active
			return new BatteryInverterConstraint[] { //
					new BatteryInverterConstraint("Manual Override", Phase.ALL, Pwr.ACTIVE, //
							Relationship.EQUALS, 0), //
					new BatteryInverterConstraint("Manual Override", Phase.ALL, Pwr.REACTIVE, //
							Relationship.EQUALS, 0), //
			};
		}

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
}
