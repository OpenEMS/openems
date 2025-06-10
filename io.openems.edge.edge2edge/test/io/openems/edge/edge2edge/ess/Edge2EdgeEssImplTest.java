package io.openems.edge.edge2edge.ess;

import org.junit.Test;

import io.openems.common.channel.AccessMode;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class Edge2EdgeEssImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new Edge2EdgeEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("ess0") //
						.setModbusId("modbus0") //
						.setRemoteAccessMode(AccessMode.READ_WRITE) //
						.setRemoteComponentId("ess0") //
						.build())
				.next(new TestCase());
	}

}
