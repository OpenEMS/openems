package io.openems.edge.pytes.battery;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.Unit;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface PytesBattery extends Battery, OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		STARTER_BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.VOLT)),

		BMS_CHARGE_CURRENT_LIMIT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		BMS_DISCHARGE_CURRENT_LIMIT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.MILLIAMPERE)),

		BMS_BATTERY_FAULT_STATUS01(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)),

		BMS_BATTERY_FAULT_STATUS02(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)), 
		
		DC_DISCHARGE_POWER(Doc.of(INTEGER)//
				.accessMode(READ_ONLY).unit(Unit.WATT)),

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
	 * Gets the Starter Battery Voltage in [V]. See {@link ChannelId#STARTER_BATTERY_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getStarterBatteryVoltage() {
	    return this.getStarterBatteryVoltageChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#STARTER_BATTERY_VOLTAGE} Channel.
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
	 * Gets the BMS Charge Current Limit in [mA]. See {@link ChannelId#BMS_CHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsChargeCurrentLimit() {
	    return this.getBmsChargeCurrentLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BMS_CHARGE_CURRENT_LIMIT} Channel.
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
	 * Gets the BMS Discharge Current Limit in [mA]. See {@link ChannelId#BMS_DISCHARGE_CURRENT_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsDischargeCurrentLimit() {
	    return this.getBmsDischargeCurrentLimitChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BMS_DISCHARGE_CURRENT_LIMIT} Channel.
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
	 * Gets the BMS Battery Fault Status 01. See {@link ChannelId#BMS_BATTERY_FAULT_STATUS01}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsBatteryFaultStatus01() {
	    return this.getBmsBatteryFaultStatus01Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BMS_BATTERY_FAULT_STATUS01} Channel.
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
	 * Gets the BMS Battery Fault Status 02. See {@link ChannelId#BMS_BATTERY_FAULT_STATUS02}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBmsBatteryFaultStatus02() {
	    return this.getBmsBatteryFaultStatus02Channel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#BMS_BATTERY_FAULT_STATUS02} Channel.
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
	 * Internal method to set the 'nextValue' on {@link ChannelId#DC_DISCHARGE_POWER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setDcDischargePower(Integer value) {
	    this.getDcDischargePowerChannel().setNextValue(value);
	}	

	void setMinSocPercentage(int minSocPercentage);

	int getConfiguredMaxChargeCurrent();
	
	int getConfiguredMaxDischargeCurrent();

}
