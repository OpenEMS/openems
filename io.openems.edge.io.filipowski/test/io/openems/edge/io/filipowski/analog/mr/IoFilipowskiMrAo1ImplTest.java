package io.openems.edge.io.filipowski.analog.mr;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class IoFilipowskiMrAo1ImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new IoFilipowskiMrAo1Impl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("component0") //
						.setModbusId("modbus0") //
						.setRelayContact(AnalogOutput.OUTPUT_1) //
						.build())
				.next(new TestCase());
	}
}
