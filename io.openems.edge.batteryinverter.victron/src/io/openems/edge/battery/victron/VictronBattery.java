package io.openems.edge.battery.victron;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.victron.enums.ActiveInactive;
import io.openems.edge.battery.victron.enums.Error;
import io.openems.edge.battery.victron.enums.LowCellVoltageAlarm;
import io.openems.edge.battery.victron.enums.OpenClosed;
import io.openems.edge.battery.victron.enums.SystemSwitch;
import io.openems.edge.battery.victron.enums.VictronState;
import io.openems.edge.batteryinverter.victron.enums.Alarm;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface VictronBattery extends Battery, OpenemsComponent {
	
	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		
		
		STARTER_BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT)),
		TEMPERATURE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.DEGREE_CELSIUS)),
		MID_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT)),
		MID_VOLTAGE_DEVIATION(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.PERCENT)),
		CONSUMED_AMPHOURS(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.AMPERE_HOURS)),
		ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		LOW_VOLTAGE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		HIGH_VOLTAGE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		LOW_STARTER_VOLTAGE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		HIGH_STARTER_VOLTAGE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		LOW_STATE_OF_CHARGE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		LOW_TEMPERATURE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		HIGH_TEMPERATURE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		MID_VOLTAGE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		LOW_FUSED_VOLTAGE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		HIGH_FUSED_VOLTAGE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		FUSE_BLOWN_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		HIGH_INTERNAL_TEMPERATURE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		RELAY_STATUS(Doc.of(OpenClosed.values())
				.accessMode(AccessMode.READ_WRITE)),
		DEEPEST_DISCHARGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.AMPERE_HOURS)),
		LAST_DISCHARGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.AMPERE_HOURS)),
		AVERAGE_DISCHARGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.AMPERE_HOURS)),
		CHARGE_CYCLES(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		FULL_DISCHARGES(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		TOTAL_AMPHOURS_DRAWN(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.AMPERE_HOURS)),
		HISTORY_MIN_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT)),
		HISTORY_MAX_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT)),
		TIME_SINCE_LAST_FULL_CHARGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.SECONDS)),
		AUTOMATIC_SYNCS(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		LOW_VOLTAGE_ALARMS(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		HIGH_VOLTAGE_ALARMS(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		LOW_STARTER_VOLTAGE_ALARMS(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		HIGH_STARTER_VOLTAGE_ALARMS(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		MIN_STARTER_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT)),
		MAX_STARTER_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT)),
		LOW_FUSED_VOLTAGE_ALARMS(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		HIGH_FUSED_VOLTAGE_ALARMS(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		MIN_FUSED_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT)),
		MAX_FUSED_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT)),
		DISCHARGED_ENERGY(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.KILOWATT_HOURS)),
		CHARGED_ENERGY(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.KILOWATT_HOURS)),
		TIME_TO_GO(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.SECONDS)),
		CAPACITY_IN_AMPHOURS(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.AMPERE_HOURS)),
		TIMESTAMP_1ST_LAST_ERROR(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)),
		TIMESTAMP_2ND_LAST_ERROR(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)),
		TIMESTAMP_3RD_LAST_ERROR(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)),
		TIMESTAMP_4TH_LAST_ERROR(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)),
		HIGH_CHARGE_CURRENT_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		HIGH_DISCHARGE_CURRENT_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		CELL_IMBALANCE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		INTERNAL_FAILURE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		HIGH_CHARGE_TEMPERATURE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		LOW_CHARGE_TEMPERATURE_ALARM(Doc.of(Alarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		LOW_CELL_VOLTAGE_ALARM(Doc.of(LowCellVoltageAlarm.values())
				.accessMode(AccessMode.READ_ONLY)),
		VICTRON_STATE(Doc.of(VictronState.values())
				.accessMode(AccessMode.READ_ONLY)),
		ERROR(Doc.of(Error.values())
				.accessMode(AccessMode.READ_ONLY)),
		SYSTEM_SWITCH(Doc.of(SystemSwitch.values())
				.accessMode(AccessMode.READ_ONLY)),
		BALANCING(Doc.of(ActiveInactive.values())
				.accessMode(AccessMode.READ_ONLY)),
		NUMBER_OF_BATTERIES(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		BATTERIES_PARALLEL(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		BATTERIES_SERIES(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		NUMBER_OF_CELLS_PER_BATTERY(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		SYSTEM_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT)),
		SYSTEM_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT)),
		SHUTDOWNS_DUE_ERROR(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.NONE)),
		DIAGNOSTICS_1ST_LAST_ERROR(Doc.of(Error.values())
				.accessMode(AccessMode.READ_ONLY)),
		DIAGNOSTICS_2ND_LAST_ERROR(Doc.of(Error.values())
				.accessMode(AccessMode.READ_ONLY)),
		DIAGNOSTICS_3RD_LAST_ERROR(Doc.of(Error.values())
				.accessMode(AccessMode.READ_ONLY)),
		DIAGNOSTICS_4TH_LAST_ERROR(Doc.of(Error.values())
				.accessMode(AccessMode.READ_ONLY)),
		ALLOW_TO_CHARGE(Doc.of(OpenemsType.BOOLEAN)
				.accessMode(AccessMode.READ_ONLY)),
		ALLOW_TO_DISCHARGE(Doc.of(OpenemsType.BOOLEAN)
				.accessMode(AccessMode.READ_ONLY)),
		EXTERNAL_RELAY(Doc.of(ActiveInactive.values())
				.accessMode(AccessMode.READ_ONLY)),
		HISTORY_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT)),
		HISTORY_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER)
				.accessMode(AccessMode.READ_ONLY)
				.unit(Unit.VOLT));
		
		
		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}
	
	public default IntegerReadChannel getCapacityInAmphoursChannel() {
		return this.channel(ChannelId.CAPACITY_IN_AMPHOURS);
	}
	
	public default Value<Integer> getCapacityInAmphours() {
		return this.getCapacityInAmphoursChannel().value();
	}
	
	public default void _setCapacityInAmphours(Integer value) {
		this.getCapacityInAmphoursChannel().setNextValue(value);
	}

}
