package io.openems.edge.sungrow.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumWriteChannel;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.sungrow.ess.enums.ChargeDischargeCommand;
import io.openems.edge.sungrow.ess.enums.EmsMode;
import io.openems.edge.sungrow.ess.enums.EnableDisable;
import io.openems.edge.sungrow.ess.enums.SystemState;

public interface EssSungrow extends OpenemsComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		SERIAL_NUMBER(Doc.of(OpenemsType.STRING) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		DAILY_OUTPUT_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)), //
		INSIDE_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		MPPT1_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)),
		MPPT1_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		MPPT2_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		MPPT2_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)), //
		TOTAL_DC_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)), //
		VOLTAGE_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		VOLTAGE_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)),
		VOLTAGE_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT)), //
		POWER_FACTOR(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.THOUSANDTH)), //
		GRID_FREQUENCY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIHERTZ) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		EXPORT_LIMIT_MIN(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		EXPORT_LIMIT_MAX(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		BDC_RATED_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		CHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		DISCHARGE_MAX_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		SYSTEM_STATE(Doc.of(SystemState.values()) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		POWER_GENERATED_FROM_PV(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		BATTERY_CHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		BATTERY_DISCHARGING(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		POSITIVE_LOAD_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		FEED_IN_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		IMPORT_POWER_FROM_GRID(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		NEGATIVE_LOAD_POWER(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		DAILY_PV_GENERATION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)), //
		TOTAL_PV_GENERATION(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)), //
		DAILY_EXPORT_POWER_FROM_PV(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //
		TOTAL_EXPORT_ENERGY_FROM_PV(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		LOAD_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.MEDIUM)), //
		EXPORT_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.MEDIUM)),
		DAILY_BATTERY_CHARGE_ENERGY_FROM_PV(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		TOTAL_BATTERY_CHARGE_ENERGY_FROM_PV(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		CO2_REDUCTION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		DAILY_DIRECT_ENERGY_CONSUMPTION(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS)),
		TOTAL_DIRECT_ENERGY_CONSUMPTION(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.VOLT) //
				.persistencePriority(PersistencePriority.HIGH)),
		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.AMPERE) //
				.persistencePriority(PersistencePriority.HIGH)),
		BATTERY_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.persistencePriority(PersistencePriority.HIGH)),
		SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)),
		BATTERY_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEZIDEGREE_CELSIUS) //
				.persistencePriority(PersistencePriority.MEDIUM)),
		DAILY_BATTERY_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		SELF_CONSUMPTION_OF_TODAY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT)),
		CURRENT_L1(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),
		CURRENT_L2(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),
		CURRENT_L3(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE)),
		DAILY_IMPORT_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		TOTAL_IMPORT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),
		DAILY_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		DAILY_EXPORT_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT_HOURS) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		TOTAL_EXPORT_ENERGY(Doc.of(OpenemsType.LONG) //
				.unit(Unit.WATT_HOURS)),

		EMS_MODE(Doc.of(EmsMode.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)),
		CHARGE_DISCHARGE_COMMAND(Doc.of(ChargeDischargeCommand.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)),
		CHARGE_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)),
		MAX_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		MIN_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		EXPORT_POWER_LIMIT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		OFF_GRID_OPTION(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		HEARTBEAT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.NONE) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH)), //
		DEBUG_HEARTBEAT(
				Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)), //
		METER_COMM_DETECTION(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		EXPORT_POWER_LIMITATION(Doc.of(EnableDisable.values()) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)),
		RESERVED_SOC_FOR_BACKUP(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.VERY_LOW)), //
		SMART_MODE_NOT_WORKING_WITH_PID_FILTER(Doc.of(Level.WARNING) //
				.text("SMART mode does not work correctly with active PID filter")) //
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


	/**
	 * Gets the Channel for {@link ChannelId#MPPT1_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMppt1VoltageChannel() {
		return this.channel(ChannelId.MPPT1_VOLTAGE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MPPT1_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMppt1CurrentChannel() {
		return this.channel(ChannelId.MPPT1_CURRENT);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#MPPT2_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMppt2VoltageChannel() {
		return this.channel(ChannelId.MPPT2_VOLTAGE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MPPT2_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMppt2CurrentChannel() {
		return this.channel(ChannelId.MPPT2_CURRENT);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#TOTAL_DC_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getTotalDcPowerChannel() {
		return this.channel(ChannelId.TOTAL_DC_POWER);
	}

	/**
	 * Gets the {@link ChannelId#TOTAL_DC_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getTotalDcPower() {
		return this.getTotalDcPowerChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_L1}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL1Channel() {
		return this.channel(ChannelId.VOLTAGE_L1);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_L2}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL2Channel() {
		return this.channel(ChannelId.VOLTAGE_L2);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_L3}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL3Channel() {
		return this.channel(ChannelId.VOLTAGE_L3);
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#GRID_FREQUENCY}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getGridFrequencyChannel() {
		return this.channel(ChannelId.GRID_FREQUENCY);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getChargeMaxCurrentChannel() {
		return this.channel(ChannelId.CHARGE_MAX_CURRENT);
	}

	/**
	 * Gets the {@link ChannelId#CHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargeMaxCurrent() {
		return this.getChargeMaxCurrentChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getDischargeMaxCurrentChannel() {
		return this.channel(ChannelId.DISCHARGE_MAX_CURRENT);
	}
	
	/**
	 * Gets the {@link ChannelId#DISCHARGE_MAX_CURRENT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getDischargeMaxCurrent() {
		return this.getDischargeMaxCurrentChannel().value();
	}
	
	/**
	 * Gets the Channel for {@link ChannelId#EXPORT_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getExportPowerChannel() {
		return this.channel(ChannelId.EXPORT_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BATTERY_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getBatteryVoltageChannel() {
		return this.channel(ChannelId.BATTERY_VOLTAGE);
	}
	
	/**
	 * Gets the {@link ChannelId#BATTERY_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getBatteryVoltage() {
		return this.getBatteryVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#TOTAL_IMPORT_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getTotalImportEnergyChannel() {
		return this.channel(ChannelId.TOTAL_IMPORT_ENERGY);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TOTAL_EXPORT_ENERGY}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getTotalExportEnergyChannel() {
		return this.channel(ChannelId.TOTAL_EXPORT_ENERGY);
	}

	/**
	 * Gets the Channel for {@link ChannelId#EMS_MODE}.
	 *
	 * @return the Channel
	 */
	public default EnumWriteChannel getEmsModeChannel() {
		return this.channel(ChannelId.EMS_MODE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_DISCHARGE_COMMAND}.
	 *
	 * @return the Channel
	 */
	public default EnumWriteChannel getChargeDischargeCommandChannel() {
		return this.channel(ChannelId.CHARGE_DISCHARGE_COMMAND);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHARGE_DISCHARGE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getChargeDischargePowerChannel() {
		return this.channel(ChannelId.CHARGE_DISCHARGE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HEARTBEAT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getHeartbeatChannel() {
		return this.channel(ChannelId.HEARTBEAT);
	}
}