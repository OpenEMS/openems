package io.openems.edge.aircooling.seifert;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import io.openems.common.channel.Level;
import io.openems.edge.aircooling.seifert.Seifert.ChannelId;
import io.openems.edge.aircooling.seifert.enums.ModeState;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class SeifertTest {

	@Test
	public void testDebugLog() throws Exception {
		final var seifert = new SeifertImpl();
		var test = new ComponentTest(seifert) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						// Holding Registers (FC3), addresses 0-18
						.withRegisters(0, //
								0, // 0 SET_COOLING_SETPOINT (device; config writes 315)
								0, // 1 SET_HIGH_TEMP_ALARM_SETPOINT (device; config writes 450)
								0, // 2 SET_LOW_TEMP_ALARM_SETPOINT (device; config writes 50)
								0, // 3 SET_HEATING_SETPOINT (device; config writes 150)
								0, // 4 bits: SET_ENABLE_INPUT_1_INVERT / SET_ENABLE_TEMP_UNIT
								315, // 5 CONTROL_SETPOINT
								450, // 6 HIGH_TEMP_SETPOINT
								50, // 7 LOW_TEMP_SETPOINT
								150, // 8 HEATER_SETPOINT
								300, // 9 TEMP_POT
								450, // 10 ALARM_POT
								150, // 11 HEATER_POT
								300, // 12 CONTROL_SENSOR
								2, // 13 MODE_STATE: Cooling
								0x1000, // 14 bits: COMPRESSOR_WARNING
								0x000A, // 15 bits: OUTPUT_STATUS_COMPRESSOR + OUTPUT_STATUS_INTERNAL_BLOWER
								0, // 16 door contacts
								305, // 17 INTERNAL_AIR_OUTLET_TEMP
								315)) // 18 AMBIENT_TEMPERATURE
				.activate(MyConfig.create() //
						.setId("cooling0") //
						.setModbusId("modbus0") //
						.setCoolingSetpoint(315) //
						.setHeatingSetpoint(150) //
						.setHighTempAlarmSetpoint(450) //
						.setLowTempAlarmSetpoint(50) //
						.build()) //
				.next(new TestCase("Strict-Mode: all channels defined; reads from simulated Modbus registers") //
						.activateStrictMode() //
						// OpenemsComponent + ModbusComponent
						// STATE is WARNING, because COMPRESSOR_WARNING (register 14, bit 12) is active
						.output(OpenemsComponent.ChannelId.STATE, Level.WARNING) //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
						// Read values from simulated Modbus registers
						.outputReadValue(ChannelId.SET_COOLING_SETPOINT, 0) //
						.outputReadValue(ChannelId.SET_HIGH_TEMP_ALARM_SETPOINT, 0) //
						.outputReadValue(ChannelId.SET_LOW_TEMP_ALARM_SETPOINT, 0) //
						.outputReadValue(ChannelId.SET_HEATING_SETPOINT, 0) //
						.outputReadValue(ChannelId.SET_ENABLE_INPUT_1_INVERT, false) //
						.outputReadValue(ChannelId.SET_ENABLE_TEMP_UNIT, false) //
						.outputReadValue(ChannelId.CONTROL_SETPOINT, 315) //
						.outputReadValue(ChannelId.HIGH_TEMP_SETPOINT, 450) //
						.outputReadValue(ChannelId.LOW_TEMP_SETPOINT, 50) //
						.outputReadValue(ChannelId.HEATER_SETPOINT, 150) //
						.outputReadValue(ChannelId.TEMP_POT, 300) //
						.outputReadValue(ChannelId.ALARM_POT, 450) //
						.outputReadValue(ChannelId.HEATER_POT, 150) //
						.outputReadValue(ChannelId.CONTROL_SENSOR, 300) //
						.outputReadValue(ChannelId.MODE_STATE, ModeState.COOLING) //
						.outputReadValue(ChannelId.INTERNAL_AIR_OUTLET_TEMP, 305) //
						.outputReadValue(ChannelId.AMBIENT_TEMPERATURE, 315) //
						// Faults (register 14 = 0x1000; all fault bits are 0)
						.outputReadValue(ChannelId.COMPRESSOR_OR_ICE_ALGORITHM_FAULT, false) //
						.outputReadValue(ChannelId.AMBIENT_SENSOR_FAULT, false) //
						.outputReadValue(ChannelId.AMBIENT_BLOWER_FAULT, false) //
						.outputReadValue(ChannelId.AUX_OR_PRESSURE_PIPE_SENSOR_FAULT, false) //
						.outputReadValue(ChannelId.CAREL_INVERTER_COMMS_FAULT, false) //
						.outputReadValue(ChannelId.DELTA_OUTLET_INLET_TEMP_FAULT, false) //
						.outputReadValue(ChannelId.HIGH_INTERNAL_TEMP_FAULT, false) //
						.outputReadValue(ChannelId.ICING_SENSOR_FAULT, false) //
						.outputReadValue(ChannelId.INTERNAL_BLOWER_FAULT, false) //
						.outputReadValue(ChannelId.INTERNAL_TEMP_SENSOR_FAULT, false) //
						.outputReadValue(ChannelId.LOW_INTERNAL_TEMP_FAULT, false) //
						.outputReadValue(ChannelId.PHASE_FAIL_OR_DI2_CLOSED_FAULT, false) //
						.outputReadValue(ChannelId.PRESSURE_PIPE_ALGORITHM_FAULT, false) //
						// Warnings (register 14 bit 12 -> COMPRESSOR_WARNING)
						.outputReadValue(ChannelId.AMBIENT_BLOWER_WARNING, false) //
						.outputReadValue(ChannelId.COMPRESSOR_WARNING, true) //
						.outputReadValue(ChannelId.INTERNAL_BLOWER_WARNING, false) //
						// Output statuses (register 15 = 0x000A)
						.outputReadValue(ChannelId.OUTPUT_STATUS_HEATER, false) //
						.outputReadValue(ChannelId.OUTPUT_STATUS_INTERNAL_BLOWER, true) //
						.outputReadValue(ChannelId.OUTPUT_STATUS_AMBIENT_BLOWER, false) //
						.outputReadValue(ChannelId.OUTPUT_STATUS_COMPRESSOR, true) //
						// Door contacts (register 16 = 0)
						.outputReadValue(ChannelId.DOOR_CONTACT_1_STATUS, false) //
						.outputReadValue(ChannelId.DOOR_CONTACT_2_STATUS, false) //
						.outputReadValue(ChannelId.DOOR_CONTACT_3_STATUS, false) //
						// Write channels: configured setpoints are written every cycle
						.output(ChannelId.SET_COOLING_SETPOINT, 315) //
						.output(ChannelId.SET_HEATING_SETPOINT, 150) //
						.output(ChannelId.SET_HIGH_TEMP_ALARM_SETPOINT, 450) //
						.output(ChannelId.SET_LOW_TEMP_ALARM_SETPOINT, 50) //
						.output(ChannelId.SET_ENABLE_TEMP_UNIT, false) //
						.output(ChannelId.SET_ENABLE_INPUT_1_INVERT, null));

		assertEquals("State: 2:Cooling|AmbientTemperature: 315 °dC|InternalTemperature: 300 °dC", //
				seifert.debugLog());

		test.next(new TestCase()) //
				.deactivate();
	}

}
