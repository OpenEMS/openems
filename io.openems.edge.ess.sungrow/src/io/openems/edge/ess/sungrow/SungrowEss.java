package io.openems.edge.ess.sungrow;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
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
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		NOMINAL_OUTPUT_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		DAILY_OUTPUT_ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		INSIDE_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		MPPT1_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		MPPT1_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		MPPT2_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		MPPT2_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		TOTAL_DC_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		POWER_FACTOR(Doc.of(OpenemsType.INTEGER).unit(Unit.THOUSANDTH) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		GRID_FREQUENCY(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIHERTZ) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		EXPORT_LIMIT_MIN(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		EXPORT_LIMIT_MAX(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		BDC_RATED_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //
		DISCHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //
		SYSTEM_STATE(Doc.of(SystemState.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		POWER_GENERATED_FROM_PV(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		BATTERY_CHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		BATTERY_DISCHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		POSITIVE_LOAD_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		FEED_IN_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		IMPORT_POWER_FROM_GRID(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		NEGATIVE_LOAD_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		DAILY_PV_GENERATION(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		TOTAL_PV_GENERATION(Doc.of(OpenemsType.LONG).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		DAILY_EXPORT_POWER_FROM_PV(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		TOTAL_EXPORT_ENERGY_FROM_PV(Doc.of(OpenemsType.LONG).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)),
		LOAD_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		EXPORT_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		DAILY_BATTERY_CHARGE_ENERGY_FROM_PV(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		TOTAL_BATTERY_CHARGE_ENERGY_FROM_PV(Doc.of(OpenemsType.LONG).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		CO2_REDUCTION(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW).text("kg")), //
		DAILY_DIRECT_ENERGY_CONSUMPTION(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		TOTAL_DIRECT_ENERGY_CONSUMPTION(Doc.of(OpenemsType.LONG).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.MEDIUM)),
		BATTERY_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		SOH(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER).unit(Unit.DEZIDEGREE_CELSIUS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		DAILY_BATTERY_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		SELF_CONSUMPTION_OF_TODAY(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		CURRENT_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		CURRENT_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		CURRENT_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		DAILY_IMPORT_ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		TOTAL_IMPORT_ENERGY(Doc.of(OpenemsType.LONG).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //
		DAILY_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		DAILY_EXPORT_ENERGY(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		TOTAL_EXPORT_ENERGY(Doc.of(OpenemsType.LONG).unit(Unit.WATT_HOURS) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.LOW)), //

		EMS_MODE(Doc.of(EmsMode.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		CHARGE_DISCHARGE_COMMAND(Doc.of(ChargeDischargeCommand.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		CHARGE_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		MAX_SOC(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		MIN_SOC(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		EXPORT_POWER_LIMIT(Doc.of(OpenemsType.INTEGER).unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		OFF_GRID_OPTION(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		HEARTBEAT(Doc.of(OpenemsType.INTEGER).unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		DEBUG_HEARTBEAT(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.VERY_HIGH)), //
		METER_COMM_DETECTION(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		EXPORT_POWER_LIMITATION(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		RESERVED_SOC_FOR_BACKUP(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW));

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
