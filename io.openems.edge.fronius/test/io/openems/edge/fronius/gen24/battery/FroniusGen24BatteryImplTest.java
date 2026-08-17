package io.openems.edge.fronius.gen24.battery;

import org.junit.jupiter.api.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class FroniusGen24BatteryImplTest {

	@Test
	void test() throws Exception {

		// Battery is now fully independent - no OSGi @Reference to the
		// BatteryInverter. It performs its own SunSpec discovery against the
		// same physical device (hence same Unit-ID as the inverter would use),
		// via its own Modbus bridge.
		new ComponentTest(new FroniusGen24BatteryImpl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setModbusUnitId(1) //
						.setChargeMaxVoltage(480) //
						.setDischargeMinVoltage(320) //
						.setNumberOfModules(4) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}
}
