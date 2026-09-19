package io.openems.edge.pytes.battery;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.Unit;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface PytesBattery extends Battery, OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// -----------------------------------------------------------------------
		// Starter battery (TODO - register not yet identified in datasheet)
		// -----------------------------------------------------------------------

		/**
		 * Starter battery voltage. Register not yet identified - kept as a placeholder.
		 * Unit: V
		 */
		STARTER_BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.VOLT)),

		// -----------------------------------------------------------------------
		// Inverter battery port measurements (reg 33133-33138)
		// -----------------------------------------------------------------------

		/**
		 * Battery voltage at the inverter battery port (33133, U16)
		 * Datasheet: 0.1 V -> SCALE_FACTOR_2 -> mV
		 * Used by calculateAndSetBatteryPower() to derive power and set Battery.VOLTAGE.
		 * Unit: mV
		 */
		BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		/**
		 * Battery current magnitude at the inverter port (reg 33134, S16).
		 * Always positive - direction is carried separately in BATTERY_CURRENT_DIRECTION
		 * Datasheet: 0.1 A -> SCALE_FACTOR_2 -> mA
		 * Unit: mA
		 */
		CURRENT_WITHOUT_DIRECTION(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),
		
		/**
		 * Battery current direction (reg 33135, U16).
		 * 0 = charging (power flowing into battery)
		 * 1 = discharging (power flowing out of battery)
		 * Combined with CURRENT_WITHOUT_DIRECTION to derive signed current/power
		 */
		BATTERY_CURRENT_DIRECTION(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)),

		/**
		 * LLC internal DC bus voltage between battery and inverter (reg 33136, U16).
		 * Datasheet: 0.1 V -> SCALE_FACTOR_2 -> mV
		 * Unit: mV
		 */
		LLC_BUS_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		// -----------------------------------------------------------------------
		// BMS reported values (reg 33141-33144)
		// -----------------------------------------------------------------------
		
		/**
		 * Battery voltage as reported by BMS (reg 33141, U16).
		 * The register added in the implementation file by the name VOLTAGE under file name BATTERY
		 * Datasheet: 0.01 V -> SCALE_FACTOR_1 -> mV
		 * Unit: mV
		 */
		BMS_BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		/**
		 * Battery current as reported by BMS (reg 33142, S16).
		 * Signed: Positive = charging, negative = discharging
		 * Datasheet: 0.1 A -> SCALE_FACTOR_2 -> mA
		 * Unit: mA
		 */
		BMS_BATTERY_CURRENT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),
		
		/**
		 * BMS maximum charge current limit (reg 33143, U16).
		 * Dynamically adjusted by BMS based on SoC and temperature
		 * Datasheet: 0.1 A -> SCALE_FACTOR_2 -> mA
		 * Unit: mA
		 */
		BMS_CHARGE_CURRENT_LIMIT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		/**
		 * BMS maximum discharge current limit (reg 33144, U16).
		 * Dynamically adjusted by BMS based on SoC and temperature
		 * Datasheet: 0.1 A -> SCALE_FACTOR_2 -> mA
		 * Unit: mA
		 */
		BMS_DISCHARGE_CURRENT_LIMIT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		// -----------------------------------------------------------------------
		// Battery fault status raw words (reg 33145-33146)
		// kept for logging/diagnostics - individual bits decoded below
		// -----------------------------------------------------------------------

		/**
		 * Raw battery fault status word 01 (reg 33145, U16).
		 * See Appendix 9 and BMS_FAULT01_* channels below for decoded bits
		 */
		BMS_BATTERY_FAULT_STATUS01(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)),

		/**
		 * Raw battery fault status word 02 (reg 33146, U16).
		 * See Appendix 9 and BMS_FAULT02_* channels below for decoded bits
		 */
		BMS_BATTERY_FAULT_STATUS02(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)),

		// -----------------------------------------------------------------------
		// Appendix 9 – Fault Status 01 bits (reg 33145)
		// LV = 3-5K Low Voltage Hybrid models
		// HV = 5-10K High Voltage Hybrid models
		// 0 = No fault, 1 = Fault active
		// -----------------------------------------------------------------------

		/**
		 * Battery voltage too high (protection triggered). LV: Overvoltage protection –
		 * battery voltage exceeded maximum safe limit. HV: Discharge undervoltage –
		 * battery voltage dropped too low during discharge.
		 */
		BMS_FAULT01_OVERVOLTAGE_PRO(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * Battery voltage too low (protection triggered). LV: Undervoltage protection –
		 * battery voltage dropped below minimum safe limit. HV: Core over temperature –
		 * one or more battery cells exceeded max temperature.
		 */
		BMS_FAULT01_UNDERVOLTAGE_PRO(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * Battery temperature too high (protection triggered). LV: Over temperature
		 * protection – battery pack temperature exceeded safe limit. HV: Core under
		 * temperature – one or more battery cells are too cold to operate safely.
		 */
		BMS_FAULT01_OVER_TEMPERATURE_PRO(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * Battery temperature too low (protection triggered). LV: Under temperature
		 * protection – battery pack is too cold to operate safely. HV: Charge
		 * overcurrent – charging current exceeded the BMS maximum charge limit.
		 */
		BMS_FAULT01_UNDER_TEMPERATURE_PRO(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * Battery too hot specifically during charging (protection triggered). LV: Over
		 * temperature charge protection – temperature exceeded safe limit while
		 * charging. HV: Discharge overcurrent – discharge current exceeded the BMS
		 * maximum discharge limit.
		 */
		BMS_FAULT01_OVER_TEMPERATURE_CHARGE_PRO(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * Battery too cold specifically during charging (protection triggered). LV:
		 * Under temperature charge protection – battery too cold to accept charge
		 * safely. HV: Battery internal COM fail – communication failure between BMS and
		 * battery cells/modules.
		 */
		BMS_FAULT01_UNDER_TEMPERATURE_CHARGE_PRO(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * Discharge current exceeded safe limit (protection triggered). LV: Discharge
		 * overcurrent protection – discharge current exceeded the BMS maximum limit.
		 * HV: System reboot – the BMS system has performed or is performing a reboot.
		 */
		BMS_FAULT01_DISCHARGE_OVERCURRENT_PRO(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		// -----------------------------------------------------------------------
		// Appendix 9 – Fault Status 02 bits (reg 33146)
		// LV = 3-5K Low Voltage Hybrid models
		// HV = 5-10K High Voltage Hybrid models
		// 0 = No fault, 1 = Fault active
		// -----------------------------------------------------------------------

		/**
		 * Charge current exceeded safe limit (protection triggered). LV: Charge
		 * overcurrent protection – charging current exceeded the BMS maximum charge
		 * limit. HV: Different core – mismatch detected between battery core modules
		 * (e.g. different capacity or chemistry), which can cause unbalanced operation
		 * and damage.
		 */
		BMS_FAULT02_CHARGE_OVERCURRENT_PRO(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * HV only: Battery system temperature critically low (level 1 warning). The
		 * overall battery system temperature has dropped below the first
		 * low-temperature threshold. May reduce charge/discharge capability to protect
		 * the cells. LV: Reserved – not used on LV models.
		 */
		BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_1(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * HV only: Battery system temperature critically low (level 2 warning). The
		 * overall battery system temperature has dropped below the second, more severe
		 * low-temperature threshold. Likely results in charging being stopped entirely.
		 * LV: Reserved – not used on LV models.
		 */
		BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_2(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * Internal BMS fault or system overheating. LV: BMS internal protection – an
		 * internal BMS error has been detected. Check BMS logs. HV: System high
		 * temperature – overall battery system temperature exceeded safe limit.
		 */
		BMS_FAULT02_BMS_INTERNAL_PRO(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * LV only: Individual battery modules have significantly different
		 * state-of-charge or voltage levels. This can reduce usable capacity and
		 * accelerate cell degradation. Balancing may be needed or a module may be
		 * faulty. HV: Reserved – not used on HV models.
		 */
		BMS_FAULT02_UNBALANCED_MODULES(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * LV only: BMS is requesting a full charge cycle. The BMS has determined the
		 * battery needs to be charged to 100% for calibration or balancing purposes.
		 * This is a request, not a fault — normal operational behaviour. HV: Reserved –
		 * not used on HV models.
		 */
		BMS_FAULT02_FULL_CHARGE_REQUEST(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		/**
		 * LV only: BMS is requesting an immediate forced charge. The BMS has detected
		 * the battery is critically low and is requesting the inverter to begin
		 * charging immediately, overriding normal scheduling or control logic. HV:
		 * Reserved – not used on HV models.
		 */
		BMS_FAULT02_FORCE_CHARGE_REQUEST(Doc.of(BOOLEAN).accessMode(READ_ONLY)),

		// -----------------------------------------------------------------------
		// Battery power (reg 33149)
		// -----------------------------------------------------------------------

		/**
		 * Battery power - signed, calculated programmatically (reg 33149, S32).
		 * Set by calculateAndSetBatteryPower() from:
		 * 		BATTERY_VOLTAGE x CURRENT_WITHOUT_DIRECTION X sign(BATTERY_CURRENT_DIRECTION)
		 * Sign convention: positive = charging, negative = discharging
		 * Datasheet: 1 W -> no converter needed
		 * Unit: W
		 */
		DC_DISCHARGE_POWER(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.WATT)),

		/**
		 * Battery power as read directly from the inverter (reg 33149, S32).
		 * This is the inverter's own computed value — used as a cross-check
		 * against the programmatically calculated DC_DISCHARGE_POWER.
		 * Positive = charging, negative = discharging.
		 * Datasheet: 1 W resolution → no converter needed.
		 * Unit: W
		 */
		DC_DISCHARGE_POWER_UNSIGNED(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.WATT)),
		/**
		 * Backup port load power (reg 33148, 1 W). Not a battery value, but the
		 * register sits inside the battery block that is read every cycle anyway,
		 * so it is mapped here to avoid an extra Modbus frame. Needed by the ESS:
		 * in AC output control the inverter regulates the grid-side port only,
		 * backup loads come on top.
		 */
		BACKUP_LOAD_POWER(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.WATT)),
		;

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	// -----------------------------------------------------------------------
	// Accessor methods - Starter battery (TODO)
	// -----------------------------------------------------------------------

	
	/**
	 * Channel for {@link ChannelId#STARTER_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getStarterBatteryVoltageChannel() {
		return this.channel(ChannelId.STARTER_BATTERY_VOLTAGE);
	}

	/**
	 * Starter battery voltage [V]. See {@link ChannelId#STARTER_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getStarterBatteryVoltage() {
		return this.getStarterBatteryVoltageChannel().value();
	}

	/**
	 * Sets the starter battery voltage (manual setter until Modbus mapping is known).
	 *
	 * @param value the next value
	 */
	public default void _setStarterBatteryVoltage(Integer value) {
		this.getStarterBatteryVoltageChannel().setNextValue(value);
	}

	// -----------------------------------------------------------------------
	// Accessors methods - Inverter battery port (reg 33133-33138)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#BATTERY_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryVoltageChannel() {
		return this.channel(ChannelId.BATTERY_VOLTAGE);
	}

	/**
	 * Battery port voltage [mV]. See {@link ChannelId#BATTERY_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryVoltage() {
		return this.getBatteryVoltageChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#CURRENT_WITHOUT_DIRECTION}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentWithoutDirectionChannel() {
		return this.channel(ChannelId.CURRENT_WITHOUT_DIRECTION);
	}

	/**
	 * Battery current magnitude (no sign) [mA]. See {@link ChannelId#CURRENT_WITHOUT_DIRECTION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentWithoutDirection() {
		return this.getCurrentWithoutDirectionChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BATTERY_CURRENT_DIRECTION}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryCurrentDirectionChannel() {
		return this.channel(ChannelId.BATTERY_CURRENT_DIRECTION);
	}

	/**
	 * 0 = charging, 1 = discharging. See {@link ChannelId#BATTERY_CURRENT_DIRECTION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryCurrentDirection() {
		return this.getBatteryCurrentDirectionChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#LLC_BUS_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getLlcBusVoltageChannel() {
		return this.channel(ChannelId.LLC_BUS_VOLTAGE);
	}

	/**
	 * LLC internal DC bus voltage [mV]. See {@link ChannelId#LLC_BUS_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getLlcBusVoltage() {
		return this.getLlcBusVoltageChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – BMS values (reg 33142–33144)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#BMS_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsBatteryVoltageChannel() {
		return this.channel(ChannelId.BMS_BATTERY_VOLTAGE);
	}

	/**
	 * BMS battery current [mA], signed. See {@link ChannelId#BMS_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsBatteryVoltage() {
		return this.getBmsBatteryVoltageChannel().value();
	}	
	
	/**
	 * Channel for {@link ChannelId#BMS_BATTERY_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsBatteryCurrentChannel() {
		return this.channel(ChannelId.BMS_BATTERY_CURRENT);
	}

	/**
	 * BMS battery current [mA], signed. See {@link ChannelId#BMS_BATTERY_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsBatteryCurrent() {
		return this.getBmsBatteryCurrentChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_CHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsChargeCurrentLimitChannel() {
		return this.channel(ChannelId.BMS_CHARGE_CURRENT_LIMIT);
	}

	/**
	 * BMS max charge current limit [mA]. See {@link ChannelId#BMS_CHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsChargeCurrentLimit() {
		return this.getBmsChargeCurrentLimitChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_DISCHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsDischargeCurrentLimitChannel() {
		return this.channel(ChannelId.BMS_DISCHARGE_CURRENT_LIMIT);
	}

	/**
	 * BMS max discharge current limit [mA]. See {@link ChannelId#BMS_DISCHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsDischargeCurrentLimit() {
		return this.getBmsDischargeCurrentLimitChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Raw fault status words (reg 33145–33146)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#BMS_BATTERY_FAULT_STATUS01}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsBatteryFaultStatus01Channel() {
		return this.channel(ChannelId.BMS_BATTERY_FAULT_STATUS01);
	}

	/**
	 * Raw fault status word 01. See {@link ChannelId#BMS_BATTERY_FAULT_STATUS01}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsBatteryFaultStatus01() {
		return this.getBmsBatteryFaultStatus01Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_BATTERY_FAULT_STATUS02}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsBatteryFaultStatus02Channel() {
		return this.channel(ChannelId.BMS_BATTERY_FAULT_STATUS02);
	}

	/**
	 * Raw fault status word 02. See {@link ChannelId#BMS_BATTERY_FAULT_STATUS02}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsBatteryFaultStatus02() {
		return this.getBmsBatteryFaultStatus02Channel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Fault Status 01 decoded bits (reg 33145)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#BMS_FAULT01_OVERVOLTAGE_PRO}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault01OvervoltageProChannel() {
		return this.channel(ChannelId.BMS_FAULT01_OVERVOLTAGE_PRO);
	}

	/**
	 * true if overvoltage protection active. See {@link ChannelId#BMS_FAULT01_OVERVOLTAGE_PRO}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault01OvervoltagePro() {
		return this.getBmsFault01OvervoltageProChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT01_UNDERVOLTAGE_PRO}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault01UndervoltageProChannel() {
		return this.channel(ChannelId.BMS_FAULT01_UNDERVOLTAGE_PRO);
	}

	/**
	 * true if undervoltage protection active. See {@link ChannelId#BMS_FAULT01_UNDERVOLTAGE_PRO}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault01UndervoltagePro() {
		return this.getBmsFault01UndervoltageProChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT01_OVER_TEMPERATURE_PRO}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault01OverTemperatureProChannel() {
		return this.channel(ChannelId.BMS_FAULT01_OVER_TEMPERATURE_PRO);
	}

	/**
	 * true if over temperature protection active. See {@link ChannelId#BMS_FAULT01_OVER_TEMPERATURE_PRO}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault01OverTemperaturePro() {
		return this.getBmsFault01OverTemperatureProChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT01_UNDER_TEMPERATURE_PRO}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault01UnderTemperatureProChannel() {
		return this.channel(ChannelId.BMS_FAULT01_UNDER_TEMPERATURE_PRO);
	}

	/**
	 * true if under temperature protection active. See {@link ChannelId#BMS_FAULT01_UNDER_TEMPERATURE_PRO}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault01UnderTemperaturePro() {
		return this.getBmsFault01UnderTemperatureProChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT01_OVER_TEMPERATURE_CHARGE_PRO}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault01OverTemperatureChargeProChannel() {
		return this.channel(ChannelId.BMS_FAULT01_OVER_TEMPERATURE_CHARGE_PRO);
	}

	/**
	 * true if over temperature during charge active. See {@link ChannelId#BMS_FAULT01_OVER_TEMPERATURE_CHARGE_PRO}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault01OverTemperatureChargePro() {
		return this.getBmsFault01OverTemperatureChargeProChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT01_UNDER_TEMPERATURE_CHARGE_PRO}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault01UnderTemperatureChargeProChannel() {
		return this.channel(ChannelId.BMS_FAULT01_UNDER_TEMPERATURE_CHARGE_PRO);
	}

	/**
	 * true if under temperature during charge active. See {@link ChannelId#BMS_FAULT01_UNDER_TEMPERATURE_CHARGE_PRO}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault01UnderTemperatureChargePro() {
		return this.getBmsFault01UnderTemperatureChargeProChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT01_DISCHARGE_OVERCURRENT_PRO}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault01DischargeOvercurrentProChannel() {
		return this.channel(ChannelId.BMS_FAULT01_DISCHARGE_OVERCURRENT_PRO);
	}

	/**
	 * true if discharge overcurrent protection active. See {@link ChannelId#BMS_FAULT01_DISCHARGE_OVERCURRENT_PRO}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault01DischargeOvercurrentPro() {
		return this.getBmsFault01DischargeOvercurrentProChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Fault Status 02 decoded bits (reg 33146)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#BMS_FAULT02_CHARGE_OVERCURRENT_PRO}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault02ChargeOvercurrentProChannel() {
		return this.channel(ChannelId.BMS_FAULT02_CHARGE_OVERCURRENT_PRO);
	}

	/**
	 * true if charge overcurrent protection active. See {@link ChannelId#BMS_FAULT02_CHARGE_OVERCURRENT_PRO}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault02ChargeOvercurrentPro() {
		return this.getBmsFault02ChargeOvercurrentProChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_1}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault02SystemLowTemperature1Channel() {
		return this.channel(ChannelId.BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_1);
	}

	/**
	 * true if system low temperature level 1 active. See {@link ChannelId#BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_1}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault02SystemLowTemperature1() {
		return this.getBmsFault02SystemLowTemperature1Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_2}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault02SystemLowTemperature2Channel() {
		return this.channel(ChannelId.BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_2);
	}

	/**
	 * true if system low temperature level 2 active. See {@link ChannelId#BMS_FAULT02_SYSTEM_LOW_TEMPERATURE_2}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault02SystemLowTemperature2() {
		return this.getBmsFault02SystemLowTemperature2Channel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT02_BMS_INTERNAL_PRO}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault02BmsInternalProChannel() {
		return this.channel(ChannelId.BMS_FAULT02_BMS_INTERNAL_PRO);
	}

	/**
	 * true if BMS internal protection active. See {@link ChannelId#BMS_FAULT02_BMS_INTERNAL_PRO}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault02BmsInternalPro() {
		return this.getBmsFault02BmsInternalProChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT02_UNBALANCED_MODULES}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault02UnbalancedModulesChannel() {
		return this.channel(ChannelId.BMS_FAULT02_UNBALANCED_MODULES);
	}

	/**
	 * true if battery modules are unbalanced. See {@link ChannelId#BMS_FAULT02_UNBALANCED_MODULES}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault02UnbalancedModules() {
		return this.getBmsFault02UnbalancedModulesChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT02_FULL_CHARGE_REQUEST}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault02FullChargeRequestChannel() {
		return this.channel(ChannelId.BMS_FAULT02_FULL_CHARGE_REQUEST);
	}

	/**
	 * true if BMS requesting full charge cycle. See {@link ChannelId#BMS_FAULT02_FULL_CHARGE_REQUEST}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault02FullChargeRequest() {
		return this.getBmsFault02FullChargeRequestChannel().value();
	}

	/**
	 * Channel for {@link ChannelId#BMS_FAULT02_FORCE_CHARGE_REQUEST}.
	 *
	 * @return the Channel
	 */
	public default BooleanReadChannel getBmsFault02ForceChargeRequestChannel() {
		return this.channel(ChannelId.BMS_FAULT02_FORCE_CHARGE_REQUEST);
	}

	/**
	 * true if BMS requesting immediate forced charge. See {@link ChannelId#BMS_FAULT02_FORCE_CHARGE_REQUEST}.
	 *
	 * @return the value
	 */
	public default Value<Boolean> getBmsFault02ForceChargeRequest() {
		return this.getBmsFault02ForceChargeRequestChannel().value();
	}

	// -----------------------------------------------------------------------
	// Accessor methods – Battery power (reg 33149)
	// -----------------------------------------------------------------------

	/**
	 * Channel for {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcDischargePowerChannel() {
		return this.channel(ChannelId.DC_DISCHARGE_POWER);
	}

	/**
	 * Battery power [W]. Positive = charging, negative = discharging. See {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcDischargePower() {
		return this.getDcDischargePowerChannel().value();
	}

	/**
	 * Sets the signed battery power calculated from voltage × current × direction.
	 * Called by PytesBatteryImpl.calculateAndSetBatteryPower() every cycle.
	 *
	 * @param value the next value
	 */
	public default void _setDcDischargePower(Integer value) {
		this.getDcDischargePowerChannel().setNextValue(value);
	}

	/**
	 * Channel for {@link ChannelId#DC_DISCHARGE_POWER_UNSIGNED}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcDischargePowerUnsignedChannel() {
		return this.channel(ChannelId.DC_DISCHARGE_POWER_UNSIGNED);
	}

	/**
	 * Battery power raw from inverter [W]. See {@link ChannelId#DC_DISCHARGE_POWER_UNSIGNED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcDischargePowerUnsigned() {
		return this.getDcDischargePowerUnsignedChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BACKUP_LOAD_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBackupLoadPowerChannel() {
		return this.channel(ChannelId.BACKUP_LOAD_POWER);
	}

	/**
	 * Gets the backup port load power in W. See
	 * {@link ChannelId#BACKUP_LOAD_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBackupLoadPower() {
		return this.getBackupLoadPowerChannel().value();
	}

	// -----------------------------------------------------------------------
	// Abstract methods implemented by PytesBatteryImpl
	// -----------------------------------------------------------------------


	/**
	 * Gets the configured maximum charge current.
	 *
	 * @return the maximum charge current in mA
	 */
	int getConfiguredMaxChargeCurrent();

	/**
	 * Gets the configured maximum discharge current.
	 *
	 * @return the maximum discharge current in mA
	 */
	int getConfiguredMaxDischargeCurrent();
}
