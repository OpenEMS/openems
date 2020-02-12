package io.openems.edge.goodwe.et.gridmeter;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;

public enum GridMeterChannelId implements ChannelId {
	V_GRID_R(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	I_GRID_R(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	F_GRID_R(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	P_GRID_R(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	V_GRID_S(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	I_GRID_S(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	F_GRID_S(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	P_GRID_S(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),
	V_GRID_T(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_ONLY)),
	I_GRID_T(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE).accessMode(AccessMode.READ_ONLY)),
	F_GRID_T(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_ONLY)),
	P_GRID_T(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT).accessMode(AccessMode.READ_ONLY)),

	// Safety
	GRID_VOLT_HIGH_S1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_HIGH_S1_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_LOW_S1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_LOW_S1_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_HIGH_S2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_HIGH_S2_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_LOW_S2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_LOW_S2_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_QUALITY(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_HIGH_S1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_HIGH_S1_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_LOW_S1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_LOW_S1_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_HIGH_S2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_HIGH_S2_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_LOW_S2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_LOW_S2_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_HIGH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_LOW(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_HIGH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_LOW(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.HERTZ).accessMode(AccessMode.READ_WRITE)), //
	GRID_RECOVER_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.SECONDS).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_RECOVER_HIGH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_RECOVER_LOW(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_RECOVER_HIGH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_RECOVER_LOW(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_VOLT_RECOVER_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	GRID_FREQ_RECOVER_TIME(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT).accessMode(AccessMode.READ_WRITE)), //
	POWER_RATE_LIMIT_GENERATE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
	POWER_RATE_LIMIT_RECONNECT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
	POWER_RATE_LIMIT_REDUCTION(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
	GRID_PROTECT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE)), //
	POWER_SLOPE_ENABLE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE).accessMode(AccessMode.READ_WRITE));

	private final Doc doc;

	private GridMeterChannelId(Doc doc) {
		this.doc = doc;
	}

	@Override
	public Doc doc() {
		return this.doc;
	}
}