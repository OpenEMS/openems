package io.openems.edge.victron.battery;

import static io.openems.common.channel.AccessMode.READ_ONLY;
import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.Unit.AMPERE_HOURS;
import static io.openems.common.channel.Unit.NONE;
import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.channel.Unit.SECONDS;
import static io.openems.common.channel.Unit.VOLT;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;

import io.openems.common.channel.Unit;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.victron.enums.ActiveInactive;
import io.openems.edge.victron.enums.Alarm;
import io.openems.edge.victron.enums.Error;
import io.openems.edge.victron.enums.LowCellVoltageAlarm;
import io.openems.edge.victron.enums.OpenClosed;
import io.openems.edge.victron.enums.SystemSwitch;
import io.openems.edge.victron.enums.VictronState;

public interface VictronBattery extends Battery, OpenemsComponent {

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		STARTER_BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		TEMPERATURE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(Unit.DEGREE_CELSIUS)), //
		MID_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		MID_VOLTAGE_DEVIATION(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(PERCENT)), //
		CONSUMED_AMPHOURS(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(AMPERE_HOURS)), //
		ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //

		LOW_VOLTAGE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		HIGH_VOLTAGE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		LOW_STARTER_VOLTAGE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		HIGH_STARTER_VOLTAGE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		LOW_STATE_OF_CHARGE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		LOW_TEMPERATURE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		HIGH_TEMPERATURE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		MID_VOLTAGE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		LOW_FUSED_VOLTAGE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		HIGH_FUSED_VOLTAGE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		FUSE_BLOWN_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		HIGH_INTERNAL_TEMPERATURE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		RELAY_STATUS(Doc.of(OpenClosed.values())//
				.accessMode(READ_WRITE)), //
		DEEPEST_DISCHARGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(AMPERE_HOURS)), //
		LAST_DISCHARGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(AMPERE_HOURS)), //
		AVERAGE_DISCHARGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(AMPERE_HOURS)), //
		CHARGE_CYCLES(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		FULL_DISCHARGES(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		TOTAL_AMPHOURS_DRAWN(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(AMPERE_HOURS)), //
		HISTORY_MIN_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		HISTORY_MAX_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		TIME_SINCE_LAST_FULL_CHARGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(SECONDS)), //
		AUTOMATIC_SYNCS(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		LOW_VOLTAGE_ALARMS(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		HIGH_VOLTAGE_ALARMS(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		LOW_STARTER_VOLTAGE_ALARMS(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		HIGH_STARTER_VOLTAGE_ALARMS(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		MIN_STARTER_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		MAX_STARTER_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		LOW_FUSED_VOLTAGE_ALARMS(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		HIGH_FUSED_VOLTAGE_ALARMS(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		MIN_FUSED_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		MAX_FUSED_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		DC_DISCHARGED_ENERGY(Doc.of(LONG)//
				.accessMode(READ_ONLY)//
				.unit(WATT_HOURS)//
				.persistencePriority(HIGH)), //
		DC_CHARGED_ENERGY(Doc.of(LONG)//
				.accessMode(READ_ONLY)//
				.unit(WATT_HOURS)//
				.persistencePriority(HIGH)), //
		TIME_TO_GO(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(SECONDS)), //
		CAPACITY_IN_AMPHOURS(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(AMPERE_HOURS)), //
		TIMESTAMP_1ST_LAST_ERROR(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)), //
		TIMESTAMP_2ND_LAST_ERROR(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)), //
		TIMESTAMP_3RD_LAST_ERROR(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)), //
		TIMESTAMP_4TH_LAST_ERROR(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)), //
		HIGH_CHARGE_CURRENT_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		HIGH_DISCHARGE_CURRENT_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		CELL_IMBALANCE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		INTERNAL_FAILURE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		HIGH_CHARGE_TEMPERATURE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		LOW_CHARGE_TEMPERATURE_ALARM(Doc.of(Alarm.values())//
				.accessMode(READ_ONLY)), //
		LOW_CELL_VOLTAGE_ALARM(Doc.of(LowCellVoltageAlarm.values())//
				.accessMode(READ_ONLY)), //
		VICTRON_STATE(Doc.of(VictronState.values())//
				.accessMode(READ_ONLY)), //
		ERROR(Doc.of(Error.values())//
				.accessMode(READ_ONLY)), //
		SYSTEM_SWITCH(Doc.of(SystemSwitch.values())//
				.accessMode(READ_ONLY)), //
		BALANCING(Doc.of(ActiveInactive.values())//
				.accessMode(READ_ONLY)), //
		NUMBER_OF_BATTERIES(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		BATTERIES_PARALLEL(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		BATTERIES_SERIES(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		NUMBER_OF_CELLS_PER_BATTERY(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		SYSTEM_MIN_CELL_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		SYSTEM_MAX_CELL_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		SHUTDOWNS_DUE_ERROR(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(NONE)), //
		DIAGNOSTICS_1ST_LAST_ERROR(Doc.of(Error.values())//
				.accessMode(READ_ONLY)), //
		DIAGNOSTICS_2ND_LAST_ERROR(Doc.of(Error.values())//
				.accessMode(READ_ONLY)), //
		DIAGNOSTICS_3RD_LAST_ERROR(Doc.of(Error.values())//
				.accessMode(READ_ONLY)), //
		DIAGNOSTICS_4TH_LAST_ERROR(Doc.of(Error.values())//
				.accessMode(READ_ONLY)), //
		ALLOW_TO_CHARGE(Doc.of(BOOLEAN)//
				.accessMode(READ_ONLY)), //
		ALLOW_TO_DISCHARGE(Doc.of(BOOLEAN)//
				.accessMode(READ_ONLY)), //
		EXTERNAL_RELAY(Doc.of(ActiveInactive.values())//
				.accessMode(READ_ONLY)), //
		HISTORY_MIN_CELL_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)), //
		HISTORY_MAX_CELL_VOLTAGE(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(VOLT)),

		HAS_EMERGENCY_RESERVE(Doc.of(BOOLEAN)//
				.accessMode(READ_ONLY)),
		EMERGENCY_RESERVE_ENABLED(Doc.of(BOOLEAN)//
				.accessMode(READ_ONLY)),
		EMERGENCY_RESERVE_SOC(Doc.of(INTEGER)//
				.accessMode(READ_ONLY)//
				.unit(PERCENT)//
		)

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

	// Set DC Discharge Energy
	public default Value<Long> getDcDischargeEnergy() {
		return this.getDcDischargeEnergyChannel().value();
	}

	public default LongReadChannel getDcDischargeEnergyChannel() {
		return this.channel(ChannelId.DC_DISCHARGED_ENERGY);
	}

	// Set DC Charge Energy
	public default Value<Long> getDcChargeEnergy() {
		return this.getDcChargeEnergyChannel().value();
	}

	public default LongReadChannel getDcChargeEnergyChannel() {
		return this.channel(ChannelId.DC_CHARGED_ENERGY);
	}

	public default IntegerReadChannel getCapacityInAmphoursChannel() {
		return this.channel(ChannelId.CAPACITY_IN_AMPHOURS);
	}

	public default Value<Integer> getCapacityInAmphours() {
		return this.getCapacityInAmphoursChannel().value();
	}

	/**
	 * PLACEHOLDER JAVADOC_COMMENT.
	 *
	 * @param value the value
	 */
	public default void _setCapacityInAmphours(Integer value) {
		this.getCapacityInAmphoursChannel().setNextValue(value);
	}

	public void setMinSocPercentage(int minSocPercentage);
}
