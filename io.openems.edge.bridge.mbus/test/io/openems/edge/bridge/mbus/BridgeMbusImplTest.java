package io.openems.edge.bridge.mbus;

import org.junit.Test;

import io.openems.edge.common.test.AbstractComponentTest.TestCase;
import io.openems.edge.common.test.ComponentTest;

public class BridgeMbusImplTest {

	@Test
	public void test() throws Exception {
		new ComponentTest(new BridgeMbusImpl()) //
				.activate(MyConfig.create() //
						.setId("mbus0") //
						.setPortName("/dev/ttyUSB0") //
						.setBaudrate(2400) //
						.build()) //
				.next(new TestCase()) //
				.deactivate();
	}

}
