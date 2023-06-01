package io.openems.edge.meter.weidmueller;

import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;

public enum WeidmuellerChannelId implements io.openems.edge.common.channel.ChannelId {
	VOLTAGE_L1_L2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	VOLTAGE_L2_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	VOLTAGE_L1_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	APPARENT_POWER_S1_L1N(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE)), //
	APPARENT_POWER_S2_L2N(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE)), //
	APPARENT_POWER_S3_L3N(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE)), //
	APPARENT_POWER_SUM(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE)), //
	COSPHI_L1(Doc.of(OpenemsType.INTEGER)), //
	COSPHI_L2(Doc.of(OpenemsType.INTEGER)), //
	COSPHI_L3(Doc.of(OpenemsType.INTEGER)), //
	ROTATION_FIELD(Doc.of(RotationField.values())), //
	REAL_ENERGY_L1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.CUMULATED_WATT_HOURS)), //
	REAL_ENERGY_L2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.CUMULATED_WATT_HOURS)), //
	REAL_ENERGY_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.CUMULATED_WATT_HOURS)), //
	REAL_ENERGY_L1_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.CUMULATED_WATT_HOURS)), //
	REAL_ENERGY_L1_CONSUMED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.CUMULATED_WATT_HOURS)), //
	REAL_ENERGY_L2_CONSUMED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.CUMULATED_WATT_HOURS)), //
	REAL_ENERGY_L3_CONSUMED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.CUMULATED_WATT_HOURS)), //
	REAL_ENERGY_L1_DELIVERED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.CUMULATED_WATT_HOURS)), //
	REAL_ENERGY_L2_DELIVERED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.CUMULATED_WATT_HOURS)), //
	REAL_ENERGY_L3_DELIVERED(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.CUMULATED_WATT_HOURS)), //
	APPARENT_ENERGY_L1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_HOURS)), //
	APPARENT_ENERGY_L2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_HOURS)), //
	APPARENT_ENERGY_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_HOURS)), //
	APPARENT_ENERGY_L1_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_HOURS)), //
	REACTIVE_ENERGY_L1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_L2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_L1_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_INDUCTIVE_L1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_INDUCTIVE_L2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_INDUCTIVE_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_INDUCTIVE_L1_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_CAPACITIVE_L1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_CAPACITIVE_L2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_CAPACITIVE_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_CAPACITIVE_L1_L3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	HARMONIC_THD_VOLT_L1N(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT)), //
	HARMONIC_THD_VOLT_L2N(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT)), //
	HARMONIC_THD_VOLT_L3N(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT)), //
	HARMONIC_THD_CURRENT_L1N(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT)), //
	HARMONIC_THD_CURRENT_L2N(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT)), //
	HARMONIC_THD_CURRENT_L3N(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT));

	private final Doc doc;

	private WeidmuellerChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}