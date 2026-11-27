package io.openems.edge.aircooling.blueeplus;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.common.types.OpenemsType;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface BlueEPlus extends OpenemsComponent, ModbusComponent {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {

		INTERNAL_TEMPERATURE_SET_PT_HIGH_FAULT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		INTERNAL_TEMPERATURE_SET_PT_HIGH_WARNING(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		INTERNAL_TEMPERATURE_SET_PT_LOW_WARNING(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		INTERNAL_TEMPERATURE_SET_PT_LOW_FAULT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		INTERNAL_TEMPERATURE_HYSTERESIS(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		INTERNAL_TEMPERATURE_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		AMBIENT_TEMPERATURE_SET_PT_HIGH_ALARM(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		AMBIENT_TEMPERATURE_SET_PT_HIGH_WARNING(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		AMBIENT_TEMPERATURE_SET_PT_LOW_WARNING(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		AMBIENT_TEMPERATURE_SET_PT_LOW_ALARM(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		AMBIENT_TEMPERATURE_HYSTERESIS(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		AMBIENT_TEMPERATURE_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		EXTERNAL_TEMPERATURE_SENSOR_SET_PT_HIGH_ALARM(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		EXTERNAL_TEMPERATURE_SENSOR_SET_PT_HIGH_WARNING(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		EXTERNAL_TEMPERATURE_SENSOR_SET_PT_LOW_WARNING(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		EXTERNAL_TEMPERATURE_SENSOR_SET_PT_LOW_ALARM(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		EXTERNAL_TEMPERATURE_SENSOR_HYSTERESIS(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		EXTERNAL_TEMPERATURE_SENSOR_CATEGORY(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		MONITORING_COOLING_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		MONITORING_INTERNAL_AIR_CIRCUIT_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		MONITORING_EXTERNAL_AIR_CIRCUIT_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		MONITORING_INTERNAL_FAN_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		MONITORING_EXTERNAL_FAN_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		MONITORING_COMPRESSOR_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		MONITORING_EEV_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		MONITORING_FILTER_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		MONITORING_DOOR_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		MONITORING_ELECTRONICS_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		MONITORING_CONDENSATE_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		MONITORING_SYSTEM_MESSAGES_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		SETUP_MODE(Doc.of(OpenemsType.INTEGER)), //
		SETUP_ALARM_TOLERANCE_FILTER(Doc.of(OpenemsType.INTEGER)), //
		SETUP_STANDARD_CONTROL_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.accessMode(AccessMode.READ_WRITE)), //
		SETUP_STANDARD_CONTROL_ALARM_THRESHOLD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		SETUP_OUTLET_TEMPERATURE_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		SETUP_OUTLET_TEMPERATURE_ALARM_THRESHOLD(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		REMOTE_DOOR_COMMAND(Doc.of(OpenemsType.INTEGER)), //
		REMOTE_DOOR_TIMEOUT(Doc.of(OpenemsType.INTEGER)), //
		REMOTE_DOOR_TIMEOUT_REQUIRED(Doc.of(OpenemsType.INTEGER)), //
		REMOTE_DOOR_CATEGORY(Doc.of(OpenemsType.INTEGER)), //
		REMOTE_TEMPERATURE_VALUE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		REMOTE_TEMPERATURE_TIMEOUT(Doc.of(OpenemsType.INTEGER)), //
		REMOTE_TEMPERATURE_TIMEOUT_REQUIRED(Doc.of(OpenemsType.INTEGER)), //
		REMOTE_TEMPERATURE_CATEGORY(Doc.of(OpenemsType.INTEGER)), //

		INTERNAL_TEMPERATURE_STATUS(Doc.of(StatusValues.values())), //
		AMBIENT_TEMPERATURE_STATUS(Doc.of(StatusValues.values())), //
		EXTERNAL_TEMPERATURE_SENSOR_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_COOLING_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_INTERNAL_AIR_CIRCUIT_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_EXTERNAL_AIR_CIRCUIT_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_INTERNAL_FAN_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_EXTERNAL_FAN_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_COMPRESSOR_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_EEV_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_FILTER_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_DOOR_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_ELECTRONICS_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_CONDENSATE_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_SYSTEM_MESSAGES_STATUS(Doc.of(StatusValues.values())), //
		MONITORING_ERROR_LIST_STATUS(Doc.of(StatusValues.values())), //
		REMOTE_DOOR_STATUS(Doc.of(StatusValues.values())), //
		REMOTE_TEMPERATURE_STATUS(Doc.of(StatusValues.values())), //

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
	 * Gets the Channel for {@link ChannelId#SETUP_STANDARD_CONTROL_SETPOINT}.
	 *
	 * @return the Channel
	 */
	public default IntegerWriteChannel getSetupStandardControlSetpointChannel() {
		return this.channel(ChannelId.SETUP_STANDARD_CONTROL_SETPOINT);
	}

	/**
	 * Gets the setupStandardControlSetpoint status, see
	 * {@link ChannelId#SETUP_STANDARD_CONTROL_SETPOINT}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Integer> getSetupStandardControlSetpoint() {
		return this.getSetupStandardControlSetpointChannel().value();
	}

	/**
	 * Sets the CoolingCircuit1SetPointMax, see
	 * {@link ChannelId#SETUP_STANDARD_CONTROL_SETPOINT}.
	 * 
	 * @param value the int value
	 * @throws OpenemsNamedException on error
	 */
	public default void setSetupStandardControlSetpoint(int value) throws OpenemsNamedException {
		this.getSetupStandardControlSetpointChannel().setNextWriteValue(value);
	}

}
