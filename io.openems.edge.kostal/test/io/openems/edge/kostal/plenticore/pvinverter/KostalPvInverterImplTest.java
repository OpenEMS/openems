package io.openems.edge.kostal.plenticore.pvinverter;

import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.ACTIVE_POWER_L3;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.CURRENT_L3;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.FREQUENCY;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE_L1;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE_L2;
import static io.openems.edge.meter.api.ElectricityMeter.ChannelId.VOLTAGE_L3;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.common.types.MeterType;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class KostalPvInverterImplTest {

	private static final String PV_INVERTER_ID = "pvInverter0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void testActivateDeactivate() throws Exception {
		new ComponentTest(new KostalPvInverterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(PV_INVERTER_ID) //
						.setReadOnly(true) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(71) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

	@Test
	public void testPvProductionData() throws Exception {
		// Test validates that component can be activated and process data
		new ComponentTest(new KostalPvInverterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(PV_INVERTER_ID) //
						.setReadOnly(true) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(71) //
						.build()) //
				.next(new TestCase("Normal PV production") //
						.input(ACTIVE_POWER, 8000) //
						.input(ACTIVE_POWER_L1, 2500) //
						.input(ACTIVE_POWER_L2, 2800) //
						.input(ACTIVE_POWER_L3, 2700)) //
				.next(new TestCase("High PV production") //
						.input(ACTIVE_POWER, 12000)) //
				.next(new TestCase("Low PV production") //
						.input(ACTIVE_POWER, 500)) //
				.next(new TestCase("No PV production") //
						.input(ACTIVE_POWER, 0)) //
				.deactivate();
	}

	@Test
	public void testReadOnlyMode() throws Exception {
		new ComponentTest(new KostalPvInverterImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(PV_INVERTER_ID) //
						.setReadOnly(true) //
						.setModbusId(MODBUS_ID) //
						.setModbusUnitId(71) //
						.build()) //
				.next(new TestCase("Read-only mode active")) //
				.deactivate();
	}
}
