package io.openems.edge.phoenixcontact.plcnext.meter.data;

import java.util.List;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataVariableDefinition;
import io.openems.edge.phoenixcontact.plcnext.meter.PlcNextMeter;

public enum PlcNextGdsMeterDataVariableDefinition implements PlcNextGdsDataVariableDefinition {
	VOLTAGE_L1("voltageMeasurement.VoltagesL1N", List.of(ElectricityMeter.ChannelId.VOLTAGE_L1)), //
	VOLTAGE_L2("voltageMeasurement.VoltagesL2N", List.of(ElectricityMeter.ChannelId.VOLTAGE_L2)), //
	VOLTAGE_L3("voltageMeasurement.VoltagesL3N", List.of(ElectricityMeter.ChannelId.VOLTAGE_L3)), //
	VOLTAGE_LINE_L12("voltageMeasurement.VoltageL12", List.of(PlcNextMeter.ChannelId.VOLTAGE_LINE_L12)), //
	VOLTAGE_LINE_L23("voltageMeasurement.VoltageL23", List.of(PlcNextMeter.ChannelId.VOLTAGE_LINE_L23)), //
	VOLTAGE_LINE_L31("voltageMeasurement.VoltageL23", List.of(PlcNextMeter.ChannelId.VOLTAGE_LINE_L31)), //
	CURRENT_L1("currentMeasurement.CurrentL1", List.of(ElectricityMeter.ChannelId.CURRENT_L1)), //
	CURRENT_L2("currentMeasurement.CurrentL2", List.of(ElectricityMeter.ChannelId.CURRENT_L2)), //
	CURRENT_L3("currentMeasurement.CurrentL3", List.of(ElectricityMeter.ChannelId.CURRENT_L3)), //
	CURRENT_NEUTRAL("currentMeasurement.neutralCurrent", List.of(PlcNextMeter.ChannelId.CURRENT_NEUTRAL)), //
	ACTIVE_POWER("powerMeasurement.activePower", List.of(ElectricityMeter.ChannelId.ACTIVE_POWER)), //
	ACTIVE_POWER_L1("powerMeasurement.activePowerL1", List.of(ElectricityMeter.ChannelId.ACTIVE_POWER_L1)), //
	ACTIVE_POWER_L2("powerMeasurement.activePowerL2", List.of(ElectricityMeter.ChannelId.ACTIVE_POWER_L2)), //
	ACTIVE_POWER_L3("powerMeasurement.activePowerL3", List.of(ElectricityMeter.ChannelId.ACTIVE_POWER_L3)), //
	REACTIVE_POWER("powerMeasurement.reactivePower", List.of(ElectricityMeter.ChannelId.REACTIVE_POWER)), //
	REACTIVE_POWER_L1("powerMeasurement.reactivePowerL1", List.of(ElectricityMeter.ChannelId.REACTIVE_POWER_L1)), //
	REACTIVE_POWER_L2("powerMeasurement.reactivePowerL2", List.of(ElectricityMeter.ChannelId.REACTIVE_POWER_L2)), //
	REACTIVE_POWER_l3("powerMeasurement.reactivePowerL3", List.of(ElectricityMeter.ChannelId.REACTIVE_POWER_L3)), //
	APPARENT_POWER("powerMeasurement.apparentPower", List.of(PlcNextMeter.ChannelId.APPARENT_POWER)), //
	APPARENT_POWER_L1("powerMeasurement.apparentPowerL1", List.of(PlcNextMeter.ChannelId.APPARENT_POWER_L1)), //
	APPARENT_POWER_L2("powerMeasurement.apparentPowerL2", List.of(PlcNextMeter.ChannelId.APPARENT_POWER_L2)), //
	APPARENT_POWER_L3("powerMeasurement.apparentPowerL3", List.of(PlcNextMeter.ChannelId.APPARENT_POWER_L3)), //
	POWER_FACTOR("powerMeasurement.powerFactor", List.of(PlcNextMeter.ChannelId.POWER_FACTOR)), //
	ENERGY_IMPORT("energyMeasurement.energyImport", List.of(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY)), //
	ENERGY_EXPORT("energyMeasurement.energyExport", List.of(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY));

	private final String identifier;
	private final List<ChannelId> openEmsChannelIds;

	private PlcNextGdsMeterDataVariableDefinition(String identifier, List<ChannelId> openEmsChannelIds) {
		this.identifier = identifier;
		this.openEmsChannelIds = openEmsChannelIds;
	}

	@Override
	public String getIdentifier() {
		return this.identifier;
	}

	@Override
	public List<ChannelId> getOpenEmsChannelIds() {
		return this.openEmsChannelIds;
	}

}
