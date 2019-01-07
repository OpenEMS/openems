package io.openems.edge.kostal.piko.core.api;

import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.doc.Doc;
import io.openems.edge.common.channel.doc.Unit;
import io.openems.edge.kostal.piko.charger.KostalPikoCharger;
import io.openems.edge.kostal.piko.ess.KostalPikoEss;
import io.openems.edge.kostal.piko.gridmeter.KostalPikoGridMeter;

public interface KostalPikoCore {

	public void setEss(KostalPikoEss ess);

	public void unsetEss(KostalPikoEss ess);

	public void setCharger(KostalPikoCharger charger);

	public void unsetCharger(KostalPikoCharger charger);

	public void setGridMeter(KostalPikoGridMeter charger);

	public void unsetGridMeter(KostalPikoGridMeter charger);

	public enum ChannelId implements io.openems.edge.common.channel.doc.ChannelId {
		/*
		 * Core
		 */
		INVERTER_NAME(new Doc().type(OpenemsType.STRING)), //
		ARTICLE_NUMBER(new Doc().type(OpenemsType.STRING)), //
		INVERTER_SERIAL_NUMBER(new Doc().type(OpenemsType.STRING)), //
		FIRMWARE_VERSION(new Doc().type(OpenemsType.STRING)), //
		HARDWARE_VERSION(new Doc().type(OpenemsType.STRING)), //
		KOMBOARD_VERSION(new Doc().type(OpenemsType.STRING)), //
		PARAMETER_VERSION(new Doc().type(OpenemsType.STRING)), //
		COUNTRY_NAME(new Doc().type(OpenemsType.STRING)), //
		INVERTER_OPERATING_STATUS(new Doc().type(OpenemsType.STRING)), //
		INVERTER_TYPE_NAME(new Doc().type(OpenemsType.STRING)), //
		NUMBER_OF_STRING(new Doc().type(OpenemsType.INTEGER)), //
		NUMBER_OF_PHASES(new Doc().type(OpenemsType.INTEGER)), //
		POWER_ID(new Doc().type(OpenemsType.INTEGER)), //
		PRESENT_ERROR_EVENT_CODE_1(new Doc().type(OpenemsType.INTEGER)), //
		PRESENT_ERROR_EVENT_CODE_2(new Doc().type(OpenemsType.INTEGER)), //
		FEED_IN_TIME(new Doc().type(OpenemsType.INTEGER)), //
		INVERTER_STATUS(new Doc().type(OpenemsType.INTEGER)), //
		ADDRESS_MODBUS_RTU(new Doc().type(OpenemsType.INTEGER)), //
		BAUDRATE_INDEX_MODBUS_RTU(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP1(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP2(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP3(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP4(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_1(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_2(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_3(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_SUBNET_MASK_4(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_1(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_2(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_3(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_GATEWAY_4(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_1(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_2(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_3(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_FIRST_4(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_1(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_2(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_3(new Doc().type(OpenemsType.INTEGER)), //
		SETTING_MANUAL_IP_DNS_SECOND_4(new Doc().type(OpenemsType.INTEGER)), //

		FEED_IN_STATUS(new Doc().type(OpenemsType.BOOLEAN)), //
		SETTING_AUTO_IP(new Doc().type(OpenemsType.BOOLEAN)), //
		SETTING_MANUAL_EXTERNAL_ROUTER(new Doc().type(OpenemsType.BOOLEAN)), //
		PRELOAD_MODBUS_RTU(new Doc().type(OpenemsType.BOOLEAN)), //
		TERMINATION_MODBUS_RTU(new Doc().type(OpenemsType.BOOLEAN)), //

		/*
		 * ESS
		 */
		BATTERY_CURRENT_DIRECTION(new Doc().options(BatteryCurrentDirection.values()).type(OpenemsType.FLOAT)), //
		BATTERY_CURRENT(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		BATTERY_VOLTAGE(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		BATTERY_TEMPERATURE(new Doc().type(OpenemsType.FLOAT).unit(Unit.DEGREE_CELSIUS)), //

		/*
		 * PV Charger
		 */
		OVERALL_DC_CURRENT(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		OVERALL_DC_POWER(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		DC_CURRENT_STRING_1(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		DC_VOLTAGE_STRING_1(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		DC_POWER_STRING_1(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		DC_CURRENT_STRING_2(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		DC_VOLTAGE_STRING_2(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		DC_POWER_STRING_2(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		DC_CURRENT_STRING_3(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		DC_VOLTAGE_STRING_3(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		DC_POWER_STRING_3(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //

		/*
		 * Grid
		 */
		GRID_AC_P_TOTAL(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //

		ACTUAL_POWER(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		AC_VOLTAGE_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		AC_VOLTAGE_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		AC_VOLTAGE_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		AC_CURRENT_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		AC_CURRENT_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		AC_CURRENT_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		AC_POWER_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		AC_POWER_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		AC_POWER_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		POWER_LIMITATION_OF_EVU(new Doc().type(OpenemsType.FLOAT).unit(Unit.PERCENT)), //
		GRID_FREQUENCY(new Doc().type(OpenemsType.FLOAT).unit(Unit.HERTZ)), //
		COSINUS_PHI(new Doc().type(OpenemsType.FLOAT)), //
		HOME_CONSUMPTION_PV(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_BATTERY(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_GRID(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CURRENT_FROM_EXT_SENSOR_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		HOME_POWER_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_L1(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CURRENT_FROM_EXT_SENSOR_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		HOME_POWER_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_L2(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CURRENT_FROM_EXT_SENSOR_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		HOME_POWER_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_CONSUMPTION_L3(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_TOTAL_POWER(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		HOME_SELF_CONSUMPTION_TOTAL(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT)), //
		ISOLATION_RESISTOR(new Doc().type(OpenemsType.FLOAT).unit(Unit.KILOOHM)), //
		MAX_RESIDUAL_CURRENT(new Doc().type(OpenemsType.FLOAT).unit(Unit.AMPERE)), //
		ANALOG_INPUT_CH_1(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		ANALOG_INPUT_CH_2(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		ANALOG_INPUT_CH_3(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		ANALOG_INPUT_CH_4(new Doc().type(OpenemsType.FLOAT).unit(Unit.VOLT)), //
		YIELD_TOTAL(new Doc().type(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS)), //
		YIELD_DAY(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT_HOURS)), //
		HOME_CONSUMPTION_TOTAL(new Doc().type(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS)), //
		HOME_CONSUMPTION_DAY(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT_HOURS)), //
		SELF_CONSUMPTION_TOTAL(new Doc().type(OpenemsType.FLOAT).unit(Unit.KILOWATT_HOURS)), //
		SELF_CONSUMPTION_DAY(new Doc().type(OpenemsType.FLOAT).unit(Unit.WATT_HOURS)), //
		SELF_CONSUMPTION_RATE_TOTAL(new Doc().type(OpenemsType.FLOAT).unit(Unit.PERCENT)), //
		SELF_CONSUMPTION_RATE_DAY(new Doc().type(OpenemsType.FLOAT).unit(Unit.PERCENT)), //
		DEGREE_OF_SELF_SUFFICIENCY_DAY(new Doc().type(OpenemsType.FLOAT)), //
		DEGREE_OF_SELF_SUFFICIENCY_TOTAL(new Doc().type(OpenemsType.FLOAT));

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
