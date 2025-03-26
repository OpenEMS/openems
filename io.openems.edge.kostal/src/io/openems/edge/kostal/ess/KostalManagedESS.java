package io.openems.edge.kostal.ess;

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
import io.openems.edge.kostal.enums.BatteryManagementMode;
import io.openems.edge.kostal.enums.BatteryType;
import io.openems.edge.kostal.enums.EnergyManagerMode;
import io.openems.edge.kostal.enums.FuseState;
import io.openems.edge.kostal.enums.InverterState;
import io.openems.edge.kostal.enums.SensorType;
import io.openems.edge.kostal.ess2.KostalManagedEss.ChannelId;

public interface KostalManagedESS
		extends
			ManagedSymmetricEss,
			SymmetricEss,
			ModbusComponent,
			OpenemsComponent {
	public static enum ChannelId
			implements
				io.openems.edge.common.channel.ChannelId {
		// EnumReadChannels
		INVERTER_STATE(Doc.of(InverterState.values())), //
		FUSE_STATE(Doc.of(FuseState.values())), //
		ENERGY_MANAGER_MODE(Doc.of(EnergyManagerMode.values())), //
		OPERATING_MODE_FOR_BATTERY_MANAGEMENT(
				Doc.of(BatteryManagementMode.values())), //

		// EnumWriteChannsl
		BATTERY_TYPE(Doc.of(BatteryType.values())), //
		SENSOR_TYPE(Doc.of(SensorType.values())), //

		// LongReadChannels
		SERIAL_NUMBER(Doc.of(OpenemsType.LONG)
				.persistencePriority(PersistencePriority.HIGH) //
		), //

		GRID_VOLTAGE_L1(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT) //
		), //
		GRID_VOLTAGE_L2(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT) //
		), //
		GRID_VOLTAGE_L3(Doc.of(OpenemsType.INTEGER).unit(Unit.VOLT) //
		), //

		FREQUENCY(Doc.of(OpenemsType.INTEGER).unit(Unit.HERTZ) //
		), //

		// IntegerWriteChannels
		SET_ACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.WATT)), //
		SET_REACTIVE_POWER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.WRITE_ONLY) //
				.unit(Unit.VOLT_AMPERE)), //

		CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		CHARGE_POWER_WANTED(Doc.of(OpenemsType.INTEGER) // Charge/Discharge-Power
														// wanted from
														// controllers
				.unit(Unit.WATT)), // defined in external file

		MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.READ_ONLY)),

		SET_MAX_CHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)),

		SET_MAX_DISCHARGE_POWER(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.WATT) //
				.accessMode(AccessMode.WRITE_ONLY)),

		CURRENT_BATTERY_CAPACITY(Doc.of(OpenemsType.INTEGER).unit(Unit.PERCENT) //
		), //

		BATTERY_VOLTAGE(Doc.of(OpenemsType.INTEGER).unit(Unit.MILLIVOLT) //
		), //
		BATTERY_TEMPERATURE(
				Doc.of(OpenemsType.INTEGER).unit(Unit.DEGREE_CELSIUS) //
		), //
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
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_POWER_WANTED} Channel.
	 *
	 * @param value
	 *            the next value
	 */
	public default void _setChargePowerWanted(Integer value) {
		this.getChargePowerWantedChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#CHARGE_POWER_WANTED} Channel.
	 *
	 * @param i
	 *            the next value
	 */
	public default void _setChargePower(Integer value) {
		System.out.println("setChargePower called... setting value: " + value);
		this.getSetChargePowerChannel().setNextValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE} Channel.
	 *
	 * @param value
	 *            the next value
	 * @throws OpenemsNamedException
	 *             throws named exception
	 */
	public default void _setMaxChargePower(Integer value)
			throws OpenemsNamedException {
		this.getSetMaxChargePowerChannel().setNextWriteValue(value);
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE} Channel.
	 *
	 * @param value
	 *            the next value for max. Discharge Power
	 * @throws OpenemsNamedException
	 *             throws named exception
	 */
	public default void _setMaxDischargePower(Integer value)
			throws OpenemsNamedException {
		this.getSetMaxDischargePowerChannel().setNextWriteValue(value);
	}

	/**
	 * Is the Energy Storage System On-Grid? See
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxChargePower() {
		return this.getMaxChargePowerChannel().value();
	}

	// #############
	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxChargePowerChannel() {
		return this.channel(ChannelId.MAX_CHARGE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetChargePowerChannel() {
		return this.channel(ChannelId.CHARGE_POWER);
	}

	// ###########################
	/**
	 * Is the Energy Storage System On-Grid? See
	 * {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getMaxDischargePower() {
		return this.getMaxDischargePowerChannel().value();
	}

	// #############
	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getMaxDischargePowerChannel() {
		return this.channel(ChannelId.MAX_DISCHARGE_POWER);
	}

	/**
	 * Gets the Active Power in [W]. Negative values for Charge; positive for
	 * Discharge. See {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getChargePowerWanted() {
		return this.getChargePowerWantedChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#ACTIVE_POWER}.
	 *
	 * @return the Channel
	 */
	public default IntegerReadChannel getChargePowerWantedChannel() {
		return this.channel(ChannelId.CHARGE_POWER_WANTED);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetMaxChargePowerChannel() {
		return this.channel(ChannelId.SET_MAX_CHARGE_POWER);
	}

	/**
	 * Gets the Channel for {@link ChannelId#REMOTE_CONTROL_COMMAND_MODE}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetMaxDischargePowerChannel() {
		return this.channel(ChannelId.SET_MAX_DISCHARGE_POWER);
	}

}
