package io.openems.edge.aircooling.blueeplus;

import org.junit.jupiter.api.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class BlueEPlusTest {

	@Test
	public void testKernel66() throws Exception {
		new ComponentTest(new BlueEPlusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withInputRegisters(3038, 0, StatusValues.OK.getValue()) //
						.withInputRegisters(3728, 0, StatusValues.ERROR.getValue())) //
				.activate(MyConfig.create() //
						.setId("airCooling0") //
						.setModbusId("modbus0") //
						.setIotInterfaceVersion(IotInterfaceVersion.KERNEL_6_6) //
						.setModbusUnitId(2) //
						.build()) //
				.next(new TestCase(), 20) //
				.next(new TestCase() //
						.output(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_STATUS, StatusValues.OK) //
						.output(BlueEPlus.ChannelId.MONITORING_COOLING_STATUS, StatusValues.ERROR)) //
				.deactivate();
	}

	@Test
	public void testKernel419() throws Exception {
		new ComponentTest(new BlueEPlusImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0") //
						.withInputRegisters(396, 0, StatusValues.OK.getValue()) //
						.withInputRegisters(1021, 0, StatusValues.ERROR.getValue())) //
				.activate(MyConfig.create() //
						.setId("airCooling0") //
						.setModbusId("modbus0") //
						.setIotInterfaceVersion(IotInterfaceVersion.KERNEL_4_19) //
						.setModbusUnitId(2) //
						.build()) //
				.next(new TestCase(), 20) //
				.next(new TestCase() //
						.output(BlueEPlus.ChannelId.INTERNAL_TEMPERATURE_STATUS, StatusValues.OK) //
						.output(BlueEPlus.ChannelId.MONITORING_COOLING_STATUS, StatusValues.ERROR)) //
				.deactivate();
	}
}
