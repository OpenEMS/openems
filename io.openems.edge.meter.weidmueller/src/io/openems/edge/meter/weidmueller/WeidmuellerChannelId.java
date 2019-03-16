package io.openems.edge.meter.weidmueller;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.Unit;

public enum WeidmuellerChannelId implements io.openems.edge.common.channel.ChannelId {
	VOLTAGE_L1_L2(new Doc().unit(Unit.VOLT)), //
	VOLTAGE_L2_L3(new Doc().unit(Unit.VOLT)), //
	VOLTAGE_L1_L3(new Doc().unit(Unit.VOLT)), //
	APPARENT_POWER_S1_L1N(new Doc().unit(Unit.VOLT_AMPERE)), //
	APPARENT_POWER_S2_L2N(new Doc().unit(Unit.VOLT_AMPERE)), //
	APPARENT_POWER_S3_L3N(new Doc().unit(Unit.VOLT_AMPERE)), //
	APPARENT_POWER_SUM(new Doc().unit(Unit.VOLT_AMPERE)), //
	COSPHI_L1(new Doc()), //
	COSPHI_L2(new Doc()), //
	COSPHI_L3(new Doc()), //
	ROTATION_FIELD(new Doc() //
			.options(RotationField.values())), //
	REAL_ENERGY_L1(new Doc().unit(Unit.WATT_HOURS)), //
	REAL_ENERGY_L2(new Doc().unit(Unit.WATT_HOURS)), //
	REAL_ENERGY_L3(new Doc().unit(Unit.WATT_HOURS)), //
	REAL_ENERGY_L1_L3(new Doc().unit(Unit.WATT_HOURS)), //
	REAL_ENERGY_L1_CONSUMED(new Doc().unit(Unit.WATT_HOURS)), //
	REAL_ENERGY_L2_CONSUMED(new Doc().unit(Unit.WATT_HOURS)), //
	REAL_ENERGY_L3_CONSUMED(new Doc().unit(Unit.WATT_HOURS)), //
	REAL_ENERGY_L1_DELIVERED(new Doc().unit(Unit.WATT_HOURS)), //
	REAL_ENERGY_L2_DELIVERED(new Doc().unit(Unit.WATT_HOURS)), //
	REAL_ENERGY_L3_DELIVERED(new Doc().unit(Unit.WATT_HOURS)), //
	APPARENT_ENERGY_L1(new Doc().unit(Unit.VOLT_AMPERE_HOURS)), //
	APPARENT_ENERGY_L2(new Doc().unit(Unit.VOLT_AMPERE_HOURS)), //
	APPARENT_ENERGY_L3(new Doc().unit(Unit.VOLT_AMPERE_HOURS)), //
	APPARENT_ENERGY_L1_L3(new Doc().unit(Unit.VOLT_AMPERE_HOURS)), //
	REACTIVE_ENERGY_L1(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_L2(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_L1_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_INDUCTIVE_L1(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_INDUCTIVE_L2(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_INDUCTIVE_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_INDUCTIVE_L1_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_CAPACITIVE_L1(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_CAPACITIVE_L2(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_CAPACITIVE_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	REACTIVE_ENERGY_CAPACITIVE_L1_L3(new Doc().unit(Unit.VOLT_AMPERE_REACTIVE_HOURS)), //
	HARMONIC_THD_VOLT_L1N(new Doc().unit(Unit.PERCENT)), //
	HARMONIC_THD_VOLT_L2N(new Doc().unit(Unit.PERCENT)), //
	HARMONIC_THD_VOLT_L3N(new Doc().unit(Unit.PERCENT)), //
	HARMONIC_THD_CURRENT_L1N(new Doc().unit(Unit.PERCENT)), //
	HARMONIC_THD_CURRENT_L2N(new Doc().unit(Unit.PERCENT)), //
	HARMONIC_THD_CURRENT_L3N(new Doc().unit(Unit.PERCENT));

	private final Doc doc;

	private WeidmuellerChannelId(Doc doc) {
		this.doc = doc;
	}

	public Doc doc() {
		return this.doc;
	}
}