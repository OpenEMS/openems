package io.openems.edge.ess.streetscooter;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.ChannelId;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.ess.api.SymmetricEss;

public enum StrtsctrChannelId implements ChannelId {
	// EnumReadChannel
	INVERTER_MODE(Doc.of(InverterMode.values())), //

	// BooleanReadChannel
	DEBUG_ICU_ENABLED(Doc.of(OpenemsType.BOOLEAN)), //
	DEBUG_ICU_RUN(Doc.of(OpenemsType.BOOLEAN)), //
	ICU_RUNSTATE(Doc.of(OpenemsType.BOOLEAN)), //
	BATTERY_CONNECTED(Doc.of(OpenemsType.BOOLEAN)), //
	BATTERY_OVERLOAD(Doc.of(OpenemsType.BOOLEAN)), //
	INVERTER_CONNECTED(Doc.of(OpenemsType.BOOLEAN)),

	// BooleanWriteChannel
	ICU_RUN(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(StrtsctrChannelId.DEBUG_ICU_RUN))), //
	ICU_ENABLED(new BooleanDoc() //
			.accessMode(AccessMode.READ_WRITE) //
			.onInit(new BooleanWriteChannel.MirrorToDebugChannel(StrtsctrChannelId.DEBUG_ICU_ENABLED))), //

	// IntegerReadChannel
	DEBUG_INVERTER_SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER)), //
	ICU_STATUS(Doc.of(OpenemsType.INTEGER)), //
	BATTERY_BMS_ERR(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_BMS_I_ACT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.AMPERE)), //
	BATTERY_BMS_PWR_CHRG_MAX(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT)), //
	BATTERY_BMS_SOH(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.PERCENT)), //
	BATTERY_BMS_ST_BAT(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	BATTERY_BMS_T_MAX_PACK(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEGREE_CELSIUS)), //
	BATTERY_BMS_T_MIN_PACK(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEGREE_CELSIUS)), //
	BATTERY_BMS_U_PACK(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT) //
			// onChange calculate new value for MaxApparentPower
			.onInit(channel -> { //
				((IntegerReadChannel) channel).onChange(value -> {
					final int CHARGE_DISCHARGE_CURRENT = 40; // in ampere
					int maxApparentPower = Math.min(value.orElse(Integer.MAX_VALUE) * CHARGE_DISCHARGE_CURRENT,
							AbstractEssStreetscooter.MAX_APPARENT_POWER);
					channel.getComponent().channel(SymmetricEss.ChannelId.MAX_APPARENT_POWER)
							.setNextValue(maxApparentPower);
				});
			})), //
	BATTERY_BMS_WRN(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //

	INVERTER_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT)), //
	INVERTER_DC1_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_DC2_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_ERROR_MESSAGE_1H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_ERROR_MESSAGE_1L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_ERROR_MESSAGE_2H(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_ERROR_MESSAGE_2L(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_F_AC_1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_F_AC_2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_F_AC_3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_GF1_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_GF2_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_GF3_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_GFCI_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_GV1_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_GV2_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_GV3_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.NONE)), //
	INVERTER_P_AC(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT)), //
	INVERTER_P_AC_1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT)), //
	INVERTER_P_AC_2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT)), //
	INVERTER_P_AC_3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.WATT)), //
	INVERTER_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEGREE_CELSIUS)),
	INVERTER_TEMPERATURE_FAULT_VALUE(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.DEGREE_CELSIUS)), //
	INVERTER_V_AC_1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	INVERTER_V_AC_2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	INVERTER_V_AC_3(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	INVERTER_V_DC_1(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //
	INVERTER_V_DC_2(Doc.of(OpenemsType.INTEGER) //
			.unit(Unit.VOLT)), //

	// IntegerWriteChannel
	INVERTER_SET_ACTIVE_POWER(new IntegerDoc() //
			.accessMode(AccessMode.WRITE_ONLY) //
			.unit(Unit.WATT) //
			.onInit(new IntegerWriteChannel.MirrorToDebugChannel(StrtsctrChannelId.DEBUG_INVERTER_SET_ACTIVE_POWER))), //

	// StringWriteChannel
	SYSTEM_STATE_INFORMATION(Doc.of(OpenemsType.STRING) //
			.accessMode(AccessMode.READ_WRITE)); //

	private final Doc doc;

	@Override
	public Doc doc() {
		return this.doc;
	}

	private StrtsctrChannelId(Doc doc) {
		this.doc = doc;
	}
}