package io.openems.edge.battery.bmw;

import org.osgi.service.event.EventHandler;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.bmw.enums.BmsState;
import io.openems.edge.battery.bmw.enums.InfoBits;
import io.openems.edge.battery.bmw.statemachine.StateMachine.State;
import io.openems.edge.common.channel.BooleanDoc;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface BmwBattery extends StartStoppable, OpenemsComponent, EventHandler {

	/**
	 * Gets the Channel for {@link ChannelId#BMS_STATE}.
	 * 
	 * @return the Channel
	 */
	public default EnumReadChannel getBmsStateChannel() {
		return this.channel(ChannelId.BMS_STATE);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#BMS_STATE}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default BmsState getBmsState() {
		return this.getBmsStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_START_ATTEMPTS}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxStartAttemptsChannel() {
		return this.channel(ChannelId.MAX_START_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_START_ATTEMPTS}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartAttempts() {
		return this.getMaxStartAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#MAX_START_ATTEMPTS} Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxStartAttempts(Boolean value) {
		this.getMaxStartAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxStopAttemptsChannel() {
		return this.channel(ChannelId.MAX_STOP_ATTEMPTS);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_STOP_ATTEMPTS}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopAttempts() {
		return this.getMaxStopAttemptsChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_STOP_ATTEMPTS}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxStopAttempts(Boolean value) {
		this.getMaxStopAttemptsChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#BMS_STATE_COMMAND}.
	 * 
	 * @return the Channel
	 */
	public default IntegerWriteChannel getBmsStateCommand() {
		return this.channel(ChannelId.BMS_STATE_COMMAND);
	}

	/**
	 * Writes the value to the Register.
	 * 
	 * 
	 * @param value to close contactors or not
	 * @throws OpenemsNamedException on error
	 */
	public default void _setBmwStartStop(Integer value) {
		this.getBmsStateCommand().setNextValue(value);
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 * 
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	public static enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		HEART_BEAT_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		HEART_BEAT(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.HEART_BEAT_DEBUG)) //
		),

		BMS_STATE_COMMAND(Doc.of(OpenemsType.INTEGER).accessMode(AccessMode.READ_WRITE)), //

		BMS_STATE_COMMAND_RESET_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_RESET(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.BMS_STATE_COMMAND_RESET_DEBUG)) //
		),

		BMS_STATE_COMMAND_CLEAR_ERROR_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_CLEAR_ERROR(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.BMS_STATE_COMMAND_CLEAR_ERROR_DEBUG)) //
		),

		BMS_STATE_COMMAND_CLOSE_PRECHARGE_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_CLOSE_PRECHARGE(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.BMS_STATE_COMMAND_CLOSE_PRECHARGE_DEBUG)) //
		),

		BMS_STATE_COMMAND_ERROR_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_ERROR(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.BMS_STATE_COMMAND_ERROR_DEBUG)) //
		),

		BMS_STATE_COMMAND_CLOSE_CONTACTOR_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_CLOSE_CONTACTOR(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.BMS_STATE_COMMAND_CLOSE_CONTACTOR_DEBUG)) //
		),

		BMS_STATE_COMMAND_WAKE_UP_FROM_STOP_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_WAKE_UP_FROM_STOP(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
						ChannelId.BMS_STATE_COMMAND_WAKE_UP_FROM_STOP_DEBUG)) //
		),

		BMS_STATE_COMMAND_ENABLE_BATTERY_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		BMS_STATE_COMMAND_ENABLE_BATTERY(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.BMS_STATE_COMMAND_ENABLE_BATTERY_DEBUG)) //
		),

		OPERATING_STATE_INVERTER_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		OPERATING_STATE_INVERTER(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.OPERATING_STATE_INVERTER_DEBUG)) //
		),

		DC_LINK_VOLTAGE_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		DC_LINK_VOLTAGE(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.unit(Unit.VOLT).onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.DC_LINK_VOLTAGE_DEBUG)) //
		),

		DC_LINK_CURRENT_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		DC_LINK_CURRENT(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.DC_LINK_VOLTAGE_DEBUG)) //
		),

		OPERATION_MODE_REQUEST_GRANTED_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		OPERATION_MODE_REQUEST_GRANTED(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.OPERATION_MODE_REQUEST_GRANTED_DEBUG)) //
		),

		OPERATION_MODE_REQUEST_CANCELED_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		OPERATION_MODE_REQUEST_CANCELED(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.OPERATION_MODE_REQUEST_CANCELED_DEBUG)) //
		),

		CONNECTION_STRATEGY_HIGH_SOC_FIRST_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		CONNECTION_STRATEGY_HIGH_SOC_FIRST(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(
						ChannelId.CONNECTION_STRATEGY_HIGH_SOC_FIRST_DEBUG)) //
		),

		CONNECTION_STRATEGY_LOW_SOC_FIRST_DEBUG(Doc.of(OpenemsType.BOOLEAN)), //
		CONNECTION_STRATEGY_LOW_SOC_FIRST(new BooleanDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new BooleanWriteChannel.MirrorToDebugChannel(ChannelId.CONNECTION_STRATEGY_LOW_SOC_FIRST_DEBUG)) //
		),

		SYSTEM_TIME_DEBUG(Doc.of(OpenemsType.INTEGER)), //
		SYSTEM_TIME(new IntegerDoc() //
				.accessMode(AccessMode.READ_WRITE) //
				.onInit(new IntegerWriteChannel.MirrorToDebugChannel(ChannelId.SYSTEM_TIME_DEBUG)) //
		),

		// Read only channels
		LIFE_SIGN(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		BMS_STATE(Doc.of(BmsState.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		ERROR_BITS_1_UNSPECIFIED_ERROR(Doc.of(Level.FAULT) //
				.text("Unspecified Error")), //

		ERROR_BITS_1_LOW_VOLTAGE_ERROR(Doc.of(Level.FAULT) //
				.text("Low Voltage Error")), //

		ERROR_BITS_1_HIGH_VOLTAGE_ERROR(Doc.of(Level.FAULT) //
				.text("High Voltage Error")), //

		ERROR_BITS_1_CHARGE_CURRENT_ERROR(Doc.of(Level.FAULT) //
				.text("Charge Current Error")), //

		ERROR_BITS_1_DISCHARGE_CURRENT_ERROR(Doc.of(Level.FAULT) //
				.text("Discharge Current Error")), //

		ERROR_BITS_1_CHARGE_POWER_ERROR(Doc.of(Level.FAULT) //
				.text("Charge Power Error")), //

		ERROR_BITS_1_DISCHARGE_POWER_ERROR(Doc.of(Level.FAULT) //
				.text("Discharge Power Error")), //

		ERROR_BITS_1_LOW_SOC_ERROR(Doc.of(Level.FAULT) //
				.text("Low Soc Error")), //

		ERROR_BITS_1_HIGH_SOC_ERROR(Doc.of(Level.FAULT) //
				.text("High Soc Error")), //

		ERROR_BITS_1_LOW_TEMPERATURE_ERROR(Doc.of(Level.FAULT) //
				.text("Low Temperature Error")), //

		ERROR_BITS_1_HIGH_TEMPERATURE_ERROR(Doc.of(Level.FAULT) //
				.text("High Temperature Error")), //

		ERROR_BITS_1_INSULATION_ERROR(Doc.of(Level.FAULT) //
				.text("Insulation Error")), //

		ERROR_BITS_1_CONTACTOR_FUSE_ERROR(Doc.of(Level.FAULT) //
				.text("Contactor/Fuse Error")), //

		ERROR_BITS_1_SENSOR_ERROR(Doc.of(Level.FAULT) //
				.text("Sensor Error")), //

		ERROR_BITS_1_IMBALANCE_ERROR(Doc.of(Level.FAULT) //
				.text("Imbalance Error")), //

		ERROR_BITS_1_COMMUNICATION_ERROR(Doc.of(Level.FAULT) //
				.text("Communication Error")), //

		ERROR_BITS_2_RACK_STRING_ERROR(Doc.of(Level.FAULT) //
				.text("Rack/ String Error")), //

		ERROR_BITS_2_SOH_ERROR(Doc.of(Level.FAULT) //
				.text("SOH Error")), //

		ERROR_BITS_2_CONTAINER_ERROR(Doc.of(Level.FAULT) //
				.text("Container/ (room) Error")), //

		WARNING_BITS_1_UNSPECIFIED_WARNING(Doc.of(Level.WARNING) //
				.text("Unspecified warning")), //

		WARNING_BITS_1_LOW_VOLTAGE_WARNING(Doc.of(Level.WARNING) //
				.text("Low Voltage warning")), //

		WARNING_BITS_1_HIGH_VOLTAGE_WARNING(Doc.of(Level.WARNING) //
				.text("High Voltage warning")), //

		WARNING_BITS_1_CHARGE_CURRENT_WARNING(Doc.of(Level.WARNING) //
				.text("Charge Current warning")), //

		WARNING_BITS_1_DISCHARGE_CURRENT_WARNING(Doc.of(Level.WARNING) //
				.text("Discharge Current warning")), //

		WARNING_BITS_1_CHARGE_POWER_WARNING(Doc.of(Level.WARNING) //
				.text("Charge Power warning")), //

		WARNING_BITS_1_DISCHARGE_POWER_WARNING(Doc.of(Level.WARNING) //
				.text("Discharge Power warning")), //

		WARNING_BITS_1_LOW_SOC_WARNING(Doc.of(Level.WARNING) //
				.text("Low Soc warning")), //

		WARNING_BITS_1_HIGH_SOC_WARNING(Doc.of(Level.WARNING) //
				.text("High Soc warning")), //

		WARNING_BITS_1_LOW_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.text("Low Temperature warning")), //

		WARNING_BITS_1_HIGH_TEMPERATURE_WARNING(Doc.of(Level.WARNING) //
				.text("High Temperature warning")), //

		WARNING_BITS_1_INSULATION_WARNING(Doc.of(Level.WARNING) //
				.text("Insulation warning")), //

		WARNING_BITS_1_CONTACTOR_FUSE_WARNING(Doc.of(Level.WARNING) //
				.text("Contactor/Fuse warning")), //

		WARNING_BITS_1_SENSOR_WARNING(Doc.of(Level.WARNING) //
				.text("Sensor warning")), //

		WARNING_BITS_1_IMBALANCE_WARNING(Doc.of(Level.WARNING) //
				.text("Imbalance warning")), //

		WARNING_BITS_1_COMMUNICATION_WARNING(Doc.of(Level.WARNING) //
				.text("Communication warning")), //

		WARNING_BITS_2_RACK_STRING_WARNING(Doc.of(Level.WARNING) //
				.text("Rack/ String Error")), //

		WARNING_BITS_2_SOH_WARNING(Doc.of(Level.WARNING) //
				.text("SOH Error")), //

		WARNING_BITS_2_CONTAINER_WARNING(Doc.of(Level.WARNING) //
				.text("Container/ (room) Error")), //

		INFO_BITS(Doc.of(InfoBits.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		MAXIMUM_OPERATING_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE)), //

		MINIMUM_OPERATING_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE)), //

		MAXIMUM_OPERATING_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		MINIMUM_OPERATING_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		MAXIMUM_LIMIT_DYNAMIC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		MINIMUM_LIMIT_DYNAMIC_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		NUMBER_OF_STRINGS_CONNECTED(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		NUMBER_OF_STRINGS_INSTALLED(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		SOC_ALL_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.PERCENT)), //

		SOC_CONNECTED_STRINGS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.PERCENT)), //

		REMAINING_CHARGE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE_HOURS)), //

		REMAINING_DISCHARGE_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE_HOURS)), //

		REMAINING_CHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS)), //

		REMAINING_DISCHARGE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS)), //

		NOMINAL_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS)), //

		TOTAL_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOWATT_HOURS)), //

		NOMINAL_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE_HOURS)), //

		TOTAL_CAPACITY(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.AMPERE_HOURS)), //

		DC_VOLTAGE_CONNECTED_RACKS(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		DC_VOLTAGE_AVERAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.VOLT)), //

		DC_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.MILLIAMPERE)), //

		AVERAGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		MINIMUM_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		MAXIMUM_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		AVERAGE_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.MILLIVOLT)), //

		INTERNAL_RESISTANCE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.MILLIOHM)), //

		INSULATION_RESISTANCE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.KILOOHM)), //

		CONTAINER_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		AMBIENT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		HUMIDITY_CONTAINER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.PERCENT)), //

		MAXIMUM_LIMIT_DYNAMIC_CURRENT_HIGH_RES(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.MILLIAMPERE)), //

		MINIMUM_LIMIT_DYNAMIC_CURRENT_HIGH_RES(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.MILLIAMPERE)), //

		FULL_CYCLE_COUNT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		OPERATING_TIME_COUNT(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY) //
				.unit(Unit.HOUR)), //

		COM_PRO_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		SERIAL_NUMBER(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		SOFTWARE_VERSION(Doc.of(OpenemsType.INTEGER) //
				.accessMode(AccessMode.READ_ONLY)), //

		STATE_MACHINE(Doc.of(State.values()) //
				.accessMode(AccessMode.READ_ONLY)), //

		// OpenEMS Faults
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed")), //
		MAX_START_ATTEMPTS(Doc.of(Level.FAULT) //
				.text("The maximum number of start attempts failed")), //
		MAX_STOP_ATTEMPTS(Doc.of(Level.FAULT) //
				.text("The maximum number of stop attempts failed")), //
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