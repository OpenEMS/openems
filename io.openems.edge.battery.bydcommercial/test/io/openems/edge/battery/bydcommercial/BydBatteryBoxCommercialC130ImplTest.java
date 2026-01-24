package io.openems.edge.battery.bydcommercial;

import static io.openems.edge.common.startstop.StartStopConfig.AUTO;

import org.junit.Test;

import io.openems.common.test.DummyConfigurationAdmin;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyComponentManager;

public class BydBatteryBoxCommercialC130ImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new BydBatteryBoxCommercialC130Impl()) //
				.addReference("componentManager", new DummyComponentManager()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("battery0") //
						.setModbusId("modbus0") //
						.setStartStop(AUTO) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
