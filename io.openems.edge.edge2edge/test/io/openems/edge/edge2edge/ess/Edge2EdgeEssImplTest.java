package io.openems.edge.edge2edge.ess;

import org.junit.Test;

import io.openems.common.channel.AccessMode;
import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;
import io.openems.edge.common.test.DummyConfigurationAdmin;

public class Edge2EdgeEssImplTest {

	private static final String COMPONENT_ID = "ess0";
	private static final String MODBUS_ID = "modbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new Edge2EdgeEssImpl()) //
				.addReference("cm", new DummyConfigurationAdmin()) //
				.addReference("setModbus", new DummyModbusBridge(MODBUS_ID)) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setModbusId(MODBUS_ID) //
						.setRemoteAccessMode(AccessMode.READ_WRITE) //
						.setRemoteComponentId(COMPONENT_ID) //
						.build())
				.next(new TestCase());
	}

}
