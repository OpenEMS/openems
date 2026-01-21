package io.openems.edge.phoenixcontact.plcnext.meter;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;

public enum PlcNextMeterGdsDataReadMappingDefinition implements PlcNextGdsDataMappingDefinition {
	// Electricity meter standalone
	VOLTAGE_L1("voltageMeasurement.phasesToNeutral.L1N", ElectricityMeter.ChannelId.VOLTAGE_L1), //
	VOLTAGE_L2("voltageMeasurement.phasesToNeutral.L2N", ElectricityMeter.ChannelId.VOLTAGE_L2), //
	VOLTAGE_L3("voltageMeasurement.phasesToNeutral.L3N", ElectricityMeter.ChannelId.VOLTAGE_L3), //
	VOLTAGE_LINE_L12("voltageMeasurement.phasesToPhase.L12", PlcNextMeter.ChannelId.VOLTAGE_LINE_L12), //
	VOLTAGE_LINE_L23("voltageMeasurement.phasesToPhase.L23", PlcNextMeter.ChannelId.VOLTAGE_LINE_L23), //
	VOLTAGE_LINE_L31("voltageMeasurement.phasesToPhase.L23", PlcNextMeter.ChannelId.VOLTAGE_LINE_L31), //
	CURRENT_L1("currentMeasurement.phases.L1", ElectricityMeter.ChannelId.CURRENT_L1), //
	CURRENT_L2("currentMeasurement.phases.L2", ElectricityMeter.ChannelId.CURRENT_L2), //
	CURRENT_L3("currentMeasurement.phases.L3", ElectricityMeter.ChannelId.CURRENT_L3), //
	CURRENT_NEUTRAL("currentMeasurement.phases.Neutral", PlcNextMeter.ChannelId.CURRENT_NEUTRAL), //
	ACTIVE_POWER("powerMeasurement.activePower.L123", ElectricityMeter.ChannelId.ACTIVE_POWER), //
	ACTIVE_POWER_L1("powerMeasurement.activePower.L1", ElectricityMeter.ChannelId.ACTIVE_POWER_L1), //
	ACTIVE_POWER_L2("powerMeasurement.activePower.L2", ElectricityMeter.ChannelId.ACTIVE_POWER_L2), //
	ACTIVE_POWER_L3("powerMeasurement.activePower.L3", ElectricityMeter.ChannelId.ACTIVE_POWER_L3), //
	REACTIVE_POWER("powerMeasurement.reactivePower.L123", ElectricityMeter.ChannelId.REACTIVE_POWER), //
	REACTIVE_POWER_L1("powerMeasurement.reactivePower.L1", ElectricityMeter.ChannelId.REACTIVE_POWER_L1), //
	REACTIVE_POWER_L2("powerMeasurement.reactivePower.L2", ElectricityMeter.ChannelId.REACTIVE_POWER_L2), //
	REACTIVE_POWER_l3("powerMeasurement.reactivePower.L3", ElectricityMeter.ChannelId.REACTIVE_POWER_L3), //
	APPARENT_POWER("powerMeasurement.apparentPower.L123", PlcNextMeter.ChannelId.APPARENT_POWER), //
	APPARENT_POWER_L1("powerMeasurement.apparentPower.L1", PlcNextMeter.ChannelId.APPARENT_POWER_L1), //
	APPARENT_POWER_L2("powerMeasurement.apparentPower.L2", PlcNextMeter.ChannelId.APPARENT_POWER_L2), //
	APPARENT_POWER_L3("powerMeasurement.apparentPower.L3", PlcNextMeter.ChannelId.APPARENT_POWER_L3), //
	POWER_FACTOR("powerMeasurement.PowerFactor", PlcNextMeter.ChannelId.POWER_FACTOR), //
	ENERGY_IMPORT("energyMeasurement.EnergyImport", ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY), //
	ENERGY_EXPORT("energyMeasurement.EnergyExport", ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY);

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
