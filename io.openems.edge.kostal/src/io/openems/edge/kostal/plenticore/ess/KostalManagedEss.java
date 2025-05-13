package io.openems.edge.kostal.plenticore.ess;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerReadChannel;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.ess.api.ManagedSymmetricEss;
import io.openems.edge.ess.api.SymmetricEss;
import io.openems.edge.kostal.plenticore.enums.BatteryManagementMode;
import io.openems.edge.kostal.plenticore.enums.BatteryType;
import io.openems.edge.kostal.plenticore.enums.EnergyManagerMode;
import io.openems.edge.kostal.plenticore.enums.FuseState;
import io.openems.edge.kostal.plenticore.enums.InverterState;
import io.openems.edge.kostal.plenticore.enums.SensorType;

public interface KostalManagedEss
		extends
			ManagedSymmetricEss,
			SymmetricEss,
			ModbusComponent,
			OpenemsComponent {

	public static enum ChannelId
			implements
				io.openems.edge.common.channel.ChannelId {

		// EnumReadChannels
		/**
		 * Represents the state of the inverter.
		 */
		INVERTER_STATE(Doc.of(InverterState.values())), //
		/**
		 * Represents the state of the fuse.
		 */
		FUSE_STATE(Doc.of(FuseState.values())), //
		/**
		 * Represents the energy manager mode.
		 */
		ENERGY_MANAGER_MODE(Doc.of(EnergyManagerMode.values())), //
		/**
		 * Represents the operating mode for battery management.
		 */
		OPERATING_MODE_FOR_BATTERY_MANAGEMENT(
				Doc.of(BatteryManagementMode.values())), //

		// EnumWriteChannels
		/**
		 * Specifies the type of battery used.
		 */
		BATTERY_TYPE(Doc.of(BatteryType.values())), //
		/**
		 * Specifies the type of sensor used.
		 */
		SENSOR_TYPE(Doc.of(SensorType.values())), //

		// LongReadChannels
		/**
		 * Represents the serial number of the device. Data is persisted with
		 * high priority.
		 */
		SERIAL_NUMBER(Doc.of(OpenemsType.LONG)
				.persistencePriority(PersistencePriority.HIGH) //
		), //

		/**
		 * Represents the grid voltage of phase L1 in volts.
		 */
		GRID_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT) //
		), //
		/**
		 * Represents the grid voltage of phase L2 in volts.
		 */
		GRID_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT) //
		), //
		/**
		 * Represents the grid voltage of phase L3 in volts.
		 */
		GRID_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT) //
		), //

		/**
		 * Represents the grid frequency in hertz.
		 */
		FREQUENCY(Doc.of(OpenemsType.INTEGER).unit(Unit.HERTZ) //
		), //

		// IntegerWriteChannels
		/**
		 * Sets the active power in watts. This channel is write-only.
		 */
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)), //

		/**
		 * Sets the reactive power in volt-amperes. This channel is write-only.
		 */
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.VOLT_AMPERE)), //

		/**
		 * Represents the current charge power in watts. This channel is
		 * read-only.
		 */
		CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Represents the desired charge/discharge power in watts. Defined by
		 * external controllers.
		 */
		CHARGE_POWER_WANTED(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT)), //

		/**
		 * Represents the maximum charge power in watts. This channel is
		 * read-only.
		 */
		MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Represents the maximum discharge power in watts. This channel is
		 * read-only.
		 */
		MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		/**
		 * Sets the maximum charge power in watts. This channel is write-only.
		 */
		SET_MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)),

		/**
		 * Sets the maximum discharge power in watts. This channel is
		 * write-only.
		 */
		SET_MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)),

		/**
		 * Represents the current battery capacity as a percentage.
		 */
		CURRENT_BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
		), //

		/**
		 * Represents the battery voltage in millivolts.
		 */
		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT) //
		), //

		/**
		 * Represents the battery temperature in degrees Celsius.
		 */
		BATTERY_TEMPERATURE(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS) //
		), //

		/**
		 * Represents the battery current in amperes.
		 */
		BATTERY_CURRENT(Doc.of(OpenemsType.INTEGER).unit(Unit.AMPERE) //
		);

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
	 * Sets the desired charge power on the
	 * {@link ChannelId#CHARGE_POWER_WANTED} channel.
	 *
	 * @param value
	 *            the next value to set for charge/discharge power
	 */
	public default void _setChargePowerWanted(Integer value) {
		this.getChargePowerWantedChannel().setNextValue(value);
	}

	/**
	 * Sets the desired charge power on the {@link ChannelId#CHARGE_POWER}
	 * channel.
	 *
	 * @param value
	 *            the next value to set for charge power
	 */
	public default void _setChargePower(Integer value) {
		System.out.println("setChargePower called... setting value: " + value);
		this.getSetChargePowerChannel().setNextValue(value);
	}

	/**
	 * Sets the maximum charge power on the
	 * {@link ChannelId#SET_MAX_CHARGE_POWER} channel.
	 *
	 * @param value
	 *            the next value to set for max charge power
	 * @throws OpenemsNamedException
	 *             if an error occurs while setting the value
	 */
	public default void _setMaxChargePower(Integer value)
			throws OpenemsNamedException {
		this.getSetMaxChargePowerChannel().setNextWriteValue(value);
	}

	/**
	 * Sets the maximum discharge power on the
	 * {@link ChannelId#SET_MAX_DISCHARGE_POWER} channel.
	 *
	 * @param value
	 *            the next value to set for max discharge power
	 * @throws OpenemsNamedException
	 *             if an error occurs while setting the value
	 */
	public default void _setMaxDischargePower(Integer value)
			throws OpenemsNamedException {
		this.getSetMaxDischargePowerChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the current maximum charge power from the
	 * {@link ChannelId#MAX_CHARGE_POWER} channel.
	 *
	 * @return the current maximum charge power as a {@link Value}
	 */
	public default Value<Integer> getMaxChargePower() {
		return this.getMaxChargePowerChannel().value();
	}

	/**
	 * Gets the {@link Channel} for maximum charge power.
	 *
	 * @return the {@link IntegerReadChannel} for max charge power
	 */
	public default IntegerReadChannel getMaxChargePowerChannel() {
		return this.channel(ChannelId.MAX_CHARGE_POWER);
	}

	/**
	 * Gets the {@link Channel} for setting charge power.
	 *
	 * @return the {@link IntegerWriteChannel} for charge power
	 */
	public default IntegerWriteChannel getSetChargePowerChannel() {
		return this.channel(ChannelId.CHARGE_POWER);
	}

	/**
	 * Gets the current maximum discharge power from the
	 * {@link ChannelId#MAX_DISCHARGE_POWER} channel.
	 *
	 * @return the current maximum discharge power as a {@link Value}
	 */
	public default Value<Integer> getMaxDischargePower() {
		return this.getMaxDischargePowerChannel().value();
	}

	/**
	 * Gets the {@link Channel} for maximum discharge power.
	 *
	 * @return the {@link IntegerReadChannel} for max discharge power
	 */
	public default IntegerReadChannel getMaxDischargePowerChannel() {
		return this.channel(ChannelId.MAX_DISCHARGE_POWER);
	}

	/**
	 * Gets the current charge power wanted from the
	 * {@link ChannelId#CHARGE_POWER_WANTED} channel.
	 *
	 * @return the desired charge power as a {@link Value}
	 */
	public default Value<Integer> getChargePowerWanted() {
		return this.getChargePowerWantedChannel().value();
	}

	/**
	 * Gets the {@link Channel} for charge power wanted.
	 *
	 * @return the {@link IntegerReadChannel} for charge power wanted
	 */
	public default IntegerReadChannel getChargePowerWantedChannel() {
		return this.channel(ChannelId.CHARGE_POWER_WANTED);
	}

	/**
	 * Gets the {@link Channel} for setting maximum charge power.
	 *
	 * @return the {@link IntegerWriteChannel} for max charge power
	 */
	public default IntegerWriteChannel getSetMaxChargePowerChannel() {
		return this.channel(ChannelId.SET_MAX_CHARGE_POWER);
	}

	/**
	 * Gets the {@link Channel} for setting maximum discharge power.
	 *
	 * @return the {@link IntegerWriteChannel} for max discharge power
	 */
	public default IntegerWriteChannel getSetMaxDischargePowerChannel() {
		return this.channel(ChannelId.SET_MAX_DISCHARGE_POWER);
	}
}
