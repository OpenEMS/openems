package io.openems.edge.battery.bmw;

import static io.openems.edge.battery.bmw.enums.BatteryState.DEFAULT;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class BmwBatteryImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new BmwBatteryImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setBatteryState(DEFAULT) //
						.setErrorDelay(0) //
						.setMaxStartAttempts(0) //
						.setMaxStartTime(0) //
						.setPendingTolerance(0) //
						.setStartUnsuccessfulDelay(0) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
