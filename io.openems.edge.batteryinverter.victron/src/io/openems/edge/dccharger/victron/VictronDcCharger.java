package io.openems.edge.dccharger.victron;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.dccharger.api.EssDcCharger;
import io.openems.edge.victron.enums.Alarm;
import io.openems.edge.victron.enums.ChargeState;
import io.openems.edge.victron.enums.EqualizationPending;
import io.openems.edge.victron.enums.ErrorCode;
import io.openems.edge.victron.enums.MppOperationMode;
import io.openems.edge.victron.enums.OnOff;
import io.openems.edge.victron.enums.OpenClosed;

public interface VictronDcCharger extends EssDcCharger, OpenemsComponent {

    public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
	BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.VOLT)), //
	BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.AMPERE)), //
	BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.DEGREE_CELSIUS)), //
	CHARGER_ON_OFF(Doc.of(OnOff.values()) //
		.accessMode(AccessMode.READ_WRITE)), //
	CHARGE_STATE(Doc.of(ChargeState.values()) //
		.accessMode(AccessMode.READ_ONLY)), //
	EQUALIZATION_PENDING(Doc.of(EqualizationPending.values()) //
		.accessMode(AccessMode.READ_ONLY)), //
	EQUALIZATION_TIME_REMAINING(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.SECONDS)), //
	RELAY_ON_THE_CHARGER(Doc.of(OpenClosed.values()) //
		.accessMode(AccessMode.READ_ONLY)), //
	LOW_BATTERY_VOLTAGE_ALARM(Doc.of(Alarm.values()) //
		.accessMode(AccessMode.READ_ONLY)), //
	HIGH_BATTERY_VOLTAGE_ALARM(Doc.of(Alarm.values()) //
		.accessMode(AccessMode.READ_ONLY)), //
	YIELD_TODAY(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.KILOWATT_HOURS)), //
	MAX_CHARGE_POWER_TODAY(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	YIELD_YESTERDAY(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.KILOWATT_HOURS)), //
	MAX_CHARGE_POWER_YESTERDAY(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	ERROR_CODE(Doc.of(ErrorCode.values()) //
		.accessMode(AccessMode.READ_ONLY)), //
	MPP_OPERATION_MODE(Doc.of(MppOperationMode.values()) //
		.accessMode(AccessMode.READ_ONLY)), //
	PV_VOLTAGE_TRACKER_0(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.VOLT)), //
	PV_VOLTAGE_TRACKER_1(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.VOLT)), //
	PV_VOLTAGE_TRACKER_2(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.VOLT)), //
	PV_VOLTAGE_TRACKER_3(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.VOLT)), //
	YIELD_TODAY_TRACKER_0(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.KILOWATT_HOURS)), //
	YIELD_TODAY_TRACKER_1(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.KILOWATT_HOURS)), //
	YIELD_TODAY_TRACKER_2(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.KILOWATT_HOURS)), //
	YIELD_TODAY_TRACKER_3(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.KILOWATT_HOURS)), //
	YIELD_YESTERDAY_TRACKER_0(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.KILOWATT_HOURS)), //
	YIELD_YESTERDAY_TRACKER_1(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.KILOWATT_HOURS)), //
	YIELD_YESTERDAY_TRACKER_2(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.KILOWATT_HOURS)), //
	YIELD_YESTERDAY_TRACKER_3(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.KILOWATT_HOURS)), //
	MAX_CHARGE_POWER_TODAY_TRACKER_0(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	MAX_CHARGE_POWER_TODAY_TRACKER_1(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	MAX_CHARGE_POWER_TODAY_TRACKER_2(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	MAX_CHARGE_POWER_TODAY_TRACKER_3(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	MAX_CHARGE_POWER_YESTERDAY_TRACKER_0(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	MAX_CHARGE_POWER_YESTERDAY_TRACKER_1(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	MAX_CHARGE_POWER_YESTERDAY_TRACKER_2(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	MAX_CHARGE_POWER_YESTERDAY_TRACKER_3(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	PV_POWER_TRACKER_0(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	PV_POWER_TRACKER_1(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	PV_POWER_TRACKER_2(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)), //
	PV_POWER_TRACKER_3(Doc.of(OpenemsType.INTEGER) //
		.accessMode(AccessMode.READ_ONLY) //
		.unit(Unit.WATT)) //
	;

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
