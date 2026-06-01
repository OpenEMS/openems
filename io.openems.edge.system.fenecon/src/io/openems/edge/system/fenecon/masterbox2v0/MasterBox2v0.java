package io.openems.edge.system.fenecon.masterbox2v0;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.LongReadChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.system.fenecon.masterbox2v0.enums.GridState;
import io.openems.edge.system.fenecon.masterbox2v0.enums.StateEnergyMeter;

public interface MasterBox2v0 extends OpenemsComponent {
	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		TEMPERATURE(Doc.of(OpenemsType.DOUBLE) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEGREE_CELSIUS)),

		HUMIDITY(Doc.of(OpenemsType.DOUBLE) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.PERCENT)),

		/**
		 * Hardware config: Startup.<br>
		 * false -> FEMS<br>
		 * true -> IOC
		 */
		HW_STARTUP(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Hardware config: temperature sensor type.<br>
		 * false -> WUERTH (2525020210002)<br>
		 * true -> TI (TMP75AIDR)
		 */
		HW_TEMPERATURE_SENSOR_TYPE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Hardware config: analog out mode.<br>
		 * false -> FEMS<br>
		 * true -> IOC
		 */
		HW_ANALOG_OUT_MODE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)),

		HW_SPI_ENERGY_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)),

		HW_CAN_TOWER_ENABLE(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_ONLY)),

		GRID_STATE(Doc.of(GridState.values()) //
				.accessMode(AccessMode.READ_ONLY)),

		DEBUG_RELAY_1(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_1(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_1)),

		DEBUG_RELAY_2(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_2(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_2)),

		DEBUG_RELAY_3(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_3(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_3)),

		DEBUG_RELAY_4(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_4(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_4)),

		DEBUG_RELAY_5(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_5(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_5)),

		DEBUG_RELAY_6(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)), //

		RELAY_6(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.persistencePriority(PersistencePriority.HIGH) //
				.onChannelSetNextWriteMirrorToDebugChannel(ChannelId.DEBUG_RELAY_6)),

		TIME_STAMP_ENERGYMETER(Doc.of(OpenemsType.LONG) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		STATUS_ENERGYMETER(Doc.of(StateEnergyMeter.values()) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		VOLTAGE_L1_ENERGYMETER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		VOLTAGE_L2_ENERGYMETER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		VOLTAGE_L3_ENERGYMETER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		CURRENT_L1_ENERGYMETER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		CURRENT_L2_ENERGYMETER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		CURRENT_L3_ENERGYMETER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		ACTIVE_POWER_L1_ENERGYMETER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		ACTIVE_POWER_L2_ENERGYMETER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		ACTIVE_POWER_L3_ENERGYMETER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY) //
				.persistencePriority(PersistencePriority.HIGH)),

		DEBUG_ANALOG_OUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.persistencePriority(PersistencePriority.MEDIUM)),

		ANALOG_OUT_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_ANALOG_OUT_VOLTAGE)),

		DEBUG_ANALOG_OUT_CONTROL(Doc.of(OpenemsType.BOOLEAN) //
				.persistencePriority(PersistencePriority.MEDIUM)),

		ANALOG_OUT_CONTROL(Doc.of(OpenemsType.BOOLEAN) //
				.accessMode(AccessMode.READ_WRITE) //
				.onChannelSetNextWriteMirrorToDebugChannel(DEBUG_ANALOG_OUT_CONTROL));

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
	 * Gets the Channel for {@link ChannelId#RELAY_1}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay1Channel() {
		return this.channel(MasterBox2v0.ChannelId.RELAY_1);
	}

	/**
	 * Gets the boolean value of the first relay. See {@link ChannelId#RELAY_1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay1() {
		return this.getRelay1Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_2}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay2Channel() {
		return this.channel(MasterBox2v0.ChannelId.RELAY_2);
	}

	/**
	 * Gets the boolean value of the second relay. See {@link ChannelId#RELAY_2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay2() {
		return this.getRelay2Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_3}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay3Channel() {
		return this.channel(MasterBox2v0.ChannelId.RELAY_3);
	}

	/**
	 * Gets the boolean value of the third relay. See {@link ChannelId#RELAY_3}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay3() {
		return this.getRelay3Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_4}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay4Channel() {
		return this.channel(MasterBox2v0.ChannelId.RELAY_4);
	}

	/**
	 * Gets the boolean value of the fourth relay. See {@link ChannelId#RELAY_4}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay4() {
		return this.getRelay4Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_5}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay5Channel() {
		return this.channel(MasterBox2v0.ChannelId.RELAY_5);
	}

	/**
	 * Gets the boolean value of the fifth relay. See {@link ChannelId#RELAY_5}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay5() {
		return this.getRelay5Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#RELAY_3}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getRelay6Channel() {
		return this.channel(MasterBox2v0.ChannelId.RELAY_6);
	}

	/**
	 * Gets the boolean value of the sixth relay. See {@link ChannelId#RELAY_6}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getRelay6() {
		return this.getRelay6Channel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#TIME_STAMP_ENERGYMETER}.
	 *
	 * @return the Channel
	 */
	public default LongReadChannel getTimeStampEnergyMeterChannel() {
		return this.channel(ChannelId.TIME_STAMP_ENERGYMETER);
	}

	/**
	 * Gets the timestamp value of the energy meter. See
	 * {@link ChannelId#TIME_STAMP_ENERGYMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Long> getTimeStampEnergyMeter() {
		return this.getTimeStampEnergyMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#STATUS_ENERGYMETER}.
	 *
	 * @return the Channel
	 */
	public default Channel<StateEnergyMeter> getStatusEnergyMeterChannel() {
		return this.channel(ChannelId.STATUS_ENERGYMETER);
	}

	/**
	 * Gets the status value of the energy meter. See
	 * {@link ChannelId#STATUS_ENERGYMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<StateEnergyMeter> getStatusEnergyMeter() {
		return this.getStatusEnergyMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_L1_ENERGYMETER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL1EnergyMeterChannel() {
		return this.channel(MasterBox2v0.ChannelId.VOLTAGE_L1_ENERGYMETER);
	}

	/**
	 * Gets the voltage value of phase L1 of the energy meter in volt. See
	 * {@link ChannelId#VOLTAGE_L1_ENERGYMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL1EnergyMeter() {
		return this.getVoltageL1EnergyMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_L2_ENERGYMETER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL2EnergyMeterChannel() {
		return this.channel(MasterBox2v0.ChannelId.VOLTAGE_L2_ENERGYMETER);
	}

	/**
	 * Gets the voltage value of phase L2 of the energy meter in volt. See
	 * {@link ChannelId#VOLTAGE_L2_ENERGYMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL2EnergyMeter() {
		return this.getVoltageL2EnergyMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#VOLTAGE_L3_ENERGYMETER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getVoltageL3EnergyMeterChannel() {
		return this.channel(MasterBox2v0.ChannelId.VOLTAGE_L3_ENERGYMETER);
	}

	/**
	 * Gets the voltage value of phase L3 of the energy meter in volt. See
	 * {@link ChannelId#VOLTAGE_L3_ENERGYMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getVoltageL3EnergyMeter() {
		return this.getVoltageL3EnergyMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L1_ENERGYMETER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL1EnergyMeterChannel() {
		return this.channel(MasterBox2v0.ChannelId.CURRENT_L1_ENERGYMETER);
	}

	/**
	 * Gets the current value of phase L1 of the energy meter in ampere. See
	 * {@link ChannelId#CURRENT_L1_ENERGYMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL1EnergyMeter() {
		return this.getCurrentL1EnergyMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L2_ENERGYMETER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL2EnergyMeterChannel() {
		return this.channel(MasterBox2v0.ChannelId.CURRENT_L2_ENERGYMETER);
	}

	/**
	 * Gets the current value of phase L2 of the energy meter in ampere. See
	 * {@link ChannelId#CURRENT_L2_ENERGYMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL2EnergyMeter() {
		return this.getCurrentL2EnergyMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CURRENT_L3_ENERGYMETER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getCurrentL3EnergyMeterChannel() {
		return this.channel(MasterBox2v0.ChannelId.CURRENT_L3_ENERGYMETER);
	}

	/**
	 * Gets the current value of phase L3 of the energy meter in ampere. See
	 * {@link ChannelId#CURRENT_L3_ENERGYMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getCurrentL3EnergyMeter() {
		return this.getCurrentL3EnergyMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L1_ENERGYMETER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerL1EnergyMeterChannel() {
		return this.channel(MasterBox2v0.ChannelId.ACTIVE_POWER_L1_ENERGYMETER);
	}

	/**
	 * Gets the active power value of phase L1 of the energy meter in watt. See
	 * {@link ChannelId#ACTIVE_POWER_L1_ENERGYMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerL1EnergyMeter() {
		return this.getActivePowerL1EnergyMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L2_ENERGYMETER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerL2EnergyMeterChannel() {
		return this.channel(MasterBox2v0.ChannelId.ACTIVE_POWER_L2_ENERGYMETER);
	}

	/**
	 * Gets the active power value of phase L2 of the energy meter in watt. See
	 * {@link ChannelId#ACTIVE_POWER_L2_ENERGYMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerL2EnergyMeter() {
		return this.getActivePowerL2EnergyMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER_L3_ENERGYMETER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getActivePowerL3EnergyMeterChannel() {
		return this.channel(MasterBox2v0.ChannelId.ACTIVE_POWER_L3_ENERGYMETER);
	}

	/**
	 * Gets the active power value of phase L3 of the energy meter in watt. See
	 * {@link ChannelId#ACTIVE_POWER_L3_ENERGYMETER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getActivePowerL3EnergyMeter() {
		return this.getActivePowerL3EnergyMeterChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ANALOG_OUT_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getAnalogOutVoltageChannel() {
		return this.channel(ChannelId.ANALOG_OUT_VOLTAGE);
	}

	/**
	 * Gets the boolean value of the analog output voltage. See
	 * {@link ChannelId#ANALOG_OUT_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getAnalogOutVoltage() {
		return this.getAnalogOutVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ANALOG_OUT_CONTROL}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getAnalogOutControlChannel() {
		return this.channel(ChannelId.ANALOG_OUT_CONTROL);
	}

	/**
	 * Gets the boolean value of the analog output. See
	 * {@link ChannelId#ANALOG_OUT_CONTROL}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getAnalogOutControl() {
		return this.getAnalogOutControlChannel().value();
	}
}
