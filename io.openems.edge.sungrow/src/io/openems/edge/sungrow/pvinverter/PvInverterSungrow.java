package io.openems.edge.sungrow.pvinverter;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;

public interface PvInverterSungrow extends OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	SERIAL_NUMBER(Doc.of(OpenemsType.STRING)), //

	NOMINAL_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT) //
		.persistencePriority(PersistencePriority.VERY_LOW)), //

	DAILY_ENERGY(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT_HOURS) //
		.persistencePriority(PersistencePriority.MEDIUM)), TOTAL_RUNNING_TIME(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.HOUR) //
		.persistencePriority(PersistencePriority.VERY_LOW)),

	INTERNAL_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.DEZIDEGREE_CELSIUS) //
		.persistencePriority(PersistencePriority.VERY_LOW)),

	APPARENT_POWER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.VOLT_AMPERE) //
		.persistencePriority(PersistencePriority.MEDIUM)), //

	DC_VOLTAGE_1(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.VOLT)), //

	DC_CURRENT_1(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.MILLIAMPERE)), //

	DC_VOLTAGE_2(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.VOLT)), //

	DC_CURRENT_2(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.MILLIAMPERE)), //

	DC_VOLTAGE_3(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.VOLT)), //

	DC_CURRENT_3(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.MILLIAMPERE)), //

	DC_POWER(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.WATT) //
		.persistencePriority(PersistencePriority.HIGH)),

	POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.THOUSANDTH)),

	WORK_STATE(Doc.of(WorkState.values())),

	NEGATIVE_VOLTAGE_TO_THE_GROUND(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.VOLT) //
		.persistencePriority(PersistencePriority.VERY_LOW)),

	BUS_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.VOLT)),

	POWER_LIMITATION_SWITCH(Doc.of(OpenemsType.BOOLEAN) //
		.unit(Unit.ON_OFF) //
		.accessMode(AccessMode.READ_WRITE)), 
	POWER_LIMITATION_SETTING(Doc.of(OpenemsType.INTEGER) //
		.unit(Unit.PERCENT) //
		.accessMode(AccessMode.READ_WRITE));

	private final Doc doc;

	private ChannelId(Doc doc) {
	    this.doc = doc;
	}

	@Override
	public Doc doc() {
	    return this.doc;
	}
    }

}