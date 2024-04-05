package io.openems.edge.bridge.mbus;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class BridgeMbusImplTest {

	private static final String COMPONENT_ID = "mbus0";

	@Test
	public void test() throws Exception {
		new ComponentTest(new BridgeMbusImpl()) //
				.activate(MyConfig.create() //
						.setId(COMPONENT_ID) //
						.setPortName("/dev/ttyUSB0") //
						.setBaudrate(2400) //
						.build()) //
				.next(new TestCase()); //
	}

}
