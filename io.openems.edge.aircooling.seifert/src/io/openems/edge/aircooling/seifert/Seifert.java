package io.openems.edge.aircooling.seifert;

import io.openems.common.channel.AccessMode;
import io.openems.common.channel.Level;
import io.openems.common.channel.Unit;
import io.openems.common.types.OpenemsType;
import io.openems.edge.aircooling.seifert.enums.ModeState;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.IntegerWriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;

public interface Seifert extends OpenemsComponent {

	enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		ALARM_POT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)//
				.text("Alarm High Setpoint Potentiometer Reading")), //
		COMPRESSOR_OR_ICE_ALGORITHM_FAULT(Doc.of(Level.FAULT)//
				.text("Alarm Compressor or Ice Algorithm active")), //
		AMBIENT_SENSOR_FAULT(Doc.of(Level.FAULT)//
				.text("Faulty Ambient Sensor")), //
		AMBIENT_BLOWER_FAULT(Doc.of(Level.FAULT)//
				.text("Alarm Ambient Blower")), //
		AUX_OR_PRESSURE_PIPE_SENSOR_FAULT(Doc.of(Level.FAULT)//
				.text("Faulty Auxiliary Sensor or Faulty Pressure Pipe Sensor")), //
		CAREL_INVERTER_COMMS_FAULT(Doc.of(Level.FAULT)//
				.text("Carel/Inverter Communication Failure")), //
		DELTA_OUTLET_INLET_TEMP_FAULT(Doc.of(Level.FAULT)//
				.text("Alarm Delta Outlet-Inlet Temperature")), //
		HIGH_INTERNAL_TEMP_FAULT(Doc.of(Level.FAULT)//
				.text("High Internal Temperature")), //
		ICING_SENSOR_FAULT(Doc.of(Level.FAULT)//
				.text("Faulty Icing Sensor on S1 Location or Faulty Icing Sensor")), //
		INTERNAL_BLOWER_FAULT(Doc.of(Level.FAULT)//
				.text("Alarm Internal Blower")), //
		INTERNAL_TEMP_SENSOR_FAULT(Doc.of(Level.FAULT)//
				.text("Faulty Internal Temperature Sensor")), //
		LOW_INTERNAL_TEMP_FAULT(Doc.of(Level.FAULT)//
				.text("Low Internal Temperature")), //
		PHASE_FAIL_OR_DI2_CLOSED_FAULT(Doc.of(Level.FAULT)//
				.text("Phase Failure or Phase Failure / Digital Input 2 closed")), //
		PRESSURE_PIPE_ALGORITHM_FAULT(Doc.of(Level.FAULT)//
				.text("Pressure Pipe Algorithm Alarm")), //
		AMBIENT_BLOWER_WARNING(Doc.of(Level.WARNING)//
				.text("Warning Ambient Blower")), //
		COMPRESSOR_WARNING(Doc.of(Level.WARNING)//
				.text("Warning Compressor")), //
		INTERNAL_BLOWER_WARNING(Doc.of(Level.WARNING)//
				.text("Warning Internal Blower")), //
		AMBIENT_TEMPERATURE(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		CONTROL_SENSOR(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)), //

		CONTROL_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		DOOR_CONTACT_1_STATUS(Doc.of(OpenemsType.BOOLEAN)), //
		DOOR_CONTACT_2_STATUS(Doc.of(OpenemsType.BOOLEAN)), //
		DOOR_CONTACT_3_STATUS(Doc.of(OpenemsType.BOOLEAN)), //
		HEATER_POT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		HEATER_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		HIGH_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		INTERNAL_AIR_OUTLET_TEMP(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		LOW_TEMP_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		OUTPUT_STATUS_AMBIENT_BLOWER(Doc.of(OpenemsType.BOOLEAN)), //
		OUTPUT_STATUS_COMPRESSOR(Doc.of(OpenemsType.BOOLEAN)), //
		OUTPUT_STATUS_HEATER(Doc.of(OpenemsType.BOOLEAN)), //
		OUTPUT_STATUS_INTERNAL_BLOWER(Doc.of(OpenemsType.BOOLEAN)), //
		MODE_STATE(Doc.of(ModeState.values())), //
		TEMP_POT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)), //
		SET_COOLING_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE)), //
		SET_ENABLE_INPUT_1_INVERT(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)//
				.text("False: Unit is OFF when Door Contact is open, True: Unit is OFF when Door Contact is closed")), //
		SET_ENABLE_TEMP_UNIT(Doc.of(OpenemsType.BOOLEAN)//
				.accessMode(AccessMode.READ_WRITE)), //
		SET_HEATING_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE)), //
		SET_HIGH_TEMP_ALARM_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE)), //
		SET_LOW_TEMP_ALARM_SETPOINT(Doc.of(OpenemsType.INTEGER)//
				.unit(Unit.DEZIDEGREE_CELSIUS)//
				.accessMode(AccessMode.READ_WRITE)), //
		;

		private final Doc doc;

		ChannelId(Doc doc) {
			this.doc = doc;
		}

		@Override
		public Doc doc() {
			return this.doc;
		}
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_COOLING_SETPOINT}.
	 *
	 * @return the Channel
	 */
	default IntegerWriteChannel getSetCoolingSetpointChannel() {
		return this.channel(ChannelId.SET_COOLING_SETPOINT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_HEATING_SETPOINT}.
	 *
	 * @return the Channel
	 */
	default IntegerWriteChannel getSetHeatingSetpointChannel() {
		return this.channel(ChannelId.SET_HEATING_SETPOINT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_HIGH_TEMP_ALARM_SETPOINT}.
	 *
	 * @return the Channel
	 */
	default IntegerWriteChannel getSetHighTempAlarmSetpointChannel() {
		return this.channel(ChannelId.SET_HIGH_TEMP_ALARM_SETPOINT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_LOW_TEMP_ALARM_SETPOINT}.
	 *
	 * @return the Channel
	 */
	default IntegerWriteChannel getSetLowTempAlarmSetpointChannel() {
		return this.channel(ChannelId.SET_LOW_TEMP_ALARM_SETPOINT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#SET_ENABLE_TEMP_UNIT}.
	 *
	 * @return the Channel
	 */
	default BooleanWriteChannel getSetEnableTempUnitChannel() {
		return this.channel(ChannelId.SET_ENABLE_TEMP_UNIT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MODE_STATE}.
	 *
	 * @return the Channel
	 */
	public default Channel<ModeState> getModeStateChannel() {
		return this.channel(ChannelId.MODE_STATE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#MODE_STATE}.
	 *
	 * @return the Channel
	 */
	public default Value<ModeState> getModeState() {
		return this.getModeStateChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#AMBIENT_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getAmbientTemperatureChannel() {
		return this.channel(ChannelId.AMBIENT_TEMPERATURE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#AMBIENT_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default Value<Integer> getAmbientTemperature() {
		return this.getAmbientTemperatureChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_SENSOR}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getControlSensorChannel() {
		return this.channel(ChannelId.CONTROL_SENSOR);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CONTROL_SENSOR}.
	 *
	 * @return the Channel
	 */
	public default Value<Integer> getControlSensor() {
		return this.getControlSensorChannel().value();
	}

}
