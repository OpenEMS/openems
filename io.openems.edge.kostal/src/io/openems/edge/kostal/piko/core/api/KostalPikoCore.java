package io.openems.edge.kostal.piko.core.api;

import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.kostal.piko.charger.KostalPikoCharger;
import io.openems.edge.kostal.piko.ess.KostalPikoEss;
import io.openems.edge.kostal.piko.gridmeter.KostalPikoGridMeter;

public interface KostalPikoCore {

	public void setEss(KostalPikoEss ess);

	/**
	 * Unregister the {@link KostalPikoEss}.
	 * 
	 * @param ess the {@link KostalPikoEss}
	 */
	public void unsetEss(KostalPikoEss ess);

	public void setCharger(KostalPikoCharger charger);

	/**
	 * Unregister the {@link KostalPikoCharger}.
	 * 
	 * @param charger the {@link KostalPikoCharger}
	 */
	public void unsetCharger(KostalPikoCharger charger);

	public void setGridMeter(KostalPikoGridMeter charger);

	/**
	 * Unregister the {@link KostalPikoGridMeter}.
	 * 
	 * @param charger the {@link KostalPikoGridMeter}
	 */
	public void unsetGridMeter(KostalPikoGridMeter charger);

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		UNABLE_TO_READ_DATA(Doc.of(Level.FAULT)), //

		/*
		 * Core
		 */
		INVERTER_NAME(Doc.of(OpenemsType.STRING)), //
		ARTICLE_NUMBER(Doc.of(OpenemsType.STRING)), //
		INVERTER_SERIAL_NUMBER(Doc.of(OpenemsType.STRING)), //
		FIRMWARE_VERSION(Doc.of(OpenemsType.STRING)), //
		HARDWARE_VERSION(Doc.of(OpenemsType.STRING)), //
		KOMBOARD_VERSION(Doc.of(OpenemsType.STRING)), //
		PARAMETER_VERSION(Doc.of(OpenemsType.STRING)), //
		COUNTRY_NAME(Doc.of(OpenemsType.STRING)), //
		INVERTER_OPERATING_STATUS(Doc.of(OpenemsType.STRING)), //
		INVERTER_TYPE_NAME(Doc.of(OpenemsType.STRING)), //
		NUMBER_OF_STRING(Doc.of(OpenemsType.INTEGER)), //
		NUMBER_OF_PHASES(Doc.of(OpenemsType.INTEGER)), //
		POWER_ID(Doc.of(OpenemsType.INTEGER)), //
		PRESENT_ERROR_EVENT_CODE_1(Doc.of(OpenemsType.INTEGER)), //
		PRESENT_ERROR_EVENT_CODE_2(Doc.of(OpenemsType.INTEGER)), //
		FEED_IN_TIME(Doc.of(OpenemsType.INTEGER)), //
		INVERTER_STATUS(Doc.of(OpenemsType.INTEGER)), //
		ADDRESS_MODBUS_RTU(Doc.of(OpenemsType.INTEGER)), //
		BAUDRATE_INDEX_MODBUS_RTU(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP1(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP2(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP3(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP4(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_1(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_2(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_3(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_4(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_1(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_2(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_3(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_4(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_1(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_2(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_3(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_4(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_1(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_2(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_3(Doc.of(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_4(Doc.of(OpenemsType.INTEGER)), //

		FEED_IN_STATUS(Doc.of(OpenemsType.BOOLEAN)), //
		SETTING_AUTO_IP(Doc.of(OpenemsType.BOOLEAN)), //
		SETTING_MANUAL_EXTERNAL_ROUTER(Doc.of(OpenemsType.BOOLEAN)), //
		PRELOAD_MODBUS_RTU(Doc.of(OpenemsType.BOOLEAN)), //
		TERMINATION_MODBUS_RTU(Doc.of(OpenemsType.BOOLEAN)), //

		/*
		 * ESS
		 */
		BATTERY_CURRENT_DIRECTION(Doc.of(BatteryCurrentDirection.values())), //
		BATTERY_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		BATTERY_VOLTAGE(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		BATTERY_TEMPERATURE(Doc.of(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)), //

		/*
		 * PV Charger
		 */
		OVERALL_DC_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		OVERALL_DC_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		DC_CURRENT_STRING_1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		DC_VOLTAGE_STRING_1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		DC_POWER_STRING_1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		DC_CURRENT_STRING_2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		DC_VOLTAGE_STRING_2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		DC_POWER_STRING_2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		DC_CURRENT_STRING_3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		DC_VOLTAGE_STRING_3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		DC_POWER_STRING_3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //

		/*
		 * Grid
		 */
		GRID_AC_P_TOTAL(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //

		ACTUAL_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		AC_VOLTAGE_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		AC_VOLTAGE_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		AC_VOLTAGE_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		AC_CURRENT_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		AC_CURRENT_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		AC_CURRENT_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		AC_POWER_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		AC_POWER_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		AC_POWER_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		POWER_LIMITATION_OF_EVU(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)), //
		GRID_FREQUENCY(Doc.of(OpenemsType.FLOAT).unit(Unit.HERTZ)), //
		COSINUS_PHI(Doc.of(OpenemsType.FLOAT)), //
		HOME_CONSUMPTION_PV(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_BAT(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_GRID(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CURRENT_FROM_EXT_SENSOR_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		HOME_POWER_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_L1(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CURRENT_FROM_EXT_SENSOR_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		HOME_POWER_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_L2(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CURRENT_FROM_EXT_SENSOR_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		HOME_POWER_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_L3(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_TOTAL_POWER(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_SELF_CONSUMPTION_TOTAL(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT)), //
		ISOLATION_RESISTOR(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOOHM)), //
		MAX_RESIDUAL_CURRENT(Doc.of(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		ANALOG_INPUT_CH_1(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		ANALOG_INPUT_CH_2(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		ANALOG_INPUT_CH_3(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		ANALOG_INPUT_CH_4(Doc.of(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		YIELD_TOTAL(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS)), //
		YIELD_DAY(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS)), //
		HOME_CONSUMPTION_TOTAL(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS)), //
		HOME_CONSUMPTION_DAY(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS)), //
		SELF_CONSUMPTION_TOTAL(Doc.of(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS)), //
		SELF_CONSUMPTION_DAY(Doc.of(OpenemsType.FLOAT).unit(Unit.WATT_HOURS)), //
		SELF_CONSUMPTION_RATE_TOTAL(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)), //
		SELF_CONSUMPTION_RATE_DAY(Doc.of(OpenemsType.FLOAT).unit(Unit.PERCENT)), //
		DEGREE_OF_SELF_SUFFICIENCY_DAY(Doc.of(OpenemsType.FLOAT)), //
		DEGREE_OF_SELF_SUFFICIENCY_TOTAL(Doc.of(OpenemsType.FLOAT));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
}
