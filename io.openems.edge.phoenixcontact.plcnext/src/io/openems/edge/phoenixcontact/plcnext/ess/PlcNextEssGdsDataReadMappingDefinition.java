package io.openems.edge.phoenixcontact.plcnext.ess;

import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.phoenixcontact.plcnext.common.data.PlcNextGdsDataMappingDefinition;

public enum PlcNextEssGdsDataReadMappingDefinition implements PlcNextGdsDataMappingDefinition {
	// ESS state
	ALLOWED_CHARGE_POWER("essMeter.AllowedChargePower", ManagedSymmetricEss.ChannelId.ALLOWED_CHARGE_POWER), //
	ALLOWED_DISCHARGE_POWER("essMeter.AllowedDischargePower", ManagedSymmetricEss.ChannelId.ALLOWED_DISCHARGE_POWER), //
	SOC("essMeter.Soc", SymmetricEss.ChannelId.SOC), //
	CAPACITY("essMeter.Capacity", SymmetricEss.ChannelId.CAPACITY), //
	GRID_MODE("essMeter.GridMode", SymmetricEss.ChannelId.GRID_MODE), //
	ACTIVE_CHARGE_ENERGY("essMeter.ActiveChargeEnergy", SymmetricEss.ChannelId.ACTIVE_CHARGE_ENERGY), //
	ACTIVE_DISCHARGE_ENERGY("essMeter.ActiveDischargeEnergy", SymmetricEss.ChannelId.ACTIVE_DISCHARGE_ENERGY), //
	MIN_CELL_VOLTAGE("essMeter.MinCellVoltage", SymmetricEss.ChannelId.MIN_CELL_VOLTAGE), //
	MAX_CELL_VOLTAGE("essMeter.MaxCellVoltage", SymmetricEss.ChannelId.MAX_CELL_VOLTAGE), //
	MIN_CELL_TEMPERATURE("essMeter.MinCellTemperature", SymmetricEss.ChannelId.MIN_CELL_TEMPERATURE), //
	MAX_CELL_TEMPERATURE("essMeter.MaxCellTemperature", SymmetricEss.ChannelId.MAX_CELL_TEMPERATURE), //
	ACTIVE_POWER("electricityMeter.powerMeasurement.activePower.L123", SymmetricEss.ChannelId.ACTIVE_POWER), //
	REACTIVE_POWER("electricityMeter.powerMeasurement.reactivePower.L123", SymmetricEss.ChannelId.REACTIVE_POWER);

	private final String identifier;
	private final ChannelId channelId;

	private PlcNextEssGdsDataReadMappingDefinition(String identifier, ChannelId channelId) {
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
