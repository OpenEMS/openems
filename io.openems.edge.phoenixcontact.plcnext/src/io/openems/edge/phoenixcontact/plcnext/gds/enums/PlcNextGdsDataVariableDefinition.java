package io.openems.edge.phoenixcontact.plcnext.gds.enums;

import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.meter.api.ElectricityMeter;
import io.openems.edge.phoenixcontact.plcnext.PlcNextDevice;

public enum PlcNextGdsDataVariableDefinition {
	PHASE_VOLTAGES("phaseVoltages", PlcNextGdsDataType.FLOAT32_ARRAY_3,
			List.of(ElectricityMeter.ChannelId.VOLTAGE_L1, ElectricityMeter.ChannelId.VOLTAGE_L2,
					ElectricityMeter.ChannelId.VOLTAGE_L3)), //
	LINE_VOLTAGES("lineVoltages", PlcNextGdsDataType.FLOAT32_ARRAY_3,
			List.of(PlcNextDevice.ChannelId.LINE_VOLTAGE_L1, PlcNextDevice.ChannelId.LINE_VOLTAGE_L2,
					PlcNextDevice.ChannelId.LINE_VOLTAGE_L3)), //
	PHASE_CURRENTS("phaseCurrents", PlcNextGdsDataType.FLOAT32_ARRAY_3,
			List.of(ElectricityMeter.ChannelId.CURRENT_L1, ElectricityMeter.ChannelId.CURRENT_L2,
					ElectricityMeter.ChannelId.CURRENT_L3)), //
	NEUTRAL_CURRENT("neutralCurrent", PlcNextGdsDataType.FLOAT64, List.of(PlcNextDevice.ChannelId.NEUTRAL_CURRENT)), //
	ACTIVE_POWER("activePower", PlcNextGdsDataType.FLOAT64, List.of(ElectricityMeter.ChannelId.ACTIVE_POWER)), //
	REACTIVE_POWER("reactivePower", PlcNextGdsDataType.FLOAT64, List.of(ElectricityMeter.ChannelId.REACTIVE_POWER)), //
	APPARENT_POWER("apparentPower", PlcNextGdsDataType.FLOAT64, List.of(PlcNextDevice.ChannelId.APPARENT_POWER)), //
	POWER_FACTOR("powerFactor", PlcNextGdsDataType.FLOAT64, List.of(PlcNextDevice.ChannelId.POWER_FACTOR)), //
	ENERGY_IMPORT("energyImport", PlcNextGdsDataType.FLOAT64,
			List.of(ElectricityMeter.ChannelId.ACTIVE_CONSUMPTION_ENERGY)), //
	ENERGY_EXPORT("energyExport", PlcNextGdsDataType.FLOAT64,
			List.of(ElectricityMeter.ChannelId.ACTIVE_PRODUCTION_ENERGY));

	private final String identifier;
	private final PlcNextGdsDataType dataType;
	private final String plcNextChannel;
	private final List<ChannelId> openEmsChannelIds;

	private PlcNextGdsDataVariableDefinition(String identifier, PlcNextGdsDataType dataType,
			List<ChannelId> openEmsChannelIds) {
		this.identifier = identifier;
		this.dataType = dataType;
		this.plcNextChannel = "udtIn";
		this.openEmsChannelIds = openEmsChannelIds;
	}

	public String getIdentifier() {
		return this.identifier;
	}

	public PlcNextGdsDataType getDataType() {
		return this.dataType;
	}

	public String getPlcNextChannel() {
		return this.plcNextChannel;
	}

	public List<ChannelId> getOpenEmsChannelIds() {
		return this.openEmsChannelIds;
	}

	/**
	 * Fetches definition by identifier
	 * 
	 * @param identifier
	 * @return
	 */
	public static Optional<PlcNextGdsDataVariableDefinition> valueByIdentifier(String identifier) {
		return Stream.of(PlcNextGdsDataVariableDefinition.values())
				.filter(item -> item.getIdentifier().equals(identifier)).findFirst();
	}
}
