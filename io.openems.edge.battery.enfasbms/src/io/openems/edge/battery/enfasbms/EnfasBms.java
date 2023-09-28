package io.openems.edge.battery.enfasbms;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.battery.api.Battery;
import io.openems.edge.battery.enfasbms.enums.BalancingState;
import io.openems.edge.battery.enfasbms.enums.CommandStateRequest;
import io.openems.edge.battery.enfasbms.enums.ContactorState;
import io.openems.edge.battery.enfasbms.enums.GlobalState;
import io.openems.edge.battery.enfasbms.statemachine.StateMachine.State;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.DoubleReadChannel;
import io.openems.edge.common.channel.EnumReadChannel;
import io.openems.edge.common.channel.IntegerDoc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.StateChannel;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;

public interface EnfasBms extends Battery, OpenemsComponent, StartStoppable {

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	public StartStop getStartStopTarget();

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		MAX_ALLOWED_START_TIME_FAULT(Doc.of(Level.FAULT) //
				.text("The maximum start time is passed!") //
				.persistencePriority(PersistencePriority.HIGH)), //

		MAX_ALLOWED_STOP_TIME_FAULT(Doc.of(Level.FAULT) //
				.text("The maximum stop time is passed!") //
				.persistencePriority(PersistencePriority.HIGH)), //

		MODULE_0_AVG_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Average Temperature on Module 0")),
		MODULE_1_AVG_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Average Temperature on Module 1")),
		MODULE_2_AVG_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Average Temperature on Module 2")),
		MODULE_3_AVG_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Average Temperature on Module 3")),
		MODULE_4_AVG_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Average Temperature on Module 4")),
		MODULE_5_AVG_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Average Temperature on Module 5")),
		MODULE_6_AVG_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Average Temperature on Module 6")),
		MODULE_7_AVG_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Average Temperature on Module 7")),
		MODULE_8_AVG_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Average Temperature on Module 8")),
		MODULE_0_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Minimum Temperature on Module 0")),
		MODULE_1_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Minimum Temperature on Module 1")),
		MODULE_2_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Minimum Temperature on Module 2")),
		MODULE_3_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Minimum Temperature on Module 3")),
		MODULE_4_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Minimum Temperature on Module 4")),
		MODULE_5_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Minimum Temperature on Module 5")),
		MODULE_6_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Minimum Temperature on Module 6")),
		MODULE_7_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Minimum Temperature on Module 7")),
		MODULE_8_MIN_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Minimum Temperature on Module 8")),
		MODULE_0_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Maximum Temperature on Module 0")),
		MODULE_1_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Maximum Temperature on Module 1")),
		MODULE_2_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Maximum Temperature on Module 2")),
		MODULE_3_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Maximum Temperature on Module 3")),
		MODULE_4_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Maximum Temperature on Module 4")),
		MODULE_5_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Maximum Temperature on Module 5")),
		MODULE_6_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Maximum Temperature on Module 6")),
		MODULE_7_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Maximum Temperature on Module 7")),
		MODULE_8_MAX_CELL_VOLTAGE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIVOLT) //
				.text("Maximum Temperature on Module 8")),

		MODULE_0_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Average Temperature on Module 0")),
		MODULE_1_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Average Temperature on Module 1")),
		MODULE_2_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Average Temperature on Module 2")),
		MODULE_3_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Average Temperature on Module 3")),
		MODULE_4_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Average Temperature on Module 4")),
		MODULE_5_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Average Temperature on Module 5")),
		MODULE_6_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Average Temperature on Module 6")),
		MODULE_7_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Average Temperature on Module 7")),
		MODULE_8_AVG_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Average Temperature on Module 8")),
		MODULE_0_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Minimum Temperature on Module 0")),
		MODULE_1_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Minimum Temperature on Module 1")),
		MODULE_2_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Minimum Temperature on Module 2")),
		MODULE_3_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Minimum Temperature on Module 3")),
		MODULE_4_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Minimum Temperature on Module 4")),
		MODULE_5_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Minimum Temperature on Module 5")),
		MODULE_6_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Minimum Temperature on Module 6")),
		MODULE_7_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Minimum Temperature on Module 7")),
		MODULE_8_MIN_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Minimum Temperature on Module 8")),
		MODULE_0_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Maximum Temperature on Module 0")),
		MODULE_1_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Maximum Temperature on Module 1")),
		MODULE_2_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Maximum Temperature on Module 2")),
		MODULE_3_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Maximum Temperature on Module 3")),
		MODULE_4_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Maximum Temperature on Module 4")),
		MODULE_5_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Maximum Temperature on Module 5")),
		MODULE_6_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Maximum Temperature on Module 6")),
		MODULE_7_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Maximum Temperature on Module 7")),
		MODULE_8_MAX_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Maximum Temperature on Module 8")),

		MODULE_0_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("State of health on Module 0")),
		MODULE_1_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("State of health on Module 1")),
		MODULE_2_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("State of health on Module 2")),
		MODULE_3_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("State of health on Module 3")),
		MODULE_4_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("State of health on Module 4")),
		MODULE_5_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("State of health on Module 5")),
		MODULE_6_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("State of health on Module 6")),
		MODULE_7_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("State of health on Module 7")),
		MODULE_8_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("State of health on Module 8")),

		MODULE_0_STATE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("State Energy on Module 0")),
		MODULE_1_STATE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("State Energy on Module 1")),
		MODULE_2_STATE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("State Energy on Module 2")),
		MODULE_3_STATE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("State Energy on Module 3")),
		MODULE_4_STATE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("State Energy on Module 4")),
		MODULE_5_STATE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("State Energy on Module 5")),
		MODULE_6_STATE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("State Energy on Module 6")),
		MODULE_7_STATE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("State Energy on Module 7")),
		MODULE_8_STATE_ENERGY(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("State Energy on Module 8")),

		SYSTEM_GLOBALSTATE(Doc.of(GlobalState.values())),

		SYSTEM_EMERGENCY_SHUTDHOWN(Doc.of(OpenemsType.INTEGER)),

		SYSTEM_RESERVED(Doc.of(OpenemsType.INTEGER)),

		SYSTEM_CONTACTOR_STATE(Doc.of(ContactorState.values())),

		SYSTEM_BALANCING_STATE(Doc.of(BalancingState.values())),

		SYSTEM_SAFETY_EVENTS(Doc.of(OpenemsType.INTEGER) //
				.text("Number of Safety events active")),

		SYSTEM_WARNING_EVENTS(Doc.of(OpenemsType.INTEGER) //
				.text("Number of warning events active")),

		PACK_CURRENT(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.MILLIAMPERE) //
				.text("Total pack current")),

		PACK_SHUNT_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Shunt temperature")),

		PACK_PRECHARGE_TEMPERATURE(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.DEGREE_CELSIUS) //
				.text("Pre circuit temperature")),

		PACK_LIMIT_MAX_BATTERY_VOLTAGE(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.VOLT) //
				.text("Maximum battery voltage allowed")),

		PACK_LIMIT_MIN_BATTERY_VOLTAGE(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.VOLT) //
				.text("Minimum battery voltage allowed")),

		PACK_LIMIT_MAX_CHARGE_CURRENT(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.AMPERE) //
				.text("Maximum charge current allowed")),

		PACK_LIMIT_MAX_DISCHARGE_CURRENT(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.AMPERE) //
				.text("Maximum discharge current allowed")),

		PACK_LIMIT_MAX_CHARGE_POWER(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.KILOWATT) //
				.text("Maximum charge power allowed")),

		PACK_LIMIT_MAX_DISCHARGE_POWER(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.KILOWATT) //
				.text("Maximum discharge power allowed")),

		PACK_STATE_SOC(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("State of charge")),

		PACK_STATE_SOH(Doc.of(OpenemsType.INTEGER) //
				.unit(Unit.PERCENT) //
				.text("State of health")),

		PACK_STATE_ENERGY(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.KILOWATT_HOURS) //
				.text("Remaining energy")),

		PACK_VOLTAGE(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.VOLT) //
				.text("Total voltage of the pack")),

		PACK_HIGH_VOLTAGE_PLUS(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.VOLT) //
				.text("Plus side voltage after contactor is closed")),

		PACK_HIGH_VOLTAGE_MINUS(Doc.of(OpenemsType.DOUBLE) //
				.unit(Unit.VOLT) //
				.text("Minus side voltage after contactor is closed")),
		STRING_EVENT_CELL_OT_CUTOFF(Doc.of(Level.INFO)), //
		STRING_EVENT_CELL_OT_MAX(Doc.of(Level.INFO)), //
		STRING_EVENT_CELL_OT_WARN(Doc.of(Level.INFO)), //
		STRING_EVENT_CELL_OV_CUTOFF(Doc.of(Level.INFO)), //
		STRING_EVENT_CELL_OV_MAX(Doc.of(Level.INFO)), //
		STRING_EVENT_CELL_OV_WARN(Doc.of(Level.INFO)), //
		STRING_EVENT_CELL_UT_CUTOFF(Doc.of(Level.INFO)), //
		STRING_EVENT_CELL_UT_MAX(Doc.of(Level.INFO)), //
		STRING_EVENT_CELL_UT_WARN(Doc.of(Level.INFO)), //
		STRING_EVENT_CELL_UV_CUTOFF(Doc.of(Level.INFO)), //
		STRING_EVENT_CELL_UV_MAX(Doc.of(Level.INFO)), //
		STRING_EVENT_CELL_UV_WARN(Doc.of(Level.INFO)), //
		STRING_EVENT_COMMUNICATION_ERR(Doc.of(Level.INFO)), //
		STRING_EVENT_CONT_NEGATIVE_ERR(Doc.of(Level.INFO)), //
		STRING_EVENT_CONT_POSITIVE_ERR(Doc.of(Level.INFO)), //
		STRING_EVENT_CURRENT_OC_CHARGE(Doc.of(Level.INFO)), //
		STRING_EVENT_CURRENT_OC_DISCHARGE(Doc.of(Level.INFO)), //
		STRING_EVENT_CURRENT_MEASURE_ERROR(Doc.of(Level.INFO)), //
		STRING_EVENT_INSULATION_ERROR(Doc.of(Level.INFO)), //
		STRING_EVENT_INTERNAL_ERROR(Doc.of(Level.INFO)), //
		STRING_EVENT_SOC_ERROR(Doc.of(Level.INFO)), //
		STRING_EVENT_SOH_ERROR(Doc.of(Level.INFO)), //
		STRING_EVENT_STRING_CONNECTED(Doc.of(Level.INFO)), //
		STRING_EVENT_TEMP_DIFFERENCE_ERROR(Doc.of(Level.INFO)), //
		STRING_MODULE_COUNT(Doc.of(OpenemsType.INTEGER)), //
		STRING_EVENT_BALANCING_ERROR(Doc.of(Level.INFO)), //
		STRING_EVENT_SOC_HIGH_ERROR(Doc.of(Level.INFO)),

		CSC_INIT_BROKEN_AT(Doc.of(OpenemsType.INTEGER)),

		SOFTWARE_VERSION_MAJOR(Doc.of(OpenemsType.STRING) // //
				.unit(Unit.NONE) //
				.text("SW Version Major Value (Vx.0.0)")),
		SOFTWARE_VERSION_MINOR(Doc.of(OpenemsType.STRING) // //
				.unit(Unit.NONE) //
				.text("SW Version Minor Value (Vx.0.0)")),
		SOFTWARE_VERSION_PATH(Doc.of(OpenemsType.STRING) // //
				.unit(Unit.NONE) //
				.text("SW Version Path Value (V0.0.x)")),

		// write registers
		COMMAND_STATE_REQUEST(new IntegerDoc() //
				.accessMode(AccessMode.WRITE_ONLY)),
		COMMAND_ACTIVATE_BALANCING(new IntegerDoc() //
				.accessMode(AccessMode.WRITE_ONLY)),
		COMMAND_BALANCING_THRESHOLD(new IntegerDoc() //
				.accessMode(AccessMode.WRITE_ONLY)),
		COMMAND_CUSTOM_CURRENT_CHARGE_LIMIT(new IntegerDoc() //
				.accessMode(AccessMode.WRITE_ONLY)),
		COMMAND_CUSTOM_CURRENT_DISCHARGE_LIMIT(new IntegerDoc() //
				.accessMode(AccessMode.WRITE_ONLY)),
		COMMAND_RESET_1(new IntegerDoc() //
				.accessMode(AccessMode.WRITE_ONLY)),
		COMMAND_RESET_2(new IntegerDoc() //
				.accessMode(AccessMode.WRITE_ONLY)),
		COMMAND_ALIVE_COUNTER(new IntegerDoc() //
				.accessMode(AccessMode.WRITE_ONLY)),

		STATE_MACHINE(Doc.of(State.values()) //
				.text("Current State of State-Machine")),
		RUN_FAILED(Doc.of(Level.FAULT) //
				.text("Running the Logic failed"));

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
	 * Gets the Channel for {@link ChannelId#MAX_START_TIME}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxStartTimeChannel() {
		return this.channel(ChannelId.MAX_ALLOWED_START_TIME_FAULT);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_START_TIME}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStartTime() {
		return this.getMaxStartTimeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_START_TIME}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxStartTime(Boolean value) {
		this.getMaxStartTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MAX_STOP_TIME}.
	 * 
	 * @return the Channel
	 */
	public default StateChannel getMaxStopTimeChannel() {
		return this.channel(ChannelId.MAX_ALLOWED_STOP_TIME_FAULT);
	}

	/**
	 * Gets the {@link StateChannel} for {@link ChannelId#MAX_STOP_TIME}.
	 * 
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getMaxStopTime() {
		return this.getMaxStopTimeChannel().value();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#MAX_STOP_TIME}
	 * Channel.
	 * 
	 * @param value the next value
	 */
	public default void _setMaxStopTime(Boolean value) {
		this.getMaxStopTimeChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SYSTEM_CONTACTOR_STATE}.
	 * 
	 * @return the Channel
	 */
	public default EnumReadChannel getSystemContactorStateChannel() {
		return this.channel(ChannelId.SYSTEM_CONTACTOR_STATE);
	}

	/**
	 * Gets the ContactorState, see {@link ChannelId#SYSTEM_CONTACTOR_STATE}.
	 * 
	 * @return the Channel {@link value}.
	 */
	public default ContactorState getSystemContactorState() {
		return this.getSystemContactorStateChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#SYSTEM_GLOBALSTATE}.
	 * 
	 * @return the Channel
	 */
	public default EnumReadChannel getSystemGlobalStateChannelChannel() {
		return this.channel(ChannelId.SYSTEM_GLOBALSTATE);
	}

	/**
	 * Gets the ContactorState, see {@link ChannelId#SYSTEM_GLOBALSTATE}.
	 * 
	 * @return the Channel {@link value}.
	 */
	public default GlobalState getSystemGlobalState() {
		return this.getSystemGlobalStateChannelChannel().value().asEnum();
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMMAND_STATE_REQUEST}.
	 *
	 * @return the Channel
	 */
	public default WriteChannel<CommandStateRequest> getCommandStateRequestChannel() {
		return this.channel(ChannelId.COMMAND_STATE_REQUEST);
	}

	/**
	 * Gets the CommandStateRequest, see {@link ChannelId#COMMAND_STATE_REQUEST}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default CommandStateRequest getCommandStateRequest() {
		return this.getCommandStateRequestChannel().value().asEnum();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMMAND_STATE_REQUEST} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCommandStateRequest(CommandStateRequest value) {
		this.getCommandStateRequestChannel().setNextValue(value);
	}

	/**
	 * Writes the value to the {@link ChannelId#COMMAND_STATE_REQUEST} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCommandStateRequest(CommandStateRequest value) throws OpenemsNamedException {
		this.getCommandStateRequestChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMMAND_ACTIVATE_BALANCING}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getCommandActivateBalancingChannel() {
		return this.channel(ChannelId.COMMAND_ACTIVATE_BALANCING);
	}

	/**
	 * Gets the CommandActivateBalancing, see
	 * {@link ChannelId#COMMAND_ACTIVATE_BALANCING}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default boolean getCommandActivateBalancing() {
		return this.getCommandActivateBalancingChannel().value().asOptional().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMMAND_ACTIVATE_BALANCING} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCommandActivateBalancing(boolean value) {
		this.getCommandActivateBalancingChannel().setNextValue(value);
	}

	/**
	 * Writes the value to the {@link ChannelId#COMMAND_ACTIVATE_BALANCING}
	 * Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCommandActivateBalancing(boolean value) throws OpenemsNamedException {
		this.getCommandActivateBalancingChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMMAND_BALANCING_THRESHOLD}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getCommandBalancingThresholdChannel() {
		return this.channel(ChannelId.COMMAND_BALANCING_THRESHOLD);
	}

	/**
	 * Gets the CommandBalancingThreshold, see
	 * {@link ChannelId#COMMAND_BALANCING_THRESHOLD}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default int getCommandBalancingThreshold() {
		return this.getCommandBalancingThresholdChannel().value().asOptional().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMMAND_BALANCING_THRESHOLD} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCommandBalancingThreshold(int value) {
		this.getCommandBalancingThresholdChannel().setNextValue(value);
	}

	/**
	 * Writes the value to the {@link ChannelId#COMMAND_BALANCING_THRESHOLD}
	 * Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCommandBalancingThreshold(int value) throws OpenemsNamedException {
		this.getCommandBalancingThresholdChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMMAND_CUSTOM_CURRENT_CHARGE_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getCommandCustomCurrentChargeLimitChannel() {
		return this.channel(ChannelId.COMMAND_CUSTOM_CURRENT_CHARGE_LIMIT);
	}

	/**
	 * Gets the PreChargeControl, see
	 * {@link ChannelId#COMMAND_CUSTOM_CURRENT_CHARGE_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default int getCommandCustomCurrentChargeLimit() {
		return this.getCommandCustomCurrentChargeLimitChannel().value().asOptional().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMMAND_CUSTOM_CURRENT_CHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCommandCustomCurrentChargeLimit(int value) {
		this.getCommandCustomCurrentChargeLimitChannel().setNextValue(value);
	}

	/**
	 * Writes the value to the {@link ChannelId#COMMAND_CUSTOM_CURRENT_CHARGE_LIMIT}
	 * Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCommandCustomCurrentChargeLimit(int value) throws OpenemsNamedException {
		this.getCommandCustomCurrentChargeLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for
	 * {@link ChannelId#COMMAND_CUSTOM_CURRENT_DISCHARGE_LIMIT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getCommandCustomCurrentDischargeLimitChannel() {
		return this.channel(ChannelId.COMMAND_CUSTOM_CURRENT_DISCHARGE_LIMIT);
	}

	/**
	 * Gets the PreChargeControl, see
	 * {@link ChannelId#COMMAND_CUSTOM_CURRENT_DISCHARGE_LIMIT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default int getCommandCustomCurrentDischargeLimit() {
		return this.getCommandCustomCurrentDischargeLimitChannel().value().asOptional().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMMAND_CUSTOM_CURRENT_DISCHARGE_LIMIT} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCommandCustomCurrentDischargeLimit(int value) {
		this.getCommandCustomCurrentDischargeLimitChannel().setNextValue(value);
	}

	/**
	 * Writes the value to the
	 * {@link ChannelId#COMMAND_CUSTOM_CURRENT_DISCHARGE_LIMIT} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCommandCustomCurrentDischargeLimit(int value) throws OpenemsNamedException {
		this.getCommandCustomCurrentDischargeLimitChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMMAND_RESET_1}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getCommandReset1Channel() {
		return this.channel(ChannelId.COMMAND_RESET_1);
	}

	/**
	 * Gets the PreChargeControl, see {@link ChannelId#COMMAND_RESET_1}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default int getCommandReset1() {
		return this.getCommandReset1Channel().value().asOptional().get();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#COMMAND_RESET_1}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCommandReset1(int value) {
		this.getCommandReset1Channel().setNextValue(value);
	}

	/**
	 * Writes the value to the {@link ChannelId#COMMAND_RESET_1} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCommandReset1(int value) throws OpenemsNamedException {
		this.getCommandReset1Channel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMMAND_RESET_2}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getCommandReset2Channel() {
		return this.channel(ChannelId.COMMAND_RESET_2);
	}

	/**
	 * Gets the PreChargeControl, see {@link ChannelId#COMMAND_RESET_2}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default int getCommandReset2() {
		return this.getCommandReset2Channel().value().asOptional().get();
	}

	/**
	 * Internal method to set the 'nextValue' on {@link ChannelId#COMMAND_RESET_2}
	 * Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCommandReset2(int value) {
		this.getCommandReset2Channel().setNextValue(value);
	}

	/**
	 * Writes the value to the {@link ChannelId#COMMAND_RESET_2} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCommandReset2(int value) throws OpenemsNamedException {
		this.getCommandReset2Channel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMMAND_ALIVE_COUNTER}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getCommandAliveCounterChannel() {
		return this.channel(ChannelId.COMMAND_ALIVE_COUNTER);
	}

	/**
	 * Gets the PreChargeControl, see {@link ChannelId#COMMAND_ALIVE_COUNTER}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default int getCommandAliveCounter() {
		return this.getCommandAliveCounterChannel().value().asOptional().get();
	}

	/**
	 * Internal method to set the 'nextValue' on
	 * {@link ChannelId#COMMAND_ALIVE_COUNTER} Channel.
	 *
	 * @param value the next value
	 */
	public default void _setCommandAliveCounter(int value) {
		this.getCommandAliveCounterChannel().setNextValue(value);
	}

	/**
	 * Writes the value to the {@link ChannelId#COMMAND_ALIVE_COUNTER} Register.
	 *
	 * @param value the next value
	 * @throws OpenemsNamedException on error
	 */
	public default void setCommandAliveCounter(int value) throws OpenemsNamedException {
		this.getCommandAliveCounterChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#PACK_VOLTAGE}.
	 *
	 * @return the Channel
	 */
	public default DoubleReadChannel getPackVoltageChannel() {
		return this.channel(ChannelId.PACK_VOLTAGE);
	}

	/**
	 * Gets the Pack voltage. See {@link ChannelId#PACK_VOLTAGE}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Double> getPackVoltage() {
		return this.getPackVoltageChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#PACK_HIGH_VOLTAGE_PLUS}.
	 *
	 * @return the Channel
	 */
	public default DoubleReadChannel getPackHighVoltagePlusChannel() {
		return this.channel(ChannelId.PACK_HIGH_VOLTAGE_PLUS);
	}

	/**
	 * Gets the Pack voltage. See {@link ChannelId#PACK_HIGH_VOLTAGE_PLUS}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Double> getPackHighVoltagePlus() {
		return this.getPackHighVoltagePlusChannel().value();
	}
}
