package io.openems.edge.phoenixcontact.plcnext.meter;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;

public enum PlcNextMeterGdsDataReadMappingDefinition implements PlcNextGdsDataMappingDefinition {
	// Electricity meter standalone
	VOLTAGE_L1("VoltageL1N", ElectricityMeter.ChannelId.VOLTAGE_L1), //
	VOLTAGE_L2("VoltageL2N", ElectricityMeter.ChannelId.VOLTAGE_L2), //
	VOLTAGE_L3("VoltageL3N", ElectricityMeter.ChannelId.VOLTAGE_L3), //
	VOLTAGE_LINE_L12("VoltageL12", PlcNextMeter.ChannelId.VOLTAGE_LINE_L12), //
	VOLTAGE_LINE_L23("VoltageL23", PlcNextMeter.ChannelId.VOLTAGE_LINE_L23), //
	VOLTAGE_LINE_L31("VoltageL31", PlcNextMeter.ChannelId.VOLTAGE_LINE_L31), //
	CURRENT_L1("CurrentL1", ElectricityMeter.ChannelId.CURRENT_L1), //
	CURRENT_L2("CurrentL2", ElectricityMeter.ChannelId.CURRENT_L2), //
	CURRENT_L3("CurrentL3", ElectricityMeter.ChannelId.CURRENT_L3), //
	CURRENT_NEUTRAL("CurrentNeutral", PlcNextMeter.ChannelId.CURRENT_NEUTRAL), //
	ACTIVE_POWER("ActivePowerL123", ElectricityMeter.ChannelId.ACTIVE_POWER), //
	ACTIVE_POWER_L1("ActivePowerL1", ElectricityMeter.ChannelId.ACTIVE_POWER_L1), //
	ACTIVE_POWER_L2("ActivePowerL2", ElectricityMeter.ChannelId.ACTIVE_POWER_L2), //
	ACTIVE_POWER_L3("ActivePowerL3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3), //
	REACTIVE_POWER("ReactivePowerL123", ElectricityMeter.ChannelId.REACTIVE_POWER), //
	REACTIVE_POWER_L1("ReactivePowerL1", ElectricityMeter.ChannelId.REACTIVE_POWER_L1), //
	REACTIVE_POWER_L2("ReactivePowerL2", ElectricityMeter.ChannelId.REACTIVE_POWER_L2), //
	REACTIVE_POWER_l3("ReactivePowerL3", ElectricityMeter.ChannelId.REACTIVE_POWER_L3), //
	APPARENT_POWER("ApparentPowerL123", PlcNextMeter.ChannelId.APPARENT_POWER), //
	APPARENT_POWER_L1("ApparentPowerL1", PlcNextMeter.ChannelId.APPARENT_POWER_L1), //
	APPARENT_POWER_L2("ApparentPowerL2", PlcNextMeter.ChannelId.APPARENT_POWER_L2), //
	APPARENT_POWER_L3("ApparentPowerL3", PlcNextMeter.ChannelId.APPARENT_POWER_L3), //
	POWER_FACTOR("PowerFactor", PlcNextMeter.ChannelId.POWER_FACTOR), //
	ENERGY_IMPORT("EnergyImport", ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY), //
	ENERGY_EXPORT("EnergyExport", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	private final String identifier;
	private final ChannelId channelId;

	private PlcNextMeterGdsDataReadMappingDefinition(String identifier, ChannelId channelId) {
		this.identifier = identifier;
		this.channelId = channelId;
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public ChannelId getChannelId() {
		return this.channelId;
	}

}
