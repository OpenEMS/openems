package io.openems.edge.phoenixcontact.plcnext.pvinverter;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;

public enum PlcNextPvInverterGdsDataReadMappingDefinition implements PlcNextGdsDataMappingDefinition {
	// Electricity meter standalone
	VOLTAGE_L1("electricityMeter.voltageMeasurement.phasesToNeutral.L1N", ElectricityMeter.ChannelId.VOLTAGE_L1), //
	VOLTAGE_L2("electricityMeter.voltageMeasurement.phasesToNeutral.L2N", ElectricityMeter.ChannelId.VOLTAGE_L2), //
	VOLTAGE_L3("electricityMeter.voltageMeasurement.phasesToNeutral.L3N", ElectricityMeter.ChannelId.VOLTAGE_L3), //
	VOLTAGE_LINE_L12("electricityMeter.voltageMeasurement.phasesToPhase.L12", PlcNextPvInverter.ChannelId.VOLTAGE_LINE_L12), //
	VOLTAGE_LINE_L23("electricityMeter.voltageMeasurement.phasesToPhase.L23", PlcNextPvInverter.ChannelId.VOLTAGE_LINE_L23), //
	VOLTAGE_LINE_L31("electricityMeter.voltageMeasurement.phasesToPhase.L23", PlcNextPvInverter.ChannelId.VOLTAGE_LINE_L31), //
	CURRENT_L1("electricityMeter.currentMeasurement.phases.L1", ElectricityMeter.ChannelId.CURRENT_L1), //
	CURRENT_L2("electricityMeter.currentMeasurement.phases.L2", ElectricityMeter.ChannelId.CURRENT_L2), //
	CURRENT_L3("electricityMeter.currentMeasurement.phases.L3", ElectricityMeter.ChannelId.CURRENT_L3), //
	CURRENT_NEUTRAL("electricityMeter.currentMeasurement.phases.Neutral", PlcNextPvInverter.ChannelId.CURRENT_NEUTRAL), //
	ACTIVE_POWER("electricityMeter.powerMeasurement.activePower.L123", ElectricityMeter.ChannelId.ACTIVE_POWER), //
	ACTIVE_POWER_L1("electricityMeter.powerMeasurement.activePower.L1", ElectricityMeter.ChannelId.ACTIVE_POWER_L1), //
	ACTIVE_POWER_L2("electricityMeter.powerMeasurement.activePower.L2", ElectricityMeter.ChannelId.ACTIVE_POWER_L2), //
	ACTIVE_POWER_L3("electricityMeter.powerMeasurement.activePower.L3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3), //
	REACTIVE_POWER("electricityMeter.powerMeasurement.reactivePower.L123", ElectricityMeter.ChannelId.REACTIVE_POWER), //
	REACTIVE_POWER_L1("electricityMeter.powerMeasurement.reactivePower.L1", ElectricityMeter.ChannelId.REACTIVE_POWER_L1), //
	REACTIVE_POWER_L2("electricityMeter.powerMeasurement.reactivePower.L2", ElectricityMeter.ChannelId.REACTIVE_POWER_L2), //
	REACTIVE_POWER_l3("electricityMeter.powerMeasurement.reactivePower.L3", ElectricityMeter.ChannelId.REACTIVE_POWER_L3), //
	APPARENT_POWER("electricityMeter.powerMeasurement.apparentPower.L123", PlcNextPvInverter.ChannelId.APPARENT_POWER), //
	APPARENT_POWER_L1("electricityMeter.powerMeasurement.apparentPower.L1", PlcNextPvInverter.ChannelId.APPARENT_POWER_L1), //
	APPARENT_POWER_L2("electricityMeter.powerMeasurement.apparentPower.L2", PlcNextPvInverter.ChannelId.APPARENT_POWER_L2), //
	APPARENT_POWER_L3("electricityMeter.powerMeasurement.apparentPower.L3", PlcNextPvInverter.ChannelId.APPARENT_POWER_L3), //
	POWER_FACTOR("electricityMeter.powerMeasurement.PowerFactor", PlcNextPvInverter.ChannelId.POWER_FACTOR), //
	ENERGY_IMPORT("electricityMeter.energyMeasurement.EnergyImport", ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY), //
	ENERGY_EXPORT("electricityMeter.energyMeasurement.EnergyExport", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

	private final String identifier;
	private final ChannelId channelId;

	private PlcNextPvInverterGdsDataReadMappingDefinition(String identifier, ChannelId channelId) {
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
