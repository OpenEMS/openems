package io.openems.edge.fluidcooling.hydac;

import static io.openems.common.channel.AccessMode.READ_WRITE;
import static io.openems.common.channel.Level.FAULT;
import static io.openems.common.channel.Level.INFO;
import static io.openems.common.channel.Level.WARNING;
import static io.openems.common.types.OpenemsType.BOOLEAN;
import static io.openems.common.types.OpenemsType.INTEGER;

import io.openems.common.channel.Level;
import io.openems.common.channel.PersistencePriority;
import io.openems.common.channel.Unit;
import io.openems.common.exceptions.OpenemsError.OpenemsNamedException;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.common.channel.BooleanWriteChannel;
import io.openems.edge.common.channel.Channel;
import io.openems.edge.common.channel.Doc;
import io.openems.edge.common.channel.WriteChannel;
import io.openems.edge.common.channel.value.Value;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.startstop.StartStop;
import io.openems.edge.common.startstop.StartStoppable;
import io.openems.edge.fluidcooling.hydac.statemachine.StateMachine.State;

public interface Hydac extends OpenemsComponent, ModbusComponent, StartStoppable {

	public enum ChannelId implements io.openems.edge.common.channel.ChannelId {
		STATE_MACHINE(Doc.of(State.values())//
				.text("Current State of State-Machine")), //
		RUN_FAILED(Doc.of(Level.FAULT)//
				.text("Running the Logic failed")), //
		TIMEOUT_TARGET_UNDEFINED(Doc.of(FAULT)//
				.translationKey(Hydac.class, "targetUndefined")), //
		COOLING_CIRCUIT_1_FLOW(Doc.of(INTEGER)//
				.unit(Unit.MILLILITER_PER_MINUTE)), //
		COOLING_CIRCUIT_1_CONDUCTANCE(Doc.of(INTEGER)//
				.unit(Unit.MIKROSIEMENS_PER_CENTIMETRE)), //
		GATEWAY_ERROR(Doc.of(WARNING)//
				.translationKey(Hydac.class, "gatewayError")), //
		SYSTEM_ERROR(Doc.of(FAULT)//
				.translationKey(Hydac.class, "systemError")), //
		ALARM_ACTIVE(Doc.of(BOOLEAN)), //
		WARNING_ACTIVE(Doc.of(BOOLEAN)), //
		CHILLER_RUNNING(Doc.of(BOOLEAN)), //
		COOLING_REQUIRED(Doc.of(BOOLEAN)), //
		CHILLER_READY(Doc.of(BOOLEAN)), //
		HEATER(Doc.of(BOOLEAN)), //
		PUMP(Doc.of(BOOLEAN)), //
		COMPRESSOR(Doc.of(BOOLEAN)), //
		DRY_RUN(Doc.of(FAULT)//
				.translationKey(Hydac.class, "dryRun")), //
		OVER_TEMPERATURE_WARNING(Doc.of(INFO)//
				.translationKey(Hydac.class, "overTemperatureWarning")), //
		UNDER_TEMPERATURE_WARNING(Doc.of(INFO)//
				.translationKey(Hydac.class, "underTemperatureWarning")), //
		WATER_LEVEL_WARNING(Doc.of(WARNING)//
				.translationKey(Hydac.class, "waterLevelWarning")), //
		LOW_PRESSURE_ALARM(Doc.of(BOOLEAN)), //
		HIGH_PRESSURE_ALARM(Doc.of(BOOLEAN)), //
		FAN_ERROR(Doc.of(FAULT)//
				.translationKey(Hydac.class, "fanError")), //
		MOTOR_PROTECTION_SWITCH_PUMP(Doc.of(FAULT)//
				.translationKey(Hydac.class, "motorProtectionSwitchPump")), //
		INVERTER_ERROR_COMPRESSOR(Doc.of(BOOLEAN)), //
		MOTOR_PROTECTION_SWITCH_COMPRESSOR(Doc.of(WARNING)//
				.translationKey(Hydac.class, "motorProtectionSwitchCompressor")), //
		SENSOR_ERROR(Doc.of(FAULT)//
				.translationKey(Hydac.class, "sensorError")), //
		COOLING_CIRCUIT_1_SET_POINT_MIN(Doc.of(INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(READ_WRITE)), //
		COOLING_CIRCUIT_1_SET_POINT_MAX(Doc.of(INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(READ_WRITE)), //
		COOLING_CIRCUIT_1_SET_POINT(Doc.of(INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)//
				.accessMode(READ_WRITE)//
				.persistencePriority(PersistencePriority.HIGH)), //
		COOLING_CIRCUIT_1_SET_POINT_OFFSET(Doc.of(INTEGER)//
				.unit(Unit.MILLILITER_PER_MINUTE)//
				.accessMode(READ_WRITE)), //
		COOLING_CIRCUIT_1_FLOW_ALARM_SETPOINT(Doc.of(INTEGER)//
				.unit(Unit.MILLILITER_PER_MINUTE)//
				.accessMode(READ_WRITE)), //
		COOLING_CIRCUIT_1_CONDUCTANCE_SETPOINT(Doc.of(INTEGER)//
				.accessMode(READ_WRITE)), //
		COOLING_CIRCUIT_1_CONTROL_MODE(Doc.of(INTEGER)//
				.accessMode(READ_WRITE)), //
		CHILLER_ON(Doc.of(BOOLEAN)//
				.accessMode(READ_WRITE)), //
		CHILLER_OFF(Doc.of(BOOLEAN)//
				.accessMode(READ_WRITE)), //
		CLEAR_ERROR(Doc.of(BOOLEAN)//
				.accessMode(READ_WRITE)), //

		OUTLET_TEMPERATURE(Doc.of(INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		PUMP_OUTLET_TEMPERATURE(Doc.of(INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //
		AMBIENT_TEMPERATURE(Doc.of(INTEGER)//
				.unit(Unit.DEGREE_CELSIUS)), //

		// Connected errors
		OVER_TEMPERATURE_ALARM(Doc.of(FAULT)//
				.translationKey(Hydac.class, "overTemperatureAlarm")), //
		UNDER_TEMPERATURE_ALARM(Doc.of(FAULT)//
				.translationKey(Hydac.class, "underTemperatureAlarm")), //
		TEMPERATURE_RELATED_FAULT(Doc.of(BOOLEAN)), //
		INVERTER_COMPRESSOR_WARNING(Doc.of(WARNING)//
				.translationKey(Hydac.class, "inverterCompressorWarning")), //
		INVERTER_COMPRESSOR_FAULT(Doc.of(FAULT)//
				.translationKey(Hydac.class, "inverterCompressorFault")), //
		LOW_PRESSURE_WARNING(Doc.of(WARNING)//
				.translationKey(Hydac.class, "lowPressureWarning")), //
		LOW_PRESSURE_FAULT(Doc.of(FAULT)//
				.translationKey(Hydac.class, "lowPressureFault")), //
		HIGH_PRESSURE_WARNING(Doc.of(WARNING)//
				.translationKey(Hydac.class, "highPressureWarning")), //
		HIGH_PRESSURE_FAULT(Doc.of(FAULT)//
				.translationKey(Hydac.class, "highPressureFault")), //
		OVER_TEMPERATURE_WARNING_30_MIN(Doc.of(WARNING)//
				.translationKey(Hydac.class, "overTemperatureWarning30")), //
		OVER_TEMPERATURE_FAULT_60_MIN(Doc.of(FAULT)//
				.translationKey(Hydac.class, "overTemperatureFault60")), //
		UNDER_TEMPERATURE_WARNING_30_MIN(Doc.of(WARNING)//
				.translationKey(Hydac.class, "underTemperatureWarning30")), //

		MANUAL_MODE_ACTIVATED(Doc.of(INFO)//
				.translationKey(Hydac.class, "manualModeActivated")), //

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
	 * Gets the Channel for {@link ChannelId#CHILLER_ON}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getChillerOnChannel() {
		return this.channel(ChannelId.CHILLER_ON);
	}

	/**
	 * Sets the ChillerOn, see {@link ChannelId#CHILLER_OFF}.
	 *
	 * @param value the boolean value
	 * @throws OpenemsNamedException on error
	 */
	public default void setChillerOn(boolean value) throws OpenemsNamedException {
		this.getChillerOnChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#CHILLER_OFF}.
	 *
	 * @return the Channel
	 */
	public default BooleanWriteChannel getChillerOffChannel() {
		return this.channel(ChannelId.CHILLER_OFF);
	}

	/**
	 * Sets the ChillerOff, see {@link ChannelId#CHILLER_OFF}.
	 *
	 * @param value the boolean value
	 * @throws OpenemsNamedException on error
	 */
	public default void setChillerOff(boolean value) throws OpenemsNamedException {
		this.getChillerOffChannel().setNextWriteValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OUTLET_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getOutletTemperatureChannel() {
		return this.channel(ChannelId.OUTLET_TEMPERATURE);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OUTLET_TEMPERATURE}.
	 *
	 * @return the Channel
	 */
	public default Value<Integer> getOutletTemperature() {
		return this.getOutletTemperatureChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#COOLING_REQUIRED}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getCoolingRequiredChannel() {
		return this.channel(ChannelId.COOLING_REQUIRED);
	}

	/**
	 * Gets the CoolingRequired, see {@link ChannelId#COOLING_REQUIRED}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCoolingRequired() {
		return this.getCoolingRequiredChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#PUMP}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getPumpChannel() {
		return this.channel(ChannelId.PUMP);
	}

	/**
	 * Gets the Pump status, see {@link ChannelId#PUMP}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getPump() {
		return this.getPumpChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#COMPRESSOR}.
	 *
	 * @return the Channel
	 */
	public default Channel<Boolean> getCompressorChannel() {
		return this.channel(ChannelId.COMPRESSOR);
	}

	/**
	 * Gets the Compressor status, see {@link ChannelId#COMPRESSOR}.
	 *
	 * @return the Channel {@link Value}
	 */
	public default Value<Boolean> getCompressor() {
		return this.getCompressorChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#CLEAR_ERROR}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getClearErrorChannel() {
		return this.channel(ChannelId.CLEAR_ERROR);
	}

	/**
	 * Gets the Compressor status, see {@link ChannelId#CLEAR_ERROR}.
	 *
	 * @return the Channel {@link Value}
	 */
	default Value<Boolean> getClearError() {
		return this.getClearErrorChannel().value();
	}

	/**
	 * Clears the errors, see {@link ChannelId#CLEAR_ERROR}.
	 *
	 * @param value true/false
	 */
	default void setClearError(boolean value) {
		this.getClearErrorChannel().setNextValue(value);
	}

	/**
	 * Gets the Channel for {@link ChannelId#INVERTER_COMPRESSOR_WARNING}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getInverterCompressorWarningChannel() {
		return this.channel(ChannelId.INVERTER_COMPRESSOR_WARNING);
	}

	/**
	 * Gets the Channel for {@link ChannelId#INVERTER_COMPRESSOR_WARNING}.
	 *
	 * @return the Channel
	 */
	public default Channel<Integer> getInverterCompressorWarning() {
		return this.channel(ChannelId.INVERTER_COMPRESSOR_WARNING);
	}

	/**
	 * Gets the Channel for {@link ChannelId#INVERTER_COMPRESSOR_FAULT}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getInverterCompressorFaultChannel() {
		return this.channel(ChannelId.INVERTER_COMPRESSOR_FAULT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#INVERTER_COMPRESSOR_FAULT}.
	 *
	 * @return the Channel
	 */
	public default Value<Boolean> getInverterCompressorFault() {
		return this.getInverterCompressorFaultChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#LOW_PRESSURE_WARNING}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getLowPressureWarningChannel() {
		return this.channel(ChannelId.LOW_PRESSURE_WARNING);
	}

	/**
	 * Gets the Channel for {@link ChannelId#LOW_PRESSURE_WARNING}.
	 *
	 * @return the Channel
	 */
	public default Value<Boolean> getLowPressureWarning() {
		return this.getLowPressureWarningChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#LOW_PRESSURE_FAULT}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getLowPressureFaultChannel() {
		return this.channel(ChannelId.LOW_PRESSURE_FAULT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#LOW_PRESSURE_FAULT}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getLowPressureFault() {
		return this.getLowPressureFaultChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#HIGH_PRESSURE_WARNING}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getHighPressureWarningChannel() {
		return this.channel(ChannelId.HIGH_PRESSURE_WARNING);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HIGH_PRESSURE_WARNING}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getHighPressureWarning() {
		return this.getHighPressureWarningChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#HIGH_PRESSURE_FAULT}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getHighPressureFaultChannel() {
		return this.channel(ChannelId.HIGH_PRESSURE_FAULT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HIGH_PRESSURE_FAULT}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getHighPressureFault() {
		return this.getHighPressureFaultChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#OVER_TEMPERATURE_WARNING_30_MIN}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getOverTemperatureWarning30MinChannel() {
		return this.channel(ChannelId.OVER_TEMPERATURE_WARNING_30_MIN);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OVER_TEMPERATURE_WARNING_30_MIN}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getOverTemperatureWarning30Min() {
		return this.getOverTemperatureWarning30MinChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#OVER_TEMPERATURE_FAULT_60_MIN}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getOverTemperatureFault60MinChannel() {
		return this.channel(ChannelId.OVER_TEMPERATURE_FAULT_60_MIN);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OVER_TEMPERATURE_FAULT_60_MIN}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getOverTemperatureFault60Min() {
		return this.getOverTemperatureFault60MinChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#UNDER_TEMPERATURE_WARNING_30_MIN}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getUnderTemperatureWarning30MinChannel() {
		return this.channel(ChannelId.UNDER_TEMPERATURE_WARNING_30_MIN);
	}

	/**
	 * Gets the Channel for {@link ChannelId#UNDER_TEMPERATURE_WARNING_30_MIN}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getUnderTemperatureWarning30Min() {
		return this.getUnderTemperatureWarning30MinChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#INVERTER_ERROR_COMPRESSOR}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getInverterErrorCompressorChannel() {
		return this.channel(ChannelId.INVERTER_ERROR_COMPRESSOR);
	}

	/**
	 * Gets the Channel for {@link ChannelId#INVERTER_ERROR_COMPRESSOR}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getInverterErrorCompressor() {
		return this.getInverterErrorCompressorChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#COOLING_CIRCUIT_1_SET_POINT}.
	 *
	 * @return the Channel
	 */
	default WriteChannel<Integer> getCoolingCircuit1SetPointChannel() {
		return this.channel(ChannelId.COOLING_CIRCUIT_1_SET_POINT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OVER_TEMPERATURE_WARNING}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getOverTemperatureWarningChannel() {
		return this.channel(ChannelId.OVER_TEMPERATURE_WARNING);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OVER_TEMPERATURE_WARNING}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getOverTemperatureWarning() {
		return this.getOverTemperatureWarningChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#OVER_TEMPERATURE_ALARM}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getOverTemperatureAlarmChannel() {
		return this.channel(ChannelId.OVER_TEMPERATURE_ALARM);
	}

	/**
	 * Gets the Channel for {@link ChannelId#OVER_TEMPERATURE_WARNING}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getOverTemperatureAlarm() {
		return this.getOverTemperatureAlarmChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#LOW_PRESSURE_ALARM}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getLowPressureAlarmChannel() {
		return this.channel(ChannelId.LOW_PRESSURE_ALARM);
	}

	/**
	 * Gets the Channel for {@link ChannelId#LOW_PRESSURE_ALARM}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getLowPressureAlarm() {
		return this.getLowPressureAlarmChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#HIGH_PRESSURE_ALARM}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getHighPressureAlarmChannel() {
		return this.channel(ChannelId.HIGH_PRESSURE_ALARM);
	}

	/**
	 * Gets the Channel for {@link ChannelId#HIGH_PRESSURE_ALARM}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getHighPressureAlarm() {
		return this.getHighPressureAlarmChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_RELATED_FAULT}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getTemperatureRelatedFaultChannel() {
		return this.channel(ChannelId.TEMPERATURE_RELATED_FAULT);
	}

	/**
	 * Gets the Channel for {@link ChannelId#TEMPERATURE_RELATED_FAULT}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getTemperatureRelatedFault() {
		return this.getTemperatureRelatedFaultChannel().value();
	}

	/**
	 * Gets the Channel for {@link ChannelId#UNDER_TEMPERATURE_WARNING}.
	 *
	 * @return the Channel
	 */
	default Channel<Boolean> getUnderTemperatureAlarmChannel() {
		return this.channel(ChannelId.UNDER_TEMPERATURE_WARNING);
	}

	/**
	 * Gets the Channel for {@link ChannelId#UNDER_TEMPERATURE_WARNING}.
	 *
	 * @return the Channel
	 */
	default Value<Boolean> getUnderTemperatureAlarm() {
		return this.getUnderTemperatureAlarmChannel().value();
	}

	/**
	 * Gets the target Start/Stop mode from config or StartStop-Channel.
	 *
	 * @return {@link StartStop}
	 */
	StartStop getStartStopTarget();
}
