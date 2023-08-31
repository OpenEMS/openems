package io.openems.edge.system.fenecon.industrial.l;

import org.junit.Test;

import io.openems.edge.battery.test.DummyBattery;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;
import io.openems.edge.system.fenecon.industrial.l.envicool.Envicool;

public class SystemFeneconIndustrialLImplTest {

	private static final String COMPONENT_ID = "component0";
	private static final String MODBUS_ID = "modbus0";
	private static final String BATTERY1_ID = "battery1";
	private static final String BATTERY2_ID = "battery2";

	@Test
	public void test() throws Exception {
		new ComponentTest(new SystemFeneconIndustrialLImpl()) //
				.addReference("setAcModbus", new DummyModbusBridge(MODBUS_ID)) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("addBattery", new DummyBattery(BATTERY1_ID)) //
				.addReference("addBattery", new DummyBattery(BATTERY2_ID)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setBatteryIds(BATTERY1_ID, BATTERY2_ID) //
						.setAcModbusId(MODBUS_ID) //
						.setAcMode(Envicool.Mode.DISABLED) //
						.setAcCoolingSetPoint(20) //
						.setAcHeatingSetPoint(10) //
						.build())
				.next(new TestCase());
	}

}
