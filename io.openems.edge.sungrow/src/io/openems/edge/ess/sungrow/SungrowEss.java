package io.openems.edge.ess.sungrow;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.PersistencePriority.HIGH;
import static io.openems.common.channel.PersistencePriority.MEDIUM;
import static io.openems.common.channel.PersistencePriority.VERY_HIGH;
import static io.openems.common.channel.PersistencePriority.VERY_LOW;
import static io.openems.common.channel.Unit.AMPERE;
import static io.openems.common.channel.Unit.DEZIDEGREE_CELSIUS;
import static io.openems.common.channel.Unit.MILLIAMPERE;
import static io.openems.common.channel.Unit.MILLIHERTZ;
import static io.openems.common.channel.Unit.MILLIVOLT;
import static io.openems.common.channel.Unit.PERCENT;
import static io.openems.common.channel.Unit.THOUSANDTH;
import static io.openems.common.channel.Unit.VOLT;
import static io.openems.common.channel.Unit.WATT;
import static io.openems.common.channel.Unit.WATT_HOURS;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;
import static io.openems.common.types.OpenemsType.LONG;
import static io.openems.common.types.OpenemsType.STRING;

import io.openems.edge.common.channel.BooleanReadChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.sungrow.enums.ChargeDischargeCommand;
import io.openems.edge.ess.sungrow.enums.EmsMode;
import io.openems.edge.ess.sungrow.enums.EnableDisable;
import io.openems.edge.ess.sungrow.enums.SystemState;

public interface SungrowEss extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SERIAL_NUMBER(Doc.of(STRING)//
				.persistencePriority(VERY_LOW)), //
		NOMINAL_OUTPUT_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_LOW)),
		DAILY_OUTPUT_ENERGY(Doc.of(INTEGER)//
				.unit(WATT_HOURS)),
		INSIDE_TEMPERATURE(Doc.of(INTEGER)//
				.unit(DEZIDEGREE_CELSIUS)),
		MPPT1_VOLTAGE(Doc.of(INTEGER)//
				.unit(VOLT)),
		MPPT1_CURRENT(Doc.of(INTEGER)//
				.unit(AMPERE)),
		MPPT2_VOLTAGE(Doc.of(INTEGER)//
				.unit(VOLT)), //
		MPPT2_CURRENT(Doc.of(INTEGER)//
				.unit(AMPERE)), //
		TOTAL_DC_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(HIGH)), //
		VOLTAGE_L1(Doc.of(INTEGER)//
				.unit(MILLIVOLT)), //
		VOLTAGE_L2(Doc.of(INTEGER)//
				.unit(MILLIVOLT)),
		VOLTAGE_L3(Doc.of(INTEGER)//
				.unit(MILLIVOLT)), //
		POWER_FACTOR(Doc.of(INTEGER)//
				.unit(THOUSANDTH)), //
		GRID_FREQUENCY(Doc.of(INTEGER)//
				.unit(MILLIHERTZ)//
				.persistencePriority(MEDIUM)), //
		EXPORT_LIMIT_MIN(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_LOW)), //
		EXPORT_LIMIT_MAX(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_LOW)), //
		BDC_RATED_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(VERY_LOW)), //
		CHARGE_MAX_CURRENT(Doc.of(INTEGER)//
				.unit(AMPERE)//
				.persistencePriority(HIGH)), //
		DISCHARGE_MAX_CURRENT(Doc.of(INTEGER)//
				.unit(AMPERE)//
				.persistencePriority(HIGH)), //
		SYSTEM_STATE(Doc.of(SystemState.values())//
				.persistencePriority(VERY_LOW)), //
		POWER_GENERATED_FROM_PV(Doc.of(BOOLEAN)//
				.persistencePriority(VERY_LOW)), //
		BATTERY_CHARGING(Doc.of(BOOLEAN)//
				.persistencePriority(VERY_LOW)), //
		BATTERY_DISCHARGING(Doc.of(BOOLEAN)//
				.persistencePriority(VERY_LOW)), //
		POSITIVE_LOAD_POWER(Doc.of(BOOLEAN)//
				.persistencePriority(VERY_LOW)), //
		FEED_IN_POWER(Doc.of(BOOLEAN)//
				.persistencePriority(VERY_LOW)), //
		IMPORT_POWER_FROM_GRID(Doc.of(BOOLEAN)//
				.persistencePriority(VERY_LOW)), //
		NEGATIVE_LOAD_POWER(Doc.of(BOOLEAN)//
				.persistencePriority(VERY_LOW)), //
		DAILY_PV_GENERATION(Doc.of(INTEGER)//
				.unit(WATT_HOURS)), //
		TOTAL_PV_GENERATION(Doc.of(LONG)//
				.unit(WATT_HOURS)), //
		DAILY_EXPORT_POWER_FROM_PV(Doc.of(INTEGER)//
				.unit(WATT)), //
		TOTAL_EXPORT_ENERGY_FROM_PV(Doc.of(LONG)//
				.unit(WATT_HOURS)), //
		LOAD_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(MEDIUM)), //
		EXPORT_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(MEDIUM)), //
		DAILY_BATTERY_CHARGE_ENERGY_FROM_PV(Doc.of(INTEGER)//
				.unit(WATT_HOURS)), //
		TOTAL_BATTERY_CHARGE_ENERGY_FROM_PV(Doc.of(LONG)//
				.unit(WATT_HOURS)), //
		CO2_REDUCTION(Doc.of(INTEGER)//
				.persistencePriority(VERY_LOW)//
				.text("kg")), //
		DAILY_DIRECT_ENERGY_CONSUMPTION(Doc.of(INTEGER)//
				.unit(WATT_HOURS)), //
		TOTAL_DIRECT_ENERGY_CONSUMPTION(Doc.of(LONG)//
				.unit(WATT_HOURS)), //
		BATTERY_VOLTAGE(Doc.of(INTEGER)//
				.unit(VOLT)//
				.persistencePriority(MEDIUM)), //
		BATTERY_CURRENT(Doc.of(INTEGER)//
				.unit(AMPERE)//
				.persistencePriority(MEDIUM)),
		BATTERY_POWER(Doc.of(INTEGER)//
				.unit(WATT)//
				.persistencePriority(MEDIUM)), //
		SOH(Doc.of(INTEGER)//
				.unit(PERCENT)), //
		BATTERY_TEMPERATURE(Doc.of(INTEGER)//
				.unit(DEZIDEGREE_CELSIUS)//
				.persistencePriority(MEDIUM)), //
		DAILY_BATTERY_DISCHARGE_ENERGY(Doc.of(INTEGER)//
				.unit(WATT_HOURS)//
				.persistencePriority(VERY_LOW)),
		SELF_CONSUMPTION_OF_TODAY(Doc.of(INTEGER)//
				.unit(PERCENT)), //
		CURRENT_L1(Doc.of(INTEGER)//
				.unit(MILLIAMPERE)), //
		CURRENT_L2(Doc.of(INTEGER)//
				.unit(MILLIAMPERE)), //
		CURRENT_L3(Doc.of(INTEGER)//
				.unit(MILLIAMPERE)), //
		DAILY_IMPORT_ENERGY(Doc.of(INTEGER)//
				.unit(WATT_HOURS)//
				.persistencePriority(VERY_LOW)), //
		TOTAL_IMPORT_ENERGY(Doc.of(LONG)//
				.unit(WATT_HOURS)), //
		DAILY_CHARGE_ENERGY(Doc.of(INTEGER)//
				.unit(WATT_HOURS)//
				.persistencePriority(VERY_LOW)), //
		DAILY_EXPORT_ENERGY(Doc.of(INTEGER)//
				.unit(WATT_HOURS)//
				.persistencePriority(VERY_LOW)), //
		TOTAL_EXPORT_ENERGY(Doc.of(LONG)//
				.unit(WATT_HOURS)), //

		EMS_MODE(Doc.of(EmsMode.values())//
				.accessMode(READ_WRITE)//
				.persistencePriority(VERY_LOW)), //
		CHARGE_DISCHARGE_COMMAND(Doc.of(ChargeDischargeCommand.values())//
				.accessMode(READ_WRITE)//
				.persistencePriority(HIGH)), //
		CHARGE_DISCHARGE_POWER(Doc.of(INTEGER)//
				.unit(PERCENT)//
				.accessMode(READ_WRITE)//
				.persistencePriority(HIGH)), //
		MAX_SOC(Doc.of(INTEGER).unit(PERCENT)//
				.accessMode(READ_WRITE)//
				.persistencePriority(VERY_LOW)), //
		MIN_SOC(Doc.of(INTEGER).unit(PERCENT)//
				.accessMode(READ_WRITE)//
				.persistencePriority(VERY_LOW)), //
		EXPORT_POWER_LIMIT(Doc.of(INTEGER)//
				.unit(WATT)//
				.accessMode(READ_WRITE)//
				.persistencePriority(VERY_LOW)), //
		OFF_GRID_OPTION(Doc.of(EnableDisable.values())//
				.accessMode(READ_WRITE)//
				.persistencePriority(VERY_LOW)), //
		HEARTBEAT(Doc.of(INTEGER)//
				.accessMode(READ_WRITE)//
				.persistencePriority(VERY_LOW)), //
		DEBUG_HEARTBEAT(Doc.of(INTEGER)//
				.persistencePriority(VERY_HIGH)), //
		METER_COMM_DETECTION(Doc.of(EnableDisable.values())//
				.accessMode(READ_WRITE)//
				.persistencePriority(VERY_LOW)), //
		EXPORT_POWER_LIMITATION(Doc.of(EnableDisable.values())//
				.accessMode(READ_WRITE)//
				.persistencePriority(VERY_LOW)), //
		RESERVED_SOC_FOR_BACKUP(Doc.of(INTEGER)//
				.unit(PERCENT)//
				.accessMode(READ_WRITE)//
				.persistencePriority(VERY_LOW));

		private final Doc doc;

		private ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	public default IntegerWriteChannel getHeartbeatChannel() {
		return this.channel(ChannelId.HEARTBEAT);
	}

	public default EnumWriteChannel getEmsModeChannel() {
		return this.channel(ChannelId.EMS_MODE);
	}

	public default EnumWriteChannel getChargeDischargeCommandChannel() {
		return this.channel(ChannelId.CHARGE_DISCHARGE_COMMAND);
	}

	public default IntegerWriteChannel getChargeDischargePowerChannel() {
		return this.channel(ChannelId.CHARGE_DISCHARGE_POWER);
	}

	public default BooleanReadChannel getBatteryChargingChannel() {
		return this.channel(ChannelId.BATTERY_CHARGING);
	}

	public default BooleanReadChannel getBatteryDischargingChannel() {
		return this.channel(ChannelId.BATTERY_DISCHARGING);
	}

	public default IntegerReadChannel getGridFrequencyChannel() {
		return this.channel(ChannelId.GRID_FREQUENCY);
	}

	public default IntegerReadChannel getTotalDcPowerChannel() {
		return this.channel(ChannelId.TOTAL_DC_POWER);
	}

	public default IntegerReadChannel getExportPowerChannel() {
		return this.channel(ChannelId.EXPORT_POWER);
	}

	public default LongReadChannel getTotalBatteryChargeEnergyFromPvChannel() {
		return this.channel(ChannelId.TOTAL_BATTERY_CHARGE_ENERGY_FROM_PV);
	}

	public default Value<Long> getTotalBatteryChargeEnergyFromPv() {
		return this.getTotalBatteryChargeEnergyFromPvChannel().value();
	}

	public default LongReadChannel getTotalPvGenerationChannel() {
		return this.channel(ChannelId.TOTAL_PV_GENERATION);
	}

	public default IntegerReadChannel getBatteryPowerChannel() {
		return this.channel(ChannelId.BATTERY_POWER);
	}

	public default LongReadChannel getTotalImportEnergyChannel() {
		return this.channel(ChannelId.TOTAL_IMPORT_ENERGY);
	}

	public default LongReadChannel getTotalExportEnergyChannel() {
		return this.channel(ChannelId.TOTAL_EXPORT_ENERGY);
	}

}
