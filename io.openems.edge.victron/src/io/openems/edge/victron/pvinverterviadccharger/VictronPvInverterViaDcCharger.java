package io.openems.edge.victron.pvinverterviadccharger;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.DEGREE_CELSIUS;
import static io.openems.common.channel.Unit.KILOWATT_HOURS;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.channel.Unit.VOLT;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.pvinverter.api.ManagedSymmetricPvInverter;
import io.openems.edge.victron.enums.Alarm;
import io.openems.edge.victron.enums.ChargeState;
import io.openems.edge.victron.enums.EqualizationPending;
import io.openems.edge.victron.enums.ErrorCode;
import io.openems.edge.victron.enums.MppOperationMode;
import io.openems.edge.victron.enums.OnOff;
import io.openems.edge.victron.enums.OpenClosed;

public interface VictronPvInverterViaDcCharger extends ManagedSymmetricPvInverter, OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		BATTERY_CURRENT(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(AMPERE)), //
		BATTERY_TEMPERATURE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(DEGREE_CELSIUS)), //
		CHARGER_ON_OFF(Doc.of(OnOff.values())//
				.accessMode(READ_WRITE)), //
		CHARGE_STATE(Doc.of(ChargeState.values())//
				.accessMode(READ_ONLY)), //
		EQUALIZATION_PENDING(Doc.of(EqualizationPending.values())//
				.accessMode(READ_ONLY)), //
		EQUALIZATION_TIME_REMAINING(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(SECONDS)), //
		RELAY_ON_THE_CHARGER(Doc.of(OpenClosed.values())//
				.accessMode(READ_ONLY)), //
		LOW_BATTERY_VOLTAGE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		HIGH_BATTERY_VOLTAGE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		YIELD_TODAY(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(KILOWATT_HOURS)), //
		MAX_CHARGE_POWER_TODAY(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		YIELD_YESTERDAY(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(KILOWATT_HOURS)), //
		MAX_CHARGE_POWER_YESTERDAY(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		ERROR_CODE(Doc.of(ErrorCode.values())//
				.accessMode(READ_ONLY)), //
		MPP_OPERATION_MODE(Doc.of(MppOperationMode.values())//
				.accessMode(READ_ONLY)), //
		PV_VOLTAGE_TRACKER_0(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		PV_VOLTAGE_TRACKER_1(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		PV_VOLTAGE_TRACKER_2(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		PV_VOLTAGE_TRACKER_3(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		YIELD_TODAY_TRACKER_0(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(KILOWATT_HOURS)), //
		YIELD_TODAY_TRACKER_1(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(KILOWATT_HOURS)), //
		YIELD_TODAY_TRACKER_2(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(KILOWATT_HOURS)), //
		YIELD_TODAY_TRACKER_3(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(KILOWATT_HOURS)), //
		YIELD_YESTERDAY_TRACKER_0(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(KILOWATT_HOURS)), //
		YIELD_YESTERDAY_TRACKER_1(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(KILOWATT_HOURS)), //
		YIELD_YESTERDAY_TRACKER_2(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(KILOWATT_HOURS)), //
		YIELD_YESTERDAY_TRACKER_3(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(KILOWATT_HOURS)), //
		MAX_CHARGE_POWER_TODAY_TRACKER_0(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		MAX_CHARGE_POWER_TODAY_TRACKER_1(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		MAX_CHARGE_POWER_TODAY_TRACKER_2(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		MAX_CHARGE_POWER_TODAY_TRACKER_3(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		MAX_CHARGE_POWER_YESTERDAY_TRACKER_0(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		MAX_CHARGE_POWER_YESTERDAY_TRACKER_1(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		MAX_CHARGE_POWER_YESTERDAY_TRACKER_2(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		MAX_CHARGE_POWER_YESTERDAY_TRACKER_3(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		PV_POWER_TRACKER_0(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		PV_POWER_TRACKER_1(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		PV_POWER_TRACKER_2(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(WATT)), //
		PV_POWER_TRACKER_3(Doc.of(INTEGER) //
				.accessMode(READ_ONLY) //
				.unit(WATT)) //
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
