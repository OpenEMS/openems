package io.openems.edge.pytes.battery;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.Unit;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface PytesBattery extends Battery, OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		// TODO
		STARTER_BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.VOLT)),

		/**
		 * BMS max charge current limit The maximum current the BMS allows the inverter
		 * to charge the battery with. 0.1A resolution -> mA
		 */
		BMS_CHARGE_CURRENT_LIMIT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		/**
		 * BMS max discharge current limit The maximum current the BMS allows the
		 * inverter to draw from the battery. 0.1A resolution -> mA
		 */
		BMS_DISCHARGE_CURRENT_LIMIT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		/**
		 * Raw battery fault status word 01 See appendix 9 for bit definitions
		 * Individuals bits are decoded into channels below
		 */
		BMS_BATTERY_FAULT_STATUS01(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)),

		/**
		 * Raw battery fault status word 02 See appendix 9 for bit definitions
		 * Individuals bits are decoded into channels below
		 */
		BMS_BATTERY_FAULT_STATUS02(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)),

		LLC_BUS_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.VOLT)),

		BACKUP_AC_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		BACKUP_AC_CURRENT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.AMPERE)),

		BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		BMS_BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIVOLT)),

		BMS_BATTERY_CURRENT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

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

		/*
		 * In this register current has no direction. So it can´t be used for main
		 * battery class
		 */
		CURRENT_WITHOUT_DIRECTION(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		/*
		 * In this register current has no direction. So it can´t be used for main
		 * battery class
		 */
		BATTERY_CURRENT_DIRECTION(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
		),

		/**
		 * Battery Power / DC Discharge Power. Positive = charging, negative =
		 * discharging. Datasheet: 1 W resolution. Unit: W
		 */
		DC_DISCHARGE_POWER(Doc.of(INTEGER)//
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

	/**
	 * Gets the Channel for {@link ChannelId#STARTER_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getStarterBatteryVoltageChannel() {
		return this.channel(ChannelId.STARTER_BATTERY_VOLTAGE);
	}

	/**
	 * Gets the Starter Battery Voltage in [V]. See
	 * {@link ChannelId#STARTER_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getStarterBatteryVoltage() {
		return this.getStarterBatteryVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#STARTER_BATTERY_VOLTAGE} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setStarterBatteryVoltage(Integer value) {
		this.getStarterBatteryVoltageChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BMS_CHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsChargeCurrentLimitChannel() {
		return this.channel(ChannelId.BMS_CHARGE_CURRENT_LIMIT);
	}

	/**
	 * Gets the BMS Charge Current Limit in [mA]. See
	 * {@link ChannelId#BMS_CHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsChargeCurrentLimit() {
		return this.getBmsChargeCurrentLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#BMS_CHARGE_CURRENT_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBmsChargeCurrentLimit(Integer value) {
		this.getBmsChargeCurrentLimitChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BMS_DISCHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsDischargeCurrentLimitChannel() {
		return this.channel(ChannelId.BMS_DISCHARGE_CURRENT_LIMIT);
	}

	/**
	 * Gets the BMS Discharge Current Limit in [mA]. See
	 * {@link ChannelId#BMS_DISCHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsDischargeCurrentLimit() {
		return this.getBmsDischargeCurrentLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#BMS_DISCHARGE_CURRENT_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBmsDischargeCurrentLimit(Integer value) {
		this.getBmsDischargeCurrentLimitChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BMS_BATTERY_FAULT_STATUS01}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsBatteryFaultStatus01Channel() {
		return this.channel(ChannelId.BMS_BATTERY_FAULT_STATUS01);
	}

	/**
	 * Gets the BMS Battery Fault Status 01. See
	 * {@link ChannelId#BMS_BATTERY_FAULT_STATUS01}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsBatteryFaultStatus01() {
		return this.getBmsBatteryFaultStatus01Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#BMS_BATTERY_FAULT_STATUS01} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBmsBatteryFaultStatus01(Integer value) {
		this.getBmsBatteryFaultStatus01Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BMS_BATTERY_FAULT_STATUS02}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBmsBatteryFaultStatus02Channel() {
		return this.channel(ChannelId.BMS_BATTERY_FAULT_STATUS02);
	}

	/**
	 * Gets the BMS Battery Fault Status 02. See
	 * {@link ChannelId#BMS_BATTERY_FAULT_STATUS02}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsBatteryFaultStatus02() {
		return this.getBmsBatteryFaultStatus02Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#BMS_BATTERY_FAULT_STATUS02} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setBmsBatteryFaultStatus02(Integer value) {
		this.getBmsBatteryFaultStatus02Channel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDcDischargePowerChannel() {
		return this.channel(ChannelId.DC_DISCHARGE_POWER);
	}

	/**
	 * Gets the DC Discharge Power in [W]. See {@link ChannelId#DC_DISCHARGE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDcDischargePower() {
		return this.getDcDischargePowerChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#DC_DISCHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDcDischargePower(Integer value) {
		this.getDcDischargePowerChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_CURRENT_DIRECTION}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryCurrentDirectionChannel() {
		return this.channel(ChannelId.BATTERY_CURRENT_DIRECTION);
	}

	/**
	 * Gets the Battery Current Direction. See
	 * {@link ChannelId#BATTERY_CURRENT_DIRECTION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryCurrentDirection() {
		return this.getBatteryCurrentDirectionChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_WITHOUT_DIRECTION}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentWithoutDirectionChannel() {
		return this.channel(ChannelId.CURRENT_WITHOUT_DIRECTION);
	}

	/**
	 * Gets the Current without Direction. See
	 * {@link ChannelId#CURRENT_WITHOUT_DIRECTION}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentWithoutDirection() {
		return this.getCurrentWithoutDirectionChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryVoltageChannel() {
		return this.channel(ChannelId.BATTERY_VOLTAGE);
	}

	/**
	 * Gets the Voltage. See {@link ChannelId#BATTERY_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryVoltage() {
		return this.getBatteryVoltageChannel().value();
	}

	void setMinSocPercentage(int minSocPercentage);

	int getConfiguredMaxChargeCurrent();

	int getConfiguredMaxDischargeCurrent();

}
