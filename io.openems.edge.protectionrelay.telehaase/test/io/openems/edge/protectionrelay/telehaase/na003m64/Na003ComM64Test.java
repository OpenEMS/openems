package io.openems.edge.protectionrelay.telehaase.na003m64;

import org.junit.Test;

import io.openems.edge.bridge.modbus.test.DummyModbusBridge;
import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class Na003ComM64Test {

	@Test
	public void test() throws Exception {
		new ComponentTest(new Na003ComM64Impl()) //
				.addReference("setModbus", new DummyModbusBridge("modbus0")) //
				.activate(MyConfig.create() //
						.setId("relay0") //
						.setModbusId("modbus0") //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
