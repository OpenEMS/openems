package io.openems.edge.sma.sunnyisland;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.sma.enums.OperatingModeForActivePowerLimitation;
import io.openems.edge.sma.enums.PowerSupplyStatus;
import io.openems.edge.sma.enums.SetControlMode;
import io.openems.edge.sma.enums.SystemState;

public enum SiChannelId implements ChannelId {
	// EnumReadChannels
	SYSTEM_STATE(Doc.of(SystemState.values())), //
	POWER_SUPPLY_STATUS(Doc.of(PowerSupplyStatus.values())), //
	OPERATING_MODE_FOR_ACTIVE_POWER_LIMITATION(Doc.of(OperatingModeForActivePowerLimitation.values())), //

	// EnumWriteChannsl
	SET_CONTROL_MODE(Doc.of(SetControlMode.values()).accessMode(AccessMode.READ_WRITE)), //

	// LongReadChannels
	SERIAL_NUMBER(Doc.of(OpenemsType.LONG)), //

	// IntegerWriteChannels
	SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.WATT)), //
	SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.VOLT_AMPERE)), //
	MIN_SOC_POWER_ON(Doc.of(OpenemsType.INTEGER)), //
	GRID_GUARD_CODE(Doc.of(OpenemsType.INTEGER)), //
	MIN_SOC_POWER_OFF(Doc.of(OpenemsType.INTEGER)), //

	// IntegerReadChannels
	DEVICE_CLASS(Doc.of(OpenemsType.INTEGER)), //
	DEVICE_TYPE(Doc.of(OpenemsType.INTEGER)), //
	SOFTWARE_PACKAGE(Doc.of(OpenemsType.INTEGER)), //
	WAITING_TIME_UNTIL_FEED_IN(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS)), //
	MESSAGE(Doc.of(OpenemsType.INTEGER)), //
	RECOMMENDED_ACTION(Doc.of(OpenemsType.INTEGER)), //
	FAULT_CORRECTION_MEASURE(Doc.of(OpenemsType.INTEGER)), //
	GRID_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	GRID_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	GRID_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	FREQUENCY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIHERTZ)), //
	CURRENT_BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT)), //

	LOWEST_MEASURED_BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEGREE_CELSIUS)), //
	HIGHEST_MEASURED_BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEGREE_CELSIUS)), //
	MAX_OCCURRED_BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //

	BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.MILLIVOLT)), //
	BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEGREE_CELSIUS)), //
	BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE));

	private final Doc doc;

	private SiChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}