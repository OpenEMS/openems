package io.openems.edge.system.fenecon.masterbox2v0;

import org.junit.Test;

import io.openems.common.channel.Level;
import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.api.ModbusComponent;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.component.OpenemsComponent;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.system.fenecon.masterbox2v0.enums.GridState;
import io.openems.edge.system.fenecon.masterbox2v0.enums.StateEnergyMeter;

public class MasterBox2v0ImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new MasterBox2v0Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withRegisters(8, 0x0032) //
						.withRegisters(9, 0x0001) //
						.withRegisters(101, 0x0006) //
						.withRegisters(102, 0x0001) //
						.withRegisters(103, 0x0320) //
						.withRegisters(104, 0x00a0) //
						.withRegisters(105, 0x0001) //
						.withRegisters(106, 0x0001) //
						.withRegisters(107, 0x0001) //
						.withRegisters(108, 0x0001) //
						.withRegisters(109, 0x0001) //
						.withRegisters(110, 0x0001) //
						.withRegisters(301, 0x07d0) //
						.withRegisters(302, 0x07d0) //
						.withRegisters(303, 0x07d0) //
						.withRegisters(304, 0x0c80) //
						.withRegisters(305, 0x0c80) //
						.withRegisters(306, 0x0c80) //
						.withRegisters(307, 0x0280) //
						.withRegisters(308, 0x0280) //
						.withRegisters(309, 0x0280) //
						.withRegisters(313, 0x0001, 0x86A0) //
						.withRegisters(315, 0x0000)) //
				.activate(MyConfig.create() //
						.setId("ioc0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(10) //
						.build())
				.next(new TestCase("All Channels") //
						.activateStrictMode() //
						.input(MasterBox2v0.ChannelId.ANALOG_OUT_VOLTAGE, 500) //
						.input(MasterBox2v0.ChannelId.ANALOG_OUT_CONTROL, true) //
						.input(MasterBox2v0.ChannelId.RELAY_1, true) //
						.input(MasterBox2v0.ChannelId.RELAY_2, true) //
						.input(MasterBox2v0.ChannelId.RELAY_3, true) //
						.input(MasterBox2v0.ChannelId.RELAY_4, true) //
						.input(MasterBox2v0.ChannelId.RELAY_5, true) //
						.input(MasterBox2v0.ChannelId.RELAY_6, true) //
						.output(ModbusComponent.ChannelId.MODBUS_COMMUNICATION_FAILED, false) //
						.output(OpenemsComponent.ChannelId.STATE, Level.OK) //
						.output(MasterBox2v0.ChannelId.ANALOG_OUT_VOLTAGE, 500) //
						.output(MasterBox2v0.ChannelId.ANALOG_OUT_CONTROL, true) //
						.output(MasterBox2v0.ChannelId.HW_STARTUP, null) //
						.output(MasterBox2v0.ChannelId.HW_TEMPERATURE_SENSOR_TYPE, null) //
						.output(MasterBox2v0.ChannelId.HW_ANALOG_OUT_MODE, null) //
						.output(MasterBox2v0.ChannelId.HW_SPI_ENERGY_ENABLE, null) //
						.output(MasterBox2v0.ChannelId.HW_CAN_TOWER_ENABLE, null) //
						.output(MasterBox2v0.ChannelId.GRID_STATE, GridState.UNDEFINED) //
						.output(MasterBox2v0.ChannelId.TEMPERATURE, null) //
						.output(MasterBox2v0.ChannelId.HUMIDITY, null) //
						.output(MasterBox2v0.ChannelId.RELAY_1, true) //
						.output(MasterBox2v0.ChannelId.RELAY_2, true) //
						.output(MasterBox2v0.ChannelId.RELAY_3, true) //
						.output(MasterBox2v0.ChannelId.RELAY_4, true) //
						.output(MasterBox2v0.ChannelId.RELAY_5, true) //
						.output(MasterBox2v0.ChannelId.RELAY_6, true) //
						.output(MasterBox2v0.ChannelId.VOLTAGE_L1_ENERGYMETER, 20000) //
						.output(MasterBox2v0.ChannelId.VOLTAGE_L2_ENERGYMETER, 20000) //
						.output(MasterBox2v0.ChannelId.VOLTAGE_L3_ENERGYMETER, 20000) //
						.output(MasterBox2v0.ChannelId.CURRENT_L1_ENERGYMETER, 32000) //
						.output(MasterBox2v0.ChannelId.CURRENT_L2_ENERGYMETER, 32000) //
						.output(MasterBox2v0.ChannelId.CURRENT_L3_ENERGYMETER, 32000) //
						.output(MasterBox2v0.ChannelId.ACTIVE_POWER_L1_ENERGYMETER, 640) //
						.output(MasterBox2v0.ChannelId.ACTIVE_POWER_L2_ENERGYMETER, 640) //
						.output(MasterBox2v0.ChannelId.ACTIVE_POWER_L3_ENERGYMETER, 640) //
						.output(MasterBox2v0.ChannelId.TIME_STAMP_ENERGYMETER, null) //
						.output(MasterBox2v0.ChannelId.STATUS_ENERGYMETER, StateEnergyMeter.UNDEFINED) //
						.output(MasterBox2v0.ChannelId.DEBUG_ANALOG_OUT_VOLTAGE, 500) //
						.output(MasterBox2v0.ChannelId.DEBUG_ANALOG_OUT_CONTROL, true) //
						.output(MasterBox2v0.ChannelId.DEBUG_RELAY_1, true) //
						.output(MasterBox2v0.ChannelId.DEBUG_RELAY_2, true) //
						.output(MasterBox2v0.ChannelId.DEBUG_RELAY_3, true) //
						.output(MasterBox2v0.ChannelId.DEBUG_RELAY_4, true) //
						.output(MasterBox2v0.ChannelId.DEBUG_RELAY_5, true) //
						.output(MasterBox2v0.ChannelId.DEBUG_RELAY_6, true)) //
				.next(new TestCase()) //
				.next(new TestCase("Read Channels within a low Priority ReadTask")
						.output(MasterBox2v0.ChannelId.HW_STARTUP, false) //
						.output(MasterBox2v0.ChannelId.HW_TEMPERATURE_SENSOR_TYPE, true) //
						.output(MasterBox2v0.ChannelId.HW_ANALOG_OUT_MODE, true) //
						.output(MasterBox2v0.ChannelId.HW_SPI_ENERGY_ENABLE, false) //
						.output(MasterBox2v0.ChannelId.HW_CAN_TOWER_ENABLE, false) //
						.output(MasterBox2v0.ChannelId.GRID_STATE, GridState.POWER_SUPPLY) //
						.output(MasterBox2v0.ChannelId.TEMPERATURE, 40.0) //
						.output(MasterBox2v0.ChannelId.HUMIDITY, 16.0) //
						.output(MasterBox2v0.ChannelId.TIME_STAMP_ENERGYMETER, 100000L) //
						.output(MasterBox2v0.ChannelId.STATUS_ENERGYMETER, StateEnergyMeter.NO_ERROR) //
				);
	}
}
